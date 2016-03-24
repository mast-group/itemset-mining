package itemsetmining.eval;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.TeeOutputStream;

import com.google.common.collect.Sets;
import com.google.common.io.Files;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.ItemsetMining;
import itemsetmining.main.ItemsetMiningCore;
import itemsetmining.transaction.TransactionGenerator;
import itemsetmining.util.Logging;

public class ItemsetPrecisionRecall {

	/** Main Settings */
	private static final File dbFile = new File("/disk/data1/jfowkes/itemset.txt");
	private static final File saveDir = new File("/disk/data1/jfowkes/logs/");

	/** FIM Issues to incorporate */
	private static final String name = "caviar";
	private static final int noIterations = 300;

	/** Previously mined Itemsets to use for background distribution */
	private static final File itemsetLog = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Logs/IIM-plants-23.09.2015-06:45:55.log");
	private static final int noTransactions = 10_000;
	private static final int noSpecialItemsets = 30;

	/** Itemset Miner Settings */
	private static final int noMTVIterations = 30;
	private static final int maxStructureSteps = 100_000;
	private static final double minSup = 0.04;

	/** Spark Settings */
	private static final int sparkCores = 64;

	public static void main(final String[] args) throws IOException {

		// Read in background distribution
		final Map<Itemset, Double> backgroundItemsets = new HashMap<>(ItemsetMiningCore.readIIMItemsets(itemsetLog));

		// Set up transaction DB
		final HashMap<Itemset, Double> specialItemsets = TransactionGenerator.generateExampleItemsets(name,
				noSpecialItemsets, 0);
		backgroundItemsets.putAll(specialItemsets);
		// Generate transaction DB
		final HashMap<Itemset, Double> itemsets = TransactionGenerator.generateTransactionDatabase(backgroundItemsets,
				noTransactions, dbFile);
		System.out.print("\n============= ACTUAL ITEMSETS =============\n");
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			System.out.print(String.format("%s\tprob: %1.5f %n", entry.getKey(), entry.getValue()));
		}
		System.out.print("\n");
		System.out.println("No itemsets: " + itemsets.size());
		ItemsetScaling.printTransactionDBStats(dbFile);

		precisionRecall(itemsets, specialItemsets, "MTV");
		precisionRecall(itemsets, specialItemsets, "KRIMP");
		precisionRecall(itemsets, specialItemsets, "SLIM");
		precisionRecall(itemsets, specialItemsets, "CHARM");
		precisionRecall(itemsets, specialItemsets, "IIM");
		// precisionRecall(itemsets, specialItemsets, "Tiling"); // segfaults

	}

	public static void precisionRecall(final HashMap<Itemset, Double> itemsets,
			final HashMap<Itemset, Double> specialItemsets, final String algorithm) throws IOException {

		// Set up logging
		final FileOutputStream outFile = new FileOutputStream(saveDir + "/" + algorithm + "_" + name + "_pr.txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		System.out.println("\nSpecial Itemsets: " + noSpecialItemsets);

		// Mine itemsets
		Map<Itemset, Double> minedItemsets = null;
		final File logFile = Logging.getLogFileName(algorithm, true, saveDir, dbFile);
		final long startTime = System.currentTimeMillis();
		if (algorithm.equals("spark"))
			minedItemsets = runSpark(sparkCores, noIterations);
		else if (algorithm.equals("MTV"))
			minedItemsets = StatisticalItemsetMining.mineMTVItemsets(dbFile, minSup, noMTVIterations, logFile);
		else if (algorithm.equals("CHARM")) {
			CondensedFrequentItemsetMining.mineClosedFrequentItemsetsCharm(dbFile.getAbsolutePath(),
					logFile.getAbsolutePath(), minSup);
			minedItemsets = FrequentItemsetMining.readFrequentItemsetsChiSquared(logFile, dbFile.getAbsolutePath());
		} else if (algorithm.equals("IIM"))
			minedItemsets = ItemsetMining.mineItemsets(dbFile, new InferGreedy(), maxStructureSteps, noIterations,
					logFile);
		else if (algorithm.equals("KRIMP"))
			minedItemsets = StatisticalItemsetMining.mineKRIMPItemsets(dbFile,
					(int) Math.floor(minSup * noTransactions));
		else if (algorithm.equals("SLIM"))
			minedItemsets = StatisticalItemsetMining.mineSLIMItemsets(dbFile, (int) Math.floor(minSup * noTransactions),
					24);
		else if (algorithm.equals("Tiling"))
			minedItemsets = StatisticalItemsetMining.mineTilingItemsets(dbFile, minSup);
		else
			throw new RuntimeException("Incorrect algorithm name.");
		final long endTime = System.currentTimeMillis();
		final double time = (endTime - startTime) / (double) 1000;

		// Calculate sorted precision and recall
		final int len = minedItemsets.size();
		System.out.println("No. mined itemsets: " + len);
		final double[] precision = new double[len];
		final double[] recall = new double[len];
		for (int k = 1; k <= len; k++) {

			final Set<Itemset> topKMined = Sets.newHashSet();
			for (final Entry<Itemset, Double> entry : minedItemsets.entrySet()) {
				topKMined.add(entry.getKey());
				if (topKMined.size() == k)
					break;
			}

			final double noInBoth = Sets.intersection(itemsets.keySet(), topKMined).size();
			final double noSpecialInBoth = Sets.intersection(specialItemsets.keySet(), topKMined).size();
			final double pr = noInBoth / (double) topKMined.size();
			final double rec = noSpecialInBoth / (double) specialItemsets.size();
			precision[k - 1] = pr;
			recall[k - 1] = rec;
		}

		// Output precision and recall
		System.out.println("\n======== " + name + " ========");
		System.out.println("Special Frequency: " + noSpecialItemsets);
		System.out.println("Time: " + time);
		System.out.println("Precision (all): " + Arrays.toString(precision));
		System.out.println("Recall (special): " + Arrays.toString(recall));

	}

	static Map<Itemset, Double> runSpark(final int noCores, final int noIterations) throws IOException {
		final String cmd[] = new String[6];
		cmd[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/git/itemset-mining/run-spark.sh";
		cmd[1] = "-f " + dbFile;
		cmd[2] = " -s " + maxStructureSteps;
		cmd[3] = " -i " + noIterations;
		cmd[4] = " -c " + noCores;
		cmd[5] = " -t false";
		StatisticalItemsetMining.runScript(cmd);

		final File output = new File(ItemsetMining.LOG_DIR + FilenameUtils.getBaseName(dbFile.getName()) + ".log");
		final Map<Itemset, Double> itemsets = ItemsetMiningCore.readIIMItemsets(output);

		final String timestamp = new SimpleDateFormat("-dd.MM.yyyy-HH:mm:ss").format(new Date());
		final File newLog = new File(ItemsetMining.LOG_DIR + "/" + name + timestamp + ".log");
		Files.move(output, newLog);

		return itemsets;
	}

}
