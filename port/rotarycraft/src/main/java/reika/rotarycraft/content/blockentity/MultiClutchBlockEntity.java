/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.api.interfaces.ComplexIO;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntityMultiClutch - takes power from the side its metadata names (0 east,
 * 1 west, 2 south, 3 north) and gives it out on the side set in its GUI for the redstone power level it gets (0-15): down, up, north,
 * south, west or east (all down at first).
 */
public class MultiClutchBlockEntity extends OneDTransmitterBlockEntity {

	private static final Direction[] SIDES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

	private int redLevel;
	private int[] control = new int[16];

	public MultiClutchBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.MULTI_CLUTCH.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Multi-Clutch";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		redLevel = world.getBestNeighborSignal(pos);
		this.updateSides(this.getBlockMetadata());
		this.transferPower(world, pos, this.getBlockMetadata());
		this.basicPowerReceiver();
	}

	private void updateSides(int meta) {
		switch (meta) {
			case 0 -> read = Direction.EAST;
			case 1 -> read = Direction.WEST;
			case 2 -> read = Direction.SOUTH;
			case 3 -> read = Direction.NORTH;
		}
		write = SIDES[control[redLevel]];
	}

	public int getRedstoneLevel() {
		return redLevel;
	}

	/** The side (0-5: down, up, north, south, west, east) used at redstone level {@code state}. */
	public int getSideOfState(int state) {
		return control[state];
	}

	public void setSideOfState(int state, int side) {
		if (state < 0 || state >= 16 || side < 0 || side >= 6)
			return;
		control[state] = side;
		this.syncAllData();
		this.setChanged();
	}

	public int getNextSideForState(int state) {
		return control[state] == 5 ? 0 : control[state]+1;
	}

	public static Direction side(int idx) {
		return SIDES[idx];
	}

	@Override
	protected void transferPower(Level world, BlockPos pos, int meta) {
		omegain = torquein = 0;
		BlockEntity te = this.getAdjacentTileEntity(read);
		{
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
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putIntArray("set", control);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		int[] set = NBT.getIntArray("set");
		control = set.length == 16 ? set : new int[16];
	}
}
