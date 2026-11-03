/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

import reika.rotarycraft.lib.math.ReikaEngLibrary;

/** Original: Reika.RotaryCraft.Registry.Flywheels. Values unchanged. */
public enum Flywheels implements StringRepresentable {

	WOOD(16, 2, ReikaEngLibrary.rhowood, ReikaEngLibrary.Twood),
	STONE(128, 5, ReikaEngLibrary.rhorock, 0.9*ReikaEngLibrary.Tstone),
	IRON(512, 15, ReikaEngLibrary.rhoiron, 5*ReikaEngLibrary.Tiron),
	GOLD(4096, 40, ReikaEngLibrary.rhogold, ReikaEngLibrary.Tgold),
	TUNGSTEN(8192, 25, MaterialRegistry.tungstenDensity(), 8*ReikaEngLibrary.Ttungsten),
	DEPLETEDU(4096, 40, ReikaEngLibrary.rhouranium, 6*ReikaEngLibrary.Turanium),
	BEDROCK(Integer.MAX_VALUE, 200, 1.75*ReikaEngLibrary.rhoiron, Double.POSITIVE_INFINITY);

	/** Original: TileEntityFlywheel.MINTORQUERATIO */
	public static final int MINTORQUERATIO = 4;

	public final int maxTorque;
	public final int maxSpeed;
	public final int decayTime;
	public final double density;
	public final double tensileStrength;

	public static final Flywheels[] list = values();

	Flywheels(int t, int dec, double rho, double str) {
		maxTorque = t;
		tensileStrength = str;
		decayTime = dec;
		density = rho;
		maxSpeed = this.getLimitLoad();
	}

	private int getLimitLoad() {
		double r = 0.75;
		double s = 100*tensileStrength;
		double frac = 2*s/(density*r*r);
		double base = Math.sqrt(frac);
		return (int)base;
	}

	public int getMinTorque() {
		return this == BEDROCK ? 16384 : maxTorque/MINTORQUERATIO;
	}

	@Override
	public String getSerializedName() {
		return this.name().toLowerCase(Locale.ROOT);
	}
}
