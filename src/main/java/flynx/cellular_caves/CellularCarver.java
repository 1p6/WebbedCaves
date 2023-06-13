package flynx.cellular_caves;

import java.time.Duration;
import java.time.Instant;
import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.biome.Biome;
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

	public CellularCarver(Codec<EmptyCarverConfig> p_i231921_1_, int p_i231921_2_) {
		super(p_i231921_1_, p_i231921_2_);
		setRegistryName(CellularCaves.MODID, "cellular_caves");
	}
	
	public static enum BType {
		FILLED, EMPTY, WATER, LAVA
	}
	
	public void initCA(BType[][][] out, float prob, long seed, int x, int y, int z) {
		int xl = out.length;
		int yl = out[0].length;
		int zl = out[0][0].length;
		for(int xo = 0; xo < xl; xo++) {
			for(int yo = 0; yo < yl; yo++) {
				for(int zo = 0; zo < zl; zo++) {
					out[xo][yo][zo] = (getSeededRandom(seed, xo+x, yo+y, zo+z).nextFloat() < prob ? BType.FILLED : BType.EMPTY);
				}
			}
		}
	}
	public void initCA2D(BType[][] out, float prob, long seed, int x, int y) {
		int xl = out.length;
		int yl = out[0].length;
		for(int xo = 0; xo < xl; xo++) {
			for(int yo = 0; yo < yl; yo++) {
				out[xo][yo] = (getSeededRandom(seed, xo+x, yo+y).nextFloat() < prob ? BType.FILLED : BType.EMPTY);
			}
		}
	}
	
	public void blurCA(BType[][][] out, float pFill, float pEmpty, long seed, int x, int y, int z) {
		int xl = out.length;
		int yl = out[0].length;
		int zl = out[0][0].length;
		for(int xo = 0; xo < xl; xo++) {
			for(int yo = 0; yo < yl; yo++) {
				for(int zo = 0; zo < zl; zo++) {
					out[xo][yo][zo] = ((getSeededRandom(seed, xo+x, yo+y, zo+z).nextFloat() <
							(out[xo][yo][zo] == BType.FILLED ? pFill : pEmpty)) ? BType.FILLED : BType.EMPTY);
				}
			}
		}
	}
	public void blurCA2D(BType[][] out, float pFill, float pEmpty, long seed, int x, int y) {
		int xl = out.length;
		int yl = out[0].length;
		for(int xo = 0; xo < xl; xo++) {
			for(int yo = 0; yo < yl; yo++) {
				out[xo][yo] = (getSeededRandom(seed, xo+x, yo+y).nextFloat() <
						(out[xo][yo] == BType.FILLED ? pFill : pEmpty)) ? BType.FILLED : BType.EMPTY;
			}
		}
	}
	
	public void runCA(BType[][][] input, BType[][][] output) {
		int xl = input.length-1;
		int yl = input[0].length-1;
		int zl = input[0][0].length-1;
		for(int x = 1; x < xl; x++) {
			for(int y = 1; y < yl; y++) {
				for(int z = 1; z < zl; z++) {
//					if((x&3) == 0 && (y&3) == 0 && (z&3) == 0) continue;
					//sets each block to the median of it's 6 neighbors
					int count = 0;
//					boolean smoother = input[x][y-1][z] == BType.FILLED && input[x][y+1][z] == BType.EMPTY;
					int thresh = 9;
					
					//face neighbors
					if(input[x-1][y][z] == BType.FILLED) count++;
					if(input[x+1][y][z] == BType.FILLED) count++;
					if(input[x][y][z-1] == BType.FILLED) count++;
					if(input[x][y][z+1] == BType.FILLED) count++;
//					if(count < 2) {
//						output[x][y][z] = BType.EMPTY;
//						continue;
//					} else if(count > 2) {
//						output[x][y][z] = BType.FILLED;
//						continue;
//					}

//					if(smoother) {
						if(input[x+1][y+1][z] == BType.FILLED) count++;
						if(input[x+1][y-1][z] == BType.FILLED) count++;
						if(input[x-1][y+1][z] == BType.FILLED) count++;
						if(input[x-1][y-1][z] == BType.FILLED) count++;

						if(input[x][y+1][z+1] == BType.FILLED) count++;
						if(input[x][y+1][z-1] == BType.FILLED) count++;
						if(input[x][y-1][z+1] == BType.FILLED) count++;
						if(input[x][y-1][z-1] == BType.FILLED) count++;

						if(input[x+1][y][z+1] == BType.FILLED) count++;
						if(input[x+1][y][z-1] == BType.FILLED) count++;
						if(input[x-1][y][z+1] == BType.FILLED) count++;
						if(input[x-1][y][z-1] == BType.FILLED) count++;
//					} else {

						if(input[x][y-1][z] == BType.FILLED) count++;
						if(input[x][y+1][z] == BType.FILLED) count++;
//					}
					
					if(count < thresh) {
						output[x][y][z] = BType.EMPTY;
						continue;
					} else if(count > thresh) {
						output[x][y][z] = BType.FILLED;
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
					output[x][y][z] = input[x][y][z];
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
	public void placeLiquid(BType[][][] array, int x, int y, int z, BType liquid) {
		//places liquid if the block is empty, the block below is filled, and at least one other neighbor is filled
		if(array[x][y][z] == BType.EMPTY && array[x][y-1][z] == BType.FILLED && (
				array[x-1][y][z] == BType.FILLED ||
				array[x+1][y][z] == BType.FILLED ||
				array[x][y+1][z] == BType.FILLED ||
				array[x][y][z-1] == BType.FILLED ||
				array[x][y][z+1] == BType.FILLED))
			array[x][y][z] = liquid;
	}
	public enum Domain {
		CHUNK_ATTR, CHUNK_FILL, HEIGHT_NOISES
	}
	public Random getSeededRandom(long seed, int ...sources) {
		Random rand = new Random(seed);
//		rand.nextLong(); rand.nextLong();
//		rand.setSeed(d.ordinal() ^ rand.nextLong());
		rand.nextLong(); rand.nextLong();
		for(int x : sources) {
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
	
	public void digTunnel(BType[][][] array, Vector3i start, Vector3i end) {
		final int radius = 4;
		int dx = end.getX() - start.getX();
		int adx = Math.abs(dx);
		int dy = end.getY() - start.getY();
		int ady = Math.abs(dy);
		int dz = end.getZ() - start.getZ();
		int adz = Math.abs(dz);
		abstract class Dig {
			
		}
		
		if(adx >= ady && adx >= adz) {
			boolean flip = dx <= 0;
			int sx = Math.max(-radius-2, flip ? end.getX() : start.getX());
			int ex = Math.min(array.length+radius+2, flip ? start.getX() : end.getX());
			int y = flip ? end.getY() : start.getY();
			int z = flip ? end.getZ() : start.getZ();
			int py = 2*ady - adx;
			int pz = 2*adz - adx;
			for(int x = sx; x <= ex; x++) {
				if(y )
			}
		} else if(ady >= adz) {
			
		} else {
			
		}
	}
	
	@Override
	public boolean carve(IChunk chunk, Function<BlockPos, Biome> biomefunc, Random rand, int sealevel,
			int startChunkX, int startChunkZ, int currentChunkX, int currentChunkZ, BitSet p_225555_9_,
			EmptyCarverConfig p_225555_10_) {
		if(startChunkX != currentChunkX || startChunkZ != currentChunkZ) return false;
		Instant start = CellularCaves.debugInfo ? Instant.now() : null;
		Random r = getSeededRandom(CellularCaves.seed, 35);
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
		
		

		final int rounds = 12;
		xl = rounds+16+rounds; yl = rounds+256+rounds; zl = rounds+16+rounds;
		array = new BType[xl][yl][zl];
		arrayT = new BType[xl][yl][zl];
		for(int x = 0; x < xl; x++) {
			for(int y = 0; y < yl; y++) {
				for(int z = 0; z < zl; z++) {
					int xo = ((x >> 3) + (cpos.x << 1));
					int yo = (y >> 2);
					int zo = ((z >> 3) + (cpos.z << 1));
					array[x][y][z] = ((xo&3) == ((yo&2) == 0 ? 0 : 2)) ||
							((zo&3) == (((yo+1)&2) == 0 ? 0 : 2)) ? BType.EMPTY : BType.FILLED;
				}
			}
		}
		blurCA(array, 0.6f, 0.4f, r.nextLong(), cpos.x<<4, 0, cpos.z<<4);
		runCA(array, arrayT);
		runCA(arrayT, array);
		runCA(array, arrayT);
		runCA(arrayT, array);
		runCA(array, arrayT);
		runCA(arrayT, array);
//		blurCA(array, 0.7f, 0.45f, r.nextLong(), cpos.x<<4, 0, cpos.z<<4);
		runCA(array, arrayT);
		runCA(arrayT, array);
//		blurCA(array, 0.55f, 0.45f, r.nextLong(), cpos.x<<4, 0, cpos.z<<4);
		runCA(array, arrayT);
		runCA(arrayT, array);
		runCA(array, arrayT);
		runCA(arrayT, array);
		
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
		
		
		for(int x = rounds; x < rounds+16; x++) {
			for(int z = rounds; z < rounds+16; z++) {
				for(int y = rounds+1; y < rounds+11; y++) {
					placeLiquid(array, x, y, z, BType.LAVA);
				}
				for(int y = rounds+62; y < rounds+255; y++) {
//					placeLiquid(array, x, y, z, BType.WATER);
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
					BType bt = array[x+rounds][y+rounds][z+rounds];
					if(bt == BType.FILLED) continue;
					if(bt == BType.LAVA || y <= 5) {
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
						chunk.setBlockState(posy, y <= 5 ? LAVA_BS : CAVE_AIR, false);
					}
				}
			}
		}
		
		if(CellularCaves.debugInfo)
			CellularCaves.LOGGER.info("chunk cave gen took " + Duration.between(start, Instant.now()));
		
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
