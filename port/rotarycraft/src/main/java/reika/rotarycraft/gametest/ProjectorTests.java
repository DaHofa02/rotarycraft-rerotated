/*******************************************************************************
 * Tests for the port's projector. Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.ProjectorBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ProjectorTests {

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
	 * A gas engine behind the projector (meta 1: input from the west, it projects east), a stone wall 6 blocks in front: with a filled map
	 * it shows, 3 blocks square. Only maps go in; a glass pane is no screen.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void projectorShowsAMap(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 0, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		ProjectorBlockEntity te = place(h, RotaryBlocks.PROJECTOR.get(), 1, 2, 4, 1, ProjectorBlockEntity.class);
		h.setBlock(new BlockPos(7, 2, 4), Blocks.STONE);
		h.assertTrue(te.getFacing() == Direction.EAST, "faces "+te.getFacing());
		h.assertTrue(te.getInventory().insertItem(0, new ItemStack(Items.PAPER), true).getCount() == 1, "paper accepted");
		ItemStack map = MapItem.create(h.getLevel(), 0, 0, (byte)0, false, false);
		te.getInventory().setStackInSlot(0, map);
		boolean[] pane = {false};
		h.succeedWhen(() -> {
			if (!pane[0]) {
				h.assertTrue(te.canShow() && te.getRange() == 6 && te.getImageSize() == 3, "range "+te.getRange()+", shows "+te.canShow()+" at "+te.getPower()+" W");
				h.setBlock(new BlockPos(4, 2, 4), Blocks.GLASS_PANE);
				pane[0] = true;
				h.fail("pane");
			}
			h.assertTrue(te.getRange() == 0 && !te.canShow(), "pane is a screen: range "+te.getRange());
		});
	}
}
