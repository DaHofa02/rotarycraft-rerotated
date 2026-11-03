/*******************************************************************************
 * Live scenarios of the private NeoForge 1.21.1 port of RotaryCraft: a simulated player builds and runs parts of a base the way a player
 * does, once in survival (crafting from gathered materials, items used up, blocks mined with tools) and once in creative. Not part of
 * the original mod; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class LiveBaseTests {

	private static final String PLATFORM = "platform_9x8x9";

	static ResourceLocation rc(String id) {
		return ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, id);
	}

	/**
	 * The player's first power line. Survival: places a crafting table, crafts a worktable in it, places that and crafts in it (from
	 * gathered materials) a DC engine, a steel shaft, a steel 2:1 gearbox, a dynamometer and a screwdriver; creative takes them from the
	 * creative inventory. Then, looking east: engine, shaft, gearbox, dynamometer in a row; a redstone block beside the engine; the gearbox
	 * filled with a lubricant bucket. The dynamometer reads 128 rad/s at 8 Nm (2:1 reduction of 256 rad/s, 4 Nm). Shift-clicking the
	 * gearbox with the screwdriver switches it to speed mode: 512 rad/s at 2 Nm. The gearbox is mined with an iron pickaxe (survival: it
	 * drops as a 2:1 steel gearbox and is placed again, the line runs again).
	 */
	private static void powerLine(GameTestHelper h, GameType mode) {
		SimPlayer p = new SimPlayer(h, mode);
		SimPlayer.floor(h, 0, 0, 8, 8);
		BlockPos table = new BlockPos(1, 2, 1), work = new BlockPos(3, 2, 1);
		ItemStack engine, shaft, gearbox, dyna;
		if (p.survival()) {
			p.give(Items.CRAFTING_TABLE, 1);
			p.place(table, Items.CRAFTING_TABLE, Direction.NORTH);
			p.open(table);
			p.gatherFor(rc("worktable"));
			ItemStack wt = p.craft(rc("worktable"), 1, 0);
			p.closeMenu();
			p.place(work, wt.getItem(), Direction.NORTH);
			h.assertTrue(p.count(wt.getItem()) == 0, "worktable not used up");
			p.open(work);
			engine = craftIn(p, "dc_engine_worktable");
			shaft = craftIn(p, "shaft_steel_worktable");
			gearbox = craftIn(p, "gearbox_steel_2_worktable");
			dyna = craftIn(p, "dynamometer_worktable");
			craftIn(p, "screwdriver_worktable");
			p.closeMenu();
			p.give(RotaryItems.LUBRICANT_BUCKET.get(), 2);
			p.give(Items.REDSTONE_BLOCK, 1);
			p.give(Items.IRON_PICKAXE, 1);
		}
		else {
			engine = new ItemStack(RotaryBlocks.DC_ENGINE.get());
			shaft = reika.rotarycraft.content.block.ShaftBlock.shaftItem(reika.rotarycraft.registry.MaterialRegistry.STEEL, false);
			gearbox = GearboxBlock.gearboxItem(GearboxTypes.STEEL, 2);
			dyna = new ItemStack(RotaryBlocks.DYNAMOMETER.get());
		}
		BlockPos e = new BlockPos(6, 2, 4), s = new BlockPos(5, 2, 4), g = new BlockPos(4, 2, 4), d = new BlockPos(3, 2, 4);
		p.place(e, engine, Direction.EAST);
		p.place(s, shaft, Direction.EAST);
		p.place(g, gearbox, Direction.EAST);
		p.place(d, dyna, Direction.EAST);
		p.place(new BlockPos(6, 2, 5), new ItemStack(Items.REDSTONE_BLOCK), Direction.EAST);
		p.use(g, RotaryItems.LUBRICANT_BUCKET.get());
		GearboxBlockEntity gb = (GearboxBlockEntity)h.getBlockEntity(g);
		h.assertTrue(gb.getLubricant() > 0, "the lubricant bucket did not fill the gearbox");
		if (p.survival())
			h.assertTrue(p.count(Items.BUCKET) == 1 && p.count(RotaryItems.LUBRICANT_BUCKET.get()) == 1, "no empty bucket back");
		DynamometerBlockEntity dy = (DynamometerBlockEntity)h.getBlockEntity(d);
		h.runAtTickTime(40, () -> {
			h.assertTrue(dy.omega == 128 && dy.torque == 8, "torque mode: "+dy.omega+" rad/s @ "+dy.torque+" Nm");
			p.player.setShiftKeyDown(true);
			p.use(g, RotaryItems.SCREWDRIVER.get());
			p.player.setShiftKeyDown(false);
		});
		h.runAtTickTime(80, () -> {
			h.assertTrue(dy.omega == 512 && dy.torque == 2, "speed mode: "+dy.omega+" rad/s @ "+dy.torque+" Nm");
			List<ItemStack> drops = p.mine(g, Items.IRON_PICKAXE);
			h.assertBlockNotPresent(RotaryBlocks.GEARBOX.get(), g);
			if (p.survival()) {
				h.assertTrue(drops.size() == 1 && drops.get(0).is(RotaryBlocks.GEARBOX.get().asItem())
						&& drops.get(0).getOrDefault(RotaryComponents.GEAR_RATIO.get(), 0) == 2, "gearbox drops: "+drops);
				p.place(g, drops.get(0), Direction.EAST);
			}
			else {
				h.assertTrue(drops.isEmpty(), "creative mining dropped "+drops);
				p.place(g, gearbox, Direction.EAST);
			}
			if (((GearboxBlockEntity)h.getBlockEntity(g)).getLubricant() <= 0)
				p.use(g, RotaryItems.LUBRICANT_BUCKET.get());
		});
		h.runAtTickTime(120, () -> {
			h.assertTrue(dy.omega == 128 && dy.torque == 8, "after rebuilding: "+dy.omega+" rad/s @ "+dy.torque+" Nm");
			p.leave();
			h.succeed();
		});
	}

	/** Survival: places a crafting table, crafts a worktable, places it at (3, 2, 1) and opens it. Creative: nothing. */
	static boolean openWorktable(SimPlayer p) {
		if (!p.survival())
			return false;
		p.give(Items.CRAFTING_TABLE, 1);
		p.place(new BlockPos(1, 2, 1), Items.CRAFTING_TABLE, Direction.NORTH);
		p.open(new BlockPos(1, 2, 1));
		p.gatherFor(rc("worktable"));
		ItemStack wt = p.craft(rc("worktable"), 1, 0);
		p.closeMenu();
		p.place(new BlockPos(3, 2, 1), wt.getItem(), Direction.NORTH);
		p.open(new BlockPos(3, 2, 1));
		return true;
	}

	/** In survival crafts the item in the open worktable; in creative takes it from the creative tab. */
	static ItemStack obtain(SimPlayer p, String worktableRecipe, ItemStack creative) {
		if (!p.survival())
			return creative;
		ItemStack is = craftIn(p, worktableRecipe);
		return is;
	}

	/**
	 * A small workshop. A gas engine (fuel: four ethanol put into its GUI) drives a grinder: two canola seeds go in through the GUI,
	 * become husks (taken out by shift-click) and lubricant in the grinder's tank. A DC engine (redstone block) drives a fermenter whose
	 * water comes from a reservoir (filled with two water buckets) standing on the first of three pipes the player lays to it; sugar and
	 * dirt go in through the GUI and yeast comes out.
	 */
	private static void workshop(GameTestHelper h, GameType mode) {
		SimPlayer p = new SimPlayer(h, mode);
		SimPlayer.floor(h, 0, 0, 8, 8);
		ItemStack gas, grinder, dc, fermenter, reservoir, pipe;
		if (openWorktable(p)) {
			gas = craftIn(p, "gas_engine_worktable");
			grinder = craftIn(p, "grinder_worktable");
			dc = craftIn(p, "dc_engine_worktable");
			fermenter = craftIn(p, "fermenter_worktable");
			reservoir = craftIn(p, "reservoir_worktable");
			pipe = craftIn(p, "pipe_difficulty_"+(reika.rotarycraft.registry.DifficultyEffects.getDifficulty().ordinal()+1)+"_worktable");
			p.closeMenu();
			p.give(RotaryItems.ETHANOL.get(), 4);
			p.give(RotaryItems.CANOLA_SEEDS.get(), 2);
			p.give(Items.SUGAR, 2);
			p.give(Items.DIRT, 2);
			p.give(Items.WATER_BUCKET, 1);
			p.give(Items.WATER_BUCKET, 1);
			p.give(Items.REDSTONE_BLOCK, 1);
			p.give(Items.COBBLESTONE, 1);
			p.give(Items.STONE_PICKAXE, 1);
		}
		else {
			gas = new ItemStack(RotaryBlocks.GAS_ENGINE.get());
			grinder = new ItemStack(RotaryBlocks.GRINDER.get());
			dc = new ItemStack(RotaryBlocks.DC_ENGINE.get());
			fermenter = new ItemStack(RotaryBlocks.FERMENTER.get());
			reservoir = new ItemStack(RotaryBlocks.RESERVOIR.get());
			pipe = new ItemStack(RotaryBlocks.PIPE.get());
		}
		//grinding line (z = 4)
		BlockPos ge = new BlockPos(6, 2, 4), gr = new BlockPos(5, 2, 4);
		p.place(ge, gas, Direction.EAST);
		p.place(gr, grinder, Direction.EAST);
		p.open(ge);
		p.putInto(0, RotaryItems.ETHANOL.get(), 4);
		p.closeMenu();
		p.open(gr);
		p.putInto(0, RotaryItems.CANOLA_SEEDS.get(), 2);
		p.closeMenu();
		//fermenting line (z = 7): reservoir on a pipe, pipes to the fermenter, DC engine behind it
		BlockPos fe = new BlockPos(5, 2, 7), dce = new BlockPos(6, 2, 7);
		p.place(dce, dc, Direction.EAST);
		p.place(new BlockPos(6, 2, 8), new ItemStack(Items.REDSTONE_BLOCK), Direction.EAST);
		p.place(fe, fermenter, Direction.EAST);
		p.place(new BlockPos(2, 2, 7), new ItemStack(Items.COBBLESTONE), Direction.EAST); //a step to set the reservoir on
		p.place(new BlockPos(2, 3, 7), reservoir, Direction.EAST);
		List<ItemStack> cobble = p.mine(new BlockPos(2, 2, 7), p.survival() ? Items.STONE_PICKAXE : Items.AIR);
		if (p.survival())
			h.assertTrue(!cobble.isEmpty() && cobble.get(0).is(Items.COBBLESTONE), "mining the step gave "+cobble);
		for (int x = 2; x <= 4; x++)
			p.place(new BlockPos(x, 2, 7), pipe, Direction.EAST);
		p.use(new BlockPos(2, 3, 7), Items.WATER_BUCKET);
		p.use(new BlockPos(2, 3, 7), Items.WATER_BUCKET);
		p.open(fe);
		p.putInto(0, Items.SUGAR, 2);
		p.putInto(1, Items.DIRT, 2);
		p.closeMenu();
		reika.rotarycraft.content.blockentity.GrinderBlockEntity grt = (reika.rotarycraft.content.blockentity.GrinderBlockEntity)h.getBlockEntity(gr);
		reika.rotarycraft.content.blockentity.FermenterBlockEntity fet = (reika.rotarycraft.content.blockentity.FermenterBlockEntity)h.getBlockEntity(fe);
		boolean[] done = {false, false};
		h.onEachTick(() -> {
			if (!done[0] && grt.getInventory().getStackInSlot(1).getCount() >= 2) {
				done[0] = true;
				p.open(gr);
				p.shiftClick(1);
				p.closeMenu();
			}
			if (!done[1] && !fet.getInventory().getStackInSlot(2).isEmpty()) {
				done[1] = true;
				p.open(fe);
				p.shiftClick(2);
				p.closeMenu();
			}
		});
		h.succeedWhen(() -> {
			h.assertTrue(done[0], "grinder: "+grt.getInventory().getStackInSlot(0)+" -> "+grt.getInventory().getStackInSlot(1)+", "+grt.omega+" rad/s");
			h.assertTrue(p.count(RotaryItems.CANOLA_HUSKS.get()) >= 2 && grt.getLubricant() > 0, "husks "+p.count(RotaryItems.CANOLA_HUSKS.get())+", lubricant "+grt.getLubricant());
			h.assertTrue(done[1] && p.count(RotaryItems.YEAST.get()) >= 1, "fermenter: water "+fet.getWater()+", "+fet.omega+" rad/s, out "+fet.getInventory().getStackInSlot(2));
			p.leave();
		});
	}

	@GameTest(template = PLATFORM, timeoutTicks = 2400)
	public static void survivalWorkshop(GameTestHelper h) {
		workshop(h, GameType.SURVIVAL);
	}

	@GameTest(template = PLATFORM, timeoutTicks = 2400)
	public static void creativeWorkshop(GameTestHelper h) {
		workshop(h, GameType.CREATIVE);
	}

	private static ItemStack craftIn(SimPlayer p, String id) {
		p.gatherFor(rc(id));
		return p.craft(rc(id), 0, 13);
	}

	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void survivalPowerLine(GameTestHelper h) {
		powerLine(h, GameType.SURVIVAL);
	}

	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void creativePowerLine(GameTestHelper h) {
		powerLine(h, GameType.CREATIVE);
	}

}
