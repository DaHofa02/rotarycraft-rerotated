/*******************************************************************************
 * Tests for the ported RotaryCraft smoke detector (original TileEntitySmokeDetector / TileEntitySpringPowered). Private NeoForge 1.21.1
 * port of RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.SmokeDetectorBlockEntity;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SmokeDetectorTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static SmokeDetectorBlockEntity detector(GameTestHelper h, int x, int y, int z, int charge) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, RotaryBlocks.SMOKE_DETECTOR.get());
		SmokeDetectorBlockEntity te = (SmokeDetectorBlockEntity)h.getBlockEntity(p);
		if (charge >= 0)
			te.getInventory().setStackInSlot(0, coil(charge));
		return te;
	}

	private static ItemStack coil(int charge) {
		return CoilItem.withCharge(new ItemStack(RotaryItems.SPRING.get()), charge);
	}

	/**
	 * A detector with a wound coil alarms while fire burns within 8 blocks: weak redstone power 15 (a lamp beside it lights), a beep
	 * every 4 ticks. One without a coil does nothing. When the fire is out the alarm ends.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void detectorAlarmsOnFire(GameTestHelper h) {
		SmokeDetectorBlockEntity te = detector(h, 4, 4, 4, 100);
		SmokeDetectorBlockEntity dead = detector(h, 4, 4, 6, -1);
		BlockPos lamp = new BlockPos(5, 4, 4);
		h.setBlock(lamp, Blocks.REDSTONE_LAMP);
		BlockPos fire = new BlockPos(7, 2, 1);
		h.runAtTickTime(10, () -> {
			h.assertTrue(!te.isAlarming() && te.soundDelay == -1, "alarm without fire");
			h.assertFalse(h.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "lamp lit without fire");
			h.setBlock(fire.below(), Blocks.NETHERRACK);
			h.setBlock(fire, Blocks.FIRE);
		});
		h.runAtTickTime(20, () -> {
			h.assertTrue(te.isAlarming() && te.soundDelay == 4, "no alarm, sound delay "+te.soundDelay);
			h.assertTrue(h.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "lamp not lit");
			h.assertTrue(h.getLevel().getSignal(h.absolutePos(new BlockPos(4, 4, 4)), Direction.WEST) == 15, "no redstone signal");
			h.assertFalse(dead.isAlarming(), "detector without coil alarms");
			h.setBlock(fire, Blocks.AIR);
		});
		h.runAtTickTime(40, () -> {
			h.assertFalse(te.isAlarming(), "alarm stays without fire");
			h.assertFalse(h.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "lamp stays lit");
			h.succeed();
		});
	}

	/**
	 * One unit of charge lasts 1200 ticks for a spring (stiffness 1): 9 units become 8 after 1200 ticks, and at 8 or less the low
	 * battery chirp starts (every 600 ticks). Range shown by the tools: log2(charge^2), at most 8.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1400)
	public static void detectorUnwindsItsCoil(GameTestHelper h) {
		SmokeDetectorBlockEntity te = detector(h, 4, 4, 4, 9);
		h.assertTrue(te.getExpectedCoilLife() == 1200*9 && te.getRange() == 6, "life "+te.getExpectedCoilLife()+", range "+te.getRange());
		h.runAtTickTime(100, () -> {
			h.assertTrue(te.getCoilCharge() == 9 && !te.isLowBattery() && te.soundDelay == -1, "charge "+te.getCoilCharge()+", delay "+te.soundDelay);
		});
		h.runAtTickTime(1250, () -> {
			h.assertTrue(te.getCoilCharge() == 8, "charge "+te.getCoilCharge());
			h.assertTrue(te.isLowBattery() && te.soundDelay == 600 && !te.isAlarming(), "low battery "+te.isLowBattery()+", delay "+te.soundDelay);
			h.succeed();
		});
	}

	/** The slot takes one coil; automation gets it back only when it is unwound. A strong coil lasts 16 times as long. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void detectorKeepsAWoundCoil(GameTestHelper h) {
		SmokeDetectorBlockEntity te = detector(h, 4, 4, 4, -1);
		IItemHandler side = te.getItemHandler(Direction.UP);
		h.assertTrue(te.getRange() == 0 && !te.lowBattery(), "range without coil");
		h.assertTrue(side.insertItem(0, new ItemStack(Items.IRON_INGOT), false).getCount() == 1, "took an ingot");
		h.assertTrue(side.insertItem(0, coil(300), false).isEmpty(), "took no coil");
		h.assertTrue(side.insertItem(0, coil(300), false).getCount() == 1, "took a second coil");
		h.assertTrue(te.getRange() == 8, "range "+te.getRange());
		h.assertTrue(side.extractItem(0, 1, false).isEmpty(), "gave a wound coil back");
		te.getInventory().setStackInSlot(0, coil(0));
		h.assertTrue(side.extractItem(0, 1, true).is(RotaryItems.SPRING.get()), "kept an unwound coil");
		te.getInventory().setStackInSlot(0, CoilItem.withCharge(new ItemStack(RotaryItems.STRONGCOIL.get()), 10));
		h.assertTrue(te.getExpectedCoilLife() == 1200*16*10, "strong coil life "+te.getExpectedCoilLife());
		h.succeed();
	}
}
