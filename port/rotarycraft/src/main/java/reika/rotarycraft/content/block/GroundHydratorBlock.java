/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.HYDRATOR in BlockMMachine - an open wooden box (the reservoir model); water comes in through pipes. */
public class GroundHydratorBlock extends MachineBlock {

	private static final MapCodec<GroundHydratorBlock> CODEC = simpleCodec(GroundHydratorBlock::new);

	public GroundHydratorBlock(Properties props) {
		super(props, RotaryBlockEntities.HYDRATOR, Placement.NONE, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
