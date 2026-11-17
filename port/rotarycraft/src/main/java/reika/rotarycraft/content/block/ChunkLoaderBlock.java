/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.CHUNKLOADER in BlockMMachine - no orientation, no GUI. */
public class ChunkLoaderBlock extends MachineBlock {

	public ChunkLoaderBlock(Properties props) {
		super(props, RotaryBlockEntities.CHUNK_LOADER, Placement.NONE, false);
	}
}

