/*******************************************************************************
 * Tests for the ported RotaryCraft light bridge (original TileEntityLightBridge). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.BridgeBlock;
import reika.rotarycraft.content.blockentity.LightBridgeBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class LightBridgeTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** A light bridge at (6, 2, 4) (meta 0: input east, bridge west) with glowstone on top (light 15 above it, day or night). */
	private static LightBridgeBlockEntity bridge(GameTestHelper h) {
		h.setBlock(new BlockPos(6, 3, 4), Blocks.GLOWSTONE);
		return place(h, RotaryBlocks.LIGHT_BRIDGE.get(), 6, 2, 4, 0, LightBridgeBlockEntity.class);
	}

	/**
	 * A gas turbine choked to a quarter by a wall in its intake (16384 rad/s @ 1024 Nm = 16777216 W): range 64 * 16777216 / 33554432 =
	 * 32. Plates (direction 3 for meta 0) are laid west up to the stone block, through the tall grass, none behind the stone. Breaking
	 * the light bridge removes them.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void lightBridgeLaysPlates(GameTestHelper h) {
		JetEngineBlockEntity jet = place(h, RotaryBlocks.JET_ENGINE.get(), 7, 2, 4, 0, JetEngineBlockEntity.class);
		h.setBlock(new BlockPos(8, 2, 4), Blocks.COBBLESTONE_WALL);
		jet.addFuel(100000);
		LightBridgeBlockEntity te = bridge(h);
		h.setBlock(new BlockPos(4, 2, 4), Blocks.SHORT_GRASS);
		h.setBlock(new BlockPos(2, 2, 4), Blocks.STONE);
		boolean[] broken = {false};
		h.succeedWhen(() -> {
			if (!broken[0]) {
				h.assertTrue(te.getPower() == 16777216 && te.getRange() == 32, "light bridge "+te.getPower()+" W, range "+te.getRange());
				for (int x = 3; x <= 5; x++) {
					h.assertBlockPresent(RotaryBlocks.BRIDGE.get(), new BlockPos(x, 2, 4));
					h.assertTrue(h.getBlockState(new BlockPos(x, 2, 4)).getValue(BridgeBlock.DIR) == 3, "plate direction");
				}
				h.assertBlockPresent(Blocks.AIR, new BlockPos(1, 2, 4));
				h.destroyBlock(new BlockPos(6, 2, 4));
				broken[0] = true;
				h.fail("bridge laid; now broken");
			}
			for (int x = 3; x <= 5; x++)
				h.assertBlockNotPresent(RotaryBlocks.BRIDGE.get(), new BlockPos(x, 2, 4));
		});
	}

	/** A gas engine's 65536 W gives range 64 * 65536 / 33554432 = 0: no bridge. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void lightBridgeNeedsHalfAMegawatt(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 7, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
		LightBridgeBlockEntity te = bridge(h);
		h.runAtTickTime(150, () -> {
			h.assertTrue(te.getPower() == 65536 && te.getRange() == 0, "light bridge "+te.getPower()+" W, range "+te.getRange());
			h.assertBlockNotPresent(RotaryBlocks.BRIDGE.get(), new BlockPos(5, 2, 4));
			h.succeed();
		});
	}
}
