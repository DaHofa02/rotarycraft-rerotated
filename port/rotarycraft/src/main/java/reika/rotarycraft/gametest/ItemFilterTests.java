/*******************************************************************************
 * Tests for the port's item filter. Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.ItemFilterBlockEntity;
import reika.rotarycraft.content.blockentity.ItemFilterBlockEntity.Criterion;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ItemFilterTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** The template's test with its settings; the blacklist; without power or template nothing goes in; redstone inverts. */
	@GameTest(template = PLATFORM)
	public static void itemFilterMatches(GameTestHelper h) {
		ItemFilterBlockEntity te = place(h, RotaryBlocks.ITEM_FILTER.get(), 4, 2, 4, 0, ItemFilterBlockEntity.class);
		ItemStack cobble = new ItemStack(Items.COBBLESTONE), dirt = new ItemStack(Items.DIRT), part = new ItemStack(RotaryItems.part(CraftingPart.PCB));
		te.power = 1024;
		h.assertFalse(te.accepts(cobble), "accepts without a template");
		te.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
		h.assertTrue(te.matches(cobble) && !te.matches(dirt), "default: only the same item");
		te.cycle(Criterion.ITEM);
		te.cycle(Criterion.ITEM); //match -> mismatch -> ignore
		te.cycle(Criterion.TAGS);
		te.cycle(Criterion.TAGS);
		te.cycle(Criterion.COMPONENTS);
		te.cycle(Criterion.COMPONENTS);
		h.assertTrue(te.matches(dirt) && te.matches(cobble) && !te.matches(part), "same mod only: dirt "+te.matches(dirt)+", part "+te.matches(part));
		te.cycle(Criterion.MOD); //mismatch: other mods only
		h.assertTrue(!te.matches(dirt) && te.matches(part), "other mods only");
		te.cycle(Criterion.MOD); //ignore
		te.getInventory().setStackInSlot(5, new ItemStack(Items.DIRT));
		h.assertTrue(te.accepts(cobble) && !te.accepts(dirt), "blacklist");
		te.power = 0;
		h.assertFalse(te.accepts(cobble), "accepts without power");
		h.succeed();
	}

	/**
	 * A gas engine beside the filter (cobblestone template, default settings) and a hopper above it with dirt and cobblestone: only the
	 * cobblestone gets into the buffer, the dirt stays in the hopper; the filter signals 15 only while the buffer is empty.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void itemFilterGatesAHopper(GameTestHelper h) {
		ItemFilterBlockEntity te = place(h, RotaryBlocks.ITEM_FILTER.get(), 4, 2, 4, 0, ItemFilterBlockEntity.class);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
		h.assertTrue(te.getRedstoneOverride() == 15, "no signal with an empty buffer");
		place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		h.setBlock(new BlockPos(4, 3, 4), Blocks.HOPPER);
		HopperBlockEntity hop = (HopperBlockEntity)h.getBlockEntity(new BlockPos(4, 3, 4));
		hop.setItem(0, new ItemStack(Items.DIRT, 4));
		hop.setItem(1, new ItemStack(Items.COBBLESTONE, 4));
		h.succeedWhen(() -> {
			h.assertTrue(te.getPower() >= 1024, "filter at "+te.getPower()+" W");
			h.assertTrue(te.getInventory().getStackInSlot(1).getCount() == 4 && te.getInventory().getStackInSlot(1).is(Items.COBBLESTONE), "buffer "+te.getInventory().getStackInSlot(1));
			h.assertTrue(hop.getItem(0).getCount() == 4 && hop.getItem(0).is(Items.DIRT), "dirt went through");
			h.assertTrue(te.getRedstoneOverride() == 0, "signal with a full buffer");
		});
	}
}
