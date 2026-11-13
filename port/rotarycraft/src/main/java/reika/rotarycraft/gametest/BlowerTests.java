/*******************************************************************************
 * Tests for the ported RotaryCraft item pump (original TileEntityBlower). Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki;
 * not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BlowerBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BlowerTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Chest -> blower (powered by a gas engine beside it, 65536 W: 64 items a tick) -> blower -> chest, along z = 4 eastwards. */
	private static BlowerBlockEntity line(GameTestHelper h, int z, boolean endChest) {
		h.setBlock(new BlockPos(2, 2, z), Blocks.CHEST);
		BlowerBlockEntity b = place(h, RotaryBlocks.BLOWER.get(), 3, 2, z, 0, BlowerBlockEntity.class);
		place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, z+1, 2, EngineBlockEntity.class).addFuel(50000); //meta 2: output north into the blower
		place(h, RotaryBlocks.BLOWER.get(), 4, 2, z, 0, BlowerBlockEntity.class);
		if (endChest)
			h.setBlock(new BlockPos(5, 2, z), Blocks.CHEST);
		return b;
	}

	private static Container chest(GameTestHelper h, int x, int z) {
		return (Container)h.getBlockEntity(new BlockPos(x, 2, z));
	}

	/** Power/1024 items per tick; facing per metadata; the filter (blacklist by default, whitelist when switched). */
	@GameTest(template = PLATFORM)
	public static void blowerFollowsTheOriginalNumbers(GameTestHelper h) {
		BlowerBlockEntity te = place(h, RotaryBlocks.BLOWER.get(), 4, 2, 4, 5, BlowerBlockEntity.class);
		h.assertTrue(te.getFacingDir() == net.minecraft.core.Direction.UP, "facing "+te.getFacingDir());
		te.power = 65536;
		h.assertTrue(te.getNumberTransferrableItems() == 64, te.getNumberTransferrableItems()+" items a tick");
		h.assertTrue(te.isItemTransferrable(new ItemStack(Items.DIRT)), "empty blacklist blocks dirt");
		te.setFilter(0, new ItemStack(Items.DIRT, 5));
		h.assertFalse(te.isItemTransferrable(new ItemStack(Items.DIRT)), "blacklisted dirt passes");
		te.toggle(0);
		h.assertTrue(te.isItemTransferrable(new ItemStack(Items.DIRT)) && !te.isItemTransferrable(new ItemStack(Items.STONE)), "whitelist");
		h.succeed();
	}

	/** Two blowers in a line move the first chest into the last; a whitelist for cobblestone leaves the dirt behind. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void blowerLineMovesItems(GameTestHelper h) {
		line(h, 2, true);
		BlowerBlockEntity filtered = line(h, 5, true);
		filtered.setFilter(3, new ItemStack(Items.COBBLESTONE));
		filtered.toggle(0);
		for (int z : new int[] {2, 5}) {
			chest(h, 2, z).setItem(0, new ItemStack(Items.COBBLESTONE, 40));
			chest(h, 2, z).setItem(7, new ItemStack(Items.DIRT, 10));
		}
		h.succeedWhen(() -> {
			h.assertTrue(chest(h, 2, 2).isEmpty() && chest(h, 5, 2).countItem(Items.COBBLESTONE) == 40 && chest(h, 5, 2).countItem(Items.DIRT) == 10, "line not moved");
			h.assertTrue(chest(h, 5, 5).countItem(Items.COBBLESTONE) == 40 && chest(h, 2, 5).countItem(Items.DIRT) == 10 && chest(h, 5, 5).countItem(Items.DIRT) == 0, "filter");
		});
	}

	/** With air at the end of the line the items are spilled out. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void blowerSpillsIntoAir(GameTestHelper h) {
		line(h, 4, false);
		chest(h, 2, 4).setItem(0, new ItemStack(Items.COBBLESTONE, 5));
		h.succeedWhen(() -> {
			int n = 0;
			for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(5, 2, 4))).inflate(6)))
				n += e.getItem().is(Items.COBBLESTONE) ? e.getItem().getCount() : 0;
			h.assertTrue(chest(h, 2, 4).isEmpty() && n == 5, n+" cobblestone spilled");
		});
	}
}
