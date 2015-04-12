
public class PlayerSkeleton {
	
	/**
	 * Extended state class. Offers methods to
	 * test a move and compute the heuristics for
	 * the given state.
	 */
	private class StateEx extends State {
		
		/**
		 * Test a given move against the state.
		 * @return The number of rows that would be cleared by
		 * making the given move.
		 */
		public int[] getHeuristicVector() {
			
			int[] bumpinessAndHeight = getBumpinessAndHeight();
			
			return new int[] {this.getRowsCleared(),
							  getHoles(),
							  bumpinessAndHeight[0],
							  bumpinessAndHeight[1]};
		}
				
		private int getHoles() {
			int holes = 0;

			//start looking for empty spaces below top of pile
			for (int col = 0; col < this.getTop().length; col++){
				for (int row = this.getTop()[col]; row >= 0; row--){
					//if empty space encountered below top, it's a hole
					if (this.getField[row][col] == 0)
						holes++;
				}
			}

			return 0;
		}
		
		private int[] getBumpinessAndHeight() {
			int[] top = this.getTop();
			int bumpiness = 0;
			int aggregateHeight = top[0];
			for (int i = 1; i < top.length; i ++)
			{
				bumpiness += Math.abs(top[i] - top[i-1]);
				aggregateHeight += top[i];
			}
			return new int[] {bumpiness, aggregateHeight};
		}
		
		
		
		
	}
	
	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		
		return 0;
	}
	
	public static void main(String[] args) {
		PlayerSkeleton p = new PlayerSkeleton();
		StateEx s = p.new StateEx();
		
		new TFrame(s);
		
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}
