/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.block;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.electricraft.content.blockentity.FuseBlockEntity;
import reika.electricraft.content.blockentity.GeneratorBlockEntity;
import reika.electricraft.content.blockentity.ResistorBlockEntity;
import reika.electricraft.content.blockentity.WireComponentBlockEntity;
import reika.electricraft.registry.ElectriComponents;
import reika.electricraft.content.blockentity.MotorBlockEntity;
import reika.rotarycraft.content.block.MachineBlock;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.item.ScrewdriverItem;

/**
 * Original: ElectriCraft BlockElectricMachine with ItemElectriPlacer - the generator and motor face the way the player looks when placed;
 * the screwdriver turns them to the next horizontal side; a motor takes its amplifier (red gold, tungsten, bedrock ingot) by right-click.
 * Wire components (relay, resistors, fuse) have the box of their getAABB; a dye on a resistor band sets that band (original
 * onBlockActivated: the band is found from where along the resistor the click hit).
 */
public class ElectriMachineBlock extends MachineBlock {

	private final MapCodec<ElectriMachineBlock> codec;

	public ElectriMachineBlock(Properties props, Supplier<? extends BlockEntityType<? extends RotaryCraftBlockEntity>> type) {
		super(props, type, Placement.NONE, true);
		codec = simpleCodec(p -> new ElectriMachineBlock(p, type));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return codec;
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		if (placer != null) {
			if (world.getBlockEntity(pos) instanceof GeneratorBlockEntity g)
				g.setFacing(placer.getDirection());
			if (world.getBlockEntity(pos) instanceof MotorBlockEntity m)
				m.setFacing(placer.getDirection());
			if (world.getBlockEntity(pos) instanceof WireComponentBlockEntity c)
				c.setFacing(placer.getDirection());
			if (world.getBlockEntity(pos) instanceof reika.electricraft.content.blockentity.TransformerBlockEntity t)
				t.setFacing(placer.getDirection().getClockWise()); //original ReikaDirectionHelper.getRightBy90
			//the facing is only known now: let the wires next to it redraw their connections
			if (!world.isClientSide)
				world.updateNeighborsAt(pos, state.getBlock());
		}
	}

	/** Original: the transformer opens its GUI on any right-click (except with the screwdriver). */
	private static void openTransformer(Level world, Player ep, reika.electricraft.content.blockentity.TransformerBlockEntity t) {
		if (!world.isClientSide && ep instanceof net.minecraft.server.level.ServerPlayer sp && t.isPlayerAccessible(sp))
			sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new reika.electricraft.content.menu.TransformerMenu(id, inv, t),
					t.getBlockState().getBlock().getName()), buf -> buf.writeBlockPos(t.getBlockPos()));
	}

	@Override
	protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (world.getBlockEntity(pos) instanceof reika.electricraft.content.blockentity.TransformerBlockEntity t) {
			openTransformer(world, ep, t);
			return net.minecraft.world.InteractionResult.sidedSuccess(world.isClientSide);
		}
		return super.useWithoutItem(state, world, pos, ep, hit);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		if (level.getBlockEntity(pos) instanceof WireComponentBlockEntity c)
			return c.getShape();
		if (level.getBlockEntity(pos) instanceof reika.electricraft.content.blockentity.TransformerBlockEntity t)
			return t.getShape();
		return super.getShape(state, level, pos, ctx);
	}

	/** Original BlockElectricMachine.onBlockActivated, resistor branch: which band (1..n) the click position falls on, or -1. */
	private static int getClickedBand(ResistorBlockEntity te, BlockPos pos, BlockHitResult hit) {
		Direction dir = te.getFacing();
		double a = hit.getLocation().x-pos.getX();
		double c = hit.getLocation().z-pos.getZ();
		double inc = dir.getStepX() != 0 ? a : c;
		if (dir.getStepX() > 0 || dir.getStepZ() > 0)
			inc = 1-inc;
		if (te.isFlipped && dir.getStepZ() != 0)
			inc = 1-inc;
		double dl = 0.1875;
		double l0 = 0.125;
		double w = 0.125;
		for (int i = 0; i < te.getColorBands().length; i++) {
			double d0 = dl*i;
			if (inc >= d0+l0 && inc <= d0+l0+w)
				return i+1;
		}
		return -1;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, ctx, tooltip, flag);
		Integer lim = stack.get(ElectriComponents.FUSE_LIMIT.get());
		if (lim != null)
			tooltip.add(Component.literal("Current Limit: "+lim+"A"));
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		if (held.getItem() instanceof ScrewdriverItem) {
			if (!world.isClientSide) {
				if (world.getBlockEntity(pos) instanceof GeneratorBlockEntity g)
					g.incrementFacing();
				if (world.getBlockEntity(pos) instanceof MotorBlockEntity m)
					m.incrementFacing();
				if (world.getBlockEntity(pos) instanceof WireComponentBlockEntity c)
					c.incrementFacing();
				if (world.getBlockEntity(pos) instanceof reika.electricraft.content.blockentity.TransformerBlockEntity t)
					t.incrementFacing();
				world.updateNeighborsAt(pos, state.getBlock());
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		if (world.getBlockEntity(pos) instanceof reika.electricraft.content.blockentity.TransformerBlockEntity t) {
			openTransformer(world, ep, t);
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		DyeColor dye = DyeColor.getColor(held);
		if (dye != null && world.getBlockEntity(pos) instanceof ResistorBlockEntity r) {
			int band = getClickedBand(r, pos, hit);
			if (band > 0) {
				if (world.isClientSide)
					return ItemInteractionResult.SUCCESS;
				if (r.setColor(dye, band)) {
					if (!ep.isCreative())
						held.shrink(1);
					return ItemInteractionResult.SUCCESS;
				}
				return ItemInteractionResult.FAIL;
			}
		}
		if (world.getBlockEntity(pos) instanceof MotorBlockEntity m && !held.isEmpty()) {
			if (!world.isClientSide && m.upgrade(held)) {
				if (!ep.isCreative())
					held.shrink(1);
				m.syncAllData();
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		return super.useItemOn(held, state, world, pos, ep, hand, hit);
	}
}
