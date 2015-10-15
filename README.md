IIM: Interesting Itemset Miner [![Build Status](https://travis-ci.org/mast-group/itemset-mining.svg?branch=master)](https://travis-ci.org/mast-group/itemset-mining)
================
 
IIM is a novel algorithm that mines the itemsets that are most interesting under a probablistic model of transactions.   

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

in the main tassal directory (note that this requires [maven](https://maven.apache.org/)).

This will create the standalone runnable jar ```itemset-mining-1.0.jar``` in the itemset-mining/target subdirectory.


Running IIM
-----------

IIM uses a Bayesian Network Model to determine which itemsets are the most interesting in a given dataset.  

#### Mining Interesting Itemsets 

*itemsetmining.main.ItemsetMining* mines itemsets from a specified transaction database file. It has the following command line options:

* **-f**    database file to mine (in FIMI format)
* **-i**    max. no. iterations
* **-s**    max. no. structure steps
* **-r**    max. runtime (min)
* **-l**    log level (INFO/FINE/FINER/FINEST)
* **-v**    print to console instead of log file   

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

Spark Implementation
--------------------

IIM also has a (beta) parallel implemetation using [Spark](http://spark.apache.org/) in [Standalone Mode](http://spark.apache.org/docs/latest/spark-standalone.html) 
with an [HDFS](http://hadoop.apache.org/) filesystem (see e.g. relevant parts of [this tutorial](http://www.michael-noll.com/tutorials/running-hadoop-on-ubuntu-linux-multi-node-cluster/)). 

#### Configuring Spark Options

Basic IIM configuration for Spark and HDFS must be set in ```itemset-miner/src/main/resources/spark.properties``` (see the example config provided):

- *SparkHome*     	Spark home directory
- *SparkMaster*   	URL of spark master server
- *MachinesInCluster*  	No. machines in the cluster

- *HDFSMaster*		URL of HDFS master server
- *HDFSConfFile*	Location of Hadoop ```core-site.xml```

#### Mining Itemsets using Spark 

*itemsetmining.main.SparkItemsetMining* mines itemsets using a Standalone Spark Sever. It has the following *additional* command line options:

* **-c**    no Spark cores to use
* **-j**    location of IIM standalone jar (default is ```itemset-mining/target/itemset-mining-1.0.jar```)  

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

This algorithm is released under the new BSD license.
