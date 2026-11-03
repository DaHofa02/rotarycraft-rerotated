/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.block;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.reactorcraft.content.blockentity.UProcessorBlockEntity;
import reika.reactorcraft.content.menu.UProcessorMenu;
import reika.rotarycraft.content.block.MachineBlock;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: BlockReactorTile / BlockReactorTileModelled with ReactorGuiHandler - a 4-sided RotaryCraft-style machine block that opens its
 * GUI; the synthesizer takes a water bucket by right-click, the fluid extractor fills an empty bucket with heavy water or an empty
 * canister with molten lithium (1000 mB each).
 */
public class ReactorGuiMachineBlock extends MachineBlock {

	private final MapCodec<ReactorGuiMachineBlock> codec;

	public ReactorGuiMachineBlock(Properties props, Supplier<? extends BlockEntityType<? extends RotaryCraftBlockEntity>> type) {
		super(props, type, Placement.FOUR_SIDED, false);
		codec = simpleCodec(p -> new ReactorGuiMachineBlock(p, type));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return codec;
	}

	@Override
	protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack is, BlockState state, Level world, BlockPos pos, Player ep,
			net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
		if (is.getCount() == 1) {
			if (world.getBlockEntity(pos) instanceof reika.reactorcraft.content.blockentity.ElectrolyzerBlockEntity te && is.is(reika.reactorcraft.registry.ReactorItems.HEAVY_WATER_BUCKET.get())) {
				if (!world.isClientSide && te.addHeavyWater(1000) && !ep.isCreative())
					ep.setItemInHand(hand, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
				return net.minecraft.world.ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
			if (world.getBlockEntity(pos) instanceof reika.reactorcraft.content.blockentity.SynthesizerBlockEntity te && is.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
				if (!world.isClientSide && te.addWater(1000) && !ep.isCreative())
					ep.setItemInHand(hand, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
				return net.minecraft.world.ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
			if (world.getBlockEntity(pos) instanceof reika.reactorcraft.content.blockentity.HeavyPumpBlockEntity te && te.getTankLevel() >= 1000) {
				net.minecraft.world.level.material.Fluid f = te.getTank().getFluid().getFluid();
				net.minecraft.world.item.ItemStack out = null;
				if (is.is(net.minecraft.world.item.Items.BUCKET) && f.isSame(reika.reactorcraft.registry.ReactorFluids.get("heavy_water")))
					out = new net.minecraft.world.item.ItemStack(reika.reactorcraft.registry.ReactorItems.HEAVY_WATER_BUCKET.get());
				else if (is.is(reika.reactorcraft.registry.ReactorItems.EMPTY_CANISTER.get()) && f.isSame(reika.reactorcraft.registry.ReactorFluids.get("lithium")))
					out = new net.minecraft.world.item.ItemStack(reika.reactorcraft.registry.ReactorItems.canisterFor(f));
				if (out != null) {
					if (!world.isClientSide) {
						te.getTank().drain(1000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
						te.setChanged();
						ep.setItemInHand(hand, out);
					}
					return net.minecraft.world.ItemInteractionResult.sidedSuccess(world.isClientSide);
				}
			}
		}
		return super.useItemOn(is, state, world, pos, ep, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown())
			return InteractionResult.PASS;
		if (world.getBlockEntity(pos) instanceof UProcessorBlockEntity te) {
			if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new UProcessorMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		if (world.getBlockEntity(pos) instanceof reika.reactorcraft.content.blockentity.IsoCentrifugeBlockEntity te) {
			if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new reika.reactorcraft.content.menu.IsoCentrifugeMenu(id, inv, te), state.getBlock().getName()),
						buf -> buf.writeBlockPos(pos));
			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		if (world.getBlockEntity(pos) instanceof reika.reactorcraft.content.blockentity.ElectrolyzerBlockEntity te) {
			if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new reika.reactorcraft.content.menu.ElectrolyzerMenu(id, inv, te), state.getBlock().getName()),
						buf -> buf.writeBlockPos(pos));
			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		if (world.getBlockEntity(pos) instanceof reika.reactorcraft.content.blockentity.SynthesizerBlockEntity te) {
			if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new reika.reactorcraft.content.menu.SynthesizerMenu(id, inv, te), state.getBlock().getName()),
						buf -> buf.writeBlockPos(pos));
			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		if (world.getBlockEntity(pos) instanceof reika.reactorcraft.waste.WasteContainerBlockEntity te) {
			if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new reika.reactorcraft.content.menu.WasteMenus.Container(id, inv, te), state.getBlock().getName()),
						buf -> buf.writeBlockPos(pos));
			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		if (world.getBlockEntity(pos) instanceof reika.reactorcraft.waste.WasteStorageBlockEntity te) {
			if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new reika.reactorcraft.content.menu.WasteMenus.Storage(id, inv, te), state.getBlock().getName()),
						buf -> buf.writeBlockPos(pos));
			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		if (world.getBlockEntity(pos) instanceof reika.reactorcraft.reactor.CpuBlockEntity te) {
			if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new reika.reactorcraft.content.menu.CpuMenu(id, inv, te), state.getBlock().getName()),
						buf -> buf.writeBlockPos(pos));
			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		return super.useWithoutItem(state, world, pos, ep, hit);
	}
}
