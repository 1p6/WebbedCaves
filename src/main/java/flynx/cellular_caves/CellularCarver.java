package flynx.cellular_caves;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

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
					//sets each block to the median of it's 6 neighbors
					int count = 0;
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
	
	
	public void placeLiquid(BType[][][] array, BType[][][] arrayOut, int x, int y, int z, BType liquid) {
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
	
	private static class Node {
		final Vector3i v;
		final long seed;
		Node(Vector3i v, long seed) {
			this.v = v;
			this.seed = seed;
		}
	}
	
	public int calcWeight(Vector3i a, Vector3i b) {
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
					digBezier(chances, ip, closest, jp);
				}
			}
		}
		Instant tunnels = Instant.now();
		
		{
			long blurSeed = r.nextLong();
			for(int xo = 0; xo < xl; xo++) {
				for(int zo = 0; zo < zl; zo++) {
					FastRandom blurRandom = new FastRandom(blurSeed, xo+cpos.getMinBlockX(), zo+cpos.getMinBlockZ());
					for(int yo = 0; yo < yl; yo++) {
						array[xo][yo][zo] = blurRandom.nextFloat() < chances[xo][yo][zo] ? BType.FILLED : BType.EMPTY;
					}
				}
			}
		}
		
		Instant beforeCA = Instant.now();
		runCA(array, arrayT, true);
		runCA(arrayT, array, true);
		runCA(array, arrayT, true);
		runCA(arrayT, array, true);
		runCA(array, arrayT, true);
		runCA(arrayT, array, true);
		runCA(array, arrayT, true);
		
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
