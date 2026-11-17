/*******************************************************************************
 * Tests for the ported RotaryCraft cave scanner (original TileEntityCaveFinder). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.CaveScannerBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CaveScannerTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	private static boolean point(CaveScannerBlockEntity te, GameTestHelper h, BlockPos corner) {
		BlockPos d = h.absolutePos(corner).subtract(te.getSource());
		int r = te.getRange();
		return te.hasPointAt(d.getX()+r, d.getY()+r, d.getZ()+r);
	}

	/**
	 * A choked gas turbine (16777216 W) beside the scanner: corners on the floor surface (half stone, half air) and around a stone block
	 * are mapped, corners in open air and inside solid stone are not. Moving the source 4 up shifts the map.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void caveScannerMapsSurfaces(GameTestHelper h) {
		place(h, RotaryBlocks.JET_ENGINE.get(), 3, 2, 4, 0, JetEngineBlockEntity.class).addFuel(100000);
		h.setBlock(new BlockPos(4, 2, 4), Blocks.COBBLESTONE_WALL);
		CaveScannerBlockEntity te = place(h, RotaryBlocks.CAVE_SCANNER.get(), 2, 2, 4, 0, CaveScannerBlockEntity.class);
		h.setBlock(new BlockPos(6, 4, 2), Blocks.STONE);
		for (int x = 5; x <= 6; x++) //a 2x2x2 stone cube: its middle corner (6, 3, 6) is surrounded by stone
			for (int y = 2; y <= 3; y++)
				for (int z = 5; z <= 6; z++)
					h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
		h.assertFalse(te.on, "on without power");
		boolean[] moved = {false};
		h.succeedWhen(() -> {
			h.assertTrue(te.on, "scanner off at "+te.getPower()+" W");
			if (!moved[0]) {
				h.assertTrue(point(te, h, new BlockPos(6, 2, 7)), "no point on the floor");
				h.assertTrue(point(te, h, new BlockPos(7, 5, 3)), "no point at the stone block's corner");
				h.assertFalse(point(te, h, new BlockPos(6, 6, 7)), "point in open air");
				h.assertFalse(point(te, h, new BlockPos(6, 3, 6)), "point inside the stone cube");
				te.moveSrc(4, Direction.UP);
				moved[0] = true;
				h.fail("mapped; now moved");
			}
			h.assertTrue(te.getSource().equals(h.absolutePos(new BlockPos(2, 6, 4))), "source at "+te.getSource());
			h.assertTrue(point(te, h, new BlockPos(6, 2, 7)), "floor point lost after moving 4 up (still in range)");
		});
	}

	/** A gas engine's 65536 W are below the 131072 W it needs: off, nothing mapped. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void caveScannerNeedsPower(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		CaveScannerBlockEntity te = place(h, RotaryBlocks.CAVE_SCANNER.get(), 2, 2, 4, 0, CaveScannerBlockEntity.class);
		h.runAtTickTime(60, () -> {
			h.assertTrue(te.getPower() == 65536 && !te.on && !point(te, h, new BlockPos(6, 2, 7)), "scanner "+te.getPower()+" W, on "+te.on);
			h.succeed();
		});
	}
}
