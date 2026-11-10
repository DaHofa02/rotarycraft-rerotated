/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.BlockFillerBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.SplitterBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.EngineUpgradeItem;
import reika.rotarycraft.content.item.MeterItem;
import reika.rotarycraft.content.item.ScrewdriverItem;
import reika.rotarycraft.content.menu.MachineMenu;
import reika.rotarycraft.registry.RotaryMenus;
import reika.rotarycraft.content.blockentity.TemperatureMachine;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.RotaryCraft.Base.BlockBasicMultiTE / BlockModelledMultiTE and the machine placer items.
 * Orientation is kept in the block entity (see {@link RotaryCraftBlockEntity#getBlockMetadata()}), set on placement from the player's
 * look exactly as RotaryAux.get4/6SidedMetadataFromPlayerLook did. Machines are drawn by their block entity renderers.
 */
public class MachineBlock extends BaseEntityBlock {

	/** Original placement rules (MachineRegistry.is6Sided/is4Sided and the custom placers). */
	public enum Placement {
		/** No orientation from the player (e.g. bevel gears, which find their own route). */
		NONE,
		SIX_SIDED,
		FOUR_SIDED,
		/** ItemFlywheelPlacer: 4-sided, then meta^1. */
		FLYWHEEL,
		/** ItemMachinePlacer with isXFlipped and isZFlipped (Bedrock Breaker): 6-sided, then 0<->1 and 2<->3. */
		SIX_SIDED_XZ_FLIPPED,
		/** ItemMachinePlacer with is4Sided and isZFlipped (heat ray): 4-sided, then 2<->3. */
		FOUR_SIDED_Z_FLIPPED,
		/** ItemMachinePlacer with is6Sided and isZFlipped (fan): 6-sided, then 2<->3. */
		SIX_SIDED_Z_FLIPPED,
		/** MachineRegistry.is2Sided (pump, ...): RotaryAux.get2SidedMetadataFromPlayerLook, 0 = north/south, 1 = east/west. */
		TWO_SIDED,
		/** MachineRegistry.isSidePlaced (cooling fin): attached to the clicked block (ItemMachinePlacer side -> 1, 0, 4, 2, 5, 3). */
		SIDE_ATTACHED;
	}

	/** Original RotaryAux.canHarvestSteelMachine: any tool that can harvest iron ore. */
	public static final BiPredicate<Player, BlockEntity> STEEL_MACHINE = (ep, te) -> !ep.isCreative() && ep.hasCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState());

	private final Supplier<? extends BlockEntityType<? extends RotaryCraftBlockEntity>> type;
	private final Placement placement;
	private final boolean canFlip;
	private final MapCodec<MachineBlock> codec;

	public MachineBlock(Properties props, Supplier<? extends BlockEntityType<? extends RotaryCraftBlockEntity>> type, Placement placement, boolean canFlip) {
		super(props);
		this.type = type;
		this.placement = placement;
		this.canFlip = canFlip;
		codec = simpleCodec(p -> new MachineBlock(p, type, placement, canFlip));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return codec;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return type.get().create(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
		return createTickerHelper(given, type.get(), RotaryCraftBlockEntity::tick);
	}

	public static int get4SidedMetadataFromPlayerLook(LivingEntity ep) {
		int i = Mth.floor((ep.getYRot() * 4F) / 360F + 0.5D);
		while (i > 3)
			i -= 4;
		while (i < 0)
			i += 4;
		return switch (i) {
			case 0 -> 2;
			case 1 -> 1;
			case 2 -> 3;
			case 3 -> 0;
			default -> -1;
		};
	}

	/** Original RotaryAux.get2SidedMetadataFromPlayerLook: yaw quadrant 0/2 -> 0, 1/3 -> 1. */
	public static int get2SidedMetadataFromPlayerLook(LivingEntity ep) {
		int i = Mth.floor((ep.getYRot() * 4F) / 360F + 0.5D);
		while (i > 3)
			i -= 4;
		while (i < 0)
			i += 4;
		return i%2;
	}

	public static int get6SidedMetadataFromPlayerLook(LivingEntity ep) {
		if (Math.abs(ep.getXRot()) < 60) {
			return get4SidedMetadataFromPlayerLook(ep);
		}
		else { //Looking up/down
			return ep.getXRot() > 0 ? 4 : 5;
		}
	}

	/** Original: RotaryAux.shouldSetFlipped - solid above and soft below. */
	public static boolean shouldSetFlipped(Level world, BlockPos pos) {
		boolean softBelow = world.getBlockState(pos.below()).canBeReplaced();
		boolean softAbove = world.getBlockState(pos.above()).canBeReplaced();
		return !softAbove && softBelow;
	}

	/** Orientation for a new machine; subclasses add type-specific offsets (gearbox ratio, shaft cross). */
	protected int getPlacementMetadata(LivingEntity placer, ItemStack stack) {
		return switch (placement) {
			case NONE -> 0;
			case SIX_SIDED -> get6SidedMetadataFromPlayerLook(placer);
			case FOUR_SIDED -> get4SidedMetadataFromPlayerLook(placer);
			case TWO_SIDED -> get2SidedMetadataFromPlayerLook(placer);
			case FLYWHEEL -> get4SidedMetadataFromPlayerLook(placer)^1;
			case FOUR_SIDED_Z_FLIPPED -> {
				int m = get4SidedMetadataFromPlayerLook(placer);
				yield m == 2 ? 3 : m == 3 ? 2 : m;
			}
			case SIX_SIDED_Z_FLIPPED -> {
				int m = get6SidedMetadataFromPlayerLook(placer);
				yield m == 2 ? 3 : m == 3 ? 2 : m;
			}
			case SIX_SIDED_XZ_FLIPPED -> {
				int m = get6SidedMetadataFromPlayerLook(placer);
				yield m < 4 ? m^1 : m;
			}
			case SIDE_ATTACHED -> {
				net.minecraft.core.Direction face = CLICKED_FACE.get();
				yield face == null ? 0 : new int[]{1, 0, 4, 2, 5, 3}[face.ordinal()];
			}
		};
	}

	/** The face clicked when placing (only known in getStateForPlacement), for SIDE_ATTACHED machines. */
	private static final ThreadLocal<net.minecraft.core.Direction> CLICKED_FACE = new ThreadLocal<>();

	@Override
	public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
		CLICKED_FACE.set(ctx.getClickedFace());
		return super.getStateForPlacement(ctx);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		if (world.getBlockEntity(pos) instanceof RotaryCraftBlockEntity te) {
			if (placer != null) {
				te.setBlockMetadata(this.getPlacementMetadata(placer, stack));
				if (placer instanceof Player ep)
					te.setPlacer(ep);
			}
			if (canFlip)
				te.isFlipped = shouldSetFlipped(world, pos);
			if (te instanceof TemperatureMachine tm)
				tm.setTemperature(AmbientTemperature.getAmbientTemperatureAt(world, pos));
			te.syncAllData();
			//The orientation is only known now; let neighbours that cache connections (pipes) look again.
			if (!world.isClientSide)
				world.updateNeighborsAt(pos, state.getBlock());
		}
	}

	/** Whether this player's tool harvests the machine (original canHarvest/harvestBlock checks). */
	protected boolean canHarvest(Player ep, BlockEntity te) {
		return STEEL_MACHINE.test(ep, te);
	}

	@Override
	public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
		BlockEntity te = level.getBlockEntity(pos);
		return te != null && this.canHarvest(player, te);
	}

	/** Item data that is the machine's contents or wear, not its variant: not part of a middle-click pick (vanilla copies it on Ctrl). */
	private static final java.util.Set<java.util.function.Supplier<? extends net.minecraft.core.component.DataComponentType<?>>> CONTENTS = java.util.Set.of(
			reika.rotarycraft.registry.RotaryComponents.GEARBOX_DATA, reika.rotarycraft.registry.RotaryComponents.RESERVOIR_DATA,
			reika.rotarycraft.registry.RotaryComponents.JET_DAMAGE);

	/**
	 * Original getPickBlock: the machine as crafted - its variant (gearbox material and ratio, shaft material, flywheel type, bedrock
	 * versions ...), without contents or wear.
	 */
	@Override
	public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, net.minecraft.world.level.LevelReader level,
			BlockPos pos, Player player) {
		ItemStack is = super.getCloneItemStack(state, target, level, pos, player);
		if (!is.is(this.asItem()) || !(level.getBlockEntity(pos) instanceof RotaryCraftBlockEntity te))
			return is;
		is.applyComponents(te.collectComponents());
		for (var c : CONTENTS)
			is.remove(c.get());
		return is;
	}

	/** Drops when harvested by a player with a suitable tool (original harvestBlock). */
	protected List<ItemStack> getHarvestDrops(BlockState state, RotaryCraftBlockEntity te) {
		ItemStack is = new ItemStack(this);
		is.applyComponents(te.collectComponents());
		return List.of(is);
	}

	/** Drops without a player, e.g. explosions (original getDrops: the plain machine item). */
	protected List<ItemStack> getPlainDrops(BlockState state, @Nullable RotaryCraftBlockEntity te) {
		return List.of(new ItemStack(this));
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		Entity breaker = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
		RotaryCraftBlockEntity te = be instanceof RotaryCraftBlockEntity r ? r : null;
		if (breaker instanceof Player ep) {
			if (te == null || !this.canHarvest(ep, te))
				return List.of();
			return this.getHarvestDrops(state, te);
		}
		return this.getPlainDrops(state, te);
	}

	/** Which GUI a machine opens on right-click, if any (original GuiHandler / RotaryAux.hasGui). */
	@Nullable
	protected static MachineMenu.Layout getLayout(BlockEntity te) {
		if (te instanceof EngineBlockEntity e) {
			return switch (e.getEngineType()) {
				case STEAM -> MachineMenu.Layout.STEAM;
				case GAS -> MachineMenu.Layout.ETHANOL;
				case AC -> MachineMenu.Layout.ONE_SLOT;
				case SPORT -> MachineMenu.Layout.PERFORMANCE;
				case MICRO, JET -> MachineMenu.Layout.JET;
				default -> null;
			};
		}
		if (te instanceof BlockFillerBlockEntity)
			return MachineMenu.Layout.ONE_SLOT;
		if (te instanceof GearboxBlockEntity)
			return MachineMenu.Layout.GEARBOX;
		if (te instanceof SplitterBlockEntity spl)
			return spl.getBlockMetadata() >= 8 ? MachineMenu.Layout.SPLITTER : null;
		if (te instanceof BevelGearBlockEntity)
			return MachineMenu.Layout.BEVEL;
		return null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof RotaryCraftBlockEntity te))
			return InteractionResult.PASS;
		MachineMenu.Layout layout = getLayout(te);
		if (layout == null)
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp)) {
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new MachineMenu(RotaryMenus.get(layout), id, inv, layout, te),
					state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		//Tools act on their own (original ItemRegistry.overridesRightClick: screwdriver, meter, upgrades, ...).
		if (held.getItem() instanceof ScrewdriverItem || held.getItem() instanceof MeterItem || held.getItem() instanceof EngineUpgradeItem)
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		//Original BlockBasicMultiTE.onBlockActivated: a bedrock 2:1 gear unit turns a shaft junction into a bedrock junction
		if (world.getBlockEntity(pos) instanceof SplitterBlockEntity spl && !spl.isBedrock()
				&& held.is(reika.rotarycraft.registry.RotaryItems.gearPart(reika.rotarycraft.registry.GearboxTypes.BEDROCK, reika.rotarycraft.registry.GearboxTypes.GearPart.UNIT2))) {
			if (!world.isClientSide) {
				spl.setBedrock();
				if (!ep.isCreative())
					held.shrink(1);
				spl.syncAllData();
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		//Original BlockBasicMultiTE.onBlockActivated: a fluid container is emptied into the fuel enhancer if it all fits (creative: always)
		if (world.getBlockEntity(pos) instanceof reika.rotarycraft.content.blockentity.FuelEnhancerBlockEntity tf) {
			var liq = net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(held);
			if (liq.isPresent()) {
				if (!world.isClientSide) {
					int amt = tf.fill(net.minecraft.core.Direction.UP, liq.get(), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
					if (amt >= liq.get().getAmount() || ep.isCreative()) {
						tf.fill(net.minecraft.core.Direction.UP, liq.get(), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
						if (!ep.isCreative())
							ep.setItemInHand(hand, held.getCraftingRemainingItem().isEmpty() ? new ItemStack(net.minecraft.world.item.Items.BUCKET) : held.getCraftingRemainingItem());
						tf.syncAllData();
					}
				}
				return ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		if (!world.isClientSide && !state.is(newState.getBlock()) && world.getBlockEntity(pos) instanceof RotaryCraftBlockEntity te)
			te.onBlockBroken(world, pos);
		super.onRemove(state, world, pos, newState, moved);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
		return world.getBlockEntity(pos) instanceof RotaryCraftBlockEntity te ? te.getRedstoneOverride() : 0;
	}
}
