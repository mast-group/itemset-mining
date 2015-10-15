package itemsetmining.eval;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import ca.pfv.spmf.tools.other_dataset_tools.FixTransactionDatabaseTool;
import itemsetmining.itemset.Itemset;

public class StatisticalItemsetMining {

	private static final FixTransactionDatabaseTool dbTool = new FixTransactionDatabaseTool();

	public static void main(final String[] args) throws IOException {

		// MTV Parameters
		final String[] datasets = new String[] { "plants", "mammals", "abstracts", "uganda" };
		final double[] minSupps = new double[] { 0.05750265949, 0.1872659176, 0.01164144353, 0.001 }; // relative
		final int[] minAbsSupps = new int[] { 2000, 500, 10, 125 }; // absolute
		// final int noItemsets = 1000;

		// for (int i = 0; i < datasets.length; i++) {

		final String dbPath = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Datasets/Succintly/" + datasets[1]
				+ ".dat";

		// final String saveFile =
		// "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/MTV/" +
		// datasets[i] + ".txt";
		// mineItemsets(new File(dbPath), minSupps[i], noItemsets, new
		// File(saveFile));

		mineKRIMPItemsets(new File(dbPath), minAbsSupps[1]);
		// mineTilingItemsets(new File(dbPath), minSupps[i]); // min
		// area

		// }

	}

	public static LinkedHashMap<Itemset, Double> mineMTVItemsets(final File dbFile, final double minSup,
			final int noItemsets, final File saveFile) throws IOException {

		// Remove transaction duplicates and sort items ascending
		final File TMPDB = File.createTempFile("fixed-dataset", ".dat");
		dbTool.convert(dbFile.getAbsolutePath(), TMPDB.getAbsolutePath());

		// Set MTV settings
		final String cmd[] = new String[6];
		cmd[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Packages/mtv/mtv.sh";
		cmd[1] = "-f " + TMPDB;
		cmd[2] = "-s " + minSup;
		cmd[3] = "-k " + noItemsets;
		cmd[4] = "-o " + saveFile;
		cmd[5] = "-g 10"; // Max items per group (for efficiency)
		// cmd[6] = "-q" // Quiet mode
		runScript(cmd);

		return readMTVItemsets(saveFile);
	}

	public static LinkedHashMap<Itemset, Double> mineTilingItemsets(final File dbFile, final double minArea)
			throws IOException {
		final String tileminerOutDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/TileMiner/";

		// Remove transaction duplicates and sort items ascending
		final String dbName = FilenameUtils.getBaseName(dbFile.getName());
		final String dB = tileminerOutDir + dbName + ".dat";
		dbTool.convert(dbFile.getAbsolutePath(), dB);

		// Set MTV settings
		final String cmd[] = new String[3];
		cmd[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Packages/tileminer/bin/zbk-tiling";
		cmd[1] = dbName + ".dat";
		cmd[2] = "" + minArea;
		runScript(cmd, tileminerOutDir);

		// Find output file
		final String[] fileNames = new File(tileminerOutDir).list(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String fileName) {
				if (fileName.startsWith("tiling-ov-" + dbName) && fileName.endsWith(".txt"))
					return true;
				return false;
			}
		});
		Arrays.sort(fileNames);

		return readTilingItemsets(new File(fileNames[0]));

	}

	public static LinkedHashMap<Itemset, Double> mineKRIMPItemsets(final File dbFile, final int absMinSup)
			throws IOException {
		final String krimpDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Packages/krimp/";
		final String krimpDataDir = "/disk/data2/jfowkes/krimp/";
		final String krimpOutDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/KRIMP/";

		// Remove transaction duplicates and sort items ascending
		final String dbName = FilenameUtils.getBaseName(dbFile.getName());
		final String dB = krimpDataDir + "data/datasets/" + dbName + ".dat";
		dbTool.convert(dbFile.getAbsolutePath(), dB);

		// Convert db to krimp format
		final Path cconf = Paths.get(krimpDir + "bin/convertdb.conf");
		final List<String> clines = Files.readAllLines(cconf);
		clines.set(12, "dbName = " + dbName);
		Files.write(cconf, clines);
		String cmd[] = new String[2];
		cmd[0] = "./krimp";
		cmd[1] = "convertdb";
		runScript(cmd, krimpDir + "bin/");

		// Generate item number mapping
		final Path aconf = Paths.get(krimpDir + "bin/analysedb.conf");
		final List<String> alines = Files.readAllLines(aconf);
		alines.set(12, "dbName = " + dbName);
		Files.write(aconf, alines);
		cmd[1] = "analysedb";
		runScript(cmd, krimpDir + "bin/");

		// Set KRIMP settings and run
		final Path conf = Paths.get(krimpDir + "bin/compress.conf");
		final List<String> lines = Files.readAllLines(conf);
		lines.set(21, "iscName = " + dbName + "-all-" + absMinSup + "d");
		lines.set(42, "numThreads = " + Runtime.getRuntime().availableProcessors());
		Files.write(conf, lines);
		cmd[1] = "compress";
		runScript(cmd, krimpDir + "bin/");

		// Decode itemsets
		cmd = new String[3];
		cmd[0] = "python";
		cmd[1] = "DecodeItemsets.py";
		cmd[2] = dbName + "-all-" + absMinSup + "d";
		runScript(cmd, krimpDir + "bin/");

		return readKRIMPItemsets(new File(krimpOutDir + dbName + "_itemsets.txt"));
	}

	/** Read in TileMiner itemsets */
	public static LinkedHashMap<Itemset, Double> readTilingItemsets(final File output) throws IOException {
		final LinkedHashMap<Itemset, Double> itemsets = new LinkedHashMap<>();

		final String[] lines = FileUtils.readFileToString(output).split("\n");

		for (int i = 6; i < lines.length; i++) {
			if (!lines[i].trim().isEmpty() && lines[i].charAt(0) != '#') {
				final String[] splitLine = lines[i].trim().split(" ");
				final Itemset itemset = new Itemset();
				for (int j = 1; j < splitLine.length - 1; j++)
					itemset.add(Integer.parseInt(splitLine[j]));
				final double area = Double
						.parseDouble(splitLine[splitLine.length - 1].replace("(", "").replace(")", ""));
				itemsets.put(itemset, area);
			}
		}

		return itemsets;
	}

	/** Read in KRIMP itemsets */
	public static LinkedHashMap<Itemset, Double> readKRIMPItemsets(final File output) throws IOException {
		final LinkedHashMap<Itemset, Double> itemsets = new LinkedHashMap<>();

		final String[] lines = FileUtils.readFileToString(output).split("\n");

		for (final String line : lines) {
			if (!line.trim().isEmpty()) {
				final String[] splitLine = line.split(" ");
				final Itemset itemset = new Itemset();
				for (int j = 1; j < splitLine.length; j++)
					itemset.add(Integer.parseInt(splitLine[j].trim()));
				final double usage = Double.parseDouble(splitLine[0].trim());
				itemsets.put(itemset, usage);
			}
		}

		return itemsets;
	}

	/** Read in MTV itemsets */
	public static LinkedHashMap<Itemset, Double> readMTVItemsets(final File output) throws IOException {
		final LinkedHashMap<Itemset, Double> itemsets = new LinkedHashMap<>();

		final String[] lines = FileUtils.readFileToString(output).split("\n");

		for (final String line : lines) {
			if (!line.trim().isEmpty() && line.charAt(0) != '#') {
				final String[] splitLine = line.split(" ");
				final Itemset itemset = new Itemset();
				for (int i = 1; i < splitLine.length; i++)
					itemset.add(Integer.parseInt(splitLine[i].trim()));
				final double prob = Double.parseDouble(splitLine[0].trim());
				itemsets.put(itemset, prob);
			}
		}

		return itemsets;
	}

	/** Run shell script with command line arguments */
	public static void runScript(final String cmd[]) {
		runScript(cmd, null);
	}

	/** Run shell script with command line arguments in working directory */
	public static void runScript(final String cmd[], final String workingDir) {

		try {
			final ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.inheritIO();
			if (workingDir != null)
				pb.directory(new File(workingDir));
			final Process process = pb.start();
			process.waitFor();
			process.destroy();
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

}
