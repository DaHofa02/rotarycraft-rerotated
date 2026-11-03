/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import reika.electricraft.network.WireNetwork;
import reika.electricraft.network.WrappableWireSource;
import reika.reactorcraft.content.block.GeneratorMultiBlock;
import reika.reactorcraft.reactor.HydrogenExplosion;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorSounds;
import reika.rotarycraft.api.interfaces.Screwdriverable;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;

/**
 * Original: Reika.ReactorCraft.TileEntities.TileEntityReactorGenerator - the end of the generator multiblock. It reads the steam
 * turbine (normal or high pressure) 10 blocks along its facing direction, if that turbine's steam moves towards it, and turns its
 * power into RF (1 RF/t per 520 W, the original's default without Thermal Expansion), EU (IC2 - does not exist on 1.21.1, so the mode
 * is skipped like in the original without IC2) or ElectriCraft current (voltage omega*8, current = 20-tick average torque/8, both
 * clamped to the turbine's generator limits). Output goes only to the side opposite its facing; RF is pushed every tick. Without the
 * formed multiblock it reads nothing; un-forming it above 1024 rad/s blows it up. Screwdriver: right-click on a side sets the facing,
 * shift-right-click cycles the mode.
 */
public class ReactorGeneratorBlockEntity extends RotaryCraftBlockEntity implements Screwdriverable, WrappableWireSource {

	public static final int GENERATOR_LENGTH = 10;

	@Nullable
	private Direction facingDir;

	private long power;
	private int torquein;
	private int omegain;

	private int lasttorquein;
	private int lastomegain;

	private Modes mode = Modes.RF;

	private boolean hasMultiblock;

	/** Original DragonAPI MovingAverage(20). */
	private final ArrayDeque<Double> torqueAvg = new ArrayDeque<>();
	private double currentAverage;
	private double lastAverage;

	public ReactorGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.GENERATOR.get(), pos, state);
		for (int i = 0; i < 20; i++)
			torqueAvg.add(0D);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public boolean hasMultiBlock() {
		return hasMultiblock;
	}

	public void setHasMultiBlock(boolean has) {
		if (hasMultiblock && !has)
			this.testBreakageFailure();
		hasMultiblock = has;
		this.syncAllData();
	}

	private void testBreakageFailure() {
		if (omegain > 1024)
			this.fail();
	}

	/** Original: ItemReactorPlacer sets the facing to the direction the player looks. */
	@Override
	public void setPlacer(Player ep) {
		super.setPlacer(ep);
		this.setFacing(ep.getDirection());
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (!world.isClientSide && (world.getGameTime()&127) == 0)
			world.updateNeighborsAt(pos, state.getBlock());

		lastomegain = omegain;
		lasttorquein = torquein;

		if (hasMultiblock)
			this.getPower(world, pos);
		else
			omegain = torquein = 0;
		torqueAvg.add((double)torquein);
		torqueAvg.remove();
		lastAverage = currentAverage;
		double avg = 0;
		for (double d : torqueAvg)
			avg += d;
		currentAverage = avg/torqueAvg.size();
		if (Math.abs(currentAverage-lastAverage) <= Math.min(lastAverage, currentAverage)*0.05)
			currentAverage = lastAverage;

		power = (long)omegain*(long)torquein;

		if (power > 0 && !world.isClientSide) {
			Direction write = this.getFacing().getOpposite();
			java.util.function.Supplier<net.minecraft.sounds.SoundEvent> rs = null;
			int len = 1;
			switch (mode) {
				case RF -> {
					IEnergyStorage rc = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(write), this.getFacing());
					if (rc != null)
						rc.receiveEnergy((int)this.getGenUnits(), false);
					rs = ReactorSounds.GENERATOR_RF;
					len = 129;
				}
				case EU -> {
					rs = null; //IC2 does not exist on 1.21.1; the mode is never selectable
				}
				case ELC -> { //handled by the wire network
					rs = ReactorSounds.GENERATOR_ELC;
					len = 94;
				}
			}
			if (rs != null && this.getTicksExisted()%len == 0) {
				Direction f = this.getFacing();
				int l = GENERATOR_LENGTH;
				ReactorSounds.play(world, pos, rs.get(), 2, 1);
				ReactorSounds.play(world, pos.offset(f.getStepX()*l, 0, f.getStepZ()*l), rs.get(), 2, 1);
				ReactorSounds.play(world, pos.offset(f.getStepX()*l/2, 0, f.getStepZ()*l/2), rs.get(), 2, 1);
			}
		}
	}

	/** Original fail: FlyingBlocksExplosion of power 12 in the middle of the generator (here the matching HydrogenExplosion). */
	private void fail() {
		if (!(level instanceof ServerLevel sl))
			return;
		int l = GENERATOR_LENGTH/2;
		BlockPos c = worldPosition.offset(this.getFacing().getStepX()*l, 0, this.getFacing().getStepZ()*l);
		sl.removeBlock(worldPosition, false);
		HydrogenExplosion.explode(sl, c, 12);
	}

	private void getPower(Level world, BlockPos pos) {
		TurbineCoreBlockEntity te = this.getTurbine(world, pos);
		if (te != null && te.getSteamMovement() == this.getFacing().getOpposite()) {
			power = te.getPower();
			omegain = te.getOmega();
			torquein = te.getTorque();
		}
		else {
			omegain = torquein = 0;
			power = 0;
		}
	}

	@Nullable
	private TurbineCoreBlockEntity getTurbine(Level world, BlockPos pos) {
		if (world == null)
			return null;
		Direction f = this.getFacing();
		return world.getBlockEntity(pos.offset(f.getStepX()*GENERATOR_LENGTH, f.getStepY()*GENERATOR_LENGTH, f.getStepZ()*GENERATOR_LENGTH))
				instanceof TurbineCoreBlockEntity te ? te : null;
	}

	public Direction getFacing() {
		return facingDir != null ? facingDir : Direction.EAST;
	}

	public void setFacing(Direction dir) {
		facingDir = dir;
		this.syncAllData();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)(0.5*Math.pow(Math.log(omegain+1)/Math.log(2), 1.05));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		facingDir = NBT.contains("face") ? Direction.from3DDataValue(NBT.getInt("face")) : null;
		hasMultiblock = NBT.getBoolean("multi");
		power = NBT.getLong("pwr");
		if (NBT.contains("mode"))
			mode = Modes.list[NBT.getInt("mode")];
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("face", this.getFacing().get3DDataValue());
		NBT.putBoolean("multi", hasMultiblock);
		NBT.putLong("pwr", power);
		NBT.putInt("mode", mode.ordinal());
	}

	public double getGenUnits() {
		return power*mode.ratio;
	}

	public String getGeneratedOutputForDisplay() {
		return this.getName()+" generating "+mode.getDisplay(this);
	}

	/** Original IEnergyHandler: RF taken from the output side only (the amount offered is not used up, as in the original). */
	@Nullable
	public IEnergyStorage getEnergyStorage(@Nullable Direction from) {
		if (from != this.getFacing().getOpposite())
			return null;
		return new IEnergyStorage() {
			@Override
			public int receiveEnergy(int maxReceive, boolean simulate) {
				return 0;
			}

			@Override
			public int extractEnergy(int maxExtract, boolean simulate) {
				return mode == Modes.RF ? (int)Math.min(maxExtract, ReactorGeneratorBlockEntity.this.getGenUnits()) : 0;
			}

			@Override
			public int getEnergyStored() {
				return 0;
			}

			@Override
			public int getMaxEnergyStored() {
				return Integer.MAX_VALUE;
			}

			@Override
			public boolean canExtract() {
				return mode == Modes.RF;
			}

			@Override
			public boolean canReceive() {
				return false;
			}
		};
	}

	public enum Modes {
		RF("Redstone Flux", 1D/Modes.WATTS_PER_RF),
		EU("EU", 1D/(Modes.WATTS_PER_RF*4D)),
		ELC("ElectriCraft", 1);

		/** Original ReikaRFHelper.getWattsPerRF: 20*5.2 MJ / 200000 RF (Thermal Expansion's default stone melt energy). */
		public static final int WATTS_PER_RF = 520;

		public final String name;
		private final double ratio;

		private static final Modes[] list = values();

		Modes(String s, double r) {
			name = s;
			ratio = r;
		}

		public String getDisplay(ReactorGeneratorBlockEntity te) {
			if (this == ELC)
				return te.getTorque()/WireNetwork.TORQUE_PER_AMP+"A @ "+te.getOmega()*WireNetwork.TORQUE_PER_AMP+"V";
			return String.format(Locale.ROOT, "%.3f %s/t.", te.power*ratio, this.name());
		}

		/** Original type.isLoaded(): RF (NeoForge energy) and ElectriCraft exist, IC2 does not. */
		public boolean exists() {
			return this != EU;
		}
	}

	public Modes stepType() {
		int o = mode.ordinal();
		Modes m;
		do {
			o = o < Modes.list.length-1 ? o+1 : 0;
			m = Modes.list[o];
		} while (!m.exists());
		mode = m;
		this.syncAllData();
		return mode;
	}

	public Modes getMode() {
		return mode;
	}

	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		this.stepType();
		return true;
	}

	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		if (side.getStepY() == 0) {
			this.setFacing(side);
			return true;
		}
		return false;
	}

	/** Original breakBlock: un-forms the multiblock parts beside it. */
	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (Direction dir : Direction.values())
			GeneratorMultiBlock.breakMultiBlock(world, pos.relative(dir));
	}

	@Override
	public boolean canConnectToSide(Direction dir) {
		return dir == this.getFacing().getOpposite();
	}

	@Override
	public boolean isFunctional() {
		return hasMultiblock && mode == Modes.ELC && !this.isRemoved();
	}

	@Override
	public int getOmega() {
		TurbineCoreBlockEntity te = this.getTurbine(level, worldPosition);
		int lim = te instanceof HiPTurbineBlockEntity ? HiPTurbineBlockEntity.GEN_OMEGA : TurbineCoreBlockEntity.GEN_OMEGA;
		return Math.min(omegain, (int)(lim*0.995));
	}

	@Override
	public int getTorque() {
		TurbineCoreBlockEntity te = this.getTurbine(level, worldPosition);
		if (te == null)
			return 0;
		double max = te.getTorque();
		if (!(te instanceof HiPTurbineBlockEntity))
			max = te.isAmmonia() ? TurbineCoreBlockEntity.TORQUE_CAP*0.95 : TurbineCoreBlockEntity.TORQUE_CAP*1.95;
		return (int)Math.min(max, currentAverage);
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public int getIORenderAlpha() {
		return 0;
	}

	@Override
	public void setIORenderAlpha(int io) {}

	@Override
	public boolean hasPowerStatusChangedSinceLastTick() {
		return lastomegain != omegain || lasttorquein != torquein;
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		PowerSourceList p = new PowerSourceList();
		if (power > 0) {
			TurbineCoreBlockEntity te = this.getTurbine(level, worldPosition);
			if (te != null)
				p.addSource(te);
		}
		return p;
	}

	@Override
	public void getAllOutputs(Collection<net.minecraft.world.level.block.entity.BlockEntity> c, Direction dir) {
		c.add(this.getAdjacentTileEntity(this.getFacing().getOpposite()));
	}

	@Override
	public Level getWorld() {
		return level;
	}

	@Override
	public BlockPos getPos() {
		return worldPosition;
	}

	@Override
	public BlockPos getIoOffset() {
		return BlockPos.ZERO;
	}
}
