/*******************************************************************************
 * Practice factory zone (see Factory): the storage and decoration blocks of RotaryCraft and ReactorCraft, placed and mined by a simulated
 * survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.gametest.SimPlayer;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneStorage {

	/** RotaryCraft BlockDeco and blast glass; ReactorCraft BlockReactorMat (original damageDropped = meta: each drops itself). */
	public static final List<String> BLOCKS = List.of("rotarycraft:steel_block", "rotarycraft:anthracite_block", "rotarycraft:lonsdaleite_block",
			"rotarycraft:shield_block", "rotarycraft:bedrock_ingot_block", "rotarycraft:coke_block", "rotarycraft:blast_glass",
			"reactorcraft:calcite_block", "reactorcraft:concrete", "reactorcraft:graphite_block");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * For each block the player takes two from an infinite chest, places one in a row (z = 2) and one at the mining spot, and mines that
	 * one with a diamond pickaxe: each drops itself, as in the originals.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 100, batch = "factory")
	public static void storageBlocksDropThemselves(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8), spot = new BlockPos(4, 2, 6);
		List<Block> blocks = BLOCKS.stream().map(id -> BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id))).toList();
		List<ItemStack> supplies = new ArrayList<>();
		supplies.add(new ItemStack(Items.DIAMOND_PICKAXE));
		blocks.forEach(b -> supplies.add(new ItemStack(b)));
		Factory.infiniteChest(h, chest, supplies.toArray(ItemStack[]::new));
		Factory.take(p, chest, Items.DIAMOND_PICKAXE, 1);
		List<String> bad = new ArrayList<>();
		for (int i = 0; i < blocks.size(); i++) {
			Block blk = blocks.get(i);
			BlockPos display = new BlockPos(i < 7 ? 1+i : i-6, 2, i < 7 ? 2 : 4);
			List<ItemStack> drops = Factory.placeAndMine(p, chest, blk, display, spot, Items.DIAMOND_PICKAXE);
			if (drops.size() != 1 || !drops.get(0).is(blk.asItem()) || drops.get(0).getCount() != 1)
				bad.add(BLOCKS.get(i)+" -> "+drops);
		}
		h.assertTrue(bad.isEmpty(), "blocks not dropping themselves: "+bad);
		Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 2, 5));
		p.leave();
		h.succeed();
	}
}
