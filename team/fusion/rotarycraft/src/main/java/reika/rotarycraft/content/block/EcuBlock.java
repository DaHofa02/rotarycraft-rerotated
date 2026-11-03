/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.EngineControllerBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: MachineRegistry.ECU in BlockMachine - a plain textured cube (steel bottom, ecu_top, ecu_side; after a dye right-click the sides
 * show ecu_color/&lt;dye&gt;). The colour lives in the blockstate so the block model can pick the texture.
 */
public class EcuBlock extends MachineBlock {

	/** 0 = no colour, 1-16 = original ReikaDyeHelper ordinal + 1 (black .. white). */
	public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 16);

	private static final MapCodec<EcuBlock> CODEC = simpleCodec(EcuBlock::new);

	public EcuBlock(Properties props) {
		super(props, RotaryBlockEntities.ECU, Placement.NONE, false);
		this.registerDefaultState(this.stateDefinition.any().setValue(COLOR, 0));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(COLOR);
	}

	/** Original ReikaDyeHelper order is the 1.7 dye damage (black first); DyeColor ids run the other way. */
	public static int originalDyeIndex(net.minecraft.world.item.DyeColor c) {
		return 15-c.getId();
	}

	/** Original BlockBasicMultiTE.onBlockActivated, ECU branch: a dye sets the signal colour. */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		ItemInteractionResult tool = super.useItemOn(held, state, world, pos, ep, hand, hit);
		if (tool != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
			return tool;
		if (held.getItem() instanceof DyeItem dye && world.getBlockEntity(pos) instanceof EngineControllerBlockEntity te) {
			if (!world.isClientSide)
				te.setColor(originalDyeIndex(dye.getDyeColor()));
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}
}
