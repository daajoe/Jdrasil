package de.uniluebeck.tcs.algorithms.upperbounds;

/**
 * SimpleLocalSearchDecomposer.java
 * @author bannach
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.uniluebeck.tcs.App;
import de.uniluebeck.tcs.algorithms.EliminationOrderDecomposer;
import de.uniluebeck.tcs.graph.Graph;
import de.uniluebeck.tcs.graph.TreeDecomposer;
import de.uniluebeck.tcs.graph.TreeDecomposition;
import de.uniluebeck.tcs.graph.TreeDecomposition.TreeDecompositionQuality;

/**
 * This class implements a simple local search on the space of elimination orders.
 * The algorithms expects two error parameters r and s: the number of restarts and the number of steps.
 * To find a tree-decomposition the algorithms start r times with a random permutation, then it will
 * s times swap to random elements of this permutation and store if, and only if, this improves the decomposition.
 *
 * At the end, the best found tree-decomposition is returned.
 *
 * @param <T>
 */
public class SimpleLocalSearchDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	private static final long serialVersionUID = 259934854873290095L;

	/** The Graph that should be decomposed. */
	private final Graph<T> graph;
	
	/** The number of restarts. */
	private final int r;
	
	/** The number of steps per restarts. */
	private final int s;
	
	/** A random generator for the algorithm. */
	private final Random dice;
	
	/**
	 * Initialize the algorithm to decompose the given graph.
	 * @param graph
	 * @param r the number of restarts
	 * @param s the number of steps per restarts
	 */
	public SimpleLocalSearchDecomposer(Graph<T> graph, int r, int s) {
		this.graph = graph;
		this.r = r;
		this.s = s;
		this.dice = App.getSourceOfRandomness();
	}
	
	/**
	 * Use the Fisher-Yates algorithm to compute a random permutation of 
	 * vertices of the stored graph.
	 * @return random permutation of the vertices
	 */
	private List<T> randomPerm() {
		List<T> P = new ArrayList<>(graph.getVertices());
		for (int i = P.size()-1; i >= 0; i--) {
			int z = dice.nextInt(i+1);
			T tmp = P.get(i);
			P.set(i, P.get(z));
			P.set(z, tmp);
		}
		return P;
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		
		// catch the empty graph
		if (graph.getVertices().size() == 0) return new TreeDecomposition<T>(graph);
		
		// we try to optimize this
		TreeDecomposition<T> bestDecomposition = new EliminationOrderDecomposer<>(
				graph, randomPerm(), TreeDecompositionQuality.Heuristic
				).call();
		int bestWidth = Integer.MAX_VALUE;
		int n = graph.getVertices().size();
		
		// outer witness loop
		for (int i = 0; i < r; i++) {
			List<T> P = randomPerm();
			TreeDecomposition<T> dec = new EliminationOrderDecomposer<>(graph, P, TreeDecompositionQuality.Heuristic).call();
			
			// inner witness loop
			for (int j = 0; j < s; j++) {
				if (Thread.currentThread().isInterrupted()) throw new Exception();
				
				// make a random swap
				int a = dice.nextInt(n);
				int b = dice.nextInt(n);
				T tmp = P.get(a);
				P.set(a, P.get(b));
				P.set(b, tmp);
				
				// check if this make the local result better
				TreeDecomposition<T> dec_intern = new EliminationOrderDecomposer<>(graph, P, TreeDecompositionQuality.Heuristic).call();
				if (dec_intern.getWidth() < dec.getWidth()) {
					dec = dec_intern;
				} else {
					tmp = P.get(a);
					P.set(a, P.get(b));
					P.set(b, tmp);
				}
			}
			
			// check if the global result become better
			if (dec.getWidth() < bestWidth) {
				bestDecomposition = dec;
				bestWidth = dec.getWidth();
			}
		}
		
		// done
		return bestDecomposition;
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Heuristic;
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return null;
	}
	
}