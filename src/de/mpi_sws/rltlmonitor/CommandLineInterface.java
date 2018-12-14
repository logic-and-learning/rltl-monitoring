package de.mpi_sws.rltlmonitor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Optional;
import java.util.function.Supplier;

import org.mpi_sws.rltl.parser.ParseException;

import net.automatalib.automata.transout.impl.FastMoore;

import javax.swing.text.html.Option;

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
		System.out.println("Usage: [(--stats | -s) <path/to/statistics/file>] (rltl | ltl) formula");
		System.out.println("By default, an rLTL monitor is constructed.");

	}

	/**
	 * Main method.
	 * 
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {

		Optional<Config> cfgOpt = parseCliArguments(args);
		if (cfgOpt.isEmpty()) {
			printUsage();
			System.exit(1);
		}
		Config cfg = cfgOpt.get();

		try {

			//
			// Do the monitor construction
			//

			FastMoore<BitSet, BitSet> rltlmonitor = null;
			FastMoore<BitSet, BitSet> ltlmonitor = null;

			if (cfg.logic == Logic.rLTL || cfg.logic == Logic.BOTH) {
				// rLTL monitor construction
				rltlmonitor = MonitorConstructor.constructrLTLMonitor(cfg.formula);
			}

			if (cfg.logic == Logic.LTL || cfg.logic == Logic.BOTH) {
				// LTL monitor construction (according to Brauer et al.)
				ltlmonitor = MonitorConstructor.constructLTLMonitor(cfg.formula);
			}

			//
			// Output monitor(s)
			//
			if (rltlmonitor != null) {
				System.out.println("\n========== Final rLTL Monitor ==========\n");
				System.out.println(Owl2Automatalib.toDot(rltlmonitor));
			}
			if (ltlmonitor != null) {
				System.out.println("\n========== Final LTL Monitor ==========\n");
				System.out.println(Owl2Automatalib.toDot(ltlmonitor));
			}

			//
			// Output statistics
			//
			if (cfg.statsFile.isPresent()) {
				dumpInStatsFile(cfg.statsFile.get(), rltlmonitor, ltlmonitor);
			}
		} catch (ParseException | IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}


	static void dumpInStatsFile(String path, FastMoore<BitSet, BitSet> rltl, FastMoore<BitSet, BitSet> ltl) throws IOException {
		File f = new File(path);
		f.createNewFile(); // Just in case it doesn't exist, yet.
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(f, true))) {
			if (rltl != null && ltl != null) {
				writer.write(String.valueOf(ltl.size()));
				writer.write(", ");
				writer.write(String.valueOf(rltl.size()));
			} else if (rltl != null) {
				writer.write(String.valueOf(rltl.size()));
			} else if (ltl != null) {
				writer.write(String.valueOf(ltl.size()));
			}
			writer.newLine();
		}
	}

	static Optional<Config> parseCliArguments(String[] args) {
		Optional<Logic> useRLTL = Optional.empty();
		Optional<String> stats = Optional.empty();
		Optional<String> formula = Optional.empty();
		for (int i = 0; i < args.length; i += 1) {
			String arg = args[i];
			switch (arg.toLowerCase()) {
				case "--help": case "-h":
						return Optional.empty();
				case "rltl":
					if (useRLTL.isPresent()) {
						System.out.println("Logic specified twice.");
						return Optional.empty();
					}
					useRLTL = Optional.of(Logic.rLTL);
					break;
				case "ltl":
					if (useRLTL.isPresent()) {
						System.out.println("Logic specified twice.");
						return Optional.empty();
					}
					useRLTL = Optional.of(Logic.LTL);
					break;
				case "both":
					if (useRLTL.isPresent()) {
						System.out.println("Logic specified twice.");
						return Optional.empty();
					}
					useRLTL = Optional.of(Logic.BOTH);
					break;
				case "--stats": case "-s":
					i += 1;
					if (i == args.length || stats.isPresent()) {
						System.out.println("No statistic file given.");
						return Optional.empty();
					}
					stats = Optional.of(args[i]);
					break;
				default:
					if (i < args.length - 1) {
						System.out.println("Unknown command line argument: " + arg);
						return Optional.empty();
					}
					formula = Optional.of(arg);
					break;
			}
		}
		if (formula.isEmpty()) {
			System.out.println("No formula given.");
			return Optional.empty();
		} else {
			return Optional.of(new Config(useRLTL.orElse(Logic.rLTL), stats, formula.get()));
		}
	}

	static enum Logic {
		LTL, rLTL, BOTH
	}

	static class Config {
		Logic logic;
		String formula;
		Optional<String> statsFile;

		Config(Logic logic, Optional<String> statsFile, String formula) {
			this.logic = logic;
			this.statsFile = statsFile;
			this.formula = formula;
		}
	}

}
