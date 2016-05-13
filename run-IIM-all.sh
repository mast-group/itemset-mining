#!/bin/bash
for p in \
uganda \
abstracts \
plants \
mammals
do
java -cp itemset-mining/target/itemset-mining-1.0.jar itemsetmining.main.ItemsetMining \
-f /afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Datasets/Succintly/$p.dat
done 
