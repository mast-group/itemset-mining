package itemsetmining.eval.rules;

import itemsetmining.eval.FrequentItemsetMining;
import itemsetmining.itemset.Itemset;
import itemsetmining.itemset.ItemsetTree;
import itemsetmining.main.ItemsetMining;
import itemsetmining.rule.Rule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.ImmutableSortedMap;

public class AbstractsAssociationRules {

	private static final int topN = 10;
	private static final String baseDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/";
	private static String[] dict;

	public static void main(final String[] args) throws IOException {

		final String dataset = "abstracts";
		final String FIMlog = "abstracts.txt";

		System.out.println("===== Dataset: " + dataset);

		// Read in dictionary
		dict = FileUtils.readFileToString(
				new File(baseDir + "/Datasets/Abstracts/abstracts.dictionary"))
				.split("\n");

		// Build itemset tree
		final File dbFile = new File(baseDir + "Datasets/Succintly/" + dataset
				+ ".dat");
		final ItemsetTree tree = new ItemsetTree(
				ItemsetMining
						.scanDatabaseToDetermineFrequencyOfSingleItems(dbFile));
		tree.buildTree(dbFile);
		final int noTransactions = getNoTransactions(dbFile);

		// Read in FIM Association Rules
		final ImmutableSortedMap<Rule, Integer> freqRules = (ImmutableSortedMap<Rule, Integer>) FrequentItemsetMining
				.readAssociationRules(new File(baseDir + "FIM/Rules/" + FIMlog));
		System.out.println("\nFIM Rules\n------------");
		System.out.println("No rules: " + freqRules.size());

		// Calculate min lift
		int count = 0;
		for (final Entry<Rule, Integer> entry : freqRules.entrySet()) {
			final Rule rule = entry.getKey();
			final Itemset antecedent = rule.getAntecedent();
			final Itemset consequent = rule.getConsequent();
			System.out.println("\nRule: " + decode(antecedent) + " ==> "
					+ decode(consequent));
			System.out.println("Lift: " + rule.getProbability());
			final double oneLift = calculateOneLift(rule, tree, noTransactions);
			System.out.println("One Lift: " + oneLift);
			final double confidence = calculateConfidence(antecedent,
					consequent, tree, noTransactions);
			System.out.println("Confidence: " + confidence);
			final double support = (double) rule.getProbability()
					/ noTransactions;
			System.out.println("Support: " + support);
			count++;
			if (count == topN)
				break;
		}

	}

	private static String decode(final Itemset set) {
		final StringBuilder sb = new StringBuilder(set.size() * 2);
		sb.append("{");
		String prefix = "";
		for (final int item : set) {
			sb.append(prefix + dict[item]);
			prefix = ", ";
		}
		sb.append("}");
		return sb.toString();
	}

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
		System.out.println("+++++ Rule: " + decode(antecedent) + " == > "
				+ decode(consequent) + " Lift: " + lift);
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

}
