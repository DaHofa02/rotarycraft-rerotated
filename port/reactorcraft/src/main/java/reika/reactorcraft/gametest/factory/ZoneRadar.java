/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft mob radar built and run by a simulated survival player. Not part of the
 * original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.MobRadarBlockEntity;
import reika.rotarycraft.content.menu.MobRadarMenu;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneRadar {

	public static final List<String> BLOCKS = List.of("rotarycraft:mob_radar");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel line -> gas engine (65536 W) -> bevel gears (east -> up) -> mob radar: range 8 + (65536 - 8192)/1024 =
	 * 64. The player sets a cow out of a spawn egg; the radar lists the cow and the player, and its GUI opens.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1200, batch = "factory")
	public static void radarSeesCowAndBuilder(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.MOB_RADAR.get()), new ItemStack(Items.DIRT),
				new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.COW_SPAWN_EGG));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());

		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.MOB_RADAR.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		Factory.takeExactly(p, chest, Items.COW_SPAWN_EGG, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		// the radar on a dirt pillar, whose top block is dug away again and replaced by the bevel gears (placed last: east -> up)
		BlockPos radar = new BlockPos(5, 4, 5);
		p.place(new BlockPos(5, 2, 5), Items.DIRT, Direction.EAST);
		p.place(radar.below(), Items.DIRT, Direction.EAST);
		p.place(radar, RotaryBlocks.MOB_RADAR.get(), Direction.EAST);
		p.mine(radar.below(), Items.IRON_SHOVEL);
		p.placeSneaking(radar.below(), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		p.hold(Items.COW_SPAWN_EGG);
		p.useOn(new BlockPos(2, 1, 2), Direction.UP);
		h.assertTrue(p.count(Items.COW_SPAWN_EGG) == 0, "spawn egg not used");
		MobRadarBlockEntity te = (MobRadarBlockEntity)h.getBlockEntity(radar);
		p.join(); //from here on the player is in the world for the radar to find

		h.succeedWhen(() -> {
			h.assertTrue(te.power == 65536 && te.getRange() == 64, "radar at "+te.omega+" rad/s @ "+te.torque+" Nm, range "+te.getRange());
			h.assertTrue(te.getEntities().contains(p.player), "radar does not list its builder");
			BlockPos at = h.absolutePos(new BlockPos(4, 2, 4));
			h.assertTrue(te.getEntities().stream().anyMatch(e -> e instanceof Cow && e.blockPosition().closerThan(at, 24)), "radar lists no cow: "+te.getEntities());
			h.assertTrue(p.open(radar) instanceof MobRadarMenu, "radar opens no GUI");
			p.closeMenu();
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 5, 8));
			p.leave();
		});
	}
}
