/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.stacks.AEItemKey;

import reika.rotarycraft.content.blockentity.PowerReceiverBlockEntity;
import reika.rotaryme.compat.ProjectRedCompat;

/**
 * The bundled bus, written for the port from RotaryCraft's original behaviour: 16 filter slots, one per dye colour. With shaft power into its
 * back (1024 W per active channel, up to 16) it checks every 10 ticks which colours of a Project Red bundled cable next to it are on, and for
 * each such colour with a filter moves up to a stack of the filter's item out of the ME network into the inventory in front of it. Needs a
 * channel. Without Project Red Transmission nothing switches it, the rest works.
 */
public class BundledBusBlockEntity extends PowerReceiverBlockEntity implements IInWorldGridNodeHost {

	public static final int CHANNELS = 16;
	public static final long POWER_PER_CHANNEL = 1024;
	/** Bundled input for tests (bit i = colour i on); null = read Project Red. */
	@Nullable
	public static Integer testInput;

	private final MEGridHost me = new MEGridHost(this, 1);
	private final ItemStackHandler filters = new ItemStackHandler(CHANNELS) {
		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		protected void onContentsChanged(int slot) {
			BundledBusBlockEntity.this.setChanged();
		}
	};
	public long moved;

	public BundledBusBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryME.BUNDLED_BUS_BE.get(), pos, state, 1, 1, POWER_PER_CHANNEL);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getFilters() {
		return filters;
	}

	public int getActiveChannels() {
		return (int)Math.min(CHANNELS, power/POWER_PER_CHANNEL);
	}

	public Direction getOutputSide() {
		return read == null ? Direction.WEST : read.getOpposite();
	}

	private int[] input(Level world, BlockPos pos) {
		if (testInput != null) {
			int[] ret = new int[CHANNELS];
			for (int i = 0; i < CHANNELS; i++)
				ret[i] = (testInput >> i & 1) != 0 ? 255 : 0;
			return ret;
		}
		return reika.rotaryme.compat.OptionalMods.projectRed() ? ProjectRedCompat.getBundledInput(world, pos) : new int[CHANNELS];
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		me.tick(world, read);
		this.readPower();
		if (power < MINPOWER || this.getTicksExisted()%10 != 0 || !me.isActive())
			return;
		Direction out = this.getOutputSide();
		IItemHandler target = world.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(out), out.getOpposite());
		if (target == null)
			return;
		int[] in = this.input(world, pos);
		for (int i = 0; i < this.getActiveChannels(); i++) {
			ItemStack f = filters.getStackInSlot(i);
			if (f.isEmpty() || in[i] <= 0)
				continue;
			AEItemKey key = AEItemKey.of(f);
			ItemStack want = f.copyWithCount(f.getMaxStackSize());
			int fits = want.getCount()-ItemHandlerHelper.insertItemStacked(target, want, true).getCount();
			long got = fits <= 0 ? 0 : me.extract(key, fits, Actionable.MODULATE);
			if (got > 0) {
				ItemStack rest = ItemHandlerHelper.insertItemStacked(target, f.copyWithCount((int)got), false);
				if (!rest.isEmpty()) //should not happen (simulated first): give it back
					me.insert(AEItemKey.of(rest), rest.getCount(), Actionable.MODULATE);
				moved += got-rest.getCount();
			}
		}
	}

	@Nullable
	@Override
	public IGridNode getGridNode(Direction dir) {
		return me.getNode(dir);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		me.destroy();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		me.destroy();
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < CHANNELS; i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), filters.getStackInSlot(i).copy());
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		me.save(NBT);
		NBT.put("filters", filters.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		me.load(NBT);
		filters.deserializeNBT(registries, NBT.getCompound("filters"));
	}
}
