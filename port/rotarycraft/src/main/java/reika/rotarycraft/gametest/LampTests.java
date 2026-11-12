/*******************************************************************************
 * Tests for the ported RotaryCraft lamp (original TileEntityLamp). Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not
 * for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.LampBlockEntity;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class LampTests {

	private static final String PLATFORM = "platform_9x8x9";
	private static final BlockPos LAMP = new BlockPos(4, 3, 4);

	private static LampBlockEntity lamp(GameTestHelper h, int charge) {
		h.setBlock(LAMP, RotaryBlocks.LAMP.get());
		LampBlockEntity te = (LampBlockEntity)h.getBlockEntity(LAMP);
		if (charge > 0)
			te.getInventory().setStackInSlot(0, CoilItem.withCharge(new ItemStack(RotaryItems.SPRING.get()), charge));
		return te;
	}

	private static boolean isLight(GameTestHelper h, int x, int y, int z) {
		return h.getBlockState(new BlockPos(x, y, z)).is(Blocks.LIGHT);
	}

	private static void assertLit(GameTestHelper h, int x, int y, int z) {
		h.assertTrue(isLight(h, x, y, z) && h.getBlockState(new BlockPos(x, y, z)).getValue(LightBlock.LEVEL) == 15, "no light at "+x+" "+y+" "+z+": "+h.getBlockState(new BlockPos(x, y, z)));
	}

	private static void assertDark(GameTestHelper h, int x, int y, int z) {
		h.assertFalse(isLight(h, x, y, z), "light at "+x+" "+y+" "+z);
	}

	/**
	 * With a wound coil the lamp sets level-15 light blocks into the air along the six axes (a stone block on the way is skipped, the air
	 * behind it still lit) and on every second diagonal spot (2, 4, ...), above and below too; the spots between stay air.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void lampLightsTheAirAround(GameTestHelper h) {
		h.setBlock(new BlockPos(3, 3, 4), Blocks.STONE);
		LampBlockEntity te = lamp(h, 100);
		h.succeedWhen(() -> {
			h.assertTrue(te.isLit(), "lamp not lit");
			assertLit(h, 5, 3, 4);
			assertLit(h, 8, 3, 4);
			assertLit(h, 2, 3, 4);
			h.assertBlockPresent(Blocks.STONE, new BlockPos(3, 3, 4));
			assertLit(h, 4, 7, 4);
			assertLit(h, 4, 2, 4);
			assertLit(h, 4, 3, 0);
			assertLit(h, 6, 3, 6);
			assertLit(h, 2, 5, 2);
			assertLit(h, 8, 7, 8);
			assertDark(h, 5, 3, 5);
			assertDark(h, 7, 3, 7);
			assertDark(h, 6, 4, 6);
			h.assertTrue(h.getLevel().getBrightness(LightLayer.BLOCK, h.absolutePos(new BlockPos(5, 3, 5))) == 14, "air beside the lights only at "+h.getLevel().getBrightness(LightLayer.BLOCK, h.absolutePos(new BlockPos(5, 3, 5))));
		});
	}

	/** A redstone signal puts the lights out and stops the coil unwinding; without it they come back. */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void lampGoesOutWithRedstone(GameTestHelper h) {
		LampBlockEntity te = lamp(h, 100);
		BlockPos red = new BlockPos(4, 3, 5);
		int[] stage = {0};
		int[] t = {0};
		h.succeedWhen(() -> {
			if (stage[0] == 0) {
				assertLit(h, 6, 3, 4);
				h.setBlock(red, Blocks.REDSTONE_BLOCK);
				stage[0] = 1;
				t[0] = (int)h.getTick();
				h.fail("redstone block set");
			}
			if (stage[0] == 1) {
				assertDark(h, 6, 3, 4);
				h.assertFalse(te.isLit(), "lamp lit with a signal");
				h.assertTrue(h.getTick()-t[0] >= 200, "waiting");
				h.assertTrue(te.getCoilCharge() == 100, "coil unwound with a signal: "+te.getCoilCharge());
				h.setBlock(red, Blocks.AIR);
				stage[0] = 2;
				h.fail("redstone block removed");
			}
			assertLit(h, 6, 3, 4);
		});
	}

	/** One charge lasts 120 ticks with a plain spring; then the lamp goes dark. Without a coil it never lights. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void lampRunsOnItsCoil(GameTestHelper h) {
		LampBlockEntity te = lamp(h, 1);
		h.runAtTickTime(60, () -> {
			assertLit(h, 6, 3, 4);
			h.assertTrue(te.getCoilCharge() == 1, "charge after 60 ticks: "+te.getCoilCharge());
		});
		h.runAtTickTime(140, () -> {
			h.assertTrue(te.getCoilCharge() == 0 && !te.isLit(), "lamp after 140 ticks: charge "+te.getCoilCharge()+", lit "+te.isLit());
			assertDark(h, 6, 3, 4);
			assertDark(h, 4, 7, 4);
			h.setBlock(LAMP, Blocks.AIR);
			LampBlockEntity empty = lamp(h, 0);
			h.runAfterDelay(40, () -> {
				h.assertFalse(empty.isLit(), "lamp without a coil lit");
				assertDark(h, 6, 3, 4);
				h.succeed();
			});
		});
	}

	/** Breaking the lamp drops the coil and removes its lights, but not a block built on a lit spot since. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void lampTakesItsLightsAlongWhenBroken(GameTestHelper h) {
		LampBlockEntity te = lamp(h, 50);
		h.succeedWhen(() -> {
			assertLit(h, 6, 3, 4);
			h.assertTrue(te.getLightSpots().contains(h.absolutePos(new BlockPos(7, 3, 4))), "spot not collected");
			h.setBlock(new BlockPos(7, 3, 4), Blocks.GLASS);
			h.setBlock(LAMP, Blocks.AIR);
			assertDark(h, 6, 3, 4);
			assertDark(h, 4, 6, 4);
			h.assertBlockPresent(Blocks.GLASS, new BlockPos(7, 3, 4));
			int coils = 0;
			for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(LAMP)).inflate(2)))
				if (e.getItem().is(RotaryItems.SPRING.get()) && CoilItem.getCharge(e.getItem()) == 50)
					coils++;
			h.assertTrue(coils == 1, coils+" coils dropped");
		});
	}
}
