/*******************************************************************************
 * Tests for the ported RotaryCraft bucket filler (original TileEntityBucketFiller). Private NeoForge 1.21.1 port of RotaryCraft by Reika
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BucketFillerBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BucketFillerTests {

	private static final String PLATFORM = "platform_9x8x9";
	private static final BlockPos POS = new BlockPos(4, 2, 4);

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine (512 rad/s @ 128 Nm) east of a bucket filler: it acts every 20 ticks (max(1, 200 - 20*log2(513)) = 19, plus 1). */
	private static BucketFillerBlockEntity rig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
		return place(h, RotaryBlocks.BUCKET_FILLER.get(), 4, 2, 4, 0, BucketFillerBlockEntity.class);
	}

	private static int count(ItemStackHandler inv, net.minecraft.world.item.Item item) {
		int n = 0;
		for (int i = 0; i < inv.getSlots(); i++)
			if (inv.getStackInSlot(i).is(item))
				n += inv.getStackInSlot(i).getCount();
		return n;
	}

	/** Filling: 3000 mB of water fill three of four empty buckets in one go. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void fillerFillsBuckets(GameTestHelper h) {
		BucketFillerBlockEntity te = rig(h);
		te.getTank().fill(new FluidStack(Fluids.WATER, 3000), IFluidHandler.FluidAction.EXECUTE);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.BUCKET, 4));
		h.succeedWhen(() -> {
			h.assertTrue(te.getOperationTime() == 19, "operation time "+te.getOperationTime());
			h.assertTrue(count(te.getInventory(), Items.WATER_BUCKET) == 3 && count(te.getInventory(), Items.BUCKET) == 1 && te.getTank().isEmpty(),
					count(te.getInventory(), Items.WATER_BUCKET)+" water buckets, "+te.getTank().getFluidAmount()+" mB left");
		});
	}

	/** Switched with the screwdriver it empties one container per round into the tank. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void fillerEmptiesBuckets(GameTestHelper h) {
		BucketFillerBlockEntity te = rig(h);
		te.onRightClick(h.getLevel(), te.getBlockPos(), Direction.UP);
		h.assertFalse(te.filling, "still filling");
		te.getInventory().setStackInSlot(3, new ItemStack(Items.LAVA_BUCKET));
		te.getInventory().setStackInSlot(7, new ItemStack(Items.LAVA_BUCKET));
		h.runAtTickTime(25, () -> h.assertTrue(te.getTank().getFluidAmount() <= 1000, "more than one per round: "+te.getTank().getFluidAmount()));
		h.succeedWhen(() -> {
			h.assertTrue(te.getTank().getFluid().is(Fluids.LAVA) && te.getTank().getFluidAmount() == 2000, te.getTank().getFluidAmount()+" mB");
			h.assertTrue(count(te.getInventory(), Items.BUCKET) == 2 && count(te.getInventory(), Items.LAVA_BUCKET) == 0, "buckets");
		});
	}

	/** Liquid in from the sides while filling, out from the sides while emptying; containers out at the bottom only when done. */
	@GameTest(template = PLATFORM)
	public static void fillerSidesFollowTheOriginal(GameTestHelper h) {
		BucketFillerBlockEntity te = place(h, RotaryBlocks.BUCKET_FILLER.get(), 4, 2, 4, 0, BucketFillerBlockEntity.class);
		IFluidHandler up = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(POS), Direction.UP);
		IFluidHandler side = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(POS), Direction.EAST);
		h.assertTrue(up.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "water from above");
		h.assertTrue(side.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "water from the side refused");
		h.assertTrue(side.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "water out while filling");
		IItemHandler down = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(POS), Direction.DOWN);
		IItemHandler east = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(POS), Direction.EAST);
		h.assertTrue(east.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "dirt went in");
		h.assertTrue(east.insertItem(0, new ItemStack(Items.BUCKET), false).isEmpty(), "bucket refused");
		te.getInventory().setStackInSlot(1, new ItemStack(Items.WATER_BUCKET));
		h.assertTrue(down.extractItem(0, 1, false).isEmpty() && east.extractItem(1, 1, false).isEmpty(), "empty bucket or side extraction");
		h.assertTrue(down.extractItem(1, 1, false).is(Items.WATER_BUCKET), "filled bucket did not come out");
		te.onRightClick(h.getLevel(), te.getBlockPos(), Direction.UP);
		h.assertTrue(side.drain(100, IFluidHandler.FluidAction.EXECUTE).getAmount() == 100, "water not out while emptying");
		h.assertTrue(side.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "water in while emptying");
		h.succeed();
	}
}
