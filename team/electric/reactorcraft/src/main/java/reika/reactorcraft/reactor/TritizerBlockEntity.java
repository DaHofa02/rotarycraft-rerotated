/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.RandomHelper;

/**
 * Original: Reika.ReactorCraft.TileEntities.Processing.TileEntityTritizer - a reactor column block with an input and an output tank
 * (1000 mB each). Neutrons that can irradiate materials (fission, breeder, fusion, thorium) turn its input with a chance into the
 * product: deuterium -> tritium (75 %, 25 mB), water -> heavy water (25 %, 100 mB); a neutron that does so is absorbed. If the output
 * holds another liquid, the input is used up for nothing (as in the original). A stack passes both tanks down to the tritizer below;
 * filling only from the top (only liquids with a reaction), draining only from the bottom; pipes connect on top and bottom. Its
 * temperature follows the surroundings only (not a temperatured reactor part in the original). Sides show the column position, top
 * and bottom #4. The custom tritizer recipes of the original's config file are not ported.
 */
public class TritizerBlockEntity extends ReactorBlockEntity implements NeutronTile, ReactorBlock, PipeConnector {

	public static final int CAPACITY = 1000;

	private final FluidTank input = new FluidTank(CAPACITY) {
		@Override
		protected void onContentsChanged() {
			TritizerBlockEntity.this.setChanged();
		}
	};
	private final FluidTank output = new FluidTank(CAPACITY) {
		@Override
		protected void onContentsChanged() {
			TritizerBlockEntity.this.setChanged();
		}
	};

	public TritizerBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.TRITIZER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Tritizer";
	}

	public enum Reactions {
		TRITIUM(() -> ReactorFluids.get("deuterium"), () -> ReactorFluids.get("tritium"), 75, 25),
		D20(() -> Fluids.WATER, () -> ReactorFluids.get("heavy_water"), 25, 100);

		private final Supplier<Fluid> input;
		private final Supplier<Fluid> output;
		public final int chance;
		public final int amount;

		Reactions(Supplier<Fluid> in, Supplier<Fluid> out, int chance, int amt) {
			input = in;
			output = out;
			this.chance = chance;
			amount = amt;
		}

		public Fluid getInput() {
			return input.get();
		}

		public Fluid getOutput() {
			return output.get();
		}

		@Nullable
		public static Reactions getReactionFrom(Fluid in) {
			for (Reactions r : values())
				if (r.getInput().isSame(in))
					return r;
			return null;
		}
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (!world.isClientSide)
			this.feed(world, pos);
		if (this.thermalTick() && !world.isClientSide)
			this.updateTemperature(world, pos);
	}

	private void feed(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos.below()) instanceof TritizerBlockEntity tile) {
			int amt = tile.feedIn(input.getFluid(), false);
			if (amt > 0)
				input.drain(amt, IFluidHandler.FluidAction.EXECUTE);
			amt = tile.feedIn(output.getFluid(), true);
			if (amt > 0)
				output.drain(amt, IFluidHandler.FluidAction.EXECUTE);
		}
	}

	private int feedIn(FluidStack is, boolean out) {
		if (is.isEmpty())
			return 0;
		FluidTank tank = out ? output : input;
		Fluid f = is.getFluid();
		if (!out && Reactions.getReactionFrom(f) == null)
			return 0;
		if (!tank.isEmpty() && !tank.getFluid().getFluid().isSame(f))
			return 0;
		int add = Math.min(tank.getSpace(), is.getAmount());
		tank.fill(new FluidStack(f, add), IFluidHandler.FluidAction.EXECUTE);
		return add;
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		if (input.isEmpty())
			return false;
		if (e.getNeutronType().canIrradiateMaterials()) {
			Reactions r = Reactions.getReactionFrom(input.getFluid().getFluid());
			if (!world.isClientSide && r != null && this.canMake(r) && RandomHelper.doWithChance(r.chance, rand)) {
				this.make(r);
				return true;
			}
		}
		return false;
	}

	private void make(Reactions r) {
		input.drain(r.amount, IFluidHandler.FluidAction.EXECUTE);
		output.fill(new FluidStack(r.getOutput(), r.amount), IFluidHandler.FluidAction.EXECUTE);
	}

	private boolean canMake(Reactions r) {
		return input.getFluidAmount() >= r.amount && output.getSpace() >= r.amount && input.getFluid().getFluid().isSame(r.getInput());
	}

	public FluidTank getInput() {
		return input;
	}

	public FluidTank getOutput() {
		return output;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p) && side.getStepY() != 0;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from != Direction.UP || Reactions.getReactionFrom(resource.getFluid()) == null)
			return 0;
		return input.fill(resource, action);
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from != Direction.DOWN || output.isEmpty())
			return FluidStack.EMPTY;
		return output.drain(maxDrain, action);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		if (side == Direction.UP)
			return Flow.INPUT;
		if (side == Direction.DOWN)
			return Flow.OUTPUT;
		return Flow.NONE;
	}

	/** Capability: input tank filled from the top, output tank drained from the bottom. */
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 2;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return t == 0 ? input.getFluid() : output.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return t == 0 && Reactions.getReactionFrom(stack.getFluid()) != null;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return TritizerBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				if (side != Direction.DOWN || output.isEmpty() || !output.getFluid().getFluid().isSame(resource.getFluid()))
					return FluidStack.EMPTY;
				return output.drain(resource.getAmount(), action);
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return TritizerBlockEntity.this.drain(side, max, action);
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tritizerin", input.writeToNBT(registries, new CompoundTag()));
		NBT.put("tritizerout", output.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		input.setFluid(FluidStack.EMPTY);
		input.readFromNBT(registries, NBT.getCompound("tritizerin"));
		output.setFluid(FluidStack.EMPTY);
		output.readFromNBT(registries, NBT.getCompound("tritizerout"));
	}
}
