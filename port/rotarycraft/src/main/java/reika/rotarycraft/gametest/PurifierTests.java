/*******************************************************************************
 * Tests for the port's purifier. Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.PurifierBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PurifierTests {

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Gas engine (west) -> purifier with contained lava to the east: it heats towards ambient+600, works only from 600 C, and turns one "mod
	 * steel" (netherite scrap stands in for one here) from each input slot into RotaryCraft steel per cycle; its own steel is no input.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1400)
	public static void purifierTurnsModSteelIntoSteel(GameTestHelper h) {
		PurifierBlockEntity.TEST_STEELS.add(Items.NETHERITE_SCRAP);
		h.setBlock(new BlockPos(1, 2, 4), RotaryBlocks.GAS_ENGINE.get());
		((RotaryCraftBlockEntity)h.getBlockEntity(new BlockPos(1, 2, 4))).setBlockMetadata(1);
		((EngineBlockEntity)h.getBlockEntity(new BlockPos(1, 2, 4))).addFuel(50000);
		h.setBlock(new BlockPos(2, 2, 4), RotaryBlocks.PURIFIER.get());
		for (BlockPos p : new BlockPos[] {new BlockPos(4, 2, 4), new BlockPos(3, 2, 3), new BlockPos(3, 2, 5)})
			h.setBlock(p, Blocks.STONE);
		h.setBlock(new BlockPos(3, 2, 4), Blocks.LAVA);
		PurifierBlockEntity te = (PurifierBlockEntity)h.getBlockEntity(new BlockPos(2, 2, 4));
		h.assertTrue(!PurifierBlockEntity.isModSteel(new ItemStack(RotaryItems.part(CraftingPart.STEELINGOT))), "own steel counts as mod steel");
		te.getInventory().setStackInSlot(0, new ItemStack(Items.GUNPOWDER, 16));
		te.getInventory().setStackInSlot(7, new ItemStack(Items.SAND, 16));
		te.getInventory().setStackInSlot(1, new ItemStack(Items.NETHERITE_SCRAP, 4));
		te.getInventory().setStackInSlot(2, new ItemStack(Items.NETHERITE_SCRAP, 4));
		boolean[] wasCold = {false};
		h.onEachTick(() -> {
			if (te.temperature < PurifierBlockEntity.SMELTTEMP && te.converted == 0 && te.temperature > -100)
				wasCold[0] = true;
		});
		h.succeedWhen(() -> {
			h.assertTrue(te.getPower() >= 16384, "power "+te.getPower());
			h.assertTrue(te.converted >= 2, "converted "+te.converted+" at "+te.temperature+" C, cook "+te.cookTime+"/"+te.getOperationTime());
			h.assertTrue(wasCold[0] && te.temperature >= PurifierBlockEntity.SMELTTEMP, "temperature "+te.temperature);
			h.assertTrue(te.getInventory().getStackInSlot(6).is(RotaryItems.part(CraftingPart.STEELINGOT)) && te.getInventory().getStackInSlot(6).getCount() == te.converted, "output "+te.getInventory().getStackInSlot(6));
			h.setBlock(new BlockPos(3, 2, 4), Blocks.STONE);
			PurifierBlockEntity.TEST_STEELS.remove(Items.NETHERITE_SCRAP);
		});
	}
}
