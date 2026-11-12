/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft beam mirror built and run by a simulated survival player. Not part of the
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
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.BeamMirrorBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSunlight {

	public static final List<String> BLOCKS = List.of("rotarycraft:beam_mirror");

	private static final String PLATFORM = "platform_9x8x9";

	/** The player sets a beam mirror down facing east and a cobblestone wall five blocks on: the sunlight beam lights the way to it. */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "factory")
	public static void mirrorLightsAPath(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.BEAM_MIRROR.get()), new ItemStack(Items.COBBLESTONE));
		Factory.take(p, chest, RotaryBlocks.BEAM_MIRROR.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.COBBLESTONE, 1);
		BlockPos pos = new BlockPos(1, 2, 4);
		p.place(new BlockPos(6, 2, 4), Items.COBBLESTONE, Direction.EAST);
		p.place(pos, RotaryBlocks.BEAM_MIRROR.get(), Direction.EAST); //looking east: shines east
		BeamMirrorBlockEntity te = (BeamMirrorBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(te.getFacing() == Direction.EAST, "mirror faces "+te.getFacing());

		h.succeedWhen(() -> {
			h.assertTrue(te.getRange() == 5, "range "+te.getRange());
			for (int x = 3; x <= 5; x++)
				h.assertBlockPresent(Blocks.LIGHT, new BlockPos(x, 2, 4));
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
