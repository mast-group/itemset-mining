package itemsetmining.eval.rules;

import itemsetmining.eval.FrequentItemsetMining;
import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.ItemsetTree;
import itemsetmining.main.ItemsetMining;
import itemsetmining.rule.Rule;

import java.io.File;
import java.io.IOException;

import com.google.common.collect.ImmutableSortedMap;

public class BackgroundAssociationRules {

	private static final int topN = 100;
	private static final String baseDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/";
	private static final double LIFT_DIFF = 2.0;

	public static void main(final String[] args) throws IOException {

		final String dataset = "plants";

		System.out.println("===== Dataset: " + dataset);

		// Build itemset tree
		final File dbFile = new File(baseDir + "Datasets/Succintly/" + dataset
				+ ".dat");
		final ItemsetTree tree = new ItemsetTree(
				ItemsetMining
						.scanDatabaseToDetermineFrequencyOfSingleItems(dbFile));
		tree.buildTree(dbFile);

		// Read in IIM Association Rules
		final ImmutableSortedMap<Rule, Integer> intRules = (ImmutableSortedMap<Rule, Integer>) FrequentItemsetMining
				.readAssociationRules(new File(baseDir + "Rules/" + dataset
						+ ".txt"));
		System.out.println("\nIIM Rules\n------------");
		System.out.println("No rules: " + intRules.size());

		// Calculate redundancy
		double redundancyPerc = calculateRedundancy(tree, intRules);
		System.out.println("\nIIM Redundancy: " + redundancyPerc * 100 + "%");

		// Read in FIM Association Rules
		final ImmutableSortedMap<Rule, Integer> freqRules = (ImmutableSortedMap<Rule, Integer>) FrequentItemsetMining
				.readAssociationRules(new File(baseDir + "FIM/Rules/" + dataset
						+ ".txt"));
		System.out.println("\nFIM Rules\n------------");
		System.out.println("No rules: " + freqRules.size());

		// Calculate redundancy
		redundancyPerc = calculateRedundancy(tree, freqRules);
		System.out.println("\nFIM Redundancy: " + redundancyPerc * 100 + "%");
	}

	private static double calculateRedundancy(final ItemsetTree tree,
			final ImmutableSortedMap<Rule, Integer> intRules)
			throws IOException {
		int countRedundant = 0;
		int count = 0;
		for (final Rule rule : intRules.keySet()) {
			// final Rule rule = entry.getKey();
			// final Itemset antecedent = rule.getAntecedent();
			// final Itemset consequent = rule.getConsequent();
			System.out.println(rule);
			final double oneLift = calculateOneLift(rule, tree);
			System.out.println("Lift: " + rule.getProbability());
			System.out.println("One Lift: " + oneLift);
			// final double confidence = calculateConfidence(antecedent,
			// consequent, tree, noTransactions);
			// System.out.println("Confidence: " + confidence);
			// final double support = (double) rule.getProbability()
			// / noTransactions;
			// System.out.println("Support: " + support);
			if (isRedundant(rule, oneLift))
				countRedundant++;
			count++;
			if (count == topN)
				break;
		}
		if (count < topN)
			System.out.println("Not enough rules in set: " + count);
		return (double) countRedundant / count;
	}

	private static boolean isRedundant(final Rule rule, final double oneLift) {
		final double lift = rule.getProbability();
		if (Math.abs(lift - oneLift) > LIFT_DIFF)
			return true;
		return false;
	}

	/** Calculate one lift for Association Rule */
	private static double calculateOneLift(final Rule rule,
			final ItemsetTree tree) throws IOException {

		double oneLift = Double.POSITIVE_INFINITY;
		final Itemset set = new Itemset(rule.getAntecedent());
		set.addAll(rule.getConsequent());

		oneLift = recursiveOneLift(oneLift, set, new Itemset(), tree);

		return oneLift;
	}

	/** Recursively calculate one lift for association rule */
	private static double recursiveOneLift(double oneLift,
			final Itemset antecedent, final Itemset consequent,
			final ItemsetTree tree) {

		// Stop if no more rules to generate
		if (antecedent.isEmpty())
			return oneLift;

		// Get lift of current rule
		if (!antecedent.isEmpty() && !consequent.isEmpty()) {
			final double lift = calculateLift(antecedent, consequent, tree);
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
					newConsequent, tree);
			if (Math.abs(lift - 1) < Math.abs(oneLift - 1))
				oneLift = lift;
		}

		return oneLift;
	}

	/** Calculate the lift of an association rule */
	private static double calculateLift(final Itemset antecedent,
			final Itemset consequent, final ItemsetTree tree) {
		final Itemset union = new Itemset(antecedent);
		union.addAll(consequent);
		final double lift = (double) tree.getRelativeSupportOfItemset(union)
				/ (tree.getRelativeSupportOfItemset(antecedent) * tree
						.getRelativeSupportOfItemset(consequent));
		return lift;
	}

	/** Calculate the confidence of an association rule */
	@SuppressWarnings("unused")
	private static double calculateConfidence(final Itemset antecedent,
			final Itemset consequent, final ItemsetTree tree) {
		final Itemset union = new Itemset(antecedent);
		union.addAll(consequent);
		final double lift = (double) tree.getRelativeSupportOfItemset(union)
				/ tree.getRelativeSupportOfItemset(antecedent);
		return lift;
	}

}
