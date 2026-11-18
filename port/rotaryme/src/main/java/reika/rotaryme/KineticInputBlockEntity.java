/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;

import reika.rotarycraft.content.blockentity.PowerReceiverBlockEntity;

/**
 * ME kinetic input: shaft power into its back is stored in the ME network as kinetic energy, power/20 J a tick (lossless), up to the
 * configured speed and torque (more is cut off). What the network cannot take is lost. Needs a channel.
 */
public class KineticInputBlockEntity extends PowerReceiverBlockEntity implements IInWorldGridNodeHost {

	private final MEGridHost me = new MEGridHost(this, 2);
	private double carry;
	public long storedTotal;

	public KineticInputBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryME.KINETIC_INPUT_BE.get(), pos, state, 1, 1, 1);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	/** Power taken after the speed and torque caps. */
	public long getAcceptedPower() {
		long om = Math.min(omega, MEConfig.MAX_SPEED.get());
		long tq = Math.min(torque, MEConfig.MAX_TORQUE.get());
		return om*tq;
	}

	public boolean isConnected() {
		return me.isActive();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		me.tick(world, read);
		this.readPower();
		carry += this.getAcceptedPower()/20D;
		long j = (long)carry;
		if (j > 0) {
			long done = me.insert(KineticKey.INSTANCE, j, Actionable.MODULATE);
			storedTotal += done;
			carry -= j;
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
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		me.save(NBT);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		me.load(NBT);
	}
}
