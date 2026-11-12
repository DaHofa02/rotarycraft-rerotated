/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.MAGNETIZER in BlockDMIMachine - four directions (the input side); the core goes in through the one-slot GUI. */
public class MagnetizerBlock extends MachineBlock {

	private static final MapCodec<MagnetizerBlock> CODEC = simpleCodec(MagnetizerBlock::new);

	public MagnetizerBlock(Properties props) {
		super(props, RotaryBlockEntities.MAGNETIZER, Placement.FOUR_SIDED, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
