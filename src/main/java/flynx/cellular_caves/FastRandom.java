package flynx.cellular_caves;

public class FastRandom {
	public int seed1 = 0;
	public int seed2 = 0;
	public FastRandom(long ...data) {
		mix(data);
	}
	public void setSeed(long seed) {
		seed1 = (int) seed;
		seed2 = (int) (seed >>> 32);
	}
	public long getSeed() {
		return (seed1 & 0xFFFFFFFFL) | (((long) seed2) << 32);
	}
	public static final int mult1 = 0xe927773d;
	public static final int add1 = 0x17cce727;
	public static final int mult2 = 0x49a49afd;
	public static final int add2 = 0x17a172b9;
	public void step() {
		/* the right shifts help to ensure the least significant bits have a long period
		 * since otherwise, addition and multiplication only modify more significant bits with
		 * less significant ones. this whole function is reversible, due to the xor's only depending
		 * on values that stay fixed during the xor
		 */
		seed2 ^= seed1 >>> 16;
		seed2 = seed2 * mult2 + add2;
		seed1 ^= seed2 >>> 16;
		seed1 = seed1 * mult1 + add1;
	}
	public void mix(long ...data) {
		for(long l: data) {
			step();
			seed1 ^= l;
			step();
			seed1 ^= (l >>> 32);
		}
	}
	public long nextLong() {
		step();
		int a = seed1;
		step();
		return (a & 0xFFFFFFFFL) | (((long) seed1) << 32);
	}
	public int nextInt() {
		step();
		return seed1;
	}
	public int nextInt(int bound) {
		int mask = Integer.MAX_VALUE;
		int lim = mask - ((mask - bound + 1) % bound);
		int res;
		do res = nextInt() & mask; while(res > lim);
		return res % bound;
	}
	public float nextFloat() {
		return (nextInt() & Integer.MAX_VALUE) * 0x1p-31f;
	}
	public double nextDouble() {
		return (nextLong() & Long.MAX_VALUE) * 0x1p-63d;
	}
}
