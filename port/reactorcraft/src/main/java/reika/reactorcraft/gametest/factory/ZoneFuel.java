/*******************************************************************************
 * Practice factory zone (see Factory): RotaryCraft fluid lines and the engines and machines they feed, built and run by a simulated
 * survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.ReservoirBlockEntity;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFuel {

	public static final List<String> BLOCKS = List.of("rotarycraft:fuel_line", "rotarycraft:ecu", "rotarycraft:gas_engine", "rotarycraft:micro_engine",
			"rotarycraft:pipe", "rotarycraft:steam_engine", "rotarycraft:hose", "rotarycraft:reservoir", "rotarycraft:valve", "rotarycraft:bedrock_pipe");

	private static final String PLATFORM = "platform_9x8x9";

	private static SimPlayer start(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		return new SimPlayer(h, GameType.SURVIVAL);
	}

	/**
	 * Ethanol barrel (it gives only downwards) -> fuel lines -> ECU (accepts fuel from the side) -> gas engine on top of it -> dynamometer:
	 * the ECU on its default setting FULL passes the fuel up, the engine gives 512 rad/s at 128 Nm.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void fuelLineFeedsEngineThroughEcu(GameTestHelper h) {
		SimPlayer p = start(h);
		BlockPos chest = new BlockPos(1, 2, 7);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.ECU.get()),
				new ItemStack(RotaryBlocks.GAS_ENGINE.get()), new ItemStack(RotaryBlocks.DYNAMOMETER.get()));
		Factory.fluidBarrel(h, new BlockPos(7, 3, 4), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.ECU.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.DYNAMOMETER.get().asItem(), 1);
		for (int x = 7; x >= 5; x--)
			p.place(new BlockPos(x, 2, 4), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.place(new BlockPos(4, 2, 4), RotaryBlocks.ECU.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(4, 3, 4), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(3, 3, 4), new ItemStack(RotaryBlocks.DYNAMOMETER.get()), Direction.EAST);
		DynamometerBlockEntity dyna = (DynamometerBlockEntity)h.getBlockEntity(new BlockPos(3, 3, 4));
		h.succeedWhen(() -> {
			h.assertTrue(dyna.omega == 512 && dyna.torque == 128, "gas engine on the ECU: "+dyna.omega+" rad/s @ "+dyna.torque+" Nm");
			Factory.assertBuilt(h, List.of("rotarycraft:fuel_line", "rotarycraft:ecu", "rotarycraft:gas_engine"), new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}

	/** Jet fuel barrel -> fuel lines -> the bottom of a microturbine -> dynamometer: it runs at its 16 Nm and spins up. */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void fuelLineFeedsMicroturbine(GameTestHelper h) {
		SimPlayer p = start(h);
		BlockPos chest = new BlockPos(1, 2, 7);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()),
				new ItemStack(RotaryBlocks.DYNAMOMETER.get()));
		Factory.fluidBarrel(h, new BlockPos(7, 3, 4), RotaryFluids.JET_FUEL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 4);
		Factory.take(p, chest, RotaryBlocks.MICRO_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.DYNAMOMETER.get().asItem(), 1);
		for (int x = 7; x >= 4; x--)
			p.place(new BlockPos(x, 2, 4), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(4, 3, 4), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(3, 3, 4), new ItemStack(RotaryBlocks.DYNAMOMETER.get()), Direction.EAST);
		DynamometerBlockEntity dyna = (DynamometerBlockEntity)h.getBlockEntity(new BlockPos(3, 3, 4));
		h.succeedWhen(() -> {
			h.assertTrue(dyna.omega > 0 && dyna.torque == 16, "microturbine: "+dyna.omega+" rad/s @ "+dyna.torque+" Nm");
			Factory.assertBuilt(h, List.of("rotarycraft:fuel_line", "rotarycraft:micro_engine"), new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}

	/**
	 * Steam engine over a fire: the player digs out a floor block, puts netherrack in, sets the engine on a dirt block above it, digs the
	 * dirt away and lights the netherrack with flint and steel. Water from a barrel flows through liquid pipes into the engine's back
	 * (east); once the fire has heated it past boiling the engine runs at 512 rad/s, 32 Nm.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 3000, batch = "factory")
	public static void steamEngineOnFireAndPipedWater(GameTestHelper h) {
		SimPlayer p = start(h);
		BlockPos chest = new BlockPos(1, 2, 7);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(RotaryBlocks.STEAM_ENGINE.get()),
				new ItemStack(RotaryBlocks.DYNAMOMETER.get()), new ItemStack(Items.NETHERRACK), new ItemStack(Items.DIRT),
				new ItemStack(Items.FLINT_AND_STEEL), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_SHOVEL));
		Factory.take(p, chest, Items.IRON_PICKAXE, 1);
		Factory.take(p, chest, Items.IRON_SHOVEL, 1);
		Factory.take(p, chest, Items.NETHERRACK, 1);
		Factory.take(p, chest, Items.DIRT, 1);
		Factory.take(p, chest, Items.FLINT_AND_STEEL, 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 4);
		Factory.take(p, chest, RotaryBlocks.STEAM_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.DYNAMOMETER.get().asItem(), 1);
		BlockPos hearth = new BlockPos(2, 1, 4), eng = new BlockPos(2, 3, 4);
		p.mine(hearth, Items.IRON_PICKAXE);
		p.place(hearth, Items.NETHERRACK, Direction.EAST);
		p.place(hearth.above(), Items.DIRT, Direction.EAST);
		p.place(eng, RotaryBlocks.STEAM_ENGINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(1, 3, 4), new ItemStack(RotaryBlocks.DYNAMOMETER.get()), Direction.EAST);
		Factory.fluidBarrel(h, new BlockPos(6, 4, 4), Fluids.WATER);
		for (int x = 6; x >= 3; x--) //sneaking: the last one is set against the engine, which would take the click itself
			p.placeSneaking(new BlockPos(x, 3, 4), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.mine(hearth.above(), Items.IRON_SHOVEL);
		p.hold(Items.FLINT_AND_STEEL);
		p.useOn(hearth, Direction.UP);
		h.assertBlockPresent(net.minecraft.world.level.block.Blocks.FIRE, hearth.above());
		DynamometerBlockEntity dyna = (DynamometerBlockEntity)h.getBlockEntity(new BlockPos(1, 3, 4));
		h.succeedWhen(() -> {
			h.assertTrue(dyna.omega == 512 && dyna.torque == 32, "steam engine: "+dyna.omega+" rad/s @ "+dyna.torque+" Nm");
			Factory.assertBuilt(h, List.of("rotarycraft:pipe", "rotarycraft:steam_engine"), new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}

	/**
	 * DC engine -> dry steel 2:1 gearbox -> dynamometer (128 rad/s @ 8 Nm); a hose from a lubricant barrel reaches the gearbox's side and
	 * fills it.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void hoseLubricatesGearbox(GameTestHelper h) {
		SimPlayer p = start(h);
		BlockPos chest = new BlockPos(1, 2, 7);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.STEEL, 2);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.HOSE.get()), new ItemStack(RotaryBlocks.DC_ENGINE.get()), gbx,
				new ItemStack(RotaryBlocks.DYNAMOMETER.get()), new ItemStack(Items.REDSTONE_BLOCK));
		Factory.take(p, chest, RotaryBlocks.HOSE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.DC_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, gbx, 1);
		Factory.take(p, chest, RotaryBlocks.DYNAMOMETER.get().asItem(), 1);
		Factory.take(p, chest, Items.REDSTONE_BLOCK, 1);
		p.place(new BlockPos(6, 2, 4), RotaryBlocks.DC_ENGINE.get(), Direction.EAST);
		p.place(new BlockPos(6, 2, 5), Items.REDSTONE_BLOCK, Direction.EAST);
		p.place(new BlockPos(5, 2, 4), gbx, Direction.EAST);
		p.place(new BlockPos(4, 2, 4), RotaryBlocks.DYNAMOMETER.get(), Direction.EAST);
		GearboxBlockEntity g = (GearboxBlockEntity)h.getBlockEntity(new BlockPos(5, 2, 4));
		h.assertTrue(g.getLubricant() == 0, "a new gearbox already has lubricant");
		Factory.fluidBarrel(h, new BlockPos(5, 3, 1), RotaryFluids.LUBRICANT.get());
		p.place(new BlockPos(5, 2, 1), RotaryBlocks.HOSE.get(), Direction.EAST);
		p.place(new BlockPos(5, 2, 2), RotaryBlocks.HOSE.get(), Direction.EAST);
		p.place(new BlockPos(5, 2, 3), RotaryBlocks.HOSE.get(), Direction.EAST);
		DynamometerBlockEntity dyna = (DynamometerBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		h.succeedWhen(() -> {
			h.assertTrue(g.getLubricant() > 0, "gearbox got no lubricant");
			h.assertTrue(dyna.omega == 128 && dyna.torque == 8, "gearbox output "+dyna.omega+" rad/s @ "+dyna.torque+" Nm");
			Factory.assertBuilt(h, List.of("rotarycraft:hose"), new BlockPos(0, 2, 0), new BlockPos(8, 2, 8));
			p.leave();
		});
	}

	/**
	 * The player sets down a reservoir and fills it with water buckets from the barrel; under it a valve, then a bedrock pipe and a liquid
	 * pipe. Without a redstone signal the valve passes nothing; the player flips the lever next to it and the water reaches the pipes.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void reservoirValveAndPipes(GameTestHelper h) {
		SimPlayer p = start(h);
		BlockPos chest = new BlockPos(1, 2, 7);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.RESERVOIR.get()), new ItemStack(RotaryBlocks.VALVE.get()),
				new ItemStack(RotaryBlocks.BEDROCK_PIPE.get()), new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.BUCKET), new ItemStack(Items.LEVER));
		Factory.fluidBarrel(h, new BlockPos(7, 2, 7), Fluids.WATER);
		Factory.take(p, chest, RotaryBlocks.RESERVOIR.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.VALVE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEDROCK_PIPE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.BUCKET, 1);
		Factory.take(p, chest, Items.LEVER, 1);
		BlockPos valve = new BlockPos(4, 2, 4), res = valve.above();
		p.place(new BlockPos(5, 2, 4), RotaryBlocks.BEDROCK_PIPE.get(), Direction.EAST);
		p.place(new BlockPos(6, 2, 4), RotaryBlocks.PIPE.get(), Direction.EAST);
		p.place(valve, RotaryBlocks.VALVE.get(), Direction.EAST);
		p.placeSneaking(res, new ItemStack(RotaryBlocks.RESERVOIR.get()), Direction.EAST);
		p.place(new BlockPos(4, 2, 3), Items.LEVER, Direction.EAST);
		ReservoirBlockEntity r = (ReservoirBlockEntity)h.getBlockEntity(res);
		for (int i = 0; i < 4; i++) {
			p.use(new BlockPos(7, 2, 7), Items.BUCKET);
			p.use(res, Items.WATER_BUCKET);
		}
		h.assertTrue(r.getFluidAmount() == 4000, "reservoir holds "+r.getFluidAmount()+" mB after four buckets");
		PipingBlockEntity pipe = (PipingBlockEntity)h.getBlockEntity(new BlockPos(6, 2, 4));
		boolean[] on = {false};
		h.runAtTickTime(60, () -> {
			h.assertTrue(pipe.getFluidLevel() == 0 && r.getFluidAmount() == 4000, "water passed the closed valve: pipe "+pipe.getFluidLevel());
			p.emptyHand();
			p.use(new BlockPos(4, 2, 3));
			on[0] = true;
		});
		h.succeedWhen(() -> {
			h.assertTrue(on[0], "lever not flipped yet");
			h.assertTrue(pipe.getFluidLevel() > 0 && pipe.getFluidType() == Fluids.WATER, "pipe behind the valve: "+pipe.getFluidLevel()+" mB");
			Factory.assertBuilt(h, List.of("rotarycraft:reservoir", "rotarycraft:valve", "rotarycraft:bedrock_pipe", "rotarycraft:pipe"),
					new BlockPos(0, 2, 0), new BlockPos(8, 3, 8));
			p.leave();
		});
	}
}
