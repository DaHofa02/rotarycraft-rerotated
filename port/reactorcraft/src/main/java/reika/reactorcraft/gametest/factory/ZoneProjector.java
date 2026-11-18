/*******************************************************************************
 * Practice factory zone (see Factory): the port's projector set up by a simulated survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.ProjectorBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneProjector {

	public static final List<String> BLOCKS = List.of("rotarycraft:projector");

	private static final String PLATFORM = "platform_9x8x9";

	/** Ethanol barrel -> fuel lines -> gas engine -> projector looking west at a stone wall; the player puts a map in: it shows it. */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void projectorShowsTheMap(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack map = MapItem.create(h.getLevel(), 0, 0, (byte)0, false, false);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.PROJECTOR.get()), new ItemStack(Items.DIRT), new ItemStack(Items.STONE), map);
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PROJECTOR.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.STONE, 2);
		Factory.take(p, chest, map, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST); //looking east: drives west
		BlockPos pos = new BlockPos(5, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.PROJECTOR.get()), Direction.EAST); //input from the engine to the east: projects west
		p.place(new BlockPos(1, 2, 5), Items.STONE, Direction.EAST);
		p.place(new BlockPos(1, 3, 5), Items.STONE, Direction.EAST); //the screen, 4 blocks west
		h.assertTrue(p.open(pos) != null, "projector opens no GUI");
		p.putInto(0, Items.FILLED_MAP, 1);
		p.closeMenu();
		ProjectorBlockEntity te = (ProjectorBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(te.canShow() && te.getRange() == 4, "projector "+te.getFacing()+", range "+te.getRange()+", "+te.getPower()+" W, map "+te.getInventory().getStackInSlot(0));
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
