/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.blockentity;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.electricraft.network.WireEmitter;
import reika.electricraft.network.WireNetwork;
import reika.electricraft.registry.ElectriBlockEntities;
import reika.rotarycraft.api.IOMachine;
import reika.rotarycraft.api.interfaces.ComplexIO;
import reika.rotarycraft.api.power.PowerGenerator;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.api.power.ShaftPowerReceiver;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.lib.math.ReikaMathLibrary;

/**
 * Original: Reika.ElectriCraft.TileEntities.TileEntityGenerator - takes shaft power on its facing side and feeds the wire network on the
 * opposite side with voltage = 8·omega and current = torque/8 (WireNetwork.TORQUE_PER_AMP). Above 400 MW it explodes. The screwdriver
 * turns it to the next horizontal side.
 */
public class GeneratorBlockEntity extends NetworkBlockEntity implements WireEmitter, ShaftPowerReceiver, PowerSourceTracker {

	private int lastomega;
	private int lasttorque;
	protected int omega;
	protected int torque;
	protected long power;
	protected int iotick;
	@Nullable
	private Direction facing;

	public GeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ElectriBlockEntities.GENERATOR.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Electric Generator";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		super.updateEntity(world, pos, state);
		if (iotick > 0)
			iotick -= 8;
		if (world.isClientSide)
			return;
		if (!checkPowerFrom(this, this.getFacing()))
			this.noInputMachine();
		if (power == 0 || omega == 0 || torque == 0) {
			power = 0;
			omega = torque = 0;
		}
		if (power > 400e6) {
			world.removeBlock(pos, false);
			world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 4, true, Level.ExplosionInteraction.BLOCK);
			return;
		}
		if (network != null && (omega != lastomega || torque != lasttorque))
			network.updateWires();
		lastomega = omega;
		lasttorque = torque;
	}

	/** Original RotaryCraft API PowerTransferHelper.checkPowerFrom. */
	public static boolean checkPowerFrom(BlockEntity tile, Direction dir) {
		return reika.rotarycraft.api.power.PowerTransferHelper.checkPowerFrom(tile, dir);
	}

	public final Direction getFacing() {
		return facing != null ? facing : Direction.EAST;
	}

	public void setFacing(Direction dir) {
		facing = dir;
		this.setChanged();
	}

	/** Original incrementFacing (screwdriver): the next of the four horizontal sides. */
	public void incrementFacing() {
		int o = this.getFacing().ordinal();
		this.setFacing(o == 5 ? Direction.values()[2] : Direction.values()[o+1]);
		this.rebuildNetwork();
		this.syncAllData();
	}

	@Override
	public final int getOmega() {
		return omega;
	}

	@Override
	public final int getTorque() {
		return torque;
	}

	@Override
	public final long getPower() {
		return power;
	}

	@Override
	public final int getIORenderAlpha() {
		return iotick;
	}

	@Override
	public final void setIORenderAlpha(int io) {
		iotick = io;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getGenVoltage() {
		return omega*WireNetwork.TORQUE_PER_AMP;
	}

	@Override
	public int getGenCurrent() {
		return torque/WireNetwork.TORQUE_PER_AMP;
	}

	@Override
	public boolean canNetworkOnSide(Direction dir) {
		return dir == this.getFacing().getOpposite();
	}

	@Override
	public void setOmega(int omega) {
		this.omega = omega;
	}

	@Override
	public void setTorque(int torque) {
		this.torque = torque;
	}

	@Override
	public void setPower(long power) {
		this.power = power;
	}

	@Override
	public boolean canReadFrom(Direction from) {
		return from == this.getFacing();
	}

	@Override
	public boolean isReceiving() {
		return true;
	}

	@Override
	public void noInputMachine() {
		omega = torque = 0;
		power = 0;
	}

	@Override
	public boolean canEmitPowerToSide(Direction dir) {
		return this.canNetworkOnSide(dir);
	}

	@Override
	public boolean canEmitPower() {
		return true;
	}

	@Override
	public int getMinTorque(int available) {
		return torque;
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		Direction dir = this.getFacing();
		return PowerSourceList.getAllFrom(level, dir, worldPosition.relative(dir), this, caller);
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {}

	@Override
	public BlockPos getIoOffset() {
		return BlockPos.ZERO;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("face", this.getFacing().ordinal());
		NBT.putInt("omg", omega);
		NBT.putInt("tq", torque);
		NBT.putLong("pwr", power);
		NBT.putInt("io", iotick);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		facing = NBT.contains("face") ? Direction.values()[NBT.getInt("face")] : null;
		omega = NBT.getInt("omg");
		torque = NBT.getInt("tq");
		power = NBT.getLong("pwr");
		iotick = NBT.getInt("io");
	}
}
