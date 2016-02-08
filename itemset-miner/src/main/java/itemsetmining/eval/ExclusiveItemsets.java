package itemsetmining.eval;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.ItemsetMiningCore;

public class ExclusiveItemsets {

	private static final int topN = 10;
	private static final String baseDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/";

	public static void main(final String[] args) throws IOException {

		final String IIMlog = "IIM-abstracts-18.09.2015-15:14:13.log";
		final String KRIMPlog = "abstracts_itemsets.txt";
		final String MTVlog = "abstracts.txt";

		final Map<Itemset, Double> intItemsets = ItemsetMiningCore
				.readIIMItemsets(new File(baseDir + "Logs/" + IIMlog));
		final LinkedHashMap<Itemset, Double> krimpItemsets = StatisticalItemsetMining
				.readKRIMPItemsets(new File(baseDir + "KRIMP/" + KRIMPlog));
		final LinkedHashMap<Itemset, Double> mtvItemsets = StatisticalItemsetMining
				.readMTVItemsets(new File(baseDir + "MTV/" + MTVlog));

		final Set<Itemset> IIMnotMTVorKRIMP = getExclusiveItemsets(intItemsets.keySet(), mtvItemsets.keySet(),
				krimpItemsets.keySet());
		final Set<Itemset> MTVnotIIMorKRIMP = getExclusiveItemsets(mtvItemsets.keySet(), intItemsets.keySet(),
				krimpItemsets.keySet());
		final Set<Itemset> KRIMPnotIIMorMTV = getExclusiveItemsets(krimpItemsets.keySet(), intItemsets.keySet(),
				mtvItemsets.keySet());

		final List<String> dict = FileUtils.readLines(new File(baseDir + "Datasets/Abstracts/abstracts.dictionary"));

		// Print top ten
		System.out.print("\n============= IIM not MTV/KRIMP =============\n");
		printTopExclusiveItemsets(intItemsets, IIMnotMTVorKRIMP, dict);
		System.out.print("\n============= MTV not IIM/KRIMP =============\n");
		printTopExclusiveItemsets(mtvItemsets, MTVnotIIMorKRIMP, dict);
		System.out.print("\n============= KRIMP not IIM/MTV =============\n");
		printTopExclusiveItemsets(krimpItemsets, KRIMPnotIIMorMTV, dict);

	}

	/**
	 * Set A \ B u C
	 * <p>
	 * Note: slow but Guava contains/Set.difference doesn't work here
	 */
	private static Set<Itemset> getExclusiveItemsets(final Set<Itemset> setA, final Set<Itemset> setB,
			final Set<Itemset> setC) {
		final Set<Itemset> exclItemsets = new HashSet<>();
		outer: for (final Itemset itemsetA : setA) {
			for (final Itemset itemsetB : setB) {
				if (itemsetA.equals(itemsetB))
					continue outer;
			}
			for (final Itemset itemsetC : setC) {
				if (itemsetA.equals(itemsetC))
					continue outer;
			}
			exclItemsets.add(itemsetA);
		}
		return exclItemsets;
	}

	private static void printTopExclusiveItemsets(final Map<Itemset, Double> itemsets,
			final Set<Itemset> exclusiveItemsets, final List<String> dict) {
		int count = 0;
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			final Itemset set = entry.getKey();
			if (set.size() > 1 && exclusiveItemsets.contains(set)) {
				System.out.print(String.format("%s\tprob: %1.5f %n", decode(entry.getKey(), dict), entry.getValue()));
				count++;
				if (count == topN)
					break;
			}
		}
		System.out.println();
	}

	private static String decode(final Itemset set, final List<String> dict) {
		String prefix = "";
		final StringBuilder sb = new StringBuilder();
		for (final Integer item : set) {
			sb.append(prefix);
			sb.append(dict.get(item));
			prefix = ", ";
		}
		return sb.toString();
	}

}
