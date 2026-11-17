/*******************************************************************************
 * Tests for the ported RotaryCraft GPR (original TileEntityGPR). Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for
 * distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.GprBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class GprTests {

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
	 * Gas engine (65536 W): range 2 * log2(65536 - 32768) = 30. Unflipped the slice runs north-south: a gold block two south of it, one
	 * down, shows in column +2 at depth 1 in gold's colour. Moving the scan point one east brings a diamond block there into column 0;
	 * flipped, the slice runs east-west.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void gprScansTheGround(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		GprBlockEntity te = place(h, RotaryBlocks.GPR.get(), 2, 2, 4, 0, GprBlockEntity.class);
		h.setBlock(new BlockPos(2, 1, 6), Blocks.GOLD_BLOCK);
		h.setBlock(new BlockPos(3, 1, 4), Blocks.DIAMOND_BLOCK);
		h.setBlock(new BlockPos(4, 1, 4), Blocks.EMERALD_BLOCK);
		int[] phase = {0};
		h.succeedWhen(() -> {
			h.assertTrue(te.getRange() == 30, "range "+te.getRange()+" at "+te.getPower()+" W");
			switch (phase[0]) {
				case 0 -> {
					h.assertTrue(te.getSliceDirection().getAxis() == Direction.Axis.Z, "slice "+te.getSliceDirection());
					h.assertTrue(Blocks.GOLD_BLOCK.defaultBlockState().equals(te.getBlock(2, 1)) && te.getColor(2, 1) == MapColor.GOLD.col, "column 2: "+te.getBlock(2, 1));
					te.shift(te.getGuiDirection(), 1);
					phase[0] = 1;
					h.fail("scanned; now shifted");
				}
				case 1 -> {
					h.assertTrue(Blocks.DIAMOND_BLOCK.defaultBlockState().equals(te.getBlock(0, 1)), "column 0 after the shift: "+te.getBlock(0, 1));
					te.flipDirection();
					te.shift(Direction.EAST, 0);
					phase[0] = 2;
					h.fail("shifted; now flipped");
				}
				default -> h.assertTrue(Blocks.EMERALD_BLOCK.defaultBlockState().equals(te.getBlock(2, 1)), "flipped column 2: "+te.getBlock(2, 1));
			}
		});
	}
}
