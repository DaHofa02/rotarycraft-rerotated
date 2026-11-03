/*******************************************************************************
 * Tests for the ported RotaryCraft heat ray (original TileEntityHeatRay). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.HeatRayBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class HeatRayTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Microturbine (131072 rad/s @ 16 Nm = 2 MW, meta 1: output east) into a heat ray (meta 1: input west, beam east): range 8. */
	private static HeatRayBlockEntity rig(GameTestHelper h) {
		EngineBlockEntity e = place(h, RotaryBlocks.MICRO_ENGINE.get(), 1, 1, 4, 1, EngineBlockEntity.class);
		IFluidHandler fh = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, e.getBlockPos(), Direction.DOWN);
		h.assertTrue(fh != null && fh.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 20000), IFluidHandler.FluidAction.EXECUTE) > 0, "no jet fuel taken");
		return place(h, RotaryBlocks.HEAT_RAY.get(), 2, 1, 4, 1, HeatRayBlockEntity.class);
	}

	/** At 2 MW (the microturbine needs about 1930 ticks to spin up, +68 rad/s per tick): sand 3 blocks ahead becomes glass (after 4*3/1 ticks), then cobblestone 5 ahead melts to lava. */
	@GameTest(template = PLATFORM, timeoutTicks = 2400)
	public static void heatRayMeltsAndGlassifies(GameTestHelper h) {
		HeatRayBlockEntity ray = rig(h);
		h.setBlock(new BlockPos(5, 0, 4), Blocks.STONE); //the template has no floor under y=1
		h.setBlock(new BlockPos(5, 1, 4), Blocks.SAND);
		h.setBlock(new BlockPos(7, 1, 4), Blocks.COBBLESTONE);
		h.succeedWhen(() -> {
			h.assertTrue(ray.getPower() == 2097152 && ray.getRange() == 8, "heat ray "+ray.getPower()+" W, range "+ray.getRange());
			h.assertBlockPresent(Blocks.GLASS, new BlockPos(5, 1, 4));
			h.assertBlockPresent(Blocks.LAVA, new BlockPos(7, 1, 4));
		});
	}

	/** A cow in the beam is set on fire for 2 + 16 = 18 seconds. */
	@GameTest(template = PLATFORM, timeoutTicks = 2400)
	public static void heatRayBurnsEntities(GameTestHelper h) {
		HeatRayBlockEntity ray = rig(h);
		h.setBlock(new BlockPos(5, 0, 4), Blocks.STONE); //the template has no floor under y=1
		Cow cow = h.spawn(EntityType.COW, new BlockPos(5, 1, 4));
		cow.setNoAi(true);
		h.succeedWhen(() -> {
			h.assertTrue(ray.getBurnTime() == 18, "burn time "+ray.getBurnTime());
			h.assertTrue(cow.isOnFire(), "cow not burning");
		});
	}

	/** A gas engine (65536 W) is far below the 2 MW the heat ray needs: the sand stays. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void heatRayNeedsTwoMegawatts(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 1, 4, 1, EngineBlockEntity.class).addFuel(50000);
		place(h, RotaryBlocks.HEAT_RAY.get(), 2, 1, 4, 1, HeatRayBlockEntity.class);
		h.setBlock(new BlockPos(4, 0, 4), Blocks.STONE); //the template has no floor under y=1
		h.setBlock(new BlockPos(4, 1, 4), Blocks.SAND);
		h.runAtTickTime(60, () -> {
			h.assertBlockPresent(Blocks.SAND, new BlockPos(4, 1, 4));
			h.succeed();
		});
	}
}
