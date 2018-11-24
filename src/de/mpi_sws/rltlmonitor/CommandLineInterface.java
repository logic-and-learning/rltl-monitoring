package de.mpi_sws.rltlmonitor;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.util.EnumSet;

import org.mpi_sws.rltl.parser.LTLParser;
import org.mpi_sws.rltl.parser.ParseException;
import org.mpi_sws.rltl.visitors.PrettyPrintVisitor;
import org.mpi_sws.rltl.visitors.RLTL2LTLVisitor;

import net.automatalib.automata.transout.impl.FastMoore;
import net.automatalib.util.automata.Automata;

public class CommandLineInterface {

	/**
	 * Main method for rLTL monitor construction
	 * 
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		
		//
		// Check command line arguments
		//
		if (args.length != 1) {

			System.out.println("Invalid command line arguments.");
			System.out.println("Usage: rLTLmonitor formula");
			System.exit(1);

		}

		//
		// Do the monitor construction
		//
		try {

			var monitor = constructrLTLMonitor(args[0]);

			System.out.println("\n========== Final Monitor ==========\n");
			System.out.println(Owl2Automatalib.toDot(monitor));

		} catch (UnsupportedEncodingException | ParseException e) {
			e.printStackTrace();
		}

	}

	private static FastMoore<BitSet, BitSet> constructrLTLMonitor(String rLTLFormula)
			throws ParseException, UnsupportedEncodingException {

		//
		// Parse rLTL expressions
		//
		var parser = new LTLParser(new ByteArrayInputStream(rLTLFormula.getBytes("UTF-8")));

		@SuppressWarnings("static-access")
		var rLTLExpr = parser.expression();

		//
		// Convert to LTL expressions
		//
		var ltlExprs = RLTL2LTLVisitor.convert(rLTLExpr);

		//
		// Construct monitor
		//
		FastMoore<BitSet, BitSet> combined = null;
		for (int bit = 0; bit < 4; ++bit) {

			System.out.println("\n========== (bit=" + bit + ") ==========\n");

			//
			// Convert LTL expression to Owl automaton
			//
			var visitor = new PrettyPrintVisitor();

			var ltlString = visitor.expression2String(ltlExprs[bit]);
			System.out.println("LTL formula is: " + ltlString);

			var translator = new owl.translations.LTL2NAFunction(owl.run.DefaultEnvironment.standard(),
					EnumSet.allOf(owl.translations.LTL2NAFunction.Constructions.class));

			var owlAutomaton = owl.automaton.AutomatonUtil
					.cast(translator.apply(owl.ltl.parser.LtlParser.parse(ltlString)));
			System.out.println("\n---------- Start Owl automaton ----------");
			System.out.println(owl.automaton.output.HoaPrinter.toString(owlAutomaton));
			System.out.print("Alphabet: ");
			for (int i = 0; i < owlAutomaton.factory().alphabet().size(); ++i) {
				System.out.print((i == 0 ? "" : "; ") + owlAutomaton.factory().alphabet().get(i) + " (" + i + ")");
			}
			System.out.println("\n---------- End Owl automaton ----------");

			//
			// Convert to Moore machine
			//
			var automatalibMachine = Owl2Automatalib.toAutomatalib(owlAutomaton, bit);
			System.out.println("\n---------- Start Moore machine ----------");
			System.out.println(Owl2Automatalib.toDot(automatalibMachine));
			System.out.println("---------- End Moore machine ----------");

			//
			// Minimize Moore machine
			//
			Automata.invasiveMinimize(automatalibMachine, automatalibMachine.getInputAlphabet());
			System.out.println("\n---------- Start minimized Moore machine ----------");
			System.out.println(Owl2Automatalib.toDot(automatalibMachine));
			System.out.println("---------- End minimized Moore machine ----------");

			//
			// In first iteration, just store AutomataLib Moore machine
			//
			if (bit == 0) {
				combined = automatalibMachine;
			}

			//
			// Otherwise compute product with previous Moore machine
			//
			else {

				// Combine
				combined = Owl2Automatalib.naiveProduct(combined, automatalibMachine);
				System.out.println("\n---------- Start combined Moore machine ----------");
				System.out.println(Owl2Automatalib.toDot(combined));
				System.out.println("---------- End combined Moore machine ----------");

				// Minimize
				Automata.invasiveMinimize(combined, combined.getInputAlphabet());
				System.out.println("\n---------- Start minimized combined Moore machine ----------");
				System.out.println(Owl2Automatalib.toDot(combined));
				System.out.println("---------- End minimized combined Moore machine ----------");

			}

		}

		return combined;

	}

}
