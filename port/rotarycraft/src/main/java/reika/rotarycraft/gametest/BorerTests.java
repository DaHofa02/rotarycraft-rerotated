/*******************************************************************************
 * Tests for the ported RotaryCraft borer (original TileEntityBorer). Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for
 * distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BorerBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BorerTests {

	/** The first slice is always cut whole (7 wide, 5 high), so the tests need the tall platform. Its stone floor is at y = 1. */
	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Borer at (7, 2, 4) (meta 0: input east, digs west) with only the centre cell at its own height selected. */
	private static BorerBlockEntity borer(GameTestHelper h) {
		BorerBlockEntity b = place(h, RotaryBlocks.BORER.get(), 7, 2, 4, 0, BorerBlockEntity.class);
		b.setCut(3, 4, true);
		return b;
	}

	/** Original calculateTorqueForHardness: ceilPseudo2Exp(10 * hardness), i.e. the next 2^n or 1.5 * 2^n. */
	@GameTest(template = "platform_9x4x9")
	public static void borerTorqueFollowsHardness(GameTestHelper h) {
		h.assertTrue(BorerBlockEntity.calculateTorqueForHardness(0.5F, 0) == 6, "dirt: "+BorerBlockEntity.calculateTorqueForHardness(0.5F, 0));
		h.assertTrue(BorerBlockEntity.calculateTorqueForHardness(1.5F, 0) == 16, "stone: "+BorerBlockEntity.calculateTorqueForHardness(1.5F, 0));
		h.assertTrue(BorerBlockEntity.calculateTorqueForHardness(50F, 0) == 512, "obsidian: "+BorerBlockEntity.calculateTorqueForHardness(50F, 0));
		h.succeed();
	}

	/**
	 * Gas engine (512 rad/s @ 128 Nm = 65536 W): three dirt blocks (hardness 0.5: 64 * 10 * 0.5 = 320 W and 6 Nm each) become mining pipes,
	 * the dirt goes into the chest on top of the borer, and the Borer Shield Block behind them jams it.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 2500)
	public static void borerDigsDirtAndJamsAtTheShield(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 8, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
		BorerBlockEntity b = borer(h);
		BlockPos chest = new BlockPos(7, 3, 4);
		h.setBlock(chest, Blocks.CHEST);
		for (int x = 6; x >= 4; x--)
			h.setBlock(new BlockPos(x, 2, 4), Blocks.DIRT);
		BlockPos shield = new BlockPos(3, 2, 4);
		h.setBlock(shield, RotaryBlocks.SHIELD_BLOCK.get());
		boolean[] seen = {false};
		h.succeedWhen(() -> {
			if (b.getRequiredPower() == 320 && b.getRequiredTorque() == 6)
				seen[0] = true;
			for (int x = 6; x >= 4; x--)
				h.assertBlockPresent(RotaryBlocks.MINING_PIPE.get(), new BlockPos(x, 2, 4));
			h.assertBlockPresent(RotaryBlocks.MINING_PIPE.get(), new BlockPos(6, 6, 1)); //the first slice is cut whole
			h.assertBlockPresent(Blocks.AIR, new BlockPos(5, 3, 4)); //later slices only where selected
			h.assertBlockPresent(RotaryBlocks.SHIELD_BLOCK.get(), shield);
			h.assertTrue(b.isJammed() && b.getRequiredPower() == -1 && b.getRedstoneOverride() == 15, "not jammed at the shield, step "+b.getStep());
			h.assertTrue(seen[0], "never saw 320 W / 6 Nm for a dirt slice");
			h.assertTrue(b.getOperationTime() == 359, "slice time "+b.getOperationTime()); //720 - 40 * log2(513)
			int dirt = ((Container)h.getBlockEntity(chest)).countItem(Items.DIRT);
			h.assertTrue(dirt == 3, dirt+" dirt in the chest");
		});
	}

	/** A DC engine (256 rad/s @ 4 Nm) has enough power for dirt (1024 W >= 320 W) but not the torque (6 Nm): the borer jams and digs nothing. */
	@GameTest(template = PLATFORM, timeoutTicks = 1000)
	public static void borerJamsWithoutTorque(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 8, 2, 4, 0, EngineBlockEntity.class);
		h.setBlock(new BlockPos(8, 2, 3), Blocks.REDSTONE_BLOCK);
		BorerBlockEntity b = borer(h);
		BlockPos dirt = new BlockPos(6, 2, 4);
		h.setBlock(dirt, Blocks.DIRT);
		h.runAtTickTime(900, () -> {
			h.assertTrue(b.getPower() == 1024 && b.getOperationTime() == 399, "borer "+b.getPower()+" W, slice time "+b.getOperationTime());
			h.assertTrue(b.isJammed() && b.getRequiredTorque() == 6 && b.getRedstoneOverride() == 15, "not jammed, torque needed "+b.getRequiredTorque());
			h.assertBlockPresent(Blocks.DIRT, dirt);
			h.assertTrue(b.getStep() == 1, "step "+b.getStep());
			h.succeed();
		});
	}
}
