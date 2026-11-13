/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft bucket filler built and run by a simulated survival player. Not part of the
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
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.BucketFillerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneBuckets {

	public static final List<String> BLOCKS = List.of("rotarycraft:bucket_filler");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> bucket filler; a water barrel over a pipe on the filler's other side. The player puts
	 * two empty buckets in through the GUI and takes out two water buckets.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void fillerFillsTheBuckets(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BUCKET_FILLER.get()), new ItemStack(RotaryBlocks.PIPE.get()), new ItemStack(Items.DIRT), new ItemStack(Items.BUCKET));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BUCKET_FILLER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.PIPE.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.takeExactly(p, chest, Items.BUCKET, 2);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5), pipe = new BlockPos(4, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pipe.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.BUCKET_FILLER.get(), Direction.EAST);
		p.place(pipe, RotaryBlocks.PIPE.get(), Direction.EAST);
		Factory.fluidBarrel(h, pipe.above(), Fluids.WATER); //a reservoir drains downwards only
		BucketFillerBlockEntity te = (BucketFillerBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(p.open(pos) != null, "bucket filler opens no GUI");
		p.putInto(0, Items.BUCKET, 2);
		p.closeMenu();

		h.succeedWhen(() -> {
			int water = 0;
			for (int i = 0; i < te.getInventory().getSlots(); i++)
				if (te.getInventory().getStackInSlot(i).is(Items.WATER_BUCKET))
					water += te.getInventory().getStackInSlot(i).getCount();
			h.assertTrue(water == 2, water+" water buckets, "+te.getTank().getFluidAmount()+" mB, "+te.omega+" rad/s");
			p.open(pos);
			for (int i = 0; i < 18; i++)
				p.shiftClick(i);
			p.closeMenu();
			h.assertTrue(p.count(Items.WATER_BUCKET) == 2, "player has "+p.count(Items.WATER_BUCKET)+" water buckets");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
