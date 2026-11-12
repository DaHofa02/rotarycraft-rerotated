/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft ground hydrator built and run by a simulated survival player. Not part of the
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
import reika.rotarycraft.content.blockentity.GroundHydratorBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneIrrigation {

	public static final List<String> BLOCKS = List.of("rotarycraft:hydrator");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * The player sets a ground hydrator into a small field, a pipe on top of it and a water barrel over the pipe (a reservoir drains
	 * downwards). Then the player tills the dirt around it and sows wheat: the hydrator keeps the farmland wet (moisture 7).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 4000, batch = "factory")
	public static void hydratorWatersAField(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.HYDRATOR.get()), new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.DIRT),
				new ItemStack(Items.DIAMOND_HOE), new ItemStack(Items.WHEAT_SEEDS));
		Factory.take(p, chest, RotaryBlocks.HYDRATOR.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		BlockPos pos = new BlockPos(4, 2, 4);
		BlockPos[] soil = {new BlockPos(5, 2, 4), new BlockPos(3, 2, 4), new BlockPos(4, 2, 5), new BlockPos(5, 2, 5)};
		Factory.takeExactly(p, chest, Items.DIRT, soil.length);
		Factory.takeExactly(p, chest, Items.DIAMOND_HOE, 1);
		Factory.takeExactly(p, chest, Items.WHEAT_SEEDS, soil.length);

		p.place(pos, RotaryBlocks.HYDRATOR.get(), Direction.EAST);
		p.placeSneaking(pos.above(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		Factory.fluidBarrel(h, pos.above(2), Fluids.WATER);
		for (BlockPos s : soil) {
			p.place(s, Items.DIRT, Direction.EAST);
			p.hold(Items.DIAMOND_HOE);
			p.useOn(s, Direction.UP);
			p.hold(Items.WHEAT_SEEDS);
			p.useOn(s, Direction.UP);
			h.assertBlockPresent(Blocks.WHEAT, s.above());
		}
		GroundHydratorBlockEntity te = (GroundHydratorBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(te.getWaterLevel() > 0, "no water in the hydrator");
			for (BlockPos s : soil) {
				h.assertBlockPresent(Blocks.FARMLAND, s);
				int m = h.getBlockState(s).getValue(FarmBlock.MOISTURE);
				h.assertTrue(m == FarmBlock.MAX_MOISTURE, "farmland at "+s+" at moisture "+m);
			}
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
