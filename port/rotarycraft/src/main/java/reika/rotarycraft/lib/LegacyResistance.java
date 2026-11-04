/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft/ReactorCraft/ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.lib;

/**
 * Blast resistance of a 1.7.10 block in 1.21 terms. 1.7.10 {@code Block.setResistance(x)} stored {@code 3x}, and explosions used a fifth
 * of that ({@code Block.getExplosionResistance(Entity)} = blockResistance / 5); 1.21 uses its explosion resistance directly in the same
 * formula ({@code (resistance + 0.3) * 0.3} per step). So {@code setResistance(x)} becomes {@code 0.6x}. Check: the iron block had
 * setResistance(10) in 1.7.10, i.e. 6, and has explosion resistance 6 in 1.21. Values that an original block returned from its own
 * getExplosionResistance override (blast glass, bedrock ingot block: 6000) are used as they are, not through this.
 */
public final class LegacyResistance {

	private LegacyResistance() {}

	/** @param setResistance the argument of the original {@code setResistance} (obfuscated {@code func_149752_b}) */
	public static float of(float setResistance) {
		return setResistance*3F/5F;
	}
}
