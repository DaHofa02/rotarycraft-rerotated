/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft workshop - the Worktable built in the world, a Grinder and a Blast Furnace heated
 * by a Friction Heater, with gas engines on a fuel line - built and run by a simulated survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.BlastFurnaceBlockEntity;
import reika.rotarycraft.content.blockentity.GrinderBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneWorkshop {

	public static final List<String> BLOCKS = List.of("rotarycraft:worktable", "rotarycraft:grinder", "rotarycraft:friction_heater",
			"rotarycraft:blast_furnace");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * The Worktable as the original builds it (RotaryEventManager.buildWorktables): a redstone block in a ring of eight double smooth stone
	 * slabs, above it a steel block in a ring of eight bricks; setting a crafting table on the steel block turns the structure into a
	 * Worktable (bottom centre). Then an ethanol barrel feeds a fuel line under two gas engines: one drives a Friction Heater that heats a
	 * Blast Furnace, which the player loads through its GUI with coal, gunpowder, sand and three iron ingots - three HSLA steel ingots come
	 * out (above 600 C); the other drives a Grinder that turns cobblestone into gravel.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1500, batch = "factory")
	public static void worktableGrinderAndBlastFurnace(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos a = new BlockPos(0, 2, 8), b = new BlockPos(1, 2, 8);
		Factory.infiniteChest(h, a, new ItemStack(Items.REDSTONE_BLOCK), new ItemStack(Items.SMOOTH_STONE_SLAB), new ItemStack(RotaryBlocks.STEEL_BLOCK.get()),
				new ItemStack(Items.BRICKS), new ItemStack(Items.CRAFTING_TABLE));
		Factory.infiniteChest(h, b, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.FRICTION_HEATER.get()), new ItemStack(RotaryBlocks.BLAST_FURNACE.get()), new ItemStack(RotaryBlocks.GRINDER.get()),
				new ItemStack(Items.COAL), new ItemStack(Items.GUNPOWDER), new ItemStack(Items.SAND), new ItemStack(Items.IRON_INGOT),
				new ItemStack(Items.COBBLESTONE));

		// the Worktable structure, centre (5, *, 6)
		Factory.take(p, a, Items.REDSTONE_BLOCK, 1);
		Factory.take(p, a, Items.SMOOTH_STONE_SLAB, 16);
		Factory.take(p, a, RotaryBlocks.STEEL_BLOCK.get().asItem(), 1);
		Factory.take(p, a, Items.BRICKS, 8);
		Factory.takeExactly(p, a, Items.CRAFTING_TABLE, 1);
		BlockPos low = new BlockPos(5, 2, 6), up = low.above();
		p.place(low, Items.REDSTONE_BLOCK, Direction.NORTH);
		for (int i = -1; i <= 1; i++)
			for (int k = -1; k <= 1; k++)
				if (i != 0 || k != 0) {
					p.place(low.offset(i, 0, k), Items.SMOOTH_STONE_SLAB, Direction.NORTH);
					p.place(low.offset(i, 0, k), Items.SMOOTH_STONE_SLAB, Direction.NORTH); //the second slab makes it double
				}
		p.place(up, RotaryBlocks.STEEL_BLOCK.get(), Direction.NORTH);
		for (int i = -1; i <= 1; i++)
			for (int k = -1; k <= 1; k++)
				if (i != 0 || k != 0)
					p.place(up.offset(i, 0, k), Items.BRICKS, Direction.NORTH);
		//set down like a block, but the click turns the structure into the Worktable at once (the table's own spot stays empty)
		p.hold(Items.CRAFTING_TABLE);
		p.look(Direction.NORTH);
		p.useOn(up, Direction.UP);
		h.assertBlockPresent(RotaryBlocks.WORKTABLE.get(), low);
		h.assertTrue(p.count(Items.CRAFTING_TABLE) == 0, "crafting table not used up");

		// fuel line along z = 2 under the engines, fed by an ethanol barrel at its east end
		Factory.fluidBarrel(h, new BlockPos(8, 3, 2), RotaryFluids.ETHANOL.get());
		Factory.take(p, b, RotaryBlocks.FUEL_LINE.get().asItem(), 7);
		for (int x = 8; x >= 2; x--)
			p.place(new BlockPos(x, 2, 2), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		Factory.take(p, b, RotaryBlocks.GAS_ENGINE.get().asItem(), 2);
		Factory.take(p, b, RotaryBlocks.FRICTION_HEATER.get().asItem(), 1);
		Factory.take(p, b, RotaryBlocks.BLAST_FURNACE.get().asItem(), 1);
		Factory.take(p, b, RotaryBlocks.GRINDER.get().asItem(), 1);
		p.placeSneaking(new BlockPos(6, 3, 2), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(5, 3, 2), new ItemStack(RotaryBlocks.FRICTION_HEATER.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(4, 3, 2), new ItemStack(RotaryBlocks.BLAST_FURNACE.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(2, 3, 2), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(1, 3, 2), new ItemStack(RotaryBlocks.GRINDER.get()), Direction.EAST);

		// load the Blast Furnace (menu slots = machine slots: 0 coal, 1-9 grid, 11 gunpowder, 14 sand) and the Grinder (0 input)
		Factory.take(p, b, Items.COAL, 1);
		Factory.take(p, b, Items.GUNPOWDER, 1);
		Factory.take(p, b, Items.SAND, 1);
		Factory.take(p, b, Items.IRON_INGOT, 3);
		Factory.take(p, b, Items.COBBLESTONE, 1);
		BlockPos bfp = new BlockPos(4, 3, 2), grp = new BlockPos(1, 3, 2);
		p.open(bfp);
		p.putInto(0, Items.COAL, 4);
		p.putInto(11, Items.GUNPOWDER, 4);
		p.putInto(14, Items.SAND, 4);
		for (int i = 1; i <= 3; i++)
			p.putInto(i, Items.IRON_INGOT, 1);
		p.closeMenu();
		p.open(grp);
		p.putInto(0, Items.COBBLESTONE, 2);
		p.closeMenu();
		BlastFurnaceBlockEntity bf = (BlastFurnaceBlockEntity)h.getBlockEntity(bfp);
		GrinderBlockEntity gr = (GrinderBlockEntity)h.getBlockEntity(grp);
		h.succeedWhen(() -> {
			ItemStack steel = bf.getInventory().getStackInSlot(10);
			h.assertTrue(steel.is(RotaryItems.part(CraftingPart.STEELINGOT)) && steel.getCount() == 3,
					"blast furnace at "+bf.getTemperature()+" C, output "+steel);
			ItemStack gravel = gr.getInventory().getStackInSlot(1);
			h.assertTrue(gravel.is(Items.GRAVEL) && gravel.getCount() == 2, "grinder at "+gr.omega+" rad/s, output "+gravel);
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
