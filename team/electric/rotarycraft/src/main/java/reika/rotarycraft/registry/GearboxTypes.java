/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

import reika.rotarycraft.lib.math.ReikaMathLibrary;

/** Original: Reika.RotaryCraft.Registry.GearboxTypes. The Botania-only LIVINGWOOD and LIVINGROCK types are not ported (docs/06 §6). */
public enum GearboxTypes implements StringRepresentable {

	WOOD(1, MaterialRegistry.WOOD),
	STONE(2, MaterialRegistry.STONE),
	STEEL(0, MaterialRegistry.STEEL),
	TUNGSTEN(7, MaterialRegistry.TUNGSTEN),
	DIAMOND(3, MaterialRegistry.DIAMOND),
	BEDROCK(4, MaterialRegistry.BEDROCK);

	/** Row of this material's parts in the original gear crafting sprite sheet. */
	public final int metaOffset;
	public final MaterialRegistry material;

	public static final GearboxTypes[] typeList = values();

	GearboxTypes(int row, MaterialRegistry mat) {
		metaOffset = row;
		material = mat;
	}

	@Override
	public String getSerializedName() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public boolean needsLubricant() {
		return this != WOOD && this.isDamageableGear();
	}

	public boolean consumesLubricant() {
		return this.needsLubricant() && this != DIAMOND;
	}

	public boolean acceptsDiamondUpgrade() {
		return this.consumesLubricant() && this != STONE;
	}

	public float getLubricantConsumeRate(int omegain) {
		switch(this) {
			case STONE:
				return Math.max(1, 1+omegain/8192F);
			case TUNGSTEN:
				return Math.min(1, 0.5F+Math.max(0, 0.03125F*(ReikaMathLibrary.logbase2(omegain)-2)));
			default:
				return 1;
		}
	}

	public boolean generatesHeat(int omega, int Tamb) {
		switch(this) {
			case WOOD:
				return true;
			case STONE:
				return omega >= 8192;
			default:
				return false;
		}
	}

	public boolean takesTemperatureDamage() {
		return this == WOOD || this == STONE;
	}

	public boolean isDamageableGear() {
		return !material.isInfiniteStrength();
	}

	/** Base name of the gearbox model texture (original: getBaseGearboxTexture, without ".png"). */
	public String getBaseGearboxTexture() {
		return switch(this) {
			case BEDROCK -> "geartexb";
			case DIAMOND -> "geartexd";
			case STONE -> "geartexs";
			case TUNGSTEN -> "geartext";
			case WOOD -> "geartexw";
			default -> "geartex";
		};
	}

	public int getMaxLubricant() {
		switch(this) {
			case DIAMOND:
				return 1000;
			case STEEL:
			case TUNGSTEN:
				return 24000;
			case STONE:
				return 8000;
			default:
				return 0;
		}
	}

	public boolean acceptsBearingUpgrade(GearboxTypes mat) {
		switch(this) {
			case BEDROCK:
			case WOOD:
				return false;
			default:
				return mat != WOOD && mat.material.ordinal() <= material.ordinal()+2;
		}
	}

	public double getOmegaForRotFailure(int omega, int omegain) {
		return ReikaMathLibrary.doubpow(Math.max(omega, omegain), material.getSpeedForceExponent());
	}

	public static GearboxTypes getFromMaterial(MaterialRegistry mat) {
		return switch(mat) {
			case BEDROCK -> BEDROCK;
			case DIAMOND -> DIAMOND;
			case STEEL -> STEEL;
			case STONE -> STONE;
			case WOOD -> WOOD;
			case TUNGSTEN -> TUNGSTEN;
		};
	}

	/** Original: GearboxTypes.GearPart. Crafting parts made of each gear material. */
	public enum GearPart {
		SHAFT,
		GEAR,
		UNIT2,
		UNIT4,
		UNIT8,
		UNIT16,
		BEARING,
		SHAFTCORE;

		public static final GearPart[] list = values();

		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}

		public static GearPart getGearUnitPartItemFromRatio(int r) {
			return switch(r) {
				case 2 -> UNIT2;
				case 4 -> UNIT4;
				case 8 -> UNIT8;
				case 16 -> UNIT16;
				default -> throw new IllegalArgumentException("Invalid gear ratio!");
			};
		}
	}
}
