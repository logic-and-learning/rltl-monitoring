package de.mpi_sws.rltlmonitor;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.util.Set;

import org.mpi_sws.rltl.parser.LTLParser;
import org.mpi_sws.rltl.parser.ParseException;
import org.mpi_sws.rltl.visitors.PrettyPrintVisitor;
import org.mpi_sws.rltl.visitors.RLTL2LTLVisitor;

import net.automatalib.automata.transout.impl.FastMoore;
import net.automatalib.util.automata.Automata;
import owl.automaton.Automaton;
import owl.automaton.AutomatonUtil;
import owl.ltl.parser.LtlParser;
import owl.run.DefaultEnvironment;
import owl.translations.ltl2dpa.LTL2DPAFunction;

/**
 * This class implements the (r)LTL monitor construction.
 * 
 * @author Daniel Neider
 *
 */
public class MonitorConstructor {

	/**
	 * Constructs the unique LTL monitor given an LTL formula (according to Brauer
	 * et al.).
	 * 
	 * @param ltlFormula The LTL formula to construct the monitor from
	 * @return the unique LTL monitor corresponding to the given LTL formula
	 * @throws ParseException 
	 */
	public static FastMoore<BitSet, BitSet> constructLTLMonitor(String ltlFormula) throws ParseException {

		//
		// Use the rLTL2LTL parser in order to avoid inconsistencies in operator precedence
		//
		LTLParser parser = new LTLParser(new BufferedReader(new StringReader(ltlFormula)));
		var expr = parser.expression();
		var parsedLTLFormula = (new PrettyPrintVisitor()).expression2String(expr);
		
		//
		// Negate LTL formula
		//
		String negatedLTLFormula = "!(" + parsedLTLFormula + ")";

		//
		// Owl automaton for negated formula
		//
		var environment = DefaultEnvironment.standard();
		var translator = new LTL2DPAFunction(environment,
				Set.of(LTL2DPAFunction.Configuration.OPTIMISE_INITIAL_STATE, LTL2DPAFunction.Configuration.COMPLETE,
						LTL2DPAFunction.Configuration.EXISTS_SAFETY_CORE,
						LTL2DPAFunction.Configuration.COMPRESS_COLOURS));

		var negatedOwlAutomaton = AutomatonUtil.cast(translator.apply(LtlParser.parse(negatedLTLFormula)));
		assert (negatedOwlAutomaton.is(Automaton.Property.COMPLETE));
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
		var originalOwlAutomaton = AutomatonUtil.cast(translator.apply(LtlParser.parse(parsedLTLFormula)));
		assert (originalOwlAutomaton.is(Automaton.Property.COMPLETE));
		System.out.println("Original formula: " + parsedLTLFormula);
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

	/**
	 * Constructs the unique rLTL monitor given an rLTL formula.
	 * 
	 * @param rLTLFormula The rLTL formula to construct the monitor from
	 * @return the unique rLTL monitor corresponding to the given rLTL formula
	 * @throws ParseException               Throws this exception if the formula
	 *                                      cannot be parsed
	 * @throws UnsupportedEncodingException Throws this exception if the formula is
	 *                                      not an appropriate character encoding
	 */
	public static FastMoore<BitSet, BitSet> constructrLTLMonitor(String rLTLFormula)
			throws ParseException, UnsupportedEncodingException {

		//
		// Parse rLTL expressions
		//
		var parser = new LTLParser(new ByteArrayInputStream(rLTLFormula.getBytes("UTF-8")));
		var rLTLExpr = parser.expression();

		//
		// Convert to LTL expressions
		//
		var ltlExprs = RLTL2LTLVisitor.convert(rLTLExpr);

		//
		// Convert LTL expression to Strings
		//
		var visitor = new PrettyPrintVisitor();

		var ltlStrings = new String[] { visitor.expression2String(ltlExprs[0]), visitor.expression2String(ltlExprs[1]),
				visitor.expression2String(ltlExprs[2]), visitor.expression2String(ltlExprs[3]) };

		//
		// Construct monitor
		//
		FastMoore<BitSet, BitSet> combinedMachine = null;
		for (int truthValue = 0; truthValue < 5; ++truthValue) {

			System.out.println("\n========== (truth value=" + truthValue + ") ==========\n");

			//
			// Generate LTL expression
			//
			String ltlString = null;
			if (truthValue == 0) {
				ltlString = ltlStrings[0];
			} else if (truthValue == 1) {
				ltlString = "!(" + ltlStrings[0] + ") & (" + ltlStrings[1] + ")";
			} else if (truthValue == 2) {
				ltlString = "!(" + ltlStrings[1] + ") & (" + ltlStrings[2] + ")";
			} else if (truthValue == 3) {
				ltlString = "!(" + ltlStrings[2] + ") & (" + ltlStrings[3] + ")";
			} else {
				ltlString = "!(" + ltlStrings[3] + ")";
			}
			System.out.println("LTL formula is: " + ltlString);

			//
			// Convert LTL expression to Owl automaton
			//
			// var translator = new
			// owl.translations.LTL2DAFunction(owl.run.DefaultEnvironment.standard(), false,
			// EnumSet.allOf(owl.translations.LTL2DAFunction.Constructions.class));
			// var translator = new LTL2DAFunction(DefaultEnvironment.standard(), false,
			// EnumSet.of(LTL2DAFunction.Constructions.RABIN));
			var environment = DefaultEnvironment.standard();
			var translator = new LTL2DPAFunction(environment,
					Set.of(LTL2DPAFunction.Configuration.OPTIMISE_INITIAL_STATE, LTL2DPAFunction.Configuration.COMPLETE,
							LTL2DPAFunction.Configuration.EXISTS_SAFETY_CORE,
							LTL2DPAFunction.Configuration.COMPRESS_COLOURS));

			var owlAutomaton = owl.automaton.AutomatonUtil
					.cast(translator.apply(owl.ltl.parser.LtlParser.parse(ltlString)));
			assert (owlAutomaton.is(Automaton.Property.COMPLETE));
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
			var automatalibMachine = Owl2Automatalib.toAutomatalib(owlAutomaton, truthValue);
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
			if (truthValue == 0) {
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
