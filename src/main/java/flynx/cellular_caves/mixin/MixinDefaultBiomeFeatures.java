package flynx.cellular_caves.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import flynx.cellular_caves.CellularCarver;
import net.minecraft.world.biome.BiomeGenerationSettings.Builder;
import net.minecraft.world.biome.DefaultBiomeFeatures;
import net.minecraft.world.gen.GenerationStage.Carving;

@Mixin(DefaultBiomeFeatures.class)
public abstract class MixinDefaultBiomeFeatures {
	@Overwrite
	public static void addDefaultCarvers(Builder b) {
		b.addCarver(Carving.LIQUID, CellularCarver.CONFIGURED);
	}
	@Overwrite
	public static void addOceanCarvers(Builder b) {
		b.addCarver(Carving.LIQUID, CellularCarver.CONFIGURED);
	}
}
