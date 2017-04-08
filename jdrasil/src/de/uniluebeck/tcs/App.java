package de.uniluebeck.tcs;

/**
 * App.java
 * @author bannach
 */
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.uniluebeck.tcs.algorithms.ExactDecomposer;
import de.uniluebeck.tcs.algorithms.HeuristicDecomposer;
import de.uniluebeck.tcs.graph.Graph;
import de.uniluebeck.tcs.graph.GraphFactory;
import de.uniluebeck.tcs.graph.TreeDecomposition;
import de.uniluebeck.tcs.sat.encodings.GenericCardinalityEncoder;

/**
 * Jdrasil is a program to compute a small tree-decomposition of a given graph.
 * It is developed at the Universitaet zu Luebeck in context of the PACE challenge (www.pacechallenge.wordpress.com).
 */
public class App {

	/** Version of the program. */
	private static final float VERSION = 1f;
	
	/** The random source of the whole program. */
	private static Random dice;
	
	/** The programs parameter, can accessed from everywhere. */
	public static final Map<String, String> parameters = new HashMap<>();
	
	/**
	 * Remember if the result has been written already
	 */
	private static boolean resultWritten;
	
	/** Entry point! */
	public static void main(String[] args) {
            
		// parsing arguments
		parseArguments(args);
		
		// initialize the source of randomness
		if (parameters.containsKey("s")) {
			dice = new Random(Long.parseLong(parameters.get("s")));
		} else {
			dice = new Random(System.currentTimeMillis());
		}

		// set encoding for SAT-solver
		if (parameters.containsKey("e")) {
			setSATEncoding(parameters.get("e"));
		}
				
		try{
			Graph<Integer> input = GraphFactory.graphFromStdin();
			
			/* Compute a explicit decomposition */
			long tstart = System.nanoTime();
			TreeDecomposition<Integer> decomposition = null;	

			if (parameters.containsKey("heuristic")) {
				
				/* compute a tree-decomposition heuristically */				
				HeuristicDecomposer<Integer> heuristic = new HeuristicDecomposer<Integer>(input, parameters.containsKey("parallel"));
				decomposition = heuristic.call();
				
			} else {
				
				/* Default case: compute an exact tree-decomposition */	
				ExactDecomposer<Integer> exact = new ExactDecomposer<Integer>(input, parameters.containsKey("parallel"));				
				decomposition = exact.call();
			}
			long tend = System.nanoTime();
			
					
			/* present the result */
			if (parameters.containsKey("tikz")) {
				System.out.println(input.toTikZ());
				System.out.println();
				System.out.println(decomposition.toTikZ());
			} else {
				if(shouldIWrite()) {
					System.out.print(decomposition);
					System.out.println();
				}
			}
			App.log("");
			App.log("Tree-Width: " + decomposition.getWidth());
			App.log("Used " + (tend-tstart)/1000000000 + " seconds");
			App.log("");
			
		} 
		catch (IOException e) {
			e.printStackTrace();
			System.out.println("c Could not read the graph file.");			
		}
		catch (Exception e) {
			System.out.println("c Error during the computation of the decomposition.");
			System.out.println();
			e.printStackTrace();
		}
	}

	/**
	 * Parsing the programs argument and store them in parameter map.
	 * @ see parameters
	 * @param args
	 */
	public static void parseArguments(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String a = args[i];
			if (a.charAt(0) == '-') {
				
				// help is a special case
				if (a.equals("-h")) {
					printHelp();
					continue;
				}

				// catch format errors
				if (a.length() < 2 || (a.length() == 2 && i == args.length-1)) {
					System.err.println("Error parsing arguments.");
					System.exit(-1);
				}
				
				if (a.length() == 2) { // arguments of length one are followed by a value					
					parameters.put(a.substring(1, a.length()), args[i+1]);
				} else { // others are just flags
					parameters.put(a.substring(1, a.length()), "");
				}
			}
		}
	}
	
	public static synchronized boolean shouldIWrite(){
		boolean rVal = true;
		if(resultWritten){
			rVal = false;
		}
		resultWritten = true;
		return rVal;
	}
	
	/**
	 * This method should be used by any-time-algorithms to report whenever they found a new solution.
	 * This is a requirement by the PACE challenge.
	 * @param tw
	 */
	public static void reportNewSolution(int tw) {
		if (!parameters.containsKey("heuristic")) return; // only for heuristic
		System.out.println("c status " + (tw+1) + " " + System.currentTimeMillis());
	}
	
	/**
	 * Print a static help message.
	 */
	public static void printHelp() {
		System.out.println("Jdrasil");
		System.out.println("Authors: Max Bannach, Sebastian Berndt, and Thorsten Ehlers");
		System.out.println("Version: " + VERSION);
		System.out.println();
		System.out.println("Parameters:");
		System.out.println("  -h : print this dialog");
		System.out.println("  -s <seed> : set a random seed");
		System.out.println("  -parallel : enable parallel processing");
		System.out.println("  -heuristic : compute a heuristic solution");
		System.out.println("  -log : enable log output");
		System.out.println("  -tikz : enable tikz output");
		System.out.println("  -e <encoding> : set a cardinality encoding for SAT-solver");
		System.out.println("     ( 1) binomial");
		System.out.println("     ( 2) sequential");
		System.out.println("     ( 3) binary");
		System.out.println("     ( 4) commander");
		System.out.println("     ( 5) PBLib");
		System.out.println("     ( 6) PBLib_incremental");
	}
	
	/**
	 * Returns the source of randomness of this program.
	 * This should be the only randomness used in order to
	 * make the program depend on a single seed.
	 * @return
	 */
	public static Random getSourceOfRandomness() {
		return dice;
	}
	
	/**
	 * 	Get the random seed
	 */
	public static long getSeed(){
		if (parameters.containsKey("s")) {
			return Long.parseLong(parameters.get("s"));
		} else {
			return System.currentTimeMillis();
		}
	}

    /**
     * 	Get the random seed
     */
    public static Integer getTimeout(){
        if (parameters.containsKey("t")) {
            return Integer.parseInt(parameters.get("t"));
        } else {
            return null;
        }
    }
	
	/**
	 * Log a message as comment (with a leading 'c') to the output. 
	 * Does only work if logging is enabled.
	 * @param message
	 */
	public static void log(String message) {
		if (parameters.containsKey("log")) {
			System.out.println("c " + message);
		}
	}
		
	/**
	 * Set a new seed for the random source.
	 * @param seed
	 */
	public static void seedRandomSource(Long seed) {
		dice = new Random(seed);
	}
	
	/**
	 * Set the standard encoding for the at-most-k constraint used by different SAT-encodings.
	 * @param encoding
	 */
	public static void setSATEncoding(String encoding) {
		switch (encoding) {
		case "binomial":
			GenericCardinalityEncoder.usedEncoding = GenericCardinalityEncoder.Encoding.BINOMIAL;
			break;
		case "sequential":
			GenericCardinalityEncoder.usedEncoding = GenericCardinalityEncoder.Encoding.SEQUENTIAL;
			break;
		case "binary":
			GenericCardinalityEncoder.usedEncoding = GenericCardinalityEncoder.Encoding.BINARY;
			break;
		case "commander":
			GenericCardinalityEncoder.usedEncoding = GenericCardinalityEncoder.Encoding.COMMANDER;
			break;
		case "PBLib":
			GenericCardinalityEncoder.usedEncoding = GenericCardinalityEncoder.Encoding.PBLIB;
			break;
		case "PBLib_incremental":
			GenericCardinalityEncoder.usedEncoding = GenericCardinalityEncoder.Encoding.PBLIB_INCREMENTAL;
			break;
		}
	}
	
	/**
	 * Auxiliary method to compute n choose k with BigIntegers.
	 * @param n
	 * @param k
	 * @return
	 */
	public static BigInteger binom(BigInteger n, BigInteger k) {
		if (k.compareTo(n) > 0) return BigInteger.ZERO;
		if (k.compareTo(BigInteger.ZERO) == 0) {
			return BigInteger.ONE;
		} else if (k.multiply(new BigInteger(""+2)).compareTo(n) > 0) { 
			return binom(n, n.subtract(k));
		}
		
		BigInteger result = n.subtract(k).add(BigInteger.ONE);		
		for (BigInteger i = new BigInteger(""+2); i.compareTo(k) <= 0.; i = i.add(BigInteger.ONE)) {
			result = result.multiply(n.subtract(k).add(i));
			result = result.divide(i);
		}
		return result;
	}
	
}
