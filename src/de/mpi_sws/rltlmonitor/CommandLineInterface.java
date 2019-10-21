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

//			// DEBUG
//			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out, "UTF-8"))) {
//				dumpStatsToWriter(writer, rltlmonitor, elapsedrLTLMonitorConstructionTime, ltlmonitor,
//						elapsedLTLMonitorConstructionTime);
//			}

		} catch (ParseException | IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (RuntimeException e) {
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
	 * @param rltlMonitorConstructionTime time required to construct the rLTL
	 *                                           monitor
	 * @param ltlMonitor                         an LTL monitor
	 * @param ltlMonitorConstructionTime  time required to construct the LTL
	 *                                           monitor
	 * @throws IOException
	 * @throws RuntimeException
	 */
	static void dumpStatsToWriter(BufferedWriter writer, FastMoore<BitSet, BitSet> rltlMonitor,
			long rltlMonitorConstructionTime, FastMoore<BitSet, BitSet> ltlMonitor,
			long ltlMonitorConstructionTime) throws RuntimeException, IOException {

		//
		// Dump statistics to stream
		//

		// LTL Monitor
		if (ltlMonitor != null) {
			
			// Number of outputs 
			HashSet<BitSet> ltlMonitorOutputs = new HashSet<>(10);
			if (ltlMonitor != null) {
				ltlMonitor.forEach(new Consumer<FastMooreState<BitSet>>() {

					@Override
					public void accept(FastMooreState<BitSet> state) {
						ltlMonitorOutputs.add((BitSet) state.getOutput().clone());
					}

				});
			}
			
			// Construction time
			float ltlConstructionTimeInSeconds = (float) (ltlMonitorConstructionTime / 1000) / 1000000f;
			
			// Write
			writer.write(String.valueOf(ltlMonitor.size()));
			writer.write(",");
			writer.write(String.valueOf(ltlMonitorOutputs.size()));
			writer.write(",");
			writer.write(String.valueOf(isMonitorable(ltlMonitor, false)));
			writer.write(",");
			writer.write(String.format("%.2f", ltlConstructionTimeInSeconds));
			
		} else {
			writer.write(",,,");
		}

		// rLTL monitor
		writer.write(",");
		if (rltlMonitor != null) {
			
			// Number of outputs
			HashSet<BitSet> rltlMonitorOutputs = new HashSet<>(10);
			if (rltlMonitor != null) {
				rltlMonitor.forEach(new Consumer<FastMooreState<BitSet>>() {
					@Override
					public void accept(FastMooreState<BitSet> state) {
						rltlMonitorOutputs.add((BitSet) state.getOutput().clone());
					}

				});
			}
			
			// Construction time
			float rltlConstructionTimeInSeconds = (float) (rltlMonitorConstructionTime / 1000) / 1000000f;
			
			// Write
			writer.write(String.valueOf(rltlMonitor.size()));
			writer.write(",");
			writer.write(String.valueOf(rltlMonitorOutputs.size()));
			writer.write(",");
			writer.write(String.valueOf(isMonitorable(rltlMonitor, true)));
			writer.write(",");
			writer.write(String.format("%.2f", rltlConstructionTimeInSeconds));
			
		} else {
			writer.write(",,,");
		}

		writer.newLine();

	}

	// Assumes that the monitor is minimized!
	public static boolean isMonitorable(FastMoore<BitSet, BitSet> monitor, boolean isrLTL) throws RuntimeException {
		
		int maxBit = isrLTL ? 4 : 1;
		
		for (var state : monitor.getStates()) {

			// Skip if output is not ????
			if (!(state.getOutput().get(0) && state.getOutput().get(maxBit))) {
				continue;
			}

			// Check if state is a self loop
			boolean is_sink_state = true;
			for (var bits : monitor.getInputAlphabet()) {

				var successor = monitor.getSuccessor(state, bits);

				if (successor == null) {
					throw new RuntimeException("Monitor is not complete");
				} else if (!successor.equals(state)) {
					is_sink_state = false;
					break;
				}

			}

			if (is_sink_state) {
				return false;
			}

		}
		
		return true;
		
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
	 */
	static enum Logic {
		LTL, rLTL, BOTH
	}

	/**
	 * Object representing the command line arguments.
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
