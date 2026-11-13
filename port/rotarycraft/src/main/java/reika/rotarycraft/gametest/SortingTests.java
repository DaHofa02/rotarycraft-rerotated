/*******************************************************************************
 * Tests for the ported RotaryCraft item sorter (original TileEntitySorting). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.SortingBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SortingTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	private static int count(GameTestHelper h, BlockPos p, Item item) {
		Container c = (Container)h.getBlockEntity(p);
		int n = 0;
		for (int i = 0; i < c.getContainerSize(); i++)
			if (c.getItem(i).is(item))
				n += c.getItem(i).getCount();
		return n;
	}

	/** Facing east, the three rows send north, south and west; unmapped items go down; an item mapped twice is refused; items per tick by power. */
	@GameTest(template = PLATFORM)
	public static void sorterMapsRowsToSides(GameTestHelper h) {
		SortingBlockEntity te = place(h, RotaryBlocks.SORTING.get(), 4, 2, 4, 0, SortingBlockEntity.class);
		h.assertTrue(te.getFacing() == Direction.EAST && h.getBlockState(new BlockPos(4, 2, 4)).getValue(reika.rotarycraft.content.block.SortingBlock.FACING) == Direction.EAST, "facing");
		te.setMapping(0, new ItemStack(Items.COBBLESTONE, 5));
		te.setMapping(9, new ItemStack(Items.DIRT));
		te.setMapping(26, new ItemStack(Items.SAND));
		h.assertFalse(te.setMapping(1, new ItemStack(Items.DIRT)), "dirt mapped twice");
		h.assertTrue(te.getMapping(0).getCount() == 1, "mapping not one item");
		h.assertTrue(te.getSideForItem(new ItemStack(Items.COBBLESTONE)) == Direction.NORTH && te.getSideForItem(new ItemStack(Items.DIRT)) == Direction.SOUTH
				&& te.getSideForItem(new ItemStack(Items.SAND)) == Direction.WEST && te.getSideForItem(new ItemStack(Items.STICK)) == Direction.DOWN, "sides");
		long[][] cases = {{1024, 1}, {2048, 2}, {4096, 4}, {8192, 16}, {12288, 32}, {65536, 64}};
		for (long[] c : cases) {
			te.power = c[0];
			h.assertTrue(te.getNumberCyclesPerTick() == c[1], c[0]+" W: "+te.getNumberCyclesPerTick());
		}
		h.succeed();
	}

	/** Powered by a gas engine, it empties the chest above into the chests on its mapped sides and the one below. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void sorterSortsAChest(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
		SortingBlockEntity te = place(h, RotaryBlocks.SORTING.get(), 4, 2, 4, 0, SortingBlockEntity.class);
		te.setMapping(0, new ItemStack(Items.COBBLESTONE));
		te.setMapping(9, new ItemStack(Items.DIRT));
		te.setMapping(18, new ItemStack(Items.SAND));
		BlockPos up = new BlockPos(4, 3, 4), n = new BlockPos(4, 2, 3), s = new BlockPos(4, 2, 5), w = new BlockPos(3, 2, 4), d = new BlockPos(4, 1, 4);
		for (BlockPos p : new BlockPos[] {up, n, s, w, d})
			h.setBlock(p, Blocks.CHEST);
		Container top = (Container)h.getBlockEntity(up);
		top.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
		top.setItem(4, new ItemStack(Items.DIRT, 3));
		top.setItem(9, new ItemStack(Items.SAND, 2));
		top.setItem(20, new ItemStack(Items.STICK, 1));
		h.succeedWhen(() -> {
			h.assertTrue(top.isEmpty(), "chest above not empty; sorter at "+te.power+" W");
			h.assertTrue(count(h, n, Items.COBBLESTONE) == 5 && count(h, s, Items.DIRT) == 3 && count(h, w, Items.SAND) == 2 && count(h, d, Items.STICK) == 1,
					"north "+count(h, n, Items.COBBLESTONE)+", south "+count(h, s, Items.DIRT)+", west "+count(h, w, Items.SAND)+", down "+count(h, d, Items.STICK));
		});
	}

	/** Without an inventory above, items lying on top are sorted. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void sorterSortsItemsOnTop(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
		SortingBlockEntity te = place(h, RotaryBlocks.SORTING.get(), 4, 2, 4, 0, SortingBlockEntity.class);
		te.setMapping(0, new ItemStack(Items.COBBLESTONE));
		BlockPos n = new BlockPos(4, 2, 3);
		h.setBlock(n, Blocks.CHEST);
		BlockPos at = h.absolutePos(new BlockPos(4, 3, 4));
		ItemEntity e = new ItemEntity(h.getLevel(), at.getX()+0.5, at.getY()+0.05, at.getZ()+0.5, new ItemStack(Items.COBBLESTONE, 3), 0, 0, 0);
		h.getLevel().addFreshEntity(e);
		h.succeedWhen(() -> h.assertTrue(count(h, n, Items.COBBLESTONE) == 3 && !e.isAlive(), "north chest has "+count(h, n, Items.COBBLESTONE)));
	}
}
