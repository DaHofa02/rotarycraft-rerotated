/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft GPR built and used by a simulated survival player. Not part of the original mods.
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
import reika.rotarycraft.content.blockentity.GprBlockEntity;
import reika.rotarycraft.content.menu.GprMenu;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneGpr {

	public static final List<String> BLOCKS = List.of("rotarycraft:gpr");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> GPR on a dirt block. Its radar shows the dirt right below; in the GUI the player moves
	 * the scan point one east ('['), where a fuel line lies below.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void gprShowsTheFuelLine(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.GPR.get()), new ItemStack(Items.DIRT));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.GPR.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.GPR.get(), Direction.EAST);
		GprBlockEntity te = (GprBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(te.getGuiDirection() == Direction.EAST, "GUI direction "+te.getGuiDirection());

		boolean[] moved = {false};
		h.succeedWhen(() -> {
			if (!moved[0]) {
				h.assertTrue(te.getBlock(0, 1) != null && te.getBlock(0, 1).is(Blocks.DIRT), "below: "+te.getBlock(0, 1)+" at "+te.getPower()+" W");
				h.assertTrue(p.open(pos) instanceof GprMenu, "GPR opens no GUI");
				h.assertTrue(p.player.containerMenu.clickMenuButton(p.player, 0), "'[' refused");
				p.closeMenu();
				moved[0] = true;
				h.fail("moved; waiting for the next scan");
			}
			h.assertTrue(te.getBlock(0, 1) != null && te.getBlock(0, 1).is(RotaryBlocks.FUEL_LINE.get()), "after moving: "+te.getBlock(0, 1));
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
