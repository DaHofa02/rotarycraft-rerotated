/*******************************************************************************
 * Regression tests for the ported ElectriCraft wire network. Expected values follow from the original code: generator voltage
 * = 8*omega, current = torque/8; motor torque = 8*current, speed = (voltage - path resistance)/8; steel wire: 16 A, 64 ohm.
 * Private NeoForge 1.21.1 port of ElectriCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.electricraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.electricraft.ElectriCraft;
import net.minecraft.world.item.DyeColor;

import reika.electricraft.content.blockentity.FuseBlockEntity;
import reika.electricraft.content.blockentity.GeneratorBlockEntity;
import reika.electricraft.content.blockentity.MeterBlockEntity;
import reika.electricraft.content.blockentity.ResistorBlockEntity;
import reika.electricraft.content.blockentity.MotorBlockEntity;
import reika.electricraft.registry.ElectriBlocks;
import reika.electricraft.registry.WireType;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ElectriCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WireNetworkTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int z, Class<T> cls) {
		BlockPos p = new BlockPos(x, 1, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (!cls.isInstance(be))
			throw new IllegalStateException("no "+cls.getSimpleName()+" at "+p);
		return cls.cast(be);
	}

	/**
	 * DC engine (256 rad/s, 4 Nm, x=8, -x) -> wooden 4:1 gearbox (64 rad/s, 16 Nm) -> generator (x=6, input on +x: 512 V, 2 A) ->
	 * the given steel wires from x=5 down -> motor (input on +x) -> dynamometer on its -x side. Returns the dynamometer.
	 */
	private static DynamometerBlockEntity chain(GameTestHelper h, int wires) {
		Block[] line = new Block[wires];
		java.util.Arrays.fill(line, ElectriBlocks.getWire(WireType.STEEL, false));
		return chain(h, line);
	}

	/** As above with the given blocks (wires or wire components, which face east by default) from x=5 down. */
	private static DynamometerBlockEntity chain(GameTestHelper h, Block[] line) {
		int wires = line.length;
		RotaryCraftBlockEntity eng = place(h, RotaryBlocks.DC_ENGINE.get(), 8, 4, RotaryCraftBlockEntity.class);
		eng.setBlockMetadata(0);
		h.setBlock(new BlockPos(8, 1, 5), Blocks.REDSTONE_BLOCK);
		GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 7, 4, GearboxBlockEntity.class);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.WOOD, 4));
		gbx.setBlockMetadata(4);
		GeneratorBlockEntity gen = place(h, ElectriBlocks.GENERATOR.get(), 6, 4, GeneratorBlockEntity.class);
		gen.setFacing(Direction.EAST);
		for (int i = 0; i < wires; i++)
			h.setBlock(new BlockPos(5-i, 1, 4), line[i]);
		int motorX = 5-wires;
		MotorBlockEntity mot = place(h, ElectriBlocks.MOTOR.get(), motorX, 4, MotorBlockEntity.class);
		mot.setFacing(Direction.EAST);
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), motorX-1, 4, DynamometerBlockEntity.class);
		dyna.setBlockMetadata(0);
		return dyna;
	}

	/** Two steel wires (128 ohm): the motor gives 8*2 A = 16 Nm at (512-128)/8 = 48 rad/s. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void generatorDrivesMotorOverWires(GameTestHelper h) {
		DynamometerBlockEntity dyna = chain(h, 2);
		GeneratorBlockEntity gen = (GeneratorBlockEntity)h.getBlockEntity(new BlockPos(6, 1, 4));
		h.succeedWhen(() -> {
			h.assertTrue(gen.getGenVoltage() == 512 && gen.getGenCurrent() == 2, "generator "+gen.getGenVoltage()+" V, "+gen.getGenCurrent()+" A");
			h.assertTrue(dyna.omega == 48 && dyna.torque == 16, "expected 48 rad/s @ 16 Nm, got "+dyna.omega+" @ "+dyna.torque);
		});
	}

	/** Resistance adds up per wire block: four steel wires (256 ohm) leave (512-256)/8 = 32 rad/s, the current is unchanged. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void longerLineLosesVoltage(GameTestHelper h) {
		DynamometerBlockEntity dyna = chain(h, 4);
		h.succeedWhen(() -> h.assertTrue(dyna.omega == 32 && dyna.torque == 16, "expected 32 rad/s @ 16 Nm, got "+dyna.omega+" @ "+dyna.torque));
	}

	/** Breaking a wire splits the network: the motor stops. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void cutWireStopsMotor(GameTestHelper h) {
		DynamometerBlockEntity dyna = chain(h, 2);
		h.runAtTickTime(100, () -> {
			h.assertTrue(dyna.omega == 48, "motor not running before the cut: "+dyna.omega);
			h.setBlock(new BlockPos(4, 1, 4), Blocks.AIR);
		});
		h.runAtTickTime(160, () -> {
			h.assertTrue(dyna.power == 0, "motor still delivers "+dyna.power+" W after the cut");
			h.succeed();
		});
	}

	private static Block steel() {
		return ElectriBlocks.getWire(WireType.STEEL, false);
	}

	/** A relay conducts only with a redstone signal: wire, relay, wire (128 ohm) -> 48 rad/s once a redstone block is next to it. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void relayNeedsRedstone(GameTestHelper h) {
		DynamometerBlockEntity dyna = chain(h, new Block[] {steel(), ElectriBlocks.RELAY.get(), steel()});
		h.runAtTickTime(100, () -> {
			h.assertTrue(dyna.power == 0, "open relay passes "+dyna.power+" W");
			h.setBlock(new BlockPos(4, 1, 5), Blocks.REDSTONE_BLOCK);
		});
		h.runAtTickTime(200, () -> {
			h.assertTrue(dyna.omega == 48 && dyna.torque == 16, "closed relay: expected 48 rad/s @ 16 Nm, got "+dyna.omega+" @ "+dyna.torque);
			h.succeed();
		});
	}

	/** A resistor without bands (0 A) does not connect; black-brown-black (01 x 10^0 = 1 A) halves the 2 A: 8 Nm. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void resistorLimitsCurrent(GameTestHelper h) {
		DynamometerBlockEntity dyna = chain(h, new Block[] {steel(), ElectriBlocks.RESISTOR.get(), steel()});
		ResistorBlockEntity r = (ResistorBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		h.runAtTickTime(100, () -> {
			h.assertTrue(dyna.power == 0, "resistor without a value passes "+dyna.power+" W");
			h.assertTrue(r.setColor(DyeColor.BROWN, 2), "band 2 not set");
			h.assertTrue(!r.setColor(DyeColor.WHITE, 3), "multiplier 10^9 accepted");
			h.assertTrue(r.getCurrentLimit() == 1, "limit "+r.getCurrentLimit());
			r.rebuildNetwork();
		});
		h.runAtTickTime(200, () -> {
			h.assertTrue(dyna.omega == 48 && dyna.torque == 8, "expected 48 rad/s @ 8 Nm, got "+dyna.omega+" @ "+dyna.torque);
			h.succeed();
		});
	}

	/** Current over the fuse limit burns it out for good. 128 A fuse: passes 2 A; a 1 A fuse: blows, motor stops. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void fuseBlowsOverLimit(GameTestHelper h) {
		DynamometerBlockEntity dyna = chain(h, new Block[] {steel(), ElectriBlocks.FUSE.get(), steel()});
		FuseBlockEntity f = (FuseBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		f.setCurrentLimit(128);
		h.runAtTickTime(100, () -> {
			h.assertTrue(dyna.omega == 48 && dyna.torque == 16 && !f.isOverloaded(), "128 A fuse: got "+dyna.omega+" @ "+dyna.torque);
			f.setCurrentLimit(1);
			f.rebuildNetwork();
		});
		h.runAtTickTime(200, () -> {
			h.assertTrue(f.isOverloaded(), "1 A fuse with 2 A did not blow");
			h.assertTrue(dyna.power == 0, "blown fuse still passes "+dyna.power+" W");
			h.succeed();
		});
	}

	/** The meter in the line reads the 2 A and a voltage between source and motor, and gives comparator signal 1 (power &lt; 1 MW). */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void meterReadsLine(GameTestHelper h) {
		DynamometerBlockEntity dyna = chain(h, new Block[] {steel(), ElectriBlocks.METER.get(), steel()});
		MeterBlockEntity m = (MeterBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		h.succeedWhen(() -> {
			h.assertTrue(dyna.omega == 48, "motor "+dyna.omega+" rad/s");
			h.assertTrue(m.getWireCurrent() == 2, "meter current "+m.getWireCurrent());
			h.assertTrue(m.getWireVoltage() >= 384 && m.getWireVoltage() <= 512, "meter voltage "+m.getWireVoltage());
			h.assertTrue(m.getRedstoneOverride() == 1, "comparator "+m.getRedstoneOverride());
		});
	}

	/** Transformer 1:1 between two steel wires: 448 V in, 448 V and 2 A out (efficiency 1): motor (448-64)/8 = 48 rad/s @ 16 Nm. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void transformerOneToOne(GameTestHelper h) {
		DynamometerBlockEntity dyna = chain(h, new Block[] {steel(), ElectriBlocks.TRANSFORMER.get(), steel()});
		((reika.electricraft.content.blockentity.TransformerBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4))).setFacing(Direction.WEST);
		h.succeedWhen(() -> h.assertTrue(dyna.omega == 48 && dyna.torque == 16, "expected 48 rad/s @ 16 Nm, got "+dyna.omega+" @ "+dyna.torque));
	}

	/**
	 * Step-down 2:1: 448 V -> 224 V, 2 A -> (int)(4 * 2^-0.1) = 3 A; motor speed (224-64)/8 = 20 rad/s. The transformer is a second source
	 * in the network, so the motor without amplifier is held to total current / sources = (2+3)/2 = 2 A (original getEffectiveCurrent):
	 * 16 Nm.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void transformerStepsDown(GameTestHelper h) {
		DynamometerBlockEntity dyna = chain(h, new Block[] {steel(), ElectriBlocks.TRANSFORMER.get(), steel()});
		var t = (reika.electricraft.content.blockentity.TransformerBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		t.setFacing(Direction.WEST);
		t.setRatio(2, 1);
		h.succeedWhen(() -> {
			h.assertTrue(t.getInputVoltage() == 448 && t.getGenVoltage() == 224 && t.getGenCurrent() == 3, "transformer "+t.getInputVoltage()+" V -> "+t.getGenVoltage()+" V, "+t.getGenCurrent()+" A");
			h.assertTrue(dyna.omega == 20 && dyna.torque == 16, "expected 20 rad/s @ 16 Nm, got "+dyna.omega+" @ "+dyna.torque);
		});
	}

	/** More than 4096 A out of a transformer makes it explode: an auroral battery (4096 A) stepped down 2:1 gives 7643 A. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void transformerExplodesOverCurrent(GameTestHelper h) {
		var bat = (reika.electricraft.content.blockentity.BatteryBlockEntity)place(h, ElectriBlocks.battery(reika.electricraft.registry.BatteryType.STAR).get(), 6, 4,
				reika.electricraft.content.blockentity.BatteryBlockEntity.class);
		bat.setStoredEnergy(reika.electricraft.registry.BatteryType.STAR.maxCapacity);
		h.setBlock(new BlockPos(6, 1, 5), Blocks.REDSTONE_BLOCK);
		h.setBlock(new BlockPos(6, 2, 4), ElectriBlocks.getWire(WireType.COPPER, false));
		h.setBlock(new BlockPos(5, 2, 4), ElectriBlocks.TRANSFORMER.get());
		var t = (reika.electricraft.content.blockentity.TransformerBlockEntity)h.getBlockEntity(new BlockPos(5, 2, 4));
		t.setFacing(Direction.WEST);
		t.setRatio(2, 1);
		h.setBlock(new BlockPos(4, 2, 4), ElectriBlocks.getWire(WireType.COPPER, false));
		h.setBlock(new BlockPos(3, 2, 4), ElectriBlocks.MOTOR.get());
		((MotorBlockEntity)h.getBlockEntity(new BlockPos(3, 2, 4))).setFacing(Direction.EAST);
		h.succeedWhen(() -> h.assertBlockNotPresent(ElectriBlocks.TRANSFORMER.get(), new BlockPos(5, 2, 4)));
	}
}
