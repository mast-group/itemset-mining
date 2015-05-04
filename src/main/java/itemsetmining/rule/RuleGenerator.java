package itemsetmining.rule;

import itemsetmining.eval.ItemsetScaling;
import itemsetmining.itemset.ItemsetTree;
import itemsetmining.main.ItemsetMining;
import itemsetmining.main.ItemsetMiningCore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import ca.pfv.spmf.algorithms.associationrules.agrawal94_association_rules.AssocRules;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

public class RuleGenerator {

	public static void main(final String[] args) throws IOException {

		// Generator parameters
		final String dataset = "plants";
		final String IIMlog = "plants-20.10.2014-11:12:45.log";
		final double minConf = 0.8;
		final double minLift = 0.8;
		String baseDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/";

		// Read in interesting itemsets
		final Map<itemsetmining.itemset.Itemset, Double> intItemsets = ItemsetMiningCore
				.readIIMItemsets(new File(baseDir + "Logs/" + IIMlog));
		System.out.println("\nIIM Itemsets\n-----------");
		System.out.println("No itemsets: " + intItemsets.size());
		System.out.println("No items: "
				+ ItemsetScaling.countNoItems(intItemsets.keySet()));

		// Build itemset tree
		final File dbFile = new File(baseDir + "Datasets/Succintly/" + dataset
				+ ".dat");
		final ItemsetTree tree = new ItemsetTree(
				ItemsetMining
						.scanDatabaseToDetermineFrequencyOfSingleItems(dbFile));
		tree.buildTree(dbFile);

		// Generate IIM association rules
		generateAssociationRules(intItemsets, tree, baseDir + "Rules/"
				+ dataset + ".txt", minConf, minLift);
	}

	public static AssocRules generateAssociationRules(
			final Map<itemsetmining.itemset.Itemset, Double> itemsets,
			final ItemsetTree tree, final String saveFile,
			final double minConf, final double minLift) throws IOException {

		final Itemsets patterns = new Itemsets("Interesting Itemsets");
		for (final itemsetmining.itemset.Itemset set : itemsets.keySet()) {
			final ArrayList<Integer> lset = new ArrayList<>();
			lset.addAll(set);
			final int support = tree.getSupportOfItemset(set);
			final ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset newset = new ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset(
					lset, support);
			patterns.addItemset(newset, set.size());
		}
		final AlgoAgrawal algo = new AlgoAgrawal();
		final AssocRules rules = algo.runAlgorithm(patterns, saveFile, tree,
				minConf, minLift);
		algo.printStats();
		// rules.printRulesWithLift(noTransactions);

		return rules;
	}

}
