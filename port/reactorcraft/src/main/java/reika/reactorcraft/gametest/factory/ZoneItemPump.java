/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft item pump (blower) built and run by a simulated survival player. Not part of the
 * original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.BlowerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneItemPump {

	public static final List<String> BLOCKS = List.of("rotarycraft:blower");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> item pump between two chests (the engine beside the pump). The player fills the
	 * first chest with cobblestone: the pump moves it into the other.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void pumpMovesAChest(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BLOWER.get()), new ItemStack(Items.CHEST), new ItemStack(Items.COBBLESTONE));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BLOWER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.CHEST, 3);
		Factory.takeExactly(p, chest, Items.COBBLESTONE, 32);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST); //drives west into the pump
		BlockPos pump = new BlockPos(5, 3, 5), from = new BlockPos(5, 3, 6), to = new BlockPos(5, 3, 4);
		p.place(new BlockPos(5, 2, 5), net.minecraft.world.item.Items.CHEST, Direction.EAST); //support under the pump
		p.placeSneaking(pump, new ItemStack(RotaryBlocks.BLOWER.get()), Direction.NORTH);
		BlowerBlockEntity te = (BlowerBlockEntity)h.getBlockEntity(pump);
		h.assertTrue(te.getFacingDir() == Direction.NORTH, "pump meta "+te.getBlockMetadata()+" faces "+te.getFacingDir());
		p.placeSneaking(from, new ItemStack(Items.CHEST), Direction.NORTH);
		p.placeSneaking(to, new ItemStack(Items.CHEST), Direction.NORTH);
		p.open(from);
		p.putInto(0, Items.COBBLESTONE, 32);
		p.closeMenu();

		h.succeedWhen(() -> {
			h.assertTrue(((Container)h.getBlockEntity(from)).isEmpty() && ((Container)h.getBlockEntity(to)).countItem(Items.COBBLESTONE) == 32, "chests not swapped; "+te.power+" W");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
