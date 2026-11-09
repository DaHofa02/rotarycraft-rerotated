/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.piping;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.rotarycraft.api.interfaces.PumpablePipe;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.FluidPipeView;
import reika.rotarycraft.content.piping.PipeConnector;

/**
 * Original: Reika.ReactorCraft.Base.TileEntityReactorPiping - the ReactorCraft ducts (waste pipe, magnetic pipe, gas duct). Each tick
 * a duct takes from every connected neighbour a quarter (+1) of the level difference (from machines: of what they would give), then
 * gives every connected neighbour a quarter (+1) of the level difference (to machines: of its own level), always keeping 5 mB. Fluid
 * handlers that are not {@link PipeConnector}s are filled only sideways (original: {@code dir.offsetY == 0}). Ducts of one kind
 * never mix fluids; what a duct carries and what it touches come from the subclass ({@link #isValidFluid}, {@link #isInteractableTile}).
 * The connections are the six {@link PipeBlock} properties of the block state, set by {@link reika.reactorcraft.content.block.ReactorPipeBlock}.
 * A pipe pump moves fluid between two ducts of the same kind (original PumpablePipe).
 */
public abstract class ReactorPipingBlockEntity extends RotaryCraftBlockEntity implements FluidPipeView, PumpablePipe {

	@Nullable
	protected Fluid fluid;
	protected int level;

	protected ReactorPipingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public abstract boolean isValidFluid(Fluid f);

	/**
	 * Original isInteractableTile: whether this duct exchanges fluid with {@code te}, which sits on side {@code dir} of it. Must agree
	 * with the block's connection test, since only connected sides are looked at.
	 */
	protected abstract boolean isInteractableTile(@Nullable BlockEntity te, Direction dir);

	/** Original onIntake: called after fluid was taken from {@code te}. */
	protected void onIntake(BlockEntity te) {}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		Fluid f = fluid;
		this.intakeFluid(world, pos);
		if (level <= 0) {
			level = 0;
			fluid = null;
		}
		else {
			this.dumpContents(world, pos);
		}
		if (f != fluid)
			this.syncAllData();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	/** Original TransferAmount.FORCEDQUARTER. */
	protected static int forcedQuarter(int max) {
		return max <= 0 ? 0 : max/4+1;
	}

	protected final boolean canIntakeFluid(Fluid f) {
		return this.isValidFluid(f) && (fluid == null || f.isSame(fluid));
	}

	private void intakeFluid(Level world, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			if (!this.isConnectedDirectly(dir))
				continue;
			BlockPos dp = pos.relative(dir);
			BlockEntity te = world.getBlockEntity(dp);
			if (!this.isInteractableTile(te, dir))
				continue;
			if (te instanceof ReactorPipingBlockEntity tp) {
				if (tp.fluid != null) {
					int todrain = forcedQuarter(tp.level-level);
					if (todrain > 0 && this.canIntakeFluid(tp.fluid)) {
						fluid = tp.fluid;
						level += todrain;
						tp.level -= todrain;
						this.onIntake(te);
					}
				}
			}
			else if (te instanceof PipeConnector pc) {
				if (pc.getFlowForSide(dir.getOpposite()).canOutput) {
					FluidStack fs = pc.drain(dir.getOpposite(), Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
					if (!fs.isEmpty()) {
						int todrain = forcedQuarter(fs.getAmount()-level);
						if (todrain > 0 && this.canIntakeFluid(fs.getFluid())) {
							level += todrain;
							fluid = fs.getFluid();
							pc.drain(dir.getOpposite(), todrain, IFluidHandler.FluidAction.EXECUTE);
							this.onIntake(te);
						}
					}
				}
			}
			else {
				IFluidHandler fl = world.getCapability(Capabilities.FluidHandler.BLOCK, dp, dir.getOpposite());
				if (fl != null) {
					FluidStack fs = fl.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
					if (!fs.isEmpty()) {
						int todrain = forcedQuarter(fs.getAmount()-level);
						if (todrain > 0 && this.canIntakeFluid(fs.getFluid())) {
							FluidStack got = fl.drain(new FluidStack(fs.getFluid(), todrain), IFluidHandler.FluidAction.EXECUTE);
							if (!got.isEmpty()) {
								level += got.getAmount();
								fluid = got.getFluid();
								this.onIntake(te);
							}
						}
					}
				}
			}
		}
	}

	private void dumpContents(Level world, BlockPos pos) {
		if (level <= 0 || fluid == null)
			return;
		for (Direction dir : Direction.values()) {
			if (level <= 0) {
				fluid = null;
				return;
			}
			if (!this.isConnectedDirectly(dir))
				continue;
			BlockPos dp = pos.relative(dir);
			BlockEntity te = world.getBlockEntity(dp);
			if (!this.isInteractableTile(te, dir))
				continue;
			if (te instanceof ReactorPipingBlockEntity tp) {
				if (tp.canIntakeFluid(fluid)) {
					int toadd = this.getPipeOutput(level-tp.level);
					if (toadd > 0) {
						tp.fluid = fluid;
						tp.level += toadd;
						level -= toadd;
					}
				}
			}
			else if (te instanceof PipeConnector pc) {
				if (pc.getFlowForSide(dir.getOpposite()).canIntake) {
					int toadd = this.getPipeOutput(level);
					if (toadd > 0) {
						int added = pc.fill(dir.getOpposite(), new FluidStack(fluid, toadd), IFluidHandler.FluidAction.EXECUTE);
						if (added > 0)
							level -= added;
					}
				}
			}
			else if (dir.getAxis() != Direction.Axis.Y) {
				IFluidHandler fl = world.getCapability(Capabilities.FluidHandler.BLOCK, dp, dir.getOpposite());
				if (fl != null) {
					int toadd = this.getPipeOutput(level);
					if (toadd > 0) {
						int added = fl.fill(new FluidStack(fluid, toadd), IFluidHandler.FluidAction.EXECUTE);
						if (added > 0)
							level -= added;
					}
				}
			}
		}
	}

	/** Original getPipeOutput: a quarter (+1) of {@code max}, keeping 5 mB in the duct. */
	protected final int getPipeOutput(int max) {
		return Math.min(forcedQuarter(max), level-5);
	}

	@Override
	@Nullable
	public Fluid getFluidType() {
		return fluid;
	}

	@Override
	public int getFluidLevel() {
		return level;
	}

	public void setFluid(@Nullable Fluid f, int amt) {
		fluid = f;
		level = amt;
	}

	/** Original canTransferTo: only into a duct of the same kind. */
	@Override
	public boolean canTransferTo(PumpablePipe p, Direction dir) {
		return p instanceof ReactorPipingBlockEntity && p.getClass() == this.getClass();
	}

	/** Original transferFrom. */
	@Override
	public void transferFrom(PumpablePipe from, int amt) {
		ReactorPipingBlockEntity te = (ReactorPipingBlockEntity)from;
		level += amt;
		fluid = te.fluid;
		te.level -= amt;
		if (te.level == 0)
			te.fluid = null;
		this.setChanged();
		te.setChanged();
	}

	@Override
	public boolean isConnectedDirectly(Direction dir) {
		return this.getBlockState().getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir));
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		if (fluid != null)
			NBT.putString("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
		NBT.putInt("level", level);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		fluid = NBT.contains("fluid") ? BuiltInRegistries.FLUID.get(ResourceLocation.parse(NBT.getString("fluid"))) : null;
		level = NBT.getInt("level");
	}
}
