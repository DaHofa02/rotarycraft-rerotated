/*******************************************************************************
 * Tests for the ported ReactorCraft reactor generator (original TileEntityReactorGenerator, BlockGeneratorMulti and the steam bypass
 * branch of BlockSteam). Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.electricraft.content.blockentity.MotorBlockEntity;
import reika.electricraft.content.blockentity.RFBatteryBlockEntity;
import reika.electricraft.content.blockentity.WireBlockEntity;
import reika.electricraft.registry.ElectriBlocks;
import reika.electricraft.registry.WireType;
import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.block.GeneratorMultiBlock;
import reika.reactorcraft.content.block.SteamBlock;
import reika.reactorcraft.powergen.ReactorGeneratorBlockEntity;
import reika.reactorcraft.powergen.TurbineCoreBlockEntity;
import reika.reactorcraft.powergen.WorkingFluid;
import reika.reactorcraft.registry.ReactorBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class GeneratorTests {

	/** Turbine (x=1) + 9 rotor cores (x=2..10) + generator (x=11) + output (x=12..14). */
	private static final String LONG = "platform_16x8x9";

	private static final int CY = 3;
	private static final int CZ = 4;

	private static Block part(int variant) {
		return switch (variant) {
			case 0 -> ReactorBlocks.GENERATOR_CORE.get();
			case 1 -> ReactorBlocks.GENERATOR_WINDING.get();
			case 2 -> ReactorBlocks.GENERATOR_HOUSING.get();
			default -> ReactorBlocks.STEAM_BYPASS.get();
		};
	}

	/** The original blueprint (BlockGeneratorMulti checks) along +x from x=2, centred on (CY, CZ); the generator at x=11 facing west. */
	private static ReactorGeneratorBlockEntity buildGenerator(GameTestHelper h) {
		for (int i = 0; i < 9; i++) {
			int x = 2+i;
			h.setBlock(new BlockPos(x, CY, CZ), part(0));
			int wind = i < 2 ? 3 : 1;
			for (int k = -1; k <= 1; k++) {
				h.setBlock(new BlockPos(x, CY+k, CZ-1), part(wind));
				h.setBlock(new BlockPos(x, CY+k, CZ+1), part(wind));
				h.setBlock(new BlockPos(x, CY+k, CZ-2), part(2));
				h.setBlock(new BlockPos(x, CY+k, CZ+2), part(2));
				if (k != 0)
					h.setBlock(new BlockPos(x, CY+k, CZ), part(wind));
			}
			for (int k = -2; k <= 2; k += 4) {
				h.setBlock(new BlockPos(x, CY+k, CZ-1), part(2));
				h.setBlock(new BlockPos(x, CY+k, CZ+1), part(2));
				h.setBlock(new BlockPos(x, CY+k, CZ), part(i == 1 && k == 2 ? 3 : 2));
				if (i < 2) {
					h.setBlock(new BlockPos(x, CY+k, CZ-2), part(2));
					h.setBlock(new BlockPos(x, CY+k, CZ+2), part(2));
				}
			}
		}
		for (int k = -2; k <= 2; k++)
			for (int m = -2; m <= 2; m++)
				if ((Math.abs(k) != 2 || Math.abs(m) != 2) && (k != 0 || m != 0))
					h.setBlock(new BlockPos(11, CY+k, CZ+m), part(2));
		h.setBlock(new BlockPos(11, CY, CZ), ReactorBlocks.GENERATOR.get());
		ReactorGeneratorBlockEntity gen = (ReactorGeneratorBlockEntity)h.getBlockEntity(new BlockPos(11, CY, CZ));
		gen.setFacing(Direction.WEST);
		return gen;
	}

	private static boolean form(GameTestHelper h) {
		BlockPos start = h.absolutePos(new BlockPos(2, CY, CZ));
		if (!GeneratorMultiBlock.checkForFullMultiBlock(h.getLevel(), start, Direction.EAST))
			return false;
		GeneratorMultiBlock.onCreateFullMultiBlock(h.getLevel(), start);
		return true;
	}

	/** A steam turbine at x=1 with its steam moving east (towards the generator) over a steam grate two blocks below it. */
	private static TurbineCoreBlockEntity turbine(GameTestHelper h) {
		h.setBlock(new BlockPos(1, CY-2, CZ), ReactorBlocks.STEAM_GRATE.get());
		BlockEntity grate = h.getBlockEntity(new BlockPos(1, CY-2, CZ));
		CompoundTag t = grate.saveWithoutMetadata(h.getLevel().registryAccess());
		t.putInt("energy", 100000);
		WorkingFluid.WATER.saveToNBT(t);
		grate.loadWithComponents(t, h.getLevel().registryAccess());
		h.setBlock(new BlockPos(1, CY, CZ), ReactorBlocks.TURBINE_CORE.get());
		TurbineCoreBlockEntity te = (TurbineCoreBlockEntity)h.getBlockEntity(new BlockPos(1, CY, CZ));
		te.setBlockMetadata(1);
		te.addLubricant(10000);
		return te;
	}

	/**
	 * The blueprint matches only from the turbine end of the rotor along the row, and only with the generator facing back along it;
	 * forming hides every part, and taking out one winding un-forms the whole structure again.
	 */
	@GameTest(template = LONG, timeoutTicks = 40)
	public static void multiblockFormsAndBreaks(GameTestHelper h) {
		ReactorGeneratorBlockEntity gen = buildGenerator(h);
		h.assertFalse(GeneratorMultiBlock.checkForFullMultiBlock(h.getLevel(), h.absolutePos(new BlockPos(10, CY, CZ)), Direction.WEST), "matched from the generator end");
		gen.setFacing(Direction.EAST);
		h.assertFalse(GeneratorMultiBlock.checkForFullMultiBlock(h.getLevel(), h.absolutePos(new BlockPos(2, CY, CZ)), Direction.EAST), "matched a wrongly facing generator");
		gen.setFacing(Direction.WEST);
		h.assertTrue(form(h), "blueprint does not match");
		h.assertTrue(gen.hasMultiBlock(), "generator not formed");
		h.assertTrue(h.getBlockState(new BlockPos(5, CY+1, CZ)).getValue(GeneratorMultiBlock.FORMED), "winding not formed");
		h.assertTrue(h.getBlockState(new BlockPos(11, CY+2, CZ+1)).getValue(GeneratorMultiBlock.FORMED), "end cap not formed");
		h.setBlock(new BlockPos(6, CY-1, CZ+1), Blocks.AIR);
		h.runAfterDelay(2, () -> {
			h.assertFalse(gen.hasMultiBlock(), "generator still formed");
			h.assertFalse(h.getBlockState(new BlockPos(2, CY+2, CZ)).getValue(GeneratorMultiBlock.FORMED), "housing still formed");
			h.succeed();
		});
	}

	/** Shift-screwdriving cycles RF -> ElectriCraft -> RF; the IC2 EU mode does not exist on 1.21.1 and is skipped. */
	@GameTest(template = LONG, timeoutTicks = 20)
	public static void modeCycleSkipsEU(GameTestHelper h) {
		h.setBlock(new BlockPos(5, 1, 4), ReactorBlocks.GENERATOR.get());
		ReactorGeneratorBlockEntity gen = (ReactorGeneratorBlockEntity)h.getBlockEntity(new BlockPos(5, 1, 4));
		h.assertTrue(gen.getMode() == ReactorGeneratorBlockEntity.Modes.RF, "default mode "+gen.getMode());
		gen.onShiftRightClick(h.getLevel(), gen.getBlockPos(), Direction.UP);
		h.assertTrue(gen.getMode() == ReactorGeneratorBlockEntity.Modes.ELC, "second mode "+gen.getMode());
		gen.onShiftRightClick(h.getLevel(), gen.getBlockPos(), Direction.UP);
		h.assertTrue(gen.getMode() == ReactorGeneratorBlockEntity.Modes.RF, "third mode "+gen.getMode());
		h.assertFalse(gen.onRightClick(h.getLevel(), gen.getBlockPos(), Direction.UP), "facing set to up");
		h.assertTrue(gen.onRightClick(h.getLevel(), gen.getBlockPos(), Direction.NORTH) && gen.getFacing() == Direction.NORTH, "facing not set");
		h.succeed();
	}

	/**
	 * Formed generator on a running turbine in RF mode: it reads the turbine's speed and torque (power = omega*torque, as in the
	 * original, not the turbine's own power figure) and pushes power/520 RF per tick into the RF
	 * battery on its output side. The turbine's exhaust steam hits the steam bypass and comes out 3 blocks above the turbine, past it.
	 */
	@GameTest(template = LONG, timeoutTicks = 400)
	public static void generatorMakesRF(GameTestHelper h) {
		ReactorGeneratorBlockEntity gen = buildGenerator(h);
		h.assertTrue(form(h), "blueprint does not match");
		TurbineCoreBlockEntity tb = turbine(h);
		h.setBlock(new BlockPos(12, CY, CZ), ElectriBlocks.RF_BATTERY.get());
		RFBatteryBlockEntity bat = (RFBatteryBlockEntity)h.getBlockEntity(new BlockPos(12, CY, CZ));
		boolean[] bypassed = new boolean[1];
		h.onEachTick(() -> {
			if (h.getBlockState(new BlockPos(3, CY+3, CZ)).getBlock() instanceof SteamBlock)
				bypassed[0] = true;
		});
		h.runAtTickTime(300, () -> {
			h.assertTrue(tb.getPower() > 0 && gen.getPower() > 0, "turbine "+tb.getPower()+" W, generator "+gen.getPower()+" W");
			h.assertTrue((int)gen.getGenUnits() == (int)(gen.getPower()/520D), "RF/t "+gen.getGenUnits()+" for "+gen.getPower()+" W");
			h.assertTrue(bat.getEnergyStorage(Direction.WEST).getEnergyStored() > 0, "battery empty");
			h.assertTrue(bypassed[0], "no steam came over the generator");
			h.succeed();
		});
	}

	/** Without the formed multiblock the generator reads nothing from the same running turbine. */
	@GameTest(template = LONG, timeoutTicks = 400)
	public static void unformedGeneratorIsIdle(GameTestHelper h) {
		ReactorGeneratorBlockEntity gen = buildGenerator(h);
		TurbineCoreBlockEntity tb = turbine(h);
		h.setBlock(new BlockPos(12, CY, CZ), ElectriBlocks.RF_BATTERY.get());
		RFBatteryBlockEntity bat = (RFBatteryBlockEntity)h.getBlockEntity(new BlockPos(12, CY, CZ));
		h.runAtTickTime(300, () -> {
			h.assertTrue(tb.getPower() > 0, "turbine not running");
			h.assertTrue(gen.getPower() == 0 && bat.getEnergyStorage(Direction.WEST).getEnergyStored() == 0, "unformed generator made "+gen.getPower()+" W");
			h.succeed();
		});
	}

	/**
	 * ElectriCraft mode: the wire next to the output side wraps the generator as a network source (voltage = 8*omega, current =
	 * averaged torque/8) and an ElectriCraft motor at the end of two steel wires receives it.
	 */
	@GameTest(template = LONG, timeoutTicks = 400)
	public static void generatorFeedsWires(GameTestHelper h) {
		ReactorGeneratorBlockEntity gen = buildGenerator(h);
		h.assertTrue(form(h), "blueprint does not match");
		gen.stepType();
		turbine(h);
		Block wire = ElectriBlocks.getWire(WireType.STEEL, false);
		h.setBlock(new BlockPos(12, CY, CZ), wire);
		h.setBlock(new BlockPos(13, CY, CZ), wire);
		h.setBlock(new BlockPos(14, CY, CZ), ElectriBlocks.MOTOR.get());
		MotorBlockEntity mot = (MotorBlockEntity)h.getBlockEntity(new BlockPos(14, CY, CZ));
		mot.setFacing(Direction.WEST);
		WireBlockEntity w = (WireBlockEntity)h.getBlockEntity(new BlockPos(12, CY, CZ));
		h.runAtTickTime(300, () -> {
			h.assertTrue(gen.isFunctional() && gen.getOmega() > 0, "generator: "+gen.getMode()+", "+gen.getOmega()+" rad/s");
			h.assertTrue(w.getNetwork() != null && w.getNetwork().getNumberSources() == 1, "network sources: "+(w.getNetwork() == null ? "no network" : w.getNetwork().getNumberSources()));
			h.assertTrue(w.getWireVoltage() > 0 && w.getWireCurrent() > 0, "wire "+w.getWireVoltage()+" V, "+w.getWireCurrent()+" A");
			h.succeed();
		});
	}

	/** Original testBreakageFailure: un-forming the generator while its rotor turns faster than 1024 rad/s blows it up. */
	@GameTest(template = LONG, timeoutTicks = 400, batch = "meltdown")
	public static void unformingAtSpeedExplodes(GameTestHelper h) {
		ReactorGeneratorBlockEntity gen = buildGenerator(h);
		h.assertTrue(form(h), "blueprint does not match");
		turbine(h);
		h.runAtTickTime(300, () -> {
			h.assertTrue(gen.getOmega() > 1024, "rotor at "+gen.getOmega()+" rad/s");
			h.setBlock(new BlockPos(11, CY+1, CZ), Blocks.AIR);
		});
		h.runAtTickTime(305, () -> {
			h.assertFalse(h.getBlockState(new BlockPos(11, CY, CZ)).is(ReactorBlocks.GENERATOR.get()), "generator survived");
			h.succeed();
		});
	}
}
