/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft defoliator set up by a simulated survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.DefoliatorBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneDefoliator {

	public static final List<String> BLOCKS = List.of("rotarycraft:defoliator");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine -> bevel gears (east -> up) -> defoliator on top. The player puts two potions of poison in
	 * through its GUI: it turns them into poison (the bottles come out below).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "defoliatorzone")
	public static void defoliatorTakesPoison(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack potion = PotionContents.createItemStack(Items.POTION, Potions.POISON);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), new ItemStack(RotaryBlocks.DEFOLIATOR.get()), new ItemStack(Items.DIRT),
				new ItemStack(Items.IRON_SHOVEL), potion);
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.BEVEL_GEARS.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.DEFOLIATOR.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 2);
		Factory.takeExactly(p, chest, Items.IRON_SHOVEL, 1);
		Factory.takeExactly(p, chest, Items.POTION, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos bevel = new BlockPos(5, 3, 5), pos = bevel.above();
		p.place(bevel.below(), Items.DIRT, Direction.EAST);
		p.place(bevel, Items.DIRT, Direction.EAST); //a support, dug out again
		p.place(pos, RotaryBlocks.DEFOLIATOR.get(), Direction.EAST);
		p.mine(bevel, Items.IRON_SHOVEL);
		p.placeSneaking(bevel, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST); //last: finds east -> up
		h.assertTrue(p.open(pos) != null, "defoliator opens no GUI");
		p.putInto(0, Items.POTION, 1);
		p.closeMenu();
		DefoliatorBlockEntity te = (DefoliatorBlockEntity)h.getBlockEntity(pos);

		boolean[] second = {false};
		h.succeedWhen(() -> {
			h.assertTrue(te.getPower() == 65536, "defoliator at "+te.getPower()+" W");
			if (!second[0]) {
				h.assertTrue(te.getPoison() == 1000 && te.getInventory().getStackInSlot(1).is(Items.GLASS_BOTTLE), "poison "+te.getPoison());
				Factory.takeExactly(p, chest, Items.POTION, 1); //potions do not stack: one at a time
				h.assertTrue(p.open(pos) != null, "defoliator opens no GUI");
				p.putInto(0, Items.POTION, 1);
				p.closeMenu();
				second[0] = true;
				h.fail("first potion in; now the second");
			}
			h.assertTrue(te.getPoison() == 2000 && te.getInventory().getStackInSlot(1).getCount() == 2, "poison "+te.getPoison());
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
			te.setPoison(0); //the structure stays: it would go on killing plants in other tests
		});
	}
}
