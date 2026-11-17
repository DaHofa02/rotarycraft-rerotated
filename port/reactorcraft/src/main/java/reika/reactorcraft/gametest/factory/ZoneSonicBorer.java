/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft sonic borer built and run by a simulated survival player. Not part of the original
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
import reika.rotarycraft.content.blockentity.SonicBorerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSonicBorer {

	public static final List<String> BLOCKS = List.of("rotarycraft:sonic_borer");

	/**
	 * On a stone bench (height 3) the player builds a gas turbine (fuel lines in a trench under a jet fuel barrel, a cobblestone wall in
	 * its intake), three bedrock shafts, a bedrock 16:1 gearbox and the sonic borer facing west, then a dirt wall two blocks in front of
	 * it: the borer breaks through it.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 600, batch = "sonicborerzone")
	public static void sonicBorerDigsThroughAWall(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 47, 47);
		for (int x = 2; x <= 20; x++) //the bench the line stands on (terrain)
			for (int z = 2; z <= 8; z++)
				for (int y = 2; y <= 3; y++)
					h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos a = new BlockPos(1, 2, 1);
		ItemStack shaft = ShaftBlock.shaftItem(MaterialRegistry.BEDROCK, false);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16);
		Factory.infiniteChest(h, a, new ItemStack(Items.IRON_PICKAXE), new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.JET_ENGINE.get()),
				new ItemStack(Items.COBBLESTONE_WALL), shaft, gbx, new ItemStack(RotaryBlocks.SONIC_BORER.get()), new ItemStack(Items.DIRT));
		Factory.take(p, a, Items.IRON_PICKAXE, 1);
		Factory.take(p, a, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, a, RotaryBlocks.JET_ENGINE.get().asItem(), 1);
		Factory.takeExactly(p, a, Items.COBBLESTONE_WALL, 1);
		Factory.take(p, a, shaft, 3);
		Factory.take(p, a, gbx, 1);
		Factory.take(p, a, RotaryBlocks.SONIC_BORER.get().asItem(), 1);
		Factory.take(p, a, Items.DIRT, 9);
		BlockPos jet = new BlockPos(18, 4, 5), pos = new BlockPos(13, 4, 5);
		for (BlockPos t : new BlockPos[] {jet.below(), jet.below().south()}) {
			p.mine(t, Items.IRON_PICKAXE);
			p.place(t, RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		}
		p.placeSneaking(jet, new ItemStack(RotaryBlocks.JET_ENGINE.get()), Direction.EAST);
		p.place(jet.east(), Items.COBBLESTONE_WALL, Direction.EAST);
		for (int x = 17; x >= 15; x--)
			p.placeSneaking(new BlockPos(x, 4, 5), shaft, Direction.EAST);
		p.placeSneaking(new BlockPos(14, 4, 5), gbx, Direction.EAST);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.SONIC_BORER.get()), Direction.EAST); //looking east: input east, fires west
		SonicBorerBlockEntity te = (SonicBorerBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(te.getBlockMetadata() == 0, "borer meta "+te.getBlockMetadata());
		for (int z = 4; z <= 6; z++) //a dirt wall on the bench two blocks ahead, three wide and high
			for (int y = 4; y <= 6; y++)
				p.place(new BlockPos(11, y, z), Items.DIRT, Direction.EAST);
		Factory.fluidBarrel(h, jet.south(), RotaryFluids.JET_FUEL.get()); //last: now the turbine starts

		h.succeedWhen(() -> {
			for (int z = 4; z <= 6; z++)
				for (int y = 4; y <= 6; y++)
					h.assertBlockPresent(Blocks.AIR, new BlockPos(11, y, z));
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(47, 6, 47));
			p.leave();
		});
	}
}
