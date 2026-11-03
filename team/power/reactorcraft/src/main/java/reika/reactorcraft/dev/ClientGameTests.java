/*******************************************************************************
 * Test tool of the private NeoForge 1.21.1 ports (not part of the original mods): runs one GameTest batch inside a client's integrated
 * server, for mods that do not load on a dedicated (GameTest) server here (GregTech CEu Modern 7.0.2, see reactorcraft/build.gradle).
 * With the system property "reactorcraft.gametest.batch" (and neoforge.enableGameTest) it starts the tests of that batch in the opened
 * world a few seconds after it loads, logs progress and failures as the GameTest server does, ends with the same summary line
 * ("All N required tests passed :)" or "N required tests failed :("), writes it to gametest_result.txt and closes the client.
 ******************************************************************************/
package reika.reactorcraft.dev;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestBatchFactory;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.gametest.framework.StructureGridSpawner;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import reika.reactorcraft.ReactorCraft;

@EventBusSubscriber(modid = ReactorCraft.MOD_ID)
public final class ClientGameTests {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String BATCH = System.getProperty("reactorcraft.gametest.batch");

	private static int wait = 100;
	private static MultipleTestTracker tracker;
	/** Set once the summary is written: the client closes itself (ClientGameTestsExit). */
	public static volatile boolean finished;

	private ClientGameTests() {}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post e) {
		if (BATCH == null || finished)
			return;
		ServerLevel w = e.getServer().overworld();
		if (tracker == null) {
			if (--wait > 0)
				return;
			List<TestFunction> fs = GameTestRegistry.getAllTestFunctions().stream().filter(f -> f.batchName().equals(BATCH)).toList();
			BlockPos at = new BlockPos(20000, -59, 20000);
			GameTestRunner runner = GameTestRunner.Builder.fromBatches(GameTestBatchFactory.fromTestFunction(fs, w), w)
					.newStructureSpawner(new StructureGridSpawner(at, 8, false)).build();
			Collection<GameTestInfo> infos = runner.getTestInfos();
			tracker = new MultipleTestTracker(infos);
			LOGGER.info("{} tests of batch {} are now running at position {}!", tracker.getTotalCount(), BATCH, at.toShortString());
			runner.start();
			return;
		}
		if (w.getGameTime()%20 == 0)
			LOGGER.info(tracker.getProgressBar());
		if (tracker.isDone()) {
			GlobalTestReporter.finish();
			String sum = tracker.hasFailedRequired() ? tracker.getFailedRequiredCount()+" required tests failed :(" : "All "+tracker.getTotalCount()+" required tests passed :)";
			LOGGER.info("========= {} GAME TESTS OF BATCH {} COMPLETE ======================", tracker.getTotalCount(), BATCH);
			LOGGER.info(sum);
			StringBuilder sb = new StringBuilder(sum).append('\n');
			tracker.getFailedRequired().forEach(t -> {
				LOGGER.info("   - {}: {}", t.getTestName(), t.getError() != null ? t.getError().getMessage() : "");
				sb.append("   - ").append(t.getTestName()).append(": ").append(t.getError() != null ? t.getError().getMessage() : "").append('\n');
			});
			try {
				Files.writeString(Path.of("gametest_result.txt"), sb.toString());
			}
			catch (java.io.IOException ex) {
				LOGGER.error("could not write gametest_result.txt", ex);
			}
			finished = true;
		}
	}
}
