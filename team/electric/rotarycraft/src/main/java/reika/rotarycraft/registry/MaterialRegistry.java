/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

import reika.rotarycraft.lib.math.ReikaEngLibrary;

/** Original: Reika.RotaryCraft.Registry.MaterialRegistry. Shaft and gear materials with their mechanical limits; values unchanged. */
public enum MaterialRegistry implements StringRepresentable {

	WOOD(	-1, ReikaEngLibrary.Ewood, 		ReikaEngLibrary.Gwood, 		ReikaEngLibrary.Twood, 		ReikaEngLibrary.Swood, 		ReikaEngLibrary.rhowood),
	STONE(	0, ReikaEngLibrary.Estone, 		ReikaEngLibrary.Gstone, 	ReikaEngLibrary.Tstone, 	ReikaEngLibrary.Sstone, 	ReikaEngLibrary.rhorock),
	STEEL(	1, ReikaEngLibrary.Esteel, 		ReikaEngLibrary.Gsteel, 	ReikaEngLibrary.Tsteel, 	ReikaEngLibrary.Ssteel, 	ReikaEngLibrary.rhoiron),
	TUNGSTEN(1, ReikaEngLibrary.Etungsten, 	ReikaEngLibrary.Gtungsten, 	ReikaEngLibrary.Ttungsten, 	ReikaEngLibrary.Stungsten, 	MaterialRegistry.tungstenDensity()),
	DIAMOND(2, ReikaEngLibrary.Ediamond, 	ReikaEngLibrary.Gdiamond, 	ReikaEngLibrary.Tdiamond, 	ReikaEngLibrary.Sdiamond, 	ReikaEngLibrary.rhodiamond),
	BEDROCK(3, Double.POSITIVE_INFINITY, 	Double.POSITIVE_INFINITY, 	Double.POSITIVE_INFINITY, 	Double.POSITIVE_INFINITY, 	ReikaEngLibrary.rhoiron);

	public final double Emod;
	public final double Smod;
	public final double tensile;
	public final double shear;
	public final double rho;

	public final int harvestLevel;

	public static final MaterialRegistry[] matList = values();

	MaterialRegistry(int h, double E, double G, double TS, double S, double den) {
		Emod = E;
		Smod = G;
		tensile = TS;
		shear = S;
		rho = den;
		harvestLevel = h;
	}

	/** Original: RotaryAux.tungstenDensity */
	public static double tungstenDensity() {
		return ReikaEngLibrary.rhoiron*0.8+0.2*ReikaEngLibrary.rhotungsten;
	}

	@Override
	public String getSerializedName() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public boolean isInfiniteStrength() {
		return this == BEDROCK;
	}

	public boolean isFlammable() {
		return this == WOOD;
	}

	public double getSpeedForceExponent() {
		switch(this) {
			case WOOD:
			case STONE:
			case STEEL:
				return 1-(0.11D*this.ordinal());
			case TUNGSTEN:
				return 0.70;
			case DIAMOND:
				return 0.67;
			default:
				return 1;
		}
	}

	public double getMaxShaftTorque() {
		if (this.isInfiniteStrength())
			return Double.POSITIVE_INFINITY;
		double r = 0.0625;
		double tau = shear;
		return 0.5*Math.PI*r*r*r*tau/16D;
	}

	public double getMaxShaftSpeed() {
		if (this.isInfiniteStrength())
			return Double.POSITIVE_INFINITY;
		double f = 1D/this.getSpeedForceExponent();
		double r = 0.0625;
		double base = Math.sqrt(2*tensile/(rho*r*r));
		return Math.pow(base, f);
	}

	/** Base name of the shaft model texture (original: getBaseShaftTexture, without ".png"). */
	public String getBaseShaftTexture() {
		return switch(this) {
			case BEDROCK -> "shafttexb";
			case DIAMOND -> "shafttexd";
			case STONE -> "shafttexs";
			case TUNGSTEN -> "shafttext";
			case WOOD -> "shafttexw";
			default -> "shafttex";
		};
	}
}
