/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * The distillery, written for the port from the original's behaviour: with power from below it converts, every 5 ticks, crude oil (fluid tag
 * c:oil) into lubricant (1 mB to 6 mB; at least 2048 Nm and 8192 W), bioethanol (c:bioethanol) into ethanol 1:1 and biofuel (c:biofuel) into
 * ethanol 2:1 (both at least 512 Nm and 131072 W). Input from the sides and below, output at the top; 6000 mB tanks.
 */
public class DistilleryBlockEntity extends PowerReceiverBlockEntity implements PipeConnector {

	public static final int CAPACITY = 6000;

	public enum Conversion {
		OIL("oil", 1, 6, 2048, 8192),
		BIOETHANOL("bioethanol", 1, 1, 512, 131072),
		BIOFUEL("biofuel", 2, 1, 512, 131072);

		public final TagKey<Fluid> input;
		public final int in, out, minTorque;
		public final long minPower;

		Conversion(String tag, int in, int out, int minTorque, long minPower) {
			input = FluidTags.create(ResourceLocation.fromNamespaceAndPath("c", tag));
			this.in = in;
			this.out = out;
			this.minTorque = minTorque;
			this.minPower = minPower;
		}

		public Fluid output() {
			return this == OIL ? RotaryFluids.LUBRICANT.get() : RotaryFluids.ETHANOL.get();
		}
	}

	/** Extra fluids for a conversion (GameTests run without an oil mod). */
	public static final Map<Fluid, Conversion> TEST_INPUTS = new HashMap<>();

	private final FluidTank input = new FluidTank(CAPACITY, fs -> conversionFor(fs.getFluid()) != null);
	private final FluidTank output = new FluidTank(CAPACITY);
	public int made;

	public DistilleryBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.DISTILLERY.get(), pos, state, 1, 1, 1);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	@Nullable
	public static Conversion conversionFor(Fluid f) {
		if (f == Fluids.EMPTY || f.isSame(RotaryFluids.ETHANOL.get()) || f.isSame(RotaryFluids.LUBRICANT.get()))
			return null;
		if (TEST_INPUTS.containsKey(f))
			return TEST_INPUTS.get(f);
		for (Conversion c : Conversion.values())
			if (f.is(c.input))
				return c;
		return null;
	}

	public FluidTank getInputTank() {
		return input;
	}

	public FluidTank getOutputTank() {
		return output;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 2;
			}

			@Override
			public FluidStack getFluidInTank(int i) {
				return (i == 0 ? input : output).getFluid();
			}

			@Override
			public int getTankCapacity(int i) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int i, FluidStack fs) {
				return i == 0 && input.isFluidValid(fs);
			}

			@Override
			public int fill(FluidStack fs, FluidAction action) {
				return side == Direction.UP ? 0 : input.fill(fs, action);
			}

			@Override
			public FluidStack drain(FluidStack fs, FluidAction action) {
				return side == null || side == Direction.UP ? output.drain(fs, action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return side == null || side == Direction.UP ? output.drain(max, action) : FluidStack.EMPTY;
			}
		};
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		read = Direction.DOWN;
		this.readPower();
		if (this.getTicksExisted()%5 != 0 || input.isEmpty())
			return;
		Conversion c = conversionFor(input.getFluid().getFluid());
		if (c == null || power < c.minPower || torque < c.minTorque || input.getFluidAmount() < c.in)
			return;
		FluidStack out = new FluidStack(c.output(), c.out);
		if (output.fill(out, IFluidHandler.FluidAction.SIMULATE) < c.out)
			return;
		input.drain(c.in, IFluidHandler.FluidAction.EXECUTE);
		output.fill(out, IFluidHandler.FluidAction.EXECUTE);
		made += c.out;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe() || p == PipeType.HOSE || p == PipeType.FUEL_LINE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == Direction.UP ? Flow.OUTPUT : Flow.INPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack fs, IFluidHandler.FluidAction action) {
		return from == Direction.UP ? 0 : input.fill(fs, action);
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return from == null || from == Direction.UP ? output.drain(maxDrain, action) : FluidStack.EMPTY;
	}

	private static CompoundTag write(FluidTank t) {
		CompoundTag c = new CompoundTag();
		c.putString("id", BuiltInRegistries.FLUID.getKey(t.getFluid().getFluid()).toString());
		c.putInt("amt", t.getFluidAmount());
		return c;
	}

	private static FluidStack read(CompoundTag t) {
		Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(t.getString("id")));
		return t.getInt("amt") > 0 && f != Fluids.EMPTY ? new FluidStack(f, t.getInt("amt")) : FluidStack.EMPTY;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("in", write(input));
		NBT.put("out", write(output));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		input.setFluid(read(NBT.getCompound("in")));
		output.setFluid(read(NBT.getCompound("out")));
	}
}
