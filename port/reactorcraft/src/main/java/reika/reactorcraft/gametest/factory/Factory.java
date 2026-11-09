/*******************************************************************************
 * The practice factory of the private NeoForge 1.21.1 ports of RotaryCraft, ReactorCraft and ElectriCraft (GameTests; not part of the
 * original mods). Every block of the three mods is built into a working factory by a simulated SURVIVAL player, split into zones (one
 * GameTest class per zone in this package). The rules every zone keeps:
 *
 *  - The only supplies are infinite chests (items) and fluid barrels (fluids), made here. Nothing else is given: the player starts with an
 *    empty inventory and takes every item from an infinite chest through the chest's GUI ({@link #take}); fluids reach machines from a
 *    barrel through pipes, buckets filled at a barrel, or machines that pump them.
 *  - No shortcuts: no setBlock for anything but the supplies and the floor, no setting of energy, heat, speed, torque, fluid or inventory
 *    contents. Power comes from engines/reactors/generators the player built and fuelled; checks read the machines' state.
 *  - Each zone lists the blocks it builds in a {@code public static final List<String> BLOCKS} (registry ids) and checks at its end that
 *    every one of them stands in its area ({@link #assertBuilt}). {@link FactoryCoverage} reports every placeable block of the three
 *    mods that is in no zone's list - a new block has to be added to the factory (optional test while the zones are being built, G-4).
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluid;

import reika.rotarycraft.content.blockentity.ReservoirBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

public final class Factory {

	public static final Set<String> MODS = Set.of("rotarycraft", "reactorcraft", "electricraft");

	/**
	 * Blocks no player sets down as such, with the reason. Everything else of the three mods must be built in a zone. Keep this short:
	 * a block that a player can place belongs into the factory.
	 */
	public static final Map<String, String> NOT_PLACED = Map.of(
			"rotarycraft:bedrock_slice", "made by the bedrock breaker while it cuts bedrock",
			"reactorcraft:steam", "a gas the boilers make");

	/** A big template every zone can use (48 x 12 x 48, stone floor at y = 1 after {@link SimPlayer#floor}). */
	public static final String BIG = "platform_48x12x48";

	private Factory() {}

	/** True for blocks the factory leaves out: liquids (they live in barrels, pipes and tanks) and {@link #NOT_PLACED}. */
	public static boolean exempt(Block b) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
		return b instanceof net.minecraft.world.level.block.LiquidBlock || NOT_PLACED.containsKey(id.toString());
	}

	/** Every block of the three mods the factory has to contain. */
	public static List<String> required() {
		List<String> li = new ArrayList<>();
		for (Block b : BuiltInRegistries.BLOCK) {
			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
			if (MODS.contains(id.getNamespace()) && !exempt(b))
				li.add(id.toString());
		}
		li.sort(null);
		return li;
	}

	/**
	 * An infinite chest at {@code rel}: a chest holding full stacks of the given items, topped up every tick (it never runs out). The only
	 * source of items in the factory.
	 */
	public static void infiniteChest(GameTestHelper h, BlockPos rel, ItemStack... contents) {
		if (contents.length > 27)
			throw new IllegalArgumentException("a chest holds 27 stacks; use more chests");
		h.setBlock(rel, Blocks.CHEST);
		Runnable refill = () -> {
			if (h.getBlockEntity(rel) instanceof ChestBlockEntity c)
				for (int i = 0; i < contents.length; i++)
					if (!ItemStack.isSameItemSameComponents(c.getItem(i), contents[i]) || c.getItem(i).getCount() < contents[i].getMaxStackSize())
						c.setItem(i, contents[i].copyWithCount(contents[i].getMaxStackSize()));
		};
		refill.run();
		h.onEachTick(refill);
	}

	/** A fluid barrel at {@code rel}: a RotaryCraft reservoir kept full (64000 mB) of the fluid. The only source of fluids in the factory. */
	public static void fluidBarrel(GameTestHelper h, BlockPos rel, Fluid f) {
		h.setBlock(rel, RotaryBlocks.RESERVOIR.get());
		Runnable refill = () -> {
			if (h.getBlockEntity(rel) instanceof ReservoirBlockEntity r && r.getFluidAmount() < ReservoirBlockEntity.CAPACITY)
				r.setLevel(ReservoirBlockEntity.CAPACITY, f);
		};
		refill.run();
		h.onEachTick(refill);
	}

	/**
	 * The player takes items from an infinite chest the way a player does: opens it and shift-clicks the stack into the inventory (repeated
	 * until it holds at least {@code n}). Returns how many of the item the player holds then.
	 */
	public static int take(SimPlayer p, BlockPos chest, ItemStack what, int n) {
		for (int guard = 0; guard < 40 && count(p, what) < n; guard++) {
			AbstractContainerMenu m = p.open(chest);
			int slot = -1;
			for (int i = 0; i < 27; i++)
				if (ItemStack.isSameItemSameComponents(m.getSlot(i).getItem(), what)) {
					slot = i;
					break;
				}
			if (slot < 0) {
				p.closeMenu();
				throw new IllegalStateException("the infinite chest at "+chest+" has no "+what);
			}
			p.shiftClick(slot);
			p.closeMenu();
			refillNow(p, chest, what); //several takes within one tick: the chest stays infinite
		}
		return count(p, what);
	}

	public static int take(SimPlayer p, BlockPos chest, Item what, int n) {
		return take(p, chest, new ItemStack(what), n);
	}

	/** Within one tick the chest is topped up again only on the next tick; for several takes in a row it is topped up right away. */
	private static void refillNow(SimPlayer p, BlockPos chest, ItemStack what) {
		if (p.helper.getBlockEntity(chest) instanceof ChestBlockEntity c)
			for (int i = 0; i < 27; i++)
				if (c.getItem(i).isEmpty()) {
					c.setItem(i, what.copyWithCount(what.getMaxStackSize()));
					return;
				}
	}

	public static int count(SimPlayer p, ItemStack what) {
		int n = 0;
		for (ItemStack is : p.player.getInventory().items)
			if (ItemStack.isSameItemSameComponents(is, what))
				n += is.getCount();
		return n;
	}

	/** Fails the test unless each of the listed blocks stands somewhere in the box (relative corners). */
	public static void assertBuilt(GameTestHelper h, Collection<String> blocks, BlockPos from, BlockPos to) {
		Set<String> found = new HashSet<>();
		for (BlockPos p : BlockPos.betweenClosed(from, to))
			found.add(BuiltInRegistries.BLOCK.getKey(h.getBlockState(p).getBlock()).toString());
		List<String> missing = new ArrayList<>();
		for (String b : blocks)
			if (!found.contains(b))
				missing.add(b);
		h.assertTrue(missing.isEmpty(), "not built in the zone: "+missing);
	}
}
