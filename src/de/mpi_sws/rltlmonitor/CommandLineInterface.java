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
import owl.automaton.AutomatonUtil;
import owl.ltl.parser.LtlParser;
import owl.run.DefaultEnvironment;
import owl.translations.LTL2DAFunction;

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
//		try {
//
//			var monitor = constructrLTLMonitor(args[0]);
//
//			System.out.println("\n========== Final Monitor ==========\n");
//			System.out.println(Owl2Automatalib.toDot(monitor));
//
//		} catch (UnsupportedEncodingException | ParseException e) {
//			e.printStackTrace();
//		}

		var monitor = constructLTLMonitor(args[0]);

		System.out.println("\n========== Final Monitor ==========\n");
		System.out.println(Owl2Automatalib.toDot(monitor));

	}

	public static FastMoore<BitSet, BitSet> constructLTLMonitor(String ltlFormula) {

		//
		// Negate LTL formula
		//
		String negatedLTLFormula = "!(" + ltlFormula + ")";

		//
		// Owl automaton for negated formula
		//
		var translator = new LTL2DAFunction(DefaultEnvironment.standard(), false,
				EnumSet.of(LTL2DAFunction.Constructions.PARITY));

		var negatedOwlAutomaton = AutomatonUtil.cast(translator.apply(LtlParser.parse(negatedLTLFormula)));
		System.out.println("Negated formula: " + negatedLTLFormula);
		System.out.println("\n---------- Start Owl automaton ----------");
		System.out.println(owl.automaton.output.HoaPrinter.toString(negatedOwlAutomaton));
		System.out.print("Alphabet: ");
		for (int i = 0; i < negatedOwlAutomaton.factory().alphabet().size(); ++i) {
			System.out.print((i == 0 ? "" : "; ") + negatedOwlAutomaton.factory().alphabet().get(i) + " (" + i + ")");
		}
		System.out.println("\n---------- End Owl automaton ----------");

		//
		// Convert to Moore machine
		//
		var negatedMachine = Owl2Automatalib.toAutomatalib(negatedOwlAutomaton, 0);
		System.out.println("\n---------- Start Moore machine ----------");
		System.out.println(Owl2Automatalib.toDot(negatedMachine));
		System.out.println("---------- End Moore machine ----------");

		//
		// Minimize Moore machine
		//
		Automata.invasiveMinimize(negatedMachine, negatedMachine.getInputAlphabet());
		System.out.println("\n---------- Start minimized Moore machine ----------");
		System.out.println(Owl2Automatalib.toDot(negatedMachine));
		System.out.println("---------- End minimized Moore machine ----------");

		//
		// Owl automaton for original formula
		//
		var originalOwlAutomaton = AutomatonUtil.cast(translator.apply(LtlParser.parse(ltlFormula)));
		System.out.println("Original formula: " + ltlFormula);
		System.out.println("\n---------- Start Owl automaton ----------");
		System.out.println(owl.automaton.output.HoaPrinter.toString(originalOwlAutomaton));
		System.out.print("Alphabet: ");
		for (int i = 0; i < originalOwlAutomaton.factory().alphabet().size(); ++i) {
			System.out.print((i == 0 ? "" : "; ") + originalOwlAutomaton.factory().alphabet().get(i) + " (" + i + ")");
		}
		System.out.println("\n---------- End Owl automaton ----------");

		//
		// Convert to Moore machine
		//
		var originalMachine = Owl2Automatalib.toAutomatalib(originalOwlAutomaton, 1);
		System.out.println("\n---------- Start Moore machine ----------");
		System.out.println(Owl2Automatalib.toDot(originalMachine));
		System.out.println("---------- End Moore machine ----------");

		//
		// Minimize Moore machine
		//
		Automata.invasiveMinimize(originalMachine, originalMachine.getInputAlphabet());
		System.out.println("\n---------- Start minimized Moore machine ----------");
		System.out.println(Owl2Automatalib.toDot(originalMachine));
		System.out.println("---------- End minimized Moore machine ----------");

		//
		// Compute product
		//
		var combinedMachine = Owl2Automatalib.naiveProduct(negatedMachine, originalMachine);
		System.out.println("\n---------- Start combined Moore machine ----------");
		System.out.println(Owl2Automatalib.toDot(combinedMachine));
		System.out.println("---------- End combined Moore machine ----------");

		// Minimize
		Automata.invasiveMinimize(combinedMachine, combinedMachine.getInputAlphabet());
		System.out.println("\n---------- Start minimized combined Moore machine ----------");
		System.out.println(Owl2Automatalib.toDot(combinedMachine));
		System.out.println("---------- End minimized combined Moore machine ----------");

		return combinedMachine;

	}

	public static FastMoore<BitSet, BitSet> constructrLTLMonitor(String rLTLFormula)
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
		FastMoore<BitSet, BitSet> combinedMachine = null;
		for (int bit = 0; bit < 4; ++bit) {

			System.out.println("\n========== (bit=" + bit + ") ==========\n");

			//
			// Convert LTL expression to Owl automaton
			//
			var visitor = new PrettyPrintVisitor();

			var ltlString = visitor.expression2String(ltlExprs[bit]);
			System.out.println("LTL formula is: " + ltlString);

			// var translator = new
			// owl.translations.LTL2DAFunction(owl.run.DefaultEnvironment.standard(), false,
			// EnumSet.allOf(owl.translations.LTL2DAFunction.Constructions.class));
			var translator = new LTL2DAFunction(DefaultEnvironment.standard(), false,
					EnumSet.of(LTL2DAFunction.Constructions.PARITY));

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
				combinedMachine = automatalibMachine;
			}

			//
			// Otherwise compute product with previous Moore machine
			//
			else {

				// Combine
				combinedMachine = Owl2Automatalib.naiveProduct(combinedMachine, automatalibMachine);
				System.out.println("\n---------- Start combined Moore machine ----------");
				System.out.println(Owl2Automatalib.toDot(combinedMachine));
				System.out.println("---------- End combined Moore machine ----------");

				// Minimize
				Automata.invasiveMinimize(combinedMachine, combinedMachine.getInputAlphabet());
				System.out.println("\n---------- Start minimized combined Moore machine ----------");
				System.out.println(Owl2Automatalib.toDot(combinedMachine));
				System.out.println("---------- End minimized combined Moore machine ----------");

			}

		}

		return combinedMachine;

	}

}
