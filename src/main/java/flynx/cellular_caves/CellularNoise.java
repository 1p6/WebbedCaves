package flynx.cellular_caves;

import java.util.Random;

import flynx.cellular_caves.CellularCarver.BType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;

public class CellularNoise {/*
	public enum Domain {
		RUN_CA
	}
	public void mixRand(Random r) {
		r.nextBoolean();
		r.nextBoolean();
		r.nextBoolean();
		r.nextBoolean();
		r.nextBoolean();
		r.nextBoolean();
	}
	public Random getSeededRandom(long seed, Domain d, int ...sources) {
		Random rand = new Random(seed);
//		mixRand(rand);
		rand = new Random(d.ordinal() ^ rand.nextLong());
//		mixRand(rand);
		for(int x : sources) {
			rand = new Random(x ^ rand.nextLong());
//			mixRand(rand);
		}
		return rand;
	}
	
	public void runCA3D(boolean[][][] input, boolean[][][] output) {
		int xl = input.length-1;
		int yl = input[0].length-1;
		int zl = input[0][0].length-1;
		for(int x = 1; x < xl; x++) {
			for(int y = 1; y < yl; y++) {
				for(int z = 1; z < zl; z++) {
					//sets each block to the median of it's 6 neighbors
					int count = 0;

					//face neighbors
					if(input[x-1][y][z]) count++;
					if(input[x][y-1][z]) count++;
					if(input[x][y][z-1]) count++;
					if(input[x+1][y][z]) count++;
					if(input[x][y+1][z]) count++;
					if(input[x][y][z+1]) count++;
					if(count < 3) {
						output[x][y][z] = false;
						continue;
					} else if(count > 3) {
						output[x][y][z] = true;
						continue;
					}

					//edge neighbors
					if(input[x][y+1][z+1]) count++;
					if(input[x][y+1][z-1]) count++;
					if(input[x][y-1][z+1]) count++;
					if(input[x][y-1][z-1]) count++;

					if(input[x+1][y][z+1]) count++;
					if(input[x+1][y][z-1]) count++;
					if(input[x-1][y][z+1]) count++;
					if(input[x-1][y][z-1]) count++;

					if(input[x+1][y+1][z]) count++;
					if(input[x+1][y-1][z]) count++;
					if(input[x-1][y+1][z]) count++;
					if(input[x-1][y-1][z]) count++;

					if(count < 9) {
						output[x][y][z] = false;
						continue;
					} else if(count > 9) {
						output[x][y][z] = true;
						continue;
					}
				}
			}
		}
	}
	public void runCA2D(boolean[][] input, boolean[][] output) {
		int xl = input.length-1;
		int yl = input[0].length-1;
		for(int x = 1; x < xl; x++) {
			for(int y = 1; y < yl; y++) {
				int count = 0;

				//face neighbors
				if(input[x-1][y][z]) count++;
				if(input[x][y-1][z]) count++;
				if(input[x][y][z-1]) count++;
				if(input[x+1][y][z]) count++;
				if(input[x][y+1][z]) count++;
				if(input[x][y][z+1]) count++;
				if(count < 3) {
					output[x][y][z] = false;
					continue;
				} else if(count > 3) {
					output[x][y][z] = true;
					continue;
				}

				//edge neighbors
				if(input[x][y+1][z+1]) count++;
				if(input[x][y+1][z-1]) count++;
				if(input[x][y-1][z+1]) count++;
				if(input[x][y-1][z-1]) count++;

				if(input[x+1][y][z+1]) count++;
				if(input[x+1][y][z-1]) count++;
				if(input[x-1][y][z+1]) count++;
				if(input[x-1][y][z-1]) count++;

				if(input[x+1][y+1][z]) count++;
				if(input[x+1][y-1][z]) count++;
				if(input[x-1][y+1][z]) count++;
				if(input[x-1][y-1][z]) count++;

				if(count < 9) {
					output[x][y][z] = false;
					continue;
				} else if(count > 9) {
					output[x][y][z] = true;
					continue;
				}
			}
		}
	}
	
	public static void generate(Random r, BlockPos start, Vector3i size, int layers, float pTrue, float pFalse) {
		float initP = pFalse / (1 + pFalse - pTrue);
		boolean[][][] res = null;
		for(int i = layers; i >= 0; i--) {
			
		}
	}
*/
}
