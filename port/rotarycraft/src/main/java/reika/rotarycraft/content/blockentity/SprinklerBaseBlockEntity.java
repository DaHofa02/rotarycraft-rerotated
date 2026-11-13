/*******************************************************************************
 * Base of the RotaryCraft sprinklers. Ported from Reika.RotaryCraft.Base.TileEntity.SprinklerBlock (RotaryCraft 1.7.10 by Reika
 * Kalseki); private NeoForge 1.21.1 port, not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.rotarycraft.content.piping.PipeBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original SprinklerBlock: draws water from the standard pipe on its pipe side (a quarter of the pipe's water + 1 a tick, up to its
 * small capacity) and takes over the pipe's pressure; range = pressure/80, at most 8. While it has range and water it performs its
 * effects every tick and uses its water consumption.
 */
public abstract class SprinklerBaseBlockEntity extends RotaryCraftBlockEntity implements PipeConnector {

	private int liquid;
	private int pressure;
	private int soundTimer;

	protected SprinklerBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public abstract int getCapacity();

	public abstract int getWaterConsumption();

	public abstract Direction getPipeDirection();

	protected abstract void performEffects(Level world, BlockPos pos);

	protected void doAnimations() {

	}

	private void getLiq(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos.relative(this.getPipeDirection())) instanceof PipeBlockEntity tile && tile.getFluidType() != null
				&& tile.getFluidType().isSame(Fluids.WATER) && tile.getFluidLevel() > 0) {
			if (liquid < this.getCapacity()) {
				int toadd = Math.min(tile.getFluidLevel()/4+1, this.getCapacity()-liquid);
				tile.removeLiquid(toadd);
				liquid = Math.max(liquid+toadd, 0);
			}
			pressure = tile.getPressure();
		}
		liquid = Math.min(liquid, this.getCapacity());
	}

	@Override
	protected final void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (!world.isClientSide)
			this.getLiq(world, pos);
		if (this.canPerformEffects()) {
			this.performEffects(world, pos);
			if (!world.isClientSide && ++soundTimer >= 40) {
				soundTimer = 0;
				RotarySounds.play(world, pos, RotarySounds.SPRINKLER.get(), 1, 1);
			}
			if (!world.isClientSide)
				liquid -= this.getWaterConsumption();
		}
		this.doAnimations();
		if (!world.isClientSide && world.getGameTime()%8 == 0)
			this.syncAllData();
	}

	public final boolean canPerformEffects() {
		return this.getRange() > 0 && liquid >= this.getWaterConsumption();
	}

	public final int getWater() {
		return liquid;
	}

	public final int getPressure() {
		return pressure;
	}

	public final int getRange() {
		return pressure <= 0 ? 0 : Math.min(this.getMaxRange(), pressure/80);
	}

	public final int getMaxRange() {
		return 8;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public final boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public final boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side == this.getPipeDirection() && p.isStandardPipe();
	}

	@Override
	public final Flow getFlowForSide(Direction side) {
		return side == this.getPipeDirection() ? Flow.INPUT : Flow.NONE;
	}

	@Override
	public final int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (!resource.is(Fluids.WATER))
			return 0;
		int toadd = Math.min(resource.getAmount(), this.getCapacity()-liquid);
		if (action.execute())
			liquid += toadd;
		return toadd;
	}

	@Override
	public final FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	/** Water in on the pipe side only. */
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 1;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return liquid > 0 ? new FluidStack(Fluids.WATER, liquid) : FluidStack.EMPTY;
			}

			@Override
			public int getTankCapacity(int t) {
				return SprinklerBaseBlockEntity.this.getCapacity();
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return stack.is(Fluids.WATER);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return side == SprinklerBaseBlockEntity.this.getPipeDirection() ? SprinklerBaseBlockEntity.this.fill(side, resource, action) : 0;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return FluidStack.EMPTY;
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("press", pressure);
		NBT.putInt("lvl", liquid);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		pressure = Math.max(0, NBT.getInt("press"));
		liquid = Math.max(0, Math.min(this.getCapacity(), NBT.getInt("lvl")));
	}
}
