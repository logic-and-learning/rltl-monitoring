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

    java -cp 'rltlmonitor.jar:lib/*' de.mpi_sws.rltlmonitor.CommandLineInterface rLTL-formula

The (only) argument `rLTL-formula` specifies  the rLTL formula for which the
monitor is constructed. Note that the rLTL formula must be a single argument.
Thus, you most likely need to enclose this argument with quotes
(e.g., `'a U b'`).