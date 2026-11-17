/*******************************************************************************
 * Tests for the ported RotaryCraft weather controller (original TileEntityWeatherController). Private NeoForge 1.21.1 port of RotaryCraft
 * by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.WeatherControllerBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WeatherControllerTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine (65536 W) -> bevel gears (west -> up) -> weather controller on top. */
	private static WeatherControllerBlockEntity rig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 2, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		return place(h, RotaryBlocks.WEATHER_CONTROLLER.get(), 2, 3, 4, 0, WeatherControllerBlockEntity.class);
	}

	private static void clear(GameTestHelper h) {
		h.getLevel().setWeatherParameters(24000, 0, false, false);
		h.getLevel().setRainLevel(0); //otherwise the rain fades out over 100 ticks, into later tests
		h.getLevel().setThunderLevel(0);
	}

	/**
	 * The structure stays after the test, and the controller keeps its last mode and sets it again whenever its cooldown runs out (as in the
	 * original, even with an empty store): remove it so it cannot change the weather for later tests.
	 */
	private static void done(GameTestHelper h, WeatherControllerBlockEntity te) {
		for (int i = 0; i < te.getInventory().getSlots(); i++)
			te.getInventory().setStackInSlot(i, ItemStack.EMPTY);
		h.setBlock(h.relativePos(te.getBlockPos()), Blocks.AIR);
		clear(h);
	}

	/** Silver iodide and redstone under a clear sky: a thunderstorm, both used up (fired into the sky), cooldown 200-599 ticks. */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "weather")
	public static void weatherControllerMakesThunder(GameTestHelper h) {
		clear(h);
		WeatherControllerBlockEntity te = rig(h);
		te.getInventory().setStackInSlot(0, new ItemStack(RotaryItems.part(CraftingPart.SILVERIODIDE), 2));
		te.getInventory().setStackInSlot(5, new ItemStack(Items.REDSTONE, 3));
		h.succeedWhen(() -> {
			h.assertTrue(h.getLevel().isRaining() && h.getLevel().isThundering(), "no thunderstorm at "+te.getPower()+" W");
			h.assertTrue(te.getMode() == WeatherControllerBlockEntity.RainMode.THUNDER && te.getCooldown() > 150 && te.getCooldown() < 600, "mode "+te.getMode()+", cooldown "+te.getCooldown());
			h.assertTrue(te.getInventory().getStackInSlot(0).getCount() == 1 && te.getInventory().getStackInSlot(5).getCount() == 2, "items not used up");
			done(h, te);
		});
	}

	/** Sawdust in the rain clears the sky; it wins over silver iodide in the same store. */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "weather2")
	public static void weatherControllerClearsTheSky(GameTestHelper h) {
		h.getLevel().setWeatherParameters(0, 24000, true, false);
		WeatherControllerBlockEntity te = rig(h);
		te.getInventory().setStackInSlot(3, new ItemStack(RotaryItems.part(CraftingPart.SILVERIODIDE)));
		te.getInventory().setStackInSlot(7, new ItemStack(RotaryItems.part(CraftingPart.SAWDUST), 4));
		h.succeedWhen(() -> {
			h.assertFalse(h.getLevel().isRaining(), "still raining at "+te.getPower()+" W");
			h.assertTrue(te.getMode() == WeatherControllerBlockEntity.RainMode.SUN, "mode "+te.getMode());
			h.assertTrue(te.getInventory().getStackInSlot(7).getCount() == 3 && te.getInventory().getStackInSlot(3).getCount() == 1, "wrong items used");
			done(h, te);
		});
	}

	/** Only weather items go in, nothing comes out; under a roof nothing is used. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void weatherControllerNeedsTheSky(GameTestHelper h) {
		WeatherControllerBlockEntity te = rig(h);
		h.setBlock(new BlockPos(2, 5, 4), Blocks.STONE);
		h.assertTrue(te.getInventory().insertItem(0, new ItemStack(Items.DIRT), true).getCount() == 1, "dirt accepted");
		te.getInventory().setStackInSlot(0, new ItemStack(Items.GLOWSTONE_DUST, 2));
		te.getInventory().setStackInSlot(1, new ItemStack(RotaryItems.part(CraftingPart.SILVERIODIDE), 2));
		h.assertTrue(te.getItemHandler(Direction.DOWN).extractItem(0, 1, true).isEmpty(), "items extracted");
		h.runAtTickTime(60, () -> {
			h.assertTrue(te.getPower() == 65536, "power "+te.getPower());
			h.assertTrue(te.getInventory().getStackInSlot(1).getCount() == 2 && te.getMode() == WeatherControllerBlockEntity.RainMode.NONE, "used under a roof");
			done(h, te);
			h.succeed();
		});
	}
}
