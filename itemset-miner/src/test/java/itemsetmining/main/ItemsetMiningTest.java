package itemsetmining.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.InferenceAlgorithms.InferPrimalDual;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.Transaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Sets;

public class ItemsetMiningTest {

	@Test
	public void testDoInference() {

		final Itemset s1 = new Itemset(1);
		final Itemset s2 = new Itemset(2);
		final Itemset s3 = new Itemset(3);
		final Itemset s4 = new Itemset(4);
		final double p1 = 0.2;
		final double p2 = 0.2;
		final double p3 = 0.4;
		final double p4 = 0.4;

		final Itemset s12 = new Itemset(1, 2);
		final Itemset s23 = new Itemset(2, 3);
		final Itemset s24 = new Itemset(2, 4);
		final Itemset s34 = new Itemset(3, 4);
		final double p12 = 0.4;
		final double p23 = 0.3;
		final double p24 = 0.2;
		final double p34 = 0.4;

		// Transaction #1
		final Transaction transaction1234 = new Transaction(1, 2, 3, 4);
		transaction1234.initializeCachedItemsets(HashMultiset.create(), 0);
		transaction1234.addItemsetCache(s1, p1);
		transaction1234.addItemsetCache(s2, p2);
		transaction1234.addItemsetCache(s3, p3);
		transaction1234.addItemsetCache(s4, p4);
		transaction1234.addItemsetCache(s12, p12);
		transaction1234.addItemsetCache(s23, p23);
		transaction1234.addItemsetCache(s24, p24);
		transaction1234.addItemsetCache(s34, p34);

		final HashMap<Itemset, Double> itemsets = new HashMap<>(
				transaction1234.getCachedItemsets());

		// Expected solution #1
		double expectedCost1234 = -Math.log(p12) - Math.log(p34);
		final Set<Itemset> expected1234 = Sets.newHashSet(s12, s34);
		for (final Itemset set : Sets.difference(itemsets.keySet(),
				expected1234)) {
			expectedCost1234 += -Math.log(1 - itemsets.get(set));
		}

		// Transaction #2
		final Transaction transaction234 = new Transaction(2, 3, 4);
		transaction234.initializeCachedItemsets(HashMultiset.create(), 0);
		transaction234.addItemsetCache(s2, p2);
		transaction234.addItemsetCache(s3, p3);
		transaction234.addItemsetCache(s4, p4);
		transaction234.addItemsetCache(s23, p23);
		transaction234.addItemsetCache(s24, p24);
		transaction234.addItemsetCache(s34, p34);

		// Expected solution #2
		double expectedCost234 = -1 * Math.log(p23) - Math.log(p34);
		final Set<Itemset> expected234 = Sets.newHashSet(s23, s34);
		for (final Itemset set : Sets
				.difference(itemsets.keySet(), expected234)) {
			if (!(set.equals(s1) || set.equals(s12)))
				expectedCost234 += -Math.log(1 - itemsets.get(set));
		}

		// Test greedy
		final InferenceAlgorithm inferGreedy = new InferGreedy();
		final HashSet<Itemset> actual1234 = inferGreedy.infer(transaction1234);
		assertEquals(expected1234, actual1234);
		transaction1234.setCachedCovering(actual1234);
		assertEquals(expectedCost1234, transaction1234.getCachedCost(), 1e-15);

		final HashSet<Itemset> actual234 = inferGreedy.infer(transaction234);
		assertEquals(expected234, actual234);
		transaction234.setCachedCovering(actual234);
		assertEquals(expectedCost234, transaction234.getCachedCost(), 1e-15);

		// Test primal-dual (only gives rough approximation)
		final InferenceAlgorithm inferPrimalDual = new InferPrimalDual();
		final HashSet<Itemset> actual1234p = inferPrimalDual
				.infer(transaction1234);
		final Set<Integer> actualItems1234 = new HashSet<>();
		for (final Itemset set : actual1234p)
			actualItems1234.addAll(set);
		assertTrue(actualItems1234.containsAll(transaction1234));

		final HashSet<Itemset> actual234p = inferPrimalDual
				.infer(transaction234);
		final Set<Integer> actualItems234 = new HashSet<>();
		for (final Itemset set : actual234p)
			actualItems234.addAll(set);
		assertTrue(actualItems234.containsAll(transaction234));

	}

	// @Test
	// public void testCombLoop() {
	//
	// final int len = 10;
	// for (int k = 0; k < 2 * len - 2; k++) {
	// System.out.println("\nStep " + k);
	//
	// for (int i = 0; i < len && i < k + 1; i++) {
	// System.out.println();
	// for (int j = i + 1; j < len && i + j < k + 1; j++) {
	// if (k <= i + j)
	// System.out.println(i + " " + j);
	// }
	// }
	//
	// }
	//
	// }

}
