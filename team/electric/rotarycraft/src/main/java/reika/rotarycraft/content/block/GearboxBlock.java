/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.GearboxTypes.GearPart;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.Blocks.BlockGearbox and Items.Placers.ItemGearPlacer. Meta = ratio index*4 + 4-sided direction.
 * Lubricant buckets (and Ctrl+bucket emptying) come with the RotaryCraft buckets (step 4); lubricant can already be piped in.
 */
public class GearboxBlock extends MachineBlock {

	public GearboxBlock(Properties props) {
		super(props, RotaryBlockEntities.GEARBOX, Placement.FOUR_SIDED, true);
	}

	@Override
	protected int getPlacementMetadata(LivingEntity placer, ItemStack stack) {
		int ratio = stack.getOrDefault(RotaryComponents.GEAR_RATIO.get(), 2);
		return (ReikaMathLibrary.logbase2(ratio)-1)*4+get4SidedMetadataFromPlayerLook(placer);
	}

	private static MaterialRegistry material(BlockEntity te) {
		return te instanceof GearboxBlockEntity gbx ? gbx.getGearboxType().material : MaterialRegistry.WOOD;
	}

	@Override
	protected boolean canHarvest(Player ep, BlockEntity te) {
		return MaterialHarvest.canHarvest(ep, material(te));
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		BlockEntity te = level.getBlockEntity(pos);
		int ordinal = te instanceof GearboxBlockEntity gbx ? gbx.getGearboxType().ordinal() : 0;
		return MaterialHarvest.getDestroyProgress(player, material(te), ordinal);
	}

	@Override
	protected List<ItemStack> getHarvestDrops(BlockState state, RotaryCraftBlockEntity te) {
		ItemStack is = new ItemStack(this);
		is.applyComponents(te.collectComponents());
		return List.of(is);
	}

	@Override
	protected List<ItemStack> getPlainDrops(BlockState state, @Nullable RotaryCraftBlockEntity te) {
		if (!(te instanceof GearboxBlockEntity gbx))
			return List.of(gearboxItem(GearboxTypes.WOOD, 2));
		return List.of(gearboxItem(gbx.getGearboxType(), gbx.getRatioFromMetadata()));
	}

	public static ItemStack gearboxItem(GearboxTypes type, int ratio) {
		ItemStack is = new ItemStack(RotaryBlocks.GEARBOX.get());
		is.set(RotaryComponents.GEARBOX_TYPE.get(), type);
		is.set(RotaryComponents.GEAR_RATIO.get(), ratio);
		return is;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		ItemInteractionResult tool = super.useItemOn(held, state, world, pos, ep, hand, hit);
		if (tool != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
			return tool;
		if (!(world.getBlockEntity(pos) instanceof GearboxBlockEntity tile) || held.isEmpty())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (held.is(RotaryItems.gearPart(tile.getGearboxType(), GearPart.GEAR))) {
			boolean flag = tile.repair(1 + 20 * world.random.nextInt(18 - tile.getRatioFromMetadata()));
			if (flag && !ep.isCreative())
				held.shrink(1);
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		//Original onBlockActivated: a stack of lubricant buckets adds 1000 mB each if it all fits (empty buckets back outside creative)
		if (held.is(RotaryItems.LUBRICANT_BUCKET.get())) {
			if (!world.isClientSide && tile.getGearboxType().needsLubricant()) {
				int amt = 1000*held.getCount();
				if (tile.canTakeLubricant(amt)) {
					tile.addLubricant(amt);
					if (!ep.isCreative())
						ep.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET, held.getCount()));
					tile.syncAllData();
				}
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		for (GearboxTypes material : GearboxTypes.typeList) {
			if (held.is(RotaryItems.gearPart(material, GearPart.BEARING))) {
				if (tile.getGearboxType().acceptsBearingUpgrade(material) && tile.getBearingTier() != material) {
					if (tile.getBearingTier() != tile.getGearboxType())
						Containers.dropItemStack(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, new ItemStack(RotaryItems.gearPart(tile.getBearingTier(), GearPart.BEARING)));
					tile.setBearingTier(material);
					if (!ep.isCreative())
						held.shrink(1);
				}
				return ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}
}
