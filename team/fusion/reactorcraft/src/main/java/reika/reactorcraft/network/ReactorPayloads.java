/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.network;

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

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.reactor.ControlRodBlockEntity;
import reika.reactorcraft.reactor.CpuBlockEntity;

/** Original: ReactorPacketCore / ReactorPackets (CPUTOGGLE, CPURAISE, CPULOWER from GuiCPU). The server checks range and access. */
public final class ReactorPayloads {

	private ReactorPayloads() {}

	public enum CpuAction {
		RAISE, LOWER, TOGGLE
	}

	/** pos: the CPU; rod: for TOGGLE the rod position relative to the CPU. */
	public record CpuControl(BlockPos pos, int action, BlockPos rod) implements CustomPacketPayload {
		public static final Type<CpuControl> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "cpu_control"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CpuControl> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, CpuControl::pos, ByteBufCodecs.VAR_INT, CpuControl::action, BlockPos.STREAM_CODEC, CpuControl::rod, CpuControl::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register(RegisterPayloadHandlersEvent e) {
		PayloadRegistrar r = e.registrar("1");
		r.playToServer(CpuControl.TYPE, CpuControl.CODEC, ReactorPayloads::onCpuControl);
	}

	private static void onCpuControl(CpuControl msg, IPayloadContext ctx) {
		if (!(ctx.player() instanceof ServerPlayer sp) || !sp.level().isLoaded(msg.pos()))
			return;
		if (!(sp.level().getBlockEntity(msg.pos()) instanceof CpuBlockEntity te))
			return;
		//in reach, or the GUI was opened with the CPU remote control
		if (!te.isPlayerAccessible(sp) && !(sp.containerMenu instanceof reika.reactorcraft.content.menu.CpuMenu m && m.tile == te))
			return;
		if (msg.action() < 0 || msg.action() >= CpuAction.values().length)
			return;
		switch (CpuAction.values()[msg.action()]) {
			case RAISE -> te.raiseAllRods();
			case LOWER -> te.lowerAllRods();
			case TOGGLE -> {
				ControlRodBlockEntity rod = te.getControlRodAtRelativePosition(msg.rod().getX(), msg.rod().getY(), msg.rod().getZ());
				if (rod != null)
					rod.toggle(true, true);
			}
		}
	}
}
