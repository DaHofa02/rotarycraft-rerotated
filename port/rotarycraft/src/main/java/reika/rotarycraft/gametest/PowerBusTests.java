/*******************************************************************************
 * Tests for the ported RotaryCraft bus controller and power bus (original TileEntityBusController, TileEntityPowerBus). Private
 * NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BusControllerBlockEntity;
import reika.rotarycraft.content.blockentity.FanBlockEntity;
import reika.rotarycraft.content.blockentity.PowerBusBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.GearboxTypes.GearPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PowerBusTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	private static PowerBusBlockEntity bus(GameTestHelper h, int x) {
		PowerBusBlockEntity b = place(h, RotaryBlocks.POWER_BUS.get(), x, 2, 4, 0, PowerBusBlockEntity.class);
		b.findNetwork(h.getLevel(), h.absolutePos(new BlockPos(x, 2, 4))); //what setting it down does
		return b;
	}

	/**
	 * Gas engine (512 rad/s @ 128 Nm) into a lubricated bus controller (meta 1: input west) and two bus blocks east of it. A steel gear on
	 * the first block's north side and a steel 2x unit on the second's east side: two output sides, 64 Nm each. North: 512 rad/s @ 64 Nm;
	 * east (torque mode) 256 rad/s @ 128 Nm, switched to speed mode 1024 rad/s @ 32 Nm. Without lubricant nothing moves.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void powerBusSplitsAndGears(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		BusControllerBlockEntity hub = place(h, RotaryBlocks.BUS_CONTROLLER.get(), 2, 2, 4, 1, BusControllerBlockEntity.class);
		hub.setLubricant(4000);
		PowerBusBlockEntity b1 = bus(h, 3), b2 = bus(h, 4);
		h.assertTrue(b1.getInputSide() == Direction.WEST && b2.getBus() == hub.getBus() && hub.getBus().getSize() == 2, "bus not built");
		h.assertFalse(b1.insertItem(new ItemStack(RotaryItems.gearPart(GearboxTypes.STEEL, GearPart.BEARING)), Direction.NORTH), "a bearing taken");
		h.assertTrue(b1.insertItem(new ItemStack(RotaryItems.gearPart(GearboxTypes.STEEL, GearPart.GEAR)), Direction.NORTH), "gear refused");
		h.assertTrue(b2.insertItem(new ItemStack(RotaryItems.gearPart(GearboxTypes.STEEL, GearPart.UNIT2)), Direction.EAST), "unit refused");
		h.assertFalse(b1.canHaveItemInSlot(Direction.EAST) || b1.canHaveItemInSlot(Direction.WEST), "sides towards the bus usable");
		FanBlockEntity north = place(h, RotaryBlocks.FAN.get(), 3, 2, 3, 3, FanBlockEntity.class);
		FanBlockEntity east = place(h, RotaryBlocks.FAN.get(), 5, 2, 4, 1, FanBlockEntity.class);
		int[] phase = {0};
		h.succeedWhen(() -> {
			switch (phase[0]) {
				case 0 -> {
					h.assertTrue(north.getOmega() == 512 && north.getTorque() == 64, "north "+north.getOmega()+" rad/s @ "+north.getTorque());
					h.assertTrue(east.getOmega() == 256 && east.getTorque() == 128, "east "+east.getOmega()+" rad/s @ "+east.getTorque());
					b2.setMode(Direction.EAST, true);
					phase[0] = 1;
					h.fail("torque mode; now speed");
				}
				case 1 -> {
					h.assertTrue(east.getOmega() == 1024 && east.getTorque() == 32, "east "+east.getOmega()+" rad/s @ "+east.getTorque());
					hub.setLubricant(0);
					phase[0] = 2;
					h.fail("speed mode; now dry");
				}
				default -> h.assertTrue(north.getPower() == 0 && east.getPower() == 0, "dry bus gives "+north.getPower()+" / "+east.getPower());
			}
		});
	}

	/** Breaking the first bus block cuts the second off the controller; breaking the controller frees the rest. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void powerBusBreaksApart(GameTestHelper h) {
		BusControllerBlockEntity hub = place(h, RotaryBlocks.BUS_CONTROLLER.get(), 2, 2, 4, 1, BusControllerBlockEntity.class);
		PowerBusBlockEntity b1 = bus(h, 3), b2 = bus(h, 4);
		PowerBusBlockEntity b3 = place(h, RotaryBlocks.POWER_BUS.get(), 2, 2, 5, 0, PowerBusBlockEntity.class);
		b3.findNetwork(h.getLevel(), h.absolutePos(new BlockPos(2, 2, 5)));
		h.assertTrue(hub.getBus().getSize() == 3 && b3.getInputSide() == Direction.NORTH, "bus size "+hub.getBus().getSize());
		h.destroyBlock(new BlockPos(3, 2, 4));
		h.assertTrue(b2.getBus() == null && b3.getBus() == hub.getBus() && hub.getBus().getSize() == 1, "after the break: "+b2.getBus()+", size "+hub.getBus().getSize());
		h.destroyBlock(new BlockPos(2, 2, 4));
		h.assertTrue(b3.getBus() == null, "controller gone, bus kept");
		h.succeed();
	}
}
