/*******************************************************************************
 * Tests for the ported RotaryCraft block filler (original TileEntityBlockFiller / TileEntityAreaFiller). Private NeoForge 1.21.1 port of
 * RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BlockFillerBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BlockFillerTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** A cobblestone basin on the template floor (y = 1): 3x3x3 hollow at x, z = 3..5, y = 2..4, open at the top. */
	private static void basin(GameTestHelper h) {
		for (int x = 2; x <= 6; x++)
			for (int z = 2; z <= 6; z++)
				if (x == 2 || x == 6 || z == 2 || z == 6)
					for (int y = 2; y <= 4; y++)
						h.setBlock(new BlockPos(x, y, z), Blocks.COBBLESTONE);
	}

	/** The filler above the middle of the basin at (4, 5, 4), driven from the west (engine meta 1: output east). */
	private static BlockFillerBlockEntity rig(GameTestHelper h, boolean gas) {
		if (gas) {
			place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 5, 4, 1, EngineBlockEntity.class).addFuel(50000); //512 rad/s @ 128 Nm = 65536 W
		}
		else {
			place(h, RotaryBlocks.DC_ENGINE.get(), 3, 5, 4, 1, EngineBlockEntity.class); //256 rad/s @ 4 Nm = 1024 W
			h.setBlock(new BlockPos(3, 5, 3), Blocks.REDSTONE_BLOCK);
		}
		return place(h, RotaryBlocks.FILLER.get(), 4, 5, 4, 0, BlockFillerBlockEntity.class);
	}

	private static int count(GameTestHelper h, Block b) {
		int n = 0;
		for (int x = 3; x <= 5; x++)
			for (int z = 3; z <= 5; z++)
				for (int y = 2; y <= 4; y++)
					if (h.getBlockState(new BlockPos(x, y, z)).is(b))
						n++;
		return n;
	}

	/** DC engine, exactly the 1024 W dirt needs, one block every (int)(22 - 2*log2(257)) = 5 ticks: 27 of 64 dirt fill the hollow. */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void fillerFillsTheHollowWithDirt(GameTestHelper h) {
		basin(h);
		BlockFillerBlockEntity f = rig(h, false);
		f.getInventory().setStackInSlot(0, new ItemStack(Items.DIRT, 64));
		h.succeedWhen(() -> {
			h.assertTrue(f.getPower() == 1024 && f.getRequiredPower() == 1024 && f.getOperationTime() == 5,
					"filler "+f.getPower()+" W, needs "+f.getRequiredPower()+", time "+f.getOperationTime());
			h.assertTrue(count(h, Blocks.DIRT) == 27, count(h, Blocks.DIRT)+" dirt set");
			h.assertTrue(f.getInventory().getStackInSlot(0).getCount() == 37 && f.getRemainingArea() == 0,
					f.getInventory().getStackInSlot(0)+" left, area "+f.getRemainingArea());
			h.assertBlockPresent(Blocks.AIR, new BlockPos(4, 6, 4)); //never above or beside itself
		});
	}

	/** Required power by block: torch 512 W, dirt 1024 W, stone 2048 W, iron 4096 W. At 1024 W the filler sets no stone. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void fillerNeedsMorePowerForStone(GameTestHelper h) {
		basin(h);
		BlockFillerBlockEntity f = rig(h, false);
		Object[][] cases = {{Items.TORCH, 512L}, {Items.DIRT, 1024L}, {Items.OAK_PLANKS, 1024L}, {Items.STONE, 2048L}, {Items.IRON_BLOCK, 4096L}};
		for (Object[] c : cases) {
			f.getInventory().setStackInSlot(0, new ItemStack((net.minecraft.world.item.Item)c[0]));
			h.assertTrue(f.getRequiredPower() == (Long)c[1], c[0]+" needs "+f.getRequiredPower()+" W");
		}
		f.getInventory().setStackInSlot(0, new ItemStack(Items.STONE, 64));
		h.runAtTickTime(100, () -> {
			h.assertTrue(f.getPower() == 1024 && f.getRequiredPower() == 2048, "filler "+f.getPower()+" W, needs "+f.getRequiredPower());
			h.assertTrue(count(h, Blocks.STONE) == 0 && f.getInventory().getStackInSlot(0).getCount() == 64, count(h, Blocks.STONE)+" stone set");
			h.succeed();
		});
	}

	/**
	 * Unlike the spiller the filler replaces liquids, as far as they are connected to the block right below it (original
	 * recursiveAddLiquidWithBounds from y - 1): a hollow full of water ends up as 27 stone (gas engine, 65536 W).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void fillerOverwritesLiquids(GameTestHelper h) {
		basin(h);
		for (int x = 3; x <= 5; x++)
			for (int z = 3; z <= 5; z++)
				for (int y = 2; y <= 4; y++)
					h.setBlock(new BlockPos(x, y, z), Blocks.WATER);
		BlockFillerBlockEntity f = rig(h, true);
		f.getInventory().setStackInSlot(0, new ItemStack(Items.STONE, 64));
		h.succeedWhen(() -> {
			h.assertTrue(f.getPower() == 65536 && f.getOperationTime() == 3, "filler "+f.getPower()+" W, time "+f.getOperationTime());
			h.assertTrue(count(h, Blocks.STONE) == 27 && count(h, Blocks.WATER) == 0, count(h, Blocks.STONE)+" stone, "+count(h, Blocks.WATER)+" water");
			h.assertTrue(f.getInventory().getStackInSlot(0).getCount() == 37, f.getInventory().getStackInSlot(0)+" left");
		});
	}

	/** The slot takes only items that place a block; automation can insert from any side and never extract. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void fillerTakesOnlyBlocks(GameTestHelper h) {
		BlockFillerBlockEntity f = place(h, RotaryBlocks.FILLER.get(), 4, 2, 4, 0, BlockFillerBlockEntity.class); //on solid ground: nothing to fill
		IItemHandler side = f.getItemHandler(Direction.DOWN);
		h.assertTrue(side.insertItem(0, new ItemStack(Items.STICK, 4), false).getCount() == 4, "took sticks");
		h.assertTrue(side.insertItem(0, new ItemStack(Items.COBBLESTONE, 8), false).isEmpty(), "took no cobblestone");
		h.assertTrue(side.extractItem(0, 8, false).isEmpty(), "gave blocks back");
		h.runAtTickTime(40, () -> {
			h.assertTrue(f.getInventory().getStackInSlot(0).getCount() == 8 && f.getRemainingArea() == 0, f.getInventory().getStackInSlot(0)+" left");
			h.succeed();
		});
	}
}
