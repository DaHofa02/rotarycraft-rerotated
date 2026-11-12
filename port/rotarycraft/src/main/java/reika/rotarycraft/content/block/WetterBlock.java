/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.WETTER in BlockMIMachine - no orientation; the item goes in through the one-slot GUI (GuiWetter). */
public class WetterBlock extends MachineBlock {

	private static final MapCodec<WetterBlock> CODEC = simpleCodec(WetterBlock::new);

	public WetterBlock(Properties props) {
		super(props, RotaryBlockEntities.WETTER, Placement.NONE, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
