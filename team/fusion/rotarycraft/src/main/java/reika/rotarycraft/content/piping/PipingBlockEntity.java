/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.piping;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: Reika.RotaryCraft.Base.TileEntity.TileEntityPiping.
 * <p>
 * Every flow step a pipe first pulls from its neighbours, then pushes to them: a quarter of the level difference to other pipes,
 * a quarter of its contents to machines. RotaryCraft machines decide the sides and flow direction themselves ({@link PipeConnector});
 * other fluid handlers are fed only from the sides and drained only from above and below ({@link #canOutputToIFluidHandler}).
 * <p>
 * Port notes: the connections that the original cached in the tile entity are the block state properties here (vanilla
 * {@link PipeBlock#PROPERTY_BY_DIRECTION}), so the block model draws them. Fluid moves on the server only; the original also
 * simulated it on the client. ChromatiCraft rifts and the pipe pump (PumpablePipe) are not ported.
 */
public abstract class PipingBlockEntity extends RotaryCraftBlockEntity implements FluidPipeView, reika.rotarycraft.api.interfaces.PumpablePipe {

	private static final Map<Class<?>, Boolean> interactableClasses = new HashMap<>();

	private static final int CAPACITY_LIMIT = 1000000000; //1 billion mB to prevent overflow

	public static final int UPPRESSURE = 40;
	public static final int HORIZPRESSURE = 20;
	public static final int DOWNPRESSURE = 0;

	private static final int MAXPRESSURE = 2400000;

	private final boolean[] interaction = new boolean[6];

	private int connectionDelay = 0;
	private int flowTimer = 0;

	protected PipingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public abstract PipeType getPipeType();

	public abstract int getFluidLevel();

	@Nullable
	public abstract Fluid getFluidType();

	protected abstract void setFluid(@Nullable Fluid f);

	protected abstract void setLevel(int amt);

	protected abstract boolean interactsWithMachines();

	protected abstract void onIntake(BlockEntity te);

	public abstract boolean canConnectToPipe(PipeType p, Direction dir);

	public abstract boolean canReceiveFromPipeOn(Direction side);

	public abstract boolean canEmitToPipeOn(Direction side);

	public abstract boolean canIntakeFromIFluidHandler(Direction side);

	public abstract boolean canOutputToIFluidHandler(Direction side);

	public abstract boolean isValidFluid(Fluid f);

	public boolean hasLiquid() {
		return this.getFluidType() != null && this.getFluidLevel() > 0;
	}

	public final boolean canIntakeFluid(@Nullable Fluid f) {
		if (f == null || f == Fluids.EMPTY)
			return false;
		return this.isValidFluid(f) && (this.getFluidType() == null || this.getFluidLevel() == 0 || this.getFluidType().isSame(f));
	}

	/** Original: p = rho*R*T approximation; gases (here: fluid types lighter than air) by temperature and density. */
	public final int getPressure() {
		Fluid f = this.getFluidType();
		int amt = this.getFluidLevel();
		if (f == null || amt <= 0)
			return 101300;
		long ret;
		if (f.getFluidType().isLighterThanAir())
			ret = 101300+(128*(int)(amt/1000D*f.getFluidType().getTemperature()*Math.abs(f.getFluidType().getDensity())/1000D));
		else
			ret = 101300+amt*24L;
		return (int)Math.min(Integer.MAX_VALUE, ret);
	}

	public int getMaxPressure() {
		return MAXPRESSURE;
	}

	private void overpressure(Level world, BlockPos pos) {
		Fluid f = this.getFluidType();
		BlockState fb = f.defaultFluidState().createLegacyBlock();
		world.setBlockAndUpdate(pos, fb.isAir() ? Blocks.AIR.defaultBlockState() : fb);
	}

	/** Original getTickDelay: flow every 6-FLOWSPEED ticks (FLOWSPEED 1-5). */
	public static int getTickDelay() {
		return 6-Math.min(Math.max(RotaryConfig.FLOWSPEED.get(), 1), 5);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		if (this.getTicksExisted() == 0)
			this.recomputeConnections(world, pos);
		Fluid f = this.getFluidType();
		flowTimer++;
		if (flowTimer >= getTickDelay()) {
			flowTimer = 0;
			this.intakeFluid(world, pos);
			this.dumpContents(world, pos);
		}
		if (this.getFluidLevel() <= 0) {
			this.setLevel(0);
			this.setFluid(null);
		}
		if (f != this.getFluidType()) {
			this.syncAllData();
			world.getLightEngine().checkBlock(pos);
		}
		if (this.getFluidType() != null && this.getPressure() > this.getMaxPressure()) {
			this.overpressure(world, pos);
			return;
		}
		if (connectionDelay > 0) {
			connectionDelay--;
			if (connectionDelay == 0)
				this.recomputeConnections(world, pos);
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	protected void queueConnectionEvaluation(int delay) {
		connectionDelay = 2;
	}

	public final boolean isConnectedDirectly(Direction dir) {
		return this.getBlockState().getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir));
	}

	private boolean canInteractWith(Level world, BlockPos pos, Direction side, boolean connected) {
		if (!connected)
			return false;
		BlockEntity te = world.getBlockEntity(pos.relative(side));
		if (te instanceof PipingBlockEntity tp)
			return this.hasReciprocalConnectivity(tp, side);
		return this.interactsWithMachines() && this.isInteractableTile(world, te, side);
	}

	/**
	 * Original isInteractableTile: any fluid handler, except other mods' pipes and conduits (recognised, as in the original,
	 * by "conduit", "fluidduct", "pipe" or "multipart" in the class name).
	 */
	private boolean isInteractableTile(Level world, @Nullable BlockEntity te, Direction side) {
		if (te == null)
			return false;
		if (world.getCapability(Capabilities.FluidHandler.BLOCK, te.getBlockPos(), side.getOpposite()) == null)
			return false;
		if (te instanceof PipeConnector)
			return true;
		return interactableClasses.computeIfAbsent(te.getClass(), c -> {
			String name = c.getSimpleName().toLowerCase(Locale.ENGLISH);
			return !(name.contains("conduit") || name.contains("fluidduct") || name.contains("pipe") || name.contains("multipart"));
		});
	}

	public final int getPipeIntake(int otherlevel) {
		return Math.min(CAPACITY_LIMIT-this.getFluidLevel(), TransferAmount.QUARTER.getTransferred(otherlevel));
	}

	public final int getPipeOutput(int max) {
		return Math.min(TransferAmount.QUARTER.getTransferred(max), this.getFluidLevel()-5);
	}

	private void dumpContents(Level world, BlockPos pos) {
		Fluid f = this.getFluidType();
		if (this.getFluidLevel() <= 1 || f == null)
			return;
		for (Direction dir : Direction.values()) {
			int level = this.getFluidLevel();
			if (level <= 0) {
				this.setFluid(null);
				return;
			}
			if (!interaction[dir.ordinal()])
				continue;
			BlockPos dp = pos.relative(dir);
			BlockEntity te = world.getBlockEntity(dp);
			if (te instanceof PipingBlockEntity tp) {
				if (this.hasReciprocalConnectivity(tp, dir) && this.canEmitToPipeOn(dir) && tp.canReceiveFromPipeOn(dir.getOpposite())) {
					if (tp.canIntakeFluid(f)) {
						int otherlevel = tp.getFluidLevel();
						int dL = level-otherlevel;
						int toadd = this.getPipeOutput(dL);
						if (toadd > 0) {
							tp.addFluid(f, toadd);
							this.removeLiquid(toadd);
						}
					}
				}
			}
			else if (te instanceof PipeConnector pc) {
				PipeConnector.Flow flow = pc.getFlowForSide(dir.getOpposite());
				if (flow.canIntake) {
					int toadd = this.getPipeOutput(this.getFluidLevel());
					if (toadd > 0) {
						int added = pc.fill(dir.getOpposite(), new FluidStack(f, toadd), IFluidHandler.FluidAction.EXECUTE);
						if (added > 0)
							this.removeLiquid(added);
					}
				}
			}
			else if (te != null && this.canOutputToIFluidHandler(dir)) {
				IFluidHandler fl = world.getCapability(Capabilities.FluidHandler.BLOCK, dp, dir.getOpposite());
				if (fl != null) {
					int toadd = this.getPipeOutput(this.getFluidLevel());
					if (toadd > 0) {
						int added = fl.fill(new FluidStack(f, toadd), IFluidHandler.FluidAction.EXECUTE);
						if (added > 0)
							this.removeLiquid(added);
					}
				}
			}
		}
	}

	private void intakeFluid(Level world, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			if (!interaction[dir.ordinal()])
				continue;
			BlockPos dp = pos.relative(dir);
			BlockEntity te = world.getBlockEntity(dp);
			if (te instanceof PipingBlockEntity tp) {
				if (this.hasReciprocalConnectivity(tp, dir) && this.canReceiveFromPipeOn(dir) && tp.canEmitToPipeOn(dir.getOpposite())) {
					Fluid f = tp.getFluidType();
					int amt = tp.getFluidLevel();
					int dL = amt-this.getFluidLevel();
					int todrain = this.getPipeIntake(dL);
					if (todrain > 0 && this.canIntakeFluid(f)) {
						this.setFluid(f);
						this.addFluid(todrain);
						tp.removeLiquid(todrain);
						this.onIntake(te);
					}
				}
			}
			else if (te instanceof PipeConnector pc) {
				PipeConnector.Flow flow = pc.getFlowForSide(dir.getOpposite());
				if (flow.canOutput) {
					//original: a pump only pushes into pipes holding less than its back pressure
					if (pc instanceof reika.rotarycraft.content.blockentity.PumpBlockEntity pump && pump.getMaxBackPressure() <= this.getFluidLevel())
						continue;
					FluidStack fs = pc.drain(dir.getOpposite(), Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
					if (!fs.isEmpty()) {
						int todrain = this.getPipeIntake(fs.getAmount()-this.getFluidLevel());
						if (todrain > 0 && this.canIntakeFluid(fs.getFluid())) {
							FluidStack got = pc.drain(dir.getOpposite(), todrain, IFluidHandler.FluidAction.EXECUTE);
							if (!got.isEmpty()) {
								this.setFluid(got.getFluid());
								this.addFluid(got.getAmount());
								this.onIntake(te);
							}
						}
					}
				}
			}
			else if (te != null && this.canIntakeFromIFluidHandler(dir)) {
				IFluidHandler fl = world.getCapability(Capabilities.FluidHandler.BLOCK, dp, dir.getOpposite());
				if (fl != null) {
					FluidStack fs = fl.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
					if (!fs.isEmpty()) {
						int todrain = this.getPipeIntake(fs.getAmount()-this.getFluidLevel());
						if (todrain > 0 && this.canIntakeFluid(fs.getFluid())) {
							this.setFluid(fs.getFluid());
							this.onIntake(te);
							this.addFluid(fl.drain(todrain, IFluidHandler.FluidAction.EXECUTE).getAmount());
						}
					}
				}
			}
		}
	}

	public final int removeLiquid(int max) {
		int has = this.getFluidLevel();
		int rem = Math.min(max, has);
		this.setLevel(has-rem);
		this.setChanged();
		return rem;
	}

	public final void addFluid(int toadd) {
		this.setLevel(this.getFluidLevel()+toadd);
		this.setChanged();
	}

	public final boolean addFluid(Fluid f, int toadd) {
		Fluid has = this.getFluidType();
		if (has != null && !has.isSame(f))
			return false;
		this.setFluid(f);
		this.addFluid(toadd);
		return true;
	}

	/** Recomputes which sides connect (block state) and which of them exchange fluid. */
	public void recomputeConnections(Level world, BlockPos pos) {
		BlockState state = this.getBlockState();
		BlockState ns = state;
		for (Direction dir : Direction.values()) {
			boolean c = this.shouldTryToConnect(world, pos, dir);
			ns = ns.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), c);
			interaction[dir.ordinal()] = this.canInteractWith(world, pos, dir, c);
		}
		if (ns != state)
			world.setBlock(pos, ns, Block.UPDATE_ALL);
		this.syncAllData();
	}

	public boolean shouldTryToConnect(Level world, BlockPos pos, Direction dir) {
		BlockEntity tile = world.getBlockEntity(pos.relative(dir));
		if (tile instanceof PipingBlockEntity tp)
			return this.hasReciprocalConnectivity(tp, dir);
		else if (tile instanceof PipeConnector pc)
			return pc.canConnectToPipe(this.getPipeType()) && pc.canConnectToPipeOnSide(this.getPipeType(), dir.getOpposite());
		else
			return this.interactsWithMachines() && this.isInteractableTile(world, tile, dir);
	}

	/** Original TileEntityPiping.canTransferTo (pipe pump): only into a pipe connected to this one both ways. */
	@Override
	public final boolean canTransferTo(reika.rotarycraft.api.interfaces.PumpablePipe p, Direction dir) {
		return p instanceof PipingBlockEntity te && this.hasReciprocalConnectivity(te, dir);
	}

	/** Original TileEntityPiping.transferFrom (pipe pump). */
	@Override
	public final void transferFrom(reika.rotarycraft.api.interfaces.PumpablePipe from, int amt) {
		PipingBlockEntity te = (PipingBlockEntity)from;
		this.setLevel(this.getFluidLevel()+amt);
		this.setFluid(te.getFluidType());
		te.setLevel(te.getFluidLevel()-amt);
		if (te.getFluidLevel() == 0)
			te.setFluid(null);
	}

	public final boolean hasReciprocalConnectivity(@Nullable PipingBlockEntity te, Direction dir) {
		return te != null && this.canConnectToPipe(te.getPipeType(), dir) && te.canConnectToPipe(this.getPipeType(), dir.getOpposite());
	}

	/** Original BlockBasicMultiTE.getLightValue: the contained fluid's luminosity. */
	public int getLightLevel() {
		Fluid f = this.getFluidType();
		return f == null || this.getFluidLevel() <= 0 ? 0 : f.getFluidType().getLightLevel();
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		Fluid f = this.getFluidType();
		if (f != null)
			NBT.putString("fluid", BuiltInRegistries.FLUID.getKey(f).toString());
		NBT.putInt("level", this.getFluidLevel());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		Fluid old = this.getFluidType();
		Fluid f = null;
		if (NBT.contains("fluid")) {
			ResourceLocation id = ResourceLocation.tryParse(NBT.getString("fluid"));
			f = id == null ? null : BuiltInRegistries.FLUID.get(id);
			if (f == Fluids.EMPTY)
				f = null;
		}
		this.setFluid(f);
		this.setLevel(NBT.getInt("level"));
		if (level != null && level.isClientSide && old != f)
			level.getLightEngine().checkBlock(worldPosition);
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		NBT.putInt("conndelay", connectionDelay);
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		connectionDelay = NBT.getInt("conndelay");
	}

	/** Original TileEntityPiping.TransferAmount. */
	public enum TransferAmount {
		UNITY,
		BUCKET,
		QUARTER,
		FORCEDQUARTER,
		ALL;

		public int getTransferred(int max) {
			if (max <= 0)
				return 0;
			return switch (this) {
				case ALL -> max;
				case FORCEDQUARTER -> max/4+1;
				case QUARTER -> max/4;
				case UNITY -> 1;
				case BUCKET -> Math.min(max, 1000);
			};
		}
	}
}
