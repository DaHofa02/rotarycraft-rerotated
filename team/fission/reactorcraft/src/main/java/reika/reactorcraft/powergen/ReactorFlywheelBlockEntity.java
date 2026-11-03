/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.content.block.FlywheelMultiBlock;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.rotarycraft.api.interfaces.Screwdriverable;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.api.power.ShaftPowerReceiver;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: Reika.ReactorCraft.TileEntities.TileEntityReactorFlywheel - sits on the output side of a steam turbine (the block its
 * metadata faces: 0 east, 1 west, 2 south, 3 north) and passes the turbine's speed on as shaft power with the torque clamped to
 * 12750 Nm (25500 Nm with ammonia; high pressure turbines unclamped). Without a turbine it slows down by omega/32+1 per tick. Its
 * power goes every tick to a RotaryCraft power receiver on the opposite side (formed or not, as in the original); shafts read it only
 * while the 20-block flywheel ring is formed. Screwdriver: right-click turns it (formed: only reverses it).
 */
public class ReactorFlywheelBlockEntity extends RotaryCraftBlockEntity implements ShaftPowerEmitter, Screwdriverable {

	private static final int MAXTORQUE = 12750;
	private static final int MAXTORQUE_AMMONIA = MAXTORQUE*2;

	private int iotick;
	private long power;
	private int omega;
	private int torque;

	@Nullable
	private Direction facing;

	private boolean hasMultiBlock = false;

	public ReactorFlywheelBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.FLYWHEEL.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public boolean hasMultiBlock() {
		return hasMultiBlock;
	}

	public void setHasMultiBlock(boolean has) {
		hasMultiBlock = has;
		this.syncAllData();
	}

	public Direction getFacing() {
		return facing != null ? facing : Direction.EAST;
	}

	@Nullable
	private static Direction facingFromMeta(int meta) {
		return switch (meta) {
			case 0 -> Direction.EAST;
			case 1 -> Direction.WEST;
			case 2 -> Direction.SOUTH;
			case 3 -> Direction.NORTH;
			default -> null;
		};
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		facing = facingFromMeta(this.getBlockMetadata());
		if (world.getBlockEntity(pos.relative(this.getFacing())) instanceof TurbineCoreBlockEntity te) {
			omega = te.getOmega();
			torque = clampTorque(te);
		}
		else if (omega > 0) {
			omega -= (omega/32)+1;
		}
		if (omega <= 0) {
			torque = 0;
			omega = 0;
		}
		power = (long)omega*(long)torque;
		if (world.getBlockEntity(pos.relative(this.getFacing().getOpposite())) instanceof ShaftPowerReceiver rec) {
			rec.setOmega(this.getOmega());
			rec.setTorque(this.getTorque());
			rec.setPower(this.getPower());
		}
	}

	public static int clampTorque(TurbineCoreBlockEntity te) {
		return te instanceof HiPTurbineBlockEntity ? te.getTorque() : Math.min(te.getTorque(), te.isAmmonia() ? MAXTORQUE_AMMONIA : MAXTORQUE);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos.relative(this.getFacing())) instanceof TurbineCoreBlockEntity te)
			phi = te.phi*6;
		iotick -= 8;
	}

	@Override
	public int getOmega() {
		return omega;
	}

	@Override
	public int getTorque() {
		return torque;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public int getIORenderAlpha() {
		return iotick;
	}

	@Override
	public void setIORenderAlpha(int io) {
		iotick = io;
	}

	@Override
	public long getMaxPower() {
		return Long.MAX_VALUE;
	}

	@Override
	public long getCurrentPower() {
		return power;
	}

	@Override
	public BlockPos getEmittingPos() {
		return worldPosition.relative(this.getFacing().getOpposite());
	}

	@Override
	public boolean canWriteTo(Direction from) {
		return from == this.getFacing().getOpposite();
	}

	@Override
	public boolean isEmitting() {
		return this.hasMultiBlock();
	}

	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		return false;
	}

	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		int meta = this.getBlockMetadata();
		if (this.hasMultiBlock())
			this.setBlockMetadata((meta-meta%2)+(1-(meta%2)));
		else
			this.setBlockMetadata(meta < 3 ? meta+1 : 0);
		this.syncAllData();
		return true;
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		facing = NBT.contains("face") ? Direction.from3DDataValue(NBT.getInt("face")) : null;
		hasMultiBlock = NBT.getBoolean("multi");
		power = NBT.getLong("pwr");
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("face", this.getFacing().get3DDataValue());
		NBT.putBoolean("multi", hasMultiBlock);
		NBT.putLong("pwr", power);
	}

	/** Original breakBlock: un-forms the multiblock parts beside it. */
	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (Direction dir : Direction.values())
			FlywheelMultiBlock.breakMultiBlock(world, pos.relative(dir));
	}
}
