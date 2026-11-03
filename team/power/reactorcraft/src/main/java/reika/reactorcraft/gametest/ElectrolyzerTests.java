/*******************************************************************************
 * Tests for the ported ReactorCraft electrolyzer driven by the RotaryCraft Van de Graaff generator (original TileEntityElectrolyzer).
 * Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.blockentity.ElectrolyzerBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.blockentity.VanDeGraffBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ElectrolyzerTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static ElectrolyzerBlockEntity setup(GameTestHelper h, VanDeGraffBlockEntity[] vdg) {
		h.setBlock(new BlockPos(4, 1, 4), ReactorBlocks.ELECTROLYZER.get());
		h.setBlock(new BlockPos(3, 1, 4), RotaryBlocks.VAN_DE_GRAFF.get());
		vdg[0] = (VanDeGraffBlockEntity)h.getBlockEntity(new BlockPos(3, 1, 4));
		return (ElectrolyzerBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
	}

	/**
	 * Heavy water (filled from the side) next to a Van de Graaff generator held at 20000 C: it sparks into the electrolyzer (weight 1000),
	 * each discharge moves the timer by sqrt(15904)/16 = 7, and at 50 it makes 100 mB deuterium (top) and 50 mB oxygen (bottom) from 100 mB
	 * heavy water. Without discharges nothing happens.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void heavyWaterSplitsOnDischarges(GameTestHelper h) {
		VanDeGraffBlockEntity[] vdg = new VanDeGraffBlockEntity[1];
		ElectrolyzerBlockEntity te = setup(h, vdg);
		h.assertTrue(te.fill(Direction.UP, new FluidStack(ReactorFluids.get("heavy_water"), 1000), IFluidHandler.FluidAction.SIMULATE) == 0, "filled from the top");
		h.assertTrue(te.fill(Direction.NORTH, new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE) == 0, "took plain water");
		te.fill(Direction.NORTH, new FluidStack(ReactorFluids.get("heavy_water"), 1000), IFluidHandler.FluidAction.EXECUTE);
		boolean[] on = {false};
		h.onEachTick(() -> {
			if (on[0])
				vdg[0].setCharge(20000);
		});
		h.runAtTickTime(20, () -> {
			h.assertTrue(te.getInputTank().getFluidAmount() == 1000 && te.getTimer() == 0, "worked without discharges: input "+te.getInputTank().getFluidAmount()+", timer "+te.getTimer()+", top "+te.getLightTank().getFluidAmount());
			on[0] = true;
		});
		h.succeedWhen(() -> {
			h.assertTrue(te.getLightTank().getFluidAmount() >= 100 && te.getLightTank().getFluid().getFluid().isSame(ReactorFluids.get("deuterium")),
					"top tank "+te.getLightTank().getFluidAmount()+", timer "+te.getTimer());
			int n = te.getLightTank().getFluidAmount()/100;
			h.assertTrue(te.getHeavyTank().getFluidAmount() == 50*n && te.getHeavyTank().getFluid().getFluid().isSame(ReactorFluids.get("oxygen")), "bottom tank "+te.getHeavyTank().getFluidAmount());
			h.assertTrue(te.getInputTank().getFluidAmount() == 1000-100*n, "input "+te.getInputTank().getFluidAmount());
			h.assertTrue(te.drain(Direction.UP, 100, IFluidHandler.FluidAction.SIMULATE).getFluid().isSame(ReactorFluids.get("deuterium"))
					&& te.drain(Direction.DOWN, 50, IFluidHandler.FluidAction.SIMULATE).getFluid().isSame(ReactorFluids.get("oxygen")), "drain sides");
		});
	}

	/** Salt only splits from 801 C: cold nothing; held at 900 C one salt gives 100 mB chlorine (top) and 100 mB sodium (bottom). */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void saltNeedsHeat(GameTestHelper h) {
		VanDeGraffBlockEntity[] vdg = new VanDeGraffBlockEntity[1];
		ElectrolyzerBlockEntity te = setup(h, vdg);
		te.getInventory().setStackInSlot(0, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
				net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("rotarycraft", "salt")), 4));
		boolean[] hot = {false};
		h.onEachTick(() -> {
			vdg[0].setCharge(20000);
			if (hot[0])
				te.setTemperature(900);
		});
		h.runAtTickTime(80, () -> {
			h.assertTrue(te.getLightTank().isEmpty() && te.getInventory().getStackInSlot(0).getCount() == 4, "cold salt split");
			hot[0] = true;
		});
		h.succeedWhen(() -> {
			h.assertTrue(te.getLightTank().getFluidAmount() >= 100 && te.getLightTank().getFluid().getFluid().isSame(ReactorFluids.get("chlorine"))
					&& te.getHeavyTank().getFluid().getFluid().isSame(ReactorFluids.get("sodium")), "tanks "+te.getLightTank().getFluidAmount()+"/"+te.getHeavyTank().getFluidAmount());
			h.assertTrue(te.getInventory().getStackInSlot(0).getCount() == 4-te.getLightTank().getFluidAmount()/100, "salt left "+te.getInventory().getStackInSlot(0).getCount());
		});
	}
}
