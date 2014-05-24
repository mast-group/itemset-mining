package itemsetmining.main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.LineIterator;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class TransactionGenerator {

	private static HashMap<Itemset, Double> itemsets = Maps.newHashMap();

	public static void main(final String[] args) throws IOException {

		if (args.length != 2) {
			System.err.println("Usage <problemName> <noTransactions>");
			System.exit(-1);
		}

		// Create interesting itemsets that highlight problems
		// Here [1,2] is the champagne & caviar problem
		// (not generated when support is too high)
		if (args[0].equals("caviar")) {

			// Champagne & Caviar
			Itemset s12 = new Itemset(1, 2);
			double p12 = 0.1;
			itemsets.put(s12, p12);

			// Other transactions
			Itemset s3 = new Itemset(3);
			double p3 = 0.8;
			itemsets.put(s3, p3);

			Itemset s4 = new Itemset(4);
			double p4 = 0.5;
			itemsets.put(s4, p4);

		} else if (args[0].equals("freerider")) {

			Itemset s12 = new Itemset(1, 2);
			Itemset s3 = new Itemset(3);
			double p12 = 0.5;
			double p3 = 0.5;
			itemsets.put(s12, p12);
			itemsets.put(s3, p3);

			// Here [1,2] is known as a cross-support pattern
			// (spuriously generated when support is too low)
		} else if (args[0].equals("unlifted")) {

			Itemset s1 = new Itemset(1);
			double p1 = 0.2;
			itemsets.put(s1, p1);

			Itemset s2 = new Itemset(2);
			double p2 = 0.8;
			itemsets.put(s2, p2);

		} else
			throw new IllegalArgumentException("Incorrect problem name.");

		// Set output file
		final File outFile = new File("src/main/resources/" + args[0] + ".txt");
		final PrintWriter out = new PrintWriter(outFile, "UTF-8");

		// Generate transaction database
		int noTransactions = Integer.parseInt(args[1]);
		for (int i = 0; i < noTransactions; i++) {

			// Generate transaction from distribution
			Set<Integer> transaction = sampleFromDistribution();
			for (int item : transaction) {
				out.print(item + " ");
			}
			if (!transaction.isEmpty())
				out.println();

		}
		out.close();

		// Print file to screen
		FileReader reader = new FileReader(outFile);
		LineIterator it = new LineIterator(reader);
		while (it.hasNext()) {
			System.out.println(it.nextLine());
		}
		LineIterator.closeQuietly(it);
	}

	/** Randomly generate itemset with its probability */
	private static Set<Integer> sampleFromDistribution() {

		Set<Integer> transaction = Sets.newHashSet();
		for (Entry<Itemset, Double> entry : itemsets.entrySet()) {
			if (Math.random() < entry.getValue()) {
				transaction.addAll(entry.getKey().getItems());
			}
		}

		return transaction;
	}
}
