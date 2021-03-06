package itemsetmining.itemset;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.google.common.collect.Multiset;

import itemsetmining.util.MemoryLogger;

/**
 * This is a modified implementation of the Memory Efficient Itemset-tree as
 * proposed in:
 *
 * Fournier-Viger, P., Mwamikazi, E., Gueniche, T., Faghihi, U. (2013). Memory
 * Efficient Itemset Tree for Targeted Association Rule Mining. Proc. 9th
 * International Conference on Advanced Data Mining and Applications (ADMA 2013)
 * Part II, Springer LNAI 8347, pp. 95-106.
 *
 * This file is adapted from the SPMF DATA MINING SOFTWARE
 * (http://www.philippe-fournier-viger.com/spmf). Copyright (c) 2013 Philippe
 * Fournier-Viger
 *
 * SPMF is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * SPMF is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SPMF. If not, see <http://www.gnu.org/licenses/>.
 */
public class ItemsetTree {

	// root of the itemset tree
	private ItemsetTreeNode root = null;

	// items with their supports (for ordering items in the tree)
	private final Multiset<Integer> items;

	// number of transactions in database used to build this tree
	private int noTransactions = -1;

	// / Comparator for ordering items by descending order of support
	private final Comparator<Integer> itemComparator = new Comparator<Integer>() {
		@Override
		public int compare(final Integer item1, final Integer item2) {
			// compare the frequency
			final int compare = items.count(item2) - items.count(item1);
			// if the same frequency, we check the lexical ordering!
			if (compare == 0) {
				return (item1 - item2);
			}
			// otherwise, just use the frequency
			return compare;
		}
	};

	// statistics about tree construction
	int nodeCount; // number of nodes in the tree (recalculated by
					// printStatistics() )
	long totalItemCountInNodes; // total number of items stored in nodes
								// (recalculated by printStatistics()
	long startTimestamp; // start time of tree construction (buildTree())
	long endTimestamp; // end time of tree contruction (buildTree())
	long sumBranchesLength; // sum of branches length
	int totalNumberOfBranches; // total number of branches

	/**
	 * Default constructor
	 */
	public ItemsetTree(final Multiset<Integer> singletons) {
		items = singletons;
	}

	/**
	 * Random walk on tree. Uses support-weighted random walk.
	 */
	public Itemset randomWalk() {

		final Itemset set = new Itemset();
		traverse(root, set);

		return set;
	}

	/**
	 * Traverse this tree in a random walk
	 */
	public void traverse(final ItemsetTreeNode node, final Itemset itemset) {

		// Add node's itemset elements with probability 0.5
		if (!node.equals(root)) { // root node is empty
			for (final int item : node.itemset) {
				if (Math.random() < 0.5)
					itemset.add(item);
			}
		}

		// Stop if leaf node
		if (node.children.isEmpty())
			return;

		// Get support of all children
		double sumSupport = 0;
		final HashMap<ItemsetTreeNode, Integer> supports = new HashMap<>();
		for (final ItemsetTreeNode child : node.children) {
			supports.put(child, child.support);
			sumSupport += child.support;
		}

		// Stop with probability dependent on total support of children
		final double pStop = (node.support - sumSupport) / node.support;
		if (Math.random() < pStop)
			return;

		// Randomly pick child to traverse proportional to its itemset support
		double p = Math.random();
		ItemsetTreeNode child = null;
		for (final Map.Entry<ItemsetTreeNode, Integer> entry : supports.entrySet()) {

			// final double childProb = 1. / node.children.size();
			final double childProb = entry.getValue() / sumSupport;
			if (p < childProb) {
				child = entry.getKey();
			} else {
				p -= childProb;
			}
		}
		assert child != null;

		traverse(child, itemset);
	}

	/**
	 * Build the itemset-tree based on an input file containing transactions
	 *
	 * @param input
	 *            an input file
	 * @return
	 */
	public void buildTree(final File inputFile) throws IOException {
		// record start time
		startTimestamp = System.currentTimeMillis();

		// reset memory usage statistics
		MemoryLogger.getInstance().reset();

		// create an empty root for the tree
		root = new ItemsetTreeNode(null, 0);

		// Scan the database to read the transactions
		int count = 0;
		final LineIterator it = FileUtils.lineIterator(inputFile, "UTF-8");
		while (it.hasNext()) {

			final String line = it.nextLine();
			// if the line is a comment, is empty or is a
			// kind of metadata
			if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}

			// add transaction to the tree
			addTransaction(line);
			count++;
		}
		// close the input file
		LineIterator.closeQuietly(it);

		// set the number of transactions
		noTransactions = count;

		// check the memory usage
		MemoryLogger.getInstance().checkMemory();
		endTimestamp = System.currentTimeMillis();
	}

	/**
	 * Build the itemset-tree based on an HDFS input file containing
	 * transactions
	 *
	 * @param hdfsPath
	 *            HDFS input path string
	 * @param hdfs
	 *            HDFS FileSystem
	 * @return
	 */
	public void buildTree(final String hdfsPath, final FileSystem hdfs) throws IOException {
		// record start time
		startTimestamp = System.currentTimeMillis();

		// reset memory usage statistics
		MemoryLogger.getInstance().reset();

		// create an empty root for the tree
		root = new ItemsetTreeNode(null, 0);

		// Scan the database to read the transactions
		int count = 0;
		final BufferedReader reader = new BufferedReader(new InputStreamReader(hdfs.open(new Path(hdfsPath))));
		String line;
		while ((line = reader.readLine()) != null) {

			// if the line is a comment, is empty or is a
			// kind of metadata
			if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}

			// add transaction to the tree
			addTransaction(line);
			count++;
		}
		// close the input file
		reader.close();

		// set the number of transactions
		noTransactions = count;

		// check the memory usage
		MemoryLogger.getInstance().checkMemory();
		endTimestamp = System.currentTimeMillis();
	}

	/**
	 * Add transaction to tree
	 *
	 * @param line
	 *            the transaction as a string of items
	 */
	private void addTransaction(final String line) {

		// split the transaction into items
		final String[] lineSplit = line.split(" ");
		// create a structure for storing the transaction
		final Itemset itemset = new Itemset();
		// for each item in the transaction
		for (int i = 0; i < lineSplit.length; i++) {
			// convert the item to integer and add it to the structure
			itemset.add(Integer.parseInt(lineSplit[i]));
		}

		// sort items in the itemset by descending order of support
		final int[] sortedItemset = itemset.stream().sorted(itemComparator).mapToInt(i -> i).toArray();

		// call the method "construct" to add the transaction to the tree
		construct(null, root, sortedItemset, null);
	}

	/**
	 * Given the root of a sub-tree, add an itemset at the proper position in
	 * that tree
	 *
	 * @param r
	 *            the root of the sub-tree
	 * @param s
	 *            the itemset to be inserted
	 * @param prefix
	 *            the current item(s) explored in this branch of the tree until
	 *            the current node r.
	 */
	private void construct(final ItemsetTreeNode parentOfR, final ItemsetTreeNode r, final int[] s,
			final int[] prefix) {

		// if the itemset in root node is the same as the one to be inserted,
		// we just increase the support, and return.
		if (same(s, prefix, r.itemset)) {
			r.support++;
			return;
		}

		final int[] rprefix = append(prefix, r.itemset);

		// if the node to be inserted is an ancestor of the itemset of the root
		// node
		// then insert the itemset between r and its parent
		// Before: parent_of_r --> r
		// After: parent_of_r --> s --> r
		// e.g. for a regular itemset tree
		// {2}:4 --> {2,3,4,5,6}:6
		// we insert {2,3}
		// {2}:4 --> {2,3}:7 --> {2,3,4,5,6}:6
		// e.g. for a compact itemset tree
		// r_parent r
		// {2}:4 --> {3,4,5,6}:6
		// we insert s={2,3}
		// r_parent s' r'
		// {2}:4 --> {3}:7 --> {4,5,6}:6
		if (ancestorOf(s, rprefix)) {
			// Calculate s' and r' by using the prefix
			final int[] sprime = copyItemsetWithoutItemsFrom(s, prefix);
			final int[] rprime = copyItemsetWithoutItemsFrom(rprefix, sprime);

			// create a new node for the itemset to be inserted with the support
			// of
			// the subtree root node + 1
			final ItemsetTreeNode newNodeS = new ItemsetTreeNode(sprime, r.support + 1);
			// set the childs and parent pointers.
			newNodeS.children.add(r);
			parentOfR.children.remove(r);
			parentOfR.children.add(newNodeS);
			// r.parent = newNodeS;
			r.itemset = rprime;
			return; // return
		}

		// Otherwise, calculate the largest common ancestor
		// of the itemset to be inserted and the root of the sutree
		final int[] l = getLargestCommonAncestor(s, rprefix);
		if (l != null) { // if there is one largest common ancestor
			final int[] sprime = copyItemsetWithoutItemsFrom(s, l);
			final int[] rprime = copyItemsetWithoutItemsFrom(r.itemset, l);

			// create a new node with that ancestor and the support of
			// the root +1.
			final ItemsetTreeNode newNode = new ItemsetTreeNode(l, r.support + 1);
			// set the node childs and parent pointers
			newNode.children.add(r);
			parentOfR.children.remove(r);
			parentOfR.children.add(newNode);
			// parentOfR = newNode;
			r.itemset = rprime;
			// append second children which is the itemset to be added with a
			// support of 1
			final ItemsetTreeNode newNode2 = new ItemsetTreeNode(sprime, 1);
			// update pointers for the new node
			newNode.children.add(newNode2);
			// newNode2.parent = newNode;
			return;
		}

		// else get the length of the root itemset
		final int indexLastItemOfR = (rprefix == null) ? 0 : rprefix.length;
		// increase the support of the root
		r.support++;
		// for each child of the root
		for (final ItemsetTreeNode ci : r.children) {
			final int[] ciprefix = append(rprefix, ci.itemset);

			// if one children of the root is the itemset to be inserted s,
			// then increase its support and stop
			if (same(s, ciprefix)) { // case 2
				ci.support++;
				return;
			}

			// if the itemset to be inserted is an ancestor of the child ci
			if (ancestorOf(s, ciprefix)) { // case 3
				final int[] sprime = copyItemsetWithoutItemsFrom(s, rprefix);
				final int[] ciprime = copyItemsetWithoutItemsFrom(ci.itemset, s);

				// create a new node between ci and r in the tree
				// and update child /parents pointers
				final ItemsetTreeNode newNode = new ItemsetTreeNode(sprime, ci.support + 1);
				newNode.children.add(ci);
				// newNode.parent = r;
				r.children.remove(ci);
				r.children.add(newNode);
				// ci.parent = newNode;
				ci.itemset = ciprime;
				return;
			}

			// if the child ci is an ancestor of s
			if (ancestorOf(ciprefix, s)) { // case 4

				// then make a recursive call to construct to handle this case.
				construct(r, ci, s, rprefix);
				return;
			}

			// case 5
			// if ci and s have a common ancestor that is larger than r:
			if (ciprefix[indexLastItemOfR] == s[indexLastItemOfR]) {
				// find the largest common ancestor
				final int[] ancestor = getLargestCommonAncestor(s, ciprefix);
				// create a new node for the ancestor itemset just found with
				// the support
				// of ci + 1

				final int[] ancestorprime = copyItemsetWithoutItemsFrom(ancestor, rprefix);

				final ItemsetTreeNode newNode = new ItemsetTreeNode(ancestorprime, ci.support + 1);
				// set r as parent
				// newNode.parent = r;
				r.children.add(newNode);
				// add ci as a children of the new node
				ci.itemset = copyItemsetWithoutItemsFrom(ci.itemset, ancestorprime);
				newNode.children.add(ci);
				// ci.parent = newNode;
				r.children.remove(ci);
				// create another new node for s with a support of 1, which
				// will be the child of the first new node
				final int[] sprime = copyItemsetWithoutItemsFromArrays(s, ancestorprime, rprefix);
				final ItemsetTreeNode newNode2 = new ItemsetTreeNode(sprime, 1);
				// newNode2.parent = newNode;
				newNode.children.add(newNode2);
				// end
				return;
			}

		}

		// Otherwise, case 1:
		// A new node is created for s with a support of 1 and is added
		// below the node r.
		final int[] sprime = copyItemsetWithoutItemsFrom(s, rprefix);
		final ItemsetTreeNode newNode = new ItemsetTreeNode(sprime, 1);
		// newNode.parent = r;
		r.children.add(newNode);

	}

	/**
	 * Make a copy of an itemset while removing items that appears in two
	 * itemsets named "prefix" and "s".
	 *
	 * @param r
	 *            the itemset
	 * @param prefix
	 *            the other itemset named "prefix"
	 * @param s
	 *            the other itemset named "s"
	 * @return the itemset
	 */
	private int[] copyItemsetWithoutItemsFromArrays(final int[] r, final int[] prefix, final int[] s) {

		// create an empty itemset
		final List<Integer> rprime = new ArrayList<Integer>(r.length);

		// for each item in r
		loop1: for (final Integer rvalue : r) {
			// if the other itemset prefix is not null
			if (prefix != null) {
				// for each item from the prefix
				for (final int pvalue : prefix) {
					// if it is the current item in r
					if (pvalue == rvalue) {
						// skip this item from r
						continue loop1;
						// if the current item from prefix is larger than (wrt
						// descending support ordering) the current item from r,
						// then break because itemsets are ordered so there will
						// be no match.
					} else if (itemComparator.compare(pvalue, rvalue) > 0) {
						break;
					}
				}
			}

			// if s is not null
			if (s != null) {
				// for each item in s
				for (final int svalue : s) {
					// if this item in s is the current item in r
					if (rvalue == svalue) {
						// skip it (don't add it to the new itemset)
						continue loop1;
						// if the current item from prefix is larger than (wrt
						// descending support ordering) the current item from r,
						// then break because itemsets are ordered so there will
						// be no match.
					} else if (itemComparator.compare(svalue, rvalue) > 0) {
						break;
					}
				}
			}
			rprime.add(rvalue);
		}
		// transform the new itemset "rprime" from ArrayList
		// to an array.
		final int[] rprimeArray = new int[rprime.size()];
		for (int i = 0; i < rprime.size(); i++) {
			rprimeArray[i] = rprime.get(i);
		}
		// return the array
		return rprimeArray;
	}

	/**
	 * Make a copy of an itemset without items from a second itemset.
	 *
	 * @param itemset1
	 *            the first itemset
	 * @param itemset2
	 *            the second itemset
	 * @return the new itemset
	 */
	private int[] copyItemsetWithoutItemsFrom(final int[] itemset1, final int[] itemset2) {
		// if the second itemset is null, just return the first itemset
		if (itemset2 == null) {
			return itemset1;
		}

		// create a new itemset
		final List<Integer> itemset1prime = new ArrayList<Integer>(itemset1.length);
		// for each item in the first itemset
		loop1: for (final int i1value : itemset1) {
			// for each it in the second itemset
			for (final int i2value : itemset2) {
				// if the items match, don't add the current item
				// from itemset1 to the new itemset
				if (i2value == i1value) {
					continue loop1;
					// otherwise, if the current item from "itemset2"
					// is larger than (wrt descending support ordering) the
					// current item from "itemset1" there will be no match
					// because itemsets are ordered .
				} else if (itemComparator.compare(i2value, i1value) > 0) {
					break;
				}
			}
			// if the current item from itemset1 was not in itemset2,
			// then add it to the new itemset
			itemset1prime.add(i1value);
		}
		// convert the new itemset from an ArrayList to an array
		final int[] itemset1primeArray = new int[itemset1prime.size()];
		for (int i = 0; i < itemset1prime.size(); i++) {
			itemset1primeArray[i] = itemset1prime.get(i);
		}
		// return the array
		return itemset1primeArray;
	}

	/**
	 * Method to calculate the largest common ancestor of two given itemsets (as
	 * defined in the paper).
	 *
	 * @param itemset1
	 *            the first itemset
	 * @param itemset2
	 *            the second itemset
	 * @return a new itemset which is the largest common ancestor or null if it
	 *         is the empty set
	 */
	private int[] getLargestCommonAncestor(final int[] itemset1, final int[] itemset2) {
		// if one of the itemsets is null,
		// return null.
		if (itemset2 == null || itemset1 == null) {
			return null;
		}

		// find the minimum length of the itemsets
		final int minI = itemset1.length < itemset2.length ? itemset1.length : itemset2.length;

		int count = 0; // to count the size of the common ancestor

		// for each position in the itemsets from 0 to the maximum length -1
		// Note that we use maxI-1 because we don't want that
		// the maximum ancestor to be equal to itemset1 or itemset2
		for (int i = 0; i < minI; i++) {
			// if the two items are different, we stop because
			// of the ordering
			if (itemset1[i] != itemset2[i]) {
				break;
			} else {
				// otherwise we increase the counter indicating the number of
				// common items in the prefix
				count++;
			}
		}
		// if there is a common ancestor of size >0
		// (we don,t want the empty set!)
		if (count > 0 && count < minI) {
			// create the itemset by copying the first "count" elements of
			// itemset1 and return it
			final int[] common = new int[count];
			System.arraycopy(itemset1, 0, common, 0, count);
			return common;
		} else {
			// otherwise, return null because the common ancestor is the empty
			// set
			return null;
		}
	}

	/**
	 * Check if a first itemset is the ancestor of the second itemset. Itemset1
	 * is an ancestor of itemset2 if: - itemset1 is null - size(itemset1) <
	 * size(itemset2) && if itemset1 has k items, then the first k items of
	 * itemset2 are equals to the first k items of itemset1.
	 *
	 * @param itemset1
	 *            the first itemset
	 * @param itemset2
	 *            the second itemset
	 * @return true, if yes, otherwise, false.
	 */
	private boolean ancestorOf(final int[] itemset1, final int[] itemset2) {
		// if the second itemset is null (empty set), return false
		if (itemset2 == null) {
			return false;
		}
		// if the first itemset is null (empty set), return true
		if (itemset1 == null) {
			return true;
		}
		// if the length of itemset 1 is greater than the one of
		// itemset2, it cannot be the ancestor, so return false
		if (itemset1.length >= itemset2.length) {
			return false;
		}
		// otherwise, loop on items from itemset1
		// and check if they are the same as itemset 2
		for (int i = 0; i < itemset1.length; i++) {
			// if one item is different, itemset1 is not the ancestor
			if (itemset1[i] != itemset2[i]) {
				return false;
			}
		}
		// otherwise itemset1 is an ancestor of itemset2
		return true;
	}

	/**
	 * Method to check if two itemsets are equals
	 *
	 * @param itemset1
	 *            the first itemset
	 * @param itemset2
	 *            the second itemset
	 * @param prefix
	 * @return true if they are the same or false otherwise
	 */
	private boolean same(final int[] itemset1, final int[] itemset2) {
		// if one is null, then returns false
		if (itemset2 == null || itemset1 == null) {
			return false;
		}
		// if they don't have the same size, then they cannot
		// be equal
		if (itemset1.length != itemset2.length) {
			return false;
		}
		// otherwise, loop on items from itemset1
		// and check if they are the same as itemset 2
		for (int i = 0; i < itemset1.length; i++) {
			if (itemset1[i] != itemset2[i]) {
				// if one is different then they are not the same
				return false;
			}
		}
		// otherwise they are the same
		return true;
	}

	/**
	 * Check if itemset1 is the same as the concatenation of prefix and itemset2
	 *
	 * @param itemset1
	 *            the first itemset
	 * @param prefix
	 *            a prefix
	 * @param itemset2
	 *            another itemset
	 * @return true if the same otherwise false
	 */
	private boolean same(final int[] itemset1, final int[] prefix, final int[] itemset2) {
		if (prefix == null) {
			return same(itemset1, itemset2);
		}
		// if one is null, then returns false
		if (itemset2 == null || itemset1 == null) {
			return false;
		}
		// if they don't have the same size, then they cannot
		// be equal
		if (itemset1.length != itemset2.length + prefix.length) {
			return false;
		}
		// otherwise, loop on items from itemset1
		// and check if they are the same as itemset 2
		int i = 0;
		while (i < prefix.length) {
			if (itemset1[i] != prefix[i]) {
				// if one is different then they are not the same
				return false;
			}
			i++;
		}
		int j = 0;
		while (j < itemset2.length) {
			if (itemset1[j++] != itemset2[i++]) {
				// if one is different then they are not the same
				return false;
			}
		}

		// otherwise they are the same
		return true;
	}

	/**
	 * Method that append two itemsets to create a larger one
	 *
	 * @param a1
	 *            the first itemset
	 * @param a2
	 *            the second itemset
	 * @return the new itemset
	 */
	public int[] append(final int[] a1, final int[] a2) {
		// if the first itemset is null, return the second one
		if (a1 == null) {
			return a2;
		}
		// if the second itemset is null, return the first one
		if (a2 == null) {
			return a1;
		}
		// create the new itemset
		final int[] newArray = new int[a1.length + a2.length];

		// copy the first itemset in the new itemset
		int i = 0;
		for (; i < a1.length; i++) {
			newArray[i] = a1[i];
		}
		// copy the second itemset in the new itemset
		for (int j = 0; j < a2.length; j++) {
			newArray[i++] = a2[j];
		}
		// return the new itemset
		return newArray;
	}

	/**
	 * Print statistics about the time and maximum memory usage for the
	 * construction of the itemset tree.
	 */
	public void printStatistics(final Logger logger) {
		System.gc();
		logger.info("========== MEMORY EFFICIENT ITEMSET TREE CONSTRUCTION - STATS ============\n");
		logger.info(" Tree construction time ~: " + (endTimestamp - startTimestamp) + " ms\n");
		logger.info(" Max memory: " + MemoryLogger.getInstance().getMaxMemory() + " MB\n");
		nodeCount = 0;
		totalItemCountInNodes = 0;
		sumBranchesLength = 0;
		totalNumberOfBranches = 0;
		recursiveStats(root, 1);
		logger.info(" Node count: " + nodeCount + "\n");
		logger.info(" No. items: " + totalItemCountInNodes + ", avg items per node: "
				+ totalItemCountInNodes / ((double) nodeCount) + "\n");
		logger.info("=====================================\n");
	}

	/**
	 * Recursive method to calculate statistics about the itemset tree
	 *
	 * @param root
	 *            the root node of the current subtree
	 * @param length
	 *            the cummulative sum of length of itemsets
	 */
	private void recursiveStats(final ItemsetTreeNode root, int length) {
		// if the root is not null or the empty set
		if (root != null && root.itemset != null) {
			// increase node count
			nodeCount++;
			// increase the total number of items
			totalItemCountInNodes += root.itemset.length;
		}
		// for each child node, make a recursive call
		for (final ItemsetTreeNode node : root.children) {
			recursiveStats(node, ++length);
		}
		// if no child, this node is a leaf, so
		// add the cummulative length of this branch to the sum
		// and add 1 to the total number of branches.
		if (root.children.size() == 0) {
			sumBranchesLength += length;
			totalNumberOfBranches += 1;
		}
	}

	/**
	 * Print the tree to System.out.
	 */
	public void printTree() {
		System.out.println(root.toString(new StringBuffer(), ""));
	}

	/**
	 * Return a string representation of the tree.
	 */
	@Override
	public String toString() {
		return root.toString(new StringBuffer(), "");
	}

	/**
	 * Get the chi-squared of the two itemsets
	 *
	 * <p>
	 * N.B. the chi-squared distribution has one degree of freedom.
	 */
	public double getChiSquared(final Itemset set1, final Itemset set2, final Itemset set1And2) {
		// sort by descending support
		final int[] sortedItems1 = set1.stream().sorted(itemComparator).mapToInt(i -> i).toArray();
		final int[] sortedItems2 = set2.stream().sorted(itemComparator).mapToInt(i -> i).toArray();
		final int[] sortedItems1And2 = set1And2.stream().sorted(itemComparator).mapToInt(i -> i).toArray();

		// contingency table
		final int supp1And2 = countEmpirical(sortedItems1And2, new int[0], root, new int[0]);
		final int supp1Not2 = countEmpirical(sortedItems1, sortedItems2, root, new int[0]);
		final int supp2Not1 = countEmpirical(sortedItems2, sortedItems1, root, new int[0]);
		final int supp1Nor2 = countEmpirical(new int[0], sortedItems1And2, root, new int[0]);

		// row & column sums
		final int supp1 = supp1And2 + supp1Not2;
		final int suppNot1 = supp2Not1 + supp1Nor2;
		final int supp2 = supp1And2 + supp2Not1;
		final int suppNot2 = supp1Not2 + supp1Nor2;
		final double total = supp1 + suppNot1; // avoid integer division

		// calculate chi-squared
		final double pInd1And2 = supp1 * supp2 / total;
		final double chi1And2 = (supp1And2 - pInd1And2) * (supp1And2 - pInd1And2) / pInd1And2;
		final double pInd1Not2 = supp1 * suppNot2 / total;
		final double chi1Not2 = (supp1Not2 - pInd1Not2) * (supp1Not2 - pInd1Not2) / pInd1Not2;
		final double pInd2Not1 = suppNot1 * supp2 / total;
		final double chi2Not1 = (supp2Not1 - pInd2Not1) * (supp2Not1 - pInd2Not1) / pInd2Not1;
		final double pInd1Nor2 = suppNot1 * suppNot2 / total;
		final double chi1Nor2 = (supp1Nor2 - pInd1Nor2) * (supp1Nor2 - pInd1Nor2) / pInd1Nor2;

		// cache support of itemset
		cachedItemsetSupport = supp1And2;

		return chi1And2 + chi1Not2 + chi2Not1 + chi1Nor2;
	}

	/** Support of last itemset tested by {@link #getChiSquared} */
	private int cachedItemsetSupport;

	/** Get support of last itemset tested by {@link #getChiSquared} */
	public int getCachedItemsetSupport() {
		return cachedItemsetSupport;
	}

	/**
	 * Get the chi-squared of the given itemset.
	 *
	 * @param set
	 *            the itemset
	 * @return the chi-squared statistic.
	 */
	public double getChiSquaredOfItemset(final Itemset set, final Multiset<Integer> singletons) {
		// sort by descending support
		final int[] sortedItems = set.stream().sorted(itemComparator).mapToInt(i -> i).toArray();
		return recursiveChiSquared(0, new BitSet(set.size()), sortedItems, singletons);
	}

	/**
	 * Pearson's chi-squared test for itemset independence. This tests the
	 * empirical itemset distribution against the independence model.
	 *
	 * <p>
	 * N.B. the chi-squared distribution has one degree of freedom.
	 *
	 * @see S. Brin et al. Beyond Market Baskets: Generalizing Association Rules
	 *      to Correlations
	 */
	private double recursiveChiSquared(final int n, final BitSet cell, final int[] sortedItems,
			final Multiset<Integer> singletons) {
		double chiSquared = 0.;
		if (n == sortedItems.length) {
			double pInd = noTransactions;
			final int[] inItems = new int[cell.cardinality()];
			final int[] outItems = new int[n - cell.cardinality()];
			int i = 0, j = 0;
			for (int k = 0; k < n; k++) {
				if (cell.get(k)) {
					inItems[i] = sortedItems[k];
					i++;
					pInd *= singletons.count(sortedItems[k]) / (double) noTransactions;
				} else {
					outItems[j] = sortedItems[k];
					j++;
					pInd *= (noTransactions - singletons.count(sortedItems[k])) / (double) noTransactions;
				}
			}
			final double pEmp = countEmpirical(inItems, outItems, root, new int[0]);
			chiSquared = ((pEmp - pInd) * (pEmp - pInd)) / pInd;
		} else {
			final BitSet celln = (BitSet) cell.clone();
			celln.set(n);
			chiSquared += recursiveChiSquared(n + 1, celln, sortedItems, singletons);
			chiSquared += recursiveChiSquared(n + 1, cell, sortedItems, singletons);
		}
		return chiSquared;
	}

	/**
	 * This method calculates the no. of transactions which support the itemset
	 * 's' but do *not* support the itemset 'exc' by using a subtree defined by
	 * its root.
	 *
	 * @param s
	 *            the itemset that should be supported
	 * @param exc
	 *            the itemset that should *not* be supported
	 * @param root
	 *            the root of the subtree
	 * @param prefix
	 *            the prefix of the subtree
	 * @return the support as an integer
	 */
	private int countEmpirical(final int[] s, final int[] exc, final ItemsetTreeNode root, final int[] prefix) {
		if (s.length == 0)
			return countNotAny(exc, root);
		if (exc.length == 0)
			return count(s, root, prefix);
		// the variable count will be used to count the support
		int count = 0;
		// for each child of the root
		for (final ItemsetTreeNode ci : root.children) {
			// if the first item of the itemset that we are looking for
			// is greater than (wrt descending support ordering) the first item
			// of the child, we need to look further in that tree. Also,
			// we can stop if any elements from exc are contained in the child.
			final int[] ciprefix = append(prefix, ci.itemset);
			if (itemComparator.compare(ciprefix[0], s[0]) <= 0 && !containsAny(ci.itemset, exc)) {

				// if s is included in ci, add the support of ci to the current
				// count.
				if (includedIn(s, ciprefix)) {
					count += ci.support;
					// remove the support of any subtree paths that contain
					// elements from exc
					count -= countSubtree(exc, ci);
				} else if (itemComparator.compare(ciprefix[ciprefix.length - 1], s[s.length - 1]) < 0) {
					// otherwise, if the last item of ci is smaller than (wrt
					// descending support ordering) the last item of s,
					// then make a recursive call to explore
					// the subtree where ci is the root
					count += countEmpirical(s, exc, ci, ciprefix);
				}
			}
		}
		// return the total count
		return count;
	}

	/**
	 * Count the support of subtrees containing any element from the given set
	 */
	private int countSubtree(final int[] exc, final ItemsetTreeNode root) {
		// the variable count will be used to count the support
		int count = 0;
		// for each child of the root
		for (final ItemsetTreeNode ci : root.children) {
			if (containsAny(ci.itemset, exc)) {
				count += ci.support;
			} else if (!(itemComparator.compare(exc[exc.length - 1], ci.itemset[0]) < 0)) {
				// otherwise, if the last item of exc is *not* smaller than (wrt
				// descending support ordering) the first item of the child,
				// make a recursive call to explore the subtree with ci as root
				count += countSubtree(exc, ci);
			}
		}
		// return the total count
		return count;
	}

	/**
	 * This method calculates the no. transactions not containing any items from
	 * an itemset by using a subtree defined by its root.
	 *
	 * @param s
	 *            the itemset
	 * @param root
	 *            the root of the subtree
	 * @return the no. transactions not containing any items from the itemset
	 */
	private int countNotAny(final int[] s, final ItemsetTreeNode root) {
		// the variable count will be used to count the support
		int count = 0;
		// for each child of the root
		for (final ItemsetTreeNode ci : root.children) {
			// If the child does not contain any items from the itemset
			if (!containsAny(ci.itemset, s)) {
				// If the last item of the itemset is smaller than (wrt
				// descending support ordering) the first item of the child,
				// the child cannot contain any items from the itemset
				if (itemComparator.compare(s[s.length - 1], ci.itemset[0]) < 0)
					count += ci.support;
				else {
					// otherwise, add the support of ci to the current count
					count += ci.support;
					// and remove the support of any subtree paths that
					// contain elements from exc
					count -= countSubtree(s, ci);
				}
			}
		}
		// return the total count
		return count;
	}

	/**
	 * Check if an itemset contains any of the items. Assumes both arrays are
	 * ordered.
	 *
	 * @param itemset
	 *            the itemset
	 * @param items
	 *            the items
	 * @return true if any of the items appear in the itemset, false otherwise
	 */
	private boolean containsAny(final int[] itemset, final int items[]) {
		// If the last item in items is smaller than (wrt descending support
		// ordering) the first item in the itemset then this is false, and
		// similarly when the first item in items is greater than the last item
		// in the itemset
		if (itemComparator.compare(items[items.length - 1], itemset[0]) < 0
				|| itemComparator.compare(items[0], itemset[itemset.length - 1]) > 0)
			return false;
		// Otherwise we have to check each item individually
		for (final int item : items) {
			if (contains(itemset, item))
				return true;
		}
		return false;
	}

	/**
	 * Check if an itemset contains an item.
	 *
	 * @param itemset
	 *            the itemset
	 * @param item
	 *            the item
	 * @return true if the item appears in the itemset, false otherwise
	 */
	private boolean contains(final int[] itemset, final int item) {
		// for each item in the itemset
		for (int i = 0; i < itemset.length; i++) {
			// if the item is found, return true
			if (itemset[i] == item) {
				return true;
				// if the current item is larger than (wrt descending support
				// ordering) the item that is searched,
				// then return false because of the ordering.
			} else if (itemComparator.compare(itemset[i], item) > 0) {
				return false;
			}
		}
		// not found, return false
		return false;
	}

	/**
	 * Get the support of the given itemset.
	 *
	 * @param set
	 *            the itemset
	 * @return the support as an integer.
	 */
	public int getSupportOfItemset(final Itemset set) {
		// sort by descending support
		final int[] sortedItems = set.stream().sorted(itemComparator).mapToInt(i -> i).toArray();
		return count(sortedItems, root, new int[0]); // call count method
	}

	/**
	 * Get the relative support of the given itemset.
	 *
	 * @param set
	 *            the itemset
	 * @return the relative support as a double.
	 */
	public double getRelativeSupportOfItemset(final Itemset set) {
		// sort by descending support
		final int[] sortedItems = set.stream().sorted(itemComparator).mapToInt(i -> i).toArray();
		// call count method
		return (double) count(sortedItems, root, new int[0]) / noTransactions;
	}

	/**
	 * Get the number of transactions in the database used to build this tree
	 *
	 * @return the number of transactions
	 */
	public int getNoTransactions() {
		return noTransactions;
	}

	/**
	 * This method calculates the support of an itemset by using a subtree
	 * defined by its root.
	 *
	 * Note: this is implemented based on the algorithm "count" of Table 2 in
	 * the paper by Kubat et al.
	 *
	 * <p>
	 * Note that there was a problem with the algorithm in the paper. I had to
	 * change > to < in : ci.itemset[ci.itemset.length -1] < s[s.length -1]).
	 *
	 * @param s
	 *            the itemset
	 * @param root
	 *            the root of the subtree
	 * @param prefix
	 *            the prefix of the subtree
	 * @return the support as an integer
	 */
	private int count(final int[] s, final ItemsetTreeNode root, final int[] prefix) {
		// the variable count will be used to count the support
		int count = 0;
		// for each child of the root
		for (final ItemsetTreeNode ci : root.children) {
			// if the first item of the itemset that we are looking for
			// is greater than (wrt descending support ordering) the first item
			// of the child, we need to look further in that tree.
			final int[] ciprefix = append(prefix, ci.itemset);

			if (itemComparator.compare(ciprefix[0], s[0]) <= 0) {

				// if s is included in ci, add the support of ci to the current
				// count.
				if (includedIn(s, ciprefix)) {
					count += ci.support;
				} else if (itemComparator.compare(ciprefix[ciprefix.length - 1], s[s.length - 1]) < 0) {
					// otherwise, if the last item of ci is smaller than (wrt
					// descending support ordering) the last item of s,
					// then make a recursive call to explore
					// the subtree where ci is the root
					count += count(s, ci, ciprefix);
				}
			}
		}
		// return the total count
		return count;
	}

	/**
	 * Check if an itemset is contained in another
	 *
	 * @param itemset1
	 *            the first itemset
	 * @param itemset2
	 *            the second itemset
	 * @return true if yes, otherwise false
	 */
	private boolean includedIn(final int[] itemset1, final int[] itemset2) {
		int count = 0; // the current position of itemset1 that we want to find
						// in itemset2

		// for each item in itemset2
		for (int i = 0; i < itemset2.length; i++) {
			// if we found the item
			if (itemset2[i] == itemset1[count]) {
				// we will look for the next item of itemset1
				count++;
				// if we have found all items already, return true
				if (count == itemset1.length) {
					return true;
				}
			}
		}
		// it is not included, so return false!
		return false;
	}

}
