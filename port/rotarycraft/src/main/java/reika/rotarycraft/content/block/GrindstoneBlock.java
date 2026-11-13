/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.GRINDSTONE in BlockDMIMachine - two axle directions (0 north/south, 1 east/west); the tool goes in through the one-slot GUI. */
public class GrindstoneBlock extends MachineBlock {

	private static final MapCodec<GrindstoneBlock> CODEC = simpleCodec(GrindstoneBlock::new);

	public GrindstoneBlock(Properties props) {
		super(props, RotaryBlockEntities.GRINDSTONE, Placement.TWO_SIDED, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
