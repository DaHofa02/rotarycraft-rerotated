/*******************************************************************************
 * Practice factory zone (see Factory): ElectriCraft power lines, built and run by a simulated survival player. Not part of the original
 * mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.electricraft.content.blockentity.BatteryBlockEntity;
import reika.electricraft.content.blockentity.MeterBlockEntity;
import reika.electricraft.content.blockentity.ResistorBlockEntity;
import reika.electricraft.registry.BatteryType;
import reika.electricraft.registry.ElectriBlocks;
import reika.electricraft.registry.ElectriComponents;
import reika.electricraft.registry.WireType;
import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneElectric {

	/** The ordinary wires (the superconductor wire has no recipe and is not a block of its own here). */
	private static final List<WireType> WIRES = List.of(WireType.STEEL, WireType.TIN, WireType.NICKEL, WireType.ALUMINUM, WireType.COPPER,
			WireType.SILVER, WireType.GOLD, WireType.PLATINUM);

	public static final List<String> BLOCKS;

	static {
		List<String> li = new ArrayList<>(List.of("electricraft:generator", "electricraft:motor", "electricraft:meter", "electricraft:relay",
				"electricraft:resistor", "electricraft:precise_resistor", "electricraft:fuse", "electricraft:transformer"));
		for (WireType w : WIRES) {
			li.add(id(ElectriBlocks.getWire(w, false)));
			li.add(id(ElectriBlocks.getWire(w, true)));
		}
		for (BatteryType b : BatteryType.batteryList)
			li.add(id(ElectriBlocks.battery(b).get()));
		BLOCKS = List.copyOf(li);
	}

	private static String id(net.minecraft.world.level.block.Block b) {
		return BuiltInRegistries.BLOCK.getKey(b).toString();
	}

	/** The line runs along z = 10, power flowing west, everything placed looking east. */
	private static final int Z = 10;

	private static ItemStack gearbox() {
		return gearbox(4);
	}

	private static ItemStack gearbox(int ratio) {
		return GearboxBlock.gearboxItem(GearboxTypes.WOOD, ratio);
	}

	/** The machines every line needs, in one infinite chest at (1, 2, 1). */
	private static BlockPos machineChest(GameTestHelper h) {
		ItemStack fuse = new ItemStack(ElectriBlocks.FUSE.get());
		fuse.set(ElectriComponents.FUSE_LIMIT.get(), 128);
		BlockPos c = new BlockPos(1, 2, 1);
		Factory.infiniteChest(h, c, new ItemStack(RotaryBlocks.DC_ENGINE.get()), gearbox(), new ItemStack(ElectriBlocks.GENERATOR.get()),
				new ItemStack(Items.REDSTONE_BLOCK), new ItemStack(ElectriBlocks.MOTOR.get()), new ItemStack(RotaryBlocks.DYNAMOMETER.get()),
				new ItemStack(ElectriBlocks.METER.get()), new ItemStack(ElectriBlocks.RELAY.get()), new ItemStack(ElectriBlocks.RESISTOR.get()),
				new ItemStack(ElectriBlocks.PRECISE_RESISTOR.get()), fuse, new ItemStack(ElectriBlocks.TRANSFORMER.get()), new ItemStack(Items.LEVER),
				new ItemStack(Items.BROWN_DYE));
		return c;
	}

	/**
	 * Source at x..x-2: DC engine (256 rad/s, 4 Nm) on a redstone block, wooden 4:1 gearbox (64 rad/s, 16 Nm), generator (512 V, 2 A),
	 * placed looking east as the player does; the line goes on west of x-2.
	 */
	private static void source(SimPlayer p, BlockPos chest, int x) {
		source(p, chest, chest, x, gearbox());
	}

	/** As above with the given gearbox (taken from {@code gearChest}). */
	private static void source(SimPlayer p, BlockPos chest, BlockPos gearChest, int x, ItemStack gbx) {
		Factory.take(p, chest, RotaryBlocks.DC_ENGINE.get().asItem(), 1);
		Factory.take(p, gearChest, gbx, 1);
		Factory.take(p, chest, ElectriBlocks.GENERATOR.get().asItem(), 1);
		Factory.take(p, chest, Items.REDSTONE_BLOCK, 1);
		p.place(new BlockPos(x, 2, Z), RotaryBlocks.DC_ENGINE.get(), Direction.EAST);
		p.place(new BlockPos(x, 2, Z+1), Items.REDSTONE_BLOCK, Direction.EAST);
		p.place(new BlockPos(x-1, 2, Z), gbx, Direction.EAST);
		p.place(new BlockPos(x-2, 2, Z), ElectriBlocks.GENERATOR.get(), Direction.EAST);
	}

	/** Motor at x and dynamometer at x-1, looking east. */
	private static DynamometerBlockEntity load(GameTestHelper h, SimPlayer p, BlockPos chest, int x) {
		Factory.take(p, chest, ElectriBlocks.MOTOR.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.DYNAMOMETER.get().asItem(), 1);
		p.place(new BlockPos(x, 2, Z), ElectriBlocks.MOTOR.get(), Direction.EAST);
		p.place(new BlockPos(x-1, 2, Z), RotaryBlocks.DYNAMOMETER.get(), Direction.EAST);
		return (DynamometerBlockEntity)h.getBlockEntity(new BlockPos(x-1, 2, Z));
	}

	private static SimPlayer start(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 47, 47);
		return new SimPlayer(h, GameType.SURVIVAL);
	}

	/**
	 * All sixteen wires (each metal bare and insulated) in one line, then a meter: 2 * (64+32+16+8+2+1+4+16) = 286 ohm, so the motor gives
	 * (512-286)/8 = 28 rad/s at 8*2 A = 16 Nm, and the meter reads the 2 A.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 300, batch = "factory")
	public static void everyWireCarriesPower(GameTestHelper h) {
		SimPlayer p = start(h);
		BlockPos chest = machineChest(h), wires = new BlockPos(3, 2, 1);
		List<ItemStack> ws = new ArrayList<>();
		for (WireType w : WIRES) {
			ws.add(new ItemStack(ElectriBlocks.getWire(w, false)));
			ws.add(new ItemStack(ElectriBlocks.getWire(w, true)));
		}
		Factory.infiniteChest(h, wires, ws.toArray(ItemStack[]::new));
		source(p, chest, 30);
		int x = 27;
		for (ItemStack w : ws) {
			Factory.take(p, wires, w, 1);
			p.place(new BlockPos(x--, 2, Z), w, Direction.EAST);
		}
		Factory.take(p, chest, ElectriBlocks.METER.get().asItem(), 1);
		p.place(new BlockPos(x, 2, Z), ElectriBlocks.METER.get(), Direction.EAST);
		MeterBlockEntity meter = (MeterBlockEntity)h.getBlockEntity(new BlockPos(x, 2, Z));
		DynamometerBlockEntity dyna = load(h, p, chest, x-1);
		h.succeedWhen(() -> {
			h.assertTrue(dyna.omega == 28 && dyna.torque == 16, "expected 28 rad/s @ 16 Nm, got "+dyna.omega+" @ "+dyna.torque);
			h.assertTrue(meter.getWireCurrent() == 2, "meter current "+meter.getWireCurrent());
			Factory.assertBuilt(h, ZoneElectricLists.LINE1, new BlockPos(0, 2, 0), new BlockPos(47, 2, 47));
			p.leave();
		});
	}

	/** Right-click with the held item at a point of the block's top face (dx, dz within the block), as the client sends it. */
	private static void useAt(GameTestHelper h, SimPlayer p, BlockPos rel, double dx, double dz) {
		p.moveNear(rel);
		BlockPos a = h.absolutePos(rel);
		BlockHitResult r = new BlockHitResult(new Vec3(a.getX()+dx, a.getY()+0.25, a.getZ()+dz), Direction.UP, a, false);
		p.player.gameMode.useItemOn(p.player, p.level(), p.player.getMainHandItem(), InteractionHand.MAIN_HAND, r);
	}

	/**
	 * Generator -> steel wire -> relay (lever beside it) -> steel wire -> resistor -> precise resistor -> 128 A fuse -> steel wire ->
	 * motor. The player paints the resistors with brown dye on the band that makes 1 A (resistor: band 2 -> black-brown-black = 01 x 10^0;
	 * precise resistor: band 3 -> 001 x 10^0). With the lever off nothing arrives; switched on, the motor gets the 1 A the resistors pass:
	 * 8 Nm at (512-3*64)/8 = 40 rad/s, and the fuse holds.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 400, batch = "factory")
	public static void relayResistorsAndFuse(GameTestHelper h) {
		SimPlayer p = start(h);
		BlockPos chest = machineChest(h), wires = new BlockPos(3, 2, 1);
		ItemStack steel = new ItemStack(ElectriBlocks.getWire(WireType.STEEL, false));
		Factory.infiniteChest(h, wires, steel);
		source(p, chest, 20);
		Factory.take(p, wires, steel, 3);
		Factory.take(p, chest, ElectriBlocks.RELAY.get().asItem(), 1);
		Factory.take(p, chest, ElectriBlocks.RESISTOR.get().asItem(), 1);
		Factory.take(p, chest, ElectriBlocks.PRECISE_RESISTOR.get().asItem(), 1);
		ItemStack fuse = new ItemStack(ElectriBlocks.FUSE.get());
		fuse.set(ElectriComponents.FUSE_LIMIT.get(), 128);
		Factory.take(p, chest, fuse, 1);
		Factory.take(p, chest, Items.LEVER, 1);
		Factory.takeExactly(p, chest, Items.BROWN_DYE, 2);
		p.place(new BlockPos(17, 2, Z), steel, Direction.EAST);
		p.place(new BlockPos(16, 2, Z), ElectriBlocks.RELAY.get(), Direction.EAST);
		p.place(new BlockPos(16, 2, Z+1), Items.LEVER, Direction.EAST);
		p.place(new BlockPos(15, 2, Z), steel, Direction.EAST);
		p.place(new BlockPos(14, 2, Z), ElectriBlocks.RESISTOR.get(), Direction.EAST);
		p.place(new BlockPos(13, 2, Z), ElectriBlocks.PRECISE_RESISTOR.get(), Direction.EAST);
		p.place(new BlockPos(12, 2, Z), fuse, Direction.EAST);
		p.place(new BlockPos(11, 2, Z), steel, Direction.EAST);
		DynamometerBlockEntity dyna = load(h, p, chest, 10);
		// bands lie along x for an east-facing component, counted from the east end: band i covers 1-x in [0.125+0.1875(i-1), +0.125]
		p.hold(Items.BROWN_DYE);
		useAt(h, p, new BlockPos(14, 2, Z), 1-0.375, 0.5);
		useAt(h, p, new BlockPos(13, 2, Z), 1-0.5625, 0.5);
		ResistorBlockEntity r = (ResistorBlockEntity)h.getBlockEntity(new BlockPos(14, 2, Z));
		ResistorBlockEntity pr = (ResistorBlockEntity)h.getBlockEntity(new BlockPos(13, 2, Z));
		h.assertTrue(r.getCurrentLimit() == 1 && pr.getCurrentLimit() == 1, "resistor limits "+r.getCurrentLimit()+" / "+pr.getCurrentLimit()+" A");
		h.assertTrue(p.count(Items.BROWN_DYE) == 0, "dye not used up");
		boolean[] on = {false};
		h.runAtTickTime(80, () -> {
			h.assertTrue(dyna.power == 0, "open relay passes "+dyna.power+" W");
			p.emptyHand();
			p.use(new BlockPos(16, 2, Z+1));
			on[0] = true;
		});
		h.succeedWhen(() -> {
			h.assertTrue(on[0], "lever not flipped yet");
			h.assertTrue(dyna.omega == 40 && dyna.torque == 8, "expected 40 rad/s @ 8 Nm, got "+dyna.omega+" @ "+dyna.torque);
			h.assertTrue(!((reika.electricraft.content.blockentity.FuseBlockEntity)h.getBlockEntity(new BlockPos(12, 2, Z))).isOverloaded(), "fuse blew");
			Factory.assertBuilt(h, ZoneElectricLists.LINE2, new BlockPos(0, 2, 0), new BlockPos(47, 2, 47));
			p.leave();
		});
	}

	/**
	 * Generator -> steel wire -> transformer (1:1; placed looking south, its output then faces west) -> steel wire -> motor: 448 V in and
	 * out, motor (448-64)/8 = 48 rad/s at 16 Nm.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 300, batch = "factory")
	public static void transformerPassesPower(GameTestHelper h) {
		SimPlayer p = start(h);
		BlockPos chest = machineChest(h), wires = new BlockPos(3, 2, 1);
		ItemStack steel = new ItemStack(ElectriBlocks.getWire(WireType.STEEL, false));
		Factory.infiniteChest(h, wires, steel);
		source(p, chest, 20);
		Factory.take(p, wires, steel, 2);
		Factory.take(p, chest, ElectriBlocks.TRANSFORMER.get().asItem(), 1);
		p.place(new BlockPos(17, 2, Z), steel, Direction.EAST);
		p.place(new BlockPos(16, 2, Z), ElectriBlocks.TRANSFORMER.get(), Direction.SOUTH);
		p.place(new BlockPos(15, 2, Z), steel, Direction.EAST);
		DynamometerBlockEntity dyna = load(h, p, chest, 14);
		h.succeedWhen(() -> {
			h.assertTrue(dyna.omega == 48 && dyna.torque == 16, "expected 48 rad/s @ 16 Nm, got "+dyna.omega+" @ "+dyna.torque);
			Factory.assertBuilt(h, List.of("electricraft:generator", "electricraft:transformer", "electricraft:motor"),
					new BlockPos(0, 2, 0), new BlockPos(47, 2, 47));
			p.leave();
		});
	}

	/**
	 * Generator -> a copper wire line with one battery of each kind beside it (south): every battery charges. The network shares its
	 * current among the batteries in whole amperes, so the 2 A of the usual source would reach only two of them; here a wooden 16:1
	 * gearbox gives the generator 64 Nm: 128 V at 8 A.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 300, batch = "factory")
	public static void everyBatteryCharges(GameTestHelper h) {
		SimPlayer p = start(h);
		BlockPos chest = machineChest(h), store = new BlockPos(3, 2, 1);
		ItemStack copper = new ItemStack(ElectriBlocks.getWire(WireType.COPPER, false));
		List<ItemStack> supplies = new ArrayList<>(List.of(copper, gearbox(16)));
		for (BatteryType b : BatteryType.batteryList)
			supplies.add(new ItemStack(ElectriBlocks.battery(b).get()));
		Factory.infiniteChest(h, store, supplies.toArray(ItemStack[]::new));
		source(p, chest, store, 20, gearbox(16));
		int n = BatteryType.batteryList.length;
		Factory.take(p, store, copper, n);
		List<BatteryBlockEntity> bats = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			BlockPos w = new BlockPos(17-i, 2, Z);
			p.place(w, copper, Direction.EAST);
			ItemStack bat = new ItemStack(ElectriBlocks.battery(BatteryType.batteryList[i]).get());
			Factory.take(p, store, bat, 1);
			p.place(w.south(), bat, Direction.EAST);
			bats.add((BatteryBlockEntity)h.getBlockEntity(w.south()));
		}
		h.succeedWhen(() -> {
			for (int i = 0; i < n; i++)
				h.assertTrue(bats.get(i).getStoredEnergy() > 0, BatteryType.batteryList[i]+" battery not charged");
			Factory.assertBuilt(h, ZoneElectricLists.BATTERIES, new BlockPos(0, 2, 0), new BlockPos(47, 2, 47));
			p.leave();
		});
	}

	/** The blocks each test method builds (for its own assertBuilt). */
	private static final class ZoneElectricLists {
		static final List<String> LINE1;
		static final List<String> LINE2 = List.of("electricraft:generator", "electricraft:relay", "electricraft:resistor", "electricraft:precise_resistor",
				"electricraft:fuse", "electricraft:motor", "electricraft:steel_wire");
		static final List<String> BATTERIES;

		static {
			List<String> l1 = new ArrayList<>(List.of("electricraft:generator", "electricraft:meter", "electricraft:motor"));
			for (WireType w : WIRES) {
				l1.add(id(ElectriBlocks.getWire(w, false)));
				l1.add(id(ElectriBlocks.getWire(w, true)));
			}
			LINE1 = List.copyOf(l1);
			List<String> b = new ArrayList<>();
			for (BatteryType t : BatteryType.batteryList)
				b.add(id(ElectriBlocks.battery(t).get()));
			BATTERIES = List.copyOf(b);
		}
	}
}
