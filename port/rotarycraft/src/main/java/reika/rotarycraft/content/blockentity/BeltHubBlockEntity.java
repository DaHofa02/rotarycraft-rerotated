/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.api.interfaces.Screwdriverable;
import reika.rotarycraft.api.power.PowerGenerator;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntityBeltHub - two hubs joined by a belt (right-click one, then the
 * other, with belts: one per block between them) pass power along a straight, clear line in the plane of their wheels. The driving hub
 * reads from its input side; the receiving end (screwdriver while sneaking) gives that power out of the same side. The belt slips above
 * 8192 Nm (torque capped) and 8192 rad/s (only the square root of the excess gets through); a wet belt (rain on it, one time in 900 per
 * tick; ~3 minutes, up to 15) grips a quarter as much. With metadata 6-11 (screwdriver) a hub splits: the driver passes half the torque
 * straight through, the receiver gives half to each side. Without a belt the receiving end stops at once.
 */
public class BeltHubBlockEntity extends PowerReceiverBlockEntity implements SimpleProvider, PowerGenerator, Screwdriverable {

	public boolean isReceivingEnd;
	private int wetTimer;
	private boolean isSlippingTorque;
	private boolean isSlippingOmega;
	@Nullable
	private BlockPos otherEnd;
	private int soundTick;

	public BeltHubBlockEntity(BlockPos pos, BlockState state) {
		this(RotaryBlockEntities.BELT_HUB.get(), pos, state);
	}

	protected BeltHubBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state, 1, 1, 1);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public boolean isSplitting() {
		return this.getBlockMetadata() >= 6;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata()%6);
		if (!world.isClientSide) {
			if (isReceivingEnd) {
				this.setReceiverIOSides();
				this.copyPowerFromDriver();
			}
			else {
				write = this.isSplitting() ? read.getOpposite() : null;
				write2 = null;
				this.readPower();
				if (this.isSplitting()) {
					torque = Math.max(0, this.getPropagatedTorque(torque));
					power = (long)omega*(long)torque;
				}
			}
			if (power > 0 && ++soundTick >= 26) {
				soundTick = 0;
				RotarySounds.play(world, pos, RotarySounds.BELT.get(), 0.6F, 1.0F);
			}
			if (world.isRainingAt(pos.above()) && world.random.nextInt(900) == 0)
				this.makeWet(1);
			if (wetTimer > 0 && power > 0)
				wetTimer--;
		}
		else {
			if (isReceivingEnd)
				this.setReceiverIOSides();
			else
				write = this.isSplitting() ? read.getOpposite() : null;
		}
	}

	protected int getPropagatedTorque(int input) {
		return input/2;
	}

	protected void setReceiverIOSides() {
		write = read;
		write2 = this.isSplitting() ? read.getOpposite() : null;
		read = null;
	}

	public final boolean areInSamePlane(BeltHubBlockEntity belt) {
		return (this.getBlockMetadata()%6)/2 == (belt.getBlockMetadata()%6)/2;
	}

	public final void reset() {
		otherEnd = null;
		this.syncAllData();
	}

	public final void resetOther() {
		if (otherEnd != null && level != null && level.getBlockEntity(otherEnd) instanceof BeltHubBlockEntity te && te.getType() == this.getType())
			te.reset();
	}

	private boolean canConnect(BlockPos c) {
		int dx = c.getX()-worldPosition.getX(), dy = c.getY()-worldPosition.getY(), dz = c.getZ()-worldPosition.getZ();
		if ((dx != 0 ? 1 : 0)+(dy != 0 ? 1 : 0)+(dz != 0 ? 1 : 0) != 1)
			return false;
		Direction dir = Direction.fromDelta(Integer.signum(dx), Integer.signum(dy), Integer.signum(dz));
		if (dir == null || !this.isValidDirection(dir))
			return false;
		if (!(level.getBlockEntity(c) instanceof BeltHubBlockEntity tb) || tb.isReceivingEnd == isReceivingEnd || !this.areInSamePlane(tb))
			return false;
		for (int i = 1; i < Math.abs(dx+dy+dz); i++) {
			BlockState s = level.getBlockState(worldPosition.relative(dir, i));
			if (!s.isAir() && !s.canBeReplaced()) //ReikaWorldHelper.softBlocks
				return false;
		}
		return true;
	}

	public final boolean hasValidConnection() {
		return otherEnd != null && level != null && level.getBlockEntity(otherEnd) instanceof BeltHubBlockEntity te && te.getType() == this.getType()
				&& this.canConnect(otherEnd);
	}

	public final boolean tryConnect(BlockPos c) {
		if (otherEnd != null || c.equals(worldPosition) || !this.canConnect(c))
			return false;
		otherEnd = c.immutable();
		this.syncAllData();
		return true;
	}

	public int getMaxTorque() {
		return 8192;
	}

	public int getMaxSmoothSpeed() {
		return 8192;
	}

	public int copyTorqueFromDriverSide(int input) {
		int max = this.isWet() ? this.getMaxTorque()/4 : this.getMaxTorque();
		isSlippingTorque = input > max;
		int t = Math.min(input, max);
		return this.isSplitting() ? t/2 : t;
	}

	public int copyOmegaFromDriverSide(int input) {
		int s = this.isWet() ? this.getMaxSmoothSpeed()/4 : this.getMaxSmoothSpeed();
		isSlippingOmega = input > s;
		return input <= s ? input : (int)(s+Math.sqrt(input-s));
	}

	private void copyPowerFromDriver() {
		boolean noInput = true;
		omega = torque = 0;
		power = 0;
		if (this.hasValidConnection()) {
			BeltHubBlockEntity tile = (BeltHubBlockEntity)level.getBlockEntity(otherEnd);
			omega = this.copyOmegaFromDriverSide(tile.omegain);
			torque = this.copyTorqueFromDriverSide(tile.torquein);
			power = (long)omega*(long)torque;
			noInput = false;
		}
		if (this.mergeReceivingPower())
			noInput = false;
		if (noInput) { //the original's spin-down (omega * 0.98) never runs: omega was zeroed above
			torque = 0;
			power = 0;
		}
	}

	protected boolean mergeReceivingPower() {
		return false;
	}

	/** From this hub towards the other end. */
	@Nullable
	public final Direction getBeltDirection() {
		if (otherEnd == null)
			return null;
		BlockPos d = otherEnd.subtract(worldPosition);
		return Direction.fromDelta(Integer.signum(d.getX()), Integer.signum(d.getY()), Integer.signum(d.getZ()));
	}

	public final int getDistanceToTarget() {
		return otherEnd == null ? -1 : otherEnd.distManhattan(worldPosition);
	}

	@Nullable
	public final BlockPos getConnection() {
		return otherEnd;
	}

	public final boolean isValidDirection(Direction dir) {
		return switch (this.getBlockMetadata()%6) {
			case 0, 1 -> dir.getStepX() == 0;
			case 2, 3 -> dir.getStepZ() == 0;
			default -> dir.getStepY() == 0;
		};
	}

	@Override
	public final boolean canProvidePower() {
		return isReceivingEnd || this.isSplitting();
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		if (isReceivingEnd) {
			if (otherEnd != null && level.getBlockEntity(otherEnd) instanceof BeltHubBlockEntity tile && tile != this && !tile.isReceivingEnd)
				return tile.getPowerSources(io, caller);
			return new PowerSourceList();
		}
		return read == null ? new PowerSourceList() : PowerSourceList.getAllFrom(level, read, worldPosition.relative(read), this, caller);
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		if (otherEnd != null && level.getBlockEntity(otherEnd) instanceof BeltHubBlockEntity belt) {
			c.add(belt.getWriteTileEntity());
			c.add(belt.getWriteTileEntity2());
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)reika.rotarycraft.lib.math.ReikaMathLibrary.doubpow(reika.rotarycraft.lib.math.ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public final long getMaxPower() {
		return this.isSplitting() ? power/2 : power;
	}

	@Override
	public final long getCurrentPower() {
		return this.getMaxPower();
	}

	/** Original getEmittingX/Y/Z: the block it gives power to. */
	@Override
	public BlockPos getEmittingPos() {
		return write == null ? worldPosition : worldPosition.relative(write);
	}

	public final boolean isWet() {
		return wetTimer > 0;
	}

	public final void makeWet(float fac) {
		wetTimer = Math.min(wetTimer+(int)(3600*fac), 18000);
	}

	/** Original Wettable.wet (the wetter's water): a quarter of rain. */
	public void wet() {
		this.makeWet(0.25F);
	}

	public boolean isSlipping() {
		return isSlippingOmega || isSlippingTorque;
	}

	public int getBeltColor() {
		return 0xC07846; //RGB 192, 120, 70
	}

	public ItemStack getBeltItem() {
		return new ItemStack(RotaryItems.part(CraftingPart.BELT));
	}

	/** Original breakBlock: the belt comes back (one per block between the hubs, at most a stack) and the other end lets go. */
	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		int num = Math.min(this.getDistanceToTarget()-1, this.getBeltItem().getMaxStackSize());
		if (!this.hasValidConnection())
			num = 0;
		for (int i = 0; i < num; i++)
			Containers.dropItemStack(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, this.getBeltItem());
		this.resetOther();
	}

	/** Original ItemScrewdriver: sneaking switches between driving and receiving end. */
	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, net.minecraft.core.Direction side) {
		isReceivingEnd = !isReceivingEnd;
		this.syncAllData();
		return true;
	}

	/** Original ItemScrewdriver: cycles through the 12 metadata (6 directions, then splitting) and drops the belt connection. */
	@Override
	public boolean onRightClick(Level world, BlockPos pos, net.minecraft.core.Direction side) {
		int m = this.getBlockMetadata();
		this.setBlockMetadata(m < 11 ? m+1 : 0);
		this.reset();
		this.resetOther();
		return true;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("emit", isReceivingEnd);
		if (otherEnd != null)
			NBT.putLong("endpoint", otherEnd.asLong());
		NBT.putInt("wet", wetTimer);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		isReceivingEnd = NBT.getBoolean("emit");
		otherEnd = NBT.contains("endpoint") ? BlockPos.of(NBT.getLong("endpoint")) : null;
		wetTimer = NBT.getInt("wet");
	}
}
