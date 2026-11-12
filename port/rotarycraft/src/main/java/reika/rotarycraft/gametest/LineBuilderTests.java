/*******************************************************************************
 * Tests for the ported RotaryCraft line builder (original TileEntityLineBuilder). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.LineBuilderBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class LineBuilderTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Microturbine -> two bedrock gearboxes 16:1 (512 rad/s @ 4096 Nm = 2 MW) -> line builder at (4, 1, 4) (meta 0: power from the east, pushes west). */
	private static LineBuilderBlockEntity rig(GameTestHelper h) {
		EngineBlockEntity e = place(h, RotaryBlocks.MICRO_ENGINE.get(), 7, 1, 4, 0, EngineBlockEntity.class);
		IFluidHandler fh = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, e.getBlockPos(), Direction.DOWN);
		fh.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 20000), IFluidHandler.FluidAction.EXECUTE);
		for (int x = 5; x <= 6; x++) {
			GearboxBlockEntity g = place(h, RotaryBlocks.GEARBOX.get(), x, 1, 4, 12, GearboxBlockEntity.class);
			g.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16));
			g.setBlockMetadata(12);
			g.fillWithLubricant();
			g.reduction = true;
		}
		return place(h, RotaryBlocks.LINE_BUILDER.get(), 4, 1, 4, 0, LineBuilderBlockEntity.class);
	}

	private static Block at(GameTestHelper h, int x) {
		return h.getBlockState(new BlockPos(x, 1, 4)).getBlock();
	}

	/** DurationRegistry.RAM: max(3, 40 - 2*log2(omega+1)); 64 blocks at most. */
	@GameTest(template = PLATFORM)
	public static void lineBuilderFollowsTheOriginalNumbers(GameTestHelper h) {
		LineBuilderBlockEntity te = place(h, RotaryBlocks.LINE_BUILDER.get(), 4, 1, 4, 0, LineBuilderBlockEntity.class);
		int[][] cases = {{0, 40}, {512, 21}, {1048576, 3}};
		for (int[] c : cases) {
			te.omega = c[0];
			h.assertTrue(te.getOperationTime() == c[1], c[0]+" rad/s: "+te.getOperationTime()+" ticks");
		}
		h.assertTrue(LineBuilderBlockEntity.MAXRANGE == 64, "range");
		IItemHandler items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(new BlockPos(4, 1, 4)), Direction.UP);
		h.assertTrue(items.insertItem(0, new ItemStack(Items.STICK), false).isEmpty(), "stick refused");
		h.assertTrue(items.extractItem(0, 1, false).isEmpty(), "item came out");
		h.succeed();
	}

	/**
	 * Powered, it pushes the stone in front of it on by one each round and sets the next block from its inventory into the gap (the stick
	 * in the first slot is skipped); with no blocks left it stops.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void lineBuilderPushesTheLine(GameTestHelper h) {
		LineBuilderBlockEntity te = rig(h);
		h.assertTrue(te.getPushDirection() == Direction.WEST, "pushes "+te.getPushDirection());
		for (int x = 0; x <= 2; x++)
			h.setBlock(new BlockPos(x, 1, 4), Blocks.AIR);
		h.setBlock(new BlockPos(3, 1, 4), Blocks.STONE);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.STICK));
		te.getInventory().setStackInSlot(4, new ItemStack(Items.COBBLESTONE, 2));
		h.succeedWhen(() -> {
			h.assertTrue(at(h, 3) == Blocks.COBBLESTONE && at(h, 2) == Blocks.COBBLESTONE && at(h, 1) == Blocks.STONE && at(h, 0) == Blocks.AIR,
					"line "+at(h, 3)+" "+at(h, 2)+" "+at(h, 1)+" "+at(h, 0)+"; "+te.power+" W @ "+te.torque+" Nm");
			h.assertTrue(te.getInventory().getStackInSlot(4).isEmpty() && te.getInventory().getStackInSlot(0).is(Items.STICK), "inventory");
		});
	}

	/** It does not push bedrock, blocks with block entities or unbreakable blocks (the barrier around the test area). */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void lineBuilderStopsAtWhatItMayNotPush(GameTestHelper h) {
		LineBuilderBlockEntity te = rig(h);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 16));
		h.setBlock(new BlockPos(3, 1, 4), Blocks.STONE);
		h.setBlock(new BlockPos(2, 1, 4), Blocks.CHEST);
		LineBuilderBlockEntity off = place(h, RotaryBlocks.LINE_BUILDER.get(), 4, 1, 1, 0, LineBuilderBlockEntity.class);
		off.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 16));
		LineBuilderBlockEntity full = place(h, RotaryBlocks.LINE_BUILDER.get(), 4, 1, 7, 0, LineBuilderBlockEntity.class);
		for (int x = 0; x <= 3; x++) //up to the edge of the test area, where the barrier around it stops the line
			h.setBlock(new BlockPos(x, 1, 7), Blocks.STONE);
		h.setBlock(new BlockPos(3, 1, 1), Blocks.BEDROCK);
		h.runAtTickTime(250, () -> {
			h.assertTrue(te.power >= te.MINPOWER, "rig at "+te.power+" W");
			h.assertTrue(at(h, 3) == Blocks.STONE && at(h, 2) == Blocks.CHEST && te.getInventory().getStackInSlot(0).getCount() == 16, "pushed a chest");
			h.assertTrue(te.getLineLength() == Integer.MIN_VALUE && !te.canShift(h.getLevel()), "line length "+te.getLineLength());
			h.assertTrue(off.getLineLength() == Integer.MIN_VALUE && off.getInventory().getStackInSlot(0).getCount() == 16, "pushed bedrock");
			h.assertTrue(full.getLineLength() == Integer.MIN_VALUE && !full.canShift(h.getLevel()), "line against the barrier: "+full.getLineLength());
			h.succeed();
		});
	}
}
