/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import reika.electricraft.ElectriCraft;
import reika.electricraft.content.blockentity.TransformerBlockEntity;

/** Original: ElectriPacketCore / ElectriPackets. The server checks range and access as for the RotaryCraft GUIs. */
public final class ElectriPayloads {

	private ElectriPayloads() {}

	/** Original ElectriPackets.TRANSFORMER from GuiTransformer: the turns n1 and n2. */
	public record TransformerRatio(BlockPos pos, int n1, int n2) implements CustomPacketPayload {
		public static final Type<TransformerRatio> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "transformer_ratio"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TransformerRatio> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, TransformerRatio::pos, ByteBufCodecs.VAR_INT, TransformerRatio::n1, ByteBufCodecs.VAR_INT, TransformerRatio::n2, TransformerRatio::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Original ElectriPackets.RFCABLE from GuiRFCable: the network limit. */
	public record RFCableLimit(BlockPos pos, int limit) implements CustomPacketPayload {
		public static final Type<RFCableLimit> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "rf_cable_limit"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RFCableLimit> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, RFCableLimit::pos, ByteBufCodecs.VAR_INT, RFCableLimit::limit, RFCableLimit::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register(RegisterPayloadHandlersEvent e) {
		PayloadRegistrar r = e.registrar("1");
		r.playToServer(TransformerRatio.TYPE, TransformerRatio.CODEC, ElectriPayloads::onTransformerRatio);
		r.playToServer(RFCableLimit.TYPE, RFCableLimit.CODEC, ElectriPayloads::onRFCableLimit);
	}

	private static void onTransformerRatio(TransformerRatio msg, IPayloadContext ctx) {
		if (!(ctx.player() instanceof ServerPlayer sp) || !sp.level().isLoaded(msg.pos()))
			return;
		//the GUI's text fields take at most 4 digits
		if (sp.level().getBlockEntity(msg.pos()) instanceof TransformerBlockEntity te && te.isPlayerAccessible(sp) && msg.n1() <= 9999 && msg.n2() <= 9999)
			te.setRatio(msg.n1(), msg.n2());
	}

	private static void onRFCableLimit(RFCableLimit msg, IPayloadContext ctx) {
		if (!(ctx.player() instanceof ServerPlayer sp) || !sp.level().isLoaded(msg.pos()))
			return;
		if (sp.level().getBlockEntity(msg.pos()) instanceof reika.electricraft.content.blockentity.RFCableBlockEntity te && te.isPlayerAccessible(sp) && msg.limit() >= 0)
			te.setRFLimit(msg.limit());
	}
}
