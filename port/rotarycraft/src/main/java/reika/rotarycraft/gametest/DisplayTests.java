/*******************************************************************************
 * Tests for the ported RotaryCraft display screen (original TileEntityDisplay). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.DisplayBlock;
import reika.rotarycraft.content.blockentity.DisplayBlockEntity;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DisplayTests {

	private static final String PLATFORM = "platform_9x8x9";

	public static WrittenBookContent book(String... pages) {
		return new WrittenBookContent(Filterable.passThrough("Notes"), "Tester", 0,
				java.util.Arrays.stream(pages).map(p -> Filterable.<Component>passThrough(Component.literal(p))).toList(), true);
	}

	/** Pages are joined with a space unless a page already ends in one. */
	@GameTest(template = PLATFORM)
	public static void displayReadsBooks(GameTestHelper h) {
		String s = DisplayBlock.readBook(book("Hello", "World ", "again"));
		h.assertTrue(s.equals("Hello World again"), "message '"+s+"'");
		h.succeed();
	}

	/**
	 * Argon blue by default, a dye's colour after it. A coil with 2 charges: the screen is on; it counts two ticks per tick, so a charge
	 * lasts about 60 ticks - after 70 one is left, after 140 none and the screen is off. A block in the 5x3 space above blanks it.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void displayRunsOnACoil(GameTestHelper h) {
		BlockPos pos = new BlockPos(4, 2, 4);
		h.setBlock(pos, RotaryBlocks.DISPLAY.get());
		DisplayBlockEntity te = (DisplayBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(java.util.Arrays.equals(te.getScreenColor(), new int[] {0, 128, 255}) && te.isArgonBlue(), "not argon blue");
		te.setDyeColor(DyeColor.RED);
		h.assertTrue(java.util.Arrays.equals(te.getScreenColor(), new int[] {0x99, 0x33, 0x33}), "red is "+java.util.Arrays.toString(te.getScreenColor()));
		te.getInventory().setStackInSlot(0, CoilItem.withCharge(new ItemStack(RotaryItems.SPRING.get()), 2));
		te.setMessage("Test");
		h.runAtTickTime(70, () -> {
			h.assertTrue(te.canDisplay() && te.hasSpace() && te.getCoilCharge() == 1, "after 70 ticks: on "+te.canDisplay()+", charge "+te.getCoilCharge());
			h.setBlock(pos.offset(0, 3, 2), Blocks.STONE);
			h.assertFalse(te.hasSpace(), "space with a block above");
		});
		h.runAtTickTime(140, () -> {
			h.assertTrue(!te.canDisplay() && te.getCoilCharge() == 0, "after 140 ticks: on "+te.canDisplay()+", charge "+te.getCoilCharge());
			h.succeed();
		});
	}
}
