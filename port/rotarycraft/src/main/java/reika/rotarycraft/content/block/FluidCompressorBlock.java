/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.GASTANK in BlockMMachine - no orientation, no GUI. */
public class FluidCompressorBlock extends MachineBlock {

	private static final MapCodec<FluidCompressorBlock> CODEC = simpleCodec(FluidCompressorBlock::new);

	public FluidCompressorBlock(Properties props) {
		super(props, RotaryBlockEntities.FLUID_COMPRESSOR, Placement.NONE, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
