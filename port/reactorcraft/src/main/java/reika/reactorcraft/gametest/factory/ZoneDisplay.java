/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft display screen set up by a simulated survival player. Not part of the original
 * mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.DisplayBlockEntity;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.gametest.DisplayTests;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneDisplay {

	public static final List<String> BLOCKS = List.of("rotarycraft:display");

	private static final String PLATFORM = "platform_9x8x9";

	/** The player sets down a display, puts a wound coil in, shows a written book on it and colours it green with a dye. */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "factory")
	public static void displayShowsABook(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, DisplayTests.book("RotaryCraft", "rules"));
		ItemStack coil = CoilItem.withCharge(new ItemStack(RotaryItems.SPRING.get()), 500);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.DISPLAY.get()), coil, book, new ItemStack(Items.GREEN_DYE));
		Factory.take(p, chest, RotaryBlocks.DISPLAY.get().asItem(), 1);
		Factory.takeExactly(p, chest, RotaryItems.SPRING.get(), 1);
		Factory.takeExactly(p, chest, Items.WRITTEN_BOOK, 1);
		Factory.takeExactly(p, chest, Items.GREEN_DYE, 1);
		BlockPos pos = new BlockPos(4, 2, 4);
		p.place(pos, RotaryBlocks.DISPLAY.get(), Direction.EAST);
		h.assertTrue(p.open(pos) != null, "display opens no GUI");
		p.putInto(0, RotaryItems.SPRING.get(), 1);
		p.closeMenu();
		p.use(pos, Items.WRITTEN_BOOK);
		p.use(pos, Items.GREEN_DYE);
		DisplayBlockEntity te = (DisplayBlockEntity)h.getBlockEntity(pos);
		h.succeedWhen(() -> {
			h.assertTrue(te.getMessage().equals("RotaryCraft rules"), "message '"+te.getMessage()+"'");
			h.assertTrue(!te.isArgonBlue() && te.getDyeColor() == DyeColor.GREEN, "colour "+te.getDyeColor());
			h.assertTrue(te.canDisplay() && te.hasSpace(), "screen off: coil "+te.getCoilCharge());
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
