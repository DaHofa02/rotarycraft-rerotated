/*******************************************************************************
 * Practice factory zone (see Factory): the port's auto crafter set up by a simulated survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.AutoCrafterBlockEntity;
import reika.rotarycraft.content.item.CraftPatternItem;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneAutoCrafter {

	public static final List<String> BLOCKS = List.of("rotarycraft:auto_crafter");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine, the auto crafter west of it with a chest on top. The player draws a log into a crafting
	 * pattern's grid, puts the pattern into the crafter, switches it to continuous and fills the chest with logs: planks come out.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "factory")
	public static void autoCrafterMakesPlanks(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.AUTO_CRAFTER.get()), new ItemStack(Items.CHEST), new ItemStack(Items.DIRT), new ItemStack(Items.OAK_LOG),
				new ItemStack(RotaryItems.CRAFT_PATTERN.get()));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.AUTO_CRAFTER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.CHEST, 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.OAK_LOG, 9);
		Factory.take(p, chest, RotaryItems.CRAFT_PATTERN.get(), 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST); //looking east: drives west
		BlockPos pos = new BlockPos(5, 3, 5), box = pos.above();
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.AUTO_CRAFTER.get(), Direction.EAST);
		p.placeSneaking(box, new ItemStack(Items.CHEST), Direction.EAST); //sneaking: a plain click opens the crafter's GUI

		//the pattern: pick up the logs, click the first grid slot (only a copy goes there), put the logs back
		p.hold(RotaryItems.CRAFT_PATTERN.get());
		p.useItem();
		int logs = p.menuSlotOf(p.player.getInventory().findSlotMatchingItem(new ItemStack(Items.OAK_LOG)));
		p.click(logs, 0, ClickType.PICKUP);
		p.click(0, 0, ClickType.PICKUP);
		p.click(logs, 0, ClickType.PICKUP);
		p.closeMenu();
		ItemStack pat = p.player.getItemInHand(InteractionHand.MAIN_HAND);
		h.assertTrue(CraftPatternItem.getOutput(pat).is(Items.OAK_PLANKS) && p.count(Items.OAK_LOG) == 9, "pattern makes "+CraftPatternItem.getOutput(pat));

		h.assertTrue(p.open(pos) != null, "auto crafter opens no GUI");
		p.putInto(0, RotaryItems.CRAFT_PATTERN.get(), 1);
		p.player.containerMenu.clickMenuButton(p.player, 0); //mode button: request -> continuous
		p.closeMenu();
		h.assertTrue(p.open(box) != null, "chest opens no GUI");
		p.putInto(0, Items.OAK_LOG, 9);
		p.closeMenu();
		AutoCrafterBlockEntity te = (AutoCrafterBlockEntity)h.getBlockEntity(pos);
		ChestBlockEntity cb = (ChestBlockEntity)h.getBlockEntity(box);

		h.succeedWhen(() -> {
			h.assertTrue(te.getInventory().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET).getCount() == 36 && cb.getItem(0).isEmpty(),
					"planks "+te.getInventory().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET)+", mode "+te.getMode()+", "+te.getPower()+" W");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
