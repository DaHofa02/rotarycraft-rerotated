/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft multi-clutch built and run by a simulated survival player. Not part of the
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

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.GrinderBlockEntity;
import reika.rotarycraft.content.blockentity.MultiClutchBlockEntity;
import reika.rotarycraft.content.menu.MultiClutchMenu;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSwitching {

	public static final List<String> BLOCKS = List.of("rotarycraft:multi_clutch");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> multi-clutch -> grinder west of it. The multi-clutch sends everything down at first;
	 * the player opens its GUI and clicks the button of redstone level 0 four times (down, up, north, south, west): the grinder runs.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void multiClutchSwitchesToTheGrinder(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.MULTI_CLUTCH.get()), new ItemStack(RotaryBlocks.GRINDER.get()), new ItemStack(Items.DIRT));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.MULTI_CLUTCH.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.GRINDER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5), grinder = new BlockPos(4, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(grinder.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.MULTI_CLUTCH.get(), Direction.EAST); //looking east: takes its power from the east
		p.place(grinder, RotaryBlocks.GRINDER.get(), Direction.EAST);
		MultiClutchBlockEntity mc = (MultiClutchBlockEntity)h.getBlockEntity(pos);
		GrinderBlockEntity gr = (GrinderBlockEntity)h.getBlockEntity(grinder);

		h.assertTrue(p.open(pos) instanceof MultiClutchMenu, "multi-clutch opens no GUI: "+p.player.containerMenu);
		for (int i = 0; i < 4; i++)
			p.player.containerMenu.clickMenuButton(p.player, 0);
		p.closeMenu();
		h.assertTrue(MultiClutchBlockEntity.side(mc.getSideOfState(0)) == Direction.WEST, "level 0 sends "+MultiClutchBlockEntity.side(mc.getSideOfState(0)));

		h.succeedWhen(() -> {
			h.assertTrue(mc.omega == 512 && mc.getWriteDirection() == Direction.WEST, "multi-clutch at "+mc.omega+" rad/s, out "+mc.getWriteDirection());
			h.assertTrue(gr.omega == 512 && gr.torque == 128, "grinder at "+gr.omega+" rad/s @ "+gr.torque+" Nm");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
