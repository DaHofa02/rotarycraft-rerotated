/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import javax.annotation.Nullable;

import reika.reactorcraft.content.entity.NeutronEntity.NeutronType;

/** Original: Reika.ReactorCraft.Registry.ReactorType. */
public enum ReactorType {
	FISSION, BREEDER, HTGR, THORIUM, FUSION, SOLAR, NONE;

	public double getHPTurbineMultiplier() {
		return switch (this) {
			case FUSION -> 1.5F;
			case HTGR -> 0.35F;
			case SOLAR -> 0.15F;
			case NONE -> 0;
			default -> 1;
		};
	}

	public float getControlCPUHeatEfficiency() {
		return switch (this) {
			case HTGR, SOLAR, FUSION -> this.getTypeMismatchHeatEfficiency();
			default -> 1;
		};
	}

	/** For conducting heat to other reactor types. */
	public float getTypeMismatchHeatEfficiency() {
		return switch (this) {
			case FISSION -> 1;
			case HTGR -> 0.0625F;
			case SOLAR, FUSION, NONE -> 0;
			case THORIUM -> 0.25F;
			default -> 0.5F;
		};
	}

	@Nullable
	public NeutronType getNeutronType() {
		return switch (this) {
			case BREEDER -> NeutronType.BREEDER;
			case FISSION -> NeutronType.FISSION;
			case FUSION -> NeutronType.FUSION;
			case THORIUM -> NeutronType.THORIUM;
			default -> null;
		};
	}
}
