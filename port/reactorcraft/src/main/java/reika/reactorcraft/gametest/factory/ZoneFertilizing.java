/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft fertilizer built and run by a simulated survival player. Not part of the
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
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.FertilizerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFertilizing {

	public static final List<String> BLOCKS = List.of("rotarycraft:fertilizer");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * DC engine (with a redstone block) -> bevel gears -> fertilizer above them (range 4); a water barrel over a pipe on top of it. The
	 * player puts bone meal in through the GUI and plants an oak sapling beside it: the sapling grows on (to its next stage or a tree).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 2000, batch = "factory")
	public static void fertilizerGrowsASapling(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.DC_ENGINE.get()), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()),
				new ItemStack(RotaryBlocks.FERTILIZER.get()), new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.REDSTONE_BLOCK),
				new ItemStack(Items.DIRT), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.BONE_MEAL), new ItemStack(Items.OAK_SAPLING));
		Factory.take(p, chest, RotaryBlocks.DC_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.FERTILIZER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.REDSTONE_BLOCK, 1);
		Factory.takeExactly(p, chest, Items.DIRT, 3);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		Factory.takeExactly(p, chest, Items.BONE_MEAL, 16);
		Factory.takeExactly(p, chest, Items.OAK_SAPLING, 1);
		BlockPos dc = new BlockPos(3, 2, 4), pos = new BlockPos(4, 3, 4);
		p.place(dc, RotaryBlocks.DC_ENGINE.get(), Direction.WEST); //looking west: drives east
		p.place(new BlockPos(3, 2, 3), Items.REDSTONE_BLOCK, Direction.EAST);
		p.place(new BlockPos(4, 2, 4), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.FERTILIZER.get(), Direction.EAST);
		p.mine(new BlockPos(4, 2, 4), Items.IRON_SHOVEL);
		p.placeSneaking(new BlockPos(4, 2, 4), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //finds west -> up
		p.placeSneaking(pos.above(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		Factory.fluidBarrel(h, pos.above(2), Fluids.WATER);
		FertilizerBlockEntity te = (FertilizerBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(p.open(pos) != null, "fertilizer opens no GUI");
		p.putInto(0, Items.BONE_MEAL, 16);
		p.closeMenu();
		BlockPos soil = new BlockPos(6, 2, 4);
		p.place(soil, Items.DIRT, Direction.EAST);
		p.place(soil.above(), Items.OAK_SAPLING, Direction.EAST);
		h.assertBlockPresent(Blocks.OAK_SAPLING, soil.above());

		h.succeedWhen(() -> {
			h.assertTrue(te.power == 1024 && te.getTank().getFluidAmount() > 0, "fertilizer at "+te.power+" W with "+te.getTank().getFluidAmount()+" mB");
			boolean grown = h.getBlockState(soil.above()).is(Blocks.OAK_LOG)
					|| h.getBlockState(soil.above()).is(Blocks.OAK_SAPLING) && h.getBlockState(soil.above()).getValue(SaplingBlock.STAGE) == 1;
			h.assertTrue(grown, "sapling still "+h.getBlockState(soil.above()));
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
