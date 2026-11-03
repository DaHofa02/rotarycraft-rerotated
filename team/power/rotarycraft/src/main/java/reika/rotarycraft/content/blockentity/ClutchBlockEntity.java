/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.api.interfaces.ComplexIO;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntityClutch. Passes power only while its redstone state matches. */
public class ClutchBlockEntity extends OneDTransmitterBlockEntity {

	public boolean needsRedstone = true;

	public ClutchBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.CLUTCH.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Clutch";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();

		this.getIOSides(world, pos, this.getBlockMetadata(), true);
		this.transferPower(world, pos, this.getBlockMetadata());
		power = (long)omega*(long)torque;

		this.basicPowerReceiver();
	}

	@Override
	protected void transferPower(Level world, BlockPos pos, int meta) {
		omegain = torquein = 0;
		BlockEntity te = this.getAdjacentTileEntity(read);
		if (this.isOutputEnabled()) {
			if (this.isProvider(te)) {
				if (te instanceof ShaftBlockEntity devicein) {
					if (devicein.isCross()) {
						this.readFromCross(devicein);
						return;
					}
					else if (devicein.isWritingTo(this)) {
						torquein = devicein.torque;
						omegain = devicein.omega;
					}
				}
				if (te instanceof SimpleProvider) {
					this.copyStandardPower(te);
				}
				if (te instanceof ComplexIO pwr) {
					Direction dir = this.getInputForgeDirection().getOpposite();
					omegain = pwr.getSpeedToSide(dir);
					torquein = pwr.getTorqueToSide(dir);
				}
				if (te instanceof ShaftPowerEmitter sp) {
					if (sp.isEmitting() && sp.canWriteTo(read.getOpposite())) {
						torquein = sp.getTorque();
						omegain = sp.getOmega();
					}
				}
				if (te instanceof SplitterBlockEntity devicein) {
					if (devicein.isSplitting()) {
						this.readFromSplitter(world, pos, devicein);
						torquein = torque;
						omegain = omega;
						return;
					}
					else if (devicein.isWritingTo(this)) {
						torquein = devicein.torque;
						omegain = devicein.omega;
					}
				}
				omega = omegain;
				torque = torquein;
			}
			else {
				omega = 0;
				torque = 0;
				power = 0;
			}
		}
		else {
			omega = torque = 0;
		}
	}

	public boolean hasRedstoneSignal() {
		return level != null && level.hasNeighborSignal(worldPosition);
	}

	public boolean isOutputEnabled() {
		return this.hasRedstoneSignal() == needsRedstone;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("redstone", needsRedstone);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		needsRedstone = !NBT.contains("redstone") || NBT.getBoolean("redstone");
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		return this.isOutputEnabled() ? super.getPowerSources(io, caller) : new PowerSourceList();
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		if (this.isOutputEnabled())
			super.getAllOutputs(c, dir);
	}
}
