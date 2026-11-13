/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft lawn sprinkler built and run by a simulated survival player. Not part of the
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
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.LawnSprinklerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneLawn {

	public static final List<String> BLOCKS = List.of("rotarycraft:lawn_sprinkler");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * A water barrel feeds a pipe that runs over to a pipe stub with the lawn sprinkler on top. The player tills two dirt blocks next to
	 * it and sows wheat: the sprinkler keeps the farmland wet.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 2000, batch = "factory")
	public static void sprinklerWatersAField(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.LAWN_SPRINKLER.get()), new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.DIRT),
				new ItemStack(Items.DIAMOND_HOE), new ItemStack(Items.WHEAT_SEEDS));
		Factory.take(p, chest, RotaryBlocks.LAWN_SPRINKLER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 3);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.takeExactly(p, chest, Items.DIAMOND_HOE, 1);
		Factory.takeExactly(p, chest, Items.WHEAT_SEEDS, 2);
		for (int x = 6; x >= 4; x--)
			p.place(new BlockPos(x, 2, 4), RotaryBlocks.PIPE.get(), Direction.EAST);
		Factory.fluidBarrel(h, new BlockPos(6, 3, 4), Fluids.WATER); //over the end of the pipe; a reservoir drains downwards only
		BlockPos pos = new BlockPos(4, 3, 4);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.LAWN_SPRINKLER.get()), Direction.EAST);
		BlockPos[] soil = {new BlockPos(3, 2, 4), new BlockPos(4, 2, 5)};
		for (BlockPos s : soil) {
			p.place(s, Items.DIRT, Direction.EAST);
			p.hold(Items.DIAMOND_HOE);
			p.useOn(s, Direction.UP);
			p.hold(Items.WHEAT_SEEDS);
			p.useOn(s, Direction.UP);
			h.assertBlockPresent(Blocks.WHEAT, s.above());
		}
		LawnSprinklerBlockEntity te = (LawnSprinklerBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(te.canPerformEffects(), "sprinkler idle: water "+te.getWater()+", range "+te.getRange());
			for (BlockPos s : soil)
				h.assertTrue(h.getBlockState(s).getValue(FarmBlock.MOISTURE) == FarmBlock.MAX_MOISTURE, "farmland at "+s+" at "+h.getBlockState(s).getValue(FarmBlock.MOISTURE));
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
