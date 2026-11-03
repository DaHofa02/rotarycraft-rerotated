/*******************************************************************************
 * Tests for the ported ReactorCraft solar add-ons on the RotaryCraft solar tower (original TileEntitySolarTop, TileEntitySolarExchanger
 * and the sodium branch of TileEntitySolar). Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.powergen.SolarExchangerBlockEntity;
import reika.reactorcraft.powergen.SolarTopBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.SolarTowerBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SolarSodiumTests {

	private static final String TALL = "platform_9x8x9";

	/** Tower blocks at x=4, z=4 from y0 up, the two-block sodium receiver on top, mirrors at the given level around (x, z). */
	private static SolarTowerBlockEntity buildTower(GameTestHelper h, int y0, int height, int[][] mirrors, int my) {
		for (int i = 0; i < height; i++)
			h.setBlock(new BlockPos(4, y0+i, 4), RotaryBlocks.SOLAR_TOWER.get());
		h.setBlock(new BlockPos(4, y0+height, 4), ReactorBlocks.SOLAR_TOP.get());
		h.setBlock(new BlockPos(4, y0+height+1, 4), ReactorBlocks.SOLAR_TOP.get());
		for (int[] m : mirrors)
			h.setBlock(new BlockPos(m[0], my, m[1]), RotaryBlocks.MIRROR.get());
		return (SolarTowerBlockEntity)h.getBlockEntity(new BlockPos(4, y0, 4));
	}

	/**
	 * Eight mirrors at noon heat the receiver by 0.125 C each per tick (one degree per tick; the original adds to an integer, so fewer
	 * than 8 mirrors at full light do not heat it at all), while it falls back by one degree every 8 ticks: it gets hot, only the lower
	 * block is active, the cap shows its temperature, and the tower now accepts sodium.
	 */
	@GameTest(template = TALL, timeoutTicks = 200)
	public static void receiverHeatsUp(GameTestHelper h) {
		h.getLevel().setDayTime(6000);
		int[][] ring = {{3, 3}, {4, 3}, {5, 3}, {3, 4}, {5, 4}, {3, 5}, {4, 5}, {5, 5}};
		SolarTowerBlockEntity t = buildTower(h, 1, 2, ring, 1);
		SolarTopBlockEntity top = (SolarTopBlockEntity)h.getBlockEntity(new BlockPos(4, 3, 4));
		SolarTopBlockEntity cap = (SolarTopBlockEntity)h.getBlockEntity(new BlockPos(4, 4, 4));
		int amb = AmbientTemperature.getAmbientTemperatureAt(h.getLevel(), h.absolutePos(new BlockPos(4, 3, 4)));
		h.assertTrue(t.fill(Direction.UP, new FluidStack(ReactorFluids.get("sodium"), 1000), IFluidHandler.FluidAction.SIMULATE) == 1000, "no sodium with the receiver");
		h.runAtTickTime(150, () -> {
			h.assertTrue(top.isActive() && !cap.isActive(), "active: "+top.isActive()+"/"+cap.isActive());
			h.assertTrue(top.getTemperature() > amb+80, "receiver at "+top.getTemperature()+" C (ambient "+amb+")");
			h.assertTrue(cap.getTemperature() == top.getTemperature(), "cap shows "+cap.getTemperature());
			h.succeed();
		});
	}

	/** Without the receiver on top the tower takes water but not sodium. */
	@GameTest(template = TALL, timeoutTicks = 20)
	public static void sodiumNeedsReceiver(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 1, 4), RotaryBlocks.SOLAR_TOWER.get());
		SolarTowerBlockEntity t = (SolarTowerBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		h.assertTrue(t.fill(Direction.UP, new FluidStack(ReactorFluids.get("sodium"), 1000), IFluidHandler.FluidAction.SIMULATE) == 0, "took sodium");
		h.assertTrue(t.fill(Direction.UP, new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE) == 1000, "no water");
		h.succeed();
	}

	/**
	 * Sodium mode with the receiver at 1000 C (held there): 4096 rad/s at min(65536, 1.75 * 1 * 2 * 5^1.5) = 39 Nm; the used sodium
	 * (here 1 mB per tick) goes as warm sodium into the solar exchanger under the tower, which runs on a microturbine (2 MW) through
	 * bevel gears below it.
	 */
	@GameTest(template = TALL, timeoutTicks = 400)
	public static void sodiumTowerFeedsExchanger(GameTestHelper h) {
		h.getLevel().setDayTime(6000);
		h.setBlock(new BlockPos(5, 1, 4), RotaryBlocks.MICRO_ENGINE.get());
		EngineBlockEntity eng = (EngineBlockEntity)h.getBlockEntity(new BlockPos(5, 1, 4));
		eng.setBlockMetadata(0);
		eng.addFuel(50000);
		h.setBlock(new BlockPos(4, 1, 4), RotaryBlocks.BEVEL_GEARS.get());
		BevelGearBlockEntity bev = (BevelGearBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		h.setBlock(new BlockPos(4, 2, 4), ReactorBlocks.SOLAR_EXCHANGER.get());
		SolarExchangerBlockEntity ex = (SolarExchangerBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		int[][] cross = {{3, 4}, {5, 4}, {4, 3}, {4, 5}};
		SolarTowerBlockEntity t = buildTower(h, 3, 2, cross, 3);
		SolarTopBlockEntity top = (SolarTopBlockEntity)h.getBlockEntity(new BlockPos(4, 5, 4));
		h.onEachTick(() -> {
			top.setTemperature(1000);
			t.fill(Direction.UP, new FluidStack(ReactorFluids.get("sodium"), 4000), IFluidHandler.FluidAction.EXECUTE);
		});
		h.succeedWhen(() -> {
			h.assertTrue(ex.isActive(), "exchanger: "+ex.getPower()+" W at "+ex.getOmega()+" rad/s");
			h.assertTrue(t.getOmega() == 4096 && t.getTorque() == 39, "tower: "+t.getOmega()+" rad/s, "+t.getTorque()+" Nm, consumption "+t.getCurrentConsumption());
			h.assertTrue(ex.getTank().getFluidAmount() > 0 && ex.getTank().getFluid().getFluid().isSame(ReactorFluids.get("warm_sodium")),
					"exchanger tank "+ex.getTank().getFluidAmount());
		});
	}
}
