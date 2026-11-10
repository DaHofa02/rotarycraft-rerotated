/*******************************************************************************
 * Tests for the ported RotaryCraft aggregator (original TileEntityAggregator). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.AggregatorBlockEntity;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.lib.world.BiomeHumidity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AggregatorTests {

	/** The stone floor is at y = 1; the rigs stand on it at y = 2. */
	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/**
	 * Aggregator at (4, 3, 4) on bevel gears (east -> up) fed by a gas engine at (6, 2, 4), with a steel gearbox 8:1 between them when
	 * {@code fast} (4096 rad/s @ 16 Nm) and a shaft-less direct drive otherwise (512 rad/s @ 128 Nm, engine at (5, 2, 4)).
	 */
	private static AggregatorBlockEntity rig(GameTestHelper h, boolean fast) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), fast ? 6 : 5, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
		if (fast) {
			GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 5, 2, 4, 8, GearboxBlockEntity.class);
			gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(reika.rotarycraft.registry.GearboxTypes.STEEL, 8));
			gbx.setBlockMetadata(8);
			gbx.fillWithLubricant();
			gbx.reduction = false;
		}
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		return place(h, RotaryBlocks.AGGREGATOR.get(), 4, 3, 4, 0, AggregatorBlockEntity.class);
	}

	private static float humidity(GameTestHelper h, ResourceKey<Biome> biome) {
		return BiomeHumidity.get(h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biome));
	}

	/** The humidity table of the original (ReikaBiomeHelper.getBiomeHumidity). */
	@GameTest(template = PLATFORM)
	public static void biomeHumidityFollowsTheOriginalTable(GameTestHelper h) {
		Object[][] table = {{Biomes.JUNGLE, 0.95F}, {Biomes.OCEAN, 1F}, {Biomes.DEEP_OCEAN, 1F}, {Biomes.SWAMP, 0.85F}, {Biomes.FOREST, 0.6F},
				{Biomes.BIRCH_FOREST, 0.55F}, {Biomes.DARK_FOREST, 0.7F}, {Biomes.PLAINS, 0.4F}, {Biomes.SAVANNA, 0.3F}, {Biomes.DESERT, 0.2F},
				{Biomes.BADLANDS, 0.2F}, {Biomes.NETHER_WASTES, 0.1F}, {Biomes.THE_END, 0.1F}, {Biomes.BEACH, 0.98F}, {Biomes.SNOWY_PLAINS, 0.4F},
				{Biomes.MUSHROOM_FIELDS, 0.75F}, {Biomes.RIVER, 1F}};
		for (Object[] row : table) {
			@SuppressWarnings("unchecked")
			float is = humidity(h, (ResourceKey<Biome>)row[0]);
			h.assertTrue(is == (Float)row[1], row[0]+": humidity "+is+", expected "+row[1]);
		}
		h.succeed();
	}

	/**
	 * 4096 rad/s @ 16 Nm: max(2, (int)(16^2 * humidity)) mB over (int)(80 - 5 * log2(4097)) = 19 ticks, 102/19 = 5 mB per tick in plains
	 * (humidity 0.4). The aggregator stays below the ambient temperature; the redstone signal follows the level.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void aggregatorCondensesWater(GameTestHelper h) {
		AggregatorBlockEntity te = rig(h, true);
		Holder<Biome> biome = h.getLevel().getBiome(h.absolutePos(new BlockPos(4, 3, 4)));
		int made = Math.max(2, (int)(256*BiomeHumidity.get(biome)));
		h.assertTrue(BiomeHumidity.get(biome) != 0.4F || made == 102, "plains make "+made+" mB");
		int[] last = {-1};
		h.succeedWhen(() -> {
			h.assertTrue(te.omega == 4096 && te.torque == 16, "aggregator input "+te.omega+" rad/s @ "+te.torque+" Nm");
			h.assertTrue(te.getProductionPerTick(biome) == made/19, "makes "+te.getProductionPerTick(biome)+" mB/t, expected "+made/19);
			int now = te.getWater();
			int before = last[0];
			last[0] = now;
			h.assertTrue(before > 0 && now-before == made/19, "water went from "+before+" to "+now);
			h.assertTrue(te.getTemperature() < AmbientTemperature.getAmbientTemperatureAt(h.getLevel(), te.getBlockPos()), "at "+te.getTemperature()+" C");
			h.assertTrue(te.getRedstoneOverride() == 15*now/128000, "redstone "+te.getRedstoneOverride());
		});
	}

	/** 512 rad/s is below the 4096 rad/s minimum although 65536 W are plenty: no water. */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void aggregatorNeedsSpeed(GameTestHelper h) {
		AggregatorBlockEntity te = rig(h, false);
		h.runAtTickTime(300, () -> {
			h.assertTrue(te.omega == 512 && te.getPower() == 65536, "aggregator input "+te.omega+" rad/s, "+te.getPower()+" W");
			h.assertTrue(te.getWater() == 0 && te.getProductionPerTick(h.getLevel().getBiome(te.getBlockPos())) == 0, "made "+te.getWater()+" mB");
			h.succeed();
		});
	}

	/**
	 * Heated above ambient it makes nothing, and cooling by a quarter of the difference per second (integer division) leaves it three
	 * degrees too warm for good: it needs cooling.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 800)
	public static void aggregatorStallsWhenWarm(GameTestHelper h) {
		AggregatorBlockEntity te = rig(h, true);
		te.setTemperature(90);
		h.runAtTickTime(600, () -> {
			int amb = AmbientTemperature.getAmbientTemperatureAt(h.getLevel(), te.getBlockPos());
			h.assertTrue(te.omega == 4096, "aggregator input "+te.omega+" rad/s");
			h.assertTrue(te.getTemperature() == amb+3, "at "+te.getTemperature()+" C, ambient "+amb);
			h.assertTrue(te.getWater() == 0, "made "+te.getWater()+" mB while warm");
			h.succeed();
		});
	}

	/** The tank empties sideways: a pipe draws from it, a reservoir is filled; nothing leaves upwards. */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void aggregatorFeedsPipesAndTanks(GameTestHelper h) {
		AggregatorBlockEntity te = rig(h, true);
		BlockPos pipe = new BlockPos(3, 3, 4), tank = new BlockPos(4, 3, 3), above = new BlockPos(4, 4, 4);
		h.setBlock(pipe, RotaryBlocks.PIPE.get());
		h.setBlock(tank, RotaryBlocks.RESERVOIR.get());
		h.setBlock(above, RotaryBlocks.RESERVOIR.get());
		h.succeedWhen(() -> {
			PipingBlockEntity p = (PipingBlockEntity)h.getBlockEntity(pipe);
			h.assertTrue(p.getFluidLevel() > 0 && p.getFluidType() == Fluids.WATER, "pipe holds "+p.getFluidLevel());
			IFluidHandler side = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(tank), null);
			IFluidHandler top = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(above), null);
			h.assertTrue(side.getFluidInTank(0).getAmount() >= 100 && side.getFluidInTank(0).is(Fluids.WATER), "reservoir holds "+side.getFluidInTank(0).getAmount());
			h.assertTrue(top.getFluidInTank(0).isEmpty(), "water went upwards");
		});
	}
}
