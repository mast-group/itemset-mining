package itemsetmining.eval;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;

import ca.pfv.spmf.algorithms.frequentpatterns.charm.AlgoCharmMFI;
import ca.pfv.spmf.algorithms.frequentpatterns.charm.AlgoCharm_Bitset;
import ca.pfv.spmf.input.transaction_database_list_integers.TransactionDatabase;
import ca.pfv.spmf.patterns.itemset_array_integers_with_tids_bitset.Itemsets;
import ca.pfv.spmf.tools.other_dataset_tools.FixTransactionDatabaseTool;
import itemsetmining.itemset.Itemset;

public class CondensedFrequentItemsetMining {

	private static final FixTransactionDatabaseTool dbTool = new FixTransactionDatabaseTool();

	public static void main(final String[] args) throws IOException {

		// MTV Parameters
		final String[] datasets = new String[] { "plants", "mammals", "abstracts", "uganda" };
		final double[] minSupps = new double[] { 0.05750265949, 0.1872659176, 0.01164144353, 0.001 };

		for (int i = 0; i < datasets.length; i++) {

			final String dbPath = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Datasets/Succintly/" + datasets[i]
					+ ".dat";
			final String saveFile = "/disk/data1/jfowkes/logs/" + datasets[i] + "-closed-fim.txt";
			mineClosedFrequentItemsetsCharm(dbPath, saveFile, minSupps[i]);

		}

	}

	/** Run Charm closed FIM algorithm */
	public static SortedMap<Itemset, Integer> mineClosedFrequentItemsetsCharm(final String dataset,
			final String saveFile, final double minSupp) throws IOException {

		// Remove transaction duplicates and sort items ascending
		final File TMPDB = File.createTempFile("fixed-dataset", ".dat");
		dbTool.convert(dataset, TMPDB.getAbsolutePath());

		final TransactionDatabase database = new TransactionDatabase();
		database.loadFile(TMPDB.getAbsolutePath());

		final AlgoCharm_Bitset algo = new AlgoCharm_Bitset();
		final Itemsets patterns = algo.runAlgorithm(saveFile, database, minSupp, true, 10000);
		algo.printStats();
		// patterns.printItemsets(database.size());

		return toMap(patterns);
	}

	/** Run Charm maximal closed FIM algorithm */
	public static SortedMap<Itemset, Integer> mineMaximalFrequentItemsetsCharm(final String dataset,
			final String saveFile, final double minSupp) throws IOException {

		// Remove transaction duplicates and sort items ascending
		final File TMPDB = File.createTempFile("fixed-dataset", ".dat");
		dbTool.convert(dataset, TMPDB.getAbsolutePath());

		final TransactionDatabase database = new TransactionDatabase();
		database.loadFile(TMPDB.getAbsolutePath());

		final AlgoCharm_Bitset algo_closed = new AlgoCharm_Bitset();
		final Itemsets closed_patterns = algo_closed.runAlgorithm(null, database, minSupp, true, 10000);
		algo_closed.printStats();

		final AlgoCharmMFI algo = new AlgoCharmMFI();
		final Itemsets patterns = algo.runAlgorithm(saveFile, closed_patterns);
		algo.printStats(database.size());

		return toMap(patterns);
	}

	/** Convert frequent itemsets to sorted Map<Itemset, Integer> */
	public static SortedMap<Itemset, Integer> toMap(final Itemsets patterns) {
		if (patterns == null) {
			return null;
		} else {
			final HashMap<Itemset, Integer> itemsets = new HashMap<>();
			for (final List<ca.pfv.spmf.patterns.itemset_array_integers_with_tids_bitset.Itemset> level : patterns
					.getLevels()) {
				for (final ca.pfv.spmf.patterns.itemset_array_integers_with_tids_bitset.Itemset itemset : level)
					itemsets.put(new Itemset(itemset.getItems()), itemset.getAbsoluteSupport());
			}
			// Sort itemsets by support
			final Ordering<Itemset> comparator = Ordering.natural().reverse().onResultOf(Functions.forMap(itemsets))
					.compound(Ordering.usingToString());
			return ImmutableSortedMap.copyOf(itemsets, comparator);
		}
	}

}
