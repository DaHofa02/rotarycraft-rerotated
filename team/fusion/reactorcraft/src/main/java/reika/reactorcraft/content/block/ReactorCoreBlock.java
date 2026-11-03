/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.block;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import reika.reactorcraft.content.menu.NuclearCoreMenu;
import reika.reactorcraft.reactor.LiquidStates;
import reika.reactorcraft.reactor.NuclearCoreBlockEntity;
import reika.reactorcraft.reactor.WaterCellBlockEntity;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.content.block.MachineBlock;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: BlockReactorTile for the fuel core and the coolant cell - a full block whose texture state (property {@link #STATE}) is the
 * column position for cores (0 alone, 1 with a core above, 2 in the middle, 3 with a core below; top and bottom faces fixed) and the
 * liquid for coolant cells. Cores open their GUI; coolant cells take and give their liquid with buckets and canisters.
 */
public class ReactorCoreBlock extends MachineBlock {

	public static final IntegerProperty STATE = IntegerProperty.create("state", 0, 4);

	private final MapCodec<ReactorCoreBlock> codec;

	public ReactorCoreBlock(Properties props, Supplier<? extends BlockEntityType<? extends RotaryCraftBlockEntity>> type) {
		super(props, type, Placement.FOUR_SIDED, false);
		codec = simpleCodec(p -> new ReactorCoreBlock(p, type));
		this.registerDefaultState(this.stateDefinition.any().setValue(STATE, 0));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return codec;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(STATE);
	}

	public static void setTextureState(Level world, BlockPos pos, int state) {
		BlockState s = world.getBlockState(pos);
		if (s.hasProperty(STATE) && s.getValue(STATE) != state)
			world.setBlock(pos, s.setValue(STATE, state), 3);
	}

	/** Original TileEntityNuclearCore.getTextureState for the side faces. */
	private void updateColumnState(Level world, BlockPos pos) {
		if (world.isClientSide || !(world.getBlockEntity(pos) instanceof NuclearCoreBlockEntity || world.getBlockEntity(pos) instanceof reika.reactorcraft.reactor.ReactorBoilerBlockEntity
				|| world.getBlockEntity(pos) instanceof reika.reactorcraft.waste.WasteDecayerBlockEntity
				|| world.getBlockEntity(pos) instanceof reika.reactorcraft.reactor.IntermediateBoilerBlockEntity
				|| world.getBlockEntity(pos) instanceof reika.reactorcraft.reactor.PebbleBedBlockEntity
				|| world.getBlockEntity(pos) instanceof reika.reactorcraft.reactor.TritizerBlockEntity))
			return;
		boolean up = world.getBlockState(pos.above()).is(this), down = world.getBlockState(pos.below()).is(this);
		setTextureState(world, pos, up && down ? 2 : up ? 1 : down ? 3 : 0);
	}

	@Override
	protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moving) {
		super.onPlace(state, world, pos, old, moving);
		this.updateColumnState(world, pos);
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
		super.neighborChanged(state, world, pos, block, from, moving);
		this.updateColumnState(world, pos);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		this.updateColumnState(world, pos);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown())
			return InteractionResult.PASS;
		if (world.getBlockEntity(pos) instanceof reika.reactorcraft.reactor.ThoriumCoreBlockEntity te) {
			if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new reika.reactorcraft.content.menu.ThoriumCoreMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		if (world.getBlockEntity(pos) instanceof reika.reactorcraft.reactor.PebbleBedBlockEntity te) {
			if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new reika.reactorcraft.content.menu.WasteMenus.PebbleBed(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		if (world.getBlockEntity(pos) instanceof reika.reactorcraft.waste.WasteDecayerBlockEntity te) {
			if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new reika.reactorcraft.content.menu.WasteMenus.Decayer(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		if (world.getBlockEntity(pos) instanceof NuclearCoreBlockEntity te) {
			if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new NuclearCoreMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		return super.useWithoutItem(state, world, pos, ep, hit);
	}

	/** Original BlockReactorTile.onBlockActivated for the coolant cell and the reactor boiler (single items only). */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack is, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		//original: a sodium canister fills 1000 mB into the sodium heater and is replaced by an (iron) bucket - kept as in the original
		if (!ep.isShiftKeyDown() && is.getCount() == 1 && world.getBlockEntity(pos) instanceof reika.reactorcraft.reactor.SodiumHeaterBlockEntity sh
				&& is.is(ReactorItems.canisterFor(reika.reactorcraft.registry.ReactorFluids.get("sodium")))) {
			if (sh.getTank().getFluidAmount()+1000 <= reika.reactorcraft.reactor.SodiumHeaterBlockEntity.CAPACITY) {
				if (!world.isClientSide && sh.addSodium(1000) && !ep.isCreative())
					ep.setItemInHand(hand, new ItemStack(Items.BUCKET));
				return ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
		}
		if (!ep.isShiftKeyDown() && is.getCount() == 1 && world.getBlockEntity(pos) instanceof reika.reactorcraft.reactor.ReactorBoilerBlockEntity te
				&& te.getTank().getFluidAmount()+1000 <= reika.reactorcraft.reactor.ReactorBoilerBlockEntity.CAPACITY) {
			net.minecraft.world.level.material.Fluid f = null;
			ItemStack back = null;
			if (is.is(Items.WATER_BUCKET)) {
				f = net.minecraft.world.level.material.Fluids.WATER;
				back = new ItemStack(Items.BUCKET);
			}
			else if (is.is(ReactorItems.canisterFor(reika.reactorcraft.registry.ReactorFluids.get("ammonia")))) {
				f = reika.reactorcraft.registry.ReactorFluids.get("ammonia");
				back = new ItemStack(ReactorItems.EMPTY_CANISTER.get());
			}
			if (f != null) {
				if (!world.isClientSide && te.addLiquid(f, 1000) && !ep.isCreative())
					ep.setItemInHand(hand, back);
				return ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
		}
		if (!ep.isShiftKeyDown() && is.getCount() == 1 && world.getBlockEntity(pos) instanceof WaterCellBlockEntity te) {
			ItemStack back = null;
			LiquidStates to = null;
			switch (te.getLiquidState()) {
				case EMPTY -> {
					if (is.is(Items.WATER_BUCKET)) {
						to = LiquidStates.WATER;
						back = new ItemStack(Items.BUCKET);
					}
					else if (is.is(ReactorItems.HEAVY_WATER_BUCKET.get())) {
						to = LiquidStates.HEAVY;
						back = new ItemStack(Items.BUCKET);
					}
					else if (is.is(ReactorItems.canisterFor(reika.reactorcraft.registry.ReactorFluids.get("sodium")))) {
						to = LiquidStates.SODIUM;
						back = new ItemStack(ReactorItems.EMPTY_CANISTER.get());
					}
					else if (is.is(ReactorItems.canisterFor(reika.reactorcraft.registry.ReactorFluids.get("lifbe")))) {
						to = LiquidStates.LITHIUM;
						back = new ItemStack(ReactorItems.EMPTY_CANISTER.get());
					}
				}
				case WATER -> {
					if (is.is(Items.BUCKET)) {
						to = LiquidStates.EMPTY;
						back = new ItemStack(Items.WATER_BUCKET);
					}
				}
				case HEAVY -> {
					if (is.is(Items.BUCKET)) {
						to = LiquidStates.EMPTY;
						back = new ItemStack(ReactorItems.HEAVY_WATER_BUCKET.get());
					}
				}
				case SODIUM -> {
					if (is.is(ReactorItems.EMPTY_CANISTER.get())) {
						to = LiquidStates.EMPTY;
						back = new ItemStack(ReactorItems.canisterFor(reika.reactorcraft.registry.ReactorFluids.get("sodium")));
					}
				}
				case LITHIUM -> {
					if (is.is(ReactorItems.EMPTY_CANISTER.get())) {
						to = LiquidStates.EMPTY;
						back = new ItemStack(ReactorItems.canisterFor(reika.reactorcraft.registry.ReactorFluids.get("lifbe")));
					}
				}
			}
			if (to != null) {
				if (!world.isClientSide) {
					te.setLiquidState(to);
					//original: filling a cell in creative keeps the item, emptying it always gives the full container
					if (!ep.isCreative() || to == LiquidStates.EMPTY)
						ep.setItemInHand(hand, back);
				}
				return ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
		}
		return super.useItemOn(is, state, world, pos, ep, hand, hit);
	}
}
