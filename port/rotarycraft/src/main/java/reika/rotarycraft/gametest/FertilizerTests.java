/*******************************************************************************
 * Tests for the ported RotaryCraft fertilizer (original TileEntityFertilizer). Private NeoForge 1.21.1 port of RotaryCraft by Reika
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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.FertilizerBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FertilizerTests {

	private static final String PLATFORM = "platform_9x8x9";
	private static final BlockPos POS = new BlockPos(4, 3, 4);

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** DC engine (256 rad/s @ 4 Nm = 1024 W, with a redstone block) -> bevel gears west->up -> fertilizer above them: range 4, 32 blocks a tick. */
	private static FertilizerBlockEntity rig(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class);
		h.setBlock(new BlockPos(3, 2, 3), Blocks.REDSTONE_BLOCK);
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		return place(h, RotaryBlocks.FERTILIZER.get(), 4, 3, 4, 0, FertilizerBlockEntity.class);
	}

	/** Range 2*log2(torque) (at most 32), 4*log2(omega) blocks a tick from 1024 W, more ticks each from 2^20 rad/s; fertilizers. */
	@GameTest(template = PLATFORM)
	public static void fertilizerFollowsTheOriginalNumbers(GameTestHelper h) {
		FertilizerBlockEntity te = place(h, RotaryBlocks.FERTILIZER.get(), 4, 3, 4, 0, FertilizerBlockEntity.class);
		te.torque = 128;
		te.omega = 512;
		te.power = 65536;
		h.assertTrue(te.getRange() == 14 && te.getUpdatesPerTick() == 36 && te.getConsecutiveUpdates() == 1, "gas engine numbers "+te.getRange()+" "+te.getUpdatesPerTick());
		te.torque = 1 << 20;
		te.omega = 1 << 22;
		h.assertTrue(te.getRange() == 32 && te.getConsecutiveUpdates() == 3, "big numbers "+te.getRange()+" "+te.getConsecutiveUpdates());
		te.power = 1023;
		h.assertTrue(te.getUpdatesPerTick() == 0, "works below 1024 W");
		h.assertTrue(FertilizerBlockEntity.isValidFertilizer(new ItemStack(Items.BONE_MEAL)) && FertilizerBlockEntity.isValidFertilizer(new ItemStack(RotaryItems.part(CraftingPart.COMPOST)))
				&& !FertilizerBlockEntity.isValidFertilizer(new ItemStack(Items.WHEAT_SEEDS)), "fertilizers");
		h.assertTrue(te.getRedstoneOverride() == 15, "redstone without fertilizer");
		h.succeed();
	}

	/** With water and bone meal, sugar cane beside it grows fast (it grows on every random tick) and some water is used. */
	@GameTest(template = PLATFORM, timeoutTicks = 1500)
	public static void fertilizerMakesPlantsGrow(GameTestHelper h) {
		FertilizerBlockEntity te = rig(h);
		te.getTank().fill(new FluidStack(Fluids.WATER, 6000), IFluidHandler.FluidAction.EXECUTE);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.BONE_MEAL, 64));
		h.setBlock(new BlockPos(5, 2, 5), Blocks.DIRT);
		h.setBlock(new BlockPos(6, 2, 5), Blocks.WATER);
		for (BlockPos g : new BlockPos[] {new BlockPos(7, 2, 5), new BlockPos(6, 2, 4), new BlockPos(6, 2, 6)})
			h.setBlock(g, Blocks.GLASS);
		h.setBlock(new BlockPos(5, 3, 5), Blocks.SUGAR_CANE);
		h.succeedWhen(() -> {
			h.assertTrue(te.power == 1024 && te.getRange() == 4, "fertilizer at "+te.power+" W, range "+te.getRange());
			h.assertBlockPresent(Blocks.SUGAR_CANE, new BlockPos(5, 4, 5));
			h.assertTrue(te.getTank().getFluidAmount() < 6000, "no water used");
		});
	}

	/** Water only, and not from below; fertilizer only; nothing comes out. */
	@GameTest(template = PLATFORM)
	public static void fertilizerTakesWaterAndFertilizer(GameTestHelper h) {
		place(h, RotaryBlocks.FERTILIZER.get(), 4, 3, 4, 0, FertilizerBlockEntity.class);
		IFluidHandler down = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(POS), Direction.DOWN);
		IFluidHandler side = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(POS), Direction.EAST);
		h.assertTrue(down.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "water from below");
		h.assertTrue(side.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.EXECUTE) == 0, "lava went in");
		h.assertTrue(side.fill(new FluidStack(Fluids.WATER, 9000), IFluidHandler.FluidAction.EXECUTE) == 6000, "not 6000 mB");
		IItemHandler items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(POS), Direction.UP);
		h.assertTrue(items.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "dirt went in");
		h.assertTrue(items.insertItem(0, new ItemStack(Items.BONE_MEAL, 5), false).isEmpty(), "bone meal refused");
		h.assertTrue(items.extractItem(0, 1, false).isEmpty(), "bone meal came out");
		h.succeed();
	}
}
