/*******************************************************************************
 * Tests for the ported RotaryCraft item refresher (original TileEntityItemRefresher). Private NeoForge 1.21.1 port of RotaryCraft by
 * Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.ItemRefresherBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ItemRefresherTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	private static ItemEntity item(GameTestHelper h, int lifespan) {
		ItemEntity e = new ItemEntity(h.getLevel(), h.absolutePos(new BlockPos(4, 2, 6)).getX()+0.5, h.absolutePos(BlockPos.ZERO).getY()+2.2,
				h.absolutePos(new BlockPos(4, 2, 6)).getZ()+0.5, new ItemStack(Items.DIAMOND), 0, 0, 0);
		e.lifespan = lifespan;
		e.setNeverPickUp();
		h.getLevel().addFreshEntity(e);
		return e;
	}

	/** Range 4 + (power - 16384)/1024 blocks, at most 128; nothing below 16384 W. */
	@GameTest(template = PLATFORM)
	public static void refresherRangeFollowsTheOriginal(GameTestHelper h) {
		ItemRefresherBlockEntity te = place(h, RotaryBlocks.ITEM_REFRESHER.get(), 4, 2, 4, 0, ItemRefresherBlockEntity.class);
		long[][] cases = {{16384, 4}, {65536, 52}, {1L << 30, 128}};
		for (long[] c : cases) {
			te.power = c[0];
			h.assertTrue(te.getRange() == c[1], c[0]+" W: range "+te.getRange());
		}
		h.succeed();
	}

	/** Powered by a gas engine (65536 W), an item whose lifespan is 60 ticks is still there after 150; without power it despawns. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void refresherKeepsItemsAlive(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
		ItemRefresherBlockEntity te = place(h, RotaryBlocks.ITEM_REFRESHER.get(), 4, 2, 4, 0, ItemRefresherBlockEntity.class);
		ItemEntity kept = item(h, 60);
		h.runAtTickTime(150, () -> {
			h.assertTrue(te.power == 65536, "refresher at "+te.power+" W");
			h.assertTrue(kept.isAlive() && kept.lifespan > 60, "item gone or lifespan "+kept.lifespan);
			h.succeed();
		});
	}

	/** Without power the item despawns at its lifespan as usual. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void refresherNeedsPower(GameTestHelper h) {
		place(h, RotaryBlocks.ITEM_REFRESHER.get(), 4, 2, 4, 0, ItemRefresherBlockEntity.class);
		ItemEntity gone = item(h, 60);
		h.runAtTickTime(150, () -> {
			h.assertFalse(gone.isAlive(), "item still there");
			h.succeed();
		});
	}
}
