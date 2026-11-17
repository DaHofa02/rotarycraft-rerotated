/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft chunk loader built by a simulated survival player. Not part of the original mods.
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
import reika.rotarycraft.content.blockentity.ChunkLoaderBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneChunkLoader {

	public static final List<String> BLOCKS = List.of("rotarycraft:chunk_loader");

	/**
	 * On a stone bench the player builds a gas turbine (fuel lines in a trench under a jet fuel barrel, a wall in its intake), two bedrock
	 * 16:1 gearboxes, switched to speed-up with a sneaking screwdriver click, bevel gears and the chunk loader on top: it loads its chunks.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 600, batch = "chunkloaderzone")
	public static void chunkLoaderKeepsTheFactoryAwake(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 47, 47);
		for (int x = 2; x <= 22; x++)
			for (int z = 1; z <= 9; z++)
				for (int y = 2; y <= 3; y++)
					h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos a = new BlockPos(1, 2, 1);
		ItemStack gbx = GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16);
		Factory.infiniteChest(h, a, new ItemStack(Items.IRON_PICKAXE), new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.JET_ENGINE.get()),
				new ItemStack(Items.COBBLESTONE_WALL), gbx, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.CHUNK_LOADER.get()),
				new ItemStack(Items.DIRT), new ItemStack(RotaryItems.SCREWDRIVER.get()));
		Factory.take(p, a, Items.IRON_PICKAXE, 1);
		Factory.take(p, a, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, a, RotaryBlocks.JET_ENGINE.get().asItem(), 1);
		Factory.takeExactly(p, a, Items.COBBLESTONE_WALL, 1);
		Factory.take(p, a, gbx, 2);
		Factory.take(p, a, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, a, RotaryBlocks.CHUNK_LOADER.get().asItem(), 1);
		Factory.takeExactly(p, a, Items.DIRT, 1);
		Factory.take(p, a, RotaryItems.SCREWDRIVER.get(), 1);
		BlockPos jet = new BlockPos(18, 4, 5), bevel = new BlockPos(15, 4, 5), pos = bevel.above();
		for (BlockPos t : new BlockPos[] {jet.below(), jet.below().south()}) {
			p.mine(t, Items.IRON_PICKAXE);
			p.place(t, RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		}
		p.placeSneaking(jet, new ItemStack(RotaryBlocks.JET_ENGINE.get()), Direction.EAST);
		p.place(jet.east(), Items.COBBLESTONE_WALL, Direction.EAST);
		for (int x = 17; x >= 16; x--) {
			BlockPos g = new BlockPos(x, 4, 5);
			p.placeSneaking(g, gbx, Direction.EAST);
			p.hold(RotaryItems.SCREWDRIVER.get());
			p.player.setShiftKeyDown(true);
			p.use(g);
			p.player.setShiftKeyDown(false);
			h.assertFalse(((GearboxBlockEntity)h.getBlockEntity(g)).reduction, "gearbox at "+g+" still a reduction");
		}
		p.place(bevel, Items.DIRT, Direction.EAST); //a support, dug out again
		p.place(pos, RotaryBlocks.CHUNK_LOADER.get(), Direction.EAST);
		p.mine(bevel, Items.IRON_PICKAXE);
		p.placeSneaking(bevel, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //last: finds east -> up
		ChunkLoaderBlockEntity te = (ChunkLoaderBlockEntity)h.getBlockEntity(pos);
		Factory.fluidBarrel(h, jet.south(), RotaryFluids.JET_FUEL.get()); //last: now the turbine starts

		h.succeedWhen(() -> {
			h.assertTrue(te.isLoading() && te.getLoadedRadius() == 8, "radius "+te.getLoadedRadius()+" at "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(47, 6, 47));
			h.setBlock(pos, Blocks.AIR); //the structure stays: let the 289 chunks go
			p.leave();
		});
	}
}
