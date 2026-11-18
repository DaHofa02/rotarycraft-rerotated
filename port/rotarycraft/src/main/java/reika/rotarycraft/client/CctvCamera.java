/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.CctvBlockEntity;
import reika.rotarycraft.network.RotaryNetwork;

/**
 * Client side of the CCTV: a camera entity (never added to the world) at the CCTV, looking where it is aimed, becomes the view entity;
 * sneaking, the camera block disappearing or the server's "off" message give the view back to the player.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, value = Dist.CLIENT)
public final class CctvCamera {

	private static Entity camera;
	private static net.minecraft.core.BlockPos at;
	private static int grace;

	private CctvCamera() {}

	public static void handle(RotaryNetwork.CctvView msg) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null)
			return;
		if (!msg.on()) {
			if (msg.pos().equals(at))
				release();
			return;
		}
		Entity e = EntityType.ARMOR_STAND.create(mc.level);
		if (e == null)
			return;
		e.moveTo(msg.pos().getX()+0.5, msg.pos().getY()+0.5-e.getEyeHeight(), msg.pos().getZ()+0.5, msg.yaw(), msg.pitch());
		e.setInvisible(true);
		camera = e;
		at = msg.pos();
		grace = 10; //the sneak that called it up must not end it at once
		mc.setCameraEntity(e);
	}

	public static boolean isViewing() {
		return camera != null;
	}

	private static void release() {
		Minecraft mc = Minecraft.getInstance();
		camera = null;
		at = null;
		if (mc.player != null)
			mc.setCameraEntity(mc.player);
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post e) {
		if (camera == null)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (grace > 0) {
			grace--;
			return;
		}
		if (mc.player == null || mc.level == null || mc.options.keyShift.isDown() || !(mc.level.getBlockEntity(at) instanceof CctvBlockEntity))
			release();
	}
}
