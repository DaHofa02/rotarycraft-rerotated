/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * The steam turbine, written for the port from the original's behaviour: the converter engine of the magnetostatic engine (same tiers,
 * upgrades, speed choice, redstone mode, liquid nitrogen cooling) running on steam from another mod (fluid tag c:steam) taken in at its back,
 * up to 300000 mB stored. It uses the steam that carries power/20 J a tick divided by its efficiency (half the magnetostatic engine's), at
 * the energy the boiler puts into steam ({@link BoilerBlockEntity#JOULES_PER_STEAM} J per mB).
 * Changes: the original used ceil(sqrt(power)) mB a tick (balanced against Railcraft's steam); with the port's own boiler that would cost
 * hundreds of times the power at low tiers, so the use follows the steam's energy (about the same at the top tier).
 */
public class SteamTurbineBlockEntity extends MagneticEngineBlockEntity {

	public static final int CAPACITY = 300000;

	private final IFluidHandler steamIn = new IFluidHandler() {
		@Override
		public int getTanks() {
			return 2;
		}

		@Override
		public FluidStack getFluidInTank(int i) {
			return i == 0 ? FluidStack.EMPTY : getCoolantTank().getFluid();
		}

		@Override
		public int getTankCapacity(int i) {
			return i == 0 ? CAPACITY : getCoolantTank().getCapacity();
		}

		@Override
		public boolean isFluidValid(int i, FluidStack fs) {
			return i == 0 ? BoilerBlockEntity.isSteam(fs.getFluid()) : fs.is(RotaryFluids.LIQUID_NITROGEN.get());
		}

		@Override
		public int fill(FluidStack fs, FluidAction action) {
			return SteamTurbineBlockEntity.this.fillSteam(fs, action);
		}

		@Override
		public FluidStack drain(FluidStack fs, FluidAction action) {
			return FluidStack.EMPTY;
		}

		@Override
		public FluidStack drain(int max, FluidAction action) {
			return FluidStack.EMPTY;
		}
	};

	public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.STEAM_TURBINE.get(), pos, state);
	}

	public int fillSteam(FluidStack fs, IFluidHandler.FluidAction action) {
		if (fs.is(RotaryFluids.LIQUID_NITROGEN.get()))
			return getCoolantTank().fill(fs, action);
		if (!BoilerBlockEntity.isSteam(fs.getFluid()))
			return 0;
		int amt = Math.max(0, Math.min(fs.getAmount(), CAPACITY-stored));
		if (action.execute() && amt > 0) {
			stored += amt;
			this.setChanged();
		}
		return amt;
	}

	@Override
	public int getMaxStorage() {
		return CAPACITY;
	}

	/** Steam (mB) a tick at the chosen speed. */
	@Override
	public int getConsumption() {
		long p = (long)this.getTargetSpeed()*torqueOf(this.getTier());
		return (int)Math.ceil(p/20D/(efficiencyOf(this.getTier(), this.isEfficient())*0.5)/BoilerBlockEntity.JOULES_PER_STEAM);
	}

	@Nullable
	@Override
	public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
		return null;
	}

	@Override
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return side != null && write != null && side == write ? null : steamIn;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack fs, IFluidHandler.FluidAction action) {
		return from != null && from == write ? 0 : this.fillSteam(fs, action);
	}
}
