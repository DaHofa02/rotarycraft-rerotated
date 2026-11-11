/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft composter built and run by a simulated survival player. Not part of the
 * original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.ComposterBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneComposting {

	public static final List<String> BLOCKS = List.of("rotarycraft:composter");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * The player sets a composter on a dirt block, puts netherrack beside it and lights it with flint and steel: the fire warms the
	 * composter (ambient + 50 deg C) by 2 deg C per second. Wheat and yeast go in through the GUI (cobblestone is refused); from 40 deg C
	 * on the wheat becomes compost, one for one, which the player takes out.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 3000, batch = "factory")
	public static void composterMakesCompostBesideAFire(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Item compost = RotaryItems.part(CraftingPart.COMPOST);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.COMPOSTER.get()), new ItemStack(Items.DIRT), new ItemStack(Items.NETHERRACK),
				new ItemStack(Items.FLINT_AND_STEEL), new ItemStack(Items.WHEAT), new ItemStack(RotaryItems.YEAST.get()), new ItemStack(Items.COBBLESTONE));
		Factory.take(p, chest, RotaryBlocks.COMPOSTER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.NETHERRACK, 1);
		Factory.takeExactly(p, chest, Items.FLINT_AND_STEEL, 1);
		Factory.takeExactly(p, chest, Items.WHEAT, 4);
		Factory.takeExactly(p, chest, RotaryItems.YEAST.get(), 8);
		Factory.takeExactly(p, chest, Items.COBBLESTONE, 1);

		BlockPos pos = new BlockPos(4, 3, 4);
		BlockPos rack = new BlockPos(5, 2, 4);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.COMPOSTER.get(), Direction.EAST);
		p.place(rack, Items.NETHERRACK, Direction.EAST);
		p.hold(Items.FLINT_AND_STEEL);
		p.useOn(rack, Direction.UP);
		h.assertBlockPresent(Blocks.FIRE, rack.above());
		ComposterBlockEntity te = (ComposterBlockEntity)h.getBlockEntity(pos);

		p.open(pos);
		p.putInto(0, Items.COBBLESTONE, 1);
		h.assertTrue(te.getInventory().getStackInSlot(0).isEmpty() && p.count(Items.COBBLESTONE) == 1, "the composter took cobblestone");
		p.putInto(0, Items.WHEAT, 4);
		p.putInto(1, RotaryItems.YEAST.get(), 8);
		p.closeMenu();
		h.assertTrue(te.getInventory().getStackInSlot(0).getCount() == 4 && te.getInventory().getStackInSlot(1).getCount() == 8,
				"composter holds "+te.getInventory().getStackInSlot(0)+" and "+te.getInventory().getStackInSlot(1));
		h.assertTrue(te.getTemperature() < ComposterBlockEntity.MINTEMP && te.getCompostValue() == 0, "composter already at "+te.getTemperature()+" deg C");

		h.succeedWhen(() -> {
			h.assertBlockPresent(Blocks.FIRE, rack.above());
			h.assertTrue(te.getTemperature() >= ComposterBlockEntity.MINTEMP, "composter only at "+te.getTemperature()+" deg C");
			ItemStack out = te.getInventory().getStackInSlot(2);
			h.assertTrue(out.is(compost) && out.getCount() == 4 && te.getInventory().getStackInSlot(0).isEmpty(), "composter made "+out);
			h.assertTrue(te.getTemperature() <= ComposterBlockEntity.KILLTEMP, "composter at "+te.getTemperature()+" deg C");
			p.open(pos);
			p.shiftClick(2);
			p.closeMenu();
			h.assertTrue(p.count(compost) == 4 && te.getInventory().getStackInSlot(2).isEmpty(), "player has "+p.count(compost)+" compost");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
