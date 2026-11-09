/*******************************************************************************
 * Tests for the ported ReactorCraft fission core (original TileEntityFuelRod, TileEntityWaterCell, EntityNeutron, meltdown).
 * Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.block.FluoriteBlock;
import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.reactor.FuelRodBlockEntity;
import reika.reactorcraft.reactor.LiquidStates;
import reika.reactorcraft.reactor.WaterCellBlockEntity;
import reika.reactorcraft.registry.FluoriteTypes;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FissionTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static FuelRodBlockEntity core(GameTestHelper h, BlockPos p, int fuel) {
		h.setBlock(p, ReactorBlocks.FUEL_ROD.get());
		FuelRodBlockEntity te = (FuelRodBlockEntity)h.getBlockEntity(p);
		for (int i = 0; i < fuel; i++)
			te.getInventory().setStackInSlot(3-i, new ItemStack(ReactorItems.FUEL.get()));
		return te;
	}

	/** A 3x3 layer of fuelled cores: decay neutrons start fission in the neighbours, each fission heats the hit core by 20 C and keeps it
	 * active. Fission is random and heat loss pulls a small layer back towards ambient, so the test watches the peak of the centre core. */
	@GameTest(template = PLATFORM, timeoutTicks = 1200)
	public static void coresChainReact(GameTestHelper h) {
		FuelRodBlockEntity[] cores = new FuelRodBlockEntity[9];
		for (int i = 0; i < 9; i++)
			cores[i] = core(h, new BlockPos(3+i%3, 1, 3+i/3), 4);
		FuelRodBlockEntity mid = cores[4];
		int[] peak = {0};
		h.onEachTick(() -> peak[0] = Math.max(peak[0], mid.getTemperature()));
		h.succeedWhen(() -> {
			h.assertTrue(mid.isActive(), "centre core never hit by a neutron");
			h.assertTrue(peak[0] >= 40, "centre core peaked at only "+peak[0]+" C");
		});
	}

	/** A coolant cell with water takes half the temperature difference from a hot core next to it each second. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void waterCellCoolsCore(GameTestHelper h) {
		FuelRodBlockEntity a = core(h, new BlockPos(3, 1, 4), 0);
		a.setTemperature(600);
		h.setBlock(new BlockPos(4, 1, 4), ReactorBlocks.WATER_CELL.get());
		WaterCellBlockEntity w = (WaterCellBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		w.setLiquidState(LiquidStates.WATER);
		h.runAtTickTime(45, () -> {
			h.assertTrue(a.getTemperature() < 320, "core still at "+a.getTemperature()+" C");
			h.succeed();
		});
	}

	/** Above 1800 C a core melts down: it and the cores around it turn into corium. Own batch: the explosion (8) would reach the tests next to it. */
	@GameTest(template = PLATFORM, timeoutTicks = 100, batch = "meltdown")
	public static void meltdownMakesCorium(GameTestHelper h) {
		FuelRodBlockEntity a = core(h, new BlockPos(4, 1, 4), 1);
		a.setTemperature(2000);
		h.succeedWhen(() -> h.assertBlockPresent(ReactorBlocks.CORIUM.get(), new BlockPos(4, 1, 4)));
	}

	/** A neutron passing through fluorite activates it (it glows until a random tick resets it). The path runs through air above the
	 * floor: floor stone absorbs about 1 in 18 neutrons per block (original irradiation rule). */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void neutronActivatesFluorite(GameTestHelper h) {
		BlockPos f = new BlockPos(6, 2, 4);
		h.setBlock(f, ReactorBlocks.fluoriteBlock(FluoriteTypes.BLUE).get());
		//three neutrons: a single one is now and then caught by the air (1 in 1000 per block) or steps over the block between two ticks
		for (int i = 0; i < 3; i++)
			h.getLevel().addFreshEntity(new NeutronEntity(h.getLevel(), h.absolutePos(new BlockPos(2, 2, 4)), Direction.EAST, NeutronEntity.NeutronType.DECAY));
		h.succeedWhen(() -> h.assertBlockProperty(f, FluoriteBlock.ACTIVATED, true));
	}

	/** Coolant cell by hand: a water bucket fills it (state and texture), an empty bucket takes the water back. */
	@GameTest(template = PLATFORM)
	public static void coolantCellBuckets(GameTestHelper h) {
		BlockPos p = new BlockPos(4, 1, 4);
		h.setBlock(p, ReactorBlocks.WATER_CELL.get());
		WaterCellBlockEntity w = (WaterCellBlockEntity)h.getBlockEntity(p);
		var player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		h.useBlock(p, player);
		h.assertTrue(w.getLiquidState() == LiquidStates.WATER && player.getMainHandItem().is(Items.BUCKET), "fill: "+w.getLiquidState());
		h.assertBlockProperty(p, reika.reactorcraft.content.block.ReactorCoreBlock.STATE, 1);
		h.useBlock(p, player);
		h.assertTrue(w.getLiquidState() == LiquidStates.EMPTY && player.getMainHandItem().is(Items.WATER_BUCKET), "empty: "+w.getLiquidState());
		h.succeed();
	}
}
