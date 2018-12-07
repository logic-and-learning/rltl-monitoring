package de.mpi_sws.rltlmonitor;

import java.io.UnsupportedEncodingException;
import java.util.BitSet;

import org.mpi_sws.rltl.parser.ParseException;

import net.automatalib.automata.transout.impl.FastMoore;

/**
 * Command line interface for the (r)LTL monitor constructor.
 * 
 * @author Daniel Neider
 *
 */
public class CommandLineInterface {

	/**
	 * Prints usage information to standard output.
	 */
	private static void printUsage() {

		System.out.println("Invalid command line arguments.");
		System.out.println("Usage: (rltl | ltl) formula");
		System.out.println("By default, an rLTL monitor is constructed.");

	}

	/**
	 * Main method.
	 * 
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {

		//
		// Set up parameters
		//
		boolean constructrLTLMonitor = true;

		//
		// Check number of command line arguments
		//
		if (args.length != 2) {
			printUsage();
			System.exit(1);
		}

		//
		// Check first parameter
		//
		if (args[0].trim().toLowerCase().equals("rltl")) {
			constructrLTLMonitor = true;
		} else if (args[0].trim().toLowerCase().equals("ltl")) {
			constructrLTLMonitor = false;
		} else {
			printUsage();
			System.exit(1);
		}

		//
		// Do the monitor construction
		//
		FastMoore<BitSet, BitSet> monitor = null;

		// rLTL monitor construction
		if (constructrLTLMonitor) {

			try {
				monitor = MonitorConstructor.constructrLTLMonitor(args[1]);
			} catch (UnsupportedEncodingException | ParseException e) {
				e.printStackTrace();
				System.exit(1);
			}

		}

		// LTL monitor construction (according to Brauer et al.)
		else {
			monitor = MonitorConstructor.constructLTLMonitor(args[1]);
		}

		//
		// Output monitor
		//
		System.out.println("\n========== Final Monitor ==========\n");
		System.out.println(Owl2Automatalib.toDot(monitor));
		
	}

}
