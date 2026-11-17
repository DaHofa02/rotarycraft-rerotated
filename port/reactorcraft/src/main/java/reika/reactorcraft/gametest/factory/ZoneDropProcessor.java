/*******************************************************************************
 * Practice factory zone (see Factory): the port's drop processor built and fed by a simulated survival player. Not part of the original mods.
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
import reika.rotarycraft.content.blockentity.DropProcessorBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneDropProcessor {

	public static final List<String> BLOCKS = List.of("rotarycraft:drop_processor");

	private static final String PLATFORM = "platform_9x8x9";

	/** Ethanol barrel -> fuel lines -> gas engine -> drop processor beside it; the player puts a coal ore block in through the GUI: coal comes out. */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void dropProcessorMinesOre(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.DROP_PROCESSOR.get()), new ItemStack(Items.DIRT), new ItemStack(Items.COAL_ORE));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.DROP_PROCESSOR.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.COAL_ORE, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST); //looking east: drives west
		BlockPos pos = new BlockPos(5, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.placeSneaking(pos, new ItemStack(RotaryBlocks.DROP_PROCESSOR.get()), Direction.EAST);
		h.assertTrue(p.open(pos) != null, "drop processor opens no GUI");
		p.putInto(0, Items.COAL_ORE, 1);
		p.closeMenu();
		DropProcessorBlockEntity te = (DropProcessorBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(te.getInventory().getStackInSlot(1).is(Items.COAL), "output "+te.getInventory().getStackInSlot(1)+" (meta "+te.getBlockMetadata()+", "+te.getPower()+" W)");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
