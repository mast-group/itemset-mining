package itemsetmining.eval;

import static java.util.function.Function.identity;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;

import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.ItemsetTree;
import itemsetmining.main.ItemsetMining;
import itemsetmining.main.ItemsetMiningCore;

public class ItemsetSymmetricDistance {

	private static int topN = 100;
	private static final String baseDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/";

	public static void main(final String[] args) throws IOException {

		// FIXME add uganda IIM and mammals KRIMP
		final String[] IIMlogs = new String[] { "IIM-plants-23.09.2015-06:45:55.log",
				"IIM-mammals-23.09.2015-07:57:39.log", "IIM-abstracts-18.09.2015-15:14:13.log", 
				"IIM-uganda-29.09.2015-11:01:19.log"};
		final String[] KRIMPlogs = new String[] { "plants_itemsets.txt",
				"mammals_itemsets.txt",
				"abstracts_itemsets.txt", "uganda_itemsets.txt" };
		final String[] FIMlogs = new String[] { "plants-closed-fim.txt", "mammals-closed-fim.txt",
				"abstracts-closed-fim.txt", "uganda-closed-fim.txt" };
		final String[] datasets = new String[] { "plants", "mammals", "abstracts", "uganda" };

		for (int i = 0; i < IIMlogs.length; i++) {

			System.out.println("===== Dataset: " + FIMlogs[i].substring(0, FIMlogs[i].lastIndexOf('.')));

			// Read in interesting itemsets
			final Map<Itemset, Double> intItemsets = ItemsetMiningCore
					.readIIMItemsets(new File(baseDir + "Logs/" + IIMlogs[i]));
			System.out.println("\nIIM Itemsets\n-----------");
			System.out.println("No itemsets: " + intItemsets.size());
			System.out.println("No items: " + ItemsetScaling.countNoItems(intItemsets.keySet()));

			// Measure symmetric difference between the two sets of itemsets
			final Set<Itemset> topIntItemsets = filterSingletons(intItemsets);
			double avgMinDiff = calculateRedundancy(topIntItemsets);
			System.out.println("\nAvg min sym diff: " + avgMinDiff);

			// Calculate spuriousness
			double avgMaxSpur = calculateSpuriousness(topIntItemsets);
			System.out.println("Avg no. subsets: " + avgMaxSpur);
			

			// Read in KRIMP itemsets
			final LinkedHashMap<Itemset, Double> krimpItemsets = StatisticalItemsetMining
					.readKRIMPItemsets(new File(baseDir + "KRIMP/" + KRIMPlogs[i]));
			System.out.println("\nKRIMP Itemsets\n------------");
			System.out.println("No itemsets: " + krimpItemsets.size());
			System.out.println("No items: " + ItemsetScaling.countNoItems(krimpItemsets.keySet()));

			// Measure symmetric difference between the two sets of itemsets
			final Set<Itemset> topKrimpItemsets = filterSingletons(krimpItemsets);
			avgMinDiff = calculateRedundancy(topKrimpItemsets);
			System.out.println("\nAvg min sym diff: " + avgMinDiff);

			// Calculate spuriousness
			avgMaxSpur = calculateSpuriousness(topKrimpItemsets);
			System.out.println("Avg no. subsets: " + avgMaxSpur);


			// Read in frequent itemsets
			final SortedMap<Itemset, Integer> freqItemsets = FrequentItemsetMining
					.readFrequentItemsets(new File(baseDir + "FIM/" + FIMlogs[i]));
			System.out.println("\nFIM Itemsets\n------------");
			System.out.println("No itemsets: " + freqItemsets.size());
			System.out.println("No items: " + ItemsetScaling.countNoItems(freqItemsets.keySet()));

			// Get top 100K
			topN = 100_000;
			final Set<Itemset> top100KFreqItemsets = filterSingletons(freqItemsets);

			// Build itemset tree
			final File inputFile = new File(baseDir + "Datasets/Succintly/" + datasets[i] + ".dat");
			final Multiset<Integer> singletons = ItemsetMining.scanDatabaseToDetermineFrequencyOfSingleItems(inputFile);
			final ItemsetTree tree = new ItemsetTree(singletons);
			tree.buildTree(inputFile);

			// Parallel chi-squared calculation (yes it's that slow)
			final Map<Itemset, Double> itemsetsMap = top100KFreqItemsets.parallelStream()
					.collect(Collectors.toMap(identity(), it -> tree.getChiSquaredOfItemset(it, singletons)));

			// Sort itemsets by chi-squared
			final Ordering<Itemset> comparator = Ordering.natural().reverse().onResultOf(Functions.forMap(itemsetsMap))
					.compound(Ordering.usingToString());
			final Map<Itemset, Double> freqItemsetsChiSquared = ImmutableSortedMap.copyOf(itemsetsMap, comparator);

			// Measure symmetric difference between the two sets of itemsets
			topN = 100;
			final Set<Itemset> topFreqItemsets = filterSingletons(freqItemsetsChiSquared);
			avgMinDiff = calculateRedundancy(topFreqItemsets);
			System.out.println("\nAvg min sym diff: " + avgMinDiff);

			// Calculate spuriousness
			avgMaxSpur = calculateSpuriousness(topFreqItemsets);
			System.out.println("Avg no. subsets: " + avgMaxSpur);

			System.out.println();

		}

	}

	private static double calculateRedundancy(final Set<Itemset> topItemsets) {

		double avgMinDiff = 0;
		for (final Itemset set1 : topItemsets) {

			int minDiff = Integer.MAX_VALUE;
			for (final Itemset set2 : topItemsets) {
				if (!set1.equals(set2)) {
					final int diff = cardSymDiff(set1, set2);
					if (diff < minDiff)
						minDiff = diff;
				}
			}
			avgMinDiff += minDiff;
		}
		avgMinDiff /= topItemsets.size();

		return avgMinDiff;
	}

	private static <T> int cardSymDiff(final Collection<T> set1, final Collection<T> set2) {
		final int sizeUnion = CollectionUtils.union(set1, set2).size();
		final int sizeIntersection = CollectionUtils.intersection(set1, set2).size();
		return (sizeUnion - sizeIntersection);
	}

	private static double calculateSpuriousness(final Set<Itemset> topItemsets) {

		double avgSubseq = 0;
		for (final Itemset set1 : topItemsets) {
			for (final Itemset set2 : topItemsets) {
				if (!set1.equals(set2))
					avgSubseq += isSubset(set1, set2);
			}
		}
		avgSubseq /= topItemsets.size();

		return avgSubseq;
	}

	private static int isSubset(final Itemset set1, final Itemset set2) {
		if (set2.contains(set1))
			return 1;
		return 0;
	}

	/** Filter out singletons */
	private static <V> Set<Itemset> filterSingletons(final Map<Itemset, V> itemsets) {

		int count = 0;
		final Set<Itemset> topItemsets = new HashSet<>();
		for (final Itemset set : itemsets.keySet()) {
			if (set.size() != 1) {
				topItemsets.add(set);
				count++;
			}
			if (count == topN)
				break;
		}
		if (count < 100)
			System.out.println("Not enough non-singleton sequences in set: " + count);

		return topItemsets;
	}

}