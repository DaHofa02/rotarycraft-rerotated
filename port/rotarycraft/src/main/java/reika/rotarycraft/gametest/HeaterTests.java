/*******************************************************************************
 * Tests for the ported RotaryCraft heater (original TileEntityHeater). Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki;
 * not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.BlastFurnaceBlockEntity;
import reika.rotarycraft.content.blockentity.HeaterBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class HeaterTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine (512 rad/s @ 128 Nm = 65536 W, output east) -> bevel gears (west -> up) -> heater at (4, 3, 4). */
	private static HeaterBlockEntity rig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		return place(h, RotaryBlocks.HEATER.get(), 4, 3, 4, 0, HeaterBlockEntity.class);
	}

	/**
	 * At 512 rad/s the heater works every 200 - 10*log2(513) = 109 ticks. Coal (1600 ticks -> 64 deg C, times 1.5 = 96 per piece)
	 * towards 300 deg C: three pieces bring it above 250 deg C; the fourth is only burnt once more than 64 deg C are missing, so the
	 * set temperature is never passed.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 900)
	public static void heaterBurnsFuelTowardsItsSetTemperature(GameTestHelper h) {
		HeaterBlockEntity te = rig(h);
		te.getInventory().setStackInSlot(3, new ItemStack(Items.COAL, 8));
		te.setSetTemperature(300);
		h.onEachTick(() -> h.assertTrue(te.getTemperature() <= 300, "heater passed its set temperature: "+te.getTemperature()));
		h.succeedWhen(() -> {
			h.assertTrue(te.power == 65536 && te.getOperationTime() == 109, "heater at "+te.power+" W, works every "+te.getOperationTime());
			h.assertTrue(te.getTemperature() >= 250, "heater at "+te.getTemperature()+" deg C");
			h.assertTrue(te.getInventory().getStackInSlot(3).getCount() == 5, te.getInventory().getStackInSlot(3).getCount()+" coal left");
			h.assertTrue(!te.isIdle() && te.getRedstoneOverride() == 0, "heater idle with usable coal");
		});
	}

	/**
	 * Set to 50 deg C, coal (64 deg C) is too hot to be used: the heater idles, signals 15 and burns nothing. A stick (100 ticks ->
	 * 4 deg C) is usable and is burnt. Fuel only: stone is refused, and nothing can be pulled out.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void heaterIdlesWithoutUsefulFuel(GameTestHelper h) {
		HeaterBlockEntity te = rig(h);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.COAL, 8));
		te.setSetTemperature(50);
		h.assertTrue(te.getItemHandler(Direction.UP).insertItem(1, new ItemStack(Items.STONE), false).getCount() == 1, "heater took stone");
		h.assertTrue(te.getItemHandler(Direction.DOWN).extractItem(0, 1, false).isEmpty(), "coal pulled out of the heater");
		boolean[] stick = {false};
		h.succeedWhen(() -> {
			h.assertTrue(te.power == 65536, "heater at "+te.power+" W");
			h.assertTrue(te.getInventory().getStackInSlot(0).getCount() == 8, "coal burnt for 50 deg C");
			if (!stick[0]) {
				h.assertTrue(h.getTick() > 250, "waiting two working cycles");
				h.assertTrue(te.isIdle() && te.getRedstoneOverride() == 15, "heater not idle with unusable coal");
				h.assertTrue(te.getItemHandler(Direction.UP).insertItem(1, new ItemStack(Items.STICK, 2), false).isEmpty(), "heater took no sticks");
				stick[0] = true;
				h.fail("sticks put in; checking next tick");
			}
			h.assertTrue(!te.isIdle() && te.getRedstoneOverride() == 0, "heater idle with usable sticks");
			h.assertTrue(te.getInventory().getStackInSlot(1).getCount() < 2, "no stick burnt");
		});
	}

	/** A DC engine gives 1024 W, below the 8192 W the heater needs: nothing is burnt and it never tests for idling. */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void heaterNeedsItsPower(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class);
		h.setBlock(new BlockPos(3, 2, 3), Blocks.REDSTONE_BLOCK);
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		HeaterBlockEntity te = place(h, RotaryBlocks.HEATER.get(), 4, 3, 4, 0, HeaterBlockEntity.class);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.COAL, 8));
		te.setSetTemperature(300);
		h.runAtTickTime(350, () -> {
			h.assertTrue(te.power == 1024, "heater at "+te.power+" W");
			h.assertTrue(te.getInventory().getStackInSlot(0).getCount() == 8 && te.getTemperature() < 60, "heater works at 1024 W: "+te.getTemperature());
			h.assertTrue(!te.isIdle(), "unpowered heater tested for idling");
			h.succeed();
		});
	}

	/**
	 * A lava bucket (20000 ticks -> 800 deg C, times 1.5 = 1200) leaves its empty bucket in the heater. The blast furnace on top is
	 * heated with it.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void heaterHeatsTheMachineAboveAndKeepsTheBucket(GameTestHelper h) {
		HeaterBlockEntity te = rig(h);
		BlastFurnaceBlockEntity furnace = place(h, RotaryBlocks.BLAST_FURNACE.get(), 4, 4, 4, 0, BlastFurnaceBlockEntity.class);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.LAVA_BUCKET));
		te.setSetTemperature(900);
		h.succeedWhen(() -> {
			h.assertTrue(te.getInventory().getStackInSlot(0).is(Items.BUCKET), "no empty bucket: "+te.getInventory().getStackInSlot(0));
			h.assertTrue(te.getTemperature() >= 1000 && te.getTemperature() <= 1230, "heater at "+te.getTemperature()+" deg C");
			h.assertTrue(furnace.getTemperature() >= 900, "blast furnace at "+furnace.getTemperature()+" deg C");
			h.assertTrue(te.isIdle(), "heater not idle without fuel");
		});
	}

	/** Above 2000 deg C the heater explodes: 0-17 scrap drop (its fuel is lost in the blast) and lava takes its place (here walled in by stone). */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void heaterOverheatsIntoLava(GameTestHelper h) {
		BlockPos pos = new BlockPos(4, 2, 4);
		for (Direction d : Direction.Plane.HORIZONTAL)
			h.setBlock(pos.relative(d), Blocks.OBSIDIAN);
		HeaterBlockEntity te = place(h, RotaryBlocks.HEATER.get(), 4, 2, 4, 0, HeaterBlockEntity.class);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.COAL, 3));
		te.setSetTemperature(2100);
		te.overheat(h.getLevel(), h.absolutePos(pos));
		h.assertBlockPresent(Blocks.LAVA, pos);
		h.assertTrue(te.getTemperature() == 0 && te.getSetTemperature() == 0, "temperatures not reset");
		int coal = 0, scrap = 0;
		for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(pos)).inflate(6))) {
			if (e.getItem().is(Items.COAL))
				coal += e.getItem().getCount();
			if (e.getItem().is(RotaryItems.part(CraftingPart.SCRAP)))
				scrap += e.getItem().getCount();
			e.discard();
		}
		h.assertTrue(coal <= 3 && scrap <= 17, coal+" coal, "+scrap+" scrap dropped");
		h.setBlock(pos, Blocks.STONE);
		h.succeed();
	}
}
