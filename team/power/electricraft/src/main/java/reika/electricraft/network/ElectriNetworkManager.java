/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

import java.util.ArrayList;
import java.util.Collection;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import reika.electricraft.ElectriCraft;

/** Original: Reika.ElectriCraft.ElectriNetworkManager - ticks all networks at the start of the server tick and repaths at its end. */
@EventBusSubscriber(modid = ElectriCraft.MOD_ID)
public final class ElectriNetworkManager {

	private static final Collection<WireNetwork> networks = new ArrayList<>();
	private static final Collection<WireNetwork> discard = new ArrayList<>();
	private static final Collection<RFNetwork> rfNetworks = new ArrayList<>();
	private static final Collection<RFNetwork> rfDiscard = new ArrayList<>();

	private ElectriNetworkManager() {}

	@SubscribeEvent
	public static void tickStart(ServerTickEvent.Pre e) {
		if (!discard.isEmpty()) {
			networks.removeAll(discard);
			discard.clear();
		}
		for (WireNetwork net : new ArrayList<>(networks))
			net.tick();
		if (!rfDiscard.isEmpty()) {
			rfNetworks.removeAll(rfDiscard);
			rfDiscard.clear();
		}
		for (RFNetwork net : new ArrayList<>(rfNetworks))
			net.tick();
	}

	@SubscribeEvent
	public static void tickEnd(ServerTickEvent.Post e) {
		for (WireNetwork net : new ArrayList<>(networks))
			net.repath();
	}

	@SubscribeEvent
	public static void stopped(ServerStoppedEvent e) {
		networks.clear();
		discard.clear();
		rfNetworks.clear();
		rfDiscard.clear();
	}

	static void addNetwork(WireNetwork net) {
		networks.add(net);
	}

	static void addRFNetwork(RFNetwork net) {
		rfNetworks.add(net);
	}

	static void scheduleRFNetworkDiscard(RFNetwork net) {
		rfDiscard.add(net);
	}

	static void scheduleNetworkDiscard(WireNetwork net) {
		discard.add(net);
	}
}
