/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.SPRINKLER in BlockMMachine - no orientation, no GUI; hangs under a pipe. */
public class SprinklerBlock extends MachineBlock {

	private static final MapCodec<SprinklerBlock> CODEC = simpleCodec(SprinklerBlock::new);

	public SprinklerBlock(Properties props) {
		super(props, RotaryBlockEntities.SPRINKLER, Placement.NONE, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
