/*******************************************************************************
 * Tests for the ported RotaryCraft distribution clutch (original TileEntityDistributionClutch). Private NeoForge 1.21.1 port of
 * RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.DistributionClutchBlockEntity;
import reika.rotarycraft.content.blockentity.FanBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DistributionClutchTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/**
	 * Gas engine (512 rad/s @ 128 Nm) into a distribution clutch (meta 1: input west, through east), fans east and north of it. Nothing
	 * requested: all 128 Nm go east. North 32 Nm and south 200 Nm requested and both on: north gets 32, south the remaining 96, east
	 * nothing. South off again: east gets 96.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void distributionClutchSharesTorque(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		DistributionClutchBlockEntity te = place(h, RotaryBlocks.DISTRIBUTION_CLUTCH.get(), 2, 2, 4, 1, DistributionClutchBlockEntity.class);
		FanBlockEntity east = place(h, RotaryBlocks.FAN.get(), 3, 2, 4, 1, FanBlockEntity.class);
		FanBlockEntity north = place(h, RotaryBlocks.FAN.get(), 2, 2, 3, 3, FanBlockEntity.class);
		int[] phase = {0};
		h.succeedWhen(() -> {
			switch (phase[0]) {
				case 0 -> {
					h.assertTrue(east.getPower() == 65536 && north.getPower() == 0, "east "+east.getPower()+" W, north "+north.getPower());
					te.setTorqueRequest(Direction.NORTH, 32);
					te.setTorqueRequest(Direction.SOUTH, 200);
					te.setSideEnabled(Direction.NORTH, true);
					te.setSideEnabled(Direction.SOUTH, true);
					phase[0] = 1;
					h.fail("straight through; now shared");
				}
				case 1 -> {
					h.assertTrue(north.getOmega() == 512 && north.getTorque() == 32, "north "+north.getOmega()+" rad/s @ "+north.getTorque());
					h.assertTrue(te.getTorqueToSide(Direction.SOUTH) == 96 && te.getSpeedToSide(Direction.SOUTH) == 512, "south "+te.getTorqueToSide(Direction.SOUTH));
					h.assertTrue(east.getTorque() == 0 && te.getTorqueToSide(Direction.EAST) == 0, "east still gets "+east.getTorque());
					te.setSideEnabled(Direction.SOUTH, false);
					phase[0] = 2;
					h.fail("shared; now south off");
				}
				default -> h.assertTrue(east.getOmega() == 512 && east.getTorque() == 96 && north.getTorque() == 32, "east "+east.getTorque()+" Nm, north "+north.getTorque());
			}
		});
	}

	/**
	 * Redstone control (screwdriver while sneaking; GUI settings are dropped on the switch): without a signal only the way through is
	 * open (east 128 Nm); a repeater's strong signal 15 into it opens all four bits: north 48, south 48, east the remaining 32.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void distributionClutchRedstoneControl(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		DistributionClutchBlockEntity te = place(h, RotaryBlocks.DISTRIBUTION_CLUTCH.get(), 2, 2, 4, 1, DistributionClutchBlockEntity.class);
		te.setTorqueRequest(Direction.NORTH, 48);
		te.setTorqueRequest(Direction.SOUTH, 48);
		te.setSideEnabled(Direction.SOUTH, true);
		te.onShiftRightClick(h.getLevel(), te.getBlockPos(), Direction.UP);
		h.assertTrue(te.getControlMode() == DistributionClutchBlockEntity.ControlMode.REDSTONE && !te.isSideEnabled(Direction.SOUTH), "mode not switched");
		boolean[] on = {false};
		h.succeedWhen(() -> {
			if (!on[0]) {
				h.assertTrue(te.getTorqueToSide(Direction.EAST) == 128 && te.getTorqueToSide(Direction.NORTH) == 0, "without a signal: east "+te.getTorqueToSide(Direction.EAST));
				h.setBlock(new BlockPos(2, 2, 5), Blocks.REPEATER.defaultBlockState().setValue(net.minecraft.world.level.block.RepeaterBlock.FACING, Direction.SOUTH)); //input south, strong output north into it
				h.setBlock(new BlockPos(2, 2, 6), Blocks.REDSTONE_BLOCK); //after the repeater, so that it notices
				on[0] = true;
				h.fail("no signal; now a repeater");
			}
			h.assertTrue(te.getTorqueToSide(Direction.NORTH) == 48 && te.getTorqueToSide(Direction.SOUTH) == 48 && te.getTorqueToSide(Direction.EAST) == 32,
					"north "+te.getTorqueToSide(Direction.NORTH)+", south "+te.getTorqueToSide(Direction.SOUTH)+", east "+te.getTorqueToSide(Direction.EAST));
		});
	}
}
