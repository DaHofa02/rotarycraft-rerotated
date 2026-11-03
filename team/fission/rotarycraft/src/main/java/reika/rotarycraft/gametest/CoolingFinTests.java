/*******************************************************************************
 * Tests for the ported RotaryCraft cooling fin (original TileEntityCoolingFin). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.CoolingFinBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CoolingFinTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static GearboxBlockEntity gearbox(GameTestHelper h, BlockPos p, int temp) {
		h.setBlock(p, RotaryBlocks.GEARBOX.get());
		GearboxBlockEntity g = (GearboxBlockEntity)h.getBlockEntity(p);
		g.setTemperature(temp);
		return g;
	}

	/** Two equally hot idle gearboxes: the one with a cooling fin on top (fin metadata 0 = target below) cools faster. */
	@GameTest(template = PLATFORM, timeoutTicks = 500)
	public static void finCoolsGearbox(GameTestHelper h) {
		GearboxBlockEntity a = gearbox(h, new BlockPos(2, 1, 4), 500);
		GearboxBlockEntity b = gearbox(h, new BlockPos(6, 1, 4), 500);
		h.setBlock(new BlockPos(2, 2, 4), RotaryBlocks.COOLING_FIN.get());
		CoolingFinBlockEntity fin = (CoolingFinBlockEntity)h.getBlockEntity(new BlockPos(2, 2, 4));
		fin.setBlockMetadata(0);
		h.runAtTickTime(450, () -> {
			h.assertTrue(fin.getTarget().equals(h.absolutePos(new BlockPos(2, 1, 4))), "fin target "+fin.getTarget());
			h.assertTrue(a.getTemperature() < b.getTemperature(), "with fin "+a.getTemperature()+" C, without "+b.getTemperature()+" C");
			h.succeed();
		});
	}

	/** Original getTargetSide: metadata 0..5 -> below, above, north, west, south, east. */
	@GameTest(template = PLATFORM)
	public static void finTargetSides(GameTestHelper h) {
		Direction[] exp = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST};
		for (int i = 0; i < 6; i++)
			h.assertTrue(CoolingFinBlockEntity.getTargetDirection(i) == exp[i], "meta "+i);
		h.succeed();
	}
}
