/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.ReactorConfig;
import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.reactor.NeutronTile;
import reika.reactorcraft.reactor.ReactorChunkLoading;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.rotarycraft.api.interfaces.Screwdriverable;
import reika.rotarycraft.api.interfaces.Shockable;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.entity.DischargeEntity;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fusion.TileEntityToroidMagnet - one of the 36 magnets of the tokamak ring. It points (its
 * {@link Aim}, 32 steps; screwdriver right-click turns it on, shift right-click back, and shows the aim line) at the next part of the
 * ring 2 blocks away. Plasma passing through it is steered towards that point when the magnet may affect it: the ring must be closed
 * (every part's next part leads back here within 60 steps), a working solenoid must have reached it, it must hold more than 2500 C and
 * some liquid nitrogen (10 mB used per plasma per tick), and the plasma must come from a magnet at most 2 steps behind (fresh plasma
 * from an injector only by a cardinal magnet). Its charge comes from Van de Graaff discharges (at least 8192 C, also from 2-3 blocks
 * below) and is passed on: every few ticks (option "Toroid Spark Delay", 4-20) a quarter of the difference goes to the next magnet
 * (through an injector to the one behind it), with sparks; every second it loses 20 %. Liquid nitrogen comes from a pipe two blocks
 * above or any fluid input. While plasma passes it keeps 5x5 chunks loaded (option "chunkloading").
 */
public class ToroidMagnetBlockEntity extends RotaryCraftBlockEntity implements Screwdriverable, Shockable, FusionReactorToroidPart, NeutronTile {

	public static final int CAPACITY = 8000;
	public static final int MIN_CHARGE = 2500;

	//0 is +x(E), rotates to -z(N)
	private Aim aim = Aim.N;

	private int alpha = 512;

	protected boolean hasSolenoid = false;

	private int charge = 0;

	private int chargeTimer;
	private int reCheckTimer;

	private final FluidTank tank = new FluidTank(CAPACITY, fs -> fs.is(RotaryFluids.LIQUID_NITROGEN.get()));

	private boolean hasNext;
	private boolean firstTick = true;

	private boolean isActive;
	private int lastPlasma;

	public ToroidMagnetBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.TOROID_MAGNET.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Toroid Magnet";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (alpha > 0)
			alpha -= 8;
		if (world.isClientSide)
			return;
		if (firstTick) {
			firstTick = false;
			if (!hasSolenoid)
				this.checkSurroundingMagnetsAndCopySolenoidState();
			hasNext = this.checkCompleteness(world);
		}

		List<PlasmaEntity> li = world.getEntitiesOfClass(PlasmaEntity.class, new AABB(pos));
		BlockPos tg = this.getTarget();
		for (PlasmaEntity e : li) {
			if (this.canAffect(e)) {
				e.resetEscapeTimer();
				e.setTarget(tg.getX(), tg.getZ());
				e.magnetOrdinal = this.getOrdinal();
				tank.drain(10, IFluidHandler.FluidAction.EXECUTE);
				this.setActive();
			}
		}

		if (world.getBlockEntity(pos.above(2)) instanceof PipingBlockEntity te && te.getPipeType().isStandardPipe()) {
			int amt = Math.min(CAPACITY-tank.getFluidAmount(), te.getFluidLevel());
			if (amt > 0 && te.getFluidType() != null && te.getFluidType().isSame(RotaryFluids.LIQUID_NITROGEN.get())) {
				tank.fill(new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), amt), IFluidHandler.FluidAction.EXECUTE);
				te.removeLiquid(amt);
			}
		}

		chargeTimer++;
		if (chargeTimer%ReactorConfig.getToroidChargeRate() == 0)
			this.distributeCharge(world, pos);
		if (chargeTimer >= 20) {
			chargeTimer = 0;
			this.updateCharge();
		}

		if (hasSolenoid) {
			reCheckTimer++;
			if (reCheckTimer >= 20) {
				reCheckTimer = 0;
				hasNext = this.checkCompleteness(world);
			}
		}

		if (lastPlasma > 0) {
			lastPlasma--;
			if (lastPlasma == 0)
				this.setInactive();
		}
	}

	private void setActive() {
		boolean last = isActive;
		isActive = true;
		lastPlasma = 20;
		if (!last) {
			this.syncAllData();
			if (ReactorConfig.CHUNKLOADING.get() && level instanceof ServerLevel sl)
				ReactorChunkLoading.setLoaded(sl, worldPosition, true, 2);
		}
	}

	private void setInactive() {
		boolean last = isActive;
		isActive = false;
		if (last) {
			this.syncAllData();
			if (level instanceof ServerLevel sl)
				ReactorChunkLoading.setLoaded(sl, worldPosition, false, 2);
		}
	}

	private boolean checkCompleteness(Level world) {
		FusionReactorToroidPart te = this.getNextPart(world, worldPosition);
		int i = 60;
		while (te != null && te != this && i >= 0) {
			te = te.getNextPart(world, worldPosition);
			i--;
		}
		if (te != this && te instanceof ToroidMagnetBlockEntity m)
			m.hasNext = false;
		return te == this;
	}

	@Override
	@Nullable
	public FusionReactorToroidPart getNextPart(Level world, BlockPos pos) {
		Aim a = this.getAim();
		BlockEntity te = world.getBlockEntity(worldPosition.offset(a.xOffset, 0, a.zOffset));
		return te instanceof FusionReactorToroidPart p ? p : null;
	}

	public int getCoolant() {
		return tank.getFluidAmount();
	}

	public int getCharge() {
		return charge;
	}

	public void setCharge(int c) {
		charge = c;
	}

	public boolean hasSolenoid() {
		return hasSolenoid;
	}

	public boolean hasNext() {
		return hasNext;
	}

	private void checkSurroundingMagnetsAndCopySolenoidState() {
		Aim a = this.getAim();
		BlockPos d = worldPosition.offset(a.xOffset, 0, a.zOffset);
		BlockEntity be = level.getBlockEntity(d);
		if (be instanceof ToroidMagnetBlockEntity te) {
			hasSolenoid = te.hasSolenoid;
			te.checkCompleteness(level);
		}
		else if (be instanceof FusionInjectorBlockEntity) {
			d = d.offset(a.xOffset, 0, a.zOffset);
			BlockEntity te = level.getBlockEntity(d);
			while (te instanceof FusionInjectorBlockEntity) {
				d = d.offset(a.xOffset, 0, a.zOffset);
				te = level.getBlockEntity(d);
			}
			if (te instanceof ToroidMagnetBlockEntity m)
				hasSolenoid = m.hasSolenoid;
		}
	}

	private void distributeCharge(Level world, BlockPos pos) {
		Aim a = this.getAim();
		BlockPos d = pos.offset(a.xOffset, 0, a.zOffset);
		BlockEntity be = world.getBlockEntity(d);
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		if (be instanceof ToroidMagnetBlockEntity te) {
			int dC = charge-te.charge;
			if (dC > 0) {
				te.charge += dC/4;
				charge -= dC/4;
				BlockPos t = te.worldPosition;
				float ang = this.getAngle();
				float ang2 = te.getAngle();
				double fx = 1.75*Math.sin(Math.toRadians(ang));
				double fz = 1.75*Math.cos(Math.toRadians(ang));
				double fx2 = 1.75*Math.sin(Math.toRadians(ang2));
				double fz2 = 1.75*Math.cos(Math.toRadians(ang2));
				if (this.shouldSpawnSparks()) {
					world.addFreshEntity(new DischargeEntity(world, x+0.5, y+2.25, z+0.5, charge, t.getX()+0.5, t.getY()+2.25, t.getZ()+0.5));
					world.addFreshEntity(new DischargeEntity(world, x+0.5, y-1.25, z+0.5, charge, t.getX()+0.5, t.getY()-1.25, t.getZ()+0.5));
					world.addFreshEntity(new DischargeEntity(world, x+0.5+fx, y+0.5, z+0.5+fz, charge, t.getX()+0.5+fx2, t.getY()+0.5, t.getZ()+0.5+fz2));
					world.addFreshEntity(new DischargeEntity(world, x+0.5-fx, y+0.5, z+0.5-fz, charge, t.getX()+0.5-fx2, t.getY()+0.5, t.getZ()+0.5-fz2));
				}
			}
		}
		else if (be instanceof FusionInjectorBlockEntity) {
			if (world.getBlockEntity(d.offset(a.xOffset, 0, a.zOffset)) instanceof ToroidMagnetBlockEntity te) {
				int dC = charge-te.charge;
				if (dC > 0) {
					te.charge += dC/4;
					charge -= dC/4;
					BlockPos t = te.worldPosition;
					if (this.shouldSpawnSparks()) {
						world.addFreshEntity(new DischargeEntity(world, x+0.5, y+2, z+0.5, charge, t.getX()+0.5, t.getY()+2, t.getZ()+0.5));
						world.addFreshEntity(new DischargeEntity(world, x+0.5, y-1, z+0.5, charge, t.getX()+0.5, t.getY()-1, t.getZ()+0.5));
					}
				}
			}
		}
	}

	private boolean shouldSpawnSparks() {
		return rand.nextBoolean();
	}

	private void updateCharge() {
		if (charge <= 1)
			charge = 0;
		else
			charge *= 0.8;
	}

	private boolean canAffect(PlasmaEntity e) {
		if (!hasNext)
			return false;
		if (!hasSolenoid)
			return false;
		if (charge <= MIN_CHARGE)
			return false;
		if (tank.isEmpty())
			return false;
		int o = this.getOrdinal();
		int p = e.magnetOrdinal;
		if (p == -1)
			return o%8 == 0;
		if (o > 30)
			return p > 28 || p < 1;
		if (p > 30)
			return o > 28 || o < 1;
		return Math.abs(p-o) <= 2;
	}

	public BlockPos getTarget() {
		return worldPosition.offset(this.getAim().xOffset, 0, this.getAim().zOffset);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	public Aim getAim() {
		return aim != null ? aim : Aim.N;
	}

	public void setAim(Aim a) {
		aim = a != null ? a : aim;
		this.setChanged();
	}

	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		alpha = 512;
		this.decrementAim();
		return true;
	}

	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		this.refreshAlpha();
		this.incrementAim();
		return true;
	}

	public float getAngle() {
		return this.getAim().angle;
	}

	public int getOrdinal() {
		return this.getAim().ordinal();
	}

	public void refreshAlpha() {
		alpha = 512;
	}

	public int getAlpha() {
		return alpha;
	}

	private void incrementAim() {
		aim = this.getAim().getNext();
		if (!hasSolenoid)
			this.checkSurroundingMagnetsAndCopySolenoidState();
		this.syncAllData();
	}

	private void decrementAim() {
		aim = this.getAim().getPrev();
		if (!hasSolenoid)
			this.checkSurroundingMagnetsAndCopySolenoidState();
		this.syncAllData();
	}

	public boolean isActive() {
		return isActive;
	}

	public FluidTank getTank() {
		return tank;
	}

	@Override
	public void onDischarge(int charge, double range) {
		this.charge += charge;
	}

	@Override
	public int getMinDischarge() {
		return 8192;
	}

	@Override
	public float getAimX() {
		return 0.5F;
	}

	@Override
	public float getAimY() {
		return -1.25F;
	}

	@Override
	public float getAimZ() {
		return 0.5F;
	}

	@Override
	public boolean canDischargeLongRange() {
		return true;
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		return false;
	}

	@Override
	public void setRemoved() {
		if (level instanceof ServerLevel sl && isActive)
			ReactorChunkLoading.setLoaded(sl, worldPosition, false, 2);
		super.setRemoved();
	}

	/** Original NonIFluidTank.addFluid: liquid nitrogen in from anywhere, nothing out. */
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 1;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return tank.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return tank.isFluidValid(stack);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				int n = tank.fill(resource, action);
				if (n > 0 && action.execute())
					ToroidMagnetBlockEntity.this.setChanged();
				return n;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return FluidStack.EMPTY;
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("aim", this.getAim().ordinal());
		NBT.putBoolean("solenoid", hasSolenoid);
		NBT.putInt("alp", alpha);
		NBT.putInt("chg", charge);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.putBoolean("next", hasNext);
		NBT.putBoolean("active", isActive);
		NBT.putInt("lastplasma", lastPlasma);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		int o = NBT.getInt("aim");
		aim = o > 0 && o < Aim.list.length ? Aim.list[o] : Aim.N;
		hasSolenoid = NBT.getBoolean("solenoid");
		charge = NBT.getInt("chg");
		alpha = NBT.getInt("alp");
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		hasNext = NBT.getBoolean("next");
		isActive = NBT.getBoolean("active");
		lastPlasma = NBT.getInt("lastplasma");
	}

	/** Original TileEntityToroidMagnet.Aim: the 32 aim steps (angle for the model, offset of the next ring part). */
	public enum Aim {
		N(0,			2, 0),
		NNW1(11.3F,		2, -1),
		NNW2(24,		2, -1),
		NNW3(36.9F,		2, -2),
		NW(45,			2, -2),
		WNW1(53.1F,		1, -2),
		WNW2(66,		1, -2),
		WNW3(78.7F,		0, -2),
		W(90,			0, -2),
		WSW1(101.3F,	-1, -2),
		WSW2(114,		-1, -2),
		WSW3(126.9F,	-2, -2),
		SW(135,			-2, -2),
		SSW1(143.1F,	-2, -1),
		SSW2(156,		-2, -1),
		SSW3(168.7F,	-2, 0),
		S(180,			-2, 0),
		SSE1(191.3F,	-2, 1),
		SSE2(204,		-2, 1),
		SSE3(216.9F,	-2, 2),
		SE(225,			-2, 2),
		ESE1(233.1F,	-1, 2),
		ESE2(246,		-1, 2),
		ESE3(258.7F,	0, 2),
		E(270,			0, 2),
		ENE1(281.3F,	1, 2),
		ENE2(294,		1, 2),
		ENE3(306.9F,	2, 2),
		NE(315,			2, 2),
		NNE1(323.1F,	2, 1),
		NNE2(336,		2, 1),
		NNE3(348.7F,	2, 0);

		public final float angle;
		public final int xOffset;
		public final int zOffset;

		public static final Aim[] list = values();

		Aim(float a, int x, int z) {
			angle = a;
			xOffset = x;
			zOffset = z;
		}

		public Aim getNext() {
			return this.ordinal() < list.length-1 ? list[this.ordinal()+1] : list[0];
		}

		public Aim getPrev() {
			return this.ordinal() > 0 ? list[this.ordinal()-1] : list[list.length-1];
		}

		public boolean isCardinal() {
			return this.ordinal()%8 == 0;
		}
	}
}
