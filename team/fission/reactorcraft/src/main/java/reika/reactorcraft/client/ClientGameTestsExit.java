/*******************************************************************************
 * Test tool of the private NeoForge 1.21.1 ports (not part of the original mods): closes the client once dev/ClientGameTests has run
 * its batch and written the summary.
 ******************************************************************************/
package reika.reactorcraft.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.dev.ClientGameTests;

@EventBusSubscriber(modid = ReactorCraft.MOD_ID, value = Dist.CLIENT)
public final class ClientGameTestsExit {

	private static int delay = 40;

	private ClientGameTestsExit() {}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post e) {
		if (ClientGameTests.finished && --delay == 0)
			Minecraft.getInstance().stop();
	}
}
