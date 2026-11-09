/*******************************************************************************
 * Practice factory zone (see Factory): the ores and fluorite blocks of ReactorCraft and ElectriCraft, placed and mined by a simulated
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
public class ZoneOres {

	private static final List<String> COLORS = List.of("white", "orange", "magenta", "blue", "yellow", "green", "red", "pink");

	/** ElectriCraft ores drop themselves (loot tables); the ReactorCraft ores and fluorite decide their drops in code. */
	private static final List<String> SELF_DROPPING = List.of("electricraft:tin_ore", "electricraft:silver_ore", "electricraft:nickel_ore",
			"electricraft:aluminum_ore", "electricraft:platinum_ore");

	public static final List<String> BLOCKS;

	static {
		List<String> li = new ArrayList<>(SELF_DROPPING);
		for (String o : List.of("pitchblende", "cadmium", "indium", "silver", "endblende", "ammonium", "calcite", "magnetite", "thorium"))
			li.add("reactorcraft:"+o+"_ore");
		for (String c : COLORS) {
			li.add("reactorcraft:"+c+"_fluorite_ore");
			li.add("reactorcraft:"+c+"_fluorite_block");
		}
		BLOCKS = List.copyOf(li);
	}

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * For each of the 30 blocks the player takes two from an infinite chest, places one in the display grid (x 1..7, z 1..5) and one at
	 * the mining spot, and mines that one with a diamond pickaxe: every one drops something (ElectriCraft ores themselves), so the tool
	 * tags, harvest levels and drops work in survival.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 100, batch = "factory")
	public static void oresAreMinedForTheirDrops(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chestA = new BlockPos(0, 2, 8), chestB = new BlockPos(8, 2, 8), spot = new BlockPos(4, 2, 7);
		List<Block> blocks = BLOCKS.stream().map(id -> BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id))).toList();
		List<ItemStack> a = new ArrayList<>(), b = new ArrayList<>();
		a.add(new ItemStack(Items.DIAMOND_PICKAXE));
		for (int i = 0; i < blocks.size(); i++)
			(i < 20 ? a : b).add(new ItemStack(blocks.get(i)));
		Factory.infiniteChest(h, chestA, a.toArray(ItemStack[]::new));
		Factory.infiniteChest(h, chestB, b.toArray(ItemStack[]::new));
		Factory.take(p, chestA, Items.DIAMOND_PICKAXE, 1);
		List<String> bad = new ArrayList<>();
		for (int i = 0; i < blocks.size(); i++) {
			Block blk = blocks.get(i);
			BlockPos display = new BlockPos(1+i%7, 2, 1+i/7);
			List<ItemStack> drops = Factory.placeAndMine(p, i < 20 ? chestA : chestB, blk, display, spot, Items.DIAMOND_PICKAXE);
			String id = BLOCKS.get(i);
			if (drops.isEmpty() || SELF_DROPPING.contains(id) && drops.stream().noneMatch(s -> s.is(blk.asItem())))
				bad.add(id+" -> "+drops);
		}
		h.assertTrue(bad.isEmpty(), "wrong drops: "+bad);
		Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 2, 6));
		p.leave();
		h.succeed();
	}
}
