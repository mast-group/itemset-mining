package itemsetmining.eval;

import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.ItemsetTree;
import itemsetmining.itemset.Rule;
import itemsetmining.main.ItemsetMining;
import itemsetmining.main.ItemsetMiningCore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AlgoAgrawalFaster94;
import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AssocRules;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

public class BackgroundAssociationRules {

	private static final int topN = 100;
	private static final String baseDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/";

	public static void main(final String[] args) throws IOException {

		final String dataset = "plants";
		final String IIMlog = "plants-20.10.2014-11:12:45.log";
		final String FIMlog = "plants.txt";

		System.out.println("===== Dataset: " + dataset);

		// Build itemset tree
		final File dbFile = new File(baseDir + "Datasets/Succintly/" + dataset
				+ ".dat");
		final ItemsetTree tree = new ItemsetTree(
				ItemsetMining
						.scanDatabaseToDetermineFrequencyOfSingleItems(dbFile));
		tree.buildTree(dbFile);
		final int noTransactions = getNoTransactions(dbFile);

		// Read in interesting itemsets
		final Map<Itemset, Double> intItemsets = ItemsetMiningCore
				.readIIMItemsets(new File(baseDir + "Logs/" + IIMlog));
		System.out.println("\nIIM Itemsets\n-----------");
		System.out.println("No itemsets: " + intItemsets.size());
		System.out.println("No items: "
				+ ItemsetScaling.countNoItems(intItemsets.keySet()));

		// Generate association rules
		final double minConf = 0.1;
		final double minLift = 0.1;
		generateAssociationRules(intItemsets, tree, noTransactions, baseDir
				+ "tempRules.txt", minConf, minLift);

		// // Read in FIM Association Rules
		// final ImmutableSortedMap<Rule, Integer> freqRules =
		// (ImmutableSortedMap<Rule, Integer>) FrequentItemsetMining
		// .readAssociationRules(new File(baseDir + "FIM/Rules/" + FIMlog));
		// System.out.println("\nFIM Rules\n------------");
		// System.out.println("No rules: " + freqRules.size());
		//
		// // Calculate min lift
		// int count = 0;
		// for (final Entry<Rule, Integer> entry : freqRules.entrySet()) {
		// final Rule rule = entry.getKey();
		// final Itemset antecedent = rule.getAntecedent();
		// final Itemset consequent = rule.getConsequent();
		// System.out.println("\nRule: " + decode(antecedent) + " ==> "
		// + decode(consequent));
		// System.out.println("Lift: " + rule.getProbability());
		// final double oneLift = calculateOneLift(rule, tree, noTransactions);
		// System.out.println("One Lift: " + oneLift);
		// final double confidence = calculateConfidence(antecedent,
		// consequent, tree, noTransactions);
		// System.out.println("Confidence: " + confidence);
		// final double support = (double) rule.getProbability()
		// / noTransactions;
		// System.out.println("Support: " + support);
		// count++;
		// if (count == topN)
		// break;
		// }

	}

	// static double[] calculateAverageMinLift(
	// final Map<Itemset, Double> itemsets, final ItemsetTree tree)
	// throws IOException {
	//
	// // Filter out singletons
	// int count = 0;
	// final Map<Itemset, Double> topItemsets = new HashMap<>();
	// for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
	// if (entry.getKey().size() != 1) {
	// topItemsets.put(entry.getKey(), entry.getValue());
	// count++;
	// }
	// if (count == topN)
	// break;
	// }
	// if (count < topN)
	// System.out.println("Not enough non-singleton itemsets in set: "
	// + count);
	//
	// // Generate association rules
	// double avgMinLift = 0;
	// double avgMaxLift = 0;
	// for (final Entry<Itemset, Double> entry : topItemsets.entrySet()) {
	// System.out.println();
	// System.out.println(entry.getKey());
	// final List<Rule> rules = new ArrayList<>();
	// ItemsetMining.recursiveGenRules(rules, new Itemset(entry.getKey()),
	// new Itemset(), entry.getValue());
	// final double[] lifts = calculateMinLift(rules, tree);
	// System.out.println("Min lift: " + lifts[0]);
	// System.out.println("Max lift: " + lifts[1]);
	// avgMinLift += lifts[0];
	// avgMaxLift += lifts[1];
	// }
	// avgMinLift /= itemsets.size();
	// avgMaxLift /= itemsets.size();
	// return new double[] { avgMinLift, avgMaxLift };
	// }

	/** Calculate one lift for Association Rule */
	private static double calculateOneLift(final Rule rule,
			final ItemsetTree tree, final int noTransactions)
			throws IOException {

		double oneLift = Double.POSITIVE_INFINITY;
		final Itemset set = new Itemset(rule.getAntecedent());
		set.addAll(rule.getConsequent());

		oneLift = recursiveOneLift(oneLift, set, new Itemset(), tree,
				noTransactions);

		return oneLift;
	}

	/** Recursively calculate one lift for association rule */
	private static double recursiveOneLift(double oneLift,
			final Itemset antecedent, final Itemset consequent,
			final ItemsetTree tree, final int noTransactions) {

		// Stop if no more rules to generate
		if (antecedent.isEmpty())
			return oneLift;

		// Get lift of current rule
		if (!antecedent.isEmpty() && !consequent.isEmpty()) {
			final double lift = calculateLift(antecedent, consequent, tree,
					noTransactions);
			if (Math.abs(lift - 1) < Math.abs(oneLift - 1))
				oneLift = lift;
		}

		// Recursively generate more rules
		for (final Integer element : antecedent) {
			final Itemset newAntecedent = new Itemset(antecedent);
			newAntecedent.remove(element);
			final Itemset newConsequent = new Itemset(consequent);
			newConsequent.add(element);
			final double lift = recursiveOneLift(oneLift, newAntecedent,
					newConsequent, tree, noTransactions);
			if (Math.abs(lift - 1) < Math.abs(oneLift - 1))
				oneLift = lift;
		}

		return oneLift;
	}

	/** Calculate the lift of an association rule */
	private static double calculateLift(final Itemset antecedent,
			final Itemset consequent, final ItemsetTree tree,
			final double noTransactions) {
		final Itemset union = new Itemset(antecedent);
		union.addAll(consequent);
		final double lift = (double) (tree.getSupportOfItemset(union) / noTransactions)
				/ ((tree.getSupportOfItemset(antecedent) / noTransactions) * (tree
						.getSupportOfItemset(consequent) / noTransactions));
		return lift;
	}

	/** Calculate the confidence of an association rule */
	private static double calculateConfidence(final Itemset antecedent,
			final Itemset consequent, final ItemsetTree tree,
			final double noTransactions) {
		final Itemset union = new Itemset(antecedent);
		union.addAll(consequent);
		final double lift = (double) (tree.getSupportOfItemset(union) / noTransactions)
				/ (tree.getSupportOfItemset(antecedent) / noTransactions);
		return lift;
	}

	/** Return the size of the transaction database */
	public static int getNoTransactions(final File dbFile) throws IOException {
		final BufferedReader reader = new BufferedReader(new FileReader(dbFile));
		int lines = 0;
		while (reader.readLine() != null)
			lines++;
		reader.close();
		return lines;
	}

	public static AssocRules generateAssociationRules(
			final Map<Itemset, Double> itemsets, final ItemsetTree tree,
			final int noTransactions, final String saveFile,
			final double minConf, final double minLift) throws IOException {

		final Itemsets patterns = new Itemsets("Interesting Itemsets");
		for (final Itemset set : itemsets.keySet()) {
			final ArrayList<Integer> lset = new ArrayList<>();
			lset.addAll(set);
			final int support = tree.getSupportOfItemset(set);
			final ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset newset = new ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset(
					lset, support);
			patterns.addItemset(newset, set.size());
		}
		final AlgoAgrawalFaster94 algo = new AlgoAgrawalFaster94();
		final AssocRules rules = algo.runAlgorithm(patterns, saveFile,
				noTransactions, minConf, minLift);
		algo.printStats();
		// rules.printRulesWithLift(noTransactions);

		return rules;
	}

}
