/*******************************************************************************
 * Tests for the port's air compressor and pneumatic engine (optional PneumaticCraft integration; the test runs load PneumaticCraft).
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.compat.PneumaticCompat;
import reika.rotarycraft.content.blockentity.AirCompressorBlockEntity;
import reika.rotarycraft.content.blockentity.PneumaticEngineBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PneumaticTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine -> air compressor (from the west) -> PneumaticCraft pressure tube: power/520 mL a tick go in and the tube's pressure rises. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void compressorPressurizesATube(GameTestHelper h) {
		h.assertTrue(reika.rotarycraft.compat.OptionalMods.pneumaticCraft(), "PneumaticCraft not loaded");
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		AirCompressorBlockEntity c = place(h, RotaryBlocks.AIR_COMPRESSOR.get(), 2, 2, 4, 1, AirCompressorBlockEntity.class);
		h.setBlock(new BlockPos(3, 2, 4), BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("pneumaticcraft", "pressure_tube")));
		h.succeedWhen(() -> {
			h.assertTrue(c.hasTarget && c.getAirPerTick() == c.getPower()/520 && c.getAirPerTick() > 0, "compressor "+c.getPower()+" W, "+c.getAirPerTick()+" mL/t, target "+c.hasTarget);
			h.assertTrue(c.pumped > 2000, "pumped "+c.pumped+" mL");
		});
	}

	/** Gas engine -> air compressor -> pneumatic engine (air from the west, set to 256 rad/s): it runs on the air at 8 Nm. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void compressedAirDrivesThePneumaticEngine(GameTestHelper h) {
		h.assertTrue(reika.rotarycraft.compat.OptionalMods.pneumaticCraft(), "PneumaticCraft not loaded");
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		place(h, RotaryBlocks.AIR_COMPRESSOR.get(), 2, 2, 4, 1, AirCompressorBlockEntity.class);
		PneumaticEngineBlockEntity e = place(h, RotaryBlocks.PNEUMATIC_ENGINE.get(), 3, 2, 4, 1, PneumaticEngineBlockEntity.class);
		e.setSpeedStep(8);
		h.succeedWhen(() -> h.assertTrue(e.getOmega() == 256 && e.getTorque() == 8, "engine "+e.getOmega()+" rad/s "+e.getTorque()+" Nm, air "+e.getAir()+" mL, uses "+e.getConsumption()));
	}
}
