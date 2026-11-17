/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft spawner controller put on a found spawner by a simulated survival player. Not part of
 * the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.SpawnerControllerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSpawnerController {

	public static final List<String> BLOCKS = List.of("rotarycraft:spawner_controller");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * A pig spawner found on a grass field. The player puts the spawner controller on it and a sport engine west of it (ethanol barrel on its
	 * fuel line, water barrel and pipe on its back, redstone as additive), and types a delay of 60 ticks: pigs come out.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 500, batch = "spawnerzone")
	public static void spawnerControllerBreedsPigs(GameTestHelper h) {
		for (int x = 0; x <= 8; x++)
			for (int z = 0; z <= 8; z++)
				h.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
		BlockPos spawner = new BlockPos(5, 2, 4), pos = spawner.above(), sport = spawner.west();
		h.setBlock(spawner, Blocks.SPAWNER);
		((SpawnerBlockEntity)h.getBlockEntity(spawner)).setEntityId(EntityType.PIG, h.getLevel().random);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.SPAWNER_CONTROLLER.get()), new ItemStack(RotaryBlocks.SPORT_ENGINE.get()),
				new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.REDSTONE), new ItemStack(Items.IRON_SHOVEL));
		Factory.take(p, chest, RotaryBlocks.SPAWNER_CONTROLLER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.SPORT_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.take(p, chest, Items.REDSTONE, 64);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		p.place(pos, RotaryBlocks.SPAWNER_CONTROLLER.get(), Direction.EAST);
		//the engine's fuel line runs in the ground under it to a barrel beside it
		for (BlockPos t : new BlockPos[] {sport.below(), sport.below().north()}) {
			p.mine(t, Items.IRON_SHOVEL);
			p.place(t, RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		}
		Factory.fluidBarrel(h, sport.north(), RotaryFluids.ETHANOL.get());
		p.placeSneaking(sport, new ItemStack(RotaryBlocks.SPORT_ENGINE.get()), Direction.WEST); //looking west: drives east, into the spawner
		Factory.fluidBarrel(h, sport.west().above(), Fluids.WATER);
		p.placeSneaking(sport.west(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		p.open(sport);
		p.putInto(1, Items.REDSTONE, 64);
		p.closeMenu();
		h.assertTrue(p.open(pos) != null, "spawner controller opens no GUI");
		p.player.containerMenu.clickMenuButton(p.player, 1+60); //what typing 60 into the delay field sends
		p.closeMenu();
		SpawnerControllerBlockEntity te = (SpawnerControllerBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(te.getSetDelay() == 60 && te.getPower() >= te.MINPOWER, "controller at "+te.getPower()+" W, delay "+te.getSetDelay());
			List<Pig> pigs = h.getLevel().getEntitiesOfClass(Pig.class, new AABB(h.absolutePos(spawner)).inflate(8));
			h.assertTrue(!pigs.isEmpty(), "no pig yet");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			pigs.forEach(Pig::discard);
			h.setBlock(pos, Blocks.AIR); //the structure stays: no more pigs for later tests
			p.leave();
		});
	}
}
