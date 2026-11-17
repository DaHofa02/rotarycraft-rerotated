/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.BeltHubBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: MachineRegistry.BELT in BlockDMachine - 6 orientations. Belts are laid with the belt item (original ItemMulti
 * tryBeltConnection): the first hub clicked is remembered on the stack ("end1"), clicking the second one joins them if the stack holds one
 * belt per block between them (creative: free) and the connection is valid.
 */
public class BeltHubBlock extends MachineBlock {

	public BeltHubBlock(Properties props) {
		this(props, RotaryBlockEntities.BELT_HUB);
	}

	protected BeltHubBlock(Properties props, Supplier<? extends BlockEntityType<? extends RotaryCraftBlockEntity>> type) {
		super(props, type, Placement.SIX_SIDED, false);
	}

	/** Original isProperBelt: belts for belt hubs and take-off belts, chains for chain drives. */
	protected boolean isProperBelt(ItemStack is) {
		return is.is(RotaryItems.part(CraftingPart.BELT));
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		if (!this.isProperBelt(held) || !(world.getBlockEntity(pos) instanceof BeltHubBlockEntity te))
			return super.useItemOn(held, state, world, pos, ep, hand, hit);
		if (world.isClientSide)
			return ItemInteractionResult.SUCCESS;
		CompoundTag tag = held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (!tag.contains("end1")) {
			tag.putLong("end1", pos.asLong());
			CustomData.set(DataComponents.CUSTOM_DATA, held, tag);
			return ItemInteractionResult.SUCCESS;
		}
		BlockPos c1 = BlockPos.of(tag.getLong("end1"));
		if (!(world.getBlockEntity(c1) instanceof BeltHubBlockEntity bb)) {
			ep.displayClientMessage(Component.literal("Tile at other end is invalid"), false);
			return ItemInteractionResult.FAIL;
		}
		int dl = c1.distManhattan(pos)-1;
		if (held.getCount() < dl && !ep.isCreative())
			return ItemInteractionResult.FAIL;
		held.remove(DataComponents.CUSTOM_DATA);
		bb.resetOther();
		te.resetOther();
		bb.reset();
		te.reset();
		if (te.tryConnect(c1) && bb.tryConnect(pos)) {
			if (!ep.isCreative())
				held.shrink(dl);
			return ItemInteractionResult.SUCCESS;
		}
		te.reset();
		bb.reset();
		ep.displayClientMessage(Component.literal("Connection is invalid"), false);
		return ItemInteractionResult.FAIL;
	}
}
