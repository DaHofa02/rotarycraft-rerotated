/*******************************************************************************
 * Tests for the ported RotaryCraft lava smeltery (original TileEntityBigFurnace). Private NeoForge 1.21.1 port of RotaryCraft by Reika
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
import reika.rotarycraft.content.blockentity.BigFurnaceBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BigFurnaceTests {

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

	/** Gas engine (65536 W) -> bevel gears west->up -> lava smeltery above them. */
	private static BigFurnaceBlockEntity rig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		return place(h, RotaryBlocks.BIG_FURNACE.get(), 4, 3, 4, 0, BigFurnaceBlockEntity.class);
	}

	/** 200 ticks per round below 600 deg C, 150 from 600, 75 from 1000; nothing below 400 deg C or without power. */
	@GameTest(template = PLATFORM)
	public static void bigFurnaceFollowsTheOriginalNumbers(GameTestHelper h) {
		BigFurnaceBlockEntity te = place(h, RotaryBlocks.BIG_FURNACE.get(), 4, 3, 4, 0, BigFurnaceBlockEntity.class);
		int[][] cases = {{400, 200}, {599, 200}, {600, 150}, {999, 150}, {1000, 75}};
		for (int[] c : cases) {
			te.setTemperature(c[0]);
			h.assertTrue(te.getOperationTime() == c[1], c[0]+" deg C: "+te.getOperationTime()+" ticks");
		}
		te.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
		te.setTemperature(500);
		h.assertFalse(te.canSmelt(), "smelts without power");
		h.assertTrue(te.getMaxTemperature() == 1200 && !te.allowExternalHeating() && !te.canBeCooledWithFins(), "heat rules");
		h.succeed();
	}

	/** At 500 deg C with power from below every input slot gives one item each 200 ticks: cobblestone -> stone, sand -> glass. */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void bigFurnaceSmeltsAllSlotsAtOnce(GameTestHelper h) {
		BigFurnaceBlockEntity te = rig(h);
		te.addLava(16000);
		te.setTemperature(500);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 3));
		te.getInventory().setStackInSlot(17, new ItemStack(Items.SAND, 3));
		te.getInventory().setStackInSlot(5, new ItemStack(Items.DIRT, 3));
		int[] start = {-1};
		h.onEachTick(() -> {
			if (start[0] < 0 && te.smeltTick > 0)
				start[0] = (int)h.getTick();
		});
		h.succeedWhen(() -> {
			ItemStack stone = te.getInventory().getStackInSlot(18), glass = te.getInventory().getStackInSlot(35);
			h.assertTrue(stone.is(Items.STONE) && glass.is(Items.GLASS), "outputs "+stone+", "+glass+"; progress "+te.smeltTick+", "+te.power+" W, "+te.getTemperature()+" C");
			h.assertTrue(stone.getCount() == 1 && glass.getCount() == 1 && te.getInventory().getStackInSlot(0).getCount() == 2, "not one each");
			h.assertTrue(te.getInventory().getStackInSlot(5).getCount() == 3 && te.getInventory().getStackInSlot(23).isEmpty(), "dirt smelted");
			h.assertTrue(h.getTick()-start[0] >= 199, "first round after "+(h.getTick()-start[0])+" ticks");
		});
	}

	/**
	 * With lava the temperature climbs towards ambient + 600 (2 deg C a second from cold) and the tank loses 15 mB a second; above 100
	 * deg C a snow block beside it melts away and ice beside it becomes water.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void bigFurnaceIsHeatedByLava(GameTestHelper h) {
		BigFurnaceBlockEntity hot = place(h, RotaryBlocks.BIG_FURNACE.get(), 2, 2, 2, 0, BigFurnaceBlockEntity.class);
		hot.addLava(1000);
		BigFurnaceBlockEntity melt = place(h, RotaryBlocks.BIG_FURNACE.get(), 6, 2, 6, 0, BigFurnaceBlockEntity.class);
		melt.setTemperature(150);
		melt.addLava(1000);
		h.setBlock(new BlockPos(7, 2, 6), Blocks.SNOW_BLOCK);
		h.setBlock(new BlockPos(6, 2, 7), Blocks.ICE);
		h.runAtTickTime(205, () -> {
			h.assertTrue(hot.getTemperature() >= 18 && hot.getTemperature() <= 20, "after 10 s at "+hot.getTemperature()+" deg C");
			h.assertTrue(hot.getTank().getFluidAmount() >= 1000-11*15 && hot.getTank().getFluidAmount() <= 1000-9*15, hot.getTank().getFluidAmount()+" mB left");
			h.assertBlockNotPresent(Blocks.SNOW_BLOCK, new BlockPos(7, 2, 6)); //air, or the water from the melted ice that ran into it
			h.assertBlockPresent(Blocks.WATER, new BlockPos(6, 2, 7));
			h.succeed();
		});
	}

	/** Lava only from the four sides; items into the inputs only; only the outputs can be emptied. */
	@GameTest(template = PLATFORM)
	public static void bigFurnaceTakesLavaAndItems(GameTestHelper h) {
		BigFurnaceBlockEntity te = place(h, RotaryBlocks.BIG_FURNACE.get(), 4, 3, 4, 0, BigFurnaceBlockEntity.class);
		IFluidHandler up = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(POS), Direction.UP);
		IFluidHandler side = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(POS), Direction.EAST);
		h.assertTrue(up.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "lava from above");
		h.assertTrue(side.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "water went in");
		h.assertTrue(side.fill(new FluidStack(Fluids.LAVA, 20000), IFluidHandler.FluidAction.EXECUTE) == 16000, "not 16000 mB lava");
		IItemHandler items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(POS), Direction.UP);
		h.assertTrue(items.getSlots() == 36, items.getSlots()+" slots");
		h.assertTrue(items.insertItem(18, new ItemStack(Items.SAND), false).getCount() == 1, "sand went into an output");
		h.assertTrue(items.insertItem(3, new ItemStack(Items.SAND), false).isEmpty(), "sand refused");
		h.assertTrue(items.extractItem(3, 1, false).isEmpty(), "input came out");
		te.getInventory().setStackInSlot(21, new ItemStack(Items.GLASS, 2));
		h.assertTrue(items.extractItem(21, 2, false).getCount() == 2, "output did not come out");
		h.succeed();
	}
}
