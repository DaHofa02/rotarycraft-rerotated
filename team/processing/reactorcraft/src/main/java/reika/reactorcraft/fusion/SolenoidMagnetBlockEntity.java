/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.reactor.HydrogenExplosion;
import reika.reactorcraft.reactor.NeutronTile;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.rotarycraft.api.power.PowerTransferHelper;
import reika.rotarycraft.api.power.ShaftPowerReceiver;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fusion.TileEntitySolenoidMagnet - the central solenoid of the tokamak, powered from below
 * (at least 32768 Nm and 256 rad/s). While its multiblock is complete and powered it tells every magnet of the ring (starting 14 east,
 * 2 north of it and following their aims, over injectors, up to 39 parts) that it has a solenoid; without power or multiblock it takes
 * that back. Its coil speeds up to a speed set by omega (4.5 to 30, 512 above 8192 rad/s); spinning faster than 120 with full torque
 * it flies apart (power 16), and losing the multiblock above 32 rad/s blows it up (power 12).
 */
public class SolenoidMagnetBlockEntity extends RotaryCraftBlockEntity implements ShaftPowerReceiver, FusionMultiBlockTile, NeutronTile {

	public static final int MINOMEGA = 256;
	public static final int MAX_SPEED = 8192;
	public static final int MINTORQUE = 32768;
	private static final int MAX_SAFE_SPEED = 30;

	private boolean hasMultiBlock = false;
	private boolean checkForToroids = true;
	private boolean firstTick = true;

	private int torque;
	private int omega;
	private long power;
	private int iotick;
	private float speed = 0;

	public SolenoidMagnetBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.SOLENOID.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Solenoid Magnet";
	}

	@Override
	public boolean hasMultiBlock() {
		return hasMultiBlock;
	}

	@Override
	public void setHasMultiBlock(boolean has) {
		if (hasMultiBlock && !has)
			this.testBreakageFailure();
		hasMultiBlock = has;
		this.syncAllData();
	}

	private void testBreakageFailure() {
		if (omega > 32)
			this.fail(12);
	}

	private void fail(float power) {
		if (!(level instanceof ServerLevel sl))
			return;
		BlockPos p = worldPosition;
		sl.removeBlock(p, false);
		HydrogenExplosion.explode(sl, p, power);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (!world.isClientSide && !PowerTransferHelper.checkPowerFrom(this, Direction.DOWN))
			this.noInputMachine();

		float v = 0.1F;
		if (this.canTurn()) {
			if (speed > MAX_SAFE_SPEED)
				v *= 3;
			speed = Math.min(speed+v, this.getMaxRenderSpeed());
		}
		else {
			speed = Math.max(0, speed-v);
		}

		if (world.isClientSide)
			return;
		if (firstTick) {
			firstTick = false;
			if (!hasMultiBlock)
				this.checkForMultiBlock(world, pos);
		}
		if (hasMultiBlock && checkForToroids && this.arePowerReqsMet())
			this.addToToroids();
		if (!hasMultiBlock || !this.arePowerReqsMet())
			this.removeFromToroids();
		if (hasMultiBlock && torque >= MINTORQUE && speed > MAX_SAFE_SPEED*4) //violently fail
			this.fail(16);
	}

	private void checkForMultiBlock(Level world, BlockPos pos) {
		BlockPos b = pos.below();
		if (world.getBlockState(b).getBlock() instanceof FusionMultiBlock m && m.kind == FusionMultiBlock.Kind.SOLENOID) {
			if (FusionMultiBlock.checkSolenoid(world, b))
				FusionMultiBlock.formSolenoid(world, b);
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (hasMultiBlock)
			phi += speed;
		else
			phi = 0;
	}

	private float getMaxRenderSpeed() {
		if (omega > MAX_SPEED)
			return 512;
		else if (omega >= 4096)
			return MAX_SAFE_SPEED;
		else if (omega >= 2048)
			return 20F;
		else if (omega >= 1024)
			return 7.5F;
		else
			return 4.5F;
	}

	public float getSpeed() {
		return speed;
	}

	public boolean canTurn() {
		return hasMultiBlock && power > 0 && torque >= MINTORQUE;
	}

	public boolean arePowerReqsMet() {
		return omega >= MINOMEGA && this.canTurn();
	}

	private void walkToroids(boolean set) {
		int x = worldPosition.getX()+14; //radius of tokamak
		int y = worldPosition.getY();
		int z = worldPosition.getZ()-2;
		BlockEntity te = level.getBlockEntity(new BlockPos(x, y, z));
		int c = 0;
		ToroidMagnetBlockEntity.Aim a = ToroidMagnetBlockEntity.Aim.W;
		while ((te instanceof ToroidMagnetBlockEntity || te instanceof FusionInjectorBlockEntity) && (set ? c <= 38 : c < 38)) {
			if (te instanceof ToroidMagnetBlockEntity m) {
				m.hasSolenoid = set;
				a = m.getAim();
			}
			x += a.xOffset;
			z += a.zOffset;
			te = level.getBlockEntity(new BlockPos(x, y, z));
			c++;
		}
	}

	public void addToToroids() {
		this.walkToroids(true);
		checkForToroids = false;
	}

	public void removeFromToroids() {
		this.walkToroids(false);
		checkForToroids = true;
	}

	public boolean canRenderCoil() {
		return hasMultiBlock;
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
	public boolean canReadFrom(Direction dir) {
		return dir == Direction.DOWN;
	}

	@Override
	public boolean isReceiving() {
		return true;
	}

	@Override
	public void noInputMachine() {
		torque = omega = 0;
		power = 0;
	}

	@Override
	public int getMinTorque(int available) {
		return MINTORQUE;
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		return false;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		FusionMultiBlock.breakAround(world, pos);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("multi", hasMultiBlock);
		NBT.putInt("omg", omega);
		NBT.putInt("tq", torque);
		NBT.putLong("pwr", power);
		NBT.putFloat("phi", phi);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		hasMultiBlock = NBT.getBoolean("multi");
		omega = NBT.getInt("omg");
		torque = NBT.getInt("tq");
		power = NBT.getLong("pwr");
		phi = NBT.getFloat("phi");
	}
}
