package itemsetmining.eval;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Sets;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.ItemsetMiningCore;

public class TopItemsets {

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

		final Set<Itemset> IIMnotMTVorKRIMP = Sets
				.difference(Sets.difference(intItemsets.keySet(), mtvItemsets.keySet()), krimpItemsets.keySet());
		final Set<Itemset> MTVnotIIMorKRIMP = Sets
				.difference(Sets.difference(mtvItemsets.keySet(), intItemsets.keySet()), krimpItemsets.keySet());
		final Set<Itemset> KRIMPnotIIMorMTV = Sets
				.difference(Sets.difference(krimpItemsets.keySet(), intItemsets.keySet()), mtvItemsets.keySet());

		final List<String> dict = FileUtils.readLines(new File(baseDir + "Datasets/Abstracts/abstracts.dictionary"));

		// Print top ten
		int count = 0;
		System.out.print("\n============= IIM not MTV/KRIMP =============\n");
		for (final Entry<Itemset, Double> entry : intItemsets.entrySet()) {
			final Itemset set = entry.getKey();
			if (set.size() > 1 && IIMnotMTVorKRIMP.contains(set)) {
				System.out.print(String.format("%s\tprob: %1.5f \tint: %1.5f %n", decode(entry.getKey(), dict),
						entry.getValue(), Double.NaN));
				count++;
				if (count == topN)
					break;
			}
		}
		System.out.println();

		// Print top ten
		count = 0;
		System.out.print("\n============= MTV not IIM/KRIMP =============\n");
		for (final Entry<Itemset, Double> entry : intItemsets.entrySet()) {
			final Itemset set = entry.getKey();
			if (set.size() > 1 && MTVnotIIMorKRIMP.contains(set)) {
				System.out.print(String.format("%s\tprob: %1.5f \tint: %1.5f %n", decode(entry.getKey(), dict),
						entry.getValue(), Double.NaN));
				count++;
				if (count == topN)
					break;
			}
		}
		System.out.println();

		// Print top ten
		count = 0;
		System.out.print("\n============= KRIMP not IIM/MTV =============\n");
		for (final Entry<Itemset, Double> entry : intItemsets.entrySet()) {
			final Itemset set = entry.getKey();
			if (set.size() > 1 && KRIMPnotIIMorMTV.contains(set)) {
				System.out.print(String.format("%s\tprob: %1.5f \tint: %1.5f %n", decode(entry.getKey(), dict),
						entry.getValue(), Double.NaN));
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
