/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft floodlight built and run by a simulated survival player. Not part of the
 * original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.FloodlightBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFloodlight {

	public static final List<String> BLOCKS = List.of("rotarycraft:floodlight");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> floodlight shining west along z = 5: a line of light blocks to the zone's edge. Then
	 * the player puts a lens in: a cone (3x3 two blocks ahead).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void floodlightLightsTheZone(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack lens = new ItemStack(RotaryItems.part(CraftingPart.LENS));
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.FLOODLIGHT.get()), lens);
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.FLOODLIGHT.get().asItem(), 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.FLOODLIGHT.get()), Direction.EAST);
		FloodlightBlockEntity te = (FloodlightBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(te.getBlockMetadata() == 0 && te.getFacing() == Direction.WEST, "floodlight meta "+te.getBlockMetadata()+" shines "+te.getFacing());

		boolean[] lensIn = {false};
		h.succeedWhen(() -> {
			h.assertTrue(te.getPower() == 65536, "floodlight at "+te.getPower()+" W");
			if (!lensIn[0]) {
				for (int x = 0; x <= 4; x++)
					h.assertBlockPresent(Blocks.LIGHT, new BlockPos(x, 3, 5));
				Factory.takeExactly(p, chest, lens.getItem(), 1);
				p.use(pos, lens.getItem());
				lensIn[0] = true;
				h.assertTrue(te.fresnel && p.count(lens.getItem()) == 0, "lens not installed");
				h.fail("line lit; now the cone");
			}
			h.assertBlockPresent(Blocks.LIGHT, new BlockPos(3, 2, 4));
			h.assertBlockPresent(Blocks.LIGHT, new BlockPos(3, 4, 6));
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
