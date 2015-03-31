import java.util.PriorityQueue;
import java.util.Random;

public class Genetic {
	final Random RANDOM = new Random();
	final String REFERENCE = "Hello world!";
	
	
	private class Individual implements Comparable<Individual> {
		public float[] features;
		public float fitness;
		private float EPSILON = 0.0001f;
		
		public Individual(int nfeatures, boolean random) {
			features = new float[nfeatures];
			fitness = 0;
			if(random) {
				for(int i = 0 ; i < nfeatures ; i++) {
					//In the actual project we should use this:
					//features[i] = RANDOM.nextFloat();
					
					//For the purposes of this example I use integer values
					//and put everything between 0 and 255 (i.e. character values)
					features[i] = (int) (RANDOM.nextFloat() * 255.f);
				}
			}
		}

		public int compareTo(Individual a) {
			//"Natural ordering" means larger fitness first
			if(Math.abs(fitness - a.fitness) < EPSILON)
				return 0;
			else if(fitness > a.fitness)
				return -1;
			return 1;
		}
		
		public String toString() {
			//Just here for debug
			char[] res = new char[features.length];
			for(int i = 0 ; i < features.length ; i++) {
				res[i] = (char) features[i];
			}
			
			return new String(res);
		}
	}
	//Create generation of random individuals
	//Forever:
	//  run individuals through fitness
	//  take 10 best individuals
	//  Create next generation
	//  mutate
	
	private void fitness(Individual i) {
		//This is domain-specific. I just put an example of how close are the features to "hello world"
		//This is where we would run the game with our player and its given set of weights
		//The closer to 1, the better the fitness
		
		int sum_errors = 0;
		for(int k = 0 ; k < REFERENCE.length() ; k++)
			sum_errors += Math.abs(REFERENCE.charAt(k) - i.features[k]);
		float max_error = REFERENCE.length() * 255;
		
		i.fitness = (max_error - sum_errors) / max_error;
	}
	
	private Individual[] combine(Individual[] gen) {
		//For each pair of individuals, randomly select features from
		//either one or the other to assign to each new individual.
		Individual[] newGen = new Individual[gen.length];
		for(int i = 0 ; i < gen.length - 1 ; i += 2) {
			Individual a = new Individual(gen[0].features.length, false);
			Individual b = new Individual(gen[0].features.length, false);
			
			//Alternate method of breeding. Flip a coin on every feature to
			//determine which child should inherit from whom. This might be
			//better for the actual project?
			/*for(int j = 0 ; j < gen[i].features.length ; j++) {
				if(RANDOM.nextBoolean()) {
					a.features[j] = gen[i].features[j];
					b.features[j] = gen[i+1].features[j];
				} else {
					a.features[j] = gen[i+1].features[j];
					b.features[j] = gen[i].features[j];
				}
			}*/
			
			int split = RANDOM.nextInt(gen[i].features.length);
			for(int j = 0 ; j < split ; j++) {
				a.features[j] = gen[i].features[j];
				b.features[j] = gen[i+1].features[j];
			}
			for(int j = split ; j < gen[i].features.length ; j++) {
				a.features[j] = gen[i+1].features[j];
				b.features[j] = gen[i].features[j];
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
					int amt = 0;
					while(amt == 0)
						amt = (int) ((RANDOM.nextBoolean() ? 1 : -1) * 2 * RANDOM.nextFloat());
					
					gen[i].features[j] += amt;
					if(gen[i].features[j] > 255)
						gen[i].features[j] = 255;
					else if(gen[i].features[j] < 0)
						gen[i].features[j] = 0;
				}
			}
		}
	}
	
	private void genetic(final int gen_size, final int num_gens, final float mutation) {
		Individual[] current_gen = new Individual[gen_size];
		Individual[] better_half = new Individual[gen_size/2];
		Individual best = null;
		PriorityQueue<Individual> leaderboard = new PriorityQueue<Individual>(gen_size);
		int k = 1;
		//Create first generation
		for(int i = 0 ; i < gen_size ; i++)
			current_gen[i] = new Individual(REFERENCE.length(), true);
		
		//Here we could just run forever, or ensure that our fitness function
		//never returns 1. Or stop after some amount of iterations.
		do {
			for(int i = 0 ; i < current_gen.length ; i++) {
				fitness(current_gen[i]);
				leaderboard.add(current_gen[i]);
			}
			
			best = leaderboard.peek();
			
			for(int i = 0 ; i < current_gen.length / 2 ; i++)
				better_half[i] = leaderboard.remove();
			leaderboard.clear();
			
			current_gen = combine(better_half);
			mutate(current_gen, mutation);
			
			System.out.println("Generation " + k + ", best individual: " 
					+ best.toString() + " (fitness " + best.fitness + ")");
			k++;
		} while(best.fitness != 1.f);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Genetic p = new Genetic();
		p.genetic(8192, 0, 0.015f);
	}

}
