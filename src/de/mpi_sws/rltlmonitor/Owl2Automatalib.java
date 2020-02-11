package de.mpi_sws.rltlmonitor;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import de.tum.in.naturals.bitset.BitSets;
import net.automatalib.automata.transducers.impl.FastMoore;
import net.automatalib.automata.transducers.impl.FastMooreState;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;
import owl.automaton.Automaton;
import owl.automaton.Views;
import owl.automaton.algorithms.LanguageEmptiness;

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
	 * Converts an Owl ω-automaton to an Automatalib Moore machine as described in
	 * the paper.
	 * 
	 * More precisely, the transition structure is copied (i.e., the resulting Moore
	 * machine has the same transition structure). Moreover, the output of states is
	 * defined as follows: if the language from a state of the ω-automaton is
	 * non-empty, the output of the Moore machine is a singleton set containing the
	 * argument {@code output}; otherwise, the output is the empty set.
	 * <p>
	 * The Owl automaton must be deterministic (in particular, it must have a unique
	 * initial state).
	 * 
	 * @param owlAutomaton The Owl ω-automaton
	 * @param output       The output to set if the language from a state is
	 *                     non-empty
	 * @return The Moore machine as described in the paper
	 */
	public static FastMoore<BitSet, BitSet> toAutomatalib(Automaton<Object, ?> owlAutomaton, int output) {

		assert (owlAutomaton.initialStates().size() == 1);
		assert (owlAutomaton.is(Automaton.Property.DETERMINISTIC));
		assert (0 <= output && output <= 4);

		//
		// Create alphabet
		//
		var owlAlphabet = BitSets.powerSet(owlAutomaton.factory().alphabetSize());

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
		HashMap<Object, FastMooreState<BitSet>> stateMap = new HashMap<>(owlAutomaton.size());
		for (Object owlState : owlAutomaton.states()) {

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
				var changedInitialStateAutomaton = Views.replaceInitialState(owlAutomaton, Set.of(owlState));

				// Check whether language from this state is empty
				if (!LanguageEmptiness.isEmpty(changedInitialStateAutomaton)) {
					automatalibState.getOutput().set(output);
				}

			}

		});

		//
		// Initial state
		//
		result.setInitialState(stateMap.get(owlAutomaton.onlyInitialState()));

		//
		// Create transitions
		//
		for (Object owlState : owlAutomaton.states()) {
			for (var entry : owlAutomaton.edgeMap(owlState).entrySet()) {
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
