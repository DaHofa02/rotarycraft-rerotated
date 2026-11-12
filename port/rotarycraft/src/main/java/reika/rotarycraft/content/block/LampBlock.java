/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.LAMP in BlockMachine - a full block (icon "lamp"); the coil goes in through the one-slot GUI. */
public class LampBlock extends MachineBlock {

	private static final MapCodec<LampBlock> CODEC = simpleCodec(LampBlock::new);

	public LampBlock(Properties props) {
		super(props, RotaryBlockEntities.LAMP, Placement.NONE, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
