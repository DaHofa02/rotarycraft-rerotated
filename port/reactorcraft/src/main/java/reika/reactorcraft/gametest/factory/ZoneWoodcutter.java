/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft woodcutter built and run by a simulated survival player. Not part of the
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
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.WoodcutterBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneWoodcutter {

	public static final List<String> BLOCKS = List.of("rotarycraft:woodcutter");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> woodcutter (cutting west) on a chest. The player builds a small oak two blocks west of
	 * it (three logs on dirt, six leaves): the woodcutter cuts it and the logs end up in the chest.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void woodcutterFellsTheOak(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.WOODCUTTER.get()), new ItemStack(Items.CHEST), new ItemStack(Items.DIRT), new ItemStack(Items.OAK_LOG),
				new ItemStack(Items.OAK_LEAVES));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.WOODCUTTER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.CHEST, 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.OAK_LOG, 3);
		Factory.takeExactly(p, chest, Items.OAK_LEAVES, 6);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5);
		BlockPos box = pos.below();
		p.place(box, Items.CHEST, Direction.EAST);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.WOODCUTTER.get()), Direction.EAST);
		WoodcutterBlockEntity te = (WoodcutterBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(te.getBlockMetadata() == 0, "woodcutter meta "+te.getBlockMetadata());

		p.place(new BlockPos(3, 2, 5), Items.DIRT, Direction.EAST);
		for (int y = 3; y <= 5; y++)
			p.place(new BlockPos(3, y, 5), Items.OAK_LOG, Direction.EAST);
		BlockPos[] leaves = {new BlockPos(2, 5, 5), new BlockPos(4, 5, 5), new BlockPos(3, 5, 4), new BlockPos(3, 5, 6), new BlockPos(3, 6, 5),
				new BlockPos(2, 6, 5)};
		for (BlockPos l : leaves)
			p.place(l, Items.OAK_LEAVES, Direction.EAST);

		h.succeedWhen(() -> {
			h.assertTrue(te.getPower() == 65536, "woodcutter at "+te.getPower()+" W");
			for (int y = 3; y <= 5; y++)
				h.assertBlockPresent(Blocks.AIR, new BlockPos(3, y, 5));
			for (BlockPos l : leaves)
				h.assertBlockPresent(Blocks.AIR, l);
			ChestBlockEntity c = (ChestBlockEntity)h.getBlockEntity(box);
			int logs = 0;
			for (int i = 0; i < c.getContainerSize(); i++)
				if (c.getItem(i).is(Items.OAK_LOG))
					logs += c.getItem(i).getCount();
			h.assertTrue(logs == 3, logs+" logs in the chest");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
