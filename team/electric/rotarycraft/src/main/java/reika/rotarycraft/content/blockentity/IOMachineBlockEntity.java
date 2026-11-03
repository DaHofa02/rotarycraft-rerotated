/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.api.IOMachine;
import reika.rotarycraft.api.power.AdvancedShaftPowerReceiver;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.api.power.ShaftPowerReceiver;
import reika.rotarycraft.api.power.SimpleShaftPowerReceiver;
import reika.rotarycraft.content.power.PowerSourceTracker;

/**
 * Original: Reika.RotaryCraft.Base.TileEntity.TileEntityIOMachine.
 * Power is pulled: every tick each machine reads torque and speed from the block on its read side and pushes them into the
 * receiver on its write side. The cross-dimension WorldRift (ChromatiCraft) handling and the shaft-power blacklist for foreign
 * mods are not ported, since neither mod exists for 1.21.1.
 */
public abstract class IOMachineBlockEntity extends RotaryCraftBlockEntity implements IOMachine, PowerSourceTracker {

	public int iotick = 512;

	@Nullable protected Direction write;
	@Nullable protected Direction write2;

	public long power = 0;
	public int torque = 0;
	public int omega = 0;

	@Nullable protected Direction read;
	@Nullable protected Direction read2;
	@Nullable protected Direction read3;
	@Nullable protected Direction read4;

	private BlockPos pointOffset = BlockPos.ZERO;

	public boolean isOmniSided = false;

	protected int torquein;
	protected int omegain;

	protected IOMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** Original: updateTileEntity(), called at the start of every subclass update. */
	public void updateTileEntity() {
		if (iotick > 0)
			iotick -= 8;
	}

	public abstract boolean canProvidePower();

	public boolean canTransmitPower() {
		return true;
	}

	public void onRotate() {

	}

	public final int getOmega() {
		return omega;
	}

	public final int getTorque() {
		return torque;
	}

	public final long getPower() {
		return power;
	}

	public final int getIORenderAlpha() {
		return iotick;
	}

	public final void setIORenderAlpha(int io) {
		iotick = io;
	}

	@Override
	public void onEMP() {
		super.onEMP();
		power = 0;
		torque = 0;
		omega = 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("torque", torque);
		NBT.putInt("omega", omega);
		NBT.putLong("power", power);
		NBT.putInt("io", iotick);

		NBT.putInt("read1", read != null ? read.ordinal() : -1);
		NBT.putInt("read2", read2 != null ? read2.ordinal() : -1);
		NBT.putInt("read3", read3 != null ? read3.ordinal() : -1);
		NBT.putInt("read4", read4 != null ? read4.ordinal() : -1);

		NBT.putInt("write1", write != null ? write.ordinal() : -1);
		NBT.putInt("write2", write2 != null ? write2.ordinal() : -1);

		NBT.putBoolean("omni", isOmniSided);

		NBT.putInt("pox", pointOffset.getX());
		NBT.putInt("poy", pointOffset.getY());
		NBT.putInt("poz", pointOffset.getZ());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		torque = NBT.getInt("torque");
		omega = NBT.getInt("omega");
		power = NBT.getLong("power");
		iotick = NBT.getInt("io");

		if (torque < 0)
			torque = 0;
		if (omega < 0)
			omega = 0;

		read = dir(NBT, "read1");
		read2 = dir(NBT, "read2");
		read3 = dir(NBT, "read3");
		read4 = dir(NBT, "read4");
		write = dir(NBT, "write1");
		write2 = dir(NBT, "write2");

		isOmniSided = NBT.getBoolean("omni");

		pointOffset = new BlockPos(NBT.getInt("pox"), NBT.getInt("poy"), NBT.getInt("poz"));
	}

	@Nullable
	private static Direction dir(CompoundTag NBT, String key) {
		if (!NBT.contains(key))
			return null;
		int i = NBT.getInt(key);
		return i >= 0 && i < 6 ? Direction.from3DDataValue(i) : null;
	}

	@Nullable public final Direction getReadDirection() { return read; }
	@Nullable public final Direction getReadDirection2() { return read2; }
	@Nullable public final Direction getReadDirection3() { return read3; }
	@Nullable public final Direction getReadDirection4() { return read4; }
	@Nullable public final Direction getWriteDirection() { return write; }
	@Nullable public final Direction getWriteDirection2() { return write2; }

	protected boolean isProvider(@Nullable BlockEntity te) {
		if (te instanceof ShaftPowerEmitter)
			return true;
		if (!(te instanceof IOMachineBlockEntity io))
			return false;
		return io.canProvidePower();
	}

	@Nullable public final BlockEntity getReadTileEntity() { return read != null ? this.getAdjacentTileEntity(read) : null; }
	@Nullable public final BlockEntity getReadTileEntity2() { return read2 != null ? this.getAdjacentTileEntity(read2) : null; }
	@Nullable public final BlockEntity getReadTileEntity3() { return read3 != null ? this.getAdjacentTileEntity(read3) : null; }
	@Nullable public final BlockEntity getReadTileEntity4() { return read4 != null ? this.getAdjacentTileEntity(read4) : null; }
	@Nullable public final BlockEntity getWriteTileEntity() { return write != null ? this.getAdjacentTileEntity(write) : null; }
	@Nullable public final BlockEntity getWriteTileEntity2() { return write2 != null ? this.getAdjacentTileEntity(write2) : null; }

	public final BlockPos getPointingOffset() {
		return pointOffset;
	}

	protected final void setPointingOffset(int x, int y, int z) {
		pointOffset = new BlockPos(x, y, z);
	}

	public final boolean isWritingToCoordinate(BlockPos pos) {
		return write != null && worldPosition.relative(write).equals(pos);
	}

	public final boolean isWritingToCoordinate2(BlockPos pos) {
		return write2 != null && worldPosition.relative(write2).equals(pos);
	}

	protected final boolean matchTile(PowerSourceTracker te, @Nullable Direction dir) {
		if (dir == null || te.getWorld() == null)
			return false;
		BlockPos target = te.getPos().offset(te.getIoOffset());
		BlockEntity out = this.getAdjacentTileEntity(dir);
		if (out == null)
			return false;
		return !out.isRemoved() && out.getLevel() != null && out.getLevel().dimension() == te.getWorld().dimension() && out.getBlockPos().equals(target);
	}

	public boolean isWritingTo(PowerSourceTracker te) {
		return this.matchTile(te, write);
	}

	public final boolean isWritingTo2(PowerSourceTracker te) {
		return this.matchTile(te, write2);
	}

	public final boolean isReadingFrom(PowerSourceTracker te) {
		return this.matchTile(te, read);
	}

	public final boolean isReadingFrom2(PowerSourceTracker te) {
		return this.matchTile(te, read2);
	}

	public final boolean isReadingFrom3(PowerSourceTracker te) {
		return this.matchTile(te, read3);
	}

	public final boolean isReadingFrom4(PowerSourceTracker te) {
		return this.matchTile(te, read4);
	}

	protected final void copyStandardPower(BlockEntity te) {
		this.copyStandardPower((IOMachineBlockEntity)te);
	}

	protected final void copyStandardPower(IOMachineBlockEntity te) {
		if (te instanceof ShaftBlockEntity)
			return;
		if (!te.isWritingTo(this) && !te.isWritingTo2(this)) {
			omegain = 0;
			torquein = 0;
			return;
		}
		torquein = te.torque;
		omegain = te.omega;
	}

	private void setPower(@Nullable BlockEntity te, Direction from, int om, int tq) {
		if (te instanceof SimpleShaftPowerReceiver sp) {
			sp.setPowered(sp.canReadFrom(from.getOpposite()) && tq > 0 && om > 0);
		}
		else if (te instanceof ShaftPowerReceiver sp) {
			if (sp.isReceiving() && sp.canReadFrom(from.getOpposite())) {
				sp.setOmega(om);
				sp.setTorque(tq);
				sp.setPower((long)om*(long)tq);
			}
			else {
				sp.setOmega(0);
				sp.setTorque(0);
				sp.setPower(0);
			}
		}
		else if (te instanceof AdvancedShaftPowerReceiver sp) {
			if (sp.canReadFrom(from.getOpposite())) {
				sp.addPower(tq, om, (long)tq*(long)om, from.getOpposite());
			}
		}
	}

	protected final void basicPowerReceiver() {
		this.writeToReceiver(write);
	}

	private void writeToReceiver(@Nullable Direction dir) {
		this.writeToReceiver(dir, omega, torque);
	}

	private void writeToReceiver(@Nullable Direction dir, int om, int tq) {
		if (dir == null)
			return;
		BlockEntity te = this.getAdjacentTileEntity(dir);
		this.setPower(te, dir, om, tq);
	}

	protected void writeToPowerReceiver(@Nullable Direction dir, int om, int tq) {
		this.writeToReceiver(dir, om, tq);
	}

	@Nullable
	public final Direction getInputForgeDirection() {
		return read;
	}

	@Override
	@Nullable
	public BlockPos getWritePos() {
		return write != null ? worldPosition.relative(write) : null;
	}

	@Override
	@Nullable
	public BlockPos getWritePos2() {
		return write2 != null ? worldPosition.relative(write2) : null;
	}

	@Override
	public final Level getWorld() {
		return level;
	}

	@Override
	public final BlockPos getPos() {
		return worldPosition;
	}

	@Override
	public final BlockPos getIoOffset() {
		return pointOffset;
	}
}
