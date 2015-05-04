package itemsetmining.eval;

import itemsetmining.itemset.Itemset;
import itemsetmining.rule.Rule;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AlgoAgrawalFaster94;
import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AssocRules;
import ca.pfv.spmf.algorithms.frequentpatterns.apriori.AlgoApriori;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;
import ca.pfv.spmf.tools.other_dataset_tools.FixTransactionDatabaseTool;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;

public class FrequentItemsetMining {

	private static final String TMPDB = "/tmp/fixed-dataset.dat";
	private static final FixTransactionDatabaseTool dbTool = new FixTransactionDatabaseTool();

	public static void main(final String[] args) throws IOException {

		// FIM parameters
		final String dataset = "abstracts";
		final double minSupp = 0.06; // relative support
		final double minConf = 0.1;
		final double minLift = 0.1;
		final String dbPath = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Datasets/Succintly/"
				+ dataset + ".dat";
		final String saveFile = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/FIM/Rules/"
				+ dataset + ".txt";

		// mineFrequentItemsetsFPGrowth(dbPath, saveFile, minSupp);
		mineAssociationRulesFPGrowth(dbPath, saveFile, minSupp, minConf,
				minLift);

	}

	/** Run FPGrowth algorithm */
	public static SortedMap<Itemset, Integer> mineFrequentItemsetsFPGrowth(
			final String dataset, final String saveFile, final double minSupp)
			throws IOException {

		// Remove transaction duplicates and sort items ascending
		dbTool.convert(dataset, TMPDB);

		final AlgoFPGrowth algo = new AlgoFPGrowth();
		final Itemsets patterns = algo.runAlgorithm(TMPDB, saveFile, minSupp);
		algo.printStats();
		// patterns.printItemsets(algo.getDatabaseSize());

		return toMap(patterns);
	}

	/** Run Apriori algorithm */
	public static SortedMap<Itemset, Integer> mineFrequentItemsetsApriori(
			final String dataset, final String saveFile, final double minSupp)
			throws IOException {

		// Remove transaction duplicates and sort items ascending
		dbTool.convert(dataset, TMPDB);

		final AlgoApriori algo = new AlgoApriori();
		final Itemsets patterns = algo.runAlgorithm(minSupp, TMPDB, saveFile);
		// algo.printStats();
		// patterns.printItemsets(algo.getDatabaseSize());

		return toMap(patterns);
	}

	/** Mine association rules from FIM itemsets using FPGrowth */
	public static AssocRules mineAssociationRulesFPGrowth(final String dataset,
			final String saveFile, final double minSupp, final double minConf,
			final double minLift) throws IOException {

		// Remove transaction duplicates and sort items ascending
		dbTool.convert(dataset, TMPDB);

		final AlgoFPGrowth algo = new AlgoFPGrowth();
		final Itemsets patterns = algo.runAlgorithm(TMPDB, null, minSupp);
		algo.printStats();
		// patterns.printItemsets(algo.getDatabaseSize());

		final AlgoAgrawalFaster94 algo2 = new AlgoAgrawalFaster94();
		final AssocRules rules = algo2.runAlgorithm(patterns, saveFile,
				algo.getDatabaseSize(), minConf, minLift);
		algo2.printStats();
		// rules.printRulesWithLift(algo.getDatabaseSize());

		return rules;
	}

	/** Convert frequent itemsets to sorted Map<Itemset, Integer> */
	public static SortedMap<Itemset, Integer> toMap(final Itemsets patterns) {
		if (patterns == null) {
			return null;
		} else {
			final HashMap<Itemset, Integer> itemsets = new HashMap<>();
			for (final List<ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset> level : patterns
					.getLevels()) {
				for (final ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset itemset : level)
					itemsets.put(new Itemset(itemset.getItems()),
							itemset.getAbsoluteSupport());
			}
			// Sort itemsets by support
			final Ordering<Itemset> comparator = Ordering.natural().reverse()
					.onResultOf(Functions.forMap(itemsets))
					.compound(Ordering.usingToString());
			return ImmutableSortedMap.copyOf(itemsets, comparator);
		}
	}

	/** Read in frequent itemsets */
	public static SortedMap<Itemset, Integer> readFrequentItemsets(
			final File output) throws IOException {
		final HashMap<Itemset, Integer> itemsets = new HashMap<>();

		final LineIterator it = FileUtils.lineIterator(output);
		while (it.hasNext()) {
			final String line = it.nextLine();
			if (!line.trim().isEmpty()) {
				final String[] splitLine = line.split("#SUP:");
				final String[] items = splitLine[0].split(" ");
				final Itemset itemset = new Itemset();
				for (final String item : items)
					itemset.add(Integer.parseInt(item.trim()));
				final int supp = Integer.parseInt(splitLine[1].trim());
				itemsets.put(itemset, supp);
			}
		}
		// Sort itemsets by support
		final Ordering<Itemset> comparator = Ordering.natural().reverse()
				.onResultOf(Functions.forMap(itemsets))
				.compound(Ordering.usingToString());
		return ImmutableSortedMap.copyOf(itemsets, comparator);
	}

	/** Read in association rules (sorted by lift) */
	public static SortedMap<Rule, Integer> readAssociationRules(
			final File output) throws IOException {
		final HashMap<Rule, Integer> rules = new HashMap<>();

		final LineIterator it = FileUtils.lineIterator(output);
		while (it.hasNext()) {
			final String line = it.nextLine();
			if (!line.trim().isEmpty()) {
				String[] splitLine = line.split(" #SUP: ");
				final String[] rule = splitLine[0].split(" ==> ");
				final Itemset antecedent = new Itemset();
				for (final String item : rule[0].split(" "))
					antecedent.add(Integer.parseInt(item));
				final Itemset consequent = new Itemset();
				for (final String item : rule[1].split(" "))
					consequent.add(Integer.parseInt(item));
				splitLine = splitLine[1].split(" #CONF: ");
				final int supp = Integer.parseInt(splitLine[0]);
				splitLine = splitLine[1].split(" #LIFT: ");
				// final double conf = Double.parseDouble(splitLine[0]);
				final double lift = Double.parseDouble(splitLine[1].trim());
				rules.put(new Rule(antecedent, consequent, lift), supp);
			}
		}
		// Sort rules by lift
		final Ordering<Rule> comparator = new Ordering<Rule>() {
			@Override
			public int compare(final Rule rule1, final Rule rule2) {
				return -Double.compare(rule1.getProbability(),
						rule2.getProbability());
			}
		}.compound(Ordering.usingToString());
		return ImmutableSortedMap.copyOf(rules, comparator);
	}

}
