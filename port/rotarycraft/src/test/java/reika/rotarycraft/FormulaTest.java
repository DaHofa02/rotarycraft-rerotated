/*******************************************************************************
 * Unit tests for formulas ported unchanged from RotaryCraft/DragonAPI (by Reika Kalseki). Private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import reika.rotarycraft.content.item.PowerFormat;
import reika.rotarycraft.lib.math.ReikaEngLibrary;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.DifficultyEffects;
import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.Flywheels;
import reika.rotarycraft.registry.MaterialRegistry;

class FormulaTest {

	@Test
	void engineTableMatchesOriginal() {
		// Original EngineType: (speed, torque)
		int[][] expected = {{256, 4}, {1024, 8}, {512, 32}, {512, 128}, {256, 512}, {1024, 256}, {32, 16384}, {131072, 16}, {65536, 1024}};
		for (EngineType e : EngineType.values()) {
			assertEquals(expected[e.ordinal()][0], e.getSpeed(), e+" speed");
			assertEquals(expected[e.ordinal()][1], e.getTorque(), e+" torque");
			assertEquals((long)e.getSpeed()*e.getTorque(), e.getPower());
		}
	}

	@Test
	void shaftTorqueLimitMatchesTwistFailure() {
		// MaterialRegistry.getMaxShaftTorque and ReikaEngLibrary.mat_twistfailure (as used by TileEntityShaft) must agree.
		for (MaterialRegistry m : MaterialRegistry.values()) {
			if (m.isInfiniteStrength())
				continue;
			double max = m.getMaxShaftTorque();
			assertFalse(ReikaEngLibrary.mat_twistfailure((int)Math.floor(max*0.99), 0.0625, m.shear/16D), m+" fails below its limit");
			assertTrue(ReikaEngLibrary.mat_twistfailure((int)Math.ceil(max*1.01)+1, 0.0625, m.shear/16D), m+" survives above its limit");
		}
	}

	@Test
	void woodShaftLimit() {
		// 0.5*pi*r^3*tau/16 with r = 0.0625 m and tau = 11.6 MPa
		assertEquals(0.5*Math.PI*Math.pow(0.0625, 3)*11.6e6/16, MaterialRegistry.WOOD.getMaxShaftTorque(), 1e-9);
		assertEquals(278.0, MaterialRegistry.WOOD.getMaxShaftTorque(), 0.5);
	}

	@Test
	void flywheelSpeedLimits() {
		// Flywheels.getLimitLoad: sqrt(2*100*sigma/(rho*0.75^2))
		assertEquals((int)Math.sqrt(2*100*20e6/(800*0.5625)), Flywheels.WOOD.maxSpeed);
		assertEquals(Integer.MAX_VALUE, Flywheels.BEDROCK.maxSpeed);
		assertEquals(4, Flywheels.WOOD.getMinTorque());
		assertEquals(16384, Flywheels.BEDROCK.getMinTorque());
	}

	@Test
	void mathHelpers() {
		assertEquals(8, ReikaMathLibrary.logbase2(256));
		assertEquals(0, ReikaMathLibrary.logbase2(0));
		assertEquals(3.0, ReikaMathLibrary.logbase(8, 2), 1e-12);
		assertEquals(16.0, ReikaMathLibrary.intpow(2, 4), 1e-12);
		assertEquals(0.25, ReikaMathLibrary.intpow(2, -2), 1e-12);
		assertEquals(1.5, ReikaMathLibrary.getThousandBase(1500), 1e-12);
		assertEquals("k", ReikaEngLibrary.getSIPrefix(1500));
		assertEquals("M", ReikaEngLibrary.getSIPrefix(2.5e6));
	}

	@Test
	void powerFormat() {
		assertEquals("1.024kW", PowerFormat.formatPower(1024));
		assertEquals("256 rad/s", PowerFormat.formatSpeed(256));
		assertEquals("4 Nm", PowerFormat.formatTorque(4));
	}

	@Test
	void difficultyDefaultsToMedium() {
		// Without a loaded config the original default (2 = medium) applies.
		assertEquals(DifficultyEffects.Difficulty.MEDIUM, DifficultyEffects.getDifficulty());
		assertEquals(3, DifficultyEffects.PARTCRAFT.getInt());
		assertEquals(1F, DifficultyEffects.LUBEUSAGE.getChance());
	}
}
