
public class PlayerSkeleton {
	
	/**
	 * Extended state class. Offers methods to
	 * test a move and compute the heuristics for
	 * the given state.
	 */
	private class StateEx extends State {
		
		private int[][] fieldCopy;
		private int[] topCopy;

				
		private int getHoles() {
			int holes = 0;

			//start looking for empty spaces below top of pile
			for (int col = 0; col < this.topCopy.length; col++){
				for (int row = this.topCopy[col]; row >= 0; row--){
					//if empty space encountered below top, it's a hole
					if (this.fieldCopy[row][col] == 0)
						holes++;
				}
			}

			return holes;
		}
		
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
		
		private float testMove(int orient, int slot) {
			//Copy data over to topCopy and fieldCopy
			int[] topCopy = new int[COLS];
			int[][] fieldCopy = new int[ROWS][COLS];
			System.arraycopy( this.getTop(), 0, topCopy, 0, COLS );
			for(int i = 0;i<ROWS;i++){
				System.arraycopy(this.getField()[i], 0, fieldCopy[i], 0, COLS);
			}

			float score = 0;
			//weights, initially evenly distributed
			int[] weights = {0.25, 0.25, 0.25, 0.25};
			
			//initialize heuristics
			int rowsCleared = dryRunMove(this.nextPiece, orient, slot);
			int[] bumpinessAndHeight = getBumpinessAndHeight();
			int[] heuristics = {this.getRowsCleared(),
								getHoles(),
								bumpinessAndHeight[0],
								bumpinessAndHeight[1] };

			//score/evaluation function is dot product of heuristics[4] and weights[4]
			for (int i = 0; i < heuristics.length; i++)
				float score = heuristics[i]*weights[i];
			
			return score;
			
		}
		
		/**
		 * Play the given move on our local copy of the board.
		 * @return Number of rows cleared by the move.
		 */
		private int dryRunMove(int piece, int orient, int slot) {
			//height if the first column makes contact
			int height = topCopy[slot]-State.getpBottom()[piece][orient][0];
			//for each column beyond the first in the piece
			for(int c = 1; c < State.getpWidth()[piece][orient];c++) {
				height = Math.max(height,topCopy[slot+c]-State.getpBottom()[piece][orient][c]);
			}
			
			//for each column in the piece - fill in the appropriate blocks
			for(int i = 0; i < State.getpWidth()[piece][orient]; i++)
				for(int h = height+State.getpBottom()[piece][orient][i]; h < height+State.getpTop()[piece][orient][i]; h++)
					fieldCopy[h][i+slot] = -1;
			
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
					if(fieldCopy[r][c] == 0) {
						full = false;
						break;
					}
				}
				//if the row was full - remove it and slide above stuff down
				if(full) {
					rowsCleared++;					
					//for each column
					for(int c = 0; c < COLS; c++) {

						//slide down all bricks
						for(int i = r; i < topCopy[c]; i++) {
							fieldCopy[i][c] = fieldCopy[i+1][c];
						}
						//lower the top
						topCopy[c]--;
						while(topCopy[c]>=1 && fieldCopy[topCopy[c]-1][c]==0)	topCopy[c]--;
					}
				}
				
			}
			
			return rowsCleared;
		}
		
	}
	
	//implement this function to have a working system
	public int pickMove(StateEx s, int[][] legalMoves) {
		int maxScore = 0, bestOrient = 0, bestSlot = 0;

		for (int i = 0; i < legalMoves.length; i++){
				int testScore = testMove(legalMoves[i][ORIENT], legalMoves[i][SLOT]);
				if (testScore > maxScore){
					maxScore = testScore;
					bestOrient = ORIENT;
					bestSlot = SLOT;
				}
			}
		}
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
