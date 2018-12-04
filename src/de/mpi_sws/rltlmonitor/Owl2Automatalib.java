package de.mpi_sws.rltlmonitor;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import de.tum.in.naturals.bitset.BitSets;
import net.automatalib.automata.transout.impl.FastMoore;
import net.automatalib.automata.transout.impl.FastMooreState;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;
import owl.automaton.Automaton;
import owl.automaton.Views;
import owl.automaton.algorithms.EmptinessCheck;

public class Owl2Automatalib {

	/**
	 * Computes the product of two Moore machines in a naive way (i.e., constructs
	 * all pairs of states, even such that are unreachable). To combine the output
	 * of two states, the logical OR defined by the BitSet class is used.
	 * 
	 * @param first  The first Moore machine
	 * @param second The second moore machine
	 * @return The product of {@code first} and {@code second}
	 */
	public static FastMoore<BitSet, BitSet> naiveProduct(FastMoore<BitSet, BitSet> first,
			FastMoore<BitSet, BitSet> second) {

		assert (first.getInputAlphabet().equals(second.getInputAlphabet()));

		//
		// Create Moore machine
		//
		FastMoore<BitSet, BitSet> result = new FastMoore<>(first.getInputAlphabet());

		//
		// Create states
		//
		HashMap<FastMooreState<BitSet>, HashMap<FastMooreState<BitSet>, FastMooreState<BitSet>>> stateMap = new HashMap<>();
		for (var state1 : first.getStates()) {

			HashMap<FastMooreState<BitSet>, FastMooreState<BitSet>> tmpMap = new HashMap<>();
			stateMap.put(state1, tmpMap);

			for (var state2 : second.getStates()) {
				BitSet output = (BitSet) state1.getOutput().clone();
				output.or(state2.getOutput());
				tmpMap.put(state2, result.addState(output));
			}

		}

		//
		// Initial state
		//
		var combinedInitialState = stateMap.get(first.getInitialState()).get(second.getInitialState());
		result.setInitialState(combinedInitialState);

		//
		// Transitions
		//
		for (var state1 : first.getStates()) {
			for (var state2 : second.getStates()) {
				for (var input : first.getInputAlphabet()) {

					var successor1 = first.getSuccessor(state1, input);
					var successor2 = second.getSuccessor(state2, input);

					if (successor1 != null && successor2 != null) {
						result.addTransition(stateMap.get(state1).get(state2), input,
								stateMap.get(successor1).get(successor2));
					}

				}
			}
		}

		return result;
	}

	/**
	 * Converts an Owl omega-automaton to an Automatalib Moore machine as described
	 * in the paper. The parameter {@code bit} indicates to which bit of the rLTL
	 * formula this Moore machine refers to. This information is used to set the
	 * output accordingly. More precisely, the output is a BitSet containing
	 * {@code bit} if and only if the language of the Owl automaton starting from
	 * the given state is non-empty.
	 * <p>
	 * The Owl automaton must be deterministic (in particular, it must have a unique
	 * initial state).
	 * 
	 * @param owlAutomaton The Owl omega-automaton
	 * @param bit          The bit to which this automaton belongs to (must satisfy
	 *                     <code>0 <= bit <= 3</code>(
	 * @return The Moore machine as described in the paper
	 */
	public static FastMoore<BitSet, BitSet> toAutomatalib(Automaton<Object, ?> owlAutomaton, int bit) {

		assert (owlAutomaton.initialStates().size() == 1);
		assert (owlAutomaton.is(Automaton.Property.DETERMINISTIC));
		assert (0 <= bit && bit <= 3);

		//
		// Complete Owl automaton
		//
		var owlAutomatonCompleted = Views.complete(owlAutomaton, new Object());
		System.out.println("\n---------- Start Completed Owl automaton ----------");
		System.out.println(owl.automaton.output.HoaPrinter.toString(owlAutomatonCompleted));
		System.out.println("\n---------- End Completed Owl automaton ----------");

		//
		// Create alphabet
		//
		var owlAlphabet = BitSets.powerSet(owlAutomatonCompleted.factory().alphabetSize());

		ArrayList<BitSet> list = new ArrayList<>(owlAlphabet.size());
		for (var bits : owlAlphabet) {
			list.add((BitSet) bits.clone());
		}
		Alphabet<BitSet> automatalibAlphabet = Alphabets.fromList(list);

		//
		// Create Moore machine
		//
		FastMoore<BitSet, BitSet> result = new FastMoore<>(automatalibAlphabet);

		//
		// Create states
		//
		HashMap<Object, FastMooreState<BitSet>> stateMap = new HashMap<>(owlAutomatonCompleted.size());
		for (Object owlState : owlAutomatonCompleted.states()) {

			// Create AutomatonLib states
			var automatalibState = result.addState(new BitSet(4)); // We'll set the output later

			// Store mapping between Owl states and Brics states
			stateMap.put(owlState, automatalibState);

		}

		//
		// Define output of states
		//
		stateMap.forEach(new BiConsumer<Object, FastMooreState<BitSet>>() {

			@Override
			public void accept(Object owlState, FastMooreState<BitSet> automatalibState) {

				// Create view with given state as new initial state
				var changedInitialStateAutomaton = Views.replaceInitialState(owlAutomatonCompleted, Set.of(owlState));

				// Check whether language from this state is empty
				if (!EmptinessCheck.isEmpty(changedInitialStateAutomaton)) {
					automatalibState.getOutput().set(bit);
				}

			}

		});

		//
		// Initial state
		//
		result.setInitialState(stateMap.get(owlAutomatonCompleted.onlyInitialState()));

		//
		// Create transitions
		//
		for (Object owlState : owlAutomatonCompleted.states()) {
			for (var entry : owlAutomatonCompleted.edgeMap(owlState).entrySet()) {
				entry.getValue().forEach(new Consumer<BitSet>() {

					@Override
					public void accept(BitSet input) {
						result.setTransition(stateMap.get(owlState), input, stateMap.get(entry.getKey().successor()));
					}

				});
			}
		}

		return result;

	}

	/**
	 * Produces a textual representation of the given Moore machine in the Graphviz
	 * Dot format.
	 * 
	 * @param automatalibAutomaton The Moore machine to visualize
	 * @return a textual representation of the given Moore machine in the Graphviz
	 *         Dot format
	 */
	public static String toDot(FastMoore<BitSet, BitSet> automatalibAutomaton) {

		StringBuilder builder = new StringBuilder();

		//
		// Header
		//
		builder.append("digraph Moore {").append(System.lineSeparator());

		//
		// States
		//
		automatalibAutomaton.forEach(new Consumer<FastMooreState<?>>() {

			@Override
			public void accept(FastMooreState<?> state) {

				builder.append("  ").append(state.getId()).append(" [label=\"").append(state.getId()).append("; ")
						.append(state.getOutput()).append("\"];").append(System.lineSeparator());

			}

		});

		//
		// Initial States
		//
		for (var state : automatalibAutomaton.getInitialStates()) {
			builder.append("  ").append("initial").append(state.getId()).append(" [shape=plaintext,label=\"\"];")
					.append(System.lineSeparator());
			builder.append("  ").append("initial").append(state.getId()).append(" -> ").append(state.getId())
					.append(";").append(System.lineSeparator());
		}

		//
		// Transitions
		//
		for (var state : automatalibAutomaton.getStates()) {
			for (var bits : automatalibAutomaton.getInputAlphabet()) {

				var successor = automatalibAutomaton.getSuccessor(state, bits);

				if (successor != null) {
					builder.append("  ").append(state.getId()).append(" -> ").append(successor.getId())
							.append(" [label=\"").append(bits).append("\"];").append(System.lineSeparator());
				}

			}
		}

		//
		// Footer
		//
		builder.append("};");

		return builder.toString();

	}

}
