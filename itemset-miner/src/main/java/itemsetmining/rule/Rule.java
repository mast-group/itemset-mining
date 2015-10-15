package itemsetmining.rule;

import itemsetmining.itemset.Itemset;

import java.util.Collection;

public class Rule {
	private final Itemset antecedent;
	private final Itemset consequent;
	private final double probability;

	/**
	 * Constructor
	 *
	 * @param antecedent
	 *            the antecedent of the rule (an itemset)
	 * @param consequent
	 *            the consequent of the rule (an itemset)
	 * @param probablity
	 *            probability of the rule (double)
	 */
	public Rule(final Collection<Integer> antecedent,
			final Collection<Integer> consequent, final double probability) {
		this.antecedent = new Itemset(antecedent);
		this.consequent = new Itemset(consequent);
		this.probability = probability;
	}

	/**
	 * Constructor
	 *
	 * @param antecedent
	 *            the antecedent of the rule (an itemset)
	 * @param consequent
	 *            the consequent of the rule (an itemset)
	 * @param probablity
	 *            probability of the rule (double)
	 */
	public Rule(int[] antecedent, int[] consequent, double probability) {
		this.antecedent = new Itemset(antecedent);
		this.consequent = new Itemset(consequent);
		this.probability = probability;
	}

	/**
	 * Return a String representation of this rule
	 *
	 * @return a String
	 */
	@Override
	public String toString() {
		return antecedent.toString() + " ==> " + consequent.toString()
				+ "\tprob: " + String.format("%1.5f", probability);
	}

	/**
	 * Get the left itemset of this rule (antecedent).
	 *
	 * @return an itemset.
	 */
	public Itemset getAntecedent() {
		return antecedent;
	}

	/**
	 * Get the right itemset of this rule (consequent).
	 *
	 * @return an itemset.
	 */
	public Itemset getConsequent() {
		return consequent;
	}

	/**
	 * Get the probability of this rule.
	 *
	 * @return probability.
	 */
	public double getProbability() {
		return probability;
	}

}
