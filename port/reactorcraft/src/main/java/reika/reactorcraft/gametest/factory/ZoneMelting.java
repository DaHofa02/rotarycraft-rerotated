/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft rock melter built and run by a simulated survival player. Not part of the
 * original mods.
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
import reika.rotarycraft.content.blockentity.LavaMakerBlockEntity;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.content.menu.LavaMakerMenu;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneMelting {

	public static final List<String> BLOCKS = List.of("rotarycraft:lava_maker");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel line -> gas engine (65536 W) -> bevel gears -> rock melter above them. The player puts four netherrack in
	 * through the GUI (and finds that dirt is not taken). The melter heats by log2(65536) = 16 deg C per second minus 1/64 of what it is
	 * above ambient; from 600 deg C it collects energy and melts a netherrack into 2000 mB of lava every 480000*20/65536 = 147 ticks,
	 * which a pipe at its side takes away.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 4000, batch = "factory")
	public static void melterTurnsNetherrackIntoLava(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.LAVA_MAKER.get()), new ItemStack(RotaryBlocks.PIPE.get()),
				new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.NETHERRACK));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());

		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.LAVA_MAKER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 5);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		Factory.takeExactly(p, chest, Items.NETHERRACK, 4);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		// the melter on a dirt pillar, whose top block is dug away again and replaced by the bevel gears (placed last: east -> up)
		BlockPos pos = new BlockPos(5, 4, 5);
		p.place(new BlockPos(5, 2, 5), Items.DIRT, Direction.EAST);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.LAVA_MAKER.get(), Direction.EAST);
		p.mine(pos.below(), Items.IRON_SHOVEL);
		p.placeSneaking(pos.below(), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		// a pipe west of it, on its own dirt pillar
		BlockPos pipe = new BlockPos(4, 4, 5);
		p.place(new BlockPos(4, 2, 5), Items.DIRT, Direction.EAST);
		p.place(new BlockPos(4, 3, 5), Items.DIRT, Direction.EAST);
		p.placeSneaking(pipe, new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);

		LavaMakerBlockEntity te = (LavaMakerBlockEntity)h.getBlockEntity(pos);
		PipingBlockEntity out = (PipingBlockEntity)h.getBlockEntity(pipe);
		h.assertTrue(p.open(pos) instanceof LavaMakerMenu, "melter opens no GUI");
		int dirt = p.count(Items.DIRT);
		p.putInto(4, Items.NETHERRACK, 4);
		p.putInto(0, Items.DIRT, 1);
		p.closeMenu();
		h.assertTrue(te.getInventory().getStackInSlot(4).getCount() == 4 && p.count(Items.NETHERRACK) == 0, "netherrack not put in");
		h.assertTrue(te.getInventory().getStackInSlot(0).isEmpty() && p.count(Items.DIRT) == dirt, "the melter took dirt");

		h.succeedWhen(() -> {
			h.assertTrue(te.power == 65536, "melter at "+te.omega+" rad/s @ "+te.torque+" Nm");
			h.assertTrue(te.getTemperature() >= 600, "melter at "+te.getTemperature()+" deg C");
			h.assertTrue(te.getInventory().getStackInSlot(4).getCount() <= 2, "only "+(4-te.getInventory().getStackInSlot(4).getCount())+" netherrack melted");
			h.assertTrue(out.getFluidLevel() > 0 && out.getFluidType() == Fluids.LAVA, "the pipe took nothing: "+out.getFluidLevel());
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 5, 8));
			p.leave();
		});
	}
}
