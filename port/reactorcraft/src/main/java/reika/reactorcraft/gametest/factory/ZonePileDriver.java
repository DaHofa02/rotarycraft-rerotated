/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft pile driver built and run by a simulated survival player. Not part of the original
 * mods.
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
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.block.ShaftBlock;
import reika.rotarycraft.content.blockentity.PileDriverBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZonePileDriver {

	public static final List<String> BLOCKS = List.of("rotarycraft:pile_driver");

	/**
	 * On a stone bench (height 2-3) the player builds a gas turbine (fuel lines in a trench under a jet fuel barrel, a wall in its intake),
	 * two bedrock shafts, two bedrock 16:1 gearboxes and the pile driver (axis east-west): it smashes its way down through the bench.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 900, batch = "piledriverzone")
	public static void pileDriverBreaksTheBench(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 47, 47);
		for (int x = 2; x <= 22; x++)
			for (int z = 1; z <= 9; z++)
				for (int y = 2; y <= 3; y++)
					h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos a = new BlockPos(1, 2, 1);
		ItemStack shaft = ShaftBlock.shaftItem(MaterialRegistry.BEDROCK, false);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16);
		Factory.infiniteChest(h, a, new ItemStack(Items.IRON_PICKAXE), new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.JET_ENGINE.get()),
				new ItemStack(Items.COBBLESTONE_WALL), shaft, gbx, new ItemStack(RotaryBlocks.PILE_DRIVER.get()));
		Factory.take(p, a, Items.IRON_PICKAXE, 1);
		Factory.take(p, a, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, a, RotaryBlocks.JET_ENGINE.get().asItem(), 1);
		Factory.takeExactly(p, a, Items.COBBLESTONE_WALL, 1);
		Factory.take(p, a, shaft, 2);
		Factory.take(p, a, gbx, 2);
		Factory.take(p, a, RotaryBlocks.PILE_DRIVER.get().asItem(), 1);
		BlockPos jet = new BlockPos(18, 4, 5), pos = new BlockPos(13, 4, 5);
		for (BlockPos t : new BlockPos[] {jet.below(), jet.below().south()}) {
			p.mine(t, Items.IRON_PICKAXE);
			p.place(t, RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		}
		p.placeSneaking(jet, new ItemStack(RotaryBlocks.JET_ENGINE.get()), Direction.EAST);
		p.place(jet.east(), Items.COBBLESTONE_WALL, Direction.EAST);
		for (int x = 17; x >= 16; x--)
			p.placeSneaking(new BlockPos(x, 4, 5), shaft, Direction.EAST);
		p.placeSneaking(new BlockPos(15, 4, 5), gbx, Direction.EAST);
		p.placeSneaking(new BlockPos(14, 4, 5), gbx, Direction.EAST);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.PILE_DRIVER.get()), Direction.EAST); //looking east: axis east-west
		PileDriverBlockEntity te = (PileDriverBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(te.getBlockMetadata() == 1, "driver meta "+te.getBlockMetadata());
		Factory.fluidBarrel(h, jet.south(), RotaryFluids.JET_FUEL.get()); //last: now the turbine starts

		h.succeedWhen(() -> {
			h.assertTrue(te.step >= 3, "step "+te.step+" at "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			for (BlockPos b : new BlockPos[] {new BlockPos(13, 3, 5), new BlockPos(13, 2, 5), new BlockPos(11, 3, 5), new BlockPos(13, 2, 7)})
				h.assertBlockNotPresent(Blocks.STONE, b);
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(47, 6, 47));
			p.leave();
		});
	}
}
