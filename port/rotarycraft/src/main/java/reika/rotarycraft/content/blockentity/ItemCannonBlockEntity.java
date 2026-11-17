/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.TileEntityItemCannon - with 32768 W (at least 128 Nm) from below it sends its 9 slots to
 * another item cannon at a typed-in position (in any dimension): every 8 ticks the first stack goes over if the target has room for it -
 * the whole stack from 524288 W up, otherwise one item - with a bang and a short-lived item flying towards the target. Items can only be
 * taken out from below; a comparator-style redstone signal of 15 when it is empty.
 */
public class ItemCannonBlockEntity extends PowerReceiverBlockEntity {

	public static final int STACKPOWER = 524288;

	private final ItemStackHandler inv = new ItemStackHandler(9) {
		@Override
		protected void onContentsChanged(int slot) {
			ItemCannonBlockEntity.this.setChanged();
		}
	};
	@Nullable
	private ResourceKey<Level> targetDim;
	@Nullable
	private BlockPos target;
	private int tickcount;

	public ItemCannonBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.ITEM_CANNON.get(), pos, state, 128, 1, 32768);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Original canExtractItem: only from the bottom side. */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new net.neoforged.neoforge.items.wrapper.RangedWrapper(inv, 0, 9) {
			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return side == Direction.DOWN ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}
		};
	}

	/** The original's dimension ids: 0 overworld, -1 nether, 1 end (others are not known to the port). */
	@Nullable
	public static ResourceKey<Level> dimension(int id) {
		return switch (id) {
			case 0 -> Level.OVERWORLD;
			case -1 -> Level.NETHER;
			case 1 -> Level.END;
			default -> null;
		};
	}

	public static int dimensionId(@Nullable ResourceKey<Level> key) {
		return key == Level.NETHER ? -1 : key == Level.END ? 1 : 0;
	}

	public void setTarget(@Nullable ResourceKey<Level> dim, BlockPos pos) {
		if (dim == null)
			return;
		targetDim = dim;
		target = pos.immutable();
		this.syncAllData();
	}

	@Nullable
	public BlockPos getTarget() {
		return target;
	}

	public int getTargetDimensionId() {
		return dimensionId(targetDim);
	}

	public int getOperationTime() {
		return 8;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		tickcount++;
		read = Direction.DOWN; //getPowerBelow
		if (world.isClientSide)
			return;
		this.readPower();
		if (power < MINPOWER || tickcount < this.getOperationTime())
			return;
		int slot = this.firstFilledSlot();
		if (slot < 0)
			return;
		ItemCannonBlockEntity te = this.getTargetTE(world);
		if (te == null)
			return;
		ItemStack is = inv.getStackInSlot(slot);
		int num = power >= STACKPOWER ? is.getCount() : 1;
		ItemStack shot = is.copyWithCount(num);
		//original hasSpaceFor: the whole stack must fit
		if (!ItemHandlerHelper.insertItemStacked(te.inv, shot, true).isEmpty())
			return;
		tickcount = 0;
		this.fire(world, pos, shot);
		ItemStack left = ItemHandlerHelper.insertItemStacked(te.inv, shot, false);
		inv.extractItem(slot, num-left.getCount(), false);
	}

	private int firstFilledSlot() {
		for (int i = 0; i < inv.getSlots(); i++)
			if (!inv.getStackInSlot(i).isEmpty())
				return i;
		return -1;
	}

	@Nullable
	private ItemCannonBlockEntity getTargetTE(Level world) {
		if (target == null || targetDim == null || world.getServer() == null)
			return null;
		ServerLevel tw = world.getServer().getLevel(targetDim);
		if (tw == null || !tw.isLoaded(target))
			return null;
		return tw.getBlockEntity(target) instanceof ItemCannonBlockEntity te && te != this ? te : null;
	}

	/** The visible shot: an item flying at 4 m/tick towards the target that vanishes after 5 ticks; the transfer itself is instant. */
	private void fire(Level world, BlockPos pos, ItemStack is) {
		ItemEntity ei = new ItemEntity(world, pos.getX()+0.5, pos.getY()+1.125, pos.getZ()+0.5, is.copy());
		if (targetDim == world.dimension()) {
			double dx = target.getX()-pos.getX(), dy = target.getY()-pos.getY(), dz = target.getZ()-pos.getZ();
			double dd = Math.sqrt(dx*dx+dy*dy+dz*dz);
			if (dd > 0)
				ei.setDeltaMovement(dx/dd*4, dy/dd*4, dz/dd*4);
		}
		ei.setPickUpDelay(10);
		ei.lifespan = 5;
		world.addFreshEntity(ei);
		world.playSound(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1, 1);
	}

	public boolean isEmpty() {
		return this.firstFilledSlot() < 0;
	}

	@Override
	public int getRedstoneOverride() {
		return this.isEmpty() ? 15 : 0;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
		if (target != null && targetDim != null) {
			NBT.put("target", NbtUtils.writeBlockPos(target));
			NBT.putString("tdim", targetDim.location().toString());
		}
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
		target = NbtUtils.readBlockPos(NBT, "target").orElse(null);
		ResourceLocation dim = NBT.contains("tdim") ? ResourceLocation.tryParse(NBT.getString("tdim")) : null;
		targetDim = dim == null ? null : ResourceKey.create(Registries.DIMENSION, dim);
	}
}
