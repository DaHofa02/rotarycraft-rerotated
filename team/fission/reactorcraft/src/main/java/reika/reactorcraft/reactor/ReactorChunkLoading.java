/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import reika.reactorcraft.ReactorCraft;

/** Original: ChunkLoadingTile + DragonAPI ChunkManager - an active fission core keeps the 3x3 chunks around it loaded. */
public final class ReactorChunkLoading {

	public static final TicketController CONTROLLER = new TicketController(ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "fission_core"),
			(level, helper) -> {
				//tickets of cores that no longer exist are dropped; active cores ask again on their next neutron
				helper.getBlockTickets().keySet().forEach(pos -> helper.removeAllTickets(pos));
			});

	private ReactorChunkLoading() {}

	public static void register(RegisterTicketControllersEvent e) {
		e.register(CONTROLLER);
	}

	public static void setLoaded(ServerLevel world, BlockPos pos, boolean load) {
		setLoaded(world, pos, load, 1);
	}

	/** Original ChunkManager.getChunkSquare: the (2r+1)x(2r+1) chunks around the block (toroid magnets use r = 2). */
	public static void setLoaded(ServerLevel world, BlockPos pos, boolean load, int r) {
		int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
		for (int dx = -r; dx <= r; dx++)
			for (int dz = -r; dz <= r; dz++)
				CONTROLLER.forceChunk(world, pos, cx+dx, cz+dz, load, true);
	}
}
