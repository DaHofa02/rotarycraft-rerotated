/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import java.util.Comparator;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.reactor.ReactorBlockEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;

/**
 * Original: Reika.ReactorCraft.TileEntities.TileEntitySteamDiffuser (ReactorTiles.DIFFUSER, "Steam Diffuser"). It takes ReactorCraft steam
 * from the steam line it faces (a quarter of the difference +1 per tick, times the working fluid's efficiency) and turns it into the
 * fluid "steam" of other mods: per tick min(1 + steam/4, free space/RATIO) units, each RATIO*1000 mB (RATIO = ceil(1.696/0.01272*0.6) = 80),
 * into a 2500 B tank that pipes drain from the back. The original looked the fluid up by name (FluidRegistry "steam", registered by e.g.
 * Railcraft, never by Reika's mods); here it is a fluid in the common tag c:steam (e.g. Mekanism's steam), the first by registry name.
 * Without such a fluid the diffuser does nothing, as the original did without a steam mod. As in the original, the units are used up even
 * when the tank cannot take all of their steam (the amount is cut to the free space); the product is computed in long instead of
 * overflowing int.
 */
public class SteamDiffuserBlockEntity extends ReactorBlockEntity implements SteamTile, PipeConnector {

	public static final int RATIO = (int)Math.ceil(1.696/0.01272*0.6);
	public static final int CAPACITY = 2500000;
	public static final TagKey<Fluid> STEAM_TAG = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "steam"));

	private final FluidTank tank = new FluidTank(CAPACITY);
	private int steam;

	public SteamDiffuserBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.STEAM_DIFFUSER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Steam Diffuser";
	}

	/** Original getFacing: metadata 0 west, 1 east, 2 north, 3 south - the side of the steam line. */
	public Direction getFacing() {
		return switch (this.getBlockMetadata()) {
			case 0 -> Direction.WEST;
			case 1 -> Direction.EAST;
			case 2 -> Direction.NORTH;
			default -> Direction.SOUTH;
		};
	}

	/** The source fluid of the first entry (by registry name) of the c:steam tag, or null. */
	@Nullable
	public static Fluid getSteamFluid() {
		return BuiltInRegistries.FLUID.getTag(STEAM_TAG).flatMap(set -> set.stream().map(Holder::value)
				.filter(f -> f.isSource(f.defaultFluidState()))
				.min(Comparator.comparing(f -> BuiltInRegistries.FLUID.getKey(f).toString()))).orElse(null);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		this.getSteam(world, pos);
		this.convertSteam();
	}

	private void convertSteam() {
		if (steam <= 0)
			return;
		Fluid f = getSteamFluid();
		if (f == null)
			return;
		int n = Math.min(1+steam/4, (tank.getCapacity()-tank.getFluidAmount())/RATIO);
		if (n > 0) {
			// original HybridTank.addLiquid: the amount is cut to the capacity, then filled (only the same fluid, at most the free space)
			int amt = (int)Math.min((long)n*RATIO*1000, tank.getCapacity());
			tank.fill(new FluidStack(f, amt), IFluidHandler.FluidAction.EXECUTE);
			this.setChanged();
		}
		steam -= n;
	}

	private void getSteam(Level world, BlockPos pos) {
		BlockPos p = pos.relative(this.getFacing());
		if (!world.isLoaded(p) || !(world.getBlockEntity(p) instanceof SteamLineBlockEntity te))
			return;
		int dS = te.getSteam()-steam;
		if (dS > 0) {
			int rm = dS/4+1;
			steam = (int)(steam+rm*te.getWorkingFluid().efficiency);
			te.removeSteam(rm);
		}
	}

	@Override
	public int getSteam() {
		return steam;
	}

	public FluidTank getTank() {
		return tank;
	}

	/** Original canDrain: only from the back (opposite the steam line). */
	private boolean isOutputSide(@Nullable Direction side) {
		return side == this.getFacing().getOpposite();
	}

	/** Fluid handler for the back side (drain only, like the original IFluidHandler). */
	@Nullable
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		if (!this.isOutputSide(side))
			return null;
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 1;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return tank.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return tank.getCapacity();
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return false;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return 0;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				FluidStack d = tank.drain(resource, action);
				if (action.execute() && !d.isEmpty())
					SteamDiffuserBlockEntity.this.setChanged();
				return d;
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				FluidStack d = tank.drain(max, action);
				if (action.execute() && !d.isEmpty())
					SteamDiffuserBlockEntity.this.setChanged();
				return d;
			}
		};
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p) && this.isOutputSide(side);
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (!this.isOutputSide(from))
			return FluidStack.EMPTY;
		FluidStack d = tank.drain(maxDrain, action);
		if (action.execute() && !d.isEmpty())
			this.setChanged();
		return d;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return this.isOutputSide(side) ? Flow.OUTPUT : Flow.NONE;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("energy", steam);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		steam = NBT.getInt("energy");
		tank.readFromNBT(registries, NBT.getCompound("tank"));
	}
}
