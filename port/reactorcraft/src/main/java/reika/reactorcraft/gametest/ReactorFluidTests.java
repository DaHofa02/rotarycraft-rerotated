/*******************************************************************************
 * Tests for the ported ReactorCraft fluids and their containers (original FluidContainerRegistry entries).
 * Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.content.blockentity.ReservoirBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ReactorFluidTests {

	private static final String PLATFORM = "platform_9x4x9";

	/** An empty canister takes exactly 1000 mB of a canister fluid and becomes that canister; draining gives the empty one back. */
	@GameTest(template = PLATFORM)
	public static void canisterFillsAndEmpties(GameTestHelper h) {
		var handler = FluidUtil.getFluidHandler(new ItemStack(ReactorItems.EMPTY_CANISTER.get())).orElseThrow();
		h.assertTrue(handler.fill(new FluidStack(ReactorFluids.get("ammonia"), 500), IFluidHandler.FluidAction.EXECUTE) == 0, "half filled");
		h.assertTrue(handler.fill(new FluidStack(ReactorFluids.get("heavy_water"), 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "heavy water has no canister");
		h.assertTrue(handler.fill(new FluidStack(ReactorFluids.get("ammonia"), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "not filled");
		ItemStack full = handler.getContainer();
		h.assertTrue(full.is(ReactorItems.canisterFor(ReactorFluids.get("ammonia"))), "became "+full);
		var h2 = FluidUtil.getFluidHandler(full).orElseThrow();
		FluidStack out = h2.drain(1000, IFluidHandler.FluidAction.EXECUTE);
		h.assertTrue(out.getAmount() == 1000 && out.getFluid().isSame(ReactorFluids.get("ammonia")), "drained "+out);
		h.assertTrue(h2.getContainer().is(ReactorItems.EMPTY_CANISTER.get()), "after draining "+h2.getContainer());
		h.assertTrue(full.getCraftingRemainingItem().is(ReactorItems.EMPTY_CANISTER.get()), "crafting remainder");
		h.succeed();
	}

	/** A vanilla bucket picks up heavy water as the heavy water bucket (and pours it into a RotaryCraft reservoir); fluid data as in the original. */
	@GameTest(template = PLATFORM)
	public static void heavyWaterBucketAndReservoir(GameTestHelper h) {
		var bucket = FluidUtil.getFluidHandler(new ItemStack(Items.BUCKET)).orElseThrow();
		h.assertTrue(bucket.fill(new FluidStack(ReactorFluids.get("heavy_water"), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "bucket not filled");
		h.assertTrue(bucket.getContainer().is(ReactorItems.HEAVY_WATER_BUCKET.get()), "bucket became "+bucket.getContainer());
		BlockPos p = new BlockPos(4, 1, 4);
		h.setBlock(p, RotaryBlocks.RESERVOIR.get());
		IFluidHandler res = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(p), net.minecraft.core.Direction.NORTH);
		h.assertTrue(res != null, "reservoir has no fluid handler");
		var result = FluidUtil.tryEmptyContainer(bucket.getContainer(), res, 1000, null, true);
		h.assertTrue(result.isSuccess() && result.getResult().is(Items.BUCKET), "bucket not emptied: "+result.getResult());
		ReservoirBlockEntity tr = (ReservoirBlockEntity)h.getBlockEntity(p);
		h.assertTrue(tr.getFluidAmount() == 1000 && tr.getFluid().isSame(ReactorFluids.get("heavy_water")), "reservoir holds "+tr.getFluidAmount());
		h.assertTrue(ReactorFluids.TYPE.get("hot_sodium").get().getTemperature() == 2000 && ReactorFluids.TYPE.get("fusion_plasma").get().getLightLevel() == 15
				&& ReactorFluids.TYPE.get("heavy_water").get().getDensity() == 1100, "fluid properties");
		h.succeed();
	}
}
