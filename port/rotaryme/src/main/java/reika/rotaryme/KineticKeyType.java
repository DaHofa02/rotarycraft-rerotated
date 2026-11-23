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
 * The key type of kinetic energy. Amounts are joules; one bit of cell space holds 1024 J, so a byte (what AE2 counts cells in) holds 8192 J.
 */
public final class KineticKeyType extends AEKeyType {

	public static final KineticKeyType INSTANCE = new KineticKeyType();
	/** Default joules per byte (1024 J a bit); the real value comes from the config, {@link #joulesPerByte()}. */
	public static final int JOULES_PER_BYTE = 8*1024;

	public static int joulesPerByte() {
		return 8*MEConfig.JOULES_PER_BIT.get();
	}

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
		return joulesPerByte();
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
