/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.piping;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * Original: Reika.RotaryCraft.TileEntities.Piping.TileEntityFuelLine - the obsidian fuel line. Carries jet fuel and ethanol
 * (the original also accepted other mods' fuels by name, which have no counterpart here).
 */
public class FuelLineBlockEntity extends PipingBlockEntity {

	private int fuel = 0;
	@Nullable
	private Fluid fluid;

	public FuelLineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FUEL_LINE.get(), pos, state);
	}

	@Override
	public PipeType getPipeType() {
		return PipeType.FUEL_LINE;
	}

	@Override
	public boolean canConnectToPipe(PipeType p, Direction dir) {
		return p == PipeType.FUEL_LINE || p == PipeType.VALVE || p == PipeType.SEPARATION || p == PipeType.BYPASS || p == PipeType.SUCTION;
	}

	@Nullable
	@Override
	public Fluid getFluidType() {
		return fluid;
	}

	@Override
	public int getFluidLevel() {
		return fuel;
	}

	@Override
	protected void setFluid(@Nullable Fluid f) {
		fluid = f;
	}

	@Override
	protected void setLevel(int amt) {
		fuel = amt;
	}

	@Override
	protected boolean interactsWithMachines() {
		return true;
	}

	@Override
	protected void onIntake(BlockEntity te) {}

	@Override
	public boolean isValidFluid(Fluid f) {
		return f.isSame(RotaryFluids.JET_FUEL.get()) || f.isSame(RotaryFluids.ETHANOL.get());
	}

	@Override
	public boolean canReceiveFromPipeOn(Direction side) {
		return true;
	}

	@Override
	public boolean canEmitToPipeOn(Direction side) {
		return true;
	}

	@Override
	public boolean canIntakeFromIFluidHandler(Direction side) {
		return side.getAxis() == Direction.Axis.Y;
	}

	@Override
	public boolean canOutputToIFluidHandler(Direction side) {
		return side.getAxis() != Direction.Axis.Y;
	}
}
