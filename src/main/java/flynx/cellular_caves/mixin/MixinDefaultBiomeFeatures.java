package flynx.cellular_caves.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import flynx.cellular_caves.CellularCarver;
import net.minecraft.world.biome.BiomeGenerationSettings.Builder;
import net.minecraft.world.biome.DefaultBiomeFeatures;
import net.minecraft.world.gen.GenerationStage.Carving;

@Mixin(DefaultBiomeFeatures.class)
public abstract class MixinDefaultBiomeFeatures {
	@Inject(method = {"addDefaultCarvers", "addOceanCarvers"}, cancellable = true,
			at = @At(value = "HEAD"))
	private static void caveCarver(Builder b, CallbackInfo c) {
		try {
			b.addCarver(Carving.LIQUID, CellularCarver.CONFIGURED);
			c.cancel();
		} catch(Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
