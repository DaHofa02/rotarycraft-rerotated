/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the speed and torque typed into an open kinetic output GUI. */
public record SetOutputPayload(int speed, int torque) implements CustomPacketPayload {

	public static final Type<SetOutputPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RotaryME.MOD_ID, "set_output"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SetOutputPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SetOutputPayload::speed, ByteBufCodecs.VAR_INT, SetOutputPayload::torque, SetOutputPayload::new);

	@Override
	public Type<SetOutputPayload> type() {
		return TYPE;
	}

	public static void handle(SetOutputPayload p, IPayloadContext ctx) {
		if (ctx.player().containerMenu instanceof KineticOutputMenu m && m.stillValid(ctx.player()))
			m.tile.setOutput(p.speed(), p.torque());
	}
}
