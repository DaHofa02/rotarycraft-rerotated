/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft distribution clutch built and set up by a simulated survival player. Not part of
 * the original mods.
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
import reika.rotarycraft.content.blockentity.DistributionClutchBlockEntity;
import reika.rotarycraft.content.blockentity.FanBlockEntity;
import reika.rotarycraft.content.menu.DistributionClutchMenu;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneDistribution {

	public static final List<String> BLOCKS = List.of("rotarycraft:distribution_clutch");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> distribution clutch (input east, through west), a fan north of it. In the GUI the
	 * player asks 64 Nm for north and switches north on: the fan runs at 512 rad/s @ 64 Nm, the other 64 Nm go on west.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void distributionClutchFeedsAFan(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.DISTRIBUTION_CLUTCH.get()), new ItemStack(RotaryBlocks.FAN.get()), new ItemStack(Items.DIRT));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.DISTRIBUTION_CLUTCH.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.FAN.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5), fanPos = new BlockPos(5, 3, 4);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(fanPos.below(), Items.DIRT, Direction.EAST);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.DISTRIBUTION_CLUTCH.get()), Direction.EAST); //looking east: input east
		p.placeSneaking(fanPos, new ItemStack(RotaryBlocks.FAN.get()), Direction.SOUTH); //input south, blows north
		DistributionClutchBlockEntity te = (DistributionClutchBlockEntity)h.getBlockEntity(pos);
		FanBlockEntity fan = (FanBlockEntity)h.getBlockEntity(fanPos);
		h.assertTrue(te.getBlockMetadata() == 0 && fan.getBlockMetadata() == 3, "clutch meta "+te.getBlockMetadata()+", fan meta "+fan.getBlockMetadata());
		h.assertTrue(p.open(pos) instanceof DistributionClutchMenu, "clutch opens no GUI");
		DistributionClutchMenu menu = (DistributionClutchMenu)p.player.containerMenu;
		h.assertTrue(menu.clickMenuButton(p.player, DistributionClutchMenu.requestButton(0, 64)), "request refused");
		h.assertTrue(menu.clickMenuButton(p.player, 0), "north switch refused");
		h.assertFalse(menu.clickMenuButton(p.player, 3), "east (the input) switched");
		p.closeMenu();

		h.succeedWhen(() -> {
			h.assertTrue(fan.getOmega() == 512 && fan.getTorque() == 64, "fan at "+fan.getOmega()+" rad/s @ "+fan.getTorque()+" Nm");
			h.assertTrue(te.getTorqueToSide(Direction.WEST) == 64, "west gets "+te.getTorqueToSide(Direction.WEST));
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
