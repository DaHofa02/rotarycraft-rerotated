/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.fluids.FluidUtil;

import reika.rotarycraft.content.blockentity.ReservoirBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: MachineRegistry.RESERVOIR in BlockMachine, its branches of BlockBasicMultiTE (onBlockActivated: glass pane covers it, fluid
 * containers fill or empty it, a glass bottle takes water; onEntityCollidedWithBlock: fluid effects) and the complex hitbox (floor,
 * the walls towards non-reservoir sides, the cover).
 */
public class ReservoirBlock extends MachineBlock {

	private static final MapCodec<ReservoirBlock> CODEC = simpleCodec(ReservoirBlock::new);
	private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 1, 16);
	private static final VoxelShape COVER = Block.box(1, 14, 1, 15, 15, 15);

	public ReservoirBlock(Properties props) {
		super(props, RotaryBlockEntities.RESERVOIR, Placement.NONE, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		if (!(level.getBlockEntity(pos) instanceof ReservoirBlockEntity tr))
			return Shapes.block();
		VoxelShape s = FLOOR;
		if (tr.isCovered)
			s = Shapes.or(s, COVER);
		if (!tr.isConnectedOnSide(Direction.EAST))
			s = Shapes.or(s, Block.box(15, 0, 0, 16, 16, 16));
		if (!tr.isConnectedOnSide(Direction.WEST))
			s = Shapes.or(s, Block.box(0, 0, 0, 1, 16, 16));
		if (!tr.isConnectedOnSide(Direction.SOUTH))
			s = Shapes.or(s, Block.box(0, 0, 15, 16, 16, 16));
		if (!tr.isConnectedOnSide(Direction.NORTH))
			s = Shapes.or(s, Block.box(0, 0, 0, 16, 16, 1));
		return s;
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
		super.neighborChanged(state, world, pos, block, from, moving);
		if (!world.isClientSide && world.getBlockEntity(pos) instanceof ReservoirBlockEntity tr)
			tr.updateSides(world, pos);
	}

	@Override
	protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moving) {
		super.onPlace(state, world, pos, old, moving);
		if (!world.isClientSide)
			ReservoirBlockEntity.updateNeighbors(world, pos);
	}

	@Override
	protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		super.onRemove(state, world, pos, newState, moved);
		if (!world.isClientSide && !state.is(newState.getBlock()))
			ReservoirBlockEntity.updateNeighbors(world, pos);
	}

	@Override
	protected void entityInside(BlockState state, Level world, BlockPos pos, Entity e) {
		if (!world.isClientSide && e instanceof LivingEntity le && world.getBlockEntity(pos) instanceof ReservoirBlockEntity tr)
			tr.applyFluidEffectsToEntity(le);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack is, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		ItemInteractionResult tool = super.useItemOn(is, state, world, pos, ep, hand, hit);
		if (tool != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
			return tool;
		if (!(world.getBlockEntity(pos) instanceof ReservoirBlockEntity tr) || !tr.isPlayerAccessible(ep))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (is.is(Items.GLASS_PANE)) {
			if (tr.isCovered)
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			if (!world.isClientSide) {
				tr.isCovered = true;
				if (!ep.isCreative())
					is.shrink(1);
				tr.syncAllData();
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		if (is.is(Items.GLASS_BOTTLE)) {
			if (tr.getFluidAmount() > 0 && tr.getFluid().isSame(Fluids.WATER)) {
				if (!world.isClientSide) {
					//original: the whole stack becomes water bottles, nothing is taken from the tank
					ItemStack pot = new ItemStack(Items.POTION, is.getCount());
					pot.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
					ep.setItemInHand(hand, pot);
					tr.syncAllData();
				}
				return ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (FluidUtil.getFluidHandler(is).isPresent()) {
			if (!world.isClientSide) {
				boolean done = FluidUtil.interactWithFluidHandler(ep, hand, tr.getFluidHandler(hit.getDirection() == Direction.UP ? Direction.NORTH : hit.getDirection()));
				if (done)
					tr.syncAllData();
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}
}
