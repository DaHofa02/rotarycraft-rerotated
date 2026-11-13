/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft sprinkler built and run by a simulated survival player. Not part of the
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
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.SprinklerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneSprinkling {

	public static final List<String> BLOCKS = List.of("rotarycraft:sprinkler");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * On a dirt pillar the player sets a sprinkler, a pipe on top of it and a water barrel over the pipe; around the pillar it tills dirt
	 * into farmland: the sprinkler waters it to full moisture.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1500, batch = "factory")
	public static void sprinklerWatersFarmland(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.SPRINKLER.get()), new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.DIRT),
				new ItemStack(Items.DIAMOND_HOE));
		Factory.take(p, chest, RotaryBlocks.SPRINKLER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 6);
		Factory.takeExactly(p, chest, Items.DIAMOND_HOE, 1);
		for (int y = 2; y <= 4; y++)
			p.place(new BlockPos(4, y, 4), Items.DIRT, Direction.EAST);
		BlockPos pos = new BlockPos(4, 5, 4);
		p.place(pos, RotaryBlocks.SPRINKLER.get(), Direction.EAST);
		p.placeSneaking(pos.above(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		Factory.fluidBarrel(h, pos.above(2), Fluids.WATER);
		BlockPos[] soil = {new BlockPos(2, 2, 4), new BlockPos(6, 2, 5), new BlockPos(4, 2, 7)};
		for (BlockPos s : soil) {
			p.place(s, Items.DIRT, Direction.EAST);
			p.hold(Items.DIAMOND_HOE);
			p.useOn(s, Direction.UP);
		}
		SprinklerBlockEntity te = (SprinklerBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(te.canPerformEffects(), "sprinkler idle: water "+te.getWater()+", range "+te.getRange());
			for (BlockPos s : soil)
				h.assertTrue(h.getBlockState(s).getValue(FarmBlock.MOISTURE) == FarmBlock.MAX_MOISTURE, "farmland at "+s+" at "+h.getBlockState(s).getValue(FarmBlock.MOISTURE));
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 7, 8));
			p.leave();
		});
	}
}
