/*******************************************************************************
 * Tests for the ported ReactorCraft control rods and reactor CPU (original TileEntityControlRod, TileEntityCPU, ReactorControlLayout).
 * Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.reactor.ControlRodBlockEntity;
import reika.reactorcraft.reactor.CpuBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ControlTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static ControlRodBlockEntity rod(GameTestHelper h, BlockPos p) {
		h.setBlock(p, ReactorBlocks.CONTROL_ROD.get());
		return (ControlRodBlockEntity)h.getBlockEntity(p);
	}

	private static CpuBlockEntity cpu(GameTestHelper h, BlockPos p) {
		h.setBlock(p, ReactorBlocks.CPU.get());
		return (CpuBlockEntity)h.getBlockEntity(p);
	}

	/** Inserted rods absorb 60 % of the neutrons entering them, retracted rods none. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void rodAbsorbsOnlyWhenInserted(GameTestHelper h) {
		BlockPos p = new BlockPos(4, 1, 4);
		ControlRodBlockEntity rod = rod(h, p);
		int n = 0;
		for (int i = 0; i < 1000; i++)
			if (rod.onNeutron(null, h.getLevel(), h.absolutePos(p)))
				n++;
		h.assertTrue(n > 540 && n < 660, "inserted rod absorbed "+n+" of 1000");
		rod.setActive(false, false);
		h.runAtTickTime(40, () -> {
			h.assertTrue(rod.getRodPosition() == ControlRodBlockEntity.MAXOFFSET && !rod.isActive(), "rod at "+rod.getRodPosition());
			for (int i = 0; i < 100; i++)
				h.assertTrue(!rod.onNeutron(null, h.getLevel(), h.absolutePos(p)), "retracted rod absorbed a neutron");
			h.succeed();
		});
	}

	/** A CPU without shaft power SCRAMs after its first second: the retracting rods drop back in. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void cpuScramsWithoutPower(GameTestHelper h) {
		cpu(h, new BlockPos(5, 1, 4));
		ControlRodBlockEntity rod = rod(h, new BlockPos(4, 1, 4));
		rod.setActive(false, false);
		h.runAtTickTime(15, () -> h.assertTrue(rod.getRodPosition() > 0, "rod not moving up: "+rod.getRodPosition()));
		h.runAtTickTime(40, () -> {
			h.assertTrue(rod.isActive(), "rod not dropped, at "+rod.getRodPosition());
			h.succeed();
		});
	}

	/** DC engine (1024 W, enough for one rod) -> CPU -> rod stack of two: raise all, then insert all; the comparator follows (0 / 15). */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void cpuMovesRodsWithPower(GameTestHelper h) {
		h.setBlock(new BlockPos(6, 1, 4), RotaryBlocks.DC_ENGINE.get());
		((EngineBlockEntity)h.getBlockEntity(new BlockPos(6, 1, 4))).setBlockMetadata(0);
		h.setBlock(new BlockPos(6, 1, 5), Blocks.REDSTONE_BLOCK);
		CpuBlockEntity cpu = cpu(h, new BlockPos(5, 1, 4));
		ControlRodBlockEntity rod = rod(h, new BlockPos(4, 1, 4));
		h.runAtTickTime(40, () -> {
			h.assertTrue(cpu.getPower() >= 1024 && cpu.getNumberRods() == 1, "CPU: "+cpu.getPower()+" W, "+cpu.getNumberRods()+" rods");
			cpu.raiseAllRods();
		});
		h.runAtTickTime(80, () -> {
			h.assertTrue(!rod.isActive() && rod.getRodPosition() == ControlRodBlockEntity.MAXOFFSET, "rod not raised: "+rod.getRodPosition());
			h.assertTrue(cpu.getRedstoneOverride() == 0, "comparator "+cpu.getRedstoneOverride());
			cpu.lowerAllRods();
		});
		h.runAtTickTime(120, () -> {
			h.assertTrue(rod.isActive(), "rod not inserted: "+rod.getRodPosition());
			h.assertTrue(cpu.getRedstoneOverride() == 15, "comparator "+cpu.getRedstoneOverride());
			h.succeed();
		});
	}

	/** Two rods need 2048 W: with the DC engine's 1024 W the CPU SCRAMs again right after the rods are raised. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void cpuNeedsPowerPerRod(GameTestHelper h) {
		h.setBlock(new BlockPos(6, 1, 4), RotaryBlocks.DC_ENGINE.get());
		((EngineBlockEntity)h.getBlockEntity(new BlockPos(6, 1, 4))).setBlockMetadata(0);
		h.setBlock(new BlockPos(6, 1, 5), Blocks.REDSTONE_BLOCK);
		CpuBlockEntity cpu = cpu(h, new BlockPos(5, 1, 4));
		ControlRodBlockEntity a = rod(h, new BlockPos(4, 1, 4));
		ControlRodBlockEntity b = rod(h, new BlockPos(4, 1, 3));
		h.runAtTickTime(40, () -> {
			h.assertTrue(cpu.getNumberRods() == 2 && cpu.getMinPower() == 2048, "CPU: "+cpu.getNumberRods()+" rods, needs "+cpu.getMinPower());
			cpu.raiseAllRods();
		});
		h.runAtTickTime(60, () -> {
			h.assertTrue(a.isActive() && b.isActive(), "rods not kept in: "+a.getRodPosition()+", "+b.getRodPosition());
			h.succeed();
		});
	}
}
