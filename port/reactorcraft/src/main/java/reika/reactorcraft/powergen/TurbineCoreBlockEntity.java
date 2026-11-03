/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.content.block.SteamBlock;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorSounds;
import reika.rotarycraft.api.interfaces.Screwdriverable;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.api.power.ShaftPowerReceiver;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.RandomHelper;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.DifficultyEffects;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * Original: Reika.ReactorCraft.TileEntities.PowerGen.TileEntityTurbineCore - a line of up to five turbine stages. The first stage takes
 * steam from the steam block under it (one unit, two for ammonia) and then speeds up by 4*log2(65537) rad/s per tick to 65536 rad/s
 * (only with lubricant; otherwise it slows by omega/256+1); every stage copies speed and steam from the stage before. Each second
 * steam/32+1 is used. Torque: 24 per unit of steam (without steam omega/16+1), times the stage efficiency (1 stage 2.5 %, 2: 10 %,
 * 3: 25 %, 4: 50 %, 5: 100 %), at most 32768 Nm; the last stage writes it into the machine behind it. Blocks in the blade circle
 * jam it, liquids limit it to 512 rad/s; a living thing hit by the blades (not creative players) causes a small explosion and damages
 * the turbine (each damage level divides the steam). Lubricant (64000 mB tank, hose or bedrock pipe from the intake side) is spread
 * along the line and 20 mB are used every 20/difficulty factor ticks while turning. The screwdriver turns it.
 */
public class TurbineCoreBlockEntity extends RotaryCraftBlockEntity implements ShaftPowerEmitter, PipeConnector, Screwdriverable {

	public static final int GEN_OMEGA = 65536;
	public static final int TORQUE_CAP = 32768;
	public static final int LUBE_CAPACITY = 64000;

	protected int steam;
	protected int omega;
	private int iotick;
	private BlockPos read = BlockPos.ZERO;
	private BlockPos write = BlockPos.ZERO;
	protected boolean ammonia;
	protected final FluidTank tank = new FluidTank(this.getLubricantCapacity(), f -> f.getFluid().isSame(RotaryFluids.LUBRICANT.get()));
	private Interference inter;
	private int damage;
	private boolean enabled = true;
	private int stage;
	private int soundTimer;
	private int lubeTimer;
	private int thermalTicker;
	/** Original hasMultiBlock / readyForMultiBlock (only the high pressure turbine needs its multiblock). */
	protected boolean hasMultiBlock = !this.needsMultiblock();
	private boolean readyForMultiBlock;

	public TurbineCoreBlockEntity(BlockPos pos, BlockState state) {
		this(ReactorBlockEntities.TURBINE_CORE.get(), pos, state);
	}

	protected TurbineCoreBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	protected int getLubricantCapacity() {
		return LUBE_CAPACITY;
	}

	public boolean needsMultiblock() {
		return false;
	}

	public final void markForMulti() {
		readyForMultiBlock = true;
	}

	public void setHasMultiBlock(boolean has) {
		hasMultiBlock = has ? readyForMultiBlock : has;
		readyForMultiBlock = false;
		this.syncAllData();
	}

	public final boolean hasMultiBlock() {
		return hasMultiBlock;
	}

	protected boolean canCollideCheck() {
		return true;
	}

	protected boolean enabled(Level world, BlockPos pos) {
		return enabled;
	}

	protected void intakeLubricant(Level world, BlockPos pos) {}

	protected void dumpSteam(Level world, BlockPos pos) {}

	protected void copyDataFrom(TurbineCoreBlockEntity tile) {
		omega = tile.omega;
		phi = tile.phi;
		steam = tile.steam;
		ammonia = tile.ammonia;
	}

	@Override
	public String getName() {
		return "Steam Turbine";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.getIOSides(pos, this.getBlockMetadata());
		if (!hasMultiBlock) {
			omega = 0;
			phi = 0;
			steam = 0;
			return;
		}
		thermalTicker++;
		soundTimer++;

		stage = this.calcStage();
		this.intakeLubricant(world, pos);
		this.distributeLubricant(world);
		this.readSurroundings(world, pos);
		this.followHead(world, pos);
		if (this.canCollideCheck())
			this.enviroTest(world, pos);
		readyForMultiBlock = false;

		if (steam > 0) {
			this.dumpSteam(world, pos);
		}
		if (steam > 0 && thermalTicker >= 20) {
			thermalTicker = 0;
			steam -= this.getConsumedSteam();
		}
		if (thermalTicker >= 20)
			thermalTicker = 0;

		if (omega == 0) {
			phi = 0;
			steam = 0;
		}
		else {
			if (stage == 0 && soundTimer >= 41) {
				soundTimer = 0;
				ReactorSounds.play(world, pos, ReactorSounds.TURBINE.get(), 2F, 1F);
			}
			lubeTimer++;
			if (lubeTimer >= (int)(20/DifficultyEffects.LUBEUSAGE.getChance())) {
				lubeTimer = 0;
				if (!tank.isEmpty() && !world.isClientSide)
					tank.drain(this.getConsumedLubricant(), IFluidHandler.FluidAction.EXECUTE);
			}
		}

		steam *= this.getDamageEfficiency();

		if (world.getBlockEntity(write) instanceof ShaftPowerReceiver rec) {
			rec.setOmega(this.getOmega());
			rec.setTorque(this.getTorque());
			rec.setPower(this.getPower());
		}
	}

	protected int getConsumedLubricant() {
		return 20;
	}

	/** The stage behind this one (against the steam flow) shares its lubricant (half the difference, at most 1000 mB). */
	private void distributeLubricant(Level world) {
		Direction dir = this.getSteamMovement().getOpposite();
		if (world.getBlockEntity(worldPosition.relative(dir)) instanceof TurbineCoreBlockEntity te) {
			int max = Math.min(tank.getSpace(), 1000);
			int dl = te.tank.getFluidAmount()-tank.getFluidAmount();
			if (dl > 1) {
				int rem = Math.min(dl/2, max);
				tank.fill(new FluidStack(RotaryFluids.LUBRICANT.get(), rem), IFluidHandler.FluidAction.EXECUTE);
				te.tank.drain(rem, IFluidHandler.FluidAction.EXECUTE);
			}
		}
	}

	protected int getMaxStage() {
		return 4;
	}

	protected final int getConsumedSteam() {
		return steam/32+1;
	}

	public Direction getSteamMovement() {
		return switch (this.getBlockMetadata()) {
			case 0 -> Direction.WEST;
			case 1 -> Direction.EAST;
			case 2 -> Direction.NORTH;
			case 3 -> Direction.SOUTH;
			default -> Direction.DOWN;
		};
	}

	private void getIOSides(BlockPos pos, int meta) {
		Direction d = this.getSteamMovement();
		read = pos.relative(d.getOpposite());
		write = pos.relative(d);
	}

	public int getMaxTorque() {
		return TORQUE_CAP;
	}

	public int getMaxSpeed() {
		return GEN_OMEGA;
	}

	private void updateSpeed(boolean up) {
		if (tank.isEmpty())
			up = false;
		if (up) {
			int max = this.getMaxSpeed();
			if (omega < max) {
				omega += 4*ReikaMathLibrary.logbase(max+1, 2);
				if (omega > max)
					omega = max;
			}
		}
		else if (omega > 0) {
			omega -= omega/256+1;
		}
	}

	@Nullable
	private TurbineCoreBlockEntity getReadCore() {
		if (level.getBlockEntity(read) instanceof TurbineCoreBlockEntity tile && tile.write.equals(worldPosition))
			return tile;
		return null;
	}

	@Nullable
	private TurbineCoreBlockEntity getWriteCore() {
		if (level.getBlockEntity(write) instanceof TurbineCoreBlockEntity tile && tile.read.equals(worldPosition))
			return tile;
		return null;
	}

	public final boolean isAtEndOfLine() {
		return this.getReadCore() == null;
	}

	protected final int getGenTorque() {
		int torque = steam > 0 ? (int)(steam*24*this.getTorqueFactor()) : omega/16+1;
		int ret = omega > 0 ? (int)(torque*this.getEfficiency()) : 0;
		return Math.min(ret, this.getMaxTorque());
	}

	protected float getTorqueFactor() {
		return 1;
	}

	private float getDamageEfficiency() {
		return damage > 0 ? 1F/(damage+1) : 1;
	}

	protected final long getGenPower() {
		return (long)this.getGenTorque()*(long)omega;
	}

	protected double getEfficiency() {
		return switch (this.getNumberStagesTotal()) {
			case 1 -> 0.025;
			case 2 -> 0.1;
			case 3 -> 0.25;
			case 4 -> 0.5;
			case 5 -> 1;
			default -> 0;
		};
	}

	public final int getStage() {
		return stage;
	}

	private int calcStage() {
		TurbineCoreBlockEntity tile = this.getReadCore();
		if (tile != null) {
			int s = tile.calcStage();
			return s == this.getMaxStage() ? this.getMaxStage() : s+1;
		}
		return 0;
	}

	public final int getNumberStagesTotal() {
		if (this.needsMultiblock() && !this.hasMultiBlock())
			return 0;
		TurbineCoreBlockEntity tile = this.getWriteCore();
		if (tile != null && (tile.hasMultiBlock() || !tile.needsMultiblock()))
			return tile.getNumberStagesTotal();
		return this.calcStage()+1;
	}

	/** Steam under the first stage that may drive a turbine and has not moved sideways: +1 unit (ammonia +2); true = accelerate. */
	protected boolean intakeSteam(Level world, BlockPos pos) {
		BlockState s = world.getBlockState(pos.below());
		boolean canAccel = false;
		if (s.getBlock() instanceof SteamBlock && stage == 0) {
			int meta2 = s.getValue(SteamBlock.FLAGS);
			if ((meta2&2) != 0 && (meta2&8) == 0) {
				if ((meta2&4) != 0) {
					steam += 2;
					ammonia = true;
				}
				else {
					steam++;
					ammonia = false;
				}
				canAccel = true;
			}
		}
		if (canAccel && world.isClientSide && world.getNearestPlayer(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 64, false) != null) {
			Direction dir = this.getSteamMovement();
			for (int i = 0; i < this.getNumberStagesTotal(); i++) {
				if (!(world.getBlockEntity(pos.relative(dir)) instanceof TurbineCoreBlockEntity te))
					break;
				double r = te.getRadius()*2.4-2;
				for (int n = 0; n < 2; n++) {
					double v = 0.03125+world.random.nextDouble()*(0.25-0.03125);
					double dx = pos.getX()+0.5+dir.getStepX()*i;
					double dy = pos.getY()+0.5+(world.random.nextDouble()*2-1)*r;
					double dz = pos.getZ()+0.5+dir.getStepZ()*i;
					if (dir.getStepX() != 0)
						dz += (world.random.nextDouble()*2-1)*r;
					else if (dir.getStepZ() != 0)
						dx += (world.random.nextDouble()*2-1)*r;
					world.addParticle(ParticleTypes.CLOUD, dx, dy, dz, dir.getStepX()*v, 0, dir.getStepZ()*v);
				}
			}
		}
		return canAccel;
	}

	public boolean isAmmonia() {
		return ammonia;
	}

	/** Blocks within the blade radius (in the plane of the blades, up to 3 out) jam the turbine; liquids slow it. */
	private void readSurroundings(Level world, BlockPos pos) {
		if (!this.canCollideCheck()) {
			if (stage == 0) {
				boolean accel = this.enabled(world, pos) && this.intakeSteam(world, pos);
				if (!world.isClientSide)
					this.updateSpeed(accel);
			}
			return;
		}
		inter = null;
		int r = 3;
		Direction mv = this.getSteamMovement();
		for (int i = -r; i <= r; i++) {
			for (int j = -r; j <= r; j++) {
				if (i == 0 && j == 0)
					continue;
				BlockPos c = mv.getAxis() == Direction.Axis.Z ? pos.offset(i, j, 0) : pos.offset(0, j, i);
				if (Math.sqrt(pos.distSqr(c)) <= this.getRadius()) {
					BlockState s = world.getBlockState(c);
					if (!soft(s)) {
						phi = 0;
						omega = 0;
						if (inter == null || inter.maxSpeed > Interference.JAM.maxSpeed)
							inter = Interference.JAM;
					}
					else if (!s.getFluidState().isEmpty()) {
						if (inter == null || inter.maxSpeed > Interference.FLUID.maxSpeed)
							inter = Interference.FLUID;
					}
				}
			}
		}
		if (stage == 0) {
			boolean accel = this.enabled(world, pos) && this.intakeSteam(world, pos);
			if (!world.isClientSide)
				this.updateSpeed(accel);
		}
	}

	/** Original ReikaWorldHelper.softBlocks. */
	private static boolean soft(BlockState s) {
		return s.isAir() || !s.getFluidState().isEmpty() || s.canBeReplaced() || s.is(net.minecraft.world.level.block.Blocks.VINE);
	}

	protected double getRadius() {
		return 1.5+stage/2;
	}

	private void enviroTest(Level world, BlockPos pos) {
		AABB box = new AABB(pos);
		int rr = 2+stage;
		Direction mv = this.getSteamMovement();
		box = mv.getAxis() == Direction.Axis.Z ? box.inflate(rr/2, rr/2, 0) : box.inflate(0, rr/2, rr/2);
		int r = 2+stage/2;
		List<LivingEntity> li = world.getEntitiesOfClass(LivingEntity.class, box);
		for (LivingEntity e : li) {
			if (this.getOmega() > 0 && e.position().distanceTo(Vec3.atCenterOf(pos)) < r) {
				if (canDamageTurbine(e)) {
					if (!world.isClientSide) {
						world.explode(null, e.getX(), e.getY()+e.getEyeHeight(), e.getZ(), 2, false, Level.ExplosionInteraction.NONE);
						e.hurt(world.damageSources().explosion(null, null), 2);
						damage++;
					}
					e.setDeltaMovement(e.getDeltaMovement().add(0.4*(e.getX()-pos.getX()-0.5+0.1)+rand.nextDouble()*0.1, 0.4*(e.getY()-pos.getY()-0.5+0.1),
							0.4*(e.getZ()-pos.getZ()-0.5+0.1)+rand.nextDouble()*0.1));
					if (inter == null || inter.maxSpeed > Interference.MOB.maxSpeed)
						inter = Interference.MOB;
				}
			}
		}
		if (inter != null)
			omega = Math.min(omega, inter.maxSpeed);
	}

	public static boolean canDamageTurbine(LivingEntity e) {
		if (e instanceof Player p)
			return !p.isCreative() && !p.isSpectator();
		return true;
	}

	private void followHead(Level world, BlockPos pos) {
		TurbineCoreBlockEntity head = this.getReadCore();
		if (head != null)
			this.copyDataFrom(head);
		TurbineCoreBlockEntity next = this.getWriteCore();
		if (next != null && next.inter != null)
			inter = next.inter;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		iotick -= 8;
		phi += (float)(this.getAnimationSpeed()*Math.pow(ReikaMathLibrary.logbase(omega+1, 2), 1.05));
	}

	protected double getAnimationSpeed() {
		return 0.2F;
	}

	/** The current blade obstruction (JAM, FLUID, MOB) or null. */
	public String getInterference() {
		return inter == null ? null : inter.name();
	}

	public int getDamage() {
		return damage;
	}

	public int getSteam() {
		return steam;
	}

	public int getLubricant() {
		return tank.getFluidAmount();
	}

	public FluidTank getTank() {
		return tank;
	}

	@Override
	public final int getOmega() {
		return this.isEmitting() ? omega : 0;
	}

	public final int getRawOmega() {
		return omega;
	}

	@Override
	public final int getTorque() {
		return this.getGenTorque();
	}

	@Override
	public final long getPower() {
		return this.getGenPower();
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
	public final boolean canWriteTo(Direction dir) {
		return worldPosition.relative(dir).equals(write);
	}

	@Override
	public final boolean isEmitting() {
		return this.getGenPower() > 0;
	}

	@Override
	public long getMaxPower() {
		return this.getGenPower();
	}

	@Override
	public long getCurrentPower() {
		return this.getGenPower();
	}

	@Override
	public BlockPos getEmittingPos() {
		return write;
	}

	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		return false;
	}

	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		int meta = this.getBlockMetadata();
		this.setBlockMetadata(meta < 3 ? meta+1 : 0);
		return true;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p == PipeType.HOSE || p == PipeType.BEDROCK_PIPE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == this.getSteamMovement().getOpposite() ? Flow.INPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from != this.getSteamMovement().getOpposite() || !tank.isFluidValid(resource))
			return 0;
		int n = tank.fill(resource, action);
		if (n > 0 && action.execute())
			this.setChanged();
		return n;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	public void addLubricant(int amt) {
		tank.fill(new FluidStack(RotaryFluids.LUBRICANT.get(), amt), IFluidHandler.FluidAction.EXECUTE);
	}

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
				return LUBE_CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return tank.isFluidValid(stack);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return TurbineCoreBlockEntity.this.fill(side, resource, action);
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
		NBT.putInt("speed", omega);
		NBT.putInt("steamlevel", steam);
		NBT.putInt("dmg", damage);
		NBT.putBoolean("ammonia", ammonia);
		NBT.putInt("blocked", inter != null ? inter.ordinal() : -1);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("stage", stage);
		NBT.putBoolean("t_enable", enabled);
		if (this.needsMultiblock())
			NBT.putBoolean("multi", hasMultiBlock);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		omega = NBT.getInt("speed");
		steam = NBT.getInt("steamlevel");
		damage = NBT.getInt("dmg");
		ammonia = NBT.getBoolean("ammonia");
		int b = NBT.contains("blocked") ? NBT.getInt("blocked") : -1;
		inter = b < 0 || b >= Interference.values().length ? null : Interference.values()[b];
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		stage = NBT.getInt("stage");
		if (NBT.contains("t_enable"))
			enabled = NBT.getBoolean("t_enable");
		if (this.needsMultiblock() && NBT.contains("multi"))
			hasMultiBlock = NBT.getBoolean("multi");
	}

	private enum Interference {
		JAM(0),
		FLUID(512),
		MOB(4096);

		public final int maxSpeed;

		Interference(int max) {
			maxSpeed = max;
		}
	}
}
