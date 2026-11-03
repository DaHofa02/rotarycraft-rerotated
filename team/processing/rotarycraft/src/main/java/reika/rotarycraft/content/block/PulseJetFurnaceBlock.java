/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.rotarycraft.content.blockentity.PulseJetFurnaceBlockEntity;
import reika.rotarycraft.content.menu.PulseJetFurnaceMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryDamage;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: MachineRegistry.PULSEJET in BlockMIMachine - 9/16 of a block high, burns what touches it (dealsHeatDamage), jet fuel
 * buckets (a whole stack at once) and water buckets go in by hand (BlockBasicMultiTE.onBlockActivated); otherwise the GUI opens.
 */
public class PulseJetFurnaceBlock extends MachineBlock {

	private static final MapCodec<PulseJetFurnaceBlock> CODEC = simpleCodec(PulseJetFurnaceBlock::new);
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 9, 16);

	public PulseJetFurnaceBlock(Properties props) {
		super(props, RotaryBlockEntities.PULSE_JET, Placement.NONE, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	protected void entityInside(BlockState state, Level world, BlockPos pos, Entity e) {
		if (world.isClientSide || e instanceof ItemEntity || e instanceof ExperienceOrb)
			return;
		if (world.getBlockEntity(pos) instanceof PulseJetFurnaceBlockEntity te && te.getThermalDamage() > 0) {
			e.hurt(RotaryDamage.source(world, RotaryDamage.HEAT), te.getThermalDamage());
			e.igniteForSeconds(6);
		}
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack is, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		ItemInteractionResult tool = super.useItemOn(is, state, world, pos, ep, hand, hit);
		if (tool != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
			return tool;
		if (!(world.getBlockEntity(pos) instanceof PulseJetFurnaceBlockEntity ex))
			return tool;
		if (is.is(RotaryItems.JET_FUEL_BUCKET.get()) && ex.getFuel()+1000*is.getCount() <= PulseJetFurnaceBlockEntity.MAXFUEL) {
			if (!world.isClientSide) {
				ex.addFuel(1000*is.getCount());
				if (!ep.isCreative())
					ep.setItemInHand(hand, new ItemStack(Items.BUCKET, is.getCount()));
				ex.syncAllData();
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		if (is.is(Items.WATER_BUCKET) && ex.getWater()+1000 <= PulseJetFurnaceBlockEntity.CAPACITY) {
			if (!world.isClientSide) {
				ex.addWater(1000);
				if (!ep.isCreative())
					ep.setItemInHand(hand, new ItemStack(Items.BUCKET));
				ex.syncAllData();
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof PulseJetFurnaceBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new PulseJetFurnaceMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
