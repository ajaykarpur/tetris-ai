import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;

public class PlayerSkeleton {
	 
	/* Number of features and feature indices in the vector. */
	private final static int NUM_FEATURES = 4;
	private final static int ROWS_CLEARED = 0;
	private final static int HOLES = 1;
	private final static int BUMPINESS = 2;
	private final static int HEIGHT = 3;
	
	private static Random RANDOM = new Random();
	
	/* Each individual plays this number of games per generations. */
	private final static int NUM_GAMES_PER_GEN = 15;
	
	/**
	 * A pair of integers. Java does not provide a generic pair class.
	 */
	private class Coord {
		public int r, c;
		public Coord(int _r, int _c) {
			r = _r;
			c = _c;
		}
	}
	
	/**
	 * Extended state class. Provides methods to test a move without
	 * actually making it, and computing heuristic values.
	 */
	private class StateEx extends State {
		//Copy of the "top" array from the super-class
		//This is so that we don't have to modify the original version.
		int[] topCopy;
		//Whether a given row is full or not.
		private boolean[] fullRow;
		//Coordinates of the piece that was just played.
		private LinkedList<Coord> piecePosition = new LinkedList<Coord>();
		int[] latestHeuristics = new int[4];
		
		//Get the number of holes (empty tiles with at least one full tile above
		//them in the same column) for the current board.
		private int getHoles() {
			int[][] field = getField();
			int holes = 0;
			
			/* From the top, go down until we reach a non-empty, non-full-row tile.
			 * Then, every empty tile is a hole. */
			for (int col = 0; col < State.COLS; col++){
				boolean countHoles = false;
				for(int row = State.ROWS - 1 ; row >= 0 ; row--) {
					if(!countHoles && field[row][col] != 0 && !fullRow[row])
						countHoles = true;
					else if(countHoles && field[row][col] == 0)
						holes++;
				}
			}

			return holes;
		}
		
		/* Obtain the bumpiness (sum of differences between consecutive columns)
		 * and aggregate height (sum of heights of all columns) of the current board.
		 * Return a two-element array. First element is the bumpiness, second element
		 * is the aggregateHeight.
		 */
		private int[] getBumpinessAndHeight() {			
			int bumpiness = 0;
			int aggregateHeight = topCopy[0];
			for (int i = 1; i < topCopy.length; i ++)
			{
				bumpiness += Math.abs(topCopy[i] - topCopy[i-1]);
				aggregateHeight += topCopy[i];
			}
			return new int[] {bumpiness, aggregateHeight};
		}
		
		/* Test the given move against the current board. Compute the
		 * score of the resulting move with the given weights for each
		 * heuristic. The state itself is not modified. */
		private float testMove(int orient, int slot, float[] weights) {
			this.topCopy = Arrays.copyOf(this.getTop(), COLS);
			this.fullRow = new boolean[ROWS];
			piecePosition.clear();
			
			int piece = this.nextPiece;
			int rowsCleared = dryRunMove(piece, orient, slot);
			
			if(rowsCleared == -1) //If we lost the game, return minimal value for this move.
				return Integer.MIN_VALUE;
			
			int[] bumpinessAndHeight = getBumpinessAndHeight();
			latestHeuristics[ROWS_CLEARED] = rowsCleared;
			latestHeuristics[HOLES] = getHoles();
			latestHeuristics[BUMPINESS] = bumpinessAndHeight[0];
			latestHeuristics[HEIGHT] = bumpinessAndHeight[1];
			
			//score/evaluation function is dot product of heuristics[4] and weights[4]
			float score = 0;
			for (int i = 0; i < latestHeuristics.length; i++)
				score += latestHeuristics[i] * weights[i];
			
			//Reset the field
			int[][] field = getField();
			for(Coord c: piecePosition)
					field[c.r][c.c] = 0;
			
			return score;
			
		}
		
		/* Play the given move on our local copy of the board.
		 * Return the number of rows cleared by the move, or -1
		 * if the move makes us lose the game.
		 * 
		 * Most of the code of this method is the same as "makeMove"
		 * in the State class.
		 */
		private int dryRunMove(int piece, int orient, int slot) {
			/* Note that here we want to modify the field as little as possible,
			 * to roll back our changes easily. Copying the field and playing on
			 * the copy is a very inefficient operation (earlier profiling showed
			 * that with that method, copying took over 99% of the runtime of the
			 * algorithm).
			 * 
			 * In particular, we don't "slide down" the bricks when a row is full.
			 * This is not a problem when computing the heuristics.
			 */
			
			int[][] field = getField();
			//height if the first column makes contact
			int height = topCopy[slot]-State.getpBottom()[piece][orient][0];
			//for each column beyond the first in the piece
			for(int c = 1; c < State.getpWidth()[piece][orient];c++) {
				height = Math.max(height,topCopy[slot+c]-State.getpBottom()[piece][orient][c]);
			}
			
			//If we lost, return -1 for the number of rows cleared.
			if(height+State.getpHeight()[piece][orient] >= ROWS)
				return -1;
			
			//for each column in the piece - fill in the appropriate blocks
			for(int i = 0; i < State.getpWidth()[piece][orient]; i++)
				for(int h = height+State.getpBottom()[piece][orient][i]; h < height+State.getpTop()[piece][orient][i]; h++) {
					field[h][i+slot] = -1;
					//Remember that we modified this to clear it later
					piecePosition.add(new Coord(h, i+slot));
				}
			
			//adjust top
			for(int c = 0; c < State.getpWidth()[piece][orient]; c++) {
				topCopy[slot+c]=height+State.getpTop()[piece][orient][c];
			}
			
			//check if game ended
			if(height+State.getpHeight()[piece][orient] >= ROWS)
				return 0;
			
			int rowsCleared = 0;
			
			//check for full rows - starting at the top
			for(int r = height+State.getpHeight()[piece][orient]-1; r >= height; r--) {
				//check all columns in the row
				boolean full = true;
				for(int c = 0; c < COLS; c++) {
					if(field[r][c] == 0) {
						full = false;
						break;
					}
				}
				//if the row was full - record it and update the top for the columns.
				if(full) {
					fullRow[r] = true;
					rowsCleared++;					
					for(int c = 0; c < COLS; c++) {
						//lower the top
						topCopy[c]--;
						while(topCopy[c]>=1 && field[topCopy[c]-1][c]==0)	topCopy[c]--;
					}
				}
				
			}
			
			return rowsCleared;
		}
		
	}

	/**
	 * An individual of the genetic algorithm. Contains a game
	 * state, the features (i.e. weights of the heuristics) and the
	 * ability to play a game.
	 */
	private class Individual implements Comparable<Individual> {
		public float[] features = new float[NUM_FEATURES];
		public float fitness;
		public StateEx state = new StateEx();
		
		private float EPSILON = 0.0001f;
		
		/**
		 * @param random Indicates whether the features should be initialized to random values or not.
		 */
		public Individual(boolean random) {
			fitness = 0;
			if(random) {
				for(int i = 0 ; i < NUM_FEATURES ; i++) {
					//In the actual project we should use this:
					if(i == ROWS_CLEARED){
						features[i] = RANDOM.nextFloat();
					}
					else{
						features[i] = (-1f)*RANDOM.nextFloat();
					}
					
					
				}
			} else {
				features[ROWS_CLEARED] = 0.760666f;
				features[BUMPINESS] = -0.184483f;
				features[HEIGHT] = -0.510066f;
				features[HOLES] = -0.35663f;
			}
		}
		
		/**
		 * Compare this Individual with another. The natural ordering 
		 * of individuals is decreasing order of fitness: An individual
		 * with larger fitness is "smaller" so that it sorts first. 
		 */
		public int compareTo(Individual a) {
			//"Natural ordering" means larger fitness first
			if(Math.abs(fitness - a.fitness) < EPSILON)
				return 0;
			else if(fitness > a.fitness)
				return -1;
			return 1;
		}
		
		/**
		 * Return a string representation of this Individual
		 * (features and fitness).
		 */
		public String toString() {
			return Arrays.toString(this.features) + " (fitness " + fitness + ")";
		}
		
		public void resetState() {
			state = new StateEx();
		}
		
		/**
		 * Have this individual play one game, using on its features.
		 * @return The number of rows cleared this game.
		 */
		public int play() {
			
			float maxScore;
			int bestMove;
			
			while(!state.hasLost()) {
				int[][] legalMoves = state.legalMoves();
				maxScore = Float.NEGATIVE_INFINITY;
				bestMove = -1;
				
				/* Test every move against the board, and pick the one
				 * that maximizes the score of the resulting board,
				 * according to our own weights. */
				for (int i = 0; i < legalMoves.length; i++){
					float moveScore = this.state.testMove(legalMoves[i][State.ORIENT], 
														legalMoves[i][State.SLOT], 
														this.features);
					if (moveScore > maxScore){
						maxScore = moveScore;
						bestMove = i;
					}
				}

				state.makeMove(legalMoves[bestMove]);
			}
			
			return state.getRowsCleared();
		}
		
	}
	
	public static void main(String[] args) {
		PlayerSkeleton p = new PlayerSkeleton();
		
		if(args.length > 0 && args[0].equals("-g")) {
			p.genetic(2, 20, 0.01f);
			return;
		}
		
		/*StateEx s = p.new StateEx();
		
		new TFrame(s);
		
		while(!s.hasLost()) {
			//s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");*/
	}
	
	/* Compute the fitness of this individual.
	 * Fitness is defined as the sum of rows cleared
	 * over a certain number of games. */
	private void fitness(Individual in) {
		int totalFitness = 0;
		for(int i = 0 ; i < NUM_GAMES_PER_GEN ; i++) {
			totalFitness += in.play();
			in.resetState();
		}
		
		in.fitness = totalFitness;
	}
	
	/* ==========================================
	 * GENERIC GENETIC ALGORITHM STUFF DOWN HERE. 
	 * ======================================= */
	
	/* Take the given parent individuals and generate a set
	 * of new individuals that inherit their features
	 * from the parents. */
	private Individual[] combine(Individual[] gen) {
		/* For each pair of individuals, randomly select features from
		 * either one or the other to assign to each new individual. */
		Individual[] newGen = new Individual[gen.length];
		for(int i = 0 ; i < gen.length - 1 ; i += 2) {
			Individual a = new Individual(false);
			Individual b = new Individual(false);
			
			/* Flip a coin on every feature to determine which 
			 * child should inherit from whom. */
			for(int j = 0 ; j < gen[i].features.length ; j++) {
				if(RANDOM.nextBoolean()) {
					a.features[j] = gen[i].features[j];
					b.features[j] = gen[i+1].features[j];
				} else {
					a.features[j] = gen[i+1].features[j];
					b.features[j] = gen[i].features[j];
				}
			}

			newGen[i] = a;
			newGen[i+1] = b;
		}
		return newGen;
	}
	
	private void mutate(Individual[] gen, final float mutation) {
		//Go through the features of each individual and mutate it according to the mutation rate
		for(int i = 0 ; i < gen.length ; i++) {
			for(int j = 0 ; j < gen[i].features.length ; j++) {
				if(RANDOM.nextFloat() < mutation) {
					//Again here for the purposes of the example I'm mutating by an integer value
					//Change this by a random float between 0 and 1 (maybe with a factor of .5, .25?)
					float amt = 0.f;
					while(amt == 0.f)
						amt = (RANDOM.nextBoolean() ? 1.f : -1.f) * 0.005f * RANDOM.nextFloat();
					
					gen[i].features[j] += amt;
					if(gen[i].features[j] > 1.f)
						gen[i].features[j] = 1.f;
					else if(gen[i].features[j] < -1.f)
						gen[i].features[j] = -1.f;
				}
			}
		}
	}
	
	/* Run a genetic algorithm. Create num_gens generations of gen_size individuals,
	 * with a mutation rate of mutation. At every generation, the fitness of every
	 * individual is computed. Then, the best individuals are selected and bred to
	 * create new individuals. These new individuals have a small chance of mutating.
	 */
	private void genetic(final int gen_size, final int num_gens, final float mutation) {
		Individual[] current_gen = new Individual[gen_size];
		Individual[] better_half = new Individual[gen_size/2];
		Individual best = null;
		PriorityQueue<Individual> leaderboard = new PriorityQueue<Individual>(gen_size);
		int k = 0;
		//Create first generation
		for(int i = 0 ; i < gen_size ; i++)
			current_gen[i] = new Individual(false);
		
		while(k < num_gens) {
			for(int i = 0 ; i < current_gen.length ; i++) {
				fitness(current_gen[i]);
				/* With natural ordering, individuals with high fitness will be at
				 * the front of the priority queue. */
				leaderboard.add(current_gen[i]);
			}
			
			best = leaderboard.peek();
			System.out.println("Generation " + k + ", best individual: " 
					+ best.toString());
			
			for(int i = 0 ; i < current_gen.length / 2 ; i++) {
				Individual in = leaderboard.remove();
				better_half[i] = in;
			}
			leaderboard.clear();
			
			current_gen = combine(better_half);
			mutate(current_gen, mutation);
			
			k++;
		};
	}
	
	
}
