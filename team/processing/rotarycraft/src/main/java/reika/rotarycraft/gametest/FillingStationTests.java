/*******************************************************************************
 * Tests for the ported RotaryCraft filling station and integrated gearbox upgrade (original TileEntityFillingStation,
 * ItemIntegratedGearbox, TileEntityEngine.applyIntegratedGear). Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for
 * distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.content.blockentity.FillingStationBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.IntegratedGearboxItem;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FillingStationTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, 1, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** DC engine (256 rad/s, 1024 W, meta 0: output west) into a filling station (meta 0 reads from the east). */
	private static FillingStationBlockEntity rig(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 5, 4, 0, EngineBlockEntity.class);
		h.setBlock(new BlockPos(5, 1, 5), Blocks.REDSTONE_BLOCK);
		return place(h, RotaryBlocks.FILLING_STATION.get(), 4, 4, 0, FillingStationBlockEntity.class);
	}

	/**
	 * Lubricant piped in from the side; a 2x gear upgrade in the input slot moves to the filling slot, takes 4*log2(256) = 32 mB per
	 * tick and, full (500 mB), goes to the output as a torque gear. Nothing drains from the side, the tank drains from below.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void fillingStationFillsUpgrade(GameTestHelper h) {
		FillingStationBlockEntity fs = rig(h);
		IFluidHandler side = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, fs.getBlockPos(), Direction.NORTH);
		h.assertTrue(side.fill(new FluidStack(RotaryFluids.LUBRICANT.get(), 2000), IFluidHandler.FluidAction.EXECUTE) == 2000, "lubricant refused");
		h.assertTrue(side.drain(100, IFluidHandler.FluidAction.SIMULATE).isEmpty(), "drained from the side");
		IFluidHandler down = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, fs.getBlockPos(), Direction.DOWN);
		h.assertTrue(down.drain(100, IFluidHandler.FluidAction.SIMULATE).getAmount() == 100, "not drainable from below");
		fs.getInventory().setStackInSlot(FillingStationBlockEntity.INPUT_SLOT, new ItemStack(RotaryItems.GEAR_UPGRADES.get(1).get()));
		h.succeedWhen(() -> {
			ItemStack out = fs.getInventory().getStackInSlot(FillingStationBlockEntity.OUTPUT_SLOT);
			h.assertTrue(out.is(RotaryItems.GEAR_UPGRADES.get(1).get()), "output "+out+", filling "+fs.getInventory().getStackInSlot(0));
			h.assertTrue(IntegratedGearboxItem.getRatioFromIntegratedGearItem(out, true) == 2 && IntegratedGearboxItem.fillState(out) == 0.5F, "not a full torque gear");
			h.assertTrue(fs.getTank().getFluidAmount() == 1500 && fs.getRedstoneOverride() == 15, "tank "+fs.getTank().getFluidAmount());
		});
	}

	/** An ethanol crystal in the fuel slot gives 1000 mB ethanol; a lubricant bucket is emptied into a lubricant tank (bucket back). */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void fillingStationTakesFuelItems(GameTestHelper h) {
		FillingStationBlockEntity fs = rig(h);
		fs.getInventory().setStackInSlot(FillingStationBlockEntity.FUEL_SLOT, new ItemStack(RotaryItems.ETHANOL.get()));
		h.runAtTickTime(10, () -> {
			h.assertTrue(fs.getTank().getFluid().is(RotaryFluids.ETHANOL.get()) && fs.getTank().getFluidAmount() == 1000, "tank "+fs.getTank().getFluid().getAmount());
			fs.getTank().setFluid(FluidStack.EMPTY);
			fs.getInventory().setStackInSlot(FillingStationBlockEntity.FUEL_SLOT, new ItemStack(RotaryItems.LUBRICANT_BUCKET.get()));
		});
		h.runAtTickTime(20, () -> {
			h.assertTrue(fs.getTank().getFluid().is(RotaryFluids.LUBRICANT.get()) && fs.getTank().getFluidAmount() == 1000, "after the bucket "+fs.getTank().getFluid().getAmount());
			h.assertTrue(fs.getInventory().getStackInSlot(FillingStationBlockEntity.FUEL_SLOT).is(Items.BUCKET), "no empty bucket left");
			h.succeed();
		});
	}

	/**
	 * Gas engine (512 rad/s @ 128 Nm) with a built-in 2x gear: lubricant gives 256 rad/s @ 256 Nm, liquid nitrogen 1024 rad/s @ 64 Nm.
	 * A second gear is refused; breaking the engine drops an unfilled gear.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void integratedGearChangesEngineOutput(GameTestHelper h) {
		EngineBlockEntity torque = place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 2, 0, EngineBlockEntity.class);
		EngineBlockEntity speed = place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 6, 0, EngineBlockEntity.class);
		ItemStack lube = IntegratedGearboxItem.getIntegratedGearItem(2, RotaryFluids.LUBRICANT.get());
		ItemStack ln2 = IntegratedGearboxItem.getIntegratedGearItem(2, RotaryFluids.LIQUID_NITROGEN.get());
		h.assertTrue(!torque.applyIntegratedGear(new ItemStack(RotaryItems.GEAR_UPGRADES.get(1).get())), "unfilled gear accepted");
		h.assertTrue(torque.applyIntegratedGear(lube) && torque.getIntegratedGear() == 2, "torque gear refused");
		h.assertTrue(!torque.applyIntegratedGear(ln2), "second gear accepted");
		h.assertTrue(speed.applyIntegratedGear(ln2) && speed.getIntegratedGear() == -2, "speed gear refused");
		torque.addFuel(50000);
		speed.addFuel(50000);
		DynamometerBlockEntity d1 = place(h, RotaryBlocks.DYNAMOMETER.get(), 4, 2, 0, DynamometerBlockEntity.class);
		DynamometerBlockEntity d2 = place(h, RotaryBlocks.DYNAMOMETER.get(), 4, 6, 0, DynamometerBlockEntity.class);
		h.succeedWhen(() -> {
			h.assertTrue(d1.getOmega() == 256 && d1.getTorque() == 256, "torque gear "+d1.getOmega()+" rad/s @ "+d1.getTorque()+" Nm");
			h.assertTrue(d2.getOmega() == 1024 && d2.getTorque() == 64, "speed gear "+d2.getOmega()+" rad/s @ "+d2.getTorque()+" Nm");
			h.setBlock(new BlockPos(5, 1, 2), Blocks.AIR);
			boolean dropped = false;
			for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(5, 1, 2))).inflate(2)))
				if (e.getItem().is(RotaryItems.GEAR_UPGRADES.get(1).get()) && IntegratedGearboxItem.fillState(e.getItem()) == 0)
					dropped = true;
			h.assertTrue(dropped, "no gear dropped");
		});
	}
}
