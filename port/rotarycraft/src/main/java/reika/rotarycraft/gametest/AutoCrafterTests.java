/*******************************************************************************
 * Tests for the port's auto crafter and crafting pattern. Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.AutoCrafterBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CraftPatternItem;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AutoCrafterTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	static ItemStack pattern(GameTestHelper h, ItemStack... grid) {
		ItemStack p = new ItemStack(RotaryItems.CRAFT_PATTERN.get());
		List<ItemStack> li = new java.util.ArrayList<>(List.of(grid));
		li.addAll(Collections.nCopies(9-li.size(), ItemStack.EMPTY));
		CraftPatternItem.setGrid(p, li, h.getLevel());
		return p;
	}

	/** A pattern of four planks in a square makes a crafting table; an odd grid makes nothing. */
	@GameTest(template = PLATFORM)
	public static void craftPatternStoresTheRecipe(GameTestHelper h) {
		ItemStack p = pattern(h, new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY,
				new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS));
		h.assertTrue(CraftPatternItem.getOutput(p).is(Items.CRAFTING_TABLE), "output "+CraftPatternItem.getOutput(p));
		h.assertTrue(CraftPatternItem.getOutput(pattern(h, new ItemStack(Items.DIRT), new ItemStack(Items.STICK))).isEmpty(), "dirt+stick makes something");
		h.succeed();
	}

	/**
	 * A gas engine beside the crafter, a chest on top with 9 oak logs: in continuous mode the log -> 4 planks pattern turns all logs into
	 * 36 planks. In request mode a second pattern (4 planks -> crafting table) crafts only when asked; in sustain mode it keeps 2 tables.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void autoCrafterCrafts(GameTestHelper h) {
		AutoCrafterBlockEntity te = place(h, RotaryBlocks.AUTO_CRAFTER.get(), 4, 2, 4, 0, AutoCrafterBlockEntity.class);
		place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		h.setBlock(new BlockPos(4, 3, 4), Blocks.CHEST);
		ChestBlockEntity chest = (ChestBlockEntity)h.getBlockEntity(new BlockPos(4, 3, 4));
		chest.setItem(0, new ItemStack(Items.OAK_LOG, 9));
		te.getInventory().setStackInSlot(0, pattern(h, new ItemStack(Items.OAK_LOG)));
		te.stepMode(); //request -> continuous
		int[] phase = {0};
		h.succeedWhen(() -> {
			switch (phase[0]) {
				case 0 -> {
					h.assertTrue(te.getInventory().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET).getCount() == 36 && chest.getItem(0).isEmpty(),
							"planks "+te.getInventory().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET)+" at "+te.getPower()+" W");
					//planks back into the chest for the second pattern
					chest.setItem(1, te.getInventory().extractItem(AutoCrafterBlockEntity.OUTPUT_OFFSET, 36, false));
					te.getInventory().setStackInSlot(1, pattern(h, new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY,
							new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS)));
					te.stepMode();
					te.stepMode(); //continuous -> sustain -> request
					phase[0] = 1;
					h.fail("request mode");
				}
				case 1 -> {
					h.assertTrue(te.getInventory().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET+1).isEmpty(), "crafted without a request");
					te.request(1);
					phase[0] = 2;
					h.fail("requested");
				}
				case 2 -> {
					h.assertTrue(te.getInventory().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET+1).getCount() == 1, "request: "+te.getInventory().getStackInSlot(19));
					te.setThreshold(1, 3);
					te.stepMode();
					te.stepMode(); //request -> continuous -> sustain
					phase[0] = 3;
					h.fail("sustain");
				}
				default -> h.assertTrue(te.getInventory().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET+1).getCount() == 3 && chest.getItem(1).getCount() == 36-12,
						"sustain: "+te.getInventory().getStackInSlot(19)+", planks left "+chest.getItem(1));
			}
		});
	}
}
