/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import reika.rotarycraft.content.item.EngineUpgradeItem;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The dynamo, written for the port from the original's behaviour: takes shaft power at its back and pushes Forge Energy (FE, which replaces
 * RF) into the energy receiver in front of it, min(torque, 1024 Nm)*min(speed, 8192 rad/s)/520 FE per tick; the flux upgrade raises the
 * torque cap to 2048 Nm. It stores nothing: what the receiver does not take is lost.
 */
public class DynamoBlockEntity extends PowerReceiverBlockEntity implements UpgradeableMachine {

	public static final int MAXTORQUE = 1024;
	public static final int MAXTORQUE_UPGRADE = 2048;
	public static final int MAXOMEGA = 8192;

	private boolean upgraded;
	public long delivered;

	public DynamoBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.DYNAMO.get(), pos, state, 1, 1, 1);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public boolean isUpgraded() {
		return upgraded;
	}

	public Direction getOutputSide() {
		return read == null ? Direction.WEST : read.getOpposite();
	}

	/** FE per tick from the current input. */
	public int getGeneratedFE() {
		long tq = Math.min(torque, upgraded ? MAXTORQUE_UPGRADE : MAXTORQUE);
		long om = Math.min(omega, MAXOMEGA);
		return (int)(tq*om/MagneticEngineBlockEntity.WATTS_PER_FE);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		int fe = this.getGeneratedFE();
		if (fe <= 0)
			return;
		Direction out = this.getOutputSide();
		IEnergyStorage target = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(out), out.getOpposite());
		if (target != null && target.canReceive())
			delivered += target.receiveEnergy(fe, false);
	}

	/** Shown to cables and the like as a source that cannot be drawn from (it pushes by itself). */
	@Nullable
	public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
		if (side != null && side != this.getOutputSide())
			return null;
		return new IEnergyStorage() {
			@Override
			public int receiveEnergy(int max, boolean simulate) {
				return 0;
			}

			@Override
			public int extractEnergy(int max, boolean simulate) {
				return 0;
			}

			@Override
			public int getEnergyStored() {
				return 0;
			}

			@Override
			public int getMaxEnergyStored() {
				return 0;
			}

			@Override
			public boolean canExtract() {
				return true;
			}

			@Override
			public boolean canReceive() {
				return false;
			}
		};
	}

	@Override
	public boolean canUpgradeWith(ItemStack is) {
		return !upgraded && EngineUpgradeItem.is(is, EngineUpgradeItem.Upgrades.FLUX);
	}

	@Override
	public void upgrade(ItemStack is) {
		upgraded = true;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("upgrade", upgraded);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		upgraded = NBT.getBoolean("upgrade");
	}
}
