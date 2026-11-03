/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.api.interfaces.TensionStorage;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.registry.DifficultyEffects;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.TileEntityWinder - holds one coil. Winding (input mode) it takes shaft power from its input
 * side and adds 1 kJ every (int)(log2(charge) / (int)log2(omega+1)) ticks, up to torque/stiffness kJ (at most 32000); a wind spring
 * may snap each step with charge/65536 * difficulty (1/5/15 %). Unwinding (output mode) it gives 1024*scale rad/s at 8*scale Nm out of
 * that side and loses 1 kJ every 20*stiffness ticks. The comparator gives 15 with no wind spring or a fully wound one. Coils go in from
 * any side and come out from below.
 */
public class WinderBlockEntity extends PowerReceiverBlockEntity implements SimpleProvider {

	/** Whether in wind or unwind mode. */
	public boolean winding = true;

	private int windTick;

	private final ItemStackHandler inv = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return is.is(RotaryItems.SPRING.get()) || is.is(RotaryItems.STRONGCOIL.get());
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		protected void onContentsChanged(int slot) {
			WinderBlockEntity.this.setChanged();
		}
	};

	public WinderBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.WINDER.get(), pos, state, 0, 0, 0);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	@Nullable
	private TensionStorage coil() {
		ItemStack is = inv.getStackInSlot(0);
		return !is.isEmpty() && is.getItem() instanceof TensionStorage ts ? ts : null;
	}

	public final int getUnwindTorque() {
		TensionStorage ts = this.coil();
		return ts == null ? 0 : 8*ts.getPowerScale(inv.getStackInSlot(0));
	}

	public final int getUnwindSpeed() {
		TensionStorage ts = this.coil();
		return ts == null ? 0 : 1024*ts.getPowerScale(inv.getStackInSlot(0));
	}

	@Override
	public boolean canProvidePower() {
		return !winding;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		if (winding)
			this.readPower();
		else {
			write = read;
			read = null;
		}
		windTick++;
		TensionStorage ts = this.coil();
		if (ts == null) {
			if (!winding) {
				torque = 0;
				omega = 0;
				power = 0;
			}
			return;
		}
		ItemStack is = inv.getStackInSlot(0);
		int charge = CoilItem.getCharge(is);
		if (winding) {
			if (windTick < this.getOperationTime())
				return;
			windTick = 0;
			if (charge >= this.getMaxWind())
				return;
			CoilItem.withCharge(is, charge+1);
			if (this.breakCoil(world, is)) {
				inv.setStackInSlot(0, ItemStack.EMPTY);
				world.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1F, 1F);
			}
			this.setChanged();
		}
		else {
			if (charge <= 0) {
				omega = 0;
				torque = 0;
				power = 0;
				return;
			}
			omega = this.getUnwindSpeed();
			torque = this.getUnwindTorque();
			power = (long)omega*(long)torque;
			if (windTick < this.getUnwindTime())
				return;
			windTick = 0;
			CoilItem.withCharge(is, charge-1);
			this.setChanged();
		}
	}

	protected final int getUnwindTime() {
		ItemStack is = inv.getStackInSlot(0);
		return 20*((TensionStorage)is.getItem()).getStiffness(is);
	}

	private boolean breakCoil(Level world, ItemStack is) {
		if (!(is.getItem() instanceof TensionStorage ts) || !ts.isBreakable(is))
			return false;
		double diff = CoilItem.getCharge(is)/65536D*DifficultyEffects.BREAKCOIL.getDouble();
		return world.random.nextDouble() < diff;
	}

	/** Original getOperationTime, with its double arithmetic (log2 of 0 kJ is minus infinity: no wait). */
	public int getOperationTime() {
		ItemStack is = inv.getStackInSlot(0);
		if (is.isEmpty())
			return 1;
		int base = (int)(Math.log(CoilItem.getCharge(is))/Math.log(2));
		double factor = 1D/(int)(Math.log(omega+1)/Math.log(2));
		return (int)(base*factor);
	}

	public int getMaxWind() {
		TensionStorage ts = this.coil();
		if (ts == null)
			return 0;
		int max = torque/ts.getStiffness(inv.getStackInSlot(0));
		return Math.min(max, CoilItem.MAX_CHARGE);
	}

	@Override
	public int getRedstoneOverride() {
		ItemStack is = inv.getStackInSlot(0);
		if (is.isEmpty() || !is.is(RotaryItems.SPRING.get()))
			return 15;
		if (CoilItem.getCharge(is) >= torque && winding)
			return 15;
		return 0;
	}

	public void toggleMode() {
		winding = !winding;
		iotick = 512;
		this.syncAllData();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (!isInWorld())
			phi = 0;
		else
			phi += (float)Math.pow(Math.log(omega+1)/Math.log(2), 1.05);
	}

	private boolean isInWorld() {
		return level != null;
	}

	/** Coils in from any side, out only downwards (original canExtractItem side 0). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 1;
			}

			@Override
			public ItemStack getStackInSlot(int slot) {
				return inv.getStackInSlot(slot);
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return inv.insertItem(slot, stack, simulate);
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return side == Direction.DOWN ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return 1;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return inv.isItemValid(slot, stack);
			}
		};
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(0).copy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("winding", winding);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		winding = !NBT.contains("winding") || NBT.getBoolean("winding");
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
