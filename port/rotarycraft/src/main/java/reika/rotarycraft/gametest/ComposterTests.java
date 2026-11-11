/*******************************************************************************
 * Tests for the ported RotaryCraft composter (original TileEntityComposter). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.ComposterBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ComposterTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static ComposterBlockEntity composter(GameTestHelper h, int x, int z) {
		BlockPos p = new BlockPos(x, 2, z);
		h.setBlock(p, RotaryBlocks.COMPOSTER.get());
		return (ComposterBlockEntity)h.getBlockEntity(p);
	}

	private static int ambient(GameTestHelper h, int x, int z) {
		return AmbientTemperature.getAmbientTemperatureAt(h.getLevel(), h.absolutePos(new BlockPos(x, 2, z)));
	}

	private static Item compost() {
		return RotaryItems.part(CraftingPart.COMPOST);
	}

	private static ItemStack yeast(int n) {
		return new ItemStack(RotaryItems.YEAST.get(), n);
	}

	private static void value(GameTestHelper h, Item item, int v) {
		int is = ComposterBlockEntity.getCompostValue(new ItemStack(item));
		h.assertTrue(is == v, item+" gives "+is+" compost, not "+v);
	}

	/** Original CompostMatter: scraps and plants 1, sugar cane, leaves and vegetables 2, fish and mob drops 3, meat 4. */
	@GameTest(template = PLATFORM)
	public static void composterValuesFollowTheOriginalTable(GameTestHelper h) {
		value(h, Items.WHEAT, 1);
		value(h, Items.EGG, 1);
		value(h, RotaryItems.CANOLA_SEEDS.get(), 1);
		value(h, Items.SUGAR_CANE, 2);
		value(h, Items.OAK_SAPLING, 1);
		value(h, Items.POPPY, 1);
		value(h, Items.RED_MUSHROOM, 1);
		value(h, Items.BIRCH_LEAVES, 2);
		value(h, Items.SHORT_GRASS, 2);
		value(h, Items.SUNFLOWER, 2);
		value(h, Items.COOKED_BEEF, 4);
		value(h, Items.CHICKEN, 4);
		value(h, Items.SALMON, 3);
		value(h, Items.BREAD, 2);
		value(h, Items.MELON_SLICE, 2);
		value(h, Items.ROTTEN_FLESH, 3);
		value(h, Items.COBBLESTONE, 0);
		value(h, RotaryItems.YEAST.get(), 0);
		h.succeed();
	}

	/**
	 * At 60 deg C the timer advances by 1 + (60 - 40)/4 = 6 per tick: after 17 ticks one beef has become 4 compost. At 40 deg C
	 * it advances by 1: one wheat takes 100 ticks.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void composterMakesCompostWhenWarm(GameTestHelper h) {
		ComposterBlockEntity hot = composter(h, 2, 4);
		hot.getInventory().setStackInSlot(0, new ItemStack(Items.BEEF, 2));
		hot.getInventory().setStackInSlot(1, yeast(64));
		hot.setTemperature(60);
		ComposterBlockEntity warm = composter(h, 6, 4);
		warm.getInventory().setStackInSlot(0, new ItemStack(Items.WHEAT, 2));
		warm.getInventory().setStackInSlot(1, yeast(64));
		h.onEachTick(() -> warm.setTemperature(41)); //stays at 40 or 41 through the cooling each second: 1 + 1/4 = 1 per tick
		h.runAtTickTime(10, () -> h.assertTrue(hot.getInventory().getStackInSlot(2).isEmpty() && hot.composterCookTime > 0 && hot.getScaledTimer(48) > 0,
				"hot composter early: "+hot.getInventory().getStackInSlot(2)+", timer "+hot.composterCookTime));
		h.runAtTickTime(19, () -> {
			ItemStack out = hot.getInventory().getStackInSlot(2);
			h.assertTrue(out.is(compost()) && out.getCount() == 4 && hot.getInventory().getStackInSlot(0).getCount() == 1, "hot composter after 19 ticks: "+out);
			h.assertTrue(warm.getInventory().getStackInSlot(2).isEmpty(), "40 deg C composter was done after 19 ticks");
		});
		h.runAtTickTime(90, () -> h.assertTrue(warm.getInventory().getStackInSlot(2).isEmpty(), "40 deg C composter was done after 90 ticks"));
		h.runAtTickTime(104, () -> {
			ItemStack out = warm.getInventory().getStackInSlot(2);
			h.assertTrue(out.is(compost()) && out.getCount() == 1 && warm.getInventory().getStackInSlot(0).getCount() == 1, "40 deg C composter after 104 ticks: "+out);
			h.assertTrue(warm.getInventory().getStackInSlot(1).getCount() >= 63, "yeast left: "+warm.getInventory().getStackInSlot(1));
			h.succeed();
		});
	}

	/** Nothing below 40 or above 70 deg C, without yeast, without matter or without room for the compost. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void composterNeedsWarmthYeastAndRoom(GameTestHelper h) {
		ComposterBlockEntity te = composter(h, 4, 4);
		ItemStackHandler inv = te.getInventory();
		inv.setStackInSlot(0, new ItemStack(Items.BEEF, 8));
		inv.setStackInSlot(1, yeast(8));
		te.setTemperature(39);
		h.assertTrue(te.getCompostValue() == 0, "composts at 39 deg C");
		te.setTemperature(71);
		h.assertTrue(te.getCompostValue() == 0, "composts at 71 deg C");
		te.setTemperature(70);
		h.assertTrue(te.getCompostValue() == 4, "beef at 70 deg C gives "+te.getCompostValue());
		te.setTemperature(40);
		h.assertTrue(te.getCompostValue() == 4, "beef at 40 deg C gives "+te.getCompostValue());
		inv.setStackInSlot(2, new ItemStack(compost(), 60));
		h.assertTrue(te.getCompostValue() == 4, "60 compost leave no room for 4 more");
		inv.setStackInSlot(2, new ItemStack(compost(), 61));
		h.assertTrue(te.getCompostValue() == 0, "61 compost leave room for 4 more");
		inv.setStackInSlot(2, new ItemStack(Items.DIRT));
		h.assertTrue(te.getCompostValue() == 0, "composts onto dirt");
		inv.setStackInSlot(2, ItemStack.EMPTY);
		inv.setStackInSlot(1, ItemStack.EMPTY);
		h.assertTrue(te.getCompostValue() == 0, "composts without yeast");
		inv.setStackInSlot(1, yeast(8));
		inv.setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
		h.assertTrue(te.getCompostValue() == 0, "composts cobblestone");
		//a cold composter (it stays near the ambient temperature of about 25 deg C) does nothing
		inv.setStackInSlot(0, new ItemStack(Items.BEEF, 8));
		te.setTemperature(AmbientTemperature.getAmbientTemperatureAt(h.getLevel(), h.absolutePos(new BlockPos(4, 2, 4))));
		h.runAtTickTime(150, () -> {
			h.assertTrue(te.getTemperature() < 40 && inv.getStackInSlot(2).isEmpty() && inv.getStackInSlot(0).getCount() == 8 && te.composterCookTime == 0,
					"cold composter at "+te.getTemperature()+" deg C made "+inv.getStackInSlot(2));
			h.succeed();
		});
	}

	/**
	 * Every second the temperature moves towards the ambient one: fire beside it adds 50 (from 0 the composter gains 2 deg C per second:
	 * 20 after 10 s), water takes 5 off and ice 15.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 500)
	public static void composterFollowsTheTemperatureAroundIt(GameTestHelper h) {
		ComposterBlockEntity fire = composter(h, 1, 1);
		h.setBlock(new BlockPos(2, 1, 1), Blocks.NETHERRACK);
		h.setBlock(new BlockPos(2, 2, 1), Blocks.FIRE);
		ComposterBlockEntity water = composter(h, 1, 6);
		h.setBlock(new BlockPos(2, 2, 6), Blocks.WATER);
		for (BlockPos g : new BlockPos[] {new BlockPos(3, 2, 6), new BlockPos(2, 2, 5), new BlockPos(2, 2, 7)}) //a basin that keeps the water in
			h.setBlock(g, Blocks.GLASS);
		ComposterBlockEntity ice = composter(h, 6, 6);
		h.setBlock(new BlockPos(7, 2, 6), Blocks.ICE);
		ComposterBlockEntity plain = composter(h, 6, 1);
		water.setTemperature(ambient(h, 1, 6));
		ice.setTemperature(ambient(h, 6, 6));
		plain.setTemperature(ambient(h, 6, 1)+3);
		h.runAtTickTime(205, () -> h.assertTrue(fire.getTemperature() >= 18 && fire.getTemperature() <= 20, "composter beside fire after 10 s: "+fire.getTemperature()));
		h.runAtTickTime(430, () -> {
			h.assertBlockPresent(Blocks.FIRE, new BlockPos(2, 2, 1));
			h.assertTrue(fire.getTemperature() >= 39 && fire.getTemperature() <= 42, "composter beside fire after 21 s: "+fire.getTemperature());
			int tw = ambient(h, 1, 6), ti = ambient(h, 6, 6), tp = ambient(h, 6, 1); //the ambient temperature follows the sun: allow 1 deg C of lag
			h.assertTrue(Math.abs(water.getTemperature()-(tw-5)) <= 1, "composter beside water at "+water.getTemperature()+", ambient "+tw);
			h.assertTrue(Math.abs(ice.getTemperature()-(ti-15)) <= 1, "composter beside ice at "+ice.getTemperature()+", ambient "+ti);
			h.assertTrue(Math.abs(plain.getTemperature()-tp) <= 1, "composter at "+plain.getTemperature()+", ambient "+tp);
			h.assertTrue(fire.getMaxTemperature() == 100 && fire.allowExternalHeating() && fire.canBeCooledWithFins(), "composter heat rules");
			h.succeed();
		});
	}

	/** Matter goes into slot 0 and yeast into slot 1 from any side; only compost comes out; breaking it drops everything. */
	@GameTest(template = PLATFORM)
	public static void composterTakesMatterAndYeastAndGivesOnlyCompost(GameTestHelper h) {
		BlockPos pos = new BlockPos(4, 2, 4);
		ComposterBlockEntity te = composter(h, 4, 4);
		IItemHandler cap = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), Direction.UP);
		h.assertTrue(cap != null && cap.getSlots() == 3, "composter has no item handler");
		h.assertTrue(cap.insertItem(1, new ItemStack(Items.WHEAT, 4), false).getCount() == 4, "wheat went into the yeast slot");
		h.assertTrue(cap.insertItem(0, yeast(4), false).getCount() == 4, "yeast went into the matter slot");
		h.assertTrue(cap.insertItem(0, new ItemStack(Items.COBBLESTONE), false).getCount() == 1, "cobblestone went in");
		h.assertTrue(cap.insertItem(2, new ItemStack(compost()), false).getCount() == 1, "compost went into the output");
		h.assertTrue(cap.insertItem(0, new ItemStack(Items.WHEAT, 4), false).isEmpty(), "wheat refused");
		h.assertTrue(cap.insertItem(1, yeast(4), false).isEmpty(), "yeast refused");
		h.assertTrue(cap.extractItem(0, 4, false).isEmpty() && cap.extractItem(1, 4, false).isEmpty(), "matter or yeast came out");
		te.getInventory().setStackInSlot(2, new ItemStack(compost(), 5));
		h.assertTrue(cap.extractItem(2, 2, false).getCount() == 2, "compost did not come out");
		h.setBlock(pos, Blocks.AIR);
		int wheat = 0, yeast = 0, compost = 0;
		for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(pos)).inflate(3))) {
			ItemStack is = e.getItem();
			wheat += is.is(Items.WHEAT) ? is.getCount() : 0;
			yeast += is.is(RotaryItems.YEAST.get()) ? is.getCount() : 0;
			compost += is.is(compost()) ? is.getCount() : 0;
		}
		h.assertTrue(wheat == 4 && yeast == 4 && compost == 3, "dropped "+wheat+" wheat, "+yeast+" yeast, "+compost+" compost");
		h.succeed();
	}
}
