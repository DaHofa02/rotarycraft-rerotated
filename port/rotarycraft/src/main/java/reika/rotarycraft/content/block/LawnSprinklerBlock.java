/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.LAWNSPRINKLER in BlockMMachine - no orientation, no GUI; fed by a pipe below. */
public class LawnSprinklerBlock extends MachineBlock {

	private static final MapCodec<LawnSprinklerBlock> CODEC = simpleCodec(LawnSprinklerBlock::new);

	public LawnSprinklerBlock(Properties props) {
		super(props, RotaryBlockEntities.LAWN_SPRINKLER, Placement.NONE, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
