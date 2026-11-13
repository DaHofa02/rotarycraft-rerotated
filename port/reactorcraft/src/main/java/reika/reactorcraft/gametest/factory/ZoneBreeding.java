/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft auto breeder built and run by a simulated survival player. Not part of the
 * original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.AutoBreederBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneBreeding {

	public static final List<String> BLOCKS = List.of("rotarycraft:auto_breeder");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines in a trench -> gas engine -> bevel gears -> auto breeder above them, the gears boxed in with glass.
	 * The player puts wheat in through the GUI and lets two sheep out of spawn eggs into a glass pen beside it: they are bred into a lamb.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 800, batch = "factory")
	public static void breederBreedsSheep(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.AUTO_BREEDER.get()), new ItemStack(Items.DIRT),
				new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.GLASS), new ItemStack(Items.WHEAT),
				new ItemStack(Items.SHEEP_SPAWN_EGG));
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.AUTO_BREEDER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		Factory.takeExactly(p, chest, Items.IRON_PICKAXE, 1);
		Factory.take(p, chest, Items.GLASS, 32);
		Factory.takeExactly(p, chest, Items.WHEAT, 8);
		Factory.takeExactly(p, chest, Items.SHEEP_SPAWN_EGG, 2);
		// the engine (6, 2, 4) on a fuel line in a trench, the breeder (5, 3, 4) on bevel gears beside it, one block above the sheep
		for (int x = 7; x >= 6; x--) {
			BlockPos t = new BlockPos(x, 1, 4);
			p.mine(t, Items.IRON_PICKAXE);
			p.place(t, RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		}
		Factory.fluidBarrel(h, new BlockPos(7, 2, 4), RotaryFluids.ETHANOL.get());
		p.placeSneaking(new BlockPos(6, 2, 4), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 4);
		p.place(new BlockPos(5, 2, 4), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.AUTO_BREEDER.get(), Direction.EAST);
		p.mine(new BlockPos(5, 2, 4), Items.IRON_SHOVEL);
		p.placeSneaking(new BlockPos(5, 2, 4), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //last: finds east -> up
		// glass over the gears' free faces (spinning gears hurt mobs) and a pen for the sheep west of them
		for (BlockPos g : new BlockPos[] {new BlockPos(5, 2, 3), new BlockPos(5, 2, 5), new BlockPos(4, 2, 4)})
			p.place(g, Items.GLASS, Direction.EAST);
		for (int i = 2; i <= 6; i++)
			for (int y = 2; y <= 3; y++) {
				p.placeSneaking(new BlockPos(3, y, i), new ItemStack(Items.GLASS), Direction.EAST);
				if (i >= 4 && i <= 5 || i == 3)
					continue;
				p.placeSneaking(new BlockPos(4, y, i), new ItemStack(Items.GLASS), Direction.EAST);
			}
		AutoBreederBlockEntity te = (AutoBreederBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(p.open(pos) != null, "auto breeder opens no GUI");
		p.putInto(0, Items.WHEAT, 8);
		p.closeMenu();
		p.hold(Items.SHEEP_SPAWN_EGG);
		p.useOn(new BlockPos(4, 1, 5), Direction.UP);
		p.hold(Items.SHEEP_SPAWN_EGG);
		p.useOn(new BlockPos(4, 1, 3), Direction.UP);

		h.succeedWhen(() -> {
			List<Sheep> sheep = h.getLevel().getEntitiesOfClass(Sheep.class, new AABB(h.absolutePos(new BlockPos(4, 2, 4))).inflate(10));
			h.assertTrue(sheep.size() >= 2, sheep.size()+" sheep let out");
			long lambs = sheep.stream().filter(Sheep::isBaby).count();
			h.assertTrue(lambs >= 1, "no lamb; "+te.power+" W, wheat "+te.getInventory().getStackInSlot(0).getCount());
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
