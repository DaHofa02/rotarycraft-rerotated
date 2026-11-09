/*******************************************************************************
 * Practice factory zone (see Factory): a steam power plant - real steam from a solar-heated sodium loop - with a turbine, the reactor
 * generator, RF storage and wireless pads, built by a simulated survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import reika.electricraft.content.block.WirelessPadBlock;
import reika.electricraft.content.blockentity.RFBatteryBlockEntity;
import reika.electricraft.content.blockentity.RFCableBlockEntity;
import reika.electricraft.network.ElectriPayloads;
import reika.electricraft.registry.ElectriBlocks;
import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.block.SteamBlock;
import reika.reactorcraft.powergen.CondenserBlockEntity;
import reika.reactorcraft.powergen.HeatExchangerBlockEntity;
import reika.reactorcraft.powergen.HiPTurbineBlockEntity;
import reika.reactorcraft.powergen.ReactorFlywheelBlockEntity;
import reika.reactorcraft.powergen.ReactorGeneratorBlockEntity;
import reika.reactorcraft.powergen.SolarExchangerBlockEntity;
import reika.reactorcraft.powergen.SolarTopBlockEntity;
import reika.reactorcraft.powergen.SteamDiffuserBlockEntity;
import reika.reactorcraft.powergen.SteamGrateBlockEntity;
import reika.reactorcraft.powergen.SteamLineBlockEntity;
import reika.reactorcraft.powergen.TurbineCoreBlockEntity;
import reika.reactorcraft.powergen.TurbineMeterBlockEntity;
import reika.reactorcraft.reactor.ReactorBoilerBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.content.blockentity.SolarTowerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSteamPower {

	/** The steam plant (solar tower, sodium loop, heat exchanger, boiler) both tests build the same way. */
	private static final List<String> PLANT = List.of("rotarycraft:solar_tower", "rotarycraft:solar_mirror", "reactorcraft:solar_top",
			"reactorcraft:solar_exchanger", "reactorcraft:heat_exchanger", "reactorcraft:reactor_boiler", "reactorcraft:steam_line",
			"reactorcraft:steam_grate");

	private static final List<String> RF_PLANT = List.of("reactorcraft:turbine_core", "reactorcraft:generator_core", "reactorcraft:generator_housing",
			"reactorcraft:generator_winding", "reactorcraft:steam_bypass", "reactorcraft:reactor_generator", "reactorcraft:condenser",
			"reactorcraft:turbine_meter", "electricraft:rf_battery", "electricraft:rf_cable", "electricraft:basic_wireless_pad",
			"electricraft:improved_wireless_pad", "electricraft:advanced_wireless_pad", "electricraft:hightech_wireless_pad",
			"electricraft:superconducting_wireless_pad");

	private static final List<String> MECH_PLANT = List.of("reactorcraft:turbine_core", "reactorcraft:turbine_flywheel", "reactorcraft:flywheel_core",
			"reactorcraft:flywheel_dampener", "reactorcraft:flywheel_frame", "reactorcraft:steam_diffuser", "reactorcraft:hip_turbine",
			"reactorcraft:turbine_blades", "reactorcraft:turbine_housing", "reactorcraft:steam_injector");

	public static final List<String> BLOCKS;

	static {
		java.util.Set<String> li = new java.util.LinkedHashSet<>(PLANT);
		li.addAll(RF_PLANT);
		li.addAll(MECH_PLANT);
		BLOCKS = List.copyOf(li);
	}

	/** Solar tower column. */
	private static final int TX = 26, TZ = 24;
	/** The turbine line: steam moving west along z = ZT at height Y, the first stage at x = X0. */
	private static final int X0 = 21, Y = 5, ZT = 22;

	private record Plant(BlockPos tower, BlockPos exchanger, BlockPos hx, BlockPos boiler, BlockPos grate, int ambient) {}

	/**
	 * The steam plant, the same in every test of this zone (positions are fixed):
	 *  - solar tower (26, 4..6, 24) with the two-block sodium receiver on top and 73 mirrors around it at y = 4 (a 9x9 field, joined to
	 *    the tower and each other; the pipes and steam lines that pass through the field take the other places). Sodium from a barrel
	 *    goes into the tower through a pipe at its side. At noon the receiver passes 800 C and the tower runs on sodium.
	 *  - under the tower the solar exchanger (26, 3, 24), driven at 131072 rad/s by a microturbine through bevel gears (jet fuel by fuel
	 *    lines laid in the floor under a barrel): it takes the sodium the tower uses up as warm sodium.
	 *  - four pipes carry the warm sodium onto the heat exchanger (24, 3, 23), itself driven by a second microturbine through bevel gears:
	 *    it cools the warm sodium (the cold sodium goes out at its west side through a pipe into a reservoir) and gets hot.
	 *  - the reactor boiler (24, 3, 22) beside it, water piped in from below: the heat exchanger passes its heat on, and above 100 C the
	 *    boiler boils. Steam lines (24, 4, 22) -> (22, 4, 22) -> (22, 3, 22) lead to the steam grate (21, 3, 22).
	 * Temporary cobblestone props hold the exchangers until their bevel gears go in last (bevel gears pick their route on the first tick).
	 */
	private static Plant buildPlant(GameTestHelper h, SimPlayer p) {
		h.getLevel().setDayTime(6000);
		BlockPos chest = new BlockPos(1, 2, 1);
		Factory.infiniteChest(h, chest, new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.COBBLESTONE), new ItemStack(RotaryBlocks.SOLAR_TOWER.get()),
				new ItemStack(RotaryBlocks.MIRROR.get()), new ItemStack(ReactorBlocks.SOLAR_TOP.get()), new ItemStack(ReactorBlocks.SOLAR_EXCHANGER.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), new ItemStack(RotaryBlocks.FUEL_LINE.get()),
				new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(ReactorBlocks.HEAT_EXCHANGER.get()), new ItemStack(ReactorBlocks.BOILER.get()),
				new ItemStack(ReactorBlocks.STEAM_LINE.get()), new ItemStack(ReactorBlocks.STEAM_GRATE.get()), new ItemStack(RotaryBlocks.RESERVOIR.get()));
		Factory.take(p, chest, Items.IRON_PICKAXE, 1);
		Factory.takeExactly(p, chest, Items.COBBLESTONE, 2);
		Factory.takeExactly(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 4);
		Factory.takeExactly(p, chest, RotaryBlocks.MICRO_ENGINE.get().asItem(), 2);
		Factory.takeExactly(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 2);
		Factory.takeExactly(p, chest, RotaryBlocks.SOLAR_TOWER.get().asItem(), 3);
		Factory.takeExactly(p, chest, ReactorBlocks.SOLAR_TOP.get().asItem(), 2);
		Factory.takeExactly(p, chest, ReactorBlocks.SOLAR_EXCHANGER.get().asItem(), 1);
		Factory.takeExactly(p, chest, ReactorBlocks.HEAT_EXCHANGER.get().asItem(), 1);
		Factory.takeExactly(p, chest, ReactorBlocks.BOILER.get().asItem(), 1);
		Factory.takeExactly(p, chest, RotaryBlocks.PIPE.get().asItem(), 8);
		Factory.takeExactly(p, chest, RotaryBlocks.RESERVOIR.get().asItem(), 1);
		Factory.takeExactly(p, chest, ReactorBlocks.STEAM_LINE.get().asItem(), 4);
		Factory.takeExactly(p, chest, ReactorBlocks.STEAM_GRATE.get().asItem(), 1);

		BlockPos tower = new BlockPos(TX, 4, TZ), ex = tower.below(), bev1 = ex.below(), mt1 = bev1.east();
		BlockPos hx = new BlockPos(TX-2, 3, TZ-1), bev2 = hx.below(), mt2 = bev2.west(), boiler = hx.north();

		// solar exchanger drive: fuel lines in a trench under the first microturbine and a jet fuel barrel
		p.mine(mt1.below(), Items.IRON_PICKAXE);
		p.mine(mt1.below().east(), Items.IRON_PICKAXE);
		p.place(mt1.below().east(), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.place(mt1.below(), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(mt1, new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), Direction.EAST); //looking east: drives west
		Factory.fluidBarrel(h, mt1.east(), RotaryFluids.JET_FUEL.get());
		// the solar exchanger on a prop, the tower and the receiver on it
		p.place(bev1, Items.COBBLESTONE, Direction.EAST);
		p.placeSneaking(ex, new ItemStack(ReactorBlocks.SOLAR_EXCHANGER.get()), Direction.EAST);
		p.mine(bev1, Items.IRON_PICKAXE);
		for (int i = 0; i < 3; i++)
			p.placeSneaking(tower.above(i), new ItemStack(RotaryBlocks.SOLAR_TOWER.get()), Direction.EAST);
		p.placeSneaking(tower.above(3), new ItemStack(ReactorBlocks.SOLAR_TOP.get()), Direction.EAST);
		p.placeSneaking(tower.above(4), new ItemStack(ReactorBlocks.SOLAR_TOP.get()), Direction.EAST);
		// sodium: a pipe at the tower's side under a sodium barrel
		BlockPos naPipe = tower.south();
		p.placeSneaking(naPipe, new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		Factory.fluidBarrel(h, naPipe.above(), ReactorFluids.get("sodium"));

		// heat exchanger drive, the same way
		p.mine(mt2.below(), Items.IRON_PICKAXE);
		p.mine(mt2.below().west(), Items.IRON_PICKAXE);
		p.place(mt2.below().west(), RotaryBlocks.FUEL_LINE.get(), Direction.WEST);
		p.place(mt2.below(), RotaryBlocks.FUEL_LINE.get(), Direction.WEST);
		p.placeSneaking(mt2, new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), Direction.WEST); //looking west: drives east
		Factory.fluidBarrel(h, mt2.west(), RotaryFluids.JET_FUEL.get());
		p.place(bev2, Items.COBBLESTONE, Direction.EAST);
		p.placeSneaking(hx, new ItemStack(ReactorBlocks.HEAT_EXCHANGER.get()), Direction.EAST);
		p.mine(bev2, Items.IRON_PICKAXE);
		// warm sodium: exchanger side -> up -> onto the heat exchanger (not past the heat exchanger's sides: they are its cold outlets)
		BlockPos a = ex.west(), b = a.above(), c = b.west(), d = hx.above();
		for (BlockPos q : List.of(a, b, c, d))
			p.placeSneaking(q, new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		// the cooled sodium leaves the heat exchanger at its west side through a pipe into a reservoir
		BlockPos cold = hx.west();
		p.placeSneaking(cold, new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.placeSneaking(cold.south(), new ItemStack(RotaryBlocks.RESERVOIR.get()), Direction.EAST);

		// boiler: water pipes under it from a barrel, steam lines to the grate
		BlockPos w1 = boiler.below(), w2 = w1.north();
		p.place(w1, RotaryBlocks.PIPE.get(), Direction.EAST);
		p.place(w2, RotaryBlocks.PIPE.get(), Direction.EAST);
		Factory.fluidBarrel(h, w2.above(), Fluids.WATER);
		p.placeSneaking(boiler, new ItemStack(ReactorBlocks.BOILER.get()), Direction.EAST);
		BlockPos l1 = boiler.above(), l2 = l1.west(), l3 = l2.west(), l4 = l3.below(), grate = new BlockPos(X0, 3, ZT);
		for (BlockPos q : List.of(l1, l2, l3, l4))
			p.placeSneaking(q, new ItemStack(ReactorBlocks.STEAM_LINE.get()), Direction.EAST);
		p.placeSneaking(grate, new ItemStack(ReactorBlocks.STEAM_GRATE.get()), Direction.EAST);

		// bevel gears last
		p.placeSneaking(bev1, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		p.placeSneaking(bev2, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);

		// the mirror field, from the tower outwards (each mirror set against one already there)
		List<BlockPos> mirrors = new ArrayList<>();
		for (int dx = -4; dx <= 4; dx++)
			for (int dz = -4; dz <= 4; dz++) {
				BlockPos m = tower.offset(dx, 0, dz);
				if (h.getBlockState(m).isAir())
					mirrors.add(m);
			}
		mirrors.sort(Comparator.comparingInt(m -> Math.abs(m.getX()-TX)+Math.abs(m.getZ()-TZ)));
		Factory.take(p, chest, RotaryBlocks.MIRROR.get().asItem(), mirrors.size());
		for (BlockPos m : mirrors)
			p.placeSneaking(m, new ItemStack(RotaryBlocks.MIRROR.get()), Direction.EAST);
		int amb = reika.rotarycraft.lib.world.AmbientTemperature.getAmbientTemperatureAt(h.getLevel(), h.absolutePos(boiler));
		return new Plant(tower, ex, hx, boiler, grate, amb);
	}

	/** Positions of a blueprint built along -x from {@code at} (the ReactorCraft blueprints are drawn along +x; the generator is symmetric). */
	private static Map<BlockPos, Block> westward(String id, BlockPos at) {
		Map<BlockPos, Block> m = new LinkedHashMap<>();
		Factory.blueprint(id, BlockPos.ZERO).forEach((k, v) -> m.put(at.offset(-k.getX(), k.getY(), k.getZ()), v));
		return m;
	}

	/**
	 * The five-stage steam turbine over the plant's grate (stages 21..17, 5, 22), set looking east (the steam moves west), with lubricant
	 * by a hose from a barrel into the first stage's back. The chest holds turbine cores and hoses. Returns the first stage.
	 */
	private static BlockPos buildTurbine(GameTestHelper h, SimPlayer p, BlockPos chest) {
		Factory.takeExactly(p, chest, ReactorBlocks.TURBINE_CORE.get().asItem(), 5);
		Factory.takeExactly(p, chest, RotaryBlocks.HOSE.get().asItem(), 1);
		BlockPos head = new BlockPos(X0, Y, ZT), hose = head.east();
		Factory.fluidBarrel(h, hose.above(), RotaryFluids.LUBRICANT.get());
		p.placeSneaking(hose, new ItemStack(RotaryBlocks.HOSE.get()), Direction.EAST);
		for (int i = 0; i < 5; i++)
			p.placeSneaking(head.west(i), new ItemStack(ReactorBlocks.TURBINE_CORE.get()), Direction.EAST);
		return head;
	}

	/** Like SimPlayer.build, but every block is set down sneaking (machines in the structure would take the click otherwise). */
	private static void buildSneaking(GameTestHelper h, SimPlayer p, Map<BlockPos, Block> bp, List<BlockPos> last, Direction look) {
		List<BlockPos> todo = new ArrayList<>(bp.keySet());
		todo.removeAll(last);
		while (!todo.isEmpty()) {
			BlockPos next = null;
			for (BlockPos q : todo) {
				for (Direction d : Direction.values())
					if (!h.getBlockState(q.relative(d)).isAir() && !h.getBlockState(q.relative(d)).canBeReplaced()) {
						next = q;
						break;
					}
				if (next != null)
					break;
			}
			if (next == null)
				throw new IllegalStateException("nothing to set "+todo.get(0)+" against");
			p.placeSneaking(next, new ItemStack(bp.get(next)), look);
			todo.remove(next);
		}
		for (BlockPos q : last)
			p.placeSneaking(q, new ItemStack(bp.get(q)), look);
	}

	/** Takes what a structure needs from the chest. */
	private static void takeFor(SimPlayer p, BlockPos chest, Map<BlockPos, Block> bp) {
		Map<Block, Integer> need = new LinkedHashMap<>();
		bp.values().forEach(v -> need.merge(v, 1, Integer::sum));
		for (Map.Entry<Block, Integer> e : need.entrySet())
			Factory.take(p, chest, e.getKey().asItem(), e.getValue());
	}

	/** Cobblestone props the player sets under every block of the structure at height y (its bottom row hangs one block above the floor). */
	private static void props(SimPlayer p, BlockPos chest, Map<BlockPos, Block> bp, int y) {
		List<BlockPos> props = new ArrayList<>();
		for (BlockPos q : bp.keySet())
			if (q.getY() == y)
				props.add(q.below());
		Factory.take(p, chest, Items.COBBLESTONE, props.size());
		for (BlockPos q : props)
			p.place(q, Items.COBBLESTONE, Direction.EAST);
	}

	/**
	 * Sets the RF cable network limit the way the cable's GUI does: the player opens it and clicks the buttons, and the client sends the
	 * new limit to the server (ElectriPayloads.RFCableLimit) - here that packet's server handler runs with the player as sender.
	 */
	private static void setCableLimit(SimPlayer p, BlockPos cable, int limit) {
		p.open(cable);
		BlockPos abs = p.helper.absolutePos(cable);
		IPayloadContext ctx = (IPayloadContext)Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(), new Class<?>[] {IPayloadContext.class},
				(proxy, m, args) -> m.getName().equals("player") ? p.player : null);
		try {
			Method handler = ElectriPayloads.class.getDeclaredMethod("onRFCableLimit", ElectriPayloads.RFCableLimit.class, IPayloadContext.class);
			handler.setAccessible(true);
			handler.invoke(null, new ElectriPayloads.RFCableLimit(abs, limit), ctx);
		}
		catch (ReflectiveOperationException e) {
			throw new IllegalStateException("could not send the cable limit", e);
		}
		p.closeMenu();
	}

	/**
	 * The steam plant (see buildPlant) drives a five-stage steam turbine and the reactor generator, which makes RF:
	 *  - five turbine cores (21..17, 5, 22) over the steam grate, set looking east (the steam moves west), with lubricant by a hose from a
	 *    barrel into the first stage's back; a turbine meter in the floor under the last stage.
	 *  - the generator multiblock west of it (rotor 16..8, generator 7, built on cobblestone props at y = 2): the player puts the generator
	 *    in looking east (it faces the turbine) and the rotor core at the turbine end last, looking along the rotor - it forms.
	 *  - the turbine's exhaust goes through the steam bypass over the generator, where a condenser on two cobblestone blocks catches it.
	 *  - RF: four RF cables from the generator's output down to the floor (network limit set in the cable GUI), an RF battery beside them
	 *    and a chain of the five wireless pads (superconducting, high-tech, advanced, improved, basic; each beams two blocks on into the
	 *    next) ending at a second RF battery.
	 * Checks: the receiver passed 800 C, the exchanger made warm sodium and the heat exchanger cooled it, the boiler boiled (steam at the
	 * grate), the turbine ran with power, the generator made RF, both batteries charged, the condenser filled with low pressure water and
	 * the meter found the turbine.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 4000, batch = "factory")
	public static void solarSteamPlantMakesRF(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 47, 47);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		Plant plant = buildPlant(h, p);

		BlockPos chest = new BlockPos(2, 2, 1);
		Factory.infiniteChest(h, chest, new ItemStack(ReactorBlocks.TURBINE_CORE.get()), new ItemStack(RotaryBlocks.HOSE.get()),
				new ItemStack(ReactorBlocks.TURBINE_METER.get()), new ItemStack(ReactorBlocks.GENERATOR_CORE.get()), new ItemStack(ReactorBlocks.GENERATOR_WINDING.get()),
				new ItemStack(ReactorBlocks.GENERATOR_HOUSING.get()), new ItemStack(ReactorBlocks.STEAM_BYPASS.get()), new ItemStack(ReactorBlocks.GENERATOR.get()),
				new ItemStack(ReactorBlocks.CONDENSER.get()), new ItemStack(ElectriBlocks.RF_CABLE.get()), new ItemStack(ElectriBlocks.RF_BATTERY.get()),
				new ItemStack(ElectriBlocks.wirelessPad(WirelessPadBlock.Tier.SUPERCONDUCTING).get()), new ItemStack(ElectriBlocks.wirelessPad(WirelessPadBlock.Tier.HIGHTECH).get()),
				new ItemStack(ElectriBlocks.wirelessPad(WirelessPadBlock.Tier.ADVANCED).get()), new ItemStack(ElectriBlocks.wirelessPad(WirelessPadBlock.Tier.IMPROVED).get()),
				new ItemStack(ElectriBlocks.wirelessPad(WirelessPadBlock.Tier.BASIC).get()), new ItemStack(Items.COBBLESTONE));

		// turbine with a meter in the floor under the last stage
		BlockPos head = buildTurbine(h, p, chest), last = head.west(4), meter = new BlockPos(last.getX(), 1, ZT);
		Factory.takeExactly(p, chest, ReactorBlocks.TURBINE_METER.get().asItem(), 1);
		p.mine(meter, Items.IRON_PICKAXE);
		p.place(meter, ReactorBlocks.TURBINE_METER.get(), Direction.EAST);

		// generator: props under its bottom row, then every part, the generator, and the first rotor core last
		BlockPos core0 = head.west(5), genPos = core0.west(9);
		Map<BlockPos, Block> bp = westward("generator", core0);
		props(p, chest, bp, Y-2);
		takeFor(p, chest, bp);
		Map<BlockPos, Block> rest = new LinkedHashMap<>(bp);
		rest.remove(core0);
		p.build(rest, List.of(genPos), Direction.EAST); //the generator looking east: it faces the turbine
		p.place(core0, ReactorBlocks.GENERATOR_CORE.get(), Direction.WEST); //looking along the rotor to the generator: it forms
		ReactorGeneratorBlockEntity gen = (ReactorGeneratorBlockEntity)h.getBlockEntity(genPos);
		h.assertTrue(gen.hasMultiBlock(), "the generator did not form (facing "+gen.getFacing()+")");

		// condenser over the steam bypass, on two cobblestone blocks
		BlockPos over = head.offset(-6, 3, 0), cond = over.above();
		Factory.take(p, chest, Items.COBBLESTONE, 2);
		p.place(over.south(), Items.COBBLESTONE, Direction.EAST);
		p.place(cond.south(), Items.COBBLESTONE, Direction.EAST);
		Factory.takeExactly(p, chest, ReactorBlocks.CONDENSER.get().asItem(), 1);
		p.placeSneaking(cond, new ItemStack(ReactorBlocks.CONDENSER.get()), Direction.EAST);

		// RF: cables down from the generator's output, a battery beside them, the pad chain north to a second battery
		Factory.takeExactly(p, chest, ElectriBlocks.RF_CABLE.get().asItem(), 4);
		Factory.takeExactly(p, chest, ElectriBlocks.RF_BATTERY.get().asItem(), 2);
		BlockPos out = genPos.west(), floorCable = new BlockPos(out.getX(), 2, ZT);
		for (int y = 2; y <= Y; y++)
			p.placeSneaking(new BlockPos(out.getX(), y, ZT), new ItemStack(ElectriBlocks.RF_CABLE.get()), Direction.EAST);
		BlockPos bat1 = floorCable.west();
		p.placeSneaking(bat1, new ItemStack(ElectriBlocks.RF_BATTERY.get()), Direction.EAST);
		WirelessPadBlock.Tier[] tiers = {WirelessPadBlock.Tier.SUPERCONDUCTING, WirelessPadBlock.Tier.HIGHTECH, WirelessPadBlock.Tier.ADVANCED,
				WirelessPadBlock.Tier.IMPROVED, WirelessPadBlock.Tier.BASIC};
		for (int i = 0; i < tiers.length; i++) {
			Block pad = ElectriBlocks.wirelessPad(tiers[i]).get();
			Factory.takeExactly(p, chest, pad.asItem(), 1);
			p.placeSneaking(floorCable.north(1+2*i), new ItemStack(pad), Direction.SOUTH); //looking south: it faces north
		}
		BlockPos bat2 = floorCable.north(1+2*tiers.length);
		p.placeSneaking(bat2, new ItemStack(ElectriBlocks.RF_BATTERY.get()), Direction.EAST);
		setCableLimit(p, floorCable, 10000);
		p.moveNear(new BlockPos(1, 2, 44)); //out of the way of the blades and the steam

		SolarTopBlockEntity top = (SolarTopBlockEntity)h.getBlockEntity(plant.tower().above(3));
		SolarTowerBlockEntity tw = (SolarTowerBlockEntity)h.getBlockEntity(plant.tower());
		SolarExchangerBlockEntity sx = (SolarExchangerBlockEntity)h.getBlockEntity(plant.exchanger());
		HeatExchangerBlockEntity hx = (HeatExchangerBlockEntity)h.getBlockEntity(plant.hx());
		ReactorBoilerBlockEntity boiler = (ReactorBoilerBlockEntity)h.getBlockEntity(plant.boiler());
		SteamGrateBlockEntity grate = (SteamGrateBlockEntity)h.getBlockEntity(plant.grate());
		TurbineCoreBlockEntity t0 = (TurbineCoreBlockEntity)h.getBlockEntity(head), t4 = (TurbineCoreBlockEntity)h.getBlockEntity(last);
		TurbineMeterBlockEntity tm = (TurbineMeterBlockEntity)h.getBlockEntity(meter);
		CondenserBlockEntity cd = (CondenserBlockEntity)h.getBlockEntity(cond);
		RFBatteryBlockEntity b1 = (RFBatteryBlockEntity)h.getBlockEntity(bat1), b2 = (RFBatteryBlockEntity)h.getBlockEntity(bat2);
		RFCableBlockEntity cable = (RFCableBlockEntity)h.getBlockEntity(floorCable);
		int[] peak = new int[6]; //receiver T, heat exchanger T, boiler T, turbine omega, generator RF/t, steam seen at grate
		boolean[] cooled = {false};
		long[] when = new long[6]; //first tick (for failure messages): receiver 800 C, cooled sodium, boiler 100 C, steam, RF, second battery
		h.onEachTick(() -> {
			long t = h.getTick();
			if (when[0] == 0 && top.getTemperature() >= 800) when[0] = t;
			if (when[1] == 0 && hx.getOutputTank().getFluidAmount() > 0) when[1] = t;
			if (when[2] == 0 && boiler.getTemperature() > 100) when[2] = t;
			if (when[3] == 0 && peak[5] > 0) when[3] = t;
			if (when[4] == 0 && gen.getGenUnits() > 0) when[4] = t;
			if (when[5] == 0 && b2.getStoredEnergy() > 0) when[5] = t;
			peak[0] = Math.max(peak[0], top.getTemperature());
			peak[1] = Math.max(peak[1], hx.getTemperature());
			peak[2] = Math.max(peak[2], boiler.getTemperature());
			peak[3] = Math.max(peak[3], t4.getOmega());
			peak[4] = Math.max(peak[4], (int)gen.getGenUnits());
			if (h.getBlockState(plant.grate().above()).getBlock() instanceof SteamBlock)
				peak[5]++;
			if (hx.getOutputTank().getFluidAmount() > 0)
				cooled[0] = true;
		});
		h.succeedWhen(() -> {
			String state = "receiver "+top.getTemperature()+" C (peak "+peak[0]+"), tower "+tw.getOmega()+" rad/s use "+tw.getCurrentConsumption()+" tank "+tw.getTank().getFluidAmount()
					+", exchanger "+sx.getOmega()+" rad/s "+sx.getPower()+" W tank "+sx.getTank().getFluidAmount()
					+", HX "+hx.getTemperature()+" C (peak "+peak[1]+") "+hx.getOmega()+" rad/s in "+hx.getTank().getFluidAmount()+" out "+hx.getOutputTank().getFluidAmount()
					+", boiler "+boiler.getTemperature()+" C (peak "+peak[2]+", ambient "+plant.ambient()+") water "+boiler.getTank().getFluidAmount()
					+", grate "+grate.getSteam()+" (steam seen "+peak[5]+"), turbine "+t0.getRawOmega()+"/"+t4.getOmega()+" rad/s "+t4.getPower()+" W lube "+t0.getLubricant()
					+", generator "+gen.getPower()+" W (peak "+peak[4]+" RF/t), cable limit "+cable.getRFLimit()+", batteries "+b1.getStoredEnergy()+"/"+b2.getStoredEnergy()
					+", condenser "+cd.getTank().getFluidAmount()+", meter "+tm.getTurbinePos()+", times "+java.util.Arrays.toString(when);
			h.assertTrue(peak[0] >= 800 && cooled[0], "solar loop: "+state);
			h.assertTrue(peak[5] > 0 && peak[2] > 100, "no steam yet: "+state);
			h.assertTrue(peak[3] > 0 && peak[4] > 0, "turbine/generator: "+state);
			h.assertTrue(b1.getStoredEnergy() > 0 && b2.getStoredEnergy() > 0, "RF storage: "+state);
			h.assertTrue(cd.getTank().getFluidAmount() > 0 && cd.getTank().getFluid().getFluid().isSame(ReactorFluids.get("low_pressure_water")), "condenser: "+state);
			h.assertTrue(h.absolutePos(last).equals(tm.getTurbinePos()), "meter: "+state);
			List<String> blocks = new ArrayList<>(PLANT);
			blocks.addAll(RF_PLANT);
			Factory.assertBuilt(h, blocks, new BlockPos(0, 1, 0), new BlockPos(47, 11, 47));
			p.leave();
		});
	}

	/**
	 * The steam plant (see buildPlant) with the same five-stage turbine, and:
	 *  - a turbine flywheel right behind the last stage, its 20-block ring (cores, dampeners, frame) around it on cobblestone props, the
	 *    top frame block last (it forms); a dynamometer behind the flywheel.
	 *  - a steam diffuser beside the last steam line before the grate, facing it: it turns ReactorCraft steam into the c:steam fluid.
	 *  - a two-stage high pressure turbine (blueprint slices 5-7: blades and housing around the two cores, the injector ring at the inlet)
	 *    on cobblestone props, the last housing block set looking east (it forms); a branch of steam lines from the plant ends in the
	 *    middle of its injector ring, and a hose from a lubricant barrel fills an injector.
	 * Checks: the plant made steam, the turbine and the formed flywheel turned and the dynamometer read speed and torque, the diffuser's
	 * tank holds steam fluid, the high pressure turbine formed and drew lubricant from its injectors. (It does not spin up here: it weighs
	 * solar steam at 0.15 and only accelerates on a line holding far more steam than this plant gives.)
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 4000, batch = "factory")
	public static void solarSteamDrivesFlywheel(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 47, 47);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		Plant plant = buildPlant(h, p);

		BlockPos chest = new BlockPos(2, 2, 1);
		Factory.infiniteChest(h, chest, new ItemStack(ReactorBlocks.TURBINE_CORE.get()), new ItemStack(RotaryBlocks.HOSE.get()),
				new ItemStack(ReactorBlocks.FLYWHEEL.get()), new ItemStack(ReactorBlocks.FLYWHEEL_CORE.get()), new ItemStack(ReactorBlocks.FLYWHEEL_DAMPENER.get()),
				new ItemStack(ReactorBlocks.FLYWHEEL_FRAME.get()), new ItemStack(RotaryBlocks.DYNAMOMETER.get()), new ItemStack(ReactorBlocks.STEAM_DIFFUSER.get()),
				new ItemStack(ReactorBlocks.STEAM_LINE.get()), new ItemStack(ReactorBlocks.HIP_TURBINE.get()), new ItemStack(ReactorBlocks.TURBINE_BLADES.get()),
				new ItemStack(ReactorBlocks.TURBINE_HOUSING.get()), new ItemStack(ReactorBlocks.STEAM_INJECTOR.get()), new ItemStack(Items.COBBLESTONE));
		BlockPos head = buildTurbine(h, p, chest), last = head.west(4);

		// flywheel ring in the plane behind the last stage
		BlockPos fw = last.west();
		Map<BlockPos, Block> ring = new LinkedHashMap<>();
		for (int dy = -2; dy <= 2; dy++)
			for (int dz = -2; dz <= 2; dz++) {
				int a = Math.abs(dy), b = Math.abs(dz);
				if (a == 2 && b == 2)
					continue;
				Block blk = a == 0 && b == 0 ? ReactorBlocks.FLYWHEEL.get() : a+b == 1 ? ReactorBlocks.FLYWHEEL_CORE.get()
						: a == 1 && b == 1 ? ReactorBlocks.FLYWHEEL_DAMPENER.get() : ReactorBlocks.FLYWHEEL_FRAME.get();
				ring.put(fw.offset(0, dy, dz), blk);
			}
		props(p, chest, ring, Y-2);
		takeFor(p, chest, ring);
		buildSneaking(h, p, ring, List.of(fw.above(2)), Direction.EAST); //the flywheel looking east: it faces the turbine
		Factory.takeExactly(p, chest, RotaryBlocks.DYNAMOMETER.get().asItem(), 1);
		p.placeSneaking(fw.west(), new ItemStack(RotaryBlocks.DYNAMOMETER.get()), Direction.EAST);

		// diffuser at the end of a one-line branch north of the last steam line (22, 3, 22), facing south to the branch: set straight beside
		// the main line it raced the grate for the same steam, and which of the two ticked first depended on where the test was placed
		BlockPos lastLine = plant.grate().east(), diffLine = lastLine.north(), diff = diffLine.north();
		Factory.takeExactly(p, chest, ReactorBlocks.STEAM_LINE.get().asItem(), 1);
		p.placeSneaking(diffLine, new ItemStack(ReactorBlocks.STEAM_LINE.get()), Direction.NORTH);
		Factory.takeExactly(p, chest, ReactorBlocks.STEAM_DIFFUSER.get().asItem(), 1);
		p.placeSneaking(diff, new ItemStack(ReactorBlocks.STEAM_DIFFUSER.get()), Direction.NORTH);

		// two-stage high pressure turbine along x at z = 30: cores at x = 17, 18, injector ring at x = 19 around the inlet line
		BlockPos hip = new BlockPos(17, Y, 30), inlet = hip.east(2);
		Map<BlockPos, Block> hp = new LinkedHashMap<>();
		for (int i = 5; i <= 7; i++) {
			String[] rows = reika.reactorcraft.content.block.TurbineMultiBlock.SLICES[i];
			for (int c = 0; c < rows[0].length(); c++)
				for (int k = 0; k < rows.length; k++) {
					char ch = rows[k].charAt(c);
					Block blk = ch == 't' ? ReactorBlocks.TURBINE_BLADES.get() : ch == 'h' ? ReactorBlocks.TURBINE_HOUSING.get()
							: ch == 'e' ? ReactorBlocks.STEAM_INJECTOR.get() : null;
					if (blk != null)
						hp.put(new BlockPos(hip.getX()+i-5, Y+5-k, hip.getZ()-5+c), blk);
				}
		}
		hp.put(hip, ReactorBlocks.HIP_TURBINE.get());
		hp.put(hip.east(), ReactorBlocks.HIP_TURBINE.get());
		BlockPos lastHousing = hip.east().above(2);
		props(p, chest, hp, Y-2);
		takeFor(p, chest, hp);
		buildSneaking(h, p, hp, List.of(lastHousing), Direction.EAST); //cores looking east: steam moves west; the last housing forms it
		// steam branch: from the last line south over the jet fuel barrel, west, and up into the injector ring
		List<BlockPos> branch = new ArrayList<>();
		for (int z = lastLine.getZ()+1; z <= inlet.getZ(); z++)
			branch.add(new BlockPos(lastLine.getX(), 3, z));
		for (int x = lastLine.getX()-1; x > inlet.getX(); x--)
			branch.add(new BlockPos(x, 3, inlet.getZ()));
		branch.add(new BlockPos(inlet.getX()+1, 4, inlet.getZ()));
		branch.add(new BlockPos(inlet.getX()+1, 5, inlet.getZ()));
		branch.add(inlet);
		Factory.take(p, chest, ReactorBlocks.STEAM_LINE.get().asItem(), branch.size());
		for (BlockPos q : branch)
			p.placeSneaking(q, new ItemStack(ReactorBlocks.STEAM_LINE.get()), Direction.EAST);
		BlockPos injector = inlet.above().south(), hose = injector.east();
		Factory.fluidBarrel(h, hose.above(), RotaryFluids.LUBRICANT.get());
		Factory.takeExactly(p, chest, RotaryBlocks.HOSE.get().asItem(), 1);
		p.placeSneaking(hose, new ItemStack(RotaryBlocks.HOSE.get()), Direction.EAST);
		p.moveNear(new BlockPos(1, 2, 44));

		SteamGrateBlockEntity grate = (SteamGrateBlockEntity)h.getBlockEntity(plant.grate());
		ReactorBoilerBlockEntity boiler = (ReactorBoilerBlockEntity)h.getBlockEntity(plant.boiler());
		TurbineCoreBlockEntity t4 = (TurbineCoreBlockEntity)h.getBlockEntity(last);
		ReactorFlywheelBlockEntity fly = (ReactorFlywheelBlockEntity)h.getBlockEntity(fw);
		DynamometerBlockEntity dyn = (DynamometerBlockEntity)h.getBlockEntity(fw.west());
		SteamDiffuserBlockEntity df = (SteamDiffuserBlockEntity)h.getBlockEntity(diff);
		HiPTurbineBlockEntity hipCore = (HiPTurbineBlockEntity)h.getBlockEntity(hip.east());
		SteamLineBlockEntity inLine = (SteamLineBlockEntity)h.getBlockEntity(inlet);
		int[] peak = new int[6]; //steam seen at grate, turbine omega, flywheel omega, dynamometer omega, dynamometer torque, inlet line steam
		long[] when = new long[3]; //first tick (for failure messages): steam, dynamometer turning, diffuser fluid
		h.onEachTick(() -> {
			if (h.getBlockState(plant.grate().above()).getBlock() instanceof SteamBlock)
				peak[0]++;
			peak[1] = Math.max(peak[1], t4.getOmega());
			peak[2] = Math.max(peak[2], fly.getOmega());
			peak[3] = Math.max(peak[3], dyn.getOmega());
			peak[4] = Math.max(peak[4], dyn.getTorque());
			peak[5] = Math.max(peak[5], inLine.getSteam());
			if (when[0] == 0 && peak[0] > 0) when[0] = h.getTick();
			if (when[1] == 0 && peak[3] > 0) when[1] = h.getTick();
			if (when[2] == 0 && df.getTank().getFluidAmount() > 0) when[2] = h.getTick();
		});
		h.succeedWhen(() -> {
			String state = "boiler "+boiler.getTemperature()+" C, grate "+grate.getSteam()+" (steam seen "+peak[0]+"), turbine "+t4.getOmega()+" (peak "+peak[1]
					+"), flywheel "+(fly.hasMultiBlock() ? "formed" : "not formed")+" facing "+fly.getFacing()+" "+fly.getOmega()+" (peak "+peak[2]+"), dynamometer peak "
					+peak[3]+" rad/s "+peak[4]+" Nm, diffuser "+df.getSteam()+" / "+df.getTank().getFluidAmount()+" mB, HiP "+(hipCore.hasMultiBlock() ? "formed" : "not formed")
					+" stages "+hipCore.getNumberStagesTotal()+" lube "+hipCore.getLubricant()+" steam "+hipCore.getSteam()+" omega "+hipCore.getRawOmega()
					+", inlet line peak "+peak[5]+", times "+java.util.Arrays.toString(when);
			h.assertTrue(fly.hasMultiBlock() && hipCore.hasMultiBlock() && hipCore.getNumberStagesTotal() == 2, "multiblocks: "+state);
			h.assertTrue(peak[0] > 0 && peak[1] > 0, "no steam turbine run yet: "+state);
			h.assertTrue(peak[2] > 0 && peak[3] > 0 && peak[4] > 0, "flywheel/dynamometer: "+state);
			h.assertTrue(df.getTank().getFluidAmount() > 0 && df.getTank().getFluid().getFluid().isSame(SteamDiffuserBlockEntity.getSteamFluid()), "diffuser: "+state);
			h.assertTrue(hipCore.getLubricant() > 0, "HiP lubricant: "+state);
			List<String> blocks = new ArrayList<>(PLANT);
			blocks.addAll(MECH_PLANT);
			Factory.assertBuilt(h, blocks, new BlockPos(0, 1, 0), new BlockPos(47, 11, 47));
			p.leave();
		});
	}
}
