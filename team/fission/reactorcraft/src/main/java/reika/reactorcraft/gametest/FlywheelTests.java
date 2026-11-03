/*******************************************************************************
 * Tests for the ported ReactorCraft turbine flywheel (original TileEntityReactorFlywheel and BlockFlywheelMulti). Private NeoForge
 * 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
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

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.block.FlywheelMultiBlock;
import reika.reactorcraft.powergen.ReactorFlywheelBlockEntity;
import reika.reactorcraft.powergen.TurbineCoreBlockEntity;
import reika.reactorcraft.powergen.WorkingFluid;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FlywheelTests {

	private static final String TALL = "platform_9x8x9";

	private static final int X = 1;
	private static final int CY = 3;
	private static final int CZ = 4;

	private static Block part(int variant) {
		return switch (variant) {
			case 0 -> ReactorBlocks.FLYWHEEL_CORE.get();
			case 1 -> ReactorBlocks.FLYWHEEL_DAMPENER.get();
			default -> ReactorBlocks.FLYWHEEL_FRAME.get();
		};
	}

	/** The flywheel at (1, 3, 4) facing east (metadata 0) and its 20-block ring in the x=1 plane (5x5 without corners). */
	private static ReactorFlywheelBlockEntity buildFlywheel(GameTestHelper h) {
		h.setBlock(new BlockPos(X, CY, CZ), ReactorBlocks.FLYWHEEL.get());
		ReactorFlywheelBlockEntity fw = (ReactorFlywheelBlockEntity)h.getBlockEntity(new BlockPos(X, CY, CZ));
		fw.setBlockMetadata(0);
		for (int dy = -2; dy <= 2; dy++) {
			for (int dz = -2; dz <= 2; dz++) {
				int a = Math.abs(dy), b = Math.abs(dz);
				if ((a == 0 && b == 0) || (a == 2 && b == 2))
					continue;
				int v = a+b == 1 ? 0 : a == 1 && b == 1 ? 1 : 2;
				h.setBlock(new BlockPos(X, CY+dy, CZ+dz), part(v));
			}
		}
		return fw;
	}

	/** A steam turbine east of the flywheel with its steam moving west (output into the flywheel), over a loaded steam grate. */
	private static TurbineCoreBlockEntity turbine(GameTestHelper h) {
		h.setBlock(new BlockPos(X+1, CY-2, CZ), ReactorBlocks.STEAM_GRATE.get());
		BlockEntity grate = h.getBlockEntity(new BlockPos(X+1, CY-2, CZ));
		CompoundTag t = grate.saveWithoutMetadata(h.getLevel().registryAccess());
		t.putInt("energy", 100000);
		WorkingFluid.WATER.saveToNBT(t);
		grate.loadWithComponents(t, h.getLevel().registryAccess());
		h.setBlock(new BlockPos(X+1, CY, CZ), ReactorBlocks.TURBINE_CORE.get());
		TurbineCoreBlockEntity te = (TurbineCoreBlockEntity)h.getBlockEntity(new BlockPos(X+1, CY, CZ));
		te.setBlockMetadata(0);
		te.addLubricant(10000);
		return te;
	}

	private static DynamometerBlockEntity dynamometer(GameTestHelper h) {
		h.setBlock(new BlockPos(X-1, CY, CZ), RotaryBlocks.DYNAMOMETER.get());
		DynamometerBlockEntity d = (DynamometerBlockEntity)h.getBlockEntity(new BlockPos(X-1, CY, CZ));
		d.setBlockMetadata(0);
		return d;
	}

	private static BlockPos abs(GameTestHelper h, int dy, int dz) {
		return h.absolutePos(new BlockPos(X, CY+dy, CZ+dz));
	}

	/**
	 * The ring forms from any of its parts once all 20 are there and the flywheel sits in the middle; a dampener in place of a core does
	 * not match; taking out a part un-forms it.
	 */
	@GameTest(template = TALL, timeoutTicks = 40)
	public static void ringFormsAndBreaks(GameTestHelper h) {
		ReactorFlywheelBlockEntity fw = buildFlywheel(h);
		h.setBlock(new BlockPos(X, CY+1, CZ), part(1));
		h.assertFalse(FlywheelMultiBlock.checkForFullMultiBlock(h.getLevel(), abs(h, 2, 1)), "matched with a wrong part");
		h.setBlock(new BlockPos(X, CY+1, CZ), part(0));
		h.assertTrue(FlywheelMultiBlock.checkForFullMultiBlock(h.getLevel(), abs(h, 2, 1)), "ring does not match");
		FlywheelMultiBlock.onCreateFullMultiBlock(h.getLevel(), abs(h, 2, 1));
		h.assertTrue(fw.hasMultiBlock() && h.getBlockState(new BlockPos(X, CY-1, CZ-1)).getValue(FlywheelMultiBlock.FORMED), "not formed");
		h.setBlock(new BlockPos(X, CY-1, CZ+1), Blocks.AIR);
		h.runAfterDelay(2, () -> {
			h.assertFalse(fw.hasMultiBlock(), "flywheel still formed");
			h.assertFalse(h.getBlockState(new BlockPos(X, CY+2, CZ)).getValue(FlywheelMultiBlock.FORMED), "frame still formed");
			h.assertFalse(FlywheelMultiBlock.checkForFullMultiBlock(h.getLevel(), abs(h, 2, 0)), "19 parts matched");
			h.succeed();
		});
	}

	/**
	 * Formed flywheel on a running turbine: the dynamometer on its other side reads the turbine's speed and its torque clamped to 12750
	 * Nm; without the turbine the flywheel slows down by omega/32+1 per tick.
	 */
	@GameTest(template = TALL, timeoutTicks = 400)
	public static void flywheelPassesTurbinePower(GameTestHelper h) {
		ReactorFlywheelBlockEntity fw = buildFlywheel(h);
		FlywheelMultiBlock.onCreateFullMultiBlock(h.getLevel(), abs(h, 1, 0));
		TurbineCoreBlockEntity tb = turbine(h);
		DynamometerBlockEntity dyn = dynamometer(h);
		int[] last = new int[1];
		h.runAtTickTime(300, () -> {
			h.assertTrue(fw.hasMultiBlock() && tb.getOmega() > 0, "turbine "+tb.getOmega()+" rad/s");
			//the flywheel copies the turbine during its own tick, so it may be one turbine update behind
			h.assertTrue(Math.abs(fw.getOmega()-tb.getOmega()) <= tb.getOmega()/20 && fw.getTorque() <= 12750 && Math.abs(fw.getTorque()-Math.min(tb.getTorque(), 12750)) <= tb.getTorque()/20+1,
					"flywheel "+fw.getOmega()+" rad/s "+fw.getTorque()+" Nm, turbine "+tb.getOmega()+"/"+tb.getTorque());
			h.assertTrue(dyn.getOmega() > 0 && dyn.getTorque() > 0, "dynamometer "+dyn.getOmega()+" rad/s "+dyn.getTorque()+" Nm");
			last[0] = fw.getOmega();
			h.setBlock(new BlockPos(X+1, CY, CZ), Blocks.AIR);
		});
		h.runAtTickTime(302, () -> {
			int o = fw.getOmega();
			int w1 = last[0]-(last[0]/32+1);
			h.assertTrue(o < last[0] && o >= w1-(w1/32+1)-1, "spin-down "+last[0]+" -> "+o);
			h.succeed();
		});
	}

	/** Without the formed ring shafts do not read the flywheel (isEmitting), although it still follows the turbine. */
	@GameTest(template = TALL, timeoutTicks = 400)
	public static void unformedFlywheelNotRead(GameTestHelper h) {
		ReactorFlywheelBlockEntity fw = buildFlywheel(h);
		TurbineCoreBlockEntity tb = turbine(h);
		DynamometerBlockEntity dyn = dynamometer(h);
		h.runAtTickTime(300, () -> {
			h.assertTrue(tb.getOmega() > 0 && fw.getOmega() > 0, "turbine "+tb.getOmega()+", flywheel "+fw.getOmega());
			h.assertTrue(dyn.getOmega() == 0, "dynamometer read "+dyn.getOmega()+" rad/s");
			h.succeed();
		});
	}

	/** Screwdriver: unformed it turns through all four directions, formed it only reverses. */
	@GameTest(template = TALL, timeoutTicks = 20)
	public static void screwdriverTurns(GameTestHelper h) {
		ReactorFlywheelBlockEntity fw = buildFlywheel(h);
		fw.onRightClick(h.getLevel(), fw.getBlockPos(), Direction.UP);
		h.assertTrue(fw.getBlockMetadata() == 1, "unformed: "+fw.getBlockMetadata());
		fw.setBlockMetadata(3);
		fw.onRightClick(h.getLevel(), fw.getBlockPos(), Direction.UP);
		h.assertTrue(fw.getBlockMetadata() == 0, "unformed wrap: "+fw.getBlockMetadata());
		fw.setHasMultiBlock(true);
		fw.setBlockMetadata(2);
		fw.onRightClick(h.getLevel(), fw.getBlockPos(), Direction.UP);
		h.assertTrue(fw.getBlockMetadata() == 3, "formed: "+fw.getBlockMetadata());
		fw.onRightClick(h.getLevel(), fw.getBlockPos(), Direction.UP);
		h.assertTrue(fw.getBlockMetadata() == 2, "formed back: "+fw.getBlockMetadata());
		h.succeed();
	}
}
