/*******************************************************************************
 * Regression tests for the ported RotaryCraft power transmission. Expected values follow from the original code
 * (EngineType speeds/torques, gearbox ratio arithmetic, junction merging).
 * Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import net.minecraft.core.Direction;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.block.ShaftBlock;
import reika.rotarycraft.content.block.BedrockSliceBlock;
import reika.rotarycraft.content.blockentity.BedrockBreakerBlockEntity;
import reika.rotarycraft.content.blockentity.BlastFurnaceBlockEntity;
import reika.rotarycraft.content.blockentity.ClutchBlockEntity;
import reika.rotarycraft.content.blockentity.WorktableBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.content.blockentity.FlywheelBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.ShaftBlockEntity;
import reika.rotarycraft.content.blockentity.SplitterBlockEntity;
import reika.rotarycraft.content.blockentity.EngineControllerBlockEntity;
import reika.rotarycraft.content.blockentity.ExtractorBlockEntity;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.FrictionHeaterBlockEntity;
import reika.rotarycraft.content.blockentity.engine.DCEngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.GasEngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.PerformanceEngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PowerTransmissionTests {

	private static final String PLATFORM = "platform_9x4x9";
	private static final String TALL = "platform_9x8x9";

	/** Places a machine and sets its orientation metadata (what the placer item would do). */
	private static <T extends RotaryCraftBlockEntity> T place(GameTestHelper h, Block block, int x, int z, int meta, Class<T> cls) {
		return place(h, block, x, 1, z, meta, cls);
	}

	private static <T extends RotaryCraftBlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (!cls.isInstance(be))
			throw new IllegalStateException("no "+cls.getSimpleName()+" at "+p);
		T te = cls.cast(be);
		te.setBlockMetadata(meta);
		return te;
	}

	private static ShaftBlockEntity shaft(GameTestHelper h, MaterialRegistry mat, int x, int z, int meta) {
		ShaftBlockEntity sha = place(h, RotaryBlocks.SHAFT.get(), x, z, meta, ShaftBlockEntity.class);
		sha.applyComponentsFromItemStack(ShaftBlock.shaftItem(mat, false));
		return sha;
	}

	private static void expect(GameTestHelper h, DynamometerBlockEntity dyna, int omega, int torque) {
		h.succeedWhen(() -> {
			h.assertTrue(dyna.omega == omega && dyna.torque == torque, "expected "+omega+" rad/s @ "+torque+" Nm, got "+dyna.omega+" @ "+dyna.torque);
			h.assertTrue(dyna.power == (long)omega*torque, "power "+dyna.power+" != omega*torque");
		});
	}

	/** DC engine (256 rad/s, 4 Nm) -> steel shaft -> dynamometer, all along -x. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void dcEngineDrivesDynamometer(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 6, 4, 0, EngineBlockEntity.class);
		h.setBlock(new BlockPos(6, 1, 5), Blocks.REDSTONE_BLOCK);
		shaft(h, MaterialRegistry.STEEL, 5, 4, 0);
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 4, 4, 0, DynamometerBlockEntity.class);
		expect(h, dyna, 256, 4);
	}

	/** Without redstone the DC engine stays off. */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void dcEngineNeedsRedstone(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 6, 4, 0, EngineBlockEntity.class);
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 5, 4, 0, DynamometerBlockEntity.class);
		h.runAtTickTime(50, () -> {
			h.assertTrue(dyna.power == 0, "unpowered DC engine produced "+dyna.power+" W");
			h.succeed();
		});
	}

	/** Steel 2:1 gearbox with lubricant halves speed and doubles torque: 128 rad/s @ 8 Nm. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void steelGearboxReduces(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 6, 4, 0, EngineBlockEntity.class);
		h.setBlock(new BlockPos(6, 1, 5), Blocks.REDSTONE_BLOCK);
		GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 5, 4, 0, GearboxBlockEntity.class);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.STEEL, 2));
		gbx.setBlockMetadata(0);
		gbx.fillWithLubricant();
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 4, 4, 0, DynamometerBlockEntity.class);
		expect(h, dyna, 128, 8);
	}

	/** Wooden 4:1 gearbox (no lubricant needed): 64 rad/s @ 16 Nm. Meta = ratio index 1 * 4 + direction 0. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void woodGearboxFourToOne(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 6, 4, 0, EngineBlockEntity.class);
		h.setBlock(new BlockPos(6, 1, 5), Blocks.REDSTONE_BLOCK);
		GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 5, 4, 4, GearboxBlockEntity.class);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.WOOD, 4));
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 4, 4, 0, DynamometerBlockEntity.class);
		expect(h, dyna, 64, 16);
	}

	/** A clutch that needs redstone passes nothing until powered, then the full 256 rad/s @ 4 Nm. */
	@GameTest(template = PLATFORM, timeoutTicks = 160)
	public static void clutchFollowsRedstone(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 6, 4, 0, EngineBlockEntity.class);
		h.setBlock(new BlockPos(6, 1, 5), Blocks.REDSTONE_BLOCK);
		ClutchBlockEntity clu = place(h, RotaryBlocks.CLUTCH.get(), 5, 4, 0, ClutchBlockEntity.class);
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 4, 4, 0, DynamometerBlockEntity.class);
		h.runAtTickTime(50, () -> {
			h.assertTrue(dyna.power == 0, "disengaged clutch passed "+dyna.power+" W");
			h.setBlock(new BlockPos(5, 2, 4), Blocks.REDSTONE_BLOCK);
		});
		h.runAtTickTime(51, () -> expect(h, dyna, 256, 4));
	}

	/** Shaft junction in merge mode (meta 0: inputs east and north, output west) adds the torque of two DC engines. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void junctionMergesTwoEngines(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 6, 4, 0, EngineBlockEntity.class); // writes west
		h.setBlock(new BlockPos(6, 2, 4), Blocks.REDSTONE_BLOCK);
		place(h, RotaryBlocks.DC_ENGINE.get(), 5, 3, 3, EngineBlockEntity.class); // writes south
		h.setBlock(new BlockPos(5, 2, 3), Blocks.REDSTONE_BLOCK);
		place(h, RotaryBlocks.SHAFT_JUNCTION.get(), 5, 4, 0, SplitterBlockEntity.class);
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 4, 4, 0, DynamometerBlockEntity.class);
		expect(h, dyna, 256, 8);
	}

	/** Shaft junction in split mode (meta 8: input west, outputs east and north) with 1:1 mode halves the torque on each side. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void junctionSplitsEvenly(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 3, 4, 1, EngineBlockEntity.class); // writes east
		h.setBlock(new BlockPos(3, 2, 4), Blocks.REDSTONE_BLOCK);
		SplitterBlockEntity spl = place(h, RotaryBlocks.SHAFT_JUNCTION.get(), 4, 4, 8, SplitterBlockEntity.class);
		DynamometerBlockEntity inline = place(h, RotaryBlocks.DYNAMOMETER.get(), 5, 4, 1, DynamometerBlockEntity.class); // reads west
		DynamometerBlockEntity bend = place(h, RotaryBlocks.DYNAMOMETER.get(), 4, 3, 2, DynamometerBlockEntity.class); // meta 2: reads south
		h.succeedWhen(() -> {
			h.assertTrue(inline.omega == 256 && inline.torque == 2, "inline got "+inline.omega+" @ "+inline.torque);
			h.assertTrue(bend.omega == 256 && bend.torque == 2, "bend got "+bend.omega+" @ "+bend.torque+" (junction mode "+spl.getRatioFromMode()+")");
		});
	}

	/** Bevel gears find their route from the neighbours on the first tick and turn the power 90 degrees (east-west to north). */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void bevelGearsTurnPower(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 6, 4, 0, EngineBlockEntity.class); // writes west into the bevel at x=5
		h.setBlock(new BlockPos(6, 2, 4), Blocks.REDSTONE_BLOCK);
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 5, 3, 2, DynamometerBlockEntity.class); // meta 2: reads south
		place(h, RotaryBlocks.BEVEL_GEARS.get(), 5, 4, 0, RotaryCraftBlockEntity.class);
		expect(h, dyna, 256, 4);
	}

	/** A wooden flywheel driven at its minimum torque (4 Nm) slowly spins up and passes the torque on. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void flywheelSpinsUp(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 6, 4, 0, EngineBlockEntity.class);
		h.setBlock(new BlockPos(6, 1, 5), Blocks.REDSTONE_BLOCK);
		FlywheelBlockEntity fly = place(h, RotaryBlocks.FLYWHEEL.get(), 5, 4, 1, FlywheelBlockEntity.class); // meta 1: read east
		h.runAtTickTime(150, () -> {
			h.assertTrue(fly.omega > 0 && fly.omega < 256, "flywheel speed "+fly.omega+" should be rising slowly");
			h.assertTrue(fly.torque == 4, "flywheel torque "+fly.torque);
			h.succeed();
		});
	}

	/** Gasoline engine (512 rad/s @ 128 Nm) runs on ethanol filled from below through the fluid capability. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void gasEngineRunsOnEthanol(GameTestHelper h) {
		EngineBlockEntity eng = place(h, RotaryBlocks.GAS_ENGINE.get(), 6, 4, 0, EngineBlockEntity.class);
		IFluidHandler fh = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, eng.getBlockPos(), Direction.DOWN);
		h.assertTrue(fh != null, "no fluid handler");
		int filled = fh.fill(new FluidStack(RotaryFluids.ETHANOL.get(), 10000), IFluidHandler.FluidAction.EXECUTE);
		h.assertTrue(filled == 10000, "filled "+filled);
		h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, eng.getBlockPos(), Direction.UP).fill(new FluidStack(RotaryFluids.ETHANOL.get(), 1000), IFluidHandler.FluidAction.SIMULATE) == 0, "ethanol accepted from the top");
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 5, 4, 0, DynamometerBlockEntity.class);
		expect(h, dyna, 512, 128);
	}

	/** An unlubricated steel gearbox wears: its damage rises while it runs (original: 1/40 chance per tick after 100 ticks). */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void dryGearboxWears(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 6, 4, 0, EngineBlockEntity.class);
		h.setBlock(new BlockPos(6, 1, 5), Blocks.REDSTONE_BLOCK);
		GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 5, 4, 0, GearboxBlockEntity.class);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.STEEL, 2));
		gbx.setBlockMetadata(0);
		h.succeedWhen(() -> h.assertTrue(gbx.getDamage() > 0, "dry gearbox damage still 0"));
	}

	/**
	 * Hydrokinetic engine (meta 0: water column on its north side) beside a waterfall whose surface is 4 blocks above it.
	 * Original formula: v = 0.92*sqrt(2g*(4^1.5/32)) = 2.04 m/s; omega = (int)(2v) = 4 rad/s; tau = (int)(0.0625*1000*v*v) = 259 Nm.
	 */
	@GameTest(template = TALL, timeoutTicks = 200)
	public static void hydroEngineRunsOnWaterfall(GameTestHelper h) {
		h.setBlock(new BlockPos(6, 6, 3), Blocks.WATER);
		EngineBlockEntity eng = place(h, RotaryBlocks.HYDRO_ENGINE.get(), 6, 3, 4, 0, EngineBlockEntity.class);
		eng.addLubricant(1000);
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 5, 3, 4, 0, DynamometerBlockEntity.class);
		expect(h, dyna, 4, 259);
	}

	/** Without lubricant the hydrokinetic engine does not run. */
	@GameTest(template = TALL, timeoutTicks = 100)
	public static void hydroEngineNeedsLubricant(GameTestHelper h) {
		h.setBlock(new BlockPos(6, 6, 3), Blocks.WATER);
		place(h, RotaryBlocks.HYDRO_ENGINE.get(), 6, 3, 4, 0, EngineBlockEntity.class);
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 5, 3, 4, 0, DynamometerBlockEntity.class);
		h.runAtTickTime(80, () -> {
			h.assertTrue(dyna.power == 0, "dry hydro engine produced "+dyna.power+" W");
			h.succeed();
		});
	}

	/** Gas turbine (meta 0: intake east, output west) with jet fuel from below and a wall in its intake (original choke 0.25). */
	private static JetEngineBlockEntity chokedJet(GameTestHelper h) {
		JetEngineBlockEntity jet = place(h, RotaryBlocks.JET_ENGINE.get(), 6, 4, 0, JetEngineBlockEntity.class);
		h.setBlock(new BlockPos(7, 1, 4), Blocks.COBBLESTONE_WALL);
		IFluidHandler fh = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, jet.getBlockPos(), Direction.DOWN);
		h.assertTrue(fh != null && fh.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 20000), IFluidHandler.FluidAction.EXECUTE) == 20000, "jet fuel not accepted from below");
		return jet;
	}

	/** Choked to 25 %: 65536*0.25 = 16384 rad/s at the full 1024 Nm. */
	@GameTest(template = PLATFORM, timeoutTicks = 500)
	public static void jetEngineChokedByWall(GameTestHelper h) {
		chokedJet(h);
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 5, 4, 0, DynamometerBlockEntity.class);
		expect(h, dyna, 16384, 1024);
	}

	/** Afterburner upgrade applied and switched on: torque doubles (2048 Nm). */
	@GameTest(template = PLATFORM, timeoutTicks = 500)
	public static void jetAfterburnerDoublesTorque(GameTestHelper h) {
		JetEngineBlockEntity jet = chokedJet(h);
		ItemStack up = new ItemStack(RotaryItems.UPGRADE_AFTERBURNER.get());
		h.assertTrue(jet.canUpgradeWith(up), "afterburner upgrade rejected");
		jet.upgrade(up);
		h.assertTrue(!jet.canUpgradeWith(up), "second afterburner accepted");
		jet.setBurnerActive(true);
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 5, 4, 0, DynamometerBlockEntity.class);
		expect(h, dyna, 16384, 2048);
	}

	/** A stone item sucked into the running turbine comes out as cobblestone and causes FOD 4: torque 1024/2^4 = 64 Nm. */
	@GameTest(template = PLATFORM, timeoutTicks = 500)
	public static void jetIngestsStone(GameTestHelper h) {
		JetEngineBlockEntity jet = chokedJet(h);
		h.setBlock(new BlockPos(7, 1, 4), Blocks.AIR);
		h.setBlock(new BlockPos(7, 2, 4), Blocks.COBBLESTONE_WALL); //keep the intake clear but stop the item from flying off
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 5, 4, 0, DynamometerBlockEntity.class);
		h.runAtTickTime(20, () -> {
			h.assertTrue(jet.power > 0, "turbine not running");
			h.spawnItem(Items.STONE, 7.5F, 1.1F, 4.5F);
		});
		h.runAtTickTime(21, () -> h.succeedWhen(() -> {
			h.assertTrue(jet.FOD == 4, "FOD "+jet.FOD);
			h.assertTrue(dyna.torque == 64, "torque "+dyna.torque);
			h.assertItemEntityPresent(Items.COBBLESTONE);
		}));
	}

	/** Gas engine on an ECU set to MEDIUM (speed factor 2), fuelled through the ECU's tank: 256 rad/s @ 128 Nm. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void ecuThrottlesGasEngine(GameTestHelper h) {
		EngineControllerBlockEntity ecu = place(h, RotaryBlocks.ECU.get(), 6, 1, 4, 0, EngineControllerBlockEntity.class);
		place(h, RotaryBlocks.GAS_ENGINE.get(), 6, 2, 4, 0, EngineBlockEntity.class);
		IFluidHandler fh = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, ecu.getBlockPos(), Direction.NORTH);
		h.assertTrue(fh != null && fh.fill(new FluidStack(RotaryFluids.ETHANOL.get(), 3000), IFluidHandler.FluidAction.EXECUTE) == 3000, "ECU did not take ethanol");
		ecu.setSetting(EngineControllerBlockEntity.EngineSettings.MEDIUM.ordinal());
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 5, 2, 4, 0, DynamometerBlockEntity.class);
		expect(h, dyna, 256, 128);
	}

	/** ECU on SHUTDOWN: the engine never starts. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void ecuShutdownStopsEngine(GameTestHelper h) {
		EngineControllerBlockEntity ecu = place(h, RotaryBlocks.ECU.get(), 6, 1, 4, 0, EngineControllerBlockEntity.class);
		EngineBlockEntity eng = place(h, RotaryBlocks.GAS_ENGINE.get(), 6, 2, 4, 0, EngineBlockEntity.class);
		eng.addFuel(10000);
		ecu.setSetting(EngineControllerBlockEntity.EngineSettings.SHUTDOWN.ordinal());
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 5, 2, 4, 0, DynamometerBlockEntity.class);
		h.runAtTickTime(60, () -> {
			h.assertTrue(dyna.power == 0, "engine on a shut-down ECU produced "+dyna.power+" W");
			h.succeed();
		});
	}

	/** Integrated redstone upgrade: the DC engine runs without a redstone signal; a second upgrade is refused. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void dcRedstoneUpgrade(GameTestHelper h) {
		DCEngineBlockEntity dc = place(h, RotaryBlocks.DC_ENGINE.get(), 6, 4, 0, DCEngineBlockEntity.class);
		ItemStack up = new ItemStack(RotaryItems.UPGRADE_REDSTONE.get());
		h.assertTrue(dc.canUpgradeWith(up), "upgrade refused");
		dc.upgrade(up);
		h.assertTrue(!dc.canUpgradeWith(up), "second upgrade accepted");
		DynamometerBlockEntity dyna = place(h, RotaryBlocks.DYNAMOMETER.get(), 5, 4, 0, DynamometerBlockEntity.class);
		expect(h, dyna, 256, 4);
	}

	/** Performance upgrade: the gas engine becomes a performance engine and keeps its fuel and orientation. */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void gasPerformanceUpgrade(GameTestHelper h) {
		GasEngineBlockEntity gas = place(h, RotaryBlocks.GAS_ENGINE.get(), 6, 4, 2, GasEngineBlockEntity.class);
		gas.addFuel(5000);
		ItemStack up = new ItemStack(RotaryItems.UPGRADE_PERFORMANCE.get());
		h.assertTrue(gas.canUpgradeWith(up), "upgrade refused");
		gas.upgrade(up);
		BlockPos p = new BlockPos(6, 1, 4);
		h.assertBlockPresent(RotaryBlocks.SPORT_ENGINE.get(), p);
		h.assertTrue(h.getBlockEntity(p) instanceof PerformanceEngineBlockEntity pe && pe.getFuelLevel() == 5000 && pe.getBlockMetadata() == 2,
				"performance engine did not keep fuel/orientation");
		h.succeed();
	}

	private static ItemStack rc(CraftingPart p, int n) {
		return new ItemStack(RotaryItems.part(p), n);
	}

	/** Blast Furnace at 700 C: coal + gunpowder + sand with 3 iron ingots make 3 HSLA steel; the coal is always used up. */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void blastFurnaceMakesSteel(GameTestHelper h) {
		BlastFurnaceBlockEntity bf = place(h, RotaryBlocks.BLAST_FURNACE.get(), 4, 4, 0, BlastFurnaceBlockEntity.class);
		bf.setTemperature(700);
		var inv = bf.getInventory();
		inv.setStackInSlot(0, new ItemStack(Items.COAL, 10));
		inv.setStackInSlot(11, new ItemStack(Items.GUNPOWDER, 10));
		inv.setStackInSlot(14, new ItemStack(Items.SAND, 10));
		for (int i = 1; i <= 3; i++)
			inv.setStackInSlot(i, new ItemStack(Items.IRON_INGOT));
		h.succeedWhen(() -> {
			h.assertTrue(inv.getStackInSlot(10).is(RotaryItems.part(CraftingPart.STEELINGOT)) && inv.getStackInSlot(10).getCount() == 3, "output "+inv.getStackInSlot(10));
			h.assertTrue(inv.getStackInSlot(1).isEmpty() && inv.getStackInSlot(0).getCount() == 9, "inputs not consumed as expected");
		});
	}

	/** Below 600 C nothing happens. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void blastFurnaceTooCold(GameTestHelper h) {
		BlastFurnaceBlockEntity bf = place(h, RotaryBlocks.BLAST_FURNACE.get(), 4, 4, 0, BlastFurnaceBlockEntity.class);
		bf.setTemperature(300);
		var inv = bf.getInventory();
		inv.setStackInSlot(0, new ItemStack(Items.COAL, 10));
		inv.setStackInSlot(11, new ItemStack(Items.GUNPOWDER, 10));
		inv.setStackInSlot(14, new ItemStack(Items.SAND, 10));
		inv.setStackInSlot(1, new ItemStack(Items.IRON_INGOT));
		h.runAtTickTime(80, () -> {
			h.assertTrue(bf.smeltTime == 0 && inv.getStackInSlot(10).isEmpty(), "cold furnace worked");
			h.succeed();
		});
	}

	/** Blast Furnace crafting at 1200 C: the high-temperature combustor from its 3x3 pattern. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void blastFurnaceCraftsCombustor(GameTestHelper h) {
		BlastFurnaceBlockEntity bf = place(h, RotaryBlocks.BLAST_FURNACE.get(), 4, 4, 0, BlastFurnaceBlockEntity.class);
		bf.setTemperature(1200);
		var inv = bf.getInventory();
		ItemStack[] grid = {rc(CraftingPart.STEELINGOT, 1), rc(CraftingPart.REDGOLDINGOT, 1), rc(CraftingPart.STEELINGOT, 1),
				rc(CraftingPart.REDGOLDINGOT, 1), new ItemStack(Items.REDSTONE), rc(CraftingPart.REDGOLDINGOT, 1),
				rc(CraftingPart.STEELINGOT, 1), rc(CraftingPart.IGNITER, 1), rc(CraftingPart.STEELINGOT, 1)};
		for (int i = 0; i < 9; i++)
			inv.setStackInSlot(i+1, grid[i]);
		h.succeedWhen(() -> {
			h.assertTrue(inv.getStackInSlot(10).is(RotaryItems.part(CraftingPart.HIGHCOMBUSTOR)), "output "+inv.getStackInSlot(10));
			h.assertTrue(inv.getStackInSlot(5).isEmpty(), "grid not consumed");
		});
	}

	/** Worktable crafts a machine on a redstone pulse (machine recipes are Worktable-only by default). */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void worktableCraftsMachine(GameTestHelper h) {
		WorktableBlockEntity wt = place(h, RotaryBlocks.WORKTABLE.get(), 4, 4, 0, WorktableBlockEntity.class);
		var inv = wt.getInventory();
		ItemStack[] grid = {rc(CraftingPart.STEELINGOT, 1), rc(CraftingPart.PCB, 1), rc(CraftingPart.STEELINGOT, 1),
				rc(CraftingPart.STEELINGOT, 1), new ItemStack(Items.GOLD_INGOT), rc(CraftingPart.STEELINGOT, 1),
				rc(CraftingPart.STEELINGOT, 1), new ItemStack(Items.REDSTONE), rc(CraftingPart.STEELINGOT, 1)};
		for (int i = 0; i < 9; i++)
			inv.setStackInSlot(i, grid[i]);
		h.runAtTickTime(5, () -> h.setBlock(new BlockPos(4, 1, 5), Blocks.REDSTONE_BLOCK));
		h.succeedWhen(() -> {
			h.assertTrue(inv.getStackInSlot(WorktableBlockEntity.OUTPUT).is(RotaryBlocks.ECU.get().asItem()), "output "+inv.getStackInSlot(WorktableBlockEntity.OUTPUT));
			h.assertTrue(inv.getStackInSlot(4).isEmpty(), "grid not consumed");
		});
	}

	/** Worktable recycling: a steel 2:1 gear unit gives 48 scrap. */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void worktableRecycles(GameTestHelper h) {
		WorktableBlockEntity wt = place(h, RotaryBlocks.WORKTABLE.get(), 4, 4, 0, WorktableBlockEntity.class);
		wt.getInventory().setStackInSlot(0, new ItemStack(RotaryItems.gearPart(GearboxTypes.STEEL, GearboxTypes.GearPart.UNIT2)));
		h.runAtTickTime(5, () -> h.setBlock(new BlockPos(4, 1, 5), Blocks.REDSTONE_BLOCK));
		h.succeedWhen(() -> h.assertTrue(wt.getInventory().getStackInSlot(9).is(RotaryItems.part(CraftingPart.SCRAP)) && wt.getInventory().getStackInSlot(9).getCount() == 48,
				"scrap "+wt.getInventory().getStackInSlot(9)));
	}

	/** Worktable uncrafting: a gas engine alone in the centre is taken apart into its recipe on a redstone pulse. */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void worktableUncrafts(GameTestHelper h) {
		WorktableBlockEntity wt = place(h, RotaryBlocks.WORKTABLE.get(), 4, 4, 0, WorktableBlockEntity.class);
		var inv = wt.getInventory();
		inv.setStackInSlot(4, new ItemStack(RotaryBlocks.GAS_ENGINE.get()));
		h.runAtTickTime(5, () -> h.setBlock(new BlockPos(4, 1, 5), Blocks.REDSTONE_BLOCK));
		h.succeedWhen(() -> {
			h.assertTrue(inv.getStackInSlot(4).isEmpty(), "engine still there");
			//CgC / SGs / PIP
			h.assertTrue(inv.getStackInSlot(9).is(RotaryItems.part(CraftingPart.CYLINDER)) && inv.getStackInSlot(10).is(Items.GOLD_INGOT)
					&& inv.getStackInSlot(13).is(RotaryItems.gearPart(GearboxTypes.STEEL, GearboxTypes.GearPart.UNIT2))
					&& inv.getStackInSlot(16).is(RotaryItems.part(CraftingPart.IMPELLER)), "parts "+inv.getStackInSlot(9)+" "+inv.getStackInSlot(10)+" "+inv.getStackInSlot(13)+" "+inv.getStackInSlot(16));
		});
	}

	/** Gas engine (512 rad/s @ 128 Nm) -> Friction Heater (meta 0: reads east, heats west). */
	private static FrictionHeaterBlockEntity poweredHeater(GameTestHelper h) {
		EngineBlockEntity eng = place(h, RotaryBlocks.GAS_ENGINE.get(), 6, 4, 0, EngineBlockEntity.class);
		eng.addFuel(20000);
		return place(h, RotaryBlocks.FRICTION_HEATER.get(), 5, 4, 0, FrictionHeaterBlockEntity.class);
	}

	/**
	 * Original heating: every 20 ticks +3*log2(512)*log2(128) = 189, then -(T-30)/5, settling near 786 C, which the heater passes to the
	 * Blast Furnace on its front - more than lava can give it (about 620 C).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 700)
	public static void frictionHeaterHeatsBlastFurnace(GameTestHelper h) {
		FrictionHeaterBlockEntity heater = poweredHeater(h);
		BlastFurnaceBlockEntity bf = place(h, RotaryBlocks.BLAST_FURNACE.get(), 4, 4, 0, BlastFurnaceBlockEntity.class);
		h.succeedWhen(() -> {
			h.assertTrue(heater.isActive(), "heater not powered: "+heater.omega+" rad/s @ "+heater.torque+" Nm");
			h.assertTrue(heater.getTemperature() > 750 && heater.getTemperature() <= 790, "heater at "+heater.getTemperature()+" C");
			h.assertTrue(bf.getTemperature() > 700, "blast furnace at "+bf.getTemperature()+" C");
		});
	}

	/** Without shaft power the heater stays at ambient temperature. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void frictionHeaterNeedsPower(GameTestHelper h) {
		FrictionHeaterBlockEntity heater = place(h, RotaryBlocks.FRICTION_HEATER.get(), 5, 4, 0, FrictionHeaterBlockEntity.class);
		place(h, RotaryBlocks.BLAST_FURNACE.get(), 4, 4, 0, BlastFurnaceBlockEntity.class);
		h.runAtTickTime(150, () -> {
			h.assertTrue(!heater.isActive() && heater.getTemperature() < 100, "unpowered heater at "+heater.getTemperature()+" C");
			h.succeed();
		});
	}

	/** The heater keeps a vanilla furnace lit without fuel and smelts raw iron with its own (faster) timer. */
	@GameTest(template = PLATFORM, timeoutTicks = 700)
	public static void frictionHeaterDrivesFurnace(GameTestHelper h) {
		poweredHeater(h);
		BlockPos fp = new BlockPos(4, 1, 4);
		h.setBlock(fp, Blocks.FURNACE);
		net.minecraft.world.level.block.entity.FurnaceBlockEntity furn = (net.minecraft.world.level.block.entity.FurnaceBlockEntity)h.getBlockEntity(fp);
		furn.setItem(0, new ItemStack(Items.RAW_IRON, 4));
		h.succeedWhen(() -> h.assertTrue(furn.getItem(2).is(Items.IRON_INGOT), "furnace output "+furn.getItem(2)+", input "+furn.getItem(0)));
	}

	/**
	 * Two performance engines (1024 rad/s @ 256 Nm each, with additives and water) merged in a shaft junction -> 1024 rad/s @ 512 Nm ->
	 * Friction Heater (x=4) -> Blast Furnace (x=3). Original heating: +3*log2(1024)*log2(512) = 270 per 20 ticks, -(T-30)/5, so the heater
	 * settles at 1110 C (step-by-step simulation of the integer arithmetic: 1100 C after 20 steps).
	 */
	private static FrictionHeaterBlockEntity twoEngineHeater(GameTestHelper h) {
		for (int[] e : new int[][] {{6, 4, 0}, {5, 3, 3}}) {
			PerformanceEngineBlockEntity eng = place(h, RotaryBlocks.SPORT_ENGINE.get(), e[0], e[1], e[2], PerformanceEngineBlockEntity.class);
			eng.addFuel(50000);
			eng.addWater(50000);
			eng.additives = 200;
		}
		place(h, RotaryBlocks.SHAFT_JUNCTION.get(), 5, 4, 0, SplitterBlockEntity.class);
		return place(h, RotaryBlocks.FRICTION_HEATER.get(), 4, 4, 0, FrictionHeaterBlockEntity.class);
	}

	/** 1100 C at the heater with combined engines; the Blast Furnace follows and makes spring steel (1000 C recipe). */
	@GameTest(template = PLATFORM, timeoutTicks = 1200)
	public static void frictionHeaterReaches1100WithTwoEngines(GameTestHelper h) {
		FrictionHeaterBlockEntity heater = twoEngineHeater(h);
		BlastFurnaceBlockEntity bf = place(h, RotaryBlocks.BLAST_FURNACE.get(), 3, 4, 0, BlastFurnaceBlockEntity.class);
		var inv = bf.getInventory();
		inv.setStackInSlot(0, rc(CraftingPart.COKE, 16));
		inv.setStackInSlot(11, new ItemStack(Items.REDSTONE, 16));
		inv.setStackInSlot(1, rc(CraftingPart.STEELINGOT, 1));
		h.succeedWhen(() -> {
			h.assertTrue(heater.omega == 1024 && heater.torque == 512, "heater input "+heater.omega+" rad/s @ "+heater.torque+" Nm");
			h.assertTrue(heater.getTemperature() >= 1100 && heater.getTemperature() <= 1110, "heater at "+heater.getTemperature()+" C");
			h.assertTrue(bf.getTemperature() >= 1100, "blast furnace at "+bf.getTemperature()+" C");
			h.assertTrue(inv.getStackInSlot(10).is(RotaryItems.part(CraftingPart.SPRINGINGOT)), "output "+inv.getStackInSlot(10));
		});
	}

	/** The same rig makes the high-temperature combustor (1100 C Blast Furnace crafting), which the turbines need. */
	@GameTest(template = PLATFORM, timeoutTicks = 1200)
	public static void frictionHeaterMakesHighCombustor(GameTestHelper h) {
		twoEngineHeater(h);
		BlastFurnaceBlockEntity bf = place(h, RotaryBlocks.BLAST_FURNACE.get(), 3, 4, 0, BlastFurnaceBlockEntity.class);
		var inv = bf.getInventory();
		ItemStack[] grid = {rc(CraftingPart.STEELINGOT, 1), rc(CraftingPart.REDGOLDINGOT, 1), rc(CraftingPart.STEELINGOT, 1),
				rc(CraftingPart.REDGOLDINGOT, 1), new ItemStack(Items.REDSTONE), rc(CraftingPart.REDGOLDINGOT, 1),
				rc(CraftingPart.STEELINGOT, 1), rc(CraftingPart.IGNITER, 1), rc(CraftingPart.STEELINGOT, 1)};
		for (int i = 0; i < 9; i++)
			inv.setStackInSlot(i+1, grid[i]);
		h.succeedWhen(() -> h.assertTrue(inv.getStackInSlot(10).is(RotaryItems.part(CraftingPart.HIGHCOMBUSTOR)),
				"output "+inv.getStackInSlot(10)+" at "+bf.getTemperature()+" C"));
	}

	/**
	 * Gas turbine choked to 16384 rad/s @ 1024 Nm (wall in the intake) -> three bedrock shafts (so its exhaust heat, which reaches 3 blocks,
	 * does not touch the furnace) -> Friction Heater (x=3) -> Blast Furnace (x=2). Heater equilibrium: 1710 C.
	 */
	private static FrictionHeaterBlockEntity turbineHeater(GameTestHelper h) {
		JetEngineBlockEntity jet = place(h, RotaryBlocks.JET_ENGINE.get(), 7, 4, 0, JetEngineBlockEntity.class);
		h.setBlock(new BlockPos(8, 1, 4), Blocks.COBBLESTONE_WALL);
		jet.addFuel(100000);
		for (int x = 6; x >= 4; x--)
			shaft(h, MaterialRegistry.BEDROCK, x, 4, 0);
		return place(h, RotaryBlocks.FRICTION_HEATER.get(), 3, 4, 0, FrictionHeaterBlockEntity.class);
	}

	/**
	 * Survival route to tungsten: two microturbines (131072 rad/s @ 16 Nm each, jet fuel), each geared down 16:1 by a bedrock gearbox,
	 * merged in a shaft junction (8192 rad/s @ 512 Nm), geared down 4:1 -> 2048 rad/s @ 2048 Nm -> Friction Heater (x=3) -> vanilla
	 * furnace (x=2) with tungsten flakes. Heater equilibrium 12*log2(2048)^2+30 = 1482 C, above the 1350 C of the tungsten recipe.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 2400)
	public static void microturbinesSmeltTungsten(GameTestHelper h) {
		EngineBlockEntity a = place(h, RotaryBlocks.MICRO_ENGINE.get(), 7, 4, 0, EngineBlockEntity.class);
		EngineBlockEntity b = place(h, RotaryBlocks.MICRO_ENGINE.get(), 5, 2, 3, EngineBlockEntity.class);
		a.addFuel(100000);
		b.addFuel(100000);
		for (int[] g : new int[][] {{6, 4, 12}, {5, 3, 15}, {4, 4, 4}}) {
			int ratio = g[2] >= 12 ? 16 : 4;
			GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), g[0], g[1], g[2], GearboxBlockEntity.class);
			gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, ratio));
			gbx.setBlockMetadata(g[2]);
			gbx.reduction = true;
		}
		place(h, RotaryBlocks.SHAFT_JUNCTION.get(), 5, 4, 0, SplitterBlockEntity.class);
		FrictionHeaterBlockEntity heater = place(h, RotaryBlocks.FRICTION_HEATER.get(), 3, 4, 0, FrictionHeaterBlockEntity.class);
		BlockPos fp = new BlockPos(2, 1, 4);
		h.setBlock(fp, Blocks.FURNACE);
		net.minecraft.world.level.block.entity.FurnaceBlockEntity furn = (net.minecraft.world.level.block.entity.FurnaceBlockEntity)h.getBlockEntity(fp);
		furn.setItem(0, rc(CraftingPart.TUNGSTENFLAKES, 2));
		h.succeedWhen(() -> {
			h.assertTrue(heater.omega == 2048 && heater.torque == 2048, "heater input "+heater.omega+" rad/s @ "+heater.torque+" Nm");
			h.assertTrue(heater.getTemperature() >= 1350 && heater.getTemperature() <= 1482, "heater at "+heater.getTemperature()+" C");
			h.assertTrue(furn.getItem(2).is(RotaryItems.part(CraftingPart.TUNGSTENINGOT)), "furnace output "+furn.getItem(2)+" at "+heater.getTemperature()+" C");
		});
	}

	/**
	 * Survival route to bedrock dust: the same two microturbines (2 x 2.1 MW), geared 16:1 each, merged, then 16:1 and 2:1 ->
	 * 256 rad/s @ 16384 Nm = 4194304 W, exactly the Bedrock Breaker's minimum (meta 1: reads east, digs west).
	 * At 256 rad/s a step takes max(1, 600-30*log2(257)) = 359 ticks. The turbines spool up slowly (still ~83 % after 1600 ticks), and only
	 * full speed reaches the minimum power.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 4000)
	public static void microturbinesRunBedrockBreaker(GameTestHelper h) {
		EngineBlockEntity a = place(h, RotaryBlocks.MICRO_ENGINE.get(), 7, 4, 0, EngineBlockEntity.class);
		EngineBlockEntity b = place(h, RotaryBlocks.MICRO_ENGINE.get(), 5, 2, 3, EngineBlockEntity.class);
		a.addFuel(100000);
		b.addFuel(100000);
		java.util.List<GearboxBlockEntity> boxes = new java.util.ArrayList<>();
		for (int[] g : new int[][] {{6, 4, 12, 16}, {5, 3, 15, 16}, {4, 4, 12, 16}, {3, 4, 0, 2}}) {
			GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), g[0], g[1], g[2], GearboxBlockEntity.class);
			gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, g[3]));
			gbx.setBlockMetadata(g[2]);
			gbx.reduction = true;
			boxes.add(gbx);
		}
		place(h, RotaryBlocks.SHAFT_JUNCTION.get(), 5, 4, 0, SplitterBlockEntity.class);
		BedrockBreakerBlockEntity bb = place(h, RotaryBlocks.BEDROCK_BREAKER.get(), 2, 4, 1, BedrockBreakerBlockEntity.class);
		BlockPos head = new BlockPos(1, 1, 4);
		h.setBlock(head, Blocks.BEDROCK);
		h.succeedWhen(() -> {
			StringBuilder sb = new StringBuilder();
			for (GearboxBlockEntity g : boxes)
				sb.append(" | ").append(g.omega).append("@").append(g.torque);
			h.assertTrue(bb.omega == 256 && bb.torque == 16384, "breaker input "+bb.omega+" rad/s @ "+bb.torque+" Nm, engines "+a.omega+"/"+b.omega+sb);
			BlockState st = h.getBlockState(head);
			h.assertTrue(st.is(RotaryBlocks.BEDROCK_SLICE.get()) && st.getValue(BedrockSliceBlock.LEVEL) >= 1, "head block "+st);
		});
	}

	/** Bedrock alloy (1450 C): steel ingot + 4 bedrock dust -> bedrock ingot. */
	@GameTest(template = PLATFORM, timeoutTicks = 1200)
	public static void turbineHeaterMakesBedrockAlloy(GameTestHelper h) {
		FrictionHeaterBlockEntity heater = turbineHeater(h);
		BlastFurnaceBlockEntity bf = place(h, RotaryBlocks.BLAST_FURNACE.get(), 2, 4, 0, BlastFurnaceBlockEntity.class);
		var inv = bf.getInventory();
		inv.setStackInSlot(0, rc(CraftingPart.BEDROCKDUST, 8));
		inv.setStackInSlot(1, rc(CraftingPart.STEELINGOT, 1));
		h.succeedWhen(() -> {
			h.assertTrue(heater.getTemperature() >= 1450, "heater at "+heater.getTemperature()+" C ("+heater.omega+" rad/s @ "+heater.torque+" Nm)");
			h.assertTrue(inv.getStackInSlot(10).is(RotaryItems.part(CraftingPart.BEDINGOT)), "output "+inv.getStackInSlot(10)+" at "+bf.getTemperature()+" C");
			h.assertTrue(inv.getStackInSlot(0).getCount() == 4, "bedrock dust left: "+inv.getStackInSlot(0).getCount());
		});
	}

	/** Diamond gears (1400 C Blast Furnace crafting): 4 diamonds around a tungsten gear; medium difficulty gives 6. */
	@GameTest(template = PLATFORM, timeoutTicks = 1200)
	public static void turbineHeaterMakesDiamondGears(GameTestHelper h) {
		turbineHeater(h);
		BlastFurnaceBlockEntity bf = place(h, RotaryBlocks.BLAST_FURNACE.get(), 2, 4, 0, BlastFurnaceBlockEntity.class);
		var inv = bf.getInventory();
		inv.setStackInSlot(2, new ItemStack(Items.DIAMOND));
		inv.setStackInSlot(4, new ItemStack(Items.DIAMOND));
		inv.setStackInSlot(5, new ItemStack(RotaryItems.gearPart(GearboxTypes.TUNGSTEN, GearboxTypes.GearPart.GEAR)));
		inv.setStackInSlot(6, new ItemStack(Items.DIAMOND));
		inv.setStackInSlot(8, new ItemStack(Items.DIAMOND));
		h.succeedWhen(() -> h.assertTrue(inv.getStackInSlot(10).is(RotaryItems.gearPart(GearboxTypes.DIAMOND, GearboxTypes.GearPart.GEAR)) && inv.getStackInSlot(10).getCount() == 6,
				"output "+inv.getStackInSlot(10)+" at "+bf.getTemperature()+" C"));
	}

	/**
	 * Eight gas engines (512 rad/s @ 128 Nm each) in two lines of four (the original "engine spam" rule allows at most 4 equal engines
	 * through ordinary junctions), joined by one junction at (4,1,4) that feeds the Friction Heater (3,1,4) and Blast Furnace (2,1,4).
	 * Line A runs south along x=4 (meta 7 junctions, side engines from the east); line B runs west along z=4 (meta 4 junctions, side
	 * engines from the south).
	 */
	private static SplitterBlockEntity eightEngineRig(GameTestHelper h, boolean bedrockJoin) {
		int[][] engines = {{4, 0, 3}, {5, 1, 0}, {5, 2, 0}, {5, 3, 0}, {8, 4, 0}, {7, 5, 2}, {6, 5, 2}, {5, 5, 2}}; //x, z, meta
		for (int[] e : engines)
			place(h, RotaryBlocks.GAS_ENGINE.get(), e[0], e[1], e[2], EngineBlockEntity.class).addFuel(50000);
		for (int z = 1; z <= 3; z++)
			place(h, RotaryBlocks.SHAFT_JUNCTION.get(), 4, z, 7, SplitterBlockEntity.class);
		for (int x = 5; x <= 7; x++)
			place(h, RotaryBlocks.SHAFT_JUNCTION.get(), x, 4, 4, SplitterBlockEntity.class);
		SplitterBlockEntity join = place(h, RotaryBlocks.SHAFT_JUNCTION.get(), 4, 4, 0, SplitterBlockEntity.class);
		if (bedrockJoin)
			join.setBedrock();
		return join;
	}

	/** With a bedrock junction joining the two lines: 512 rad/s @ 1024 Nm at the heater, 9*10 like the two-engine rig, so 1100-1110 C. */
	@GameTest(template = PLATFORM, timeoutTicks = 1200)
	public static void eightGasEnginesReach1100(GameTestHelper h) {
		eightEngineRig(h, true);
		FrictionHeaterBlockEntity heater = place(h, RotaryBlocks.FRICTION_HEATER.get(), 3, 4, 0, FrictionHeaterBlockEntity.class);
		BlastFurnaceBlockEntity bf = place(h, RotaryBlocks.BLAST_FURNACE.get(), 2, 4, 0, BlastFurnaceBlockEntity.class);
		h.succeedWhen(() -> {
			h.assertTrue(heater.omega == 512 && heater.torque == 1024, "heater input "+heater.omega+" rad/s @ "+heater.torque+" Nm");
			h.assertTrue(heater.getTemperature() >= 1100 && heater.getTemperature() <= 1110, "heater at "+heater.getTemperature()+" C");
			h.assertTrue(bf.getTemperature() >= 1100, "blast furnace at "+bf.getTemperature()+" C");
		});
	}

	/** An ordinary junction refuses to combine the eight engines (engine spam), sparks and breaks; the heater gets nothing. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void eightGasEnginesNeedBedrockJunction(GameTestHelper h) {
		SplitterBlockEntity join = eightEngineRig(h, false);
		FrictionHeaterBlockEntity heater = place(h, RotaryBlocks.FRICTION_HEATER.get(), 3, 4, 0, FrictionHeaterBlockEntity.class);
		h.runAtTickTime(150, () -> {
			h.assertTrue(!heater.isActive() && heater.torque < 1024, "heater got "+heater.omega+" rad/s @ "+heater.torque+" Nm through an ordinary junction");
			h.succeed();
		});
	}

	/**
	 * Gas turbine choked to 16384 rad/s @ 1024 Nm (z=7, wall in the intake at z=8), three bedrock shafts (out of the exhaust's 3-block heat
	 * range), bedrock 16:1 gearbox (z=3) -> 1024 rad/s @ 16384 Nm (16.8 MW) into the Bedrock Breaker (z=2, meta 3: reads south, digs north).
	 * At 1024 rad/s each step takes max(1, 600-30*log2(1025)) = 299 ticks.
	 */
	private static BedrockBreakerBlockEntity poweredBreaker(GameTestHelper h) {
		JetEngineBlockEntity jet = place(h, RotaryBlocks.JET_ENGINE.get(), 4, 7, 2, JetEngineBlockEntity.class);
		h.setBlock(new BlockPos(4, 1, 8), Blocks.COBBLESTONE_WALL);
		jet.addFuel(100000);
		for (int z = 6; z >= 4; z--)
			shaft(h, MaterialRegistry.BEDROCK, 4, z, 2);
		GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 4, 3, 14, GearboxBlockEntity.class);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16));
		gbx.setBlockMetadata(14);
		gbx.fillWithLubricant();
		return place(h, RotaryBlocks.BEDROCK_BREAKER.get(), 4, 2, 3, BedrockBreakerBlockEntity.class);
	}

	/** Bedrock at the head becomes a bedrock slice (cut from the side facing the machine), which is then ground level by level. */
	@GameTest(template = PLATFORM, timeoutTicks = 1300)
	public static void bedrockBreakerGrindsBedrock(GameTestHelper h) {
		BedrockBreakerBlockEntity bb = poweredBreaker(h);
		BlockPos head = new BlockPos(4, 1, 1);
		h.setBlock(head, Blocks.BEDROCK);
		h.succeedWhen(() -> {
			h.assertTrue(bb.omega == 1024 && bb.torque == 16384, "breaker input "+bb.omega+" rad/s @ "+bb.torque+" Nm");
			BlockState st = h.getBlockState(head);
			h.assertTrue(st.is(RotaryBlocks.BEDROCK_SLICE.get()), "head block "+st);
			h.assertTrue(st.getValue(BedrockSliceBlock.FACING) == Direction.SOUTH, "slice cut from "+st.getValue(BedrockSliceBlock.FACING));
			h.assertTrue(st.getValue(BedrockSliceBlock.LEVEL) >= 1, "slice level "+st.getValue(BedrockSliceBlock.LEVEL));
		});
	}

	/** A fully ground slice (level 15) turns into bedrock dust: 2 at medium difficulty, into the adjacent chest. */
	@GameTest(template = PLATFORM, timeoutTicks = 1000)
	public static void bedrockBreakerMakesDust(GameTestHelper h) {
		poweredBreaker(h);
		BlockPos head = new BlockPos(4, 1, 1);
		h.setBlock(head, RotaryBlocks.BEDROCK_SLICE.get().defaultBlockState().setValue(BedrockSliceBlock.FACING, Direction.SOUTH).setValue(BedrockSliceBlock.LEVEL, 15));
		BlockPos chest = new BlockPos(5, 1, 2);
		h.setBlock(chest, Blocks.CHEST);
		h.succeedWhen(() -> {
			h.assertBlockPresent(Blocks.AIR, head);
			var c = (net.minecraft.world.level.block.entity.ChestBlockEntity)h.getBlockEntity(chest);
			h.assertTrue(c.getItem(0).is(RotaryItems.part(CraftingPart.BEDROCKDUST)) && c.getItem(0).getCount() == 2, "chest has "+c.getItem(0));
		});
	}

	/** Ordinary blocks in the way are destroyed without drops and the head moves on. */
	@GameTest(template = PLATFORM, timeoutTicks = 1000)
	public static void bedrockBreakerClearsBlocks(GameTestHelper h) {
		BedrockBreakerBlockEntity bb = poweredBreaker(h);
		h.setBlock(new BlockPos(4, 1, 1), Blocks.STONE);
		h.succeedWhen(() -> {
			h.assertBlockPresent(Blocks.AIR, new BlockPos(4, 1, 1));
			h.assertItemEntityNotPresent(Items.COBBLESTONE);
			h.assertItemEntityNotPresent(Items.STONE);
			h.assertTrue(bb.getStep() == 2, "step "+bb.getStep());
		});
	}

	/** One gas engine (65 kW, 128 Nm) is far below 16384 Nm / 4.2 MW: the bedrock stays. */
	@GameTest(template = PLATFORM, timeoutTicks = 700)
	public static void bedrockBreakerNeedsPower(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 4, 3, 2, EngineBlockEntity.class).addFuel(20000);
		BedrockBreakerBlockEntity bb = place(h, RotaryBlocks.BEDROCK_BREAKER.get(), 4, 2, 3, BedrockBreakerBlockEntity.class);
		h.setBlock(new BlockPos(4, 1, 1), Blocks.BEDROCK);
		h.runAtTickTime(650, () -> {
			h.assertTrue(bb.torque == 128, "breaker input "+bb.torque+" Nm");
			h.assertBlockPresent(Blocks.BEDROCK, new BlockPos(4, 1, 1));
			h.succeed();
		});
	}

	/**
	 * Extractor at (4,2,4), powered from below: gas engine (6,1,4) -> steel gearbox (5,1,4) -> bevel gears (4,1,4) turning east->up.
	 * ratio > 0: reduction (more torque), ratio < 0: speed-up.
	 */
	private static ExtractorBlockEntity extractorRig(GameTestHelper h, int ratio) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 6, 4, 0, EngineBlockEntity.class).addFuel(50000);
		int r = Math.abs(ratio);
		int idx = r == 2 ? 0 : r == 4 ? 1 : r == 8 ? 2 : 3;
		GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 5, 4, idx*4, GearboxBlockEntity.class);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.STEEL, r));
		gbx.setBlockMetadata(idx*4);
		gbx.fillWithLubricant();
		gbx.reduction = ratio > 0;
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		h.assertTrue(bev.direction >= 0, "no east->up bevel route");
		ExtractorBlockEntity ex = place(h, RotaryBlocks.EXTRACTOR.get(), 4, 2, 4, 0, ExtractorBlockEntity.class);
		ex.addWater(16000);
		return ex;
	}

	/** Stage 1 (ore -> dust) needs 512 Nm: 4:1 reduction gives 128 rad/s @ 512 Nm. Iron ore -> 1-2 iron dust, moved on to the dust input. */
	@GameTest(template = PLATFORM, timeoutTicks = 800)
	public static void extractorStageOne(GameTestHelper h) {
		ExtractorBlockEntity ex = extractorRig(h, 4);
		ex.getInventory().setStackInSlot(0, new ItemStack(Items.DEEPSLATE_IRON_ORE));
		h.succeedWhen(() -> {
			h.assertTrue(ex.omega == 128 && ex.torque == 512, "extractor input "+ex.omega+" rad/s @ "+ex.torque+" Nm");
			ItemStack dust = ex.getInventory().getStackInSlot(1);
			h.assertTrue(ex.getInventory().getStackInSlot(0).isEmpty() && dust.is(RotaryItems.part(CraftingPart.IRONOREDUST)) && dust.getCount() >= 1 && dust.getCount() <= 2,
					"dust slot "+dust);
		});
	}

	/** Stage 2 (dust -> slurry) needs 2048 rad/s: 4:1 speed-up; uses 125 mB water. */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void extractorStageTwo(GameTestHelper h) {
		ExtractorBlockEntity ex = extractorRig(h, -4);
		ex.getInventory().setStackInSlot(1, rc(CraftingPart.GOLDOREDUST, 1));
		h.succeedWhen(() -> {
			h.assertTrue(ex.omega == 2048, "extractor speed "+ex.omega);
			h.assertTrue(ex.getInventory().getStackInSlot(2).is(RotaryItems.part(CraftingPart.GOLDORESLURRY)), "slurry slot "+ex.getInventory().getStackInSlot(2));
			h.assertTrue(ex.getWater() == 16000-125, "water "+ex.getWater());
		});
	}

	/** Stage 3 (slurry -> solution) needs 8192 rad/s: 16:1 speed-up. */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void extractorStageThree(GameTestHelper h) {
		ExtractorBlockEntity ex = extractorRig(h, -16);
		ex.getInventory().setStackInSlot(2, rc(CraftingPart.DIAMONDORESLURRY, 1));
		h.succeedWhen(() -> h.assertTrue(ex.getInventory().getStackInSlot(3).is(RotaryItems.part(CraftingPart.DIAMONDSOLUTION)),
				"solution slot "+ex.getInventory().getStackInSlot(3)+" at "+ex.omega+" rad/s"));
	}

	/** Stage 4 (solution -> flakes) needs 256 Nm; flakes then smelt in a furnace. */
	@GameTest(template = PLATFORM, timeoutTicks = 900)
	public static void extractorStageFour(GameTestHelper h) {
		ExtractorBlockEntity ex = extractorRig(h, 4);
		ex.getInventory().setStackInSlot(3, rc(CraftingPart.IRONSOLUTION, 1));
		h.succeedWhen(() -> {
			ItemStack f = ex.getInventory().getStackInSlot(7);
			h.assertTrue(f.is(RotaryItems.part(CraftingPart.IRONOREFLAKES)) && f.getCount() >= 1 && f.getCount() <= 2, "flakes slot "+f);
			h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
					new net.minecraft.world.item.crafting.SingleRecipeInput(f), h.getLevel()).map(r -> r.value().getResultItem(h.getLevel().registryAccess()).is(Items.IRON_INGOT)).orElse(false),
					"iron flakes do not smelt to iron");
		});
	}

	/** Not enough torque for stage 1 with the gas engine alone (128 Nm < 512 Nm). */
	@GameTest(template = PLATFORM, timeoutTicks = 700)
	public static void extractorNeedsTorque(GameTestHelper h) {
		ExtractorBlockEntity ex = extractorRig(h, -2);
		ex.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_ORE));
		h.runAtTickTime(650, () -> {
			h.assertTrue(ex.getInventory().getStackInSlot(0).is(Items.IRON_ORE) && ex.getCookTime(0) == 0, "stage 1 ran at "+ex.torque+" Nm");
			h.succeed();
		});
	}
}
