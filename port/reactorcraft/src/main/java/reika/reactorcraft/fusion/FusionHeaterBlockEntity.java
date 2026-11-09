/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.api.interfaces.Laserable;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fusion.TileEntityFusionHeater - the plasma preheater. Inside its complete multiblock and
 * with no side open to the air, every tick in a heat ray's beam it gains 640*log2(power) degrees (and stops the beam); every tick it
 * loses 1 + (T - ambient)/16384. At 150 million degrees, with 50 mB each of deuterium and tritium, it makes 100 mB of fusion plasma
 * per tick (8000 mB tank). Deuterium and tritium (4000 mB each) come in from any side, the plasma goes out at the top into a
 * magnetic pipe only.
 */
public class FusionHeaterBlockEntity extends RotaryCraftBlockEntity implements Laserable, PipeConnector, FusionMultiBlockTile, reika.rotarycraft.content.blockentity.HeatConduction {

	public static final int PLASMA_TEMP = 150000000;

	private int temperature;

	private boolean hasMultiBlock = false;

	private final FluidTank tank = new FluidTank(8000);
	private final FluidTank h2 = new FluidTank(4000);
	private final FluidTank h3 = new FluidTank(4000);

	public FusionHeaterBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.FUSION_HEATER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Fusion Heater";
	}

	/** Original ReikaWorldHelper.isExposedToAir: a side with air or a block without collision box. */
	private boolean isExposedToAir() {
		if (level == null)
			return true;
		for (Direction d : Direction.values()) {
			BlockPos p = worldPosition.relative(d);
			if (!level.isLoaded(p))
				continue;
			BlockState s = level.getBlockState(p);
			if (s.isAir() || s.getCollisionShape(level, p).isEmpty())
				return true;
		}
		return false;
	}

	@Override
	public boolean hasMultiBlock() {
		return hasMultiBlock && !this.isExposedToAir();
	}

	@Override
	public void setHasMultiBlock(boolean has) {
		hasMultiBlock = has && !this.isExposedToAir();
		this.syncAllData();
	}

	@Override
	public void whenInBeam(Level world, BlockPos pos, long power, int range) {
		if (this.hasMultiBlock())
			temperature += 640*ReikaMathLibrary.logbase(power, 2);
	}

	@Override
	public boolean blockBeam(Level world, BlockPos pos, long power) {
		return this.hasMultiBlock();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		this.updateTemperature(world, pos);
		if (this.canMake())
			this.make();
	}

	private boolean canMake() {
		return this.hasMultiBlock() && temperature >= PLASMA_TEMP && h2.getFluidAmount() >= 50 && h3.getFluidAmount() >= 50
				&& tank.getFluidAmount()+100 <= tank.getCapacity();
	}

	private void make() {
		h2.drain(50, IFluidHandler.FluidAction.EXECUTE);
		h3.drain(50, IFluidHandler.FluidAction.EXECUTE);
		tank.fill(new FluidStack(ReactorFluids.get("fusion_plasma"), 100), IFluidHandler.FluidAction.EXECUTE);
		this.setChanged();
	}

	public void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		int dT = temperature-Tamb;
		if (dT != 0)
			temperature -= (1+dT/16384D);
	}

	public int getTemperature() {
		return temperature;
	}

	public void setTemperature(int temp) {
		temperature = temp;
	}

	public void addTemperature(int temp) {
		temperature += temp;
	}

	public FluidTank getPlasmaTank() {
		return tank;
	}

	public FluidTank getDeuteriumTank() {
		return h2;
	}

	public FluidTank getTritiumTank() {
		return h3;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return p.isStandardPipe() && side != Direction.UP;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == Direction.UP ? Flow.OUTPUT : Flow.INPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		FluidTank t = resource.is(ReactorFluids.get("deuterium")) ? h2 : resource.is(ReactorFluids.get("tritium")) ? h3 : null;
		if (t == null)
			return 0;
		int n = t.fill(resource, action);
		if (n > 0 && action.execute())
			this.setChanged();
		return n;
	}

	/** Original canDrain: only upwards, into a magnetic pipe. */
	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from != Direction.UP || !(level.getBlockEntity(worldPosition.above()) instanceof MagneticPipeBlockEntity))
			return FluidStack.EMPTY;
		FluidStack fs = tank.drain(maxDrain, action);
		if (!fs.isEmpty() && action.execute())
			this.setChanged();
		return fs;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 3;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return t == 0 ? h2.getFluid() : t == 1 ? h3.getFluid() : tank.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return t == 2 ? 8000 : 4000;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return t == 0 ? stack.is(ReactorFluids.get("deuterium")) : t == 1 && stack.is(ReactorFluids.get("tritium"));
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return FusionHeaterBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return resource.is(ReactorFluids.get("fusion_plasma")) ? FusionHeaterBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return FusionHeaterBlockEntity.this.drain(side, max, action);
			}
		};
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		FusionMultiBlock.breakAround(world, pos);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("temp", temperature);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.put("h2", h2.writeToNBT(registries, new CompoundTag()));
		NBT.put("h3", h3.writeToNBT(registries, new CompoundTag()));
		NBT.putBoolean("multi", hasMultiBlock);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		temperature = NBT.getInt("temp");
		for (String k : new String[]{"tank", "h2", "h3"}) {
			FluidTank t = k.equals("tank") ? tank : k.equals("h2") ? h2 : h3;
			t.setFluid(FluidStack.EMPTY);
			t.readFromNBT(registries, NBT.getCompound(k));
		}
		hasMultiBlock = NBT.getBoolean("multi");
	}

	/** Original TileEntityFusionHeater.allowExternalHeating (HeatConduction via TemperatureTE): false. */
	@Override
	public boolean allowExternalHeating() {
		return false;
	}

	/** Original TileEntityReactorBase.allowHeatExtraction: true. */
	@Override
	public boolean allowHeatExtraction() {
		return true;
	}
}
