/*******************************************************************************
 * Tests for the port's drop processor. Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.DropProcessorBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DropProcessorTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** max(1, 300 - 20*log2(omega+1)) ticks; several per tick above 65536 rad/s; only block items go in, only the output comes out. */
	@GameTest(template = PLATFORM)
	public static void dropProcessorFollowsTheOriginalNumbers(GameTestHelper h) {
		DropProcessorBlockEntity te = place(h, RotaryBlocks.DROP_PROCESSOR.get(), 4, 2, 4, 1, DropProcessorBlockEntity.class);
		int[][] cases = {{0, 300}, {511, 120}, {65535, 1}};
		for (int[] c : cases) {
			te.omega = c[0];
			h.assertTrue(te.getOperationTime() == c[1], c[0]+" rad/s: "+te.getOperationTime());
		}
		te.omega = 1 << 20;
		h.assertTrue(te.getNumberConsecutiveOperations() == 6, "operations at 2^20: "+te.getNumberConsecutiveOperations());
		h.assertTrue(te.getItemHandler(Direction.UP).insertItem(0, new ItemStack(Items.DIAMOND), true).getCount() == 1, "diamond accepted");
		h.assertTrue(te.getItemHandler(Direction.UP).insertItem(0, new ItemStack(Items.COAL_ORE), true).isEmpty(), "coal ore refused");
		h.succeed();
	}

	/**
	 * Gas engine (128 Nm, 512 rad/s: 120 ticks a block) into the drop processor (meta 1: input from the west) with 2 coal ore and a
	 * glowstone block: coal ore gives coal, glowstone 2-4 glowstone dust (the rest via the overflow).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void dropProcessorBreaksBlocks(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		DropProcessorBlockEntity te = place(h, RotaryBlocks.DROP_PROCESSOR.get(), 4, 2, 4, 1, DropProcessorBlockEntity.class);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.COAL_ORE, 2));
		int[] coal = {0}, dust = {0};
		h.onEachTick(() -> {
			ItemStack out = te.getInventory().getStackInSlot(1);
			if (out.is(Items.COAL))
				coal[0] += te.getInventory().extractItem(1, 64, false).getCount();
			else if (out.is(Items.GLOWSTONE_DUST))
				dust[0] += te.getInventory().extractItem(1, 64, false).getCount();
			if (coal[0] == 2 && te.getInventory().getStackInSlot(0).isEmpty() && te.processed == 2)
				te.getInventory().setStackInSlot(0, new ItemStack(Items.GLOWSTONE));
		});
		h.succeedWhen(() -> {
			h.assertTrue(te.getPower() == 65536, "processor at "+te.getPower()+" W");
			h.assertTrue(coal[0] == 2 && te.processed == 3 && dust[0] >= 2 && dust[0] <= 4 && te.getOverflowCount() == 0,
					"coal "+coal[0]+", dust "+dust[0]+", processed "+te.processed);
		});
	}
}
