/*******************************************************************************
 * Tests for the ported RotaryCraft scaleable chest (original TileEntityScaleableChest). Private NeoForge 1.21.1 port of RotaryCraft by
 * Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.ScaleableChestBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryComponents;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ScaleableChestTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** 9 + (power - 4096)/128 slots, at most 972; pages of 54. */
	@GameTest(template = PLATFORM)
	public static void chestSizeFollowsTheOriginal(GameTestHelper h) {
		ScaleableChestBlockEntity te = place(h, RotaryBlocks.SCALEABLE_CHEST.get(), 4, 2, 4, 0, ScaleableChestBlockEntity.class);
		long[][] cases = {{0, 9, 0}, {4096, 9, 0}, {65536, 489, 9}, {1L << 30, 972, 18}};
		for (long[] c : cases) {
			te.power = c[0];
			h.assertTrue(te.getNumberSlots() == c[1] && te.getMaxPage() == c[2], c[0]+" W: "+te.getNumberSlots()+" slots, last page "+te.getMaxPage());
		}
		te.power = 65536;
		h.assertTrue(te.getSlotsOnPage(0) == 54 && te.getSlotsOnPage(9) == 3 && te.getSlotsOnPage(10) == 0, "slots per page");
		h.assertTrue(ScaleableChestBlockEntity.getMaxPages() == 18, "max pages");
		h.succeed();
	}

	/** Powered by a gas engine it has 489 slots; all 972 are reachable from outside; broken, the item keeps the contents. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void chestOpensWithPowerAndKeepsItsContents(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
		ScaleableChestBlockEntity te = place(h, RotaryBlocks.SCALEABLE_CHEST.get(), 4, 2, 4, 0, ScaleableChestBlockEntity.class);
		IItemHandler items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(new BlockPos(4, 2, 4)), Direction.UP);
		h.assertTrue(items.getSlots() == 972, items.getSlots()+" slots outside");
		h.assertTrue(items.insertItem(500, new ItemStack(Items.DIAMOND, 3), false).isEmpty(), "slot 500 refused");
		h.succeedWhen(() -> {
			h.assertTrue(te.getNumberSlots() == 489 && te.getNumPowerChanges() <= 3, te.getNumberSlots()+" slots");
			CompoundTag tag = te.collectComponents().get(RotaryComponents.CHEST_CONTENTS.get());
			h.assertTrue(tag != null && tag.toString().contains("diamond"), "item data "+tag);
		});
	}

	/** Power switching on and off a few times within a second makes it smoke and lock up; steady power again, it calms down. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void chestDislikesFlickeringPower(GameTestHelper h) {
		BlockPos eng = new BlockPos(5, 2, 4);
		ScaleableChestBlockEntity te = place(h, RotaryBlocks.SCALEABLE_CHEST.get(), 4, 2, 4, 0, ScaleableChestBlockEntity.class);
		int[] peak = {0};
		h.onEachTick(() -> {
			long t = h.getTick();
			if (t >= 10 && t <= 30 && t%4 == 0) {
				if (h.getBlockState(eng).isAir())
					place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
				else
					h.setBlock(eng, Blocks.AIR);
			}
			peak[0] = Math.max(peak[0], te.getNumPowerChanges());
		});
		h.runAtTickTime(150, () -> {
			h.assertBlockPresent(RotaryBlocks.SCALEABLE_CHEST.get(), new BlockPos(4, 2, 4));
			h.assertTrue(peak[0] > 3 && peak[0] <= 10, "power changes peaked at "+peak[0]);
			h.assertTrue(te.getNumPowerChanges() == 0, "still "+te.getNumPowerChanges()+" changes");
			h.succeed();
		});
	}
}
