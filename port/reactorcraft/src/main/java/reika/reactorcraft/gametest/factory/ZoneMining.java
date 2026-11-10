/*******************************************************************************
 * Factory zone: mining (RotaryCraft borer). Private NeoForge 1.21.1 port of RotaryCraft/ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.BorerBlockEntity;
import reika.rotarycraft.content.menu.BorerMenu;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneMining {

	public static final List<String> BLOCKS = List.of("rotarycraft:borer", "rotarycraft:mining_pipe");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel line -> gas engine (65536 W) -> borer digging west along z = 4. The player builds two dirt columns and a Borer
	 * Shield Block column in front of it, sets a chest on top and selects the centre cell in the GUI: the borer cuts the first slice whole,
	 * turns the two dirt blocks at its height into mining pipes (320 W, 6 Nm each), stores the dirt in the chest and jams at the shield.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 6000, batch = "factory")
	public static void borerDigsATunnel(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BORER.get()), new ItemStack(Items.DIRT), new ItemStack(RotaryBlocks.SHIELD_BLOCK.get()),
				new ItemStack(Items.CHEST));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 4), RotaryFluids.ETHANOL.get());

		Factory.takeExactly(p, chest, Items.DIRT, 4);
		Factory.takeExactly(p, chest, RotaryBlocks.SHIELD_BLOCK.get().asItem(), 2);
		for (int y = 2; y <= 3; y++) {
			p.place(new BlockPos(4, y, 4), Items.DIRT, Direction.EAST);
			p.place(new BlockPos(3, y, 4), Items.DIRT, Direction.EAST);
			p.place(new BlockPos(2, y, 4), RotaryBlocks.SHIELD_BLOCK.get(), Direction.EAST);
		}
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BORER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.CHEST, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 4), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 4), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos borerPos = new BlockPos(5, 3, 4);
		p.placeSneaking(borerPos, new ItemStack(RotaryBlocks.BORER.get()), Direction.EAST);
		BlockPos out = new BlockPos(5, 4, 4);
		p.placeSneaking(out, new ItemStack(Items.CHEST), Direction.EAST);
		BorerBlockEntity borer = (BorerBlockEntity)h.getBlockEntity(borerPos);
		h.assertTrue(borer.getBlockMetadata() == 0 && borer.getFacing() == Direction.WEST, "borer meta "+borer.getBlockMetadata()+" digs "+borer.getFacing());

		h.assertTrue(p.open(borerPos) instanceof BorerMenu, "the borer did not open its GUI");
		p.menu().clickMenuButton(p.player, 4*7+3); //the centre cell at the borer's own height
		p.closeMenu();
		h.assertTrue(borer.cutShape[3][4], "centre cell not selected");

		h.succeedWhen(() -> {
			h.assertTrue(borer.getPower() == 65536, "borer at "+borer.getOmega()+" rad/s @ "+borer.getTorque()+" Nm");
			h.assertBlockPresent(RotaryBlocks.MINING_PIPE.get(), new BlockPos(4, 3, 4));
			h.assertBlockPresent(RotaryBlocks.MINING_PIPE.get(), new BlockPos(3, 3, 4));
			h.assertBlockPresent(RotaryBlocks.MINING_PIPE.get(), new BlockPos(4, 7, 1)); //the first slice is cut whole
			h.assertBlockPresent(RotaryBlocks.SHIELD_BLOCK.get(), new BlockPos(2, 3, 4));
			h.assertTrue(borer.isJammed() && borer.getRequiredPower() == -1, "borer not jammed at the shield, step "+borer.getStep());
			int dirt = ((Container)h.getBlockEntity(out)).countItem(Items.DIRT);
			h.assertTrue(dirt == 2, dirt+" dirt in the chest");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 7, 8));
			p.leave();
		});
	}
}
