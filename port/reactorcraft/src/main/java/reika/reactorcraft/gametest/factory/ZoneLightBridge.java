/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft light bridge built and run by a simulated survival player. Not part of the
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
import reika.rotarycraft.content.blockentity.LightBridgeBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneLightBridge {

	public static final List<String> BLOCKS = List.of("rotarycraft:light_bridge", "rotarycraft:bridge");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * A gas turbine on two fuel lines in a trench under a jet fuel barrel, choked by a cobblestone wall in its intake (16777216 W), drives
	 * a light bridge with glowstone on top: a bridge of hard-light plates across the zone to the west.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600, batch = "factory")
	public static void lightBridgeSpansTheZone(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.JET_ENGINE.get()),
				new ItemStack(Items.COBBLESTONE_WALL), new ItemStack(RotaryBlocks.LIGHT_BRIDGE.get()), new ItemStack(Items.GLOWSTONE),
				new ItemStack(Items.IRON_PICKAXE));
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 2);
		Factory.take(p, chest, RotaryBlocks.JET_ENGINE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.COBBLESTONE_WALL, 1);
		Factory.take(p, chest, RotaryBlocks.LIGHT_BRIDGE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.GLOWSTONE, 1);
		Factory.takeExactly(p, chest, Items.IRON_PICKAXE, 1);
		BlockPos jet = new BlockPos(7, 2, 4), pos = new BlockPos(6, 2, 4);
		for (BlockPos t : new BlockPos[] {jet.below(), jet.below().south()}) {
			p.mine(t, Items.IRON_PICKAXE);
			p.place(t, RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		}
		p.placeSneaking(jet, new ItemStack(RotaryBlocks.JET_ENGINE.get()), Direction.EAST);
		p.place(jet.east(), Items.COBBLESTONE_WALL, Direction.EAST);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.LIGHT_BRIDGE.get()), Direction.EAST);
		p.place(pos.above(), Items.GLOWSTONE, Direction.EAST);
		LightBridgeBlockEntity te = (LightBridgeBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(te.getBlockMetadata() == 0 && te.getFacing() == Direction.WEST, "light bridge meta "+te.getBlockMetadata()+" faces "+te.getFacing());
		Factory.fluidBarrel(h, jet.south(), RotaryFluids.JET_FUEL.get()); //last: now the turbine starts

		h.succeedWhen(() -> {
			h.assertTrue(te.getRange() == 32, "light bridge at "+te.getPower()+" W, range "+te.getRange());
			for (int x = 0; x <= 5; x++)
				h.assertBlockPresent(RotaryBlocks.BRIDGE.get(), new BlockPos(x, 2, 4));
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
