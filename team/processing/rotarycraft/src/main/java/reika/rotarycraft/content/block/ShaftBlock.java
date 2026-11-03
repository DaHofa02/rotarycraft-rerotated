/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.ShaftBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryItems;

/** Original: Reika.RotaryCraft.Blocks.BlockShaft and Items.Placers.ItemShaftPlacer. */
public class ShaftBlock extends MachineBlock {

	public ShaftBlock(Properties props) {
		super(props, RotaryBlockEntities.SHAFT, Placement.SIX_SIDED, true);
	}

	@Override
	protected int getPlacementMetadata(LivingEntity placer, ItemStack stack) {
		if (Boolean.TRUE.equals(stack.get(RotaryComponents.SHAFT_CROSS.get())))
			return 6+get4SidedMetadataFromPlayerLook(placer);
		return get6SidedMetadataFromPlayerLook(placer);
	}

	private static MaterialRegistry material(BlockEntity te) {
		return te instanceof ShaftBlockEntity sha ? sha.getShaftType() : MaterialRegistry.WOOD;
	}

	@Override
	protected boolean canHarvest(Player ep, BlockEntity te) {
		return MaterialHarvest.canHarvest(ep, material(te));
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		MaterialRegistry mat = material(level.getBlockEntity(pos));
		return MaterialHarvest.getDestroyProgress(player, mat, mat.ordinal());
	}

	@Override
	protected List<ItemStack> getHarvestDrops(BlockState state, RotaryCraftBlockEntity te) {
		ShaftBlockEntity sha = (ShaftBlockEntity)te;
		if (sha.failed()) {
			if (sha.getLevel().random.nextInt(8) != 0)
				return List.of();
			return List.of(switch(sha.getShaftType()) {
				case WOOD -> new ItemStack(Items.OAK_PLANKS, 5);
				case STONE -> new ItemStack(Items.COBBLESTONE_SLAB, 5);
				default -> new ItemStack(RotaryItems.part(CraftingPart.MOUNT)); //drop mount
			});
		}
		return List.of(shaftItem(sha.getShaftType(), sha.isCross()));
	}

	@Override
	protected List<ItemStack> getPlainDrops(BlockState state, @Nullable RotaryCraftBlockEntity te) {
		return List.of(shaftItem(material(te), false));
	}

	public static ItemStack shaftItem(MaterialRegistry mat, boolean cross) {
		ItemStack is = new ItemStack(reika.rotarycraft.registry.RotaryBlocks.SHAFT.get());
		is.set(RotaryComponents.SHAFT_MATERIAL.get(), mat);
		if (cross)
			is.set(RotaryComponents.SHAFT_CROSS.get(), true);
		return is;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		ItemInteractionResult tool = super.useItemOn(held, state, world, pos, ep, hand, hit);
		if (tool != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
			return tool;
		if (ep.isShiftKeyDown())
			return ItemInteractionResult.SUCCESS;
		if (world.getBlockEntity(pos) instanceof ShaftBlockEntity tile) {
			if (held.is(RotaryItems.shaftUnitItem(tile.getShaftType()))) {
				tile.repair();
				if (!ep.isCreative())
					held.shrink(1);
				return ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}
}
