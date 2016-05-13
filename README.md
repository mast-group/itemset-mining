IIM: Interesting Itemset Miner [![Build Status](https://travis-ci.org/mast-group/itemset-mining.svg?branch=master)](https://travis-ci.org/mast-group/itemset-mining)
================
 
IIM is a novel algorithm that mines the itemsets that are most interesting under a probablistic model of transactions. Our model is able to efficiently infer interesting itemsets directly from the transaction database.

This is an implementation of the itemset miner from our paper:  
[*A Bayesian Network Model for Interesting Itemsets*](http://arxiv.org/abs/1510.04130)  
J. Fowkes and C. Sutton. arXiv preprint 1510.04130, 2015.   


Installation 
------------

#### Installing in Eclipse

Simply import as a maven project into [Eclipse](https://eclipse.org/) using the *File -> Import...* menu option (note that this requires [m2eclipse](http://eclipse.org/m2e/)). 

It's also possible to export a runnable jar from Eclipse using the *File -> Export...* menu option.

#### Compiling a Runnable Jar

To compile a standalone runnable jar, simply run

```
mvn package
```

in the top-level directory (note that this requires [maven](https://maven.apache.org/)). This will create the standalone runnable jar ```itemset-mining-1.0.jar``` in the itemset-mining/target subdirectory. The main class is *itemsetmining.main.ItemsetMining* (see below).


Running IIM
-----------

IIM uses a Bayesian Network Model to determine which itemsets are the most interesting in a given dataset.  

#### Mining Interesting Itemsets 

Main class *itemsetmining.main.ItemsetMining* mines itemsets from a specified transaction database file. It has the following command line options:

* **-f**  &nbsp;  database file to mine (in [FIMI](http://fimi.ua.ac.be/) format)
* **-i**  &nbsp;  max. no. iterations
* **-s**  &nbsp;  max. no. structure steps
* **-r**  &nbsp;  max. runtime (min)
* **-l**  &nbsp;  log level (INFO/FINE/FINER/FINEST)
* **-v**  &nbsp;  print to console instead of log file   

See the individual file javadocs in *itemsetmining.main.ItemsetMining* for information on the Java interface.
In Eclipse you can set command line arguments for the IIM interface using the *Run Configurations...* menu option. 

#### Example Usage

A complete example using the command line interface on a runnable jar. We can mine the provided example dataset ```example.dat``` as follows: 

  ```sh 
  $ java -cp itemset-mining/target/itemset-mining-1.0.jar itemsetmining.main.ItemsetMining     
   -i 100
   -f example.dat 
   -v 
  ```

which will output to the console. Omitting the ```-v``` flag will redirect output to a log-file in ```/tmp/```. 

Input/Output Formats
--------------------

#### Input Format

IIM takes as input a transaction database file in [FIMI](http://fimi.ua.ac.be/) format. The FIMI format is very simple: each line of the input file represents a transaction 
and each transaction is a space-seperated list of items, represented by positive integers. The FIMI format requires the transaction items to be listed in increasing order 
and does not allow duplicate items (however IIM is not sensitive to item order and ignores item duplicates). For example, a few lines (transactions) from ```example.dat``` are:

```text
6 10 22 31 32 41 52 
2 12 14 26 50 
3 18 25 31 34 38 63 
17 28 30 37 
16 19 45 46 49 51 52 54 56 65 
```

Note that any other item formats (e.g. words for text corpora) 
need to be manually mapped to (and from) positive integers by means of a dictionary.   

#### Output Format

IIM outputs a list of interesting itemsets, one itemset per line, ordered first by their interestingness (given in the 'int' column) followed by their probability (given in the 'prob' column). 
For example, the first few lines of output for the usage example above are:

```text
============= INTERESTING ITEMSETS =============
{18}    prob: 0.34830   int: 1.00000 
{14}    prob: 0.13740   int: 1.00000 
{5}     prob: 0.11740   int: 1.00000 
{16}    prob: 0.09110   int: 1.00000 
{6, 7, 22, 36, 65, 67}  prob: 0.08440   int: 1.00000 
{17, 28, 30, 37}        prob: 0.07830   int: 1.00000 
{1, 2, 8, 11, 12, 13, 20, 63, 64}       prob: 0.07670   int: 1.00000 
{59, 60, 62}    prob: 0.06980   int: 1.00000 
{43, 46, 55}    prob: 0.06890   int: 1.00000 
{53}    prob: 0.06870   int: 1.00000 
```

See the accompanying [paper](http://arxiv.org/abs/1510.04130) for details of how to interpret 'interestingness' and 'probability' under IIM's probabilistic model.

Spark Implementation
--------------------

IIM also has a (beta) parallel implemetation using [Spark](http://spark.apache.org/) in [Standalone Mode](http://spark.apache.org/docs/latest/spark-standalone.html) 
with an [HDFS](http://hadoop.apache.org/) filesystem (see e.g. relevant parts of [this tutorial](http://www.michael-noll.com/tutorials/running-hadoop-on-ubuntu-linux-multi-node-cluster/)). 

#### Configuring Spark Options

Basic IIM configuration for Spark and HDFS must be set in ```itemset-miner/src/main/resources/spark.properties``` (see the example config provided):

* *SparkHome*   &nbsp;  	Spark home directory
* *SparkMaster* &nbsp;  	URL of spark master server
* *MachinesInCluster* &nbsp; 	No. machines in the cluster
* *HDFSMaster*	&nbsp;	   URL of HDFS master server
* *HDFSConfFile*	&nbsp;  Location of Hadoop ```core-site.xml```

#### Mining Itemsets using Spark 

Main class *itemsetmining.main.SparkItemsetMining* mines itemsets using a Standalone Spark Sever. It has the following *additional* command line options:

* **-c**  &nbsp;  no. Spark cores to use
* **-j**  &nbsp;  location of IIM standalone jar (default is ```itemset-mining/target/itemset-mining-1.0.jar```)  

See the individual file javadocs in *itemsetmining.main.SparkItemsetMining* for information on the Java interface.

#### Example Usage

A complete Spark example using the command line interface is as follows: 

  ```sh 
  $ java -cp itemset-mining/target/itemset-mining-1.0.jar itemsetmining.main.SparkItemsetMining
   -c 16  
   -i 100
   -f example.dat 
   -v 
  ```

which will output to the console. Omitting the ```-v``` flag will redirect output to a log-file in ```/tmp/```. 

Bugs
----

Please report any bugs using GitHub's issue tracker.


License
-------

This algorithm is released under the GNU GPLv3 license.
