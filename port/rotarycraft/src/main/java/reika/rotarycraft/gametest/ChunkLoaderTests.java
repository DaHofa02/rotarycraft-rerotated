/*******************************************************************************
 * Tests for the ported RotaryCraft chunk loader (original TileEntityChunkLoader). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.ChunkLoaderBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ChunkLoaderTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Radius (power - 2097152)/524288 chunks, at most 8; nothing below 2097152 rad/s. */
	@GameTest(template = PLATFORM)
	public static void chunkLoaderFollowsTheOriginalNumbers(GameTestHelper h) {
		ChunkLoaderBlockEntity te = place(h, RotaryBlocks.CHUNK_LOADER.get(), 4, 2, 4, 0, ChunkLoaderBlockEntity.class);
		long[][] cases = {{2097152, 0}, {2097152+3*524288, 3}, {2097152+3*524288+524287, 3}, {1L << 30, 8}};
		for (long[] c : cases) {
			te.power = c[0];
			h.assertTrue(te.getLoadingRadius() == c[1] && te.getRange() == 16*c[1], c[0]+" W: radius "+te.getLoadingRadius());
		}
		h.assertTrue(te.getMaxRange() == 128 && te.MINSPEED == 2097152, "max range "+te.getMaxRange()+", min speed "+te.MINSPEED);
		h.runAtTickTime(10, () -> {
			h.assertFalse(te.isLoading(), "loading without power");
			h.succeed();
		});
	}

	/**
	 * Gas turbine (east; its intake against the wall) -> two bedrock 16:1 gearboxes as speed-up -> bevel gears up -> chunk loader: far
	 * above 2097152 rad/s and so many watts that it holds the full 17x17 chunks; the corner chunk 8 away ticks. Taken away, it lets go.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "chunkloader")
	public static void chunkLoaderHoldsItsChunks(GameTestHelper h) {
		place(h, RotaryBlocks.JET_ENGINE.get(), 1, 2, 4, 1, JetEngineBlockEntity.class).addFuel(100000);
		for (int x = 2; x <= 3; x++) {
			GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), x, 2, 4, 13, GearboxBlockEntity.class);
			gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16));
			gbx.setBlockMetadata(13);
			gbx.reduction = false;
			gbx.fillWithLubricant();
		}
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		ChunkLoaderBlockEntity te = place(h, RotaryBlocks.CHUNK_LOADER.get(), 4, 3, 4, 0, ChunkLoaderBlockEntity.class);
		h.succeedWhen(() -> {
			h.assertTrue(te.getOmega() >= 2097152 && te.getLoadedRadius() == 8, "radius "+te.getLoadedRadius()+" at "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			ChunkPos c = new ChunkPos(h.absolutePos(new BlockPos(4, 3, 4)));
			h.assertTrue(h.getLevel().shouldTickBlocksAt(ChunkPos.asLong(c.x+8, c.z-8)), "corner chunk not ticking");
			h.setBlock(new BlockPos(4, 3, 4), Blocks.AIR);
			h.assertFalse(te.isLoading(), "still loading when removed");
		});
	}
}
