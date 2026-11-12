/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.SPILLWAY in BlockDMMachine - four directions (the side it takes water from); no GUI. */
public class SpillwayBlock extends MachineBlock {

	private static final MapCodec<SpillwayBlock> CODEC = simpleCodec(SpillwayBlock::new);

	public SpillwayBlock(Properties props) {
		super(props, RotaryBlockEntities.SPILLWAY, Placement.FOUR_SIDED, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
