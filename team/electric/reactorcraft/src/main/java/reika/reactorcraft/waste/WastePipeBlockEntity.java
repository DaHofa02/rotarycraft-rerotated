/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.waste;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.reactorcraft.reactor.ThoriumCoreBlockEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.FluidPipeView;
import reika.rotarycraft.content.piping.PipeConnector;

/**
 * Original: Reika.ReactorCraft.TileEntities.Waste.TileEntityWastePipe with TileEntityReactorPiping - a concrete duct for liquid nuclear
 * waste. It connects to other waste pipes and thorium cores (and the RotaryCraft crystallizer, not ported yet); each tick it takes a
 * quarter (+1) of the level difference from its neighbours (from machines what they would give) and gives a quarter (+1, keeping 5 mB)
 * of its level to lower neighbours and machines that take it.
 */
public class WastePipeBlockEntity extends RotaryCraftBlockEntity implements FluidPipeView {

	@Nullable
	private Fluid fluid;
	private int level;

	public WastePipeBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.WASTE_PIPE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Waste Pipe";
	}

	public static boolean isValidFluid(Fluid f) {
		return f.isSame(ReactorFluids.get("nuclear_waste"));
	}

	/** Original isWasteAcceptingBlock: waste pipes and thorium cores. */
	public static boolean connectsTo(@Nullable BlockEntity te) {
		return te instanceof WastePipeBlockEntity || te instanceof ThoriumCoreBlockEntity;
	}

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

	private static int forcedQuarter(int max) {
		return max <= 0 ? 0 : max/4+1;
	}

	private boolean canIntakeFluid(Fluid f) {
		return isValidFluid(f) && (fluid == null || f.isSame(fluid));
	}

	private void intakeFluid(Level world, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			if (!this.isConnectedDirectly(dir))
				continue;
			BlockEntity te = world.getBlockEntity(pos.relative(dir));
			if (te instanceof WastePipeBlockEntity tp) {
				if (tp.fluid != null) {
					int todrain = forcedQuarter(tp.level-level);
					if (todrain > 0 && this.canIntakeFluid(tp.fluid)) {
						fluid = tp.fluid;
						level += todrain;
						tp.level -= todrain;
					}
				}
			}
			else if (te instanceof PipeConnector pc && connectsTo(te)) {
				if (pc.getFlowForSide(dir.getOpposite()).canOutput) {
					FluidStack fs = pc.drain(dir.getOpposite(), Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
					if (!fs.isEmpty()) {
						int todrain = forcedQuarter(fs.getAmount()-level);
						if (todrain > 0 && this.canIntakeFluid(fs.getFluid())) {
							level += todrain;
							fluid = fs.getFluid();
							pc.drain(dir.getOpposite(), todrain, IFluidHandler.FluidAction.EXECUTE);
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
			BlockEntity te = world.getBlockEntity(pos.relative(dir));
			if (te instanceof WastePipeBlockEntity tp) {
				if (tp.canIntakeFluid(fluid)) {
					int toadd = this.getPipeOutput(level-tp.level);
					if (toadd > 0) {
						tp.fluid = fluid;
						tp.level += toadd;
						level -= toadd;
					}
				}
			}
			else if (te instanceof PipeConnector pc && connectsTo(te)) {
				if (pc.getFlowForSide(dir.getOpposite()).canIntake) {
					int toadd = this.getPipeOutput(level);
					if (toadd > 0) {
						int added = pc.fill(dir.getOpposite(), new FluidStack(fluid, toadd), IFluidHandler.FluidAction.EXECUTE);
						if (added > 0)
							level -= added;
					}
				}
			}
		}
	}

	private int getPipeOutput(int max) {
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
