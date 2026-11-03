/*******************************************************************************
 * Tests for the ported ReactorCraft machines, driven by real RotaryCraft power trains.
 * Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.blockentity.HeavyPumpBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ReactorMachineTests {

	private static final String PLATFORM = "platform_9x4x9";

	/**
	 * Gas engine (512 rad/s @ 128 Nm) -> steel 4:1 gearbox (128 rad/s @ 512 Nm = 65536 W, exactly the extractor's minimum) -> bevel gears
	 * turning the power down into the extractor below them at (4, y, 4). Returns the extractor.
	 */
	private static HeavyPumpBlockEntity rig(GameTestHelper h, int y) {
		h.setBlock(new BlockPos(6, y+1, 4), RotaryBlocks.GAS_ENGINE.get());
		EngineBlockEntity eng = (EngineBlockEntity)h.getBlockEntity(new BlockPos(6, y+1, 4));
		eng.setBlockMetadata(0);
		eng.addFuel(50000);
		h.setBlock(new BlockPos(5, y+1, 4), RotaryBlocks.GEARBOX.get());
		GearboxBlockEntity gbx = (GearboxBlockEntity)h.getBlockEntity(new BlockPos(5, y+1, 4));
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.STEEL, 4));
		gbx.setBlockMetadata(4);
		gbx.fillWithLubricant();
		gbx.reduction = true;
		h.setBlock(new BlockPos(4, y+1, 4), RotaryBlocks.BEVEL_GEARS.get());
		BevelGearBlockEntity bev = (BevelGearBlockEntity)h.getBlockEntity(new BlockPos(4, y+1, 4));
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.DOWN));
		h.assertTrue(bev.direction >= 0, "no east->down bevel route");
		h.setBlock(new BlockPos(4, y, 4), ReactorBlocks.FLUID_EXTRACTOR.get());
		return (HeavyPumpBlockEntity)h.getBlockEntity(new BlockPos(4, y, 4));
	}

	/** On the overworld lava surface (y -54; the test platform floor is y -60) with lava on all sides it collects molten lithium. */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void extractorMakesLithiumOnLava(GameTestHelper h) {
		int y = -54-h.absolutePos(BlockPos.ZERO).getY();
		HeavyPumpBlockEntity pump = rig(h, y);
		h.setBlock(new BlockPos(4, y-1, 4), Blocks.LAVA);
		for (Direction d : Direction.Plane.HORIZONTAL)
			if (d != Direction.EAST)
				h.setBlock(new BlockPos(4, y, 4).relative(d), Blocks.LAVA);
		h.setBlock(new BlockPos(5, y, 4), Blocks.LAVA);
		h.succeedWhen(() -> {
			h.assertTrue(pump.getOmega() == 128 && pump.getTorque() == 512, "extractor gets "+pump.getOmega()+" rad/s @ "+pump.getTorque()+" Nm");
			h.assertTrue(pump.getTankLevel() > 0 && pump.getTank().getFluid().getFluid().isSame(ReactorFluids.get("lithium")), "tank "+pump.getTankLevel());
		});
	}

	/** Heavy water needs an ocean biome (the test world is plains): deep water around it gives nothing here, while powered. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void extractorNoHeavyWaterOutsideOcean(GameTestHelper h) {
		HeavyPumpBlockEntity pump = rig(h, 1);
		for (Direction d : Direction.Plane.HORIZONTAL)
			if (d != Direction.EAST)
				h.setBlock(new BlockPos(4, 1, 4).relative(d), Blocks.WATER);
		h.runAtTickTime(180, () -> {
			h.assertTrue(pump.getTorque() == 512, "extractor not powered: "+pump.getTorque());
			h.assertTrue(pump.getTankLevel() == 0, "heavy water outside an ocean: "+pump.getTankLevel());
			h.succeed();
		});
	}

	/**
	 * Uranium processor, UF6: a water bucket in slot 1 fills the input tank; every 80 ticks a fluorite crystal and 250 mB water make 250 mB
	 * hydrofluoric acid; 400 ticks after the first acid an uranium ingot and 125 mB acid make 1000 mB uranium hexafluoride, which leaves
	 * through the front (facing west). The steps only run while the input tank holds water (original getProcess).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 700)
	public static void processorMakesUF6(GameTestHelper h) {
		BlockPos p = new BlockPos(4, 1, 4);
		h.setBlock(p, ReactorBlocks.PROCESSOR.get());
		var proc = (reika.reactorcraft.content.blockentity.UProcessorBlockEntity)h.getBlockEntity(p);
		proc.setBlockMetadata(0);
		var inv = proc.getInventory();
		inv.setStackInSlot(0, new net.minecraft.world.item.ItemStack(reika.reactorcraft.registry.ReactorItems.fluorite(reika.reactorcraft.registry.FluoriteTypes.GREEN).get(), 4));
		inv.setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
		inv.setStackInSlot(2, new net.minecraft.world.item.ItemStack(reika.reactorcraft.registry.ReactorItems.ingot(reika.reactorcraft.registry.ReactorItems.Ingot.URANIUM).get(), 2));
		h.runAtTickTime(10, () -> {
			h.assertTrue(proc.getInputTank().getFluidAmount() == 1000 && inv.getStackInSlot(1).is(net.minecraft.world.item.Items.BUCKET), "bucket not emptied: "+proc.getInputTank().getFluidAmount());
			//as in the original, the process only runs while the input tank holds its fluid: top it up through a side
			var side = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, h.absolutePos(p), Direction.NORTH);
			h.assertTrue(side.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 2000),
					net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 2000, "side does not take water");
		});
		h.runAtTickTime(95, () -> {
			h.assertTrue(proc.getIntermediateTank().getFluidAmount() == 250 && proc.getInputTank().getFluidAmount() == 2750 && inv.getStackInSlot(0).getCount() == 3,
					"after the first step: HF "+proc.getIntermediateTank().getFluidAmount()+", water "+proc.getInputTank().getFluidAmount());
			h.assertTrue(proc.getOutputTank().isEmpty(), "UF6 too early");
		});
		h.runAtTickTime(600, () -> {
			h.assertTrue(proc.getOutputTank().getFluidAmount() == 1000 && proc.getOutputTank().getFluid().getFluid().isSame(ReactorFluids.get("uranium_hexafluoride")),
					"UF6 "+proc.getOutputTank().getFluidAmount());
			h.assertTrue(inv.getStackInSlot(2).getCount() == 1, "ingots left "+inv.getStackInSlot(2).getCount());
			var front = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, h.absolutePos(p), Direction.WEST);
			h.assertTrue(front != null && front.drain(100, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE).getAmount() == 100, "front does not give UF6");
			h.assertTrue(proc.getFlowForSide(Direction.NORTH) == reika.rotarycraft.content.piping.PipeConnector.Flow.INPUT
					&& proc.getFlowForSide(Direction.WEST) == reika.rotarycraft.content.piping.PipeConnector.Flow.OUTPUT, "pipe sides");
			h.succeed();
		});
	}

	/**
	 * Synthesizer, ammonia: 250 mB water + a quicklime + an ammonium chloride -> 1000 mB ammonia every 50 ticks, but only at 220 C or more.
	 * A water bucket used on the block adds 1000 mB.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void synthesizerMakesAmmonia(GameTestHelper h) {
		BlockPos p = new BlockPos(4, 1, 4);
		h.setBlock(p, ReactorBlocks.SYNTHESIZER.get());
		var syn = (reika.reactorcraft.content.blockentity.SynthesizerBlockEntity)h.getBlockEntity(p);
		int[] temp = {200};
		h.onEachTick(() -> syn.setTemperature(temp[0]));
		var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
		h.useBlock(p, player);
		h.assertTrue(syn.getInputTank().getFluidAmount() == 1000 && player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET),
				"bucket on the block: "+syn.getInputTank().getFluidAmount()+", hand "+player.getMainHandItem());
		var inv = syn.getInventory();
		inv.setStackInSlot(1, new net.minecraft.world.item.ItemStack(reika.reactorcraft.registry.ReactorItems.raw(reika.reactorcraft.registry.ReactorItems.Raw.LIME).get(), 2));
		inv.setStackInSlot(2, new net.minecraft.world.item.ItemStack(reika.reactorcraft.registry.ReactorItems.raw(reika.reactorcraft.registry.ReactorItems.Raw.AMMONIUM).get(), 2));
		h.runAtTickTime(100, () -> {
			h.assertTrue(syn.getOutputTank().isEmpty(), "ammonia at 200 C");
			temp[0] = 300;
		});
		h.runAtTickTime(220, () -> {
			h.assertTrue(syn.getOutputTank().getFluidAmount() == 2000 && syn.getOutputTank().getFluid().getFluid().isSame(ReactorFluids.get("ammonia")),
					"ammonia "+syn.getOutputTank().getFluidAmount());
			h.assertTrue(syn.getInputTank().getFluidAmount() == 500 && inv.getStackInSlot(1).isEmpty() && inv.getStackInSlot(2).isEmpty(),
					"water "+syn.getInputTank().getFluidAmount()+", items "+inv.getStackInSlot(1)+" "+inv.getStackInSlot(2));
			h.succeed();
		});
	}

	/**
	 * Isotope centrifuge: microturbine (131072 rad/s @ 16 Nm, jet fuel) -> bedrock gearbox 2:1 speed-up (262144 rad/s @ 8 Nm, the
	 * minimum) -> bevel gears up into the centrifuge. Every 900 ticks 50 mB UF6 become one dust (9 % enriched, else depleted). UF6 is only
	 * accepted from the top.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 4000)
	public static void isoCentrifugeMakesDust(GameTestHelper h) {
		h.setBlock(new BlockPos(6, 1, 4), RotaryBlocks.MICRO_ENGINE.get());
		EngineBlockEntity eng = (EngineBlockEntity)h.getBlockEntity(new BlockPos(6, 1, 4));
		eng.setBlockMetadata(0);
		eng.addFuel(100000);
		h.setBlock(new BlockPos(5, 1, 4), RotaryBlocks.GEARBOX.get());
		GearboxBlockEntity gbx = (GearboxBlockEntity)h.getBlockEntity(new BlockPos(5, 1, 4));
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 2));
		gbx.setBlockMetadata(0);
		gbx.reduction = false;
		h.setBlock(new BlockPos(4, 1, 4), RotaryBlocks.BEVEL_GEARS.get());
		BevelGearBlockEntity bev = (BevelGearBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		BlockPos p = new BlockPos(4, 2, 4);
		h.setBlock(p, ReactorBlocks.ISO_CENTRIFUGE.get());
		var cen = (reika.reactorcraft.content.blockentity.IsoCentrifugeBlockEntity)h.getBlockEntity(p);
		var uf6 = new net.neoforged.neoforge.fluids.FluidStack(ReactorFluids.get("uranium_hexafluoride"), 1000);
		var side = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, h.absolutePos(p), Direction.NORTH);
		h.assertTrue(side == null, "centrifuge takes fluid from the side");
		var top = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, h.absolutePos(p), Direction.UP);
		h.assertTrue(top.fill(uf6, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 1000, "top does not take UF6");
		h.succeedWhen(() -> {
			h.assertTrue(cen.getOmega() == 262144 && cen.getTorque() == 8, "centrifuge input "+cen.getOmega()+" rad/s @ "+cen.getTorque()+" Nm (engine "+eng.omega+")");
			int dust = cen.getInventory().getStackInSlot(0).getCount()+cen.getInventory().getStackInSlot(1).getCount();
			h.assertTrue(dust >= 1 && cen.getTank().getFluidAmount() == 1000-50*dust, "dust "+dust+", UF6 left "+cen.getTank().getFluidAmount());
		});
	}
}
