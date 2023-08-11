package flynx.cellular_caves;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.util.concurrent.AtomicDouble;
import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.fluid.FluidState;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.RainType;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.carver.EmptyCarverConfig;
import net.minecraft.world.gen.carver.WorldCarver;

public class CellularCarver extends WorldCarver<EmptyCarverConfig> {
	
	public static final CellularCarver INST = new CellularCarver(EmptyCarverConfig.CODEC, 256);
	public static final ConfiguredCarver<EmptyCarverConfig> CONFIGURED = INST.configured(EmptyCarverConfig.INSTANCE);
	static { register(); }
	@SuppressWarnings("deprecation")
	private static void register() {
		Registry.register(Registry.CARVER, INST.getRegistryName(), INST);
		WorldGenRegistries.register(WorldGenRegistries.CONFIGURED_CARVER, INST.getRegistryName(), CONFIGURED);
	}
	
	//createLegacyBlock isn't thread safe I think, hence why these are constants
	public static final BlockState LAVA_BS = LAVA.createLegacyBlock();
	public static final BlockState WATER_BS = WATER.createLegacyBlock();
	public static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
	public static final BlockState MAGMA_BLOCK = Blocks.MAGMA_BLOCK.defaultBlockState();
	public static final BlockState PILLAR = Blocks.COBBLESTONE_WALL.defaultBlockState();
	public static final BlockState RED_GLAZED_TERRACOTTA = Blocks.RED_GLAZED_TERRACOTTA.defaultBlockState();
	
	public CellularCarver(Codec<EmptyCarverConfig> p_i231921_1_, int p_i231921_2_) {
		super(p_i231921_1_, p_i231921_2_);
		setRegistryName(CellularCaves.MODID, "cellular_caves");
	}
	
	public static enum BType {
		FILLED, EMPTY, WATER, LAVA, PILLAR
	}
	
	public void initCA(BType[][][] out, float prob, long seed, int x, int y, int z) {
		int xl = out.length;
		int yl = out[0].length;
		int zl = out[0][0].length;
		for(int xo = 0; xo < xl; xo++) {
			for(int yo = 0; yo < yl; yo++) {
				for(int zo = 0; zo < zl; zo++) {
					out[xo][yo][zo] = (new FastRandom(seed, xo+x, yo+y, zo+z).nextFloat() < prob ? BType.FILLED : BType.EMPTY);
				}
			}
		}
	}
	public void initCA2D(BType[][] out, float prob, long seed, int x, int y) {
		int xl = out.length;
		int yl = out[0].length;
		for(int xo = 0; xo < xl; xo++) {
			for(int yo = 0; yo < yl; yo++) {
				out[xo][yo] = (new FastRandom(seed, xo+x, yo+y).nextFloat() < prob ? BType.FILLED : BType.EMPTY);
			}
		}
	}
	
	public void blurCA(BType[][][] out, float pFill, float pEmpty, long seed, int x, int y, int z) {
		int xl = out.length;
		int yl = out[0].length;
		int zl = out[0][0].length;
		for(int xo = 0; xo < xl; xo++) {
			for(int zo = 0; zo < zl; zo++) {
				FastRandom blurRandom = new FastRandom(seed, xo+x, zo+z);
				for(int yo = 0; yo < yl; yo++) {
					out[xo][yo][zo] = (blurRandom.nextFloat() <
							(out[xo][yo][zo] == BType.FILLED ? pFill : pEmpty)) ? BType.FILLED : BType.EMPTY;
				}
			}
		}
	}
	public void blurCA2D(BType[][] out, float pFill, float pEmpty, long seed, int x, int y) {
		int xl = out.length;
		int yl = out[0].length;
		for(int xo = 0; xo < xl; xo++) {
			for(int yo = 0; yo < yl; yo++) {
				out[xo][yo] = (new FastRandom(seed, xo+x, yo+y).nextFloat() <
						(out[xo][yo] == BType.FILLED ? pFill : pEmpty)) ? BType.FILLED : BType.EMPTY;
			}
		}
	}
	
	public void runCA(BType[][][] input, BType[][][] output, boolean keepPillars) {
		final int offset = 1;
		int xl = input.length-offset;
		int yl = input[0].length-offset;
		int zl = input[0][0].length-offset;
		for(int x = offset; x < xl; x++) {
			for(int y = offset; y < yl; y++) {
				for(int z = offset; z < zl; z++) {
					if(keepPillars && input[x][y][z] == BType.PILLAR) {
						output[x][y][z] = BType.PILLAR;
						continue;
					}
//					if((x&3) == 0 && (y&3) == 0 && (z&3) == 0) continue;
					//sets each block to the median of it's 6 neighbors
					int count = 0;
//					boolean smoother = input[x][y-1][z] == BType.FILLED && input[x][y+1][z] == BType.EMPTY;
					int thresh = 7;
					
					//face neighbors
					if(input[x-1][y][z] == BType.EMPTY) count++;
					if(input[x+1][y][z] == BType.EMPTY) count++;
					if(input[x][y][z-1] == BType.EMPTY) count++;
					if(input[x][y][z+1] == BType.EMPTY) count++;
//					if(count < 2) {
//						output[x][y][z] = BType.EMPTY;
//						continue;
//					} else if(count > 2) {
//						output[x][y][z] = BType.FILLED;
//						continue;
//					}

//					if(smoother) {
						if(input[x+1][y+1][z] == BType.EMPTY) count++;
						if(input[x+1][y-1][z] == BType.EMPTY) count++;
						if(input[x-1][y+1][z] == BType.EMPTY) count++;
						if(input[x-1][y-1][z] == BType.EMPTY) count++;

						if(input[x][y+1][z+1] == BType.EMPTY) count++;
						if(input[x][y+1][z-1] == BType.EMPTY) count++;
						if(input[x][y-1][z+1] == BType.EMPTY) count++;
						if(input[x][y-1][z-1] == BType.EMPTY) count++;

//						if(input[x+1][y][z+1] == BType.FILLED) count++;
//						if(input[x+1][y][z-1] == BType.FILLED) count++;
//						if(input[x-1][y][z+1] == BType.FILLED) count++;
//						if(input[x-1][y][z-1] == BType.FILLED) count++;
//					} else {

						if(input[x][y-1][z] == BType.EMPTY) count++;
						if(input[x][y+1][z] == BType.EMPTY) count++;
//					}
//						if(input[x][y-2][z] == BType.FILLED) count++;
//						if(input[x][y+2][z] == BType.FILLED) count++;
					
					if(count < thresh) {
						output[x][y][z] = BType.FILLED;
						continue;
					} else if(count > thresh) {
						output[x][y][z] = BType.EMPTY;
						continue;
					}
					
					//corner neighbors
//					count = 0;
//					if(input[x+1][y+1][z+1] == BType.FILLED) count++;
//					if(input[x+1][y+1][z-1] == BType.FILLED) count++;
//					if(input[x+1][y-1][z+1] == BType.FILLED) count++;
//					if(input[x+1][y-1][z-1] == BType.FILLED) count++;
//					if(input[x-1][y+1][z+1] == BType.FILLED) count++;
//					if(input[x-1][y+1][z-1] == BType.FILLED) count++;
//					if(input[x-1][y-1][z+1] == BType.FILLED) count++;
//					if(input[x-1][y-1][z-1] == BType.FILLED) count++;
//					if(count < 4) {
//						output[x][y][z] = BType.EMPTY;
//						continue;
//					} else if(count > 4) {
//						output[x][y][z] = BType.FILLED;
//						continue;
//					}
					
////					edge neighbors
//					count = 0;
//					if(input[x][y+1][z+1] == BType.FILLED) count++;
//					if(input[x][y+1][z-1] == BType.FILLED) count++;
//					if(input[x][y-1][z+1] == BType.FILLED) count+=1;
//					if(input[x][y-1][z-1] == BType.FILLED) count+=1;
//
//					if(input[x+1][y][z+1] == BType.FILLED) count++;
//					if(input[x+1][y][z-1] == BType.FILLED) count++;
//					if(input[x-1][y][z+1] == BType.FILLED) count++;
//					if(input[x-1][y][z-1] == BType.FILLED) count++;
//
//					if(input[x+1][y+1][z] == BType.FILLED) count++;
//					if(input[x+1][y-1][z] == BType.FILLED) count+=1;
//					if(input[x-1][y+1][z] == BType.FILLED) count++;
//					if(input[x-1][y-1][z] == BType.FILLED) count+=1;
//
//					if(count < 6) {
//						output[x][y][z] = BType.EMPTY;
//						continue;
//					} else if(count > 6) {
//						output[x][y][z] = BType.FILLED;
//						continue;
//					}
					output[x][y][z] = input[x][y][z] == BType.PILLAR ? BType.FILLED : input[x][y][z];
				}
			}
		}
	}
	public void runCA2D(BType[][] input, BType[][] output) {
		int xl = input.length-1;
		int yl = input[0].length-1;
		for(int x = 1; x < xl; x++) {
			for(int y = 1; y < yl; y++) {
				//sets each block to the median of it's 6 neighbors
				int count = 0;

				//face neighbors
				if(input[x-1][y] == BType.FILLED) count++;
				if(input[x][y-1] == BType.FILLED) count++;
				if(input[x+1][y] == BType.FILLED) count++;
				if(input[x][y+1] == BType.FILLED) count++;
				if(count < 2) {
					output[x][y] = BType.EMPTY;
					continue;
				} else if(count > 2) {
					output[x][y] = BType.FILLED;
					continue;
				}
				output[x][y] = input[x][y];
			}
		}
	}
	public void runCALayered(BType[][][] input, BType[][][] output) {
		int xl = input.length-1;
		int yl = input[0].length-1;
		int zl = input[0][0].length-1;
		for(int x = 1; x < xl; x++) {
			for(int y = 1; y < yl; y++) {
				for(int z = 1; z < zl; z++) {
//					if((x&3) == 0 && (y&3) == 0 && (z&3) == 0) continue;
					//sets each block to the median of it's 6 neighbors
					int count = 0;
					
					//face neighbors
					if(input[x-1][y][z] == BType.FILLED) count++;
					if(input[x+1][y][z] == BType.FILLED) count++;
					if(input[x][y][z-1] == BType.FILLED) count++;
					if(input[x][y][z+1] == BType.FILLED) count++;
					if(count < 2) {
						output[x][y][z] = BType.EMPTY;
						continue;
					} else if(count > 2) {
						output[x][y][z] = BType.FILLED;
						continue;
					}
					output[x][y][z] = input[x][y][z];
				}
			}
		}
	}
	public void placeLiquid(BType[][][] array, BType[][][] arrayOut, int x, int y, int z, BType liquid) {
		//places liquid if the block is empty, the block below is filled, and at least one other neighbor is filled
//		if(array[x][y][z] == BType.EMPTY && array[x][y-1][z] == BType.FILLED && (
//				array[x-1][y][z] == BType.FILLED ||
//				array[x+1][y][z] == BType.FILLED ||
//				array[x][y+1][z] == BType.FILLED ||
//				array[x][y][z-1] == BType.FILLED ||
//				array[x][y][z+1] == BType.FILLED))
		//places liquid if the block is empty or any at-level neighbors of the block below are empty, and
//		//  the block below is filled, and all at-level neighbors are filled
//		if((array[x][y][z] == BType.EMPTY ||
//				((array[x-1][y-1][z] == BType.EMPTY ||
//				array[x+1][y-1][z] == BType.EMPTY ||
//				array[x][y-1][z-1] == BType.EMPTY ||
//				array[x][y-1][z+1] == BType.EMPTY) &&
//				array[x][y-2][z] == BType.EMPTY)) && // kinda iffy on this specific line, since it's ok if there is extra water in spikes
//				array[x][y-1][z] == BType.FILLED &&
//				array[x-1][y][z] == BType.FILLED &&
//				array[x+1][y][z] == BType.FILLED &&
//				array[x][y][z-1] == BType.FILLED &&
//				array[x][y][z+1] == BType.FILLED)
		if((array[x-1][y-1][z] == BType.EMPTY ||
				array[x+1][y-1][z] == BType.EMPTY ||
				array[x][y-1][z-1] == BType.EMPTY ||
				array[x][y-1][z+1] == BType.EMPTY ||
				array[x][y+1][z] == BType.EMPTY &&
				(array[x-1][y+1][z] == BType.FILLED ||
				array[x+1][y+1][z] == BType.FILLED ||
				array[x][y+1][z-1] == BType.FILLED ||
				array[x][y+1][z+1] == BType.FILLED)) &&
				array[x][y-1][z] == BType.FILLED &&
				array[x-1][y][z] == BType.FILLED &&
				array[x+1][y][z] == BType.FILLED &&
				array[x][y][z-1] == BType.FILLED &&
				array[x][y][z+1] == BType.FILLED)
			arrayOut[x][y][z] = liquid;
	}
	public enum Domain {
		CHUNK_ATTR, CHUNK_FILL, HEIGHT_NOISES
	}
	public Random getSeededRandom(long seed, long ...sources) {
		Random rand = new Random(seed);
//		rand.nextLong(); rand.nextLong();
//		rand.setSeed(d.ordinal() ^ rand.nextLong());
		rand.nextLong(); rand.nextLong();
		for(long x : sources) {
			rand = new Random(x ^ rand.nextLong());
			rand.nextLong(); rand.nextLong();
		}
		return rand;
	}
	
//	public SimplexNoiseGenerator[] initNoises(long seed, Domain domain, int size) {
//		SimplexNoiseGenerator[] noises = new SimplexNoiseGenerator[size];
//		for(int i = 0; i < size; i++) {
//			noises[i] = new SimplexNoiseGenerator(getSeededRandom(seed, domain, i));
//		}
//		return noises;
//	}
	
	public void loopLine(Vector3i start, Vector3i end, Consumer<Vector3i> body) {
		int dx = end.getX() - start.getX();
		int adx = Math.abs(dx);
		int dy = end.getY() - start.getY();
		int ady = Math.abs(dy);
		int dz = end.getZ() - start.getZ();
		int adz = Math.abs(dz);
		Axis biggest;
		int sl, el, sc1, ec1, sc2, ec2;
//		int maxl = radius;
		
		if(adx > ady && adx > adz) {
			biggest = Axis.X;
//			maxl += array.length;
			sl = start.getX(); el = end.getX();
			sc1 = start.getY(); ec1 = end.getY();
			sc2 = start.getZ(); ec2 = end.getZ();
		} else if(ady > adz) {
			biggest = Axis.Y;
//			maxl += array[0].length;
			sl = start.getY(); el = end.getY();
			sc1 = start.getX(); ec1 = end.getX();
			sc2 = start.getZ(); ec2 = end.getZ();
		} else {
			biggest = Axis.Z;
//			maxl += array[0][0].length;
			sl = start.getZ(); el = end.getZ();
			sc1 = start.getX(); ec1 = end.getX();
			sc2 = start.getY(); ec2 = end.getY();
		}
		if(sl > el) {
			int t = sl;
			sl = el;
			el = t;
			t = sc1;
			sc1 = ec1;
			ec1 = t;
			t = sc2;
			sc2 = ec2;
			ec2 = t;
		}
		int dl = el - sl;
		int dc1 = ec1 - sc1;
		int dc2 = ec2 - sc2;
		//int minl = sl < -radius ? -radius : sl;
		int minl = sl;
		//if(maxl > el) maxl = el;
		int maxl = el;
		for(int l = minl; l <= maxl; l++) {
			int c1 = dl == 0 ? sc1 : (l - sl) * dc1 / dl + sc1;
			int c2 = dl == 0 ? sc2 : (l - sl) * dc2 / dl + sc2;
			int x,y,z;
			switch(biggest) {
			case X:
				x = l; y = c1; z = c2;
				break;
			case Y:
				x = c1; y = l; z = c2;
				break;
			case Z:
			default:
				x = c1; y = c2; z = l;
			}
			body.accept(new Vector3i(x, y, z));
		}
	}
	
	public void digTunnel(float[][][] array, Vector3i start, Vector3i end, boolean cheese, boolean magma) {
		final int radius = 6;
//		final int radiusSq = 64;
		final int yRadius = 6;
		final int midR = 1;
		final int maxR = 6*6;
//		final int yShrinkSq = 4;
		loopLine(start, end, v -> {
			int x = v.getX(), y = v.getY(), z = v.getZ();
			int xs = Math.max(0, x-radius);
			int xe = Math.min(array.length-1, x+radius);
			int ys = Math.max(0, y-yRadius);
			int ye = Math.min(array[0].length-1, y+yRadius);
			int zs = Math.max(0, z-radius);
			int ze = Math.min(array[0][0].length-1, z+radius);
			for(int x2 = xs; x2 <= xe; x2++) {
				for(int y2 = ys; y2 <= ye; y2++) {
					for(int z2 = zs; z2 <= ze; z2++) {
						int dx = x2 - x;
						int dy = y2 - y;
						int dz = z2 - z;
						int r = dx*dx + dy*dy + dz*dz;
//						float chance = ((float) r) / maxR;
						if(r == 0 && magma) array[x2][0][z2] = 10f;
						float chance = !cheese && r <= midR ? 0f : (r <= maxR ? 0.5f : 1f);
						if(chance < array[x2][y2][z2]) array[x2][y2][z2] = chance;
					}
				}
			}
		});
	}
	
	public void digTriangle(float[][][] array, Vector3i a, Vector3i b, Vector3i c, boolean cheese) {
		loopLine(a, b, v -> digTunnel(array, v, c, cheese, false));
	}
	
	public void digBezier(float[][][] array, Node start, Node control, Node end) {
		//which node is the start vs end is already consistent between chunks due to
		//  it ordering the chunks lexicographically when creating the nodes
//		boolean shouldSwap = start.decider < end.decider;
//		if(((start.decider ^ end.decider) & 1) == 0) shouldSwap = !shouldSwap;
//		if(shouldSwap) { // get a consistent view of which node is the start that doesn't bias certain nodes
//			Node temp = start;
//			start = end;
//			end = temp;
//		}
		Vector3i sv = start.v;
		Vector3i cv = control.v;
		Vector3i ev = end.v;
		Vector3i prev = null;
		Vector3i pv1 = null, pv2 = null;
		Vector3d pNor = null;
		Vector3d pTan = null;
//		boolean ravine = start.ravine;
//		boolean cheese = (end.decider & 6) == 0;
//		boolean cheese = true;
		FastRandom r = new FastRandom(start.seed, end.seed);
		boolean ravine = r.nextInt(10) == 0;
		boolean cheese = r.nextInt(3) == 0;
		Vector3d UNIT1 = new Vector3d(1, 0, 0);
		Vector3d UNIT2 = new Vector3d(0, 1, 0);
		for(int i = 0; i <= 8; i++) {
//			int A = 64 - (8-i)*(8-i); // flipped bezier
//			int B = -2 * (8-i) * i;
//			int C = 64 - i * i;
			int A = (8-i)*(8-i); //normal bezier
			int B = 2 * (8-i) * i;
			int C = i * i;
			Vector3i segEnd = new Vector3i((A * sv.getX() + B * cv.getX() + C * ev.getX()) >> 6,
					(A * sv.getY() + B * cv.getY() + C * ev.getY()) >> 6,
					(A * sv.getZ() + B * cv.getZ() + C * ev.getZ()) >> 6);
			if(prev != null) {
				if(!ravine || cheese) digTunnel(array, prev, segEnd, false, true);
				if(ravine) {
					Vector3d tan = new Vector3d(segEnd.getX() - prev.getX(),
							segEnd.getY() - prev.getY(),
							segEnd.getZ() - prev.getZ()).normalize();
					if(tan != Vector3d.ZERO) {
						Vector3d nor;
						if(pNor == null) {
							Vector3d nor1 = tan.cross(UNIT1).normalize();
							if(nor1 == Vector3d.ZERO) nor1 = tan.cross(UNIT2).normalize();
							Vector3d nor2 = tan.cross(nor1);
							double angle = r.nextDouble() * Math.PI;
							pNor = nor = nor1.scale(Math.sin(angle))
									.add(nor2.scale(Math.cos(angle)));
							pv1 = pv2 = prev;
//							pv1 = new Vector3i(prev.getX() + (int) pNor.x,
//									prev.getY() + (int) pNor.y,
//									prev.getZ() + (int) pNor.z);
//							pv2 = new Vector3i(prev.getX() - (int) pNor.x,
//									prev.getY() - (int) pNor.y,
//									prev.getZ() - (int) pNor.z);
						} else {
							// rotation minimizing frame
							Vector3d refl = pTan.add(tan).normalize(); // vector to reflect through
							nor = pNor.subtract(refl.scale(2 * pNor.dot(refl))); // perform reflection
						}
						Vector3d sNor = nor.scale(B >> 1);
						Vector3i v1 = new Vector3i(segEnd.getX() + (int) sNor.x,
								segEnd.getY() + (int) sNor.y,
								segEnd.getZ() + (int) sNor.z);
						Vector3i v2 = new Vector3i(segEnd.getX() - (int) sNor.x,
								segEnd.getY() - (int) sNor.y,
								segEnd.getZ() - (int) sNor.z);
						digTriangle(array, pv1, pv2, v1, cheese);
						digTriangle(array, pv2, v1, v2, cheese);
						pTan = tan; pNor = nor; pv1 = v1; pv2 = v2;
					}
				}
			}
			prev = segEnd;
		}
	}
	
//	private static class BB {
//		final int minX;
//		final int minZ;
//		final int maxX;
//		final int maxZ;
//		static final BB ALL = new BB(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
//		BB(int minX, int minZ, int maxX, int maxZ){
//			this.minX = minX;
//			this.minZ = minZ;
//			this.maxX = maxX;
//			this.maxZ = maxZ;
//		}
//		BB merge(BB other) {
//			return new BB(Math.min(minX, other.minX), Math.min(minZ, other.minZ),
//					Math.max(maxX, other.maxX), Math.max(maxZ, other.maxZ));
//		}
//		boolean contains(BB other) {
//			return minX <= other.minX && minZ <= other.minZ && other.maxX <= maxX && other.maxZ <= maxZ;
//		}
//		boolean contains(int x, int z) {
//			return minX <= x && x <= maxX && minZ <= z && z <= maxZ;
//		}
//		BB intersect(BB other) {
//			return new BB(Math.max(minX, other.minX), Math.max(minZ, other.minZ),
//					Math.min(maxX, other.maxX), Math.min(maxZ, other.maxZ));
//		}
//		boolean empty() {
//			return minX > maxX || minZ > maxZ;
//		}
//	}
	private static class Node {
		final Vector3i v;
		final long seed;
		Node(Vector3i v, long seed) {
			this.v = v;
			this.seed = seed;
		}
	}
//	private static class Edge implements Comparable<Edge>{
//		final int weight;
//		final Node start;
//		final Node end;
////		static final Edge MAX = new Edge();
////		Edge() {
////			this.start = null;
////			this.end = null;
////			this.weight = Integer.MAX_VALUE;
////		}
//		static int calcWeight(Vector3i a, Vector3i b) {
////			int maxfromorig = Math.max(a.v.getX()*a.v.getX() + a.v.getZ()*a.v.getZ(),
////					b.v.getX()*b.v.getX() + b.v.getZ()*b.v.getZ()) >> 1; // to prevent disagreements on whether an edge is dug
//			int dx = a.getX() - b.getX();
//			int dy = a.getY() - b.getY();
//			int dz = a.getZ() - b.getZ();
//			return dx * dx + 16 * dy * dy + dz * dz;
//		}
//		Edge(Node start, Node end) {
//			this.start = start;
//			this.end = end;
//			weight = calcWeight(start.v, end.v);
//		}
//		public int compareTo(Edge o) {
////			return o.weight - weight; //large dist first
//			return weight - o.weight; //small dist first
//		}
//	}
	
	public int calcWeight(Vector3i a, Vector3i b) {
//int maxfromorig = Math.max(a.v.getX()*a.v.getX() + a.v.getZ()*a.v.getZ(),
//		b.v.getX()*b.v.getX() + b.v.getZ()*b.v.getZ()) >> 1; // to prevent disagreements on whether an edge is dug
		int dx = a.getX() - b.getX();
		int dy = a.getY() - b.getY();
		int dz = a.getZ() - b.getZ();
//		return dx * dx + 5 * dy * dy + dz * dz;
		return dx * dx + 16 * dy * dy + dz * dz;
	}
	
	@Override
	public boolean carve(IChunk chunk, Function<BlockPos, Biome> biomefunc, Random slowRand, int sealevel,
			int startChunkX, int startChunkZ, int currentChunkX, int currentChunkZ, BitSet p_225555_9_,
			EmptyCarverConfig p_225555_10_) {
		if(startChunkX != currentChunkX || startChunkZ != currentChunkZ) return false;
		Instant start = true||CellularCaves.debugInfo ? Instant.now() : null;
		FastRandom r = new FastRandom(CellularCaves.seed, 35);
		ChunkPos cpos = chunk.getPos();
		
		BType[][][] array, arrayT;
		int xl, yl, zl;

//		xl = 7+15+7; yl = 7+7+7+7+256+7+7+7+7; zl = 7+15+7;
//		arrayT = new BType[xl][yl][zl];
//		blurCA(arrayT, 0.4f, 0.4f, r.nextLong(), cpos.x<<1, 0, cpos.z<<1);
//		array = new BType[xl][yl][zl];
//		runCA(arrayT, array);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
//
//		xl = 7+15+7; yl = 7+7+7+256+7+7+7; zl = 7+15+7;
//		arrayT = new BType[xl][yl][zl];
//		for(int x = 0; x < xl; x++) {
//			for(int y = 0; y < yl; y++) {
//				for(int z = 0; z < zl; z++) {
//					arrayT[x][y][z] = array[(x>>1)+7][y+7][(z>>1)+7];
//				}
//			}
//		}
//		blurCA(arrayT, 0.55f, 0.45f, r.nextLong(), cpos.x<<2, 0, cpos.z<<2);
//		array = new BType[xl][yl][zl];
//		runCA(arrayT, array);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
//		for(int y = 7; y < yl-7; y++) {
//			int yMod = (y >> 3) & 3;
//			for(int x = 7; x < 7+15; x++) {
//				for(int z = 7; z < 7+15; z++) {
//					if((yMod & 1) == 0 ? ((cpos.x + (x>>2) + yMod) & 3) != 0 : 
//						((cpos.z + (z>>2) + yMod) & 3) != 0) array[x][y][z] = BType.FILLED;
//				}
//			}
//		}
//
//		xl = 7+15+7; yl = 7+7+256+7+7; zl = 7+15+7;
//		arrayT = new BType[xl][yl][zl];
//		for(int x = 0; x < xl; x++) {
//			for(int y = 0; y < yl; y++) {
//				for(int z = 0; z < zl; z++) {
//					arrayT[x][y][z] = array[(x>>1)+7][y+7][(z>>1)+7];
//				}
//			}
//		}
//		blurCA(arrayT, 0.55f, 0.45f, r.nextLong(), cpos.x<<3, 0, cpos.z<<3);
//		array = new BType[xl][yl][zl];
//		runCA(arrayT, array);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
		
		float ravineChance = 0.4f + (Math.floorMod(cpos.x, 10)) / 100f;
		long fillingSeed = r.nextLong();
		final int lavaHeight = 7;
		final int rounds = 12;
		xl = rounds+16+rounds; yl = rounds+256+rounds; zl = rounds+16+rounds;
		array = new BType[xl][yl][zl];
		arrayT = new BType[xl][yl][zl];
		float[][][] chances = new float[xl][yl][zl];
		for(int x = 0; x < xl; x++) {
			for(int y = 0; y < yl; y++) {
				for(int z = 0; z < zl; z++) {
					int xo = ((x >> 4) + (cpos.x >> 0)) >> 1;
					int yo = (y >> 2);
					int zo = ((z >> 4) + (cpos.z >> 0)) >> 1;
					chances[x][y][z] = 1f;
//					array[x][y][z] = BType.FILLED;
//					array[x][y][z] = (y >= 20 && y <= 52 && getSeededRandom(fillingSeed, xo, zo).nextInt(30) == 0
//							? BType.EMPTY : BType.FILLED);
//					array[x][y][z] = ((xo&3) == ((yo&2) == 0 ? 0 : 2)) ||
//							((zo&3) == (((yo+1)&2) == 0 ? 0 : 2)) ? BType.EMPTY : BType.FILLED;
				}
			}
		}
		
		{
			long tunnelSeed = r.nextLong();
			final int cradius = 8;
			final int density = 1;
			ArrayList<Node> points = new ArrayList<>(2*density*(2*cradius+1)*(2*cradius+1));
			for(int cx = -cradius; cx <= cradius; cx++) {
				for(int cz = -cradius; cz <= cradius; cz++) {
					FastRandom randPoints = new FastRandom(tunnelSeed, cx+cpos.x, cz+cpos.z);
					//poisson distribution sampling:
					double L = Math.exp((double) -density);
					double p = 1d;
					while(true) {
						p *= randPoints.nextDouble();
						if(p < L) break;
//						boolean ravine = randPoints.nextInt(10) == 0;
//						double angle = 0d, comp1 = 0d, comp2 = 0d;
//						if(ravine) {
//							angle = randPoints.nextDouble() * Math.PI;
//							comp1 = Math.sin(angle);
//							comp2 = Math.cos(angle);
//						}
						points.add(new Node(new Vector3i(randPoints.nextInt(16) + (cx << 4) + rounds,
								randPoints.nextInt(256) + rounds + 7,
								randPoints.nextInt(16) + (cz << 4) + rounds),
								randPoints.nextLong()));
					}
				}
			}
			//calculate relative neighborhood graph
			for(int i = 0; i < points.size(); i++) {
				Node ip = points.get(i);
				outer: for(int j = i+1; j < points.size(); j++) {
					Node jp = points.get(j);
					int weight = calcWeight(ip.v, jp.v);
					Node closest = ip; // in case there are no other nodes, have a non-null default in place
					int closestWeight = Integer.MAX_VALUE;
					for(int k = 0; k < points.size(); k++) {
						if(k == i || k == j) continue;
						Node kp = points.get(k);
						int kWeight = Math.max(calcWeight(ip.v, kp.v), calcWeight(jp.v, kp.v));
						if(weight > kWeight) continue outer;
						if(kWeight < closestWeight) {
							closestWeight = kWeight;
							closest = kp;
						}
					}
//					digTunnel(array, ip.v, jp.v);
					digBezier(chances, ip, closest, jp);
				}
			}
//			BB[][] pathBB = new BB[points.size()][points.size()];
//			int unconnected = points.size() * (points.size() - 1) / 2;
//			ArrayList<Edge> allEdges = new ArrayList<>(unconnected);
//			for(int i = 0; i < points.size(); i++) {
//				for(int j = 0; j < points.size(); j++) {
//					pathBB[i][j] = i == j ? points.get(i).bbNode : BB.ALL;
//					if(i < j) allEdges.add(new Edge(points.get(i), points.get(j)));
//				}
//			}
////			allEdges.sort(null);
//			ArrayList<Edge> edges = new ArrayList<>(points.size()-1);
//			outer: for(Edge e : allEdges) {
//				for(Node n : points) {
//					if(e.weight > Edge.calcWeight(e.start.v, n.v)
//							&& e.weight > Edge.calcWeight(e.end.v, n.v)) continue outer;
//				}
//				edges.add(e);
//			}
//			for(Edge e : edges) {
//				digTunnel(array, e.start.v, e.end.v);
//			}
		}
		Instant tunnels = Instant.now();
		
//		{
//			long blurSeed = r.nextLong();
//			long gapSeed = r.nextLong();
//			for(int xo = 0; xo < xl; xo++) {
//				for(int yo = 0; yo < yl; yo++) {
//					for(int zo = 0; zo < zl; zo++) {
//						int xgap = ((xo >> 4) + (cpos.x >> 0)) >> 1;
//						int zgap = ((zo >> 4) + (cpos.z >> 0)) >> 1;
//						boolean gap = yo >= 20 && yo < 52 && getSeededRandom(gapSeed, xgap, zgap).nextInt(30) == 0;
//						array[xo][yo][zo] = ((getSeededRandom(blurSeed, xo+(cpos.x<<4), yo, zo+(cpos.z<<4)).nextFloat() <
//								(array[xo][yo][zo] == BType.FILLED ? (gap ? 0.7f : 0.7) : 0.47f)) ? BType.FILLED : BType.EMPTY);
//					}
//				}
//			}
//		}
		
		//THIS PART:
//		long pillarSeed = r.nextLong();
		{
			long blurSeed = r.nextLong();
			for(int xo = 0; xo < xl; xo++) {
				for(int zo = 0; zo < zl; zo++) {
//					Random pillarRandom = getSeededRandom(pillarSeed, (xo+cpos.getMinBlockX())>>1, (zo+cpos.getMinBlockZ()) >> 1);
					FastRandom blurRandom = new FastRandom(blurSeed, xo+cpos.getMinBlockX(), zo+cpos.getMinBlockZ());
//					boolean pillar = pillarRandom.nextInt(64) == 0;
//					if(pillar) {
//						for(int yo = 0; yo < yl; yo++) {
//							if(0.8f < chances[xo][yo][zo]) chances[xo][yo][zo] = 0.8f;
//						}
//					}
					for(int yo = 0; yo < yl; yo++) {
						array[xo][yo][zo] = blurRandom.nextFloat() < chances[xo][yo][zo] ? BType.FILLED : BType.EMPTY;
					}
				}
			}
		}
//		blurCA(array, 0.7f, 0.4f, r.nextLong(), cpos.x<<4, 0, cpos.z<<4);
		Instant beforeCA = Instant.now();
		runCA(array, arrayT, true);
		runCA(arrayT, array, true);
		runCA(array, arrayT, true);
		runCA(arrayT, array, true);
		runCA(array, arrayT, true);
		runCA(arrayT, array, true);
		runCA(array, arrayT, true);
		//TO HERE
		
//		blurCA(array, 0.7f, 0.45f, r.nextLong(), cpos.x<<4, 0, cpos.z<<4);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
//		blurCA(array, 0.55f, 0.45f, r.nextLong(), cpos.x<<4, 0, cpos.z<<4);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
//		digTunnel(array, new Vector3i(rounds+5, 20, rounds+11), new Vector3i(rounds+11, 40, rounds+5));
		
		/*
		BType[][] surface = new BType[6+7+6][6+7+6];
		BType[][] surfaceT = new BType[6+7+6][6+7+6];
		initCA2D(surface, 0.6f, r.nextLong(), cpos.x << 2, cpos.z << 2);
		runCA2D(surface, surfaceT);
		runCA2D(surfaceT, surface);
		runCA2D(surface, surfaceT);
		runCA2D(surfaceT, surface);
		runCA2D(surface, surfaceT);
		runCA2D(surfaceT, surface);
		
//		BType[][][] array = new BType[6+1+6][6+16+6][6+1+6];
//		BType[][][] arrayT = new BType[6+1+6][6+16+6][6+1+6];
//		initCA(array, 0.5f, r.nextLong(), cpos.x, 0, cpos.z);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
//		runCA(array, arrayT);
//		runCA(arrayT, array);
		
		
		
		BType[][][] arrayT = new BType[12+16+12][256][12+16+12];
		
		BType[][][] array = new BType[12+16+12][256][12+16+12];
//		for(int xo = 0; xo < 12+16+12; xo++) {
//			for(int zo = 0; zo < 12+16+12; zo++) {
//				for(int y = 0; y < 55; y++) {
//					array[xo][y][zo] = BType.EMPTY;
//				}
//				for(int y = 55; y < 255; y++) {
//					array[xo][y][zo] = surface[(xo >> 2)+6][(zo >> 2)+6];
//				}
//			}
//		}
//		long finalBlurSeed1 = r.nextLong();
		long finalBlurSeed2 = r.nextLong();
		for(int yo = 0; yo < 256; yo++) {
//			boolean filledLayer = false;
//			int cPosX = cpos.x-2;
//			int cPosZ = cpos.z-2;
			for(int xo = 0; xo < 12+16+12; xo++) {
				for(int zo = 0; zo < 12+16+12; zo++) {
//					if(yo <= 40 && (cpos.x&1) == 0 && (cPosX != cpos.x + (xo>>4) || cPosZ != cpos.z + (zo>>4))) {
//						cPosX = cpos.x + (xo>>4);
//						cPosZ = cpos.z + (zo>>4);
//						filledLayer = getSeededRandom(finalBlurSeed1, cPosX, yo, cPosZ).nextInt(20) == 0;
//					}
					array[xo][yo][zo] = (getSeededRandom(finalBlurSeed2, xo+(cpos.x<<4), yo, zo+(cpos.z<<4)).nextFloat() <
							(yo < 55 ? 0.5f : surface[(xo>>2)+6][(zo>>2)+6] == BType.FILLED ? 0.55f : 0.46f)) ?
									BType.FILLED : BType.EMPTY;
				}
			}
		}
//		blurCA(array, 0.7f, 0.5f, r.nextLong(), cpos.x << 4, 0, cpos.z << 4);

		runCA(array, arrayT);
		runCA(arrayT, array);
		runCA(array, arrayT);
		runCA(arrayT, array);
		runCA(array, arrayT);
		runCA(arrayT, array);
		runCA(array, arrayT);
		runCA(arrayT, array);
		runCA(array, arrayT);
		runCA(arrayT, array);
		runCA(array, arrayT);
		runCA(arrayT, array);
		*/
		
		for(int x = 0; x < xl; x++) {
			for(int y = 0; y < yl; y++) {
				for(int z = 0; z < zl; z++) {
					array[x][y][z] = arrayT[x][y][z] == BType.PILLAR ? (arrayT[x][y][z] = BType.FILLED) : arrayT[x][y][z];
				}
			}
		}
		long pillarSeed = r.nextLong();
		for(int x = rounds; x < rounds+16; x++) {
			for(int z = rounds; z < rounds+16; z++) {
//				for(int y = rounds+1; y < rounds+11; y++) {
//					placeLiquid(array, x, y, z, BType.LAVA);
//				}
				for(int y = rounds+lavaHeight+1; y < rounds+256; y++) {
					placeLiquid(arrayT, array, x, y, z, BType.WATER);
				}
				FastRandom pillarRand = new FastRandom(pillarSeed, x+cpos.getMinBlockX(), z+cpos.getMinBlockZ());
				for(int y = rounds+lavaHeight+1; y < rounds+256; y++) {
					if(array[x][y][z] == BType.EMPTY &&
							(array[x][y-1][z] == BType.FILLED ||
							array[x][y+1][z] == BType.FILLED) &&
							pillarRand.nextInt(12) == 0)
						array[x][y][z] = BType.PILLAR;
				}
			}
		}
		Instant beforeSBS = Instant.now();
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				BlockPos pos = cpos.getWorldPosition().offset(x, 0, z);
				Biome b = biomefunc.apply(pos);
				BlockState surface = b.getGenerationSettings().getSurfaceBuilderConfig().getUnderMaterial();
				boolean flooded = b.getBiomeCategory() == Biome.Category.OCEAN;
				boolean rain = b.getPrecipitation() != RainType.NONE;
				for(int y = 1; y < 255; y++) {
					BlockPos posy = pos.offset(0, y, 0);
					BlockState bs = chunk.getBlockState(posy);
					if(bs.getMaterial() == Material.AIR ||
							bs.is(Blocks.BEDROCK)) continue;
					FluidState fs = bs.getFluidState();
					if(!fs.isEmpty()) {
						// to prevent floating liquids
						chunk.getLiquidTicks().scheduleTick(posy, fs.getType(), x);
						continue;
					}
					BType bt = array[x+rounds][y+rounds][z+rounds];
					if(bt == BType.FILLED || bt == BType.WATER && flooded) continue;
					if(bt == BType.PILLAR) {
						chunk.setBlockState(posy, PILLAR.setValue(BlockStateProperties.WATERLOGGED, flooded), false);
						if(flooded) chunk.getLiquidTicks().scheduleTick(posy, WATER.getType(), x);
					} else if(bt == BType.LAVA || y <= lavaHeight) {
						boolean crevice = chances[x+rounds][0][z+rounds] > 1f || y < lavaHeight;
						if(flooded) {
//							if(true || CellularCaves.magmaChance != 0 && rand.nextInt(CellularCaves.magmaChance) == 0) {
//							boolean xEven = (x&1) == 0;
//							boolean zEven = (z&1) == 0;
//							if((xEven && zEven) || (xEven || zEven) && rand.nextBoolean()) {
//							if(((x^z)&1) == 0 && rand.nextBoolean()) {
							if(crevice) {
								chunk.setBlockState(posy, MAGMA_BLOCK, false);
								// use the x pos in the chunk as the tick delay
								// the delay isn't currently used by ChunkPrimerTickList
								// magma blocks need ticks to form bubble columns
								chunk.getBlockTicks().scheduleTick(posy, Blocks.MAGMA_BLOCK, x);
							} else {
								chunk.setBlockState(posy, OBSIDIAN, false);
							}
						} else {
							if(crevice) {
								chunk.setBlockState(posy, LAVA_BS, false);
								chunk.getLiquidTicks().scheduleTick(posy, LAVA.getType(), x);
							} else {
								chunk.setBlockState(posy, MAGMA_BLOCK, false);
							}
						}
					} else if(bt == BType.WATER && !rain) {
						chunk.setBlockState(posy, surface, false);
					} else if(flooded || bt == BType.WATER) {
						chunk.setBlockState(posy, WATER_BS, false);
						chunk.getLiquidTicks().scheduleTick(posy, WATER.getType(), x);
					} else {
						chunk.setBlockState(posy, y <= 5 ? LAVA_BS : CAVE_AIR, false);
					}
				}
			}
		}
		
		Instant end = Instant.now();
		CellularCaves.LOGGER.info("tunnel digging took " + Duration.between(start, tunnels).toMillis() + " ms");
		CellularCaves.LOGGER.info("blur took " + Duration.between(tunnels, beforeCA).toMillis() + " ms");
		CellularCaves.LOGGER.info("ca took " + Duration.between(beforeCA, beforeSBS).toMillis() + " ms");
		CellularCaves.LOGGER.info("sbs took " + Duration.between(beforeSBS, end).toMillis() + " ms");
		if(true || CellularCaves.debugInfo)
			CellularCaves.LOGGER.info("total chunk cave gen took " + Duration.between(start, end).toMillis() + " ms");
		
		return true;
	}
	
	/*
	@Override
	public boolean carve(IChunk chunk, Function<BlockPos, Biome> biomefunc, Random rand, int sealevel,
			int startChunkX, int startChunkZ, int currentChunkX, int currentChunkZ, BitSet p_225555_9_,
			EmptyCarverConfig p_225555_10_) {
		if(startChunkX != currentChunkX || startChunkZ != currentChunkZ) return false;
		Instant start = CellularCaves.debugInfo ? Instant.now() : null;
		BType[][][] array = new BType[48][256][48];
		ChunkPos cpos = chunk.getPos();
		double[][] vals = new double[4][4];
		//SimplexNoiseGenerator[] heightNoises = initNoises(CellularCaves.seed, Domain.HEIGHT_NOISES, 4);
		
		for(int chx = 0; chx < 3; chx++) {
			for(int chz = 0; chz < 3; chz++) {
				Random rand2 = getSeededRandom(CellularCaves.seed, Domain.CHUNK_ATTR, chx + cpos.x, chz + cpos.z);
				boolean verticalChunk = CellularCaves.ravineChance != 0 &&
						rand2.nextInt(CellularCaves.ravineChance) == 0; // acts like ravines
				rand2 = getSeededRandom(CellularCaves.seed, Domain.CHUNK_FILL, chx + cpos.x, chz + cpos.z);
				for(int i = 0; i < 4; i++) {
					vals[i][0] = getSeededRandom(i, Domain.HEIGHT_NOISES, i, chx + cpos.x, chz + cpos.z).nextDouble();
					vals[i][1] = getSeededRandom(i, Domain.HEIGHT_NOISES, i, chx + cpos.x+1, chz + cpos.z).nextDouble();
					vals[i][2] = getSeededRandom(i, Domain.HEIGHT_NOISES, i, chx + cpos.x, chz + cpos.z+1).nextDouble();
					vals[i][3] = getSeededRandom(i, Domain.HEIGHT_NOISES, i, chx + cpos.x+1, chz + cpos.z+1).nextDouble();
				}
				for(int xOff = 0; xOff < 16; xOff++) {
					for(int zOff = 0; zOff < 16; zOff++) {
						int x = xOff + (chx << 4);
						int z = zOff + (chz << 4);
						double heightAcc = 1d;
						for(double[] noise : vals) {
							heightAcc *= MathHelper.lerp2(xOff/16d, zOff/16d, noise[0], noise[1], noise[2], noise[3]);
						}
						int height = 50 + (int)(heightAcc * 32);
						for(int y = 0; y < 256; y++) {
							if(y == 0) array[x][y][z] = BType.FILLED;
							else if(verticalChunk && y > 1 && y <= height) array[x][y][z] = array[x][1][z];
							else array[x][y][z] = rand2.nextFloat() < (y > height ? 1f : 0f) ?
									BType.FILLED : BType.EMPTY;
						}
					}
				}
			}
		}

		BType[][][] array2 = new BType[48][256][48]; //scratch array
		for(int i = 0; i < CellularCaves.iterations; i++) {
			runCA(array, array2);
			runCA(array2, array);
		}
		
		for(int x = 16; x < 32; x++) {
			for(int z = 16; z < 32; z++) {
				for(int y = 1; y < 11; y++) {
					placeLiquid(array, x, y, z, BType.LAVA);
				}
				for(int y = 62; y < 255; y++) {
					placeLiquid(array, x, y, z, BType.WATER);
				}
			}
		}
		
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				BlockPos pos = cpos.getWorldPosition().offset(x, 0, z);
				Biome b = biomefunc.apply(pos);
				boolean flooded = b.getBiomeCategory() == Biome.Category.OCEAN;
				float waterChance = b.getDownfall() * 0.3f;
				for(int y = 1; y < 255; y++) {
					BlockPos posy = pos.offset(0, y, 0);
					BlockState bs = chunk.getBlockState(posy);
					if(bs.getMaterial() == Material.AIR ||
							bs.is(Blocks.BEDROCK)) continue;
					FluidState fs = bs.getFluidState();
					if(!fs.isEmpty()) {
						// to prevent floating liquids
						chunk.getLiquidTicks().scheduleTick(posy, fs.getType(), x);
						continue;
					}
					BType bt = array[x+16][y][z+16];
					if(bt == BType.FILLED) continue;
					if(bt == BType.LAVA) {
						if(flooded) {
							if(CellularCaves.magmaChance != 0 && rand.nextInt(CellularCaves.magmaChance) == 0) {
								chunk.setBlockState(posy, MAGMA_BLOCK, false);
								// use the x pos in the chunk as the tick delay
								// the delay isn't currently used by ChunkPrimerTickList
								// magma blocks need ticks to form bubble columns
								chunk.getBlockTicks().scheduleTick(posy, Blocks.MAGMA_BLOCK, x);
							} else {
								chunk.setBlockState(posy, OBSIDIAN, false);
							}
						} else {
							chunk.setBlockState(posy, LAVA_BS, false);
							chunk.getLiquidTicks().scheduleTick(posy, LAVA.getType(), x);
						}
					} else if(flooded || bt == BType.WATER && rand.nextFloat() < waterChance) {
						chunk.setBlockState(posy, WATER_BS, false);
						chunk.getLiquidTicks().scheduleTick(posy, WATER.getType(), x);
					} else {
						chunk.setBlockState(posy, CAVE_AIR, false);
					}
				}
			}
		}
		
		if(CellularCaves.debugInfo)
			CellularCaves.LOGGER.info("chunk cave gen took " + Duration.between(start, Instant.now()));
		
		return true;
	}*/
	
	/*@Override
	public boolean carve(IChunk chunk, Function<BlockPos, Biome> p_225555_2_, Random rand, int sealevel,
			int startChunkX, int startChunkZ, int currentChunkX, int currentChunkZ, BitSet p_225555_9_,
			EmptyCarverConfig p_225555_10_) {
		if(startChunkX != currentChunkX || startChunkZ != currentChunkZ) return false;
		for(int chunkY = 0; chunkY < 16; chunkY++) {
			BlockPos start = chunk.getPos().getWorldPosition().offset(0, chunkY << 4, 0);
			if(chunkY == 0) {
				for(int x0 = 0; x0 < 4; x0++) {
					for(int z0 = 0; z0 < 4; z0++) {
						if(rand.nextBoolean()) continue;
						for(int x = 0; x < 4; x++) {
							for(int z = 0; z < 4; z++) {
								for(int y = 0; y < 16; y++) {
									digBlock(chunk, rand, start.offset(x + (x0 << 2), y, z + (z0 << 2)), false);
								}
							}
						}
					}
				}
			} else {
				boolean up = rand.nextInt(9) == 0;
				if(up) {
					for(int i = 0; i < 4; i++) {
						for(int j = 0; j < 4; j++) {
							for(int k = 0; k < 16; k++) {
								digBlock(chunk, rand, start.offset(i, k, j), true);
							}
						}
					}
				} else {
					boolean xb = rand.nextBoolean();
					for(int i = 0; i < 3; i++) {
						for(int j = 0; j < 4; j++) {
							for(int y0 = 0; y0 < 3; y0++) {
								if(rand.nextInt(3) < Math.max(i, y0)) continue;
								for(int x = 0; x < 4; x++) {
									for(int y = 0; y < 4; y++) {
										for(int z = 0; z < 4; z++) {
											if(xb) digBlock(chunk, rand, start.offset(x + (j << 2), y + (y0 << 2), z + (i << 2)), false);
											else digBlock(chunk, rand, start.offset(x + (i << 2), y + (y0 << 2), z + (j << 2)), false);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return true;
	}
	
	public void digBlock(IChunk c, Random r, BlockPos pos, boolean carveDirt) {
		BlockState b = c.getBlockState(pos);
		if(!this.replaceableBlocks.contains(b.getBlock())) return;
		Material m = b.getMaterial();
		if(!carveDirt) {
			if(m == Material.DIRT || m == Material.GRASS) return;
			Material m2 = c.getBlockState(pos.above()).getMaterial();
			if(m2 == Material.DIRT || m2 == Material.GRASS) return;
		} else {
			if(m == Material.GRASS && r.nextInt(2) == 0) return;
		}
		c.setBlockState(pos, pos.getY() <= 10 ? LAVA.createLegacyBlock() : CAVE_AIR, false);
	}
	
	private int size(Random r) {
		int s = 3;
		while(s < 7 && r.nextBoolean()) s++;
		return s;
	}

	public boolean carve2(IChunk chunk, Function<BlockPos, Biome> p_225555_2_, Random rand, int sealevel,
			int startChunkX, int startChunkZ, int currentChunkX, int currentChunkZ, BitSet p_225555_9_,
			EmptyCarverConfig p_225555_10_) {
		if(startChunkX != currentChunkX || startChunkZ != currentChunkZ) return false;
		boolean pokesThru = rand.nextInt(9) == 0;
		for(int chunkY = 0; chunkY < 16; chunkY++) {
			BlockPos start = chunk.getPos().getWorldPosition().offset(0, chunkY << 4, 0);
			int xSize = rand.nextInt(3) == 0 ? 0 : size(rand);
			int ySize = rand.nextInt(3) == 0 ? 0 : size(rand);
			int zSize = rand.nextInt(3) == 0 ? 0 : size(rand);
			for(int i = 0; i < 16; i++) {
				for(int j = 0; j < 16; j++) {
					float rad = (float) Math.sqrt((i-8)*(i-8) + (j-8)*(j-8)); //sqrt caches results hopefully
					float xProb = (xSize - rad)/2f;
					float yProb = Math.min(0.95f, (ySize - rad)/2f);
					float zProb = (zSize - rad)/2f;
					for(int k = 0; k < 16; k++) {
						float ends = (k == 0 || k == 15) && rad < 5f ? 0.1f : 0f;
						if(rand.nextFloat() < Math.max(ends, zProb)) digBlock(chunk, rand, start.offset(i, j, k), false);
						if(rand.nextFloat() < Math.max(ends, yProb)) digBlock(chunk, rand, start.offset(i, k, j), pokesThru);
						if(rand.nextFloat() < Math.max(ends, xProb)) digBlock(chunk, rand, start.offset(k, i, j), false);
					}
				}
			}
		}
		return true;
	}*/

	@Override
	public boolean isStartChunk(Random p_212868_1_, int p_212868_2_, int p_212868_3_, EmptyCarverConfig p_212868_4_) {
		return true;
	}

	/**
	 * never used
	 */
	@Override
	public boolean skip(double p_222708_1_, double p_222708_3_, double p_222708_5_, int p_222708_7_) {
		return false;
	}

}
