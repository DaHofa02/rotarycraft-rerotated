/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;

/**
 * The key type of kinetic energy. Amounts are joules; one bit of cell space holds 16 J, so a byte (what AE2 counts cells in) holds 128 J.
 */
public final class KineticKeyType extends AEKeyType {

	public static final KineticKeyType INSTANCE = new KineticKeyType();
	public static final int JOULES_PER_BYTE = 8*16;

	private KineticKeyType() {
		super(KineticKey.ID, KineticKey.class, Component.translatable("key.rotaryme.kinetic"));
	}

	@Override
	public MapCodec<? extends AEKey> codec() {
		return MapCodec.unit(KineticKey.INSTANCE);
	}

	@Override
	public AEKey readFromPacket(RegistryFriendlyByteBuf buf) {
		return KineticKey.INSTANCE;
	}

	@Override
	public int getAmountPerByte() {
		return JOULES_PER_BYTE;
	}

	/** Terminals move energy in kilojoule steps. */
	@Override
	public int getAmountPerOperation() {
		return 1000;
	}

	@Override
	public int getAmountPerUnit() {
		return 1;
	}

	@Override
	public String getUnitSymbol() {
		return "J";
	}
}
