/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft igniter built and run by a simulated survival player. Not part of the original
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
import reika.rotarycraft.content.blockentity.IgniterBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFirestarter {

	public static final List<String> BLOCKS = List.of("rotarycraft:firestarter");

	private static final String PLATFORM = "platform_9x8x9";

	/** Jet fuel barrel -> fuel lines -> microturbine -> igniter. The player puts coal in through the GUI: the igniter heats up past 280 C. */
	@GameTest(template = PLATFORM, timeoutTicks = 800, batch = "factory")
	public static void igniterBurnsCoal(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()),
				new ItemStack(RotaryBlocks.IGNITER.get()), new ItemStack(Items.DIRT), new ItemStack(Items.COAL));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.JET_FUEL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.MICRO_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.IGNITER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.COAL, 16);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.MICRO_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.IGNITER.get(), Direction.EAST);
		IgniterBlockEntity te = (IgniterBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(p.open(pos) != null, "igniter opens no GUI");
		p.putInto(0, Items.COAL, 16);
		p.closeMenu();
		p.leave(); //well away from the fire to come

		h.succeedWhen(() -> {
			h.assertTrue(te.getFuel() == IgniterBlockEntity.IgnitionFuel.COAL && te.getTemperature() >= 280, "igniter at "+te.getTemperature()+" C, fuel "+te.getFuel());
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
		});
	}
}
