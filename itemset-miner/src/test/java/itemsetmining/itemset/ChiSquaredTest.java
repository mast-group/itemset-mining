package itemsetmining.itemset;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.junit.Test;

import com.google.common.collect.Multiset;

import itemsetmining.main.ItemsetMining;

public class ChiSquaredTest {

	@Test
	public void testItemsetTree() throws IOException {

		final File input = getTestFile("chiSquared.txt"); // database
		// Get frequency of single items
		final Multiset<Integer> singletons = ItemsetMining.scanDatabaseToDetermineFrequencyOfSingleItems(input);

		// Applying the algorithm to build the itemset tree
		final ItemsetTree itemsetTree = new ItemsetTree(singletons);
		// method to construct the tree from a set of transactions in a file
		itemsetTree.buildTree(input);
		// print the tree in the console
		System.out.println("THIS IS THE TREE:");
		itemsetTree.printTree();

		// After the tree is built, test chi-squared
		final Itemset set1 = new Itemset(8);
		final Itemset set2 = new Itemset(9);
		final Itemset set = new Itemset(8, 9);
		assertEquals(0.9, itemsetTree.getChiSquaredOfItemset(set, singletons), 1e-15);
		assertEquals(0.9, itemsetTree.getChiSquared(set1, set2, set), 1e-15);
	}

	public File getTestFile(final String filename) throws UnsupportedEncodingException {
		final URL url = this.getClass().getClassLoader().getResource(filename);
		return new File(java.net.URLDecoder.decode(url.getPath(), "UTF-8"));
	}

}