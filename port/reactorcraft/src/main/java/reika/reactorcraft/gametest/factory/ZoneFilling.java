/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft Block Filler built and run by a simulated survival player. Not part of the
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
import reika.rotarycraft.content.blockentity.BlockFillerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFilling {

	public static final List<String> BLOCKS = List.of("rotarycraft:filler");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * The player walls in a shaft two blocks deep with cobblestone, sets the filler on top (on a dirt pillar that is mined away again) and
	 * a DC engine with a redstone block beside it (256 rad/s @ 4 Nm = 1024 W, one block every (int)(22 - 2*log2(257)) = 5 ticks). Through
	 * the filler's GUI they put four oak planks into its slot (wood needs 1024 W): two close the shaft, two stay in the slot.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1200, batch = "factory")
	public static void fillerClosesAShaft(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(Items.COBBLESTONE), new ItemStack(Items.DIRT), new ItemStack(RotaryBlocks.FILLER.get()),
				new ItemStack(RotaryBlocks.DC_ENGINE.get()), new ItemStack(Items.REDSTONE_BLOCK), new ItemStack(Items.OAK_PLANKS));

		Factory.takeExactly(p, chest, Items.COBBLESTONE, 8);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.take(p, chest, RotaryBlocks.FILLER.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.DC_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, Items.REDSTONE_BLOCK, 1);
		Factory.takeExactly(p, chest, Items.OAK_PLANKS, 4);
		BlockPos shaft = new BlockPos(4, 2, 4);
		for (int y = 2; y <= 3; y++) {
			for (Direction d : Direction.Plane.HORIZONTAL)
				p.place(shaft.atY(y).relative(d), Items.COBBLESTONE, Direction.EAST);
			p.place(shaft.atY(y), Items.DIRT, Direction.EAST);
		}
		BlockPos filler = new BlockPos(4, 4, 4);
		p.place(filler, RotaryBlocks.FILLER.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(5, 4, 4), new ItemStack(RotaryBlocks.DC_ENGINE.get()), Direction.EAST); //meta 0: output west, into the filler
		p.placeSneaking(new BlockPos(5, 5, 4), new ItemStack(Items.REDSTONE_BLOCK), Direction.EAST);
		p.mine(shaft.atY(3));
		p.mine(shaft);
		h.assertTrue(p.count(Items.DIRT) == 2, "dirt pillar not taken back");

		h.assertTrue(p.open(filler) != null, "filler opens no GUI");
		p.putInto(0, Items.OAK_PLANKS, 4);
		p.closeMenu();
		BlockFillerBlockEntity f = (BlockFillerBlockEntity)h.getBlockEntity(filler);
		h.assertTrue(p.count(Items.OAK_PLANKS) == 0 && f.getInventory().getStackInSlot(0).getCount() == 4, "planks not in the slot: "+f.getInventory().getStackInSlot(0));

		h.succeedWhen(() -> {
			h.assertTrue(f.getPower() == 1024 && f.getRequiredPower() == 1024 && f.getOperationTime() == 5,
					"filler at "+f.getOmega()+" rad/s @ "+f.getTorque()+" Nm, needs "+f.getRequiredPower()+", time "+f.getOperationTime());
			for (int y = 2; y <= 3; y++)
				h.assertBlockPresent(Blocks.OAK_PLANKS, shaft.atY(y));
			h.assertTrue(f.getInventory().getStackInSlot(0).getCount() == 2 && f.getRemainingArea() == 0,
					f.getInventory().getStackInSlot(0)+" left, "+f.getRemainingArea()+" blocks to go");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 5, 8));
			p.leave();
		});
	}
}
