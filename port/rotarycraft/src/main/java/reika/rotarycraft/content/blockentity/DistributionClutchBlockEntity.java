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
import reika.rotarycraft.api.interfaces.Screwdriverable;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntityDistributionClutch - power from behind goes out at full speed; each
 * enabled side (north, south, west, east in that order, not the input) takes the torque requested for it from what is left, the rest goes
 * straight through (always enabled). Sides are enabled in the GUI, or (screwdriver while sneaking: control mode) by the strongest
 * redstone signal into it, bit 1 north, 2 east, 4 south, 8 west.
 * Not ported: bundled redstone and computer control (other mods' modes; never selectable without them, as in the original).
 */
public class DistributionClutchBlockEntity extends TransmissionMachineBlockEntity implements ComplexIO, Screwdriverable {

	public enum ControlMode {
		GUI,
		REDSTONE;
	}

	private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

	private int[] requestedTorques = new int[4];
	private int[] outputTorques = new int[4];
	private boolean[] enabledSides = new boolean[4];
	private ControlMode control = ControlMode.GUI;

	public DistributionClutchBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.DISTRIBUTION_CLUTCH.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	private static int idx(Direction dir) {
		return dir.ordinal()-2; //north 0, south 1, west 2, east 3 (ForgeDirection order)
	}

	@Override
	public boolean canProvidePower() {
		return true;
	}

	public boolean isOutputtingToSide(Direction dir) {
		if (dir == null || dir.getAxis() == Direction.Axis.Y || dir == read)
			return false;
		int i = idx(dir);
		return enabledSides[i] && (requestedTorques[i] > 0 || outputTorques[i] > 0);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.updateControl(world, pos);
		this.intakePower(world, pos);
		omega = omegain;
		torque = torquein;
		power = (long)omega*(long)torque;
		this.distributePower();
	}

	private void getIOSides(int meta) {
		read = switch (meta%4) {
			case 0 -> Direction.EAST;
			case 1 -> Direction.WEST;
			case 2 -> Direction.SOUTH;
			default -> Direction.NORTH;
		};
		write = read.getOpposite();
	}

	private void updateControl(Level world, BlockPos pos) {
		enabledSides[idx(write)] = true;
		enabledSides[idx(read)] = false;
		if (control == ControlMode.REDSTONE) {
			int redstone = world.getDirectSignalTo(pos); //getBlockPowerInput
			enabledSides[idx(Direction.NORTH)] = (redstone & 1) != 0;
			enabledSides[idx(Direction.EAST)] = (redstone & 2) != 0;
			enabledSides[idx(Direction.SOUTH)] = (redstone & 4) != 0;
			enabledSides[idx(Direction.WEST)] = (redstone & 8) != 0;
		}
	}

	private void distributePower() {
		int leftover = torquein;
		for (Direction dir : SIDES) {
			outputTorques[idx(dir)] = 0;
			if (this.isOutputtingToSide(dir)) {
				int amt = Math.min(requestedTorques[idx(dir)], leftover);
				leftover -= amt;
				outputTorques[idx(dir)] = amt;
			}
		}
		outputTorques[idx(write)] = leftover;
		for (Direction dir : SIDES)
			if (this.getAdjacentTileEntity(dir) != null)
				this.writeToPowerReceiver(dir, this.getSpeedToSide(dir), this.getTorqueToSide(dir));
	}

	private void intakePower(Level world, BlockPos pos) {
		omegain = torquein = 0;
		BlockEntity te = this.getAdjacentTileEntity(read);
		if (!this.isProvider(te)) {
			omega = torque = 0;
			return;
		}
		if (te instanceof ShaftBlockEntity devicein) {
			if (devicein.isCross()) {
				this.readFromCross(devicein);
				torquein = torque;
				omegain = omega;
				return;
			}
			if (devicein.isWritingTo(this)) {
				torquein = devicein.torque;
				omegain = devicein.omega;
			}
		}
		if (te instanceof ComplexIO pwr) {
			Direction dir = read.getOpposite();
			omegain = pwr.getSpeedToSide(dir);
			torquein = pwr.getTorqueToSide(dir);
		}
		if (te instanceof SimpleProvider)
			this.copyStandardPower(te);
		if (te instanceof ShaftPowerEmitter sp && sp.isEmitting() && sp.canWriteTo(read.getOpposite())) {
			torquein = sp.getTorque();
			omegain = sp.getOmega();
		}
		if (te instanceof SplitterBlockEntity devicein) {
			if (devicein.isSplitting()) {
				this.readFromSplitter(world, pos, devicein);
				torquein = torque;
				omegain = omega;
				return;
			}
			if (devicein.isWritingTo(this)) {
				torquein = devicein.torque;
				omegain = devicein.omega;
			}
		}
	}

	/** The input side or the always-on output side (from the metadata, also before the first tick). */
	public boolean isFixedSide(Direction dir) {
		this.getIOSides(this.getBlockMetadata());
		return dir == read || dir == write;
	}

	@Override
	public int getSpeedToSide(Direction dir) {
		return this.isOutputtingToSide(dir) ? omega : 0;
	}

	@Override
	public int getTorqueToSide(Direction dir) {
		return dir == null || dir.getAxis() == Direction.Axis.Y ? 0 : outputTorques[idx(dir)];
	}

	public void setSideEnabled(Direction dir, boolean flag) {
		enabledSides[idx(dir)] = flag;
		this.syncAllData();
	}

	public boolean isSideEnabled(Direction dir) {
		return enabledSides[idx(dir)];
	}

	public int getTorqueRequest(Direction dir) {
		return requestedTorques[idx(dir)];
	}

	public void setTorqueRequest(Direction dir, int amt) {
		requestedTorques[idx(dir)] = amt;
		this.syncAllData();
	}

	public void setTorqueRequests(int[] vals) {
		for (int i = 0; i < 4; i++)
			requestedTorques[i] = vals[i];
		this.syncAllData();
	}

	public int getInputTorque() {
		return torquein;
	}

	public ControlMode getControlMode() {
		return control;
	}

	/** Original stepMode (screwdriver while sneaking): the next control mode, all sides off. */
	public void stepMode() {
		control = ControlMode.values()[(control.ordinal()+1)%ControlMode.values().length];
		enabledSides = new boolean[4];
		this.syncAllData();
	}

	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		this.stepMode();
		return true;
	}

	/** Original ItemScrewdriver generic case: the next of the 4 directions. */
	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		int m = this.getBlockMetadata();
		this.setBlockMetadata(m < 3 ? m+1 : 0);
		this.syncAllData();
		return true;
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		return read == null ? new PowerSourceList() : PowerSourceList.getAllFrom(level, read, worldPosition.relative(read), this, caller);
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		if (dir == read)
			for (Direction d : SIDES)
				if (this.isOutputtingToSide(d))
					c.add(this.getAdjacentTileEntity(d));
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	private static int toBits(boolean[] b) {
		int r = 0;
		for (int i = 0; i < b.length; i++)
			if (b[i])
				r |= 1 << i;
		return r;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putIntArray("output", outputTorques);
		NBT.putIntArray("req", requestedTorques);
		NBT.putInt("sides", toBits(enabledSides));
		NBT.putInt("control", control.ordinal());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		int[] r = NBT.getIntArray("req"), o = NBT.getIntArray("output");
		requestedTorques = r.length == 4 ? r : new int[4];
		outputTorques = o.length == 4 ? o : new int[4];
		int bits = NBT.getInt("sides");
		for (int i = 0; i < 4; i++)
			enabledSides[i] = (bits & (1 << i)) != 0;
		control = ControlMode.values()[Math.min(NBT.getInt("control"), ControlMode.values().length-1)];
	}
}
