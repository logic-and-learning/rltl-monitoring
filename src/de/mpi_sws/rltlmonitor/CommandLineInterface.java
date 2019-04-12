package de.mpi_sws.rltlmonitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Consumer;

import org.mpi_sws.rltl.parser.ParseException;

import net.automatalib.automata.transout.impl.FastMoore;
import net.automatalib.automata.transout.impl.FastMooreState;

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
			long elapsedLTLMonitorConstructionTime = 0;
			long elapsedrLTLMonitorConstructionTime = 0;

			// rLTL monitor construction
			if (cfg.logic == Logic.rLTL || cfg.logic == Logic.BOTH) {
				long start = System.nanoTime();
				rltlmonitor = MonitorConstructor.constructrLTLMonitor(cfg.formula);
				elapsedrLTLMonitorConstructionTime = System.nanoTime() - start;
			}

			// LTL monitor construction (according to Brauer et al.)
			if (cfg.logic == Logic.LTL || cfg.logic == Logic.BOTH) {
				long start = System.nanoTime();
				ltlmonitor = MonitorConstructor.constructLTLMonitor(cfg.formula);
				elapsedLTLMonitorConstructionTime = System.nanoTime() - start;
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

				File f = new File(cfg.statsFile.get());
				f.createNewFile(); // Just in case it doesn't exist yet

				try (BufferedWriter writer = new BufferedWriter(new FileWriter(f, true))) {
					dumpStatsToWriter(writer, rltlmonitor, elapsedrLTLMonitorConstructionTime, ltlmonitor,
							elapsedLTLMonitorConstructionTime);
				}

			}

		} catch (ParseException | IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	/***
	 * Writes statistics about an rLTL and LTL monitor to a writer/stream. Each
	 * method call appends a new line containing statistics as a comma-separated
	 * list. Each entry of this list is an argument to this method, in the order of
	 * their appearance.
	 * 
	 * @param writer                             the writer to write to
	 * @param rltlMonitor                        an rLTL monitor
	 * @param elapsedrLTLMonitorConstructionTime time required to construct the rLTL
	 *                                           monitor
	 * @param ltlMonitor                         an LTL monitor
	 * @param elapsedLTLMonitorConstructionTime  time required to construct the LTL
	 *                                           monitor
	 * @throws IOException
	 */
	static void dumpStatsToWriter(BufferedWriter writer, FastMoore<BitSet, BitSet> rltlMonitor,
			long elapsedrLTLMonitorConstructionTime, FastMoore<BitSet, BitSet> ltlMonitor,
			long elapsedLTLMonitorConstructionTime) throws IOException {

		//
		// Size of monitors
		//
		int rltlMonitorSize = rltlMonitor != null ? rltlMonitor.size() : 0;
		int ltlMonitorSize = ltlMonitor != null ? ltlMonitor.size() : 0;

		//
		// Number of outputs
		//
		HashSet<BitSet> outputs = new HashSet<>(10);
		if (rltlMonitor != null) {
			rltlMonitor.forEach(new Consumer<FastMooreState<BitSet>>() {
				@Override
				public void accept(FastMooreState<BitSet> state) {
					outputs.add((BitSet) state.getOutput().clone());
				}

			});
		}
		int rltlMonitorOutputs = outputs.size();

		outputs.clear();
		if (ltlMonitor != null) {
			ltlMonitor.forEach(new Consumer<FastMooreState<BitSet>>() {

				@Override
				public void accept(FastMooreState<BitSet> state) {
					outputs.add((BitSet) state.getOutput().clone());
				}

			});
		}
		int ltlMonitorOutputs = outputs.size();

		//
		// Elapsed Time
		//
		float floatElapsedTimerLTL = (float) (elapsedrLTLMonitorConstructionTime / 1000) / 1000000f;
		float floatElapsedTimeLTL = (float) (elapsedLTLMonitorConstructionTime / 1000) / 1000000f;

		//
		// Dump statistics to stream
		//

		// LTL Monitor
		if (ltlMonitor != null) {
			writer.write(String.valueOf(ltlMonitorSize));
			writer.write(",");
			writer.write(String.valueOf(ltlMonitorOutputs));
			writer.write(",");
			writer.write(String.format("%.2f", floatElapsedTimeLTL));
		} else {
			writer.write(",,");
		}

		// rLTL monitor
		writer.write(",");
		if (rltlMonitor != null) {
			writer.write(String.valueOf(rltlMonitorSize));
			writer.write(",");
			writer.write(String.valueOf(rltlMonitorOutputs));
			writer.write(",");
			writer.write(String.format("%.2f", floatElapsedTimerLTL));
		} else {
			writer.write(",,");
		}

		writer.newLine();

	}

	/**
	 * Parses and returns command line arguments.
	 * 
	 * @param args the command line arguments
	 * @return Returns the parsed command line arguments
	 */
	static Optional<Config> parseCliArguments(String[] args) {
		Optional<Logic> useRLTL = Optional.empty();
		Optional<String> stats = Optional.empty();
		Optional<String> formula = Optional.empty();

		//
		// Parse
		//
		for (int i = 0; i < args.length; i += 1) {
			String arg = args[i];
			switch (arg.toLowerCase()) {
			case "--help":
			case "-h":
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
			case "--stats":
			case "-s":
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

	/**
	 * Enumeration defining for which logic a monitor is supposed to be constructed.
	 * 
	 * @author Maximilian Schwenger
	 *
	 */
	static enum Logic {
		LTL, rLTL, BOTH
	}

	/**
	 * Object representing the command line arguments.
	 * 
	 * @author Maximilian Schwenger
	 *
	 */
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
