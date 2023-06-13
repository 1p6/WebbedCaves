package flynx.cellular_caves;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.electronwill.nightconfig.core.CommentedConfig;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.ModConfigEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CellularCaves.MODID)
public class CellularCaves
{
	public static final String MODID = "cellular_caves";
	public static final Logger LOGGER = LogManager.getLogger();
	public static long seed = 0L;
	public static int surfaceChance;
	public static int ravineChance;
	public static int magmaChance;
	public static int iterations;
	public static boolean debugInfo;
	private final ForgeConfigSpec configSpec;

	public CellularCaves() {
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, ()->Pair.of(
				()->FMLNetworkConstants.IGNORESERVERONLY,
				(s,b)->true
				));
		
		ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
		b.comment("1/n chance for how likely a chunk is to break through to the surface.",
				"Set to 0 to make chunks never reach the surface.")
		.defineInRange("surfaceChance", 16, 0, Integer.MAX_VALUE);
		b.comment("", "1/n chance for how likely a chunk is to generate as columns",
				"Set to 0 to make chunks never generate as columns")
		.defineInRange("ravineChance", 16, 0, Integer.MAX_VALUE);
		b.comment("", "1/n chance for how likely a lava source block will be a magma block in underwater caves",
				"Set to 0 to keep all lava source blocks as obsidian.")
		.defineInRange("magmaChance", 8, 0, Integer.MAX_VALUE);
		b.comment("", "half the number of cellular automata iterations that will be performed")
		.defineInRange("iterations", 3, 0, 8);
		b.comment("", "whether debug info should be logged")
		.define("debugInfo", false);
		configSpec = b.build();
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, configSpec);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigChange);
		
		MinecraftForge.EVENT_BUS.addListener(CellularCaves::onServerStart);
	}

	public static void onServerStart(FMLServerAboutToStartEvent ev) {
		seed = ev.getServer().getWorldData().worldGenSettings().seed();
	}
	
	public void onConfigChange(ModConfigEvent e) {
		CommentedConfig c = e.getConfig().getConfigData();
		surfaceChance = c.getInt("surfaceChance");
		ravineChance = c.getInt("ravineChance");
		magmaChance = c.getInt("magmaChance");
		iterations = c.getInt("iterations");
		debugInfo = c.get("debugInfo");
	}
	
}
