/*******************************************************************************
 * Tests for the ported RotaryCraft wetter (original TileEntityWetter). Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not
 * for distribution.
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.WetterBlockEntity;
import reika.rotarycraft.content.blockentity.engine.PerformanceEngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WetterTests {

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

	/** Performance engine (1024 rad/s @ 256 Nm, meta 1: output east) -> bevel gears west->up -> wetter above them. */
	private static WetterBlockEntity rig(GameTestHelper h) {
		PerformanceEngineBlockEntity e = place(h, RotaryBlocks.SPORT_ENGINE.get(), 3, 2, 4, 1, PerformanceEngineBlockEntity.class);
		e.addFuel(50000);
		e.addWater(50000);
		e.additives = 10000;
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		return place(h, RotaryBlocks.WETTER.get(), 4, 3, 4, 0, WetterBlockEntity.class);
	}

	private static IFluidHandler fluids(GameTestHelper h, Direction side) {
		return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(POS), side);
	}

	private static IItemHandler items(GameTestHelper h) {
		return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(POS), Direction.NORTH);
	}

	/** RecipesWetter: sand + 500 mB lubricant -> soul sand (200 ticks), cobblestone + 20 mB jet fuel -> netherrack (80 ticks). */
	@GameTest(template = PLATFORM)
	public static void wetterFollowsTheOriginalNumbers(GameTestHelper h) {
		ItemStack sand = new ItemStack(Items.SAND), cobble = new ItemStack(Items.COBBLESTONE);
		WetterBlockEntity.WettingRecipe r = WetterBlockEntity.getRecipe(sand, new FluidStack(RotaryFluids.LUBRICANT.get(), 500));
		h.assertTrue(r != null && r.output() == Items.SOUL_SAND && r.duration() == 200, "sand recipe "+r);
		h.assertTrue(WetterBlockEntity.getRecipe(sand, new FluidStack(RotaryFluids.LUBRICANT.get(), 499)) == null, "sand with 499 mB");
		h.assertTrue(WetterBlockEntity.getRecipe(sand, new FluidStack(RotaryFluids.JET_FUEL.get(), 1000)) == null, "sand with jet fuel");
		r = WetterBlockEntity.getRecipe(cobble, new FluidStack(RotaryFluids.JET_FUEL.get(), 20));
		h.assertTrue(r != null && r.output() == Items.NETHERRACK && r.duration() == 80, "cobblestone recipe "+r);
		h.assertTrue(WetterBlockEntity.getRecipe(new ItemStack(Items.DIRT), new FluidStack(RotaryFluids.JET_FUEL.get(), 1000)) == null, "dirt");
		WetterBlockEntity te = place(h, RotaryBlocks.WETTER.get(), 4, 3, 4, 0, WetterBlockEntity.class);
		int[][] cases = {{1024, 200}, {2048, 195}, {4096, 185}};
		for (int[] c : cases) {
			te.omega = c[0];
			h.assertTrue(te.getDuration(WetterBlockEntity.RECIPES[0]) == c[1], c[0]+" rad/s: "+te.getDuration(WetterBlockEntity.RECIPES[0])+" ticks");
		}
		h.succeed();
	}

	/**
	 * Powered from below at 1024 rad/s, sand in lubricant becomes soul sand after 200 ticks and takes 500 mB. While it works no liquid goes
	 * in and the item cannot be taken out; 10 ticks after, it can.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 800)
	public static void wetterSoaksSandIntoSoulSand(GameTestHelper h) {
		WetterBlockEntity te = rig(h);
		h.assertTrue(fluids(h, Direction.NORTH).fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "lubricant refused");
		h.assertTrue(items(h).insertItem(0, new ItemStack(Items.SAND, 2), false).getCount() == 1, "the slot took more than one sand");
		int[] start = {-1};
		boolean[] checked = {false};
		h.onEachTick(() -> {
			if (start[0] < 0 && te.getTick() > 0)
				start[0] = (int)h.getTick();
			if (te.getTick() > 50 && !checked[0]) {
				checked[0] = true;
				h.assertTrue(items(h).extractItem(0, 1, true).isEmpty(), "sand came out while it worked");
				h.assertTrue(fluids(h, Direction.NORTH).fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 100), IFluidHandler.FluidAction.SIMULATE) == 0, "lubricant went in while it worked");
			}
		});
		h.succeedWhen(() -> {
			h.assertTrue(te.getInventory().getStackInSlot(0).is(Items.SOUL_SAND), "no soul sand yet, timer "+te.getTick()+", "+te.omega+" rad/s, "+te.power+" W");
			h.assertTrue(te.getTank().getFluidAmount() == 500, te.getTank().getFluidAmount()+" mB left");
			h.assertTrue(checked[0] && start[0] >= 0 && h.getTick()-start[0] >= 199, "soul sand after "+(h.getTick()-start[0])+" ticks");
			h.assertTrue(items(h).extractItem(0, 1, true).is(Items.SOUL_SAND), "soul sand cannot be taken out (cooldown 10 ticks)");
		});
	}

	/** Liquids only from the four sides, only those of a recipe and only one fitting the item; without power nothing happens. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void wetterTakesOnlyFittingLiquids(GameTestHelper h) {
		WetterBlockEntity te = place(h, RotaryBlocks.WETTER.get(), 4, 3, 4, 0, WetterBlockEntity.class);
		h.assertTrue(fluids(h, Direction.UP).fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 0, "liquid from above");
		h.assertTrue(fluids(h, Direction.DOWN).fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 0, "liquid from below");
		h.assertTrue(fluids(h, Direction.EAST).fill(new FluidStack(RotaryFluids.ETHANOL.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 0, "ethanol went in");
		h.assertTrue(items(h).insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "dirt went in");
		h.assertTrue(items(h).insertItem(0, new ItemStack(Items.COBBLESTONE), false).isEmpty(), "cobblestone refused");
		h.assertTrue(fluids(h, Direction.WEST).fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 0, "lubricant went in with cobblestone");
		h.assertTrue(fluids(h, Direction.WEST).fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 100, "jet fuel refused");
		h.runAtTickTime(150, () -> {
			h.assertTrue(te.getInventory().getStackInSlot(0).is(Items.COBBLESTONE) && te.getTick() == 0 && te.getTank().getFluidAmount() == 100, "unpowered wetter worked");
			h.assertTrue(items(h).extractItem(0, 1, false).is(Items.COBBLESTONE), "cobblestone cannot be taken out");
			h.succeed();
		});
	}
}
