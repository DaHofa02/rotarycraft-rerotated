/*******************************************************************************
 * Practice factory zone (see Factory): a RotaryCraft power bus built by a simulated survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.FanBlockEntity;
import reika.rotarycraft.content.blockentity.PowerBusBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.GearboxTypes.GearPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZonePowerBus {

	public static final List<String> BLOCKS = List.of("rotarycraft:bus_controller", "rotarycraft:power_bus");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> bus controller (a lubricant barrel above it, a hose between) -> power bus block west of it. The player
	 * puts a steel gear into the bus block's north slot (GUI) and sets a fan there: it runs on the whole 512 rad/s @ 128 Nm.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void powerBusDrivesAFan(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Item gear = reika.rotarycraft.registry.RotaryItems.gearPart(GearboxTypes.STEEL, GearPart.GEAR);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BUS_CONTROLLER.get()), new ItemStack(RotaryBlocks.POWER_BUS.get()), new ItemStack(RotaryBlocks.FAN.get()),
				new ItemStack(Items.DIRT), new ItemStack(gear), new ItemStack(RotaryBlocks.HOSE.get()));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BUS_CONTROLLER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.POWER_BUS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.FAN.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 3);
		Factory.takeExactly(p, chest, gear, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos hub = new BlockPos(5, 3, 5), busPos = new BlockPos(4, 3, 5), fanPos = new BlockPos(4, 3, 4);
		for (BlockPos b : new BlockPos[] {hub, busPos, fanPos})
			p.place(b.below(), Items.DIRT, Direction.EAST);
		p.placeSneaking(hub, new ItemStack(RotaryBlocks.BUS_CONTROLLER.get()), Direction.EAST); //looking east: input east
		p.place(busPos, RotaryBlocks.POWER_BUS.get(), Direction.EAST);
		h.assertTrue(p.open(busPos) != null, "power bus opens no GUI");
		p.putInto(0, gear, 1); //the north slot (the east and west sides face the controller and nothing: east is not usable)
		p.closeMenu();
		p.placeSneaking(fanPos, new ItemStack(RotaryBlocks.FAN.get()), Direction.SOUTH); //input south
		Factory.take(p, chest, RotaryBlocks.HOSE.get().asItem(), 1);
		p.placeSneaking(hub.above(), new ItemStack(RotaryBlocks.HOSE.get()), Direction.EAST);
		Factory.fluidBarrel(h, hub.above(2), RotaryFluids.LUBRICANT.get());
		PowerBusBlockEntity bus = (PowerBusBlockEntity)h.getBlockEntity(busPos);
		h.assertTrue(bus.getBus() != null && bus.getAbsRatio(Direction.NORTH) == 1 && p.count(gear) == 0, "bus "+bus.getBus()+", ratio "+bus.getAbsRatio(Direction.NORTH));
		FanBlockEntity fan = (FanBlockEntity)h.getBlockEntity(fanPos);

		h.succeedWhen(() -> {
			h.assertTrue(fan.getOmega() == 512 && fan.getTorque() == 128, "fan at "+fan.getOmega()+" rad/s @ "+fan.getTorque()+" Nm; controller "+((reika.rotarycraft.content.blockentity.BusControllerBlockEntity)h.getBlockEntity(hub)).getPower()+" W, lube "+((reika.rotarycraft.content.blockentity.BusControllerBlockEntity)h.getBlockEntity(hub)).getLubricant());
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
