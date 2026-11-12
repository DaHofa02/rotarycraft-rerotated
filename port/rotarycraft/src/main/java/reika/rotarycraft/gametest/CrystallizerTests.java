/*******************************************************************************
 * Tests for the ported RotaryCraft crystallizer (original TileEntityCrystallizer). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.CrystallizerBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.PerformanceEngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CrystallizerTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Performance engine (1024 rad/s @ 256 Nm, meta 0: output west) at (5, 1, z) into a crystallizer at (4, 1, z) (meta 0: input east). */
	private static CrystallizerBlockEntity rig(GameTestHelper h, int z) {
		PerformanceEngineBlockEntity e = place(h, RotaryBlocks.SPORT_ENGINE.get(), 5, 1, z, 0, PerformanceEngineBlockEntity.class);
		e.addFuel(50000);
		e.addWater(50000);
		e.additives = 10000;
		return place(h, RotaryBlocks.CRYSTALLIZER.get(), 4, 1, z, 0, CrystallizerBlockEntity.class);
	}

	private static int fill(GameTestHelper h, CrystallizerBlockEntity te, FluidStack fs) {
		IFluidHandler fh = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, te.getBlockPos(), Direction.UP);
		h.assertTrue(fh != null, "crystallizer has no fluid handler");
		return fh.fill(fs, IFluidHandler.FluidAction.EXECUTE);
	}

	/**
	 * Freezing point -273 + 0.9 * T(K): water (300 K) -3, ethanol (340 K) 33, lava (1300 K) 897 deg C. 1000 mB water give ice, 200 mB a
	 * snowball; 1000 mB lava give stone. One item takes max(1, 400 - 24*log2(omega - 1024 + 1)) ticks: 400 at 1024 rad/s, 208 at 1279;
	 * from 2^17 rad/s over the minimum two items per tick.
	 */
	@GameTest(template = PLATFORM)
	public static void crystallizerFollowsTheOriginalNumbers(GameTestHelper h) {
		h.assertTrue(CrystallizerBlockEntity.getFreezingPoint(new FluidStack(Fluids.WATER, 1000)) == -3, "water freezes at "+CrystallizerBlockEntity.getFreezingPoint(new FluidStack(Fluids.WATER, 1000)));
		h.assertTrue(CrystallizerBlockEntity.getFreezingPoint(new FluidStack(RotaryFluids.ETHANOL.get(), 1000)) == 33, "ethanol freezes at "+CrystallizerBlockEntity.getFreezingPoint(new FluidStack(RotaryFluids.ETHANOL.get(), 1000)));
		h.assertTrue(CrystallizerBlockEntity.getFreezingPoint(new FluidStack(Fluids.LAVA, 1000)) == 897, "lava freezes at "+CrystallizerBlockEntity.getFreezingPoint(new FluidStack(Fluids.LAVA, 1000)));
		h.assertTrue(CrystallizerBlockEntity.getFreezingResult(new FluidStack(Fluids.WATER, 4000)).is(Items.ICE), "4000 mB water");
		h.assertTrue(CrystallizerBlockEntity.getFreezingResult(new FluidStack(Fluids.WATER, 999)).is(Items.SNOWBALL), "999 mB water");
		h.assertTrue(CrystallizerBlockEntity.getFreezingResult(new FluidStack(Fluids.WATER, 199)).isEmpty(), "199 mB water");
		h.assertTrue(CrystallizerBlockEntity.getFreezingResult(new FluidStack(Fluids.LAVA, 1000)).is(Items.STONE), "1000 mB lava");
		h.assertTrue(CrystallizerBlockEntity.getFreezingResult(new FluidStack(Fluids.LAVA, 999)).isEmpty(), "999 mB lava");
		h.assertTrue(CrystallizerBlockEntity.getFreezingResult(new FluidStack(RotaryFluids.ETHANOL.get(), 1000)).is(RotaryItems.ETHANOL.get()), "1000 mB ethanol");
		h.assertTrue(CrystallizerBlockEntity.getRecipeConsumption(new ItemStack(Items.SNOWBALL)) == 200, "a snowball takes 200 mB");
		CrystallizerBlockEntity te = place(h, RotaryBlocks.CRYSTALLIZER.get(), 4, 1, 4, 0, CrystallizerBlockEntity.class);
		int[][] cases = {{1024, 400, 1}, {1279, 208, 1}, {1024+131072, 1, 2}};
		for (int[] c : cases) {
			te.omega = c[0];
			h.assertTrue(te.getOperationTime() == c[1] && te.getNumberConsecutiveOperations() == c[2],
					c[0]+" rad/s: "+te.getOperationTime()+" ticks, "+te.getNumberConsecutiveOperations()+" per tick");
		}
		h.assertTrue(te.getMaxTemperature() == 1000 && te.allowExternalHeating() && te.canBeCooledWithFins(), "crystallizer heat rules");
		h.succeed();
	}

	/** Lava freezes at room temperature: at 1024 rad/s the first stone comes after 400 ticks and takes 1000 mB. */
	@GameTest(template = PLATFORM, timeoutTicks = 1200)
	public static void crystallizerFreezesLavaIntoStone(GameTestHelper h) {
		CrystallizerBlockEntity te = rig(h, 4);
		h.assertTrue(fill(h, te, new FluidStack(Fluids.LAVA, 2500)) == 2500, "lava not taken");
		int[] start = {-1};
		h.onEachTick(() -> {
			if (start[0] < 0 && te.omega >= 1024 && te.power >= 2048)
				start[0] = (int)h.getTick();
		});
		h.succeedWhen(() -> {
			ItemStack out = te.getInventory().getStackInSlot(0);
			h.assertTrue(out.is(Items.STONE), "no stone yet; "+te.omega+" rad/s, progress "+te.freezeTick+", "+te.getTemperature()+" deg C");
			h.assertTrue(out.getCount() == 1 && te.getTank().getFluidAmount() == 1500, out+", "+te.getTank().getFluidAmount()+" mB left");
			h.assertTrue(start[0] >= 0 && h.getTick()-start[0] >= 399, "stone after only "+(h.getTick()-start[0])+" ticks of power");
		});
	}

	/**
	 * Water needs -3 deg C: a crystallizer at room temperature (about 25 deg C) makes nothing; one beside ice (-30) with dry ice (-40)
	 * cools down within a few seconds, uses the dry ice and makes ice from 1000 mB.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1400)
	public static void crystallizerNeedsTheColdForWater(GameTestHelper h) {
		CrystallizerBlockEntity warm = rig(h, 2);
		CrystallizerBlockEntity cold = rig(h, 6);
		h.setBlock(new BlockPos(3, 1, 6), Blocks.ICE);
		fill(h, warm, new FluidStack(Fluids.WATER, 2000));
		fill(h, cold, new FluidStack(Fluids.WATER, 2000));
		cold.getInventory().setStackInSlot(1, new ItemStack(RotaryItems.part(CraftingPart.DRYICE), 32));
		h.succeedWhen(() -> {
			h.assertTrue(cold.getTemperature() <= -3, "cold crystallizer at "+cold.getTemperature()+" deg C");
			h.assertTrue(cold.getInventory().getStackInSlot(0).is(Items.ICE) && cold.getTank().getFluidAmount() == 1000,
					"cold crystallizer: "+cold.getInventory().getStackInSlot(0)+", "+cold.getTank().getFluidAmount()+" mB, progress "+cold.freezeTick);
			h.assertTrue(cold.getInventory().getStackInSlot(1).getCount() < 32, "no dry ice used");
			h.assertTrue(warm.getTemperature() > -3 && warm.getInventory().getStackInSlot(0).isEmpty() && warm.freezeTick == 0
					&& warm.getTank().getFluidAmount() == 2000, "warm crystallizer at "+warm.getTemperature()+" deg C made "+warm.getInventory().getStackInSlot(0));
		});
	}

	/** Without power nothing freezes, however cold. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void crystallizerNeedsItsPower(GameTestHelper h) {
		CrystallizerBlockEntity te = place(h, RotaryBlocks.CRYSTALLIZER.get(), 4, 1, 4, 0, CrystallizerBlockEntity.class);
		fill(h, te, new FluidStack(Fluids.LAVA, 2000));
		h.runAtTickTime(150, () -> {
			h.assertTrue(te.getInventory().getStackInSlot(0).isEmpty() && te.freezeTick == 0 && te.getTank().getFluidAmount() == 2000, "unpowered crystallizer made "+te.getInventory().getStackInSlot(0));
			h.succeed();
		});
	}

	/** Water, lava and ethanol go in (one at a time, up to 8000 mB); dry ice only into slot 1; only slot 0 can be emptied; breaking it drops both slots. */
	@GameTest(template = PLATFORM)
	public static void crystallizerTakesLiquidsAndDryIce(GameTestHelper h) {
		BlockPos pos = new BlockPos(4, 1, 4);
		CrystallizerBlockEntity te = place(h, RotaryBlocks.CRYSTALLIZER.get(), 4, 1, 4, 0, CrystallizerBlockEntity.class);
		h.assertTrue(fill(h, te, new FluidStack(RotaryFluids.LUBRICANT.get(), 1000)) == 0, "lubricant went in");
		h.assertTrue(fill(h, te, new FluidStack(RotaryFluids.ETHANOL.get(), 9000)) == 8000, "not 8000 mB ethanol");
		h.assertTrue(fill(h, te, new FluidStack(Fluids.WATER, 1000)) == 0, "water went in with the ethanol");
		IItemHandler cap = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), Direction.UP);
		h.assertTrue(cap != null && cap.getSlots() == 2, "crystallizer has no item handler");
		ItemStack dry = new ItemStack(RotaryItems.part(CraftingPart.DRYICE), 4);
		h.assertTrue(cap.insertItem(0, dry.copy(), false).getCount() == 4, "dry ice went into the output");
		h.assertTrue(cap.insertItem(1, new ItemStack(Items.SNOWBALL), false).getCount() == 1, "a snowball went into the dry ice slot");
		h.assertTrue(cap.insertItem(1, dry.copy(), false).isEmpty(), "dry ice refused");
		h.assertTrue(cap.extractItem(1, 4, false).isEmpty(), "dry ice came out");
		te.getInventory().setStackInSlot(0, new ItemStack(RotaryItems.ETHANOL.get(), 3));
		h.assertTrue(cap.extractItem(0, 1, false).getCount() == 1, "the output did not come out");
		h.setBlock(pos, Blocks.AIR);
		int eth = 0, ice = 0;
		for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(pos)).inflate(3))) {
			eth += e.getItem().is(RotaryItems.ETHANOL.get()) ? e.getItem().getCount() : 0;
			ice += e.getItem().is(RotaryItems.part(CraftingPart.DRYICE)) ? e.getItem().getCount() : 0;
		}
		h.assertTrue(eth == 2 && ice == 4, "dropped "+eth+" ethanol, "+ice+" dry ice");
		h.succeed();
	}
}
