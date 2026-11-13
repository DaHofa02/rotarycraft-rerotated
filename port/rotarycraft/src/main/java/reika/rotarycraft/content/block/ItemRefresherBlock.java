/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.REFRESHER in BlockMachine - a full block (icon "refresh"); no GUI. */
public class ItemRefresherBlock extends MachineBlock {

	private static final MapCodec<ItemRefresherBlock> CODEC = simpleCodec(ItemRefresherBlock::new);

	public ItemRefresherBlock(Properties props) {
		super(props, RotaryBlockEntities.ITEM_REFRESHER, Placement.NONE, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}
}
