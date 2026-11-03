/*******************************************************************************
 * Tests for ReactorCraft's crafting materials made in RotaryCraft machines (original RecipesBlastFurnace.addAlloyingRecipe calls).
 * Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.content.blockentity.BlastFurnaceBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ReactorCraftingTests {

	private static final String PLATFORM = "platform_9x4x9";

	/** Holds the furnace at temp[0] every tick. */
	private static BlastFurnaceBlockEntity furnace(GameTestHelper h, int[] temp) {
		BlockPos p = new BlockPos(4, 1, 4);
		h.setBlock(p, RotaryBlocks.BLAST_FURNACE.get());
		BlastFurnaceBlockEntity bf = (BlastFurnaceBlockEntity)h.getBlockEntity(p);
		h.onEachTick(() -> bf.setTemperature(temp[0]));
		return bf;
	}

	/** Cd-In-Ag alloy: one cadmium, indium and silver ingot anywhere in the grid at 1600 C give 3 alloy ingots. */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void alloyInBlastFurnace(GameTestHelper h) {
		BlastFurnaceBlockEntity bf = furnace(h, new int[] {1650});
		var inv = bf.getInventory();
		inv.setStackInSlot(2, new ItemStack(ReactorItems.ingot(ReactorItems.Ingot.SILVER).get()));
		inv.setStackInSlot(5, new ItemStack(ReactorItems.ingot(ReactorItems.Ingot.CADMIUM).get()));
		inv.setStackInSlot(9, new ItemStack(ReactorItems.ingot(ReactorItems.Ingot.INDIUM).get()));
		h.succeedWhen(() -> {
			h.assertTrue(inv.getStackInSlot(10).is(ReactorItems.crafting(ReactorItems.Crafting.ALLOY).get()) && inv.getStackInSlot(10).getCount() == 3,
					"output "+inv.getStackInSlot(10));
			h.assertTrue(inv.getStackInSlot(2).isEmpty() && inv.getStackInSlot(5).isEmpty() && inv.getStackInSlot(9).isEmpty(), "grid not consumed");
		});
	}

	/** Below the alloy temperature nothing happens; the ferromagnetic ingot (steel, iron, lodestone) needs 1200 C. */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void ferroIngotNeedsTemperature(GameTestHelper h) {
		int[] temp = {1150};
		BlastFurnaceBlockEntity bf = furnace(h, temp);
		var inv = bf.getInventory();
		inv.setStackInSlot(1, new ItemStack(RotaryItems.part(CraftingPart.STEELINGOT)));
		inv.setStackInSlot(4, new ItemStack(Items.IRON_INGOT));
		inv.setStackInSlot(7, new ItemStack(ReactorItems.raw(ReactorItems.Raw.MAGNETITE).get()));
		h.runAtTickTime(150, () -> {
			h.assertTrue(inv.getStackInSlot(10).isEmpty(), "made at 1150 C: "+inv.getStackInSlot(10));
			temp[0] = 1250;
		});
		h.runAtTickTime(390, () -> {
			h.assertTrue(inv.getStackInSlot(10).is(ReactorItems.crafting(ReactorItems.Crafting.FERROINGOT).get()), "output "+inv.getStackInSlot(10));
			h.succeed();
		});
	}
}
