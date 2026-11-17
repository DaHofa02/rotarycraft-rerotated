/*******************************************************************************
 * Tests for the ported RotaryCraft spawner controller (original TileEntitySpawnerController). Private NeoForge 1.21.1 port of RotaryCraft
 * by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.SpawnerControllerBlockEntity;
import reika.rotarycraft.content.blockentity.engine.PerformanceEngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SpawnerControllerTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Shortest delay 800 - 40*log2(omega); the GUI shows at least that. Without a spawner below it forgets its delay. */
	@GameTest(template = PLATFORM)
	public static void spawnerControllerFollowsTheOriginalNumbers(GameTestHelper h) {
		SpawnerControllerBlockEntity te = place(h, RotaryBlocks.SPAWNER_CONTROLLER.get(), 4, 3, 4, 0, SpawnerControllerBlockEntity.class);
		int[][] cases = {{0, 800}, {1024, 400}, {1 << 20, 0}, {1 << 25, 0}};
		for (int[] c : cases) {
			te.omega = c[0];
			h.assertTrue(te.getOperationTime() == c[1], c[0]+" rad/s: "+te.getOperationTime()+" ticks");
		}
		te.omega = 1024;
		te.handleGui(100);
		h.assertTrue(te.getDelay() == 400 && !te.disable, "delay shown "+te.getDelay());
		te.handleGui(-1);
		h.assertTrue(te.disable && te.getSetDelay() == 100, "disable");
		h.runAtTickTime(5, () -> {
			h.assertTrue(te.getSetDelay() == 0 && !te.disable && te.getPower() == 0, "no spawner, but delay "+te.getSetDelay());
			h.succeed();
		});
	}

	/**
	 * A pig spawner on grass, the controller on top and a sport engine (262144 W) driving into the spawner from the west. No player is near,
	 * so the spawner alone would never spawn; with a delay of 40 ticks the controller makes it spawn pigs that never despawn. Disabled, it
	 * holds the spawner at 5 ticks.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "spawner")
	public static void spawnerControllerMakesPigs(GameTestHelper h) {
		for (int x = 0; x <= 8; x++)
			for (int z = 0; z <= 8; z++)
				h.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
		h.setBlock(new BlockPos(4, 2, 4), Blocks.SPAWNER);
		SpawnerBlockEntity sp = (SpawnerBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		sp.setEntityId(EntityType.PIG, h.getLevel().random);
		SpawnerControllerBlockEntity te = place(h, RotaryBlocks.SPAWNER_CONTROLLER.get(), 4, 3, 4, 0, SpawnerControllerBlockEntity.class);
		PerformanceEngineBlockEntity e = place(h, RotaryBlocks.SPORT_ENGINE.get(), 3, 2, 4, 1, PerformanceEngineBlockEntity.class);
		e.addFuel(50000);
		e.addWater(50000);
		e.additives = 10000;
		te.handleGui(40);
		boolean[] off = {false};
		long[] since = {0};
		h.succeedWhen(() -> {
			List<Pig> pigs = h.getLevel().getEntitiesOfClass(Pig.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(12));
			if (!off[0]) {
				h.assertTrue(te.getPower() == 262144, "controller at "+te.getPower()+" W");
				h.assertTrue(!pigs.isEmpty(), "no pig yet");
				for (Pig pig : pigs)
					h.assertTrue(pig.isPersistenceRequired() && SpawnerControllerBlockEntity.isFlaggedNoDespawn(pig), "pig not flagged");
				te.handleGui(-1);
				off[0] = true;
				since[0] = pigs.size();
				h.fail("now disabled");
			}
			h.assertTrue(sp.getSpawner().spawnDelay == 5, "spawner delay "+sp.getSpawner().spawnDelay+" while disabled");
			h.assertTrue(pigs.size() == since[0], "pigs while disabled: "+pigs.size()+" vs "+since[0]);
			for (Pig pig : pigs)
				pig.discard();
		});
	}
}
