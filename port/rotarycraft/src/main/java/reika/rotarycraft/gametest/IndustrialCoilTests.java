/*******************************************************************************
 * Tests for the port's industrial coil (RotaryCraft advanced gears). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.AdvancedGearBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class IndustrialCoilTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Rounding helpers and the charging thresholds: at 1024 J the coil wants 512 Nm and 16384 W; it may give at most 8 Nm. */
	@GameTest(template = PLATFORM)
	public static void coilFollowsTheOriginalNumbers(GameTestHelper h) {
		int[][] pow2 = {{0, 0}, {1, 1}, {3, 4}, {4, 4}, {1000, 1024}};
		for (int[] c : pow2)
			h.assertTrue(AdvancedGearBlockEntity.ceilPow2(c[0]) == c[1], "ceilPow2("+c[0]+") = "+AdvancedGearBlockEntity.ceilPow2(c[0]));
		int[][] pseudo = {{3, 3}, {5, 6}, {7, 8}, {9, 12}, {13, 16}, {250, 256}};
		for (int[] c : pseudo)
			h.assertTrue(AdvancedGearBlockEntity.ceilPseudoPow2(c[0]) == c[1], "ceilPseudoPow2("+c[0]+") = "+AdvancedGearBlockEntity.ceilPseudoPow2(c[0]));
		AdvancedGearBlockEntity te = place(h, RotaryBlocks.INDUSTRIAL_COIL.get(), 4, 2, 4, 0, AdvancedGearBlockEntity.class);
		h.assertTrue(te.getChargingTorque() == 1 && te.getChargingPower() == 1, "empty thresholds");
		te.setEnergy(20*1024);
		h.assertTrue(te.getChargingTorque() == 512 && te.getChargingPower() == 16384 && te.getTorqueCap() == 8,
				"1024 J: "+te.getChargingTorque()+" Nm, "+te.getChargingPower()+" W, cap "+te.getTorqueCap());
		AdvancedGearBlockEntity bed = place(h, RotaryBlocks.BEDROCK_COIL.get(), 6, 2, 4, 0, AdvancedGearBlockEntity.class);
		h.assertTrue(bed.getChargingTorque() == 16 && bed.getMaximumEmission() == 4096 && te.getMaximumEmission() == 1024, "bedrock coil limits");
		h.succeed();
	}

	/**
	 * A gas engine (65536 W at 128 Nm) charges the coil until the charging torque outgrows 128 Nm; then nothing more goes in. With 1 MJ and a
	 * redstone block beside it, it gives 1024 rad/s at the torque cap (256 Nm for 1 MJ, though 1000 were asked for) and runs down.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void coilChargesAndReleases(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		AdvancedGearBlockEntity te = place(h, RotaryBlocks.INDUSTRIAL_COIL.get(), 2, 2, 4, 1, AdvancedGearBlockEntity.class);
		AdvancedGearBlockEntity out = place(h, RotaryBlocks.INDUSTRIAL_COIL.get(), 6, 2, 4, 1, AdvancedGearBlockEntity.class);
		out.setEnergy(20_000_000L);
		out.setReleaseOmega(1024);
		out.setReleaseTorque(1000);
		long[] seen = {-1};
		h.runAtTickTime(60, () -> {
			h.assertTrue(te.getEnergy() > 0 && te.getChargingTorque() > 128, "coil holds "+te.getEnergy()+", needs "+te.getChargingTorque()+" Nm");
			seen[0] = te.getEnergy();
			h.setBlock(new BlockPos(6, 3, 4), Blocks.REDSTONE_BLOCK);
		});
		h.runAtTickTime(70, () -> {
			h.assertTrue(te.getEnergy() == seen[0], "charged on at "+te.getChargingTorque()+" Nm: "+te.getEnergy());
			h.assertTrue(out.getOmega() == 1024 && out.getTorque() == 256 && out.getEnergy() < 20_000_000L, "release "+out.getOmega()+" rad/s @ "+out.getTorque()+" Nm, "+out.getEnergy());
			h.succeed();
		});
	}

	/**
	 * Above 720 MJ (bedrock 240 TJ) it blows up on its next tick. Only the condition is tested: the blasts (four of power 8, the bedrock coil 25)
	 * would crater the ground under the structures of later test batches.
	 */
	@GameTest(template = PLATFORM)
	public static void coilKnowsWhenItIsOvercharged(GameTestHelper h) {
		AdvancedGearBlockEntity te = place(h, RotaryBlocks.INDUSTRIAL_COIL.get(), 4, 3, 4, 0, AdvancedGearBlockEntity.class);
		AdvancedGearBlockEntity bed = place(h, RotaryBlocks.BEDROCK_COIL.get(), 6, 3, 4, 0, AdvancedGearBlockEntity.class);
		te.setEnergy(20*720_000_000L-20);
		bed.setEnergy(20*720_000_000L);
		h.assertFalse(te.isOvercharged() || bed.isOvercharged(), "overcharged below capacity");
		te.setEnergy(20*720_000_000L);
		bed.setEnergy(20*240_000_000_000_000L);
		h.assertTrue(te.isOvercharged() && bed.isOvercharged(), "not overcharged at capacity");
		te.setEnergy(0);
		bed.setEnergy(0);
		h.succeed();
	}
}
