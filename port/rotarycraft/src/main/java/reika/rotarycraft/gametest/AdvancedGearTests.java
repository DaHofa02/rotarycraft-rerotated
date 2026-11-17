/*******************************************************************************
 * Tests for the port's worm gear and high-ratio gear (RotaryCraft advanced gears). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.AdvancedGearBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.PerformanceEngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AdvancedGearTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine (512 rad/s @ 128 Nm) into the worm gear (meta 1: input from the west): 8192 Nm and 512/64 * 92% = 7 rad/s. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void wormGearReducesWithLosses(GameTestHelper h) {
		h.assertTrue(Math.abs(AdvancedGearBlockEntity.wormEfficiency(512)-0.92) < 1e-9 && Math.abs(AdvancedGearBlockEntity.wormEfficiency(65536)-0.64) < 1e-9, "efficiency table");
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		AdvancedGearBlockEntity te = place(h, RotaryBlocks.WORM_GEAR.get(), 2, 2, 4, 1, AdvancedGearBlockEntity.class);
		h.succeedWhen(() -> h.assertTrue(te.getTorque() == 8192 && te.getOmega() == 7, "worm gear out "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm"));
	}

	/**
	 * Sport engine (1024 rad/s @ 256 Nm) into the high-ratio gear. Dry it gives nothing; with lubricant 256x the torque (4 rad/s @ 65536 Nm),
	 * using lubricant as it turns; in speed mode 262144 rad/s @ 1 Nm.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void highGearMultipliesByTwoFiftySix(GameTestHelper h) {
		PerformanceEngineBlockEntity e = place(h, RotaryBlocks.SPORT_ENGINE.get(), 1, 2, 4, 1, PerformanceEngineBlockEntity.class);
		e.addFuel(50000);
		e.addWater(50000);
		e.additives = 10000;
		AdvancedGearBlockEntity te = place(h, RotaryBlocks.HIGH_GEAR.get(), 2, 2, 4, 1, AdvancedGearBlockEntity.class);
		int[] phase = {0};
		h.succeedWhen(() -> {
			if (phase[0] == 0) {
				h.assertTrue(e.getOmega() == 1024, "engine at "+e.getOmega());
				h.assertTrue(te.getOmega() == 0 && te.getTorque() == 0, "dry gear turns");
				te.addLubricant(10000);
				phase[0] = 1;
				h.fail("lubricated");
			}
			if (phase[0] == 1) {
				h.assertTrue(te.getTorque() == 65536 && te.getOmega() == 4, "torque mode "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
				h.assertTrue(te.getLubricant() < 10000, "no lubricant used");
				te.torquemode = false;
				phase[0] = 2;
				h.fail("speed mode");
			}
			h.assertTrue(te.getOmega() == 262144 && te.getTorque() == 1, "speed mode "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
		});
	}
}
