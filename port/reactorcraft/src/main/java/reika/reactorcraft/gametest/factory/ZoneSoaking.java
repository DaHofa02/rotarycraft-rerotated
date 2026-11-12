/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft wetter built and run by a simulated survival player. Not part of the original
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

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.WetterBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSoaking {

	public static final List<String> BLOCKS = List.of("rotarycraft:wetter");

	/**
	 * On the big platform (the wind engine needs about 32 clear blocks downwind): wind engine (1024 rad/s @ 8 Nm = 8192 W) -> bevel gears
	 * -> wetter above them (4096 W and 1024 rad/s from below). A lubricant barrel over a hose feeds it from the side (lubricant runs in hoses only). The player puts sand
	 * in through the GUI and takes out the soul sand.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 2000, batch = "factory")
	public static void wetterSoaksSand(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 47, 47);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(1, 2, 1);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.WIND_ENGINE.get()), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()),
				new ItemStack(RotaryBlocks.WETTER.get()), new ItemStack(RotaryBlocks.HOSE.get()), new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL),
				new ItemStack(Items.SAND));
		Factory.take(p, chest, RotaryBlocks.WIND_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.WETTER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.HOSE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 5);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		Factory.takeExactly(p, chest, Items.SAND, 1);

		BlockPos wind = new BlockPos(6, 3, 24), pos = new BlockPos(5, 4, 24), hose = new BlockPos(4, 4, 24);
		p.place(wind.below(), Items.DIRT, Direction.EAST);
		p.place(wind, RotaryBlocks.WIND_ENGINE.get(), Direction.EAST); //looks east: drives west, its rotor is east of it
		p.place(new BlockPos(5, 2, 24), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(5, 3, 24), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.WETTER.get(), Direction.EAST);
		p.place(new BlockPos(4, 2, 24), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(4, 3, 24), Items.DIRT, Direction.EAST);
		p.place(hose, RotaryBlocks.HOSE.get(), Direction.EAST);
		Factory.fluidBarrel(h, hose.above(), RotaryFluids.LUBRICANT.get()); //a reservoir drains downwards only
		p.mine(new BlockPos(5, 3, 24), Items.IRON_SHOVEL);
		p.placeSneaking(new BlockPos(5, 3, 24), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //last: finds east -> up
		WetterBlockEntity te = (WetterBlockEntity)h.getBlockEntity(pos);

		h.assertTrue(p.open(pos) != null, "wetter opens no GUI");
		p.putInto(0, Items.SAND, 1);
		p.closeMenu();
		h.assertTrue(te.getInventory().getStackInSlot(0).is(Items.SAND), "no sand in the wetter");

		h.succeedWhen(() -> {
			h.assertTrue(te.omega == 1024 && te.power >= 4096, "wetter at "+te.omega+" rad/s, "+te.power+" W");
			h.assertTrue(te.getInventory().getStackInSlot(0).is(Items.SOUL_SAND), "no soul sand yet: timer "+te.getTick()+", "+te.getTank().getFluidAmount()+" mB");
			p.open(pos);
			p.shiftClick(0);
			p.closeMenu();
			h.assertTrue(p.count(Items.SOUL_SAND) == 1 && te.getInventory().getStackInSlot(0).isEmpty(), "player has "+p.count(Items.SOUL_SAND)+" soul sand");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(47, 6, 47));
			p.leave();
		});
	}
}
