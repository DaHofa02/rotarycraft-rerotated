/*******************************************************************************
 * Practice factory zone (see Factory): two gas turbines (jet engines) driving the RotaryCraft bedrock breaker and extractor, built and
 * run by a simulated survival player. Not part of the original mods.
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.BedrockSliceBlock;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.block.ShaftBlock;
import reika.rotarycraft.content.blockentity.BedrockBreakerBlockEntity;
import reika.rotarycraft.content.blockentity.ExtractorBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneJetWorks {

	public static final List<String> BLOCKS = List.of("rotarycraft:jet_engine", "rotarycraft:bedrock_breaker", "rotarycraft:extractor");

	/**
	 * Two gas turbines, each on a fuel line the player lays into a trench in the floor under a jet fuel barrel, with a cobblestone wall in
	 * its intake (choked to a quarter: 16384 rad/s @ 1024 Nm; the wall also keeps the intake from sucking in what stands before it).
	 *  - z = 4 (turbine looking east, driving west): three bedrock shafts (out of the exhaust's heat), a bedrock 16:1 gearbox
	 *    (1024 rad/s @ 16384 Nm = 16.8 MW, above the breaker's 16384 Nm / 4.2 MW) into a bedrock breaker with a bedrock block the player
	 *    sets in front of it: it cuts a slice, grinds it level by level to nothing and puts the bedrock dust into the chest the player set
	 *    beside it; the player takes the dust out.
	 *  - z = 10 (turbine looking west, driving east): bevel gears turn the power up into an extractor (16384 rad/s and 1024 Nm run all four
	 *    stages) with water from a pipe at its side. The player puts iron ore in through its GUI; it goes through dust, slurry and solution
	 *    to iron flakes, which the player takes out and smelts in a furnace into iron.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 9000, batch = "factory")
	public static void bedrockBreakerAndExtractor(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 47, 47);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos a = new BlockPos(1, 2, 1), b = new BlockPos(2, 2, 1);
		ItemStack shaft = ShaftBlock.shaftItem(MaterialRegistry.BEDROCK, false);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16);
		Factory.infiniteChest(h, a, new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_SHOVEL), new ItemStack(RotaryBlocks.FUEL_LINE.get()),
				new ItemStack(RotaryBlocks.JET_ENGINE.get()), new ItemStack(Items.COBBLESTONE_WALL), shaft, gbx, new ItemStack(RotaryBlocks.BEDROCK_BREAKER.get()),
				new ItemStack(Items.BEDROCK), new ItemStack(Items.CHEST));
		Factory.infiniteChest(h, b, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.EXTRACTOR.get()), new ItemStack(Items.DIRT),
				new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.IRON_ORE), new ItemStack(Items.FURNACE), new ItemStack(Items.COAL));
		Factory.take(p, a, Items.IRON_PICKAXE, 1);
		Factory.take(p, a, Items.IRON_SHOVEL, 1);
		Factory.take(p, a, RotaryBlocks.FUEL_LINE.get().asItem(), 4);
		Factory.take(p, a, RotaryBlocks.JET_ENGINE.get().asItem(), 2);
		Factory.take(p, a, Items.COBBLESTONE_WALL, 2);

		// bedrock breaker line, z = 4
		Factory.take(p, a, shaft, 3);
		Factory.take(p, a, gbx, 1);
		Factory.take(p, a, RotaryBlocks.BEDROCK_BREAKER.get().asItem(), 1);
		Factory.take(p, a, Items.BEDROCK, 1);
		Factory.takeExactly(p, a, Items.CHEST, 1);
		BlockPos jet1 = new BlockPos(10, 2, 4), bb = new BlockPos(5, 2, 4), head = new BlockPos(4, 2, 4), box = new BlockPos(5, 2, 5);
		turbine(p, jet1, Direction.EAST);
		for (int x = 9; x >= 7; x--)
			p.placeSneaking(new BlockPos(x, 2, 4), shaft, Direction.EAST);
		p.placeSneaking(new BlockPos(6, 2, 4), gbx, Direction.EAST);
		p.placeSneaking(bb, new ItemStack(RotaryBlocks.BEDROCK_BREAKER.get()), Direction.EAST);
		p.placeSneaking(head, new ItemStack(Items.BEDROCK), Direction.EAST);
		p.placeSneaking(box, new ItemStack(Items.CHEST), Direction.EAST);
		Factory.fluidBarrel(h, jet1.south(), RotaryFluids.JET_FUEL.get()); //last: now the turbine starts

		// extractor line, z = 10
		Factory.take(p, b, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, b, RotaryBlocks.EXTRACTOR.get().asItem(), 1);
		Factory.take(p, b, Items.DIRT, 1);
		Factory.take(p, b, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.takeExactly(p, b, Items.IRON_ORE, 1);
		Factory.takeExactly(p, b, Items.FURNACE, 1);
		Factory.takeExactly(p, b, Items.COAL, 4);
		BlockPos jet2 = new BlockPos(4, 2, 10), ex = new BlockPos(5, 3, 10), furnace = new BlockPos(9, 2, 14);
		turbine(p, jet2, Direction.WEST);
		p.place(ex.below(), Items.DIRT, Direction.WEST);
		p.place(ex, RotaryBlocks.EXTRACTOR.get(), Direction.WEST);
		p.mine(ex.below(), Items.IRON_SHOVEL);
		p.placeSneaking(ex.below(), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.WEST); //last: finds its route west -> up
		Factory.fluidBarrel(h, ex.east().above(), Fluids.WATER);
		p.placeSneaking(ex.east(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.WEST);
		p.open(ex);
		p.putInto(0, Items.IRON_ORE, 1); //menu slot 0 = ore input
		p.closeMenu();
		p.place(furnace, Items.FURNACE, Direction.NORTH);
		Factory.fluidBarrel(h, jet2.south(), RotaryFluids.JET_FUEL.get());
		p.moveNear(new BlockPos(1, 2, 40)); //away from the turbines' intakes and exhausts

		JetEngineBlockEntity j1 = (JetEngineBlockEntity)h.getBlockEntity(jet1);
		JetEngineBlockEntity j2 = (JetEngineBlockEntity)h.getBlockEntity(jet2);
		BedrockBreakerBlockEntity breaker = (BedrockBreakerBlockEntity)h.getBlockEntity(bb);
		ExtractorBlockEntity e = (ExtractorBlockEntity)h.getBlockEntity(ex);
		Item flakes = RotaryItems.part(CraftingPart.IRONOREFLAKES), dust = RotaryItems.part(CraftingPart.BEDROCKDUST);
		boolean[] slice = {false}, smelting = {false}, ingot = {false}, gotDust = {false};
		h.onEachTick(() -> {
			BlockState st = h.getBlockState(head);
			if (st.is(RotaryBlocks.BEDROCK_SLICE.get()) && st.getValue(BedrockSliceBlock.LEVEL) >= 1)
				slice[0] = true; //the slice is ground away at the end, so it is noted while it is there
		});
		h.succeedWhen(() -> {
			h.assertTrue(j1.omega == 16384 && j2.omega == 16384, "turbines at "+j1.omega+" / "+j2.omega+" rad/s (fuel "+j1.getFuelLevel()+" / "+j2.getFuelLevel()+")");
			h.assertTrue(breaker.omega == 1024 && breaker.torque == 16384, "breaker input "+breaker.omega+" rad/s @ "+breaker.torque+" Nm");
			h.assertTrue(e.omega == 16384 && e.torque == 1024, "extractor input "+e.omega+" rad/s @ "+e.torque+" Nm");
			if (!smelting[0]) {
				for (int i = 0; i < 7; i++)
					h.assertTrue(e.getInventory().getStackInSlot(i).isEmpty(), "extractor still working: slot "+i+" "+e.getInventory().getStackInSlot(i)+", water "+e.getWater());
				ItemStack f = e.getInventory().getStackInSlot(7);
				h.assertTrue(f.is(flakes), "extractor made "+f);
				p.open(ex);
				p.shiftClick(7); //menu slot 7 = flakes
				p.closeMenu();
				int n = p.count(flakes);
				h.assertTrue(n >= 1 && n <= 16, "player took "+n+" iron flakes");
				p.open(furnace);
				p.putInto(0, flakes, n);
				p.putInto(1, Items.COAL, 4);
				p.closeMenu();
				p.moveNear(new BlockPos(1, 2, 40));
				smelting[0] = true;
			}
			if (!ingot[0]) {
				ItemStack out = ((FurnaceBlockEntity)h.getBlockEntity(furnace)).getItem(2);
				h.assertTrue(out.is(Items.IRON_INGOT), "furnace output "+out);
				p.open(furnace);
				p.shiftClick(2);
				p.closeMenu();
				p.moveNear(new BlockPos(1, 2, 40));
				ingot[0] = true;
			}
			h.assertTrue(p.count(Items.IRON_INGOT) >= 1, "no iron ingot taken from the furnace");
			h.assertTrue(slice[0], "no bedrock slice cut yet: head "+h.getBlockState(head)+", step "+breaker.getStep());
			if (!gotDust[0]) {
				ItemStack d = ((ChestBlockEntity)h.getBlockEntity(box)).getItem(0);
				h.assertTrue(d.is(dust), "no bedrock dust in the chest yet: head "+h.getBlockState(head)+", chest "+d);
				p.open(box);
				p.shiftClick(0);
				p.closeMenu();
				p.moveNear(new BlockPos(1, 2, 40));
				gotDust[0] = true;
			}
			h.assertTrue(p.count(dust) >= 1, "no bedrock dust taken");
			h.assertBlockPresent(Blocks.AIR, head);
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(12, 4, 16));
			p.leave();
		});
	}

	/**
	 * A turbine at {@code jet} looking {@code look} (it drives the other way, its intake faces {@code look}): two floor blocks dug out
	 * under it and south of it, fuel lines laid in; the jet fuel barrel goes onto the southern one later. A cobblestone wall goes into
	 * its intake.
	 */
	private static void turbine(SimPlayer p, BlockPos jet, Direction look) {
		for (BlockPos t : new BlockPos[] {jet.below(), jet.below().south()}) {
			p.mine(t, Items.IRON_PICKAXE);
			p.place(t, RotaryBlocks.FUEL_LINE.get(), look);
		}
		p.placeSneaking(jet, new ItemStack(RotaryBlocks.JET_ENGINE.get()), look);
		p.place(jet.relative(look), Items.COBBLESTONE_WALL, look);
	}
}
