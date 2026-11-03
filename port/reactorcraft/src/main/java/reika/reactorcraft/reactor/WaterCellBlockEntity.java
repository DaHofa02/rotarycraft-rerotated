/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.reactorcraft.content.block.ReactorCoreBlock;
import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.blockentity.ReservoirBlockEntity;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fission.TileEntityWaterCell (Coolant Cell) - holds 1000 mB of water, heavy water, sodium or
 * LiFBe; an empty cell takes it from a RotaryCraft reservoir (or any fluid handler: water / heavy water) above it, and its liquid falls
 * into an empty cell below. Once a second it loses an eighth of its excess heat and, unless it holds heavy water, takes half of the
 * difference from every hotter neighbour that can dump heat into its liquid - the liquid then boils off with 1/5 chance. Heavy water
 * moderates (slows) passing neutrons.
 */
public class WaterCellBlockEntity extends ReactorBlockEntity implements Temperatured, NeutronTile, ReactorBlock {

	private LiquidStates internalLiquid = LiquidStates.EMPTY;

	public WaterCellBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.WATER_CELL.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Coolant Cell";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		boolean thermal = this.thermalTick();
		if (world.isClientSide)
			return;
		if (world.getBlockEntity(pos.below()) instanceof WaterCellBlockEntity te) {
			if (te.getLiquidState() == LiquidStates.EMPTY && this.getLiquidState() != LiquidStates.EMPTY) {
				te.setLiquidState(this.getLiquidState());
				this.setLiquidState(LiquidStates.EMPTY);
			}
		}
		BlockEntity up = world.getBlockEntity(pos.above());
		if (up instanceof ReservoirBlockEntity res) {
			if (res.getFluidAmount() >= 1000) {
				Fluid f = res.getFluid();
				LiquidStates lq = f != null ? LiquidStates.getState(f) : null;
				if (lq != null && internalLiquid == LiquidStates.EMPTY) {
					res.getFluidHandler(Direction.DOWN).drain(1000, IFluidHandler.FluidAction.EXECUTE);
					this.setLiquidState(lq);
				}
			}
		}
		if (thermal)
			this.updateTemperature(world, pos);
		if (this.getLiquidState() == LiquidStates.EMPTY && !(up instanceof ReservoirBlockEntity)) {
			IFluidHandler ic = world.getCapability(Capabilities.FluidHandler.BLOCK, pos.above(), Direction.DOWN);
			if (ic != null) {
				FluidStack liq = ic.drain(1000, IFluidHandler.FluidAction.SIMULATE);
				if (liq.getAmount() >= 1000) {
					if (liq.getFluid().isSame(Fluids.WATER)) {
						ic.drain(1000, IFluidHandler.FluidAction.EXECUTE);
						this.setLiquidState(LiquidStates.WATER);
					}
					else if (liq.getFluid().isSame(ReactorFluids.get("heavy_water"))) {
						ic.drain(1000, IFluidHandler.FluidAction.EXECUTE);
						this.setLiquidState(LiquidStates.HEAVY);
					}
				}
			}
		}
	}

	@Override
	protected void updateTemperature(Level world, BlockPos pos) {
		super.updateTemperature(world, pos);
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		int dT = temperature-Tamb;
		if (dT > 0)
			temperature -= dT/8;
		for (Direction dir : Direction.values()) {
			if (world.getBlockEntity(pos.relative(dir)) instanceof Temperatured tr) {
				if (internalLiquid != LiquidStates.HEAVY && tr.canDumpHeatInto(internalLiquid)) {
					int t = tr.getTemperature();
					int dt = t-temperature;
					if (dt > 0) {
						temperature += dt/2;
						tr.setTemperature(t-dt/2);
						if (rand.nextInt(5) == 0)
							this.setLiquidState(LiquidStates.EMPTY);
					}
				}
			}
		}
		this.setChanged();
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		if (this.getLiquidState() == LiquidStates.HEAVY)
			e.moderate();
		return false;
	}

	public LiquidStates getLiquidState() {
		return internalLiquid;
	}

	public void setLiquidState(LiquidStates liq) {
		internalLiquid = liq;
		if (level != null && !level.isClientSide) {
			ReactorCoreBlock.setTextureState(level, worldPosition, liq.ordinal());
			this.setChanged();
			this.syncAllData();
		}
	}

	@Override
	public int getMaxTemperature() {
		return 1000;
	}

	@Override
	public boolean canDumpHeatInto(LiquidStates liq) {
		return liq != LiquidStates.EMPTY && this.getLiquidState().isWater() == liq.isWater();
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("liq", internalLiquid.ordinal());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		internalLiquid = LiquidStates.list[Math.min(NBT.getInt("liq"), LiquidStates.list.length-1)];
	}
}
