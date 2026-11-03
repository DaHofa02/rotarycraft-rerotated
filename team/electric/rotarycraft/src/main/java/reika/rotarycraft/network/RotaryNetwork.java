/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.network;

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

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.BlastFurnaceBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.SplitterBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;

/**
 * Original: Reika.RotaryCraft.PacketHandlerCore / Registry.PacketRegistry. Only the packets of the ported GUIs exist so far
 * (SPLITTERMODE, BEVEL, AFTERBURN, BLASTLEAVEONE); the server checks that the player can use the machine before applying them.
 */
public final class RotaryNetwork {

	private RotaryNetwork() {}

	public record SplitterMode(BlockPos pos, int mode) implements CustomPacketPayload {
		public static final Type<SplitterMode> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "splitter_mode"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SplitterMode> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, SplitterMode::pos, ByteBufCodecs.VAR_INT, SplitterMode::mode, SplitterMode::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record BevelDirection(BlockPos pos, int direction) implements CustomPacketPayload {
		public static final Type<BevelDirection> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "bevel_direction"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BevelDirection> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, BevelDirection::pos, ByteBufCodecs.VAR_INT, BevelDirection::direction, BevelDirection::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Original: PacketRegistry.AFTERBURN from GuiJet. */
	public record Afterburner(BlockPos pos, boolean active) implements CustomPacketPayload {
		public static final Type<Afterburner> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "afterburner"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Afterburner> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Afterburner::pos, ByteBufCodecs.BOOL, Afterburner::active, Afterburner::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Original: PacketRegistry.BLASTLEAVEONE from GuiBlastFurnace. */
	/** Original PacketRegistry.WINDERTOGGLE: switches the coil winder between winding and unwinding. */
	public record WinderToggle(BlockPos pos) implements CustomPacketPayload {
		public static final Type<WinderToggle> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "winder_toggle"));
		public static final StreamCodec<RegistryFriendlyByteBuf, WinderToggle> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, WinderToggle::pos, WinderToggle::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record BlastLeaveOne(BlockPos pos, boolean leave) implements CustomPacketPayload {
		public static final Type<BlastLeaveOne> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "blast_leave_one"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BlastLeaveOne> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, BlastLeaveOne::pos, ByteBufCodecs.BOOL, BlastLeaveOne::leave, BlastLeaveOne::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Split ratios offered by the original GUI. */
	private static final int[] MODES = {32, 16, 8, 4, 1, -4, -8, -16, -32};

	public static void register(RegisterPayloadHandlersEvent e) {
		PayloadRegistrar r = e.registrar("1");
		r.playToServer(SplitterMode.TYPE, SplitterMode.CODEC, RotaryNetwork::onSplitterMode);
		r.playToServer(BevelDirection.TYPE, BevelDirection.CODEC, RotaryNetwork::onBevelDirection);
		r.playToServer(Afterburner.TYPE, Afterburner.CODEC, RotaryNetwork::onAfterburner);
		r.playToServer(BlastLeaveOne.TYPE, BlastLeaveOne.CODEC, RotaryNetwork::onBlastLeaveOne);
		r.playToServer(WinderToggle.TYPE, WinderToggle.CODEC, RotaryNetwork::onWinderToggle);
	}

	private static <T extends RotaryCraftBlockEntity> T machine(IPayloadContext ctx, BlockPos pos, Class<T> cls) {
		if (!(ctx.player() instanceof ServerPlayer sp) || !sp.level().isLoaded(pos))
			return null;
		return sp.level().getBlockEntity(pos) instanceof RotaryCraftBlockEntity te && cls.isInstance(te) && te.isPlayerAccessible(sp) ? cls.cast(te) : null;
	}

	private static void onSplitterMode(SplitterMode msg, IPayloadContext ctx) {
		SplitterBlockEntity spl = machine(ctx, msg.pos(), SplitterBlockEntity.class);
		if (spl != null && java.util.Arrays.stream(MODES).anyMatch(m -> m == msg.mode()))
			spl.setMode(msg.mode());
	}

	private static void onBevelDirection(BevelDirection msg, IPayloadContext ctx) {
		BevelGearBlockEntity bev = machine(ctx, msg.pos(), BevelGearBlockEntity.class);
		if (bev != null && msg.direction() >= 0 && msg.direction() < BevelGearBlockEntity.getDirectionMap().size())
			bev.setDirection(msg.direction());
	}

	private static void onAfterburner(Afterburner msg, IPayloadContext ctx) {
		JetEngineBlockEntity jet = machine(ctx, msg.pos(), JetEngineBlockEntity.class);
		if (jet != null)
			jet.setBurnerActive(msg.active());
	}

	private static void onWinderToggle(WinderToggle msg, IPayloadContext ctx) {
		reika.rotarycraft.content.blockentity.WinderBlockEntity te = machine(ctx, msg.pos(), reika.rotarycraft.content.blockentity.WinderBlockEntity.class);
		if (te != null)
			te.toggleMode();
	}

	private static void onBlastLeaveOne(BlastLeaveOne msg, IPayloadContext ctx) {
		BlastFurnaceBlockEntity te = machine(ctx, msg.pos(), BlastFurnaceBlockEntity.class);
		if (te != null)
			te.setLeaveLastItem(msg.leave());
	}
}
