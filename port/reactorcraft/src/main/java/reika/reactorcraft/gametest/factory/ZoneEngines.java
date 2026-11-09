/*******************************************************************************
 * Practice factory zone (see Factory): more RotaryCraft engines and the canola crop, built and run by a simulated survival player. Not
 * part of the original mods.
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
import reika.rotarycraft.content.block.CanolaBlock;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneEngines {

	public static final List<String> BLOCKS = List.of("rotarycraft:ac_engine", "rotarycraft:sport_engine", "rotarycraft:hydro_engine", "rotarycraft:canola");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * An AC engine with a magnetized steel shaft core put in through its GUI and the integrated redstone upgrade; a performance (sport) engine on a fuel
	 * line from an ethanol barrel, water piped into its back and redstone dust as additive in its GUI; a hydrokinetic engine beside a
	 * waterfall the player pours from a water bucket off a stone pillar, lubricated by a hose from a barrel. Each drives a dynamometer.
	 * And canola: the player digs a floor block out, puts dirt in, hoes it, plants canola seeds
	 * on the farmland made moist by water beside it (bone meal does not work on canola, as in the original).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600, batch = "factory")
	public static void enginesAndCanola(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos a = new BlockPos(0, 2, 8), b = new BlockPos(1, 2, 8);
		//the Magnetizer is not ported yet: the core comes magnetized out of the chest (the factory's only item source)
		ItemStack core = new ItemStack(RotaryItems.gearPart(GearboxTypes.STEEL, GearboxTypes.GearPart.SHAFTCORE));
		core.set(reika.rotarycraft.registry.RotaryComponents.MAGNETIZATION.get(), 1000);
		Factory.infiniteChest(h, a, new ItemStack(RotaryBlocks.AC_ENGINE.get()), core, new ItemStack(RotaryItems.UPGRADE_REDSTONE.get()),
				new ItemStack(RotaryBlocks.DYNAMOMETER.get()), new ItemStack(RotaryBlocks.SPORT_ENGINE.get()), new ItemStack(RotaryBlocks.FUEL_LINE.get()),
				new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.REDSTONE), new ItemStack(RotaryBlocks.HYDRO_ENGINE.get()),
				new ItemStack(RotaryBlocks.HOSE.get()), new ItemStack(Items.STONE), new ItemStack(Items.BUCKET));
		Factory.infiniteChest(h, b, new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.DIRT), new ItemStack(Items.IRON_HOE),
				new ItemStack(RotaryItems.CANOLA_SEEDS.get()), new ItemStack(Items.BONE_MEAL));

		// AC engine along z = 1
		Factory.take(p, a, RotaryBlocks.AC_ENGINE.get().asItem(), 1);
		Factory.takeExactly(p, a, core.getItem(), 1);
		Factory.takeExactly(p, a, RotaryItems.UPGRADE_REDSTONE.get(), 1);
		Factory.take(p, a, RotaryBlocks.DYNAMOMETER.get().asItem(), 3);
		BlockPos ac = new BlockPos(7, 2, 1);
		p.place(ac, RotaryBlocks.AC_ENGINE.get(), Direction.EAST);
		p.use(ac, RotaryItems.UPGRADE_REDSTONE.get()); //integrated redstone: the AC engine then runs without an alternating signal
		p.place(ac.west(), RotaryBlocks.DYNAMOMETER.get(), Direction.EAST);
		p.open(ac);
		p.putInto(0, core.getItem(), 1);
		p.closeMenu();

		// sport engine on a fuel line, z = 4
		Factory.take(p, a, RotaryBlocks.SPORT_ENGINE.get().asItem(), 1);
		Factory.take(p, a, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, a, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.take(p, a, Items.REDSTONE, 64);
		BlockPos sport = new BlockPos(6, 3, 4);
		p.place(new BlockPos(6, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.place(new BlockPos(6, 2, 4), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		Factory.fluidBarrel(h, new BlockPos(6, 3, 5), RotaryFluids.ETHANOL.get());
		p.placeSneaking(sport, new ItemStack(RotaryBlocks.SPORT_ENGINE.get()), Direction.EAST);
		Factory.fluidBarrel(h, new BlockPos(7, 4, 4), Fluids.WATER);
		p.placeSneaking(sport.east(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.placeSneaking(sport.west(), new ItemStack(RotaryBlocks.DYNAMOMETER.get()), Direction.EAST);
		p.open(sport);
		p.putInto(1, Items.REDSTONE, 64); //one additive unit per redstone, used up as it runs (starved, it runs as a gas engine)
		p.closeMenu();

		// hydrokinetic engine at (2, 3, 7), waterfall on its north side from a source three blocks above it, hose at its back
		Factory.take(p, a, RotaryBlocks.HYDRO_ENGINE.get().asItem(), 1);
		Factory.take(p, a, RotaryBlocks.HOSE.get().asItem(), 1);
		Factory.take(p, a, Items.STONE, 6);
		Factory.takeExactly(p, a, Items.BUCKET, 1);
		//one block up, so the water falls on past it; set on a stone that is dug away again (a block under it would stop the blades)
		BlockPos hydro = new BlockPos(2, 3, 7);
		Factory.take(p, b, Items.IRON_PICKAXE, 1);
		p.place(hydro.below(), Items.STONE, Direction.EAST);
		p.place(hydro, RotaryBlocks.HYDRO_ENGINE.get(), Direction.EAST);
		p.placeSneaking(hydro.west(), new ItemStack(RotaryBlocks.DYNAMOMETER.get()), Direction.EAST);
		Factory.fluidBarrel(h, hydro.east().above(), RotaryFluids.LUBRICANT.get());
		p.placeSneaking(hydro.east(), new ItemStack(RotaryBlocks.HOSE.get()), Direction.EAST);
		p.mine(hydro.below(), Items.IRON_PICKAXE);
		for (int y = 2; y <= 6; y++)
			p.place(new BlockPos(2, y, 5), Items.STONE, Direction.EAST);
		Factory.fluidBarrel(h, new BlockPos(4, 2, 7), Fluids.WATER);
		p.use(new BlockPos(4, 2, 7), Items.BUCKET);
		//a bucket pours where the player looks: from the south, eye level with the pillar's top block, looking north at it - the source
		//lands at (2, 6, 6) and falls past the engine's north side
		pour(h, p, new net.minecraft.world.phys.Vec3(2.5, 6.5, 8.5), 180, 0);
		h.assertTrue(h.getBlockState(new BlockPos(2, 6, 6)).getFluidState().isSource(), "no waterfall source: "+h.getBlockState(new BlockPos(2, 6, 6))+", hand "+p.player.getMainHandItem());

		// canola
		Factory.take(p, b, Items.DIRT, 1);
		Factory.take(p, b, Items.IRON_HOE, 1);
		Factory.take(p, b, RotaryItems.CANOLA_SEEDS.get(), 1);
		Factory.takeExactly(p, b, Items.BONE_MEAL, 1);
		BlockPos soil = new BlockPos(5, 1, 7);
		p.mine(soil, Items.IRON_PICKAXE);
		p.place(soil, Items.DIRT, Direction.EAST);
		p.hold(Items.IRON_HOE);
		p.useOn(soil, Direction.UP);
		h.assertBlockPresent(net.minecraft.world.level.block.Blocks.FARMLAND, soil);
		// canola needs moist farmland (isFertile, as in the original): water in a hole beside it; farmland only gets wet on a random tick
		// (about once a minute) - that tick is run here instead of waiting for it
		BlockPos wet = soil.east().above();
		Factory.takeExactly(p, a, Items.BUCKET, 1);
		p.use(new BlockPos(4, 2, 7), Items.BUCKET);
		pour(h, p, new net.minecraft.world.phys.Vec3(6.5, 4.5, 7.5), 0, 90); //looking straight down onto the floor beside the farmland
		h.assertTrue(h.getBlockState(wet).getFluidState().isSource(), "no water beside the farmland: "+h.getBlockState(wet));
		h.getBlockState(soil).randomTick(h.getLevel(), h.absolutePos(soil), h.getLevel().random);
		h.assertTrue(h.getBlockState(soil).getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE) > 0, "farmland still dry");
		//scooped back up at once (before it flows), or it would wash the crop away; the farmland stays moist for a while
		use(h, p, Items.BUCKET, new net.minecraft.world.phys.Vec3(6.5, 4.5, 7.5), 0, 90);
		h.assertTrue(h.getBlockState(wet).isAir(), "water not scooped up: "+h.getBlockState(wet));
		p.hold(RotaryItems.CANOLA_SEEDS.get());
		var r = p.useOn(soil, Direction.UP);
		h.assertTrue(h.getBlockState(soil.above()).is(RotaryBlocks.CANOLA.get()), "seeds not planted: "+r+", above "+h.getBlockState(soil.above())+", hand "+p.player.getMainHandItem());
		h.assertBlockPresent(RotaryBlocks.CANOLA.get(), soil.above());
		//bone meal does not work on canola (the original denied the BonemealEvent); it grows on random ticks only
		h.assertTrue(p.use(soil.above(), Items.BONE_MEAL) != net.minecraft.world.InteractionResult.SUCCESS
				&& h.getBlockState(soil.above()).getValue(CanolaBlock.AGE) == 0, "bone meal made canola grow");

		DynamometerBlockEntity dac = (DynamometerBlockEntity)h.getBlockEntity(ac.west());
		DynamometerBlockEntity dsp = (DynamometerBlockEntity)h.getBlockEntity(sport.west());
		DynamometerBlockEntity dhy = (DynamometerBlockEntity)h.getBlockEntity(hydro.west());
		h.succeedWhen(() -> {
			h.assertTrue(dac.omega == 256 && dac.torque == 512, "AC engine: "+dac.omega+" rad/s @ "+dac.torque+" Nm");
			h.assertTrue(dsp.omega == 1024 && dsp.torque == 256, "sport engine: "+dsp.omega+" rad/s @ "+dsp.torque+" Nm");
			h.assertTrue(dhy.omega > 0 && dhy.torque > 0, "hydro engine: "+dhy.omega+" rad/s @ "+dhy.torque+" Nm; north of it "+h.getBlockState(hydro.north())+", lube "+((reika.rotarycraft.content.blockentity.engine.EngineBlockEntity)h.getBlockEntity(hydro)).getLube()+", meta "+((reika.rotarycraft.content.blockentity.engine.EngineBlockEntity)h.getBlockEntity(hydro)).getBlockMetadata());
			h.assertBlockPresent(RotaryBlocks.CANOLA.get(), soil.above());
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 3, 8));
			p.leave();
		});
	}

	/** Pours the held water bucket the way a player does: from that eye position (relative), looking yaw/pitch, right-clicking. */
	private static void pour(GameTestHelper h, SimPlayer p, net.minecraft.world.phys.Vec3 eye, float yaw, float pitch) {
		use(h, p, Items.WATER_BUCKET, eye, yaw, pitch);
	}

	/** Uses the held item into the air the way a player does: from that eye position (relative), looking yaw/pitch. */
	private static void use(GameTestHelper h, SimPlayer p, net.minecraft.world.item.Item item, net.minecraft.world.phys.Vec3 eye, float yaw, float pitch) {
		net.minecraft.world.phys.Vec3 a = net.minecraft.world.phys.Vec3.atLowerCornerOf(h.absolutePos(BlockPos.ZERO)).add(eye);
		p.hold(item);
		p.player.moveTo(a.x, a.y-p.player.getEyeHeight(), a.z, yaw, pitch);
		p.player.setYHeadRot(yaw);
		p.useItem();
	}
}
