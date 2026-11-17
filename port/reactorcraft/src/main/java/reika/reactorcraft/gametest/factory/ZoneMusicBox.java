/*******************************************************************************
 * Practice factory zone (see Factory): the port's music box built and programmed by a simulated survival player. Not part of the original mods.
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
import reika.rotarycraft.content.blockentity.MusicBoxBlockEntity;
import reika.rotarycraft.content.menu.MusicBoxMenu;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneMusicBox {

	public static final List<String> BLOCKS = List.of("rotarycraft:music_box");

	private static final String PLATFORM = "platform_9x8x9";

	/** Ethanol barrel -> fuel lines -> gas engine -> music box beside it; the player keys in four notes of a scale on two channels: it plays them. */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void musicBoxPlaysAScale(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.MUSIC_BOX.get()), new ItemStack(Items.DIRT));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.MUSIC_BOX.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST); //looking east: drives west
		BlockPos pos = new BlockPos(5, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.MUSIC_BOX.get(), Direction.EAST);
		h.assertTrue(p.open(pos) != null, "music box opens no GUI");
		//the keyboard buttons send these: C and E on channel 1 (guitar), G and C on channel 2 (bass), quarter notes
		int[][] keys = {{0, 24, 1}, {0, 28, 1}, {1, 31, 2}, {1, 36, 2}};
		for (int[] k : keys)
			p.player.containerMenu.clickMenuButton(p.player, MusicBoxMenu.noteButton(k[0], MusicBoxBlockEntity.NoteLength.QUARTER, k[1],
					MusicBoxBlockEntity.Instrument.values()[k[2]]));
		p.closeMenu();
		MusicBoxBlockEntity te = (MusicBoxBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(te.getChannel(0).size() == 2 && te.getChannel(1).size() == 2, "notes entered "+te.getChannel(0).size()+"/"+te.getChannel(1).size());
			h.assertTrue(te.notesPlayed >= 4, "played "+te.notesPlayed+" at "+te.getPower()+" W");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
