/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.BEAMMIRROR in BlockDMMachine - four directions (the side it shines to); no GUI. */
public class BeamMirrorBlock extends MachineBlock {

	private static final MapCodec<BeamMirrorBlock> CODEC = simpleCodec(BeamMirrorBlock::new);

	public BeamMirrorBlock(Properties props) {
		super(props, RotaryBlockEntities.BEAM_MIRROR, Placement.FOUR_SIDED, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
