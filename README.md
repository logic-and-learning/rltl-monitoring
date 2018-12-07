rLTL Runtime Verification
=========================
Code for constructing rLTL monitors.


Prerequisites
-------------
Make sure you satisfy the following requirements:

- Java 11
- [Apache Ant](https://ant.apache.org/)


Building
--------
To compile the sources, change into the root directory of this project and
execute the command

    ant

After compilation is has finished, execute

    ant jar
    
to generate a jar file.


Constructing rLTL Monitors
--------------------------

To construct an rLTL monitor, execute the following command in the root
directory of the project:

    java -cp 'rltlmonitor.jar:lib/*' de.mpi_sws.rltlmonitor.CommandLineInterface logic formula

The tools requires two arguments:

1) The argument `logic` and be either `rltl` or `ltl`. It specifies whether to
construct an rLTL monitor or an LTL monitor.

2) The argument `rLTL-formula` specifies the (r)LTL formula for which the
monitor is constructed. Note that the formula must be a single argument. Thus,
you most likely need to enclose this argument with quotes (e.g., `'a U b'`).