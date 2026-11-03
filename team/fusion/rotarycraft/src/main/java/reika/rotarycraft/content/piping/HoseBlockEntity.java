/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.piping;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import reika.rotarycraft.lib.world.TemperatureEnvironment;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * Original: Reika.RotaryCraft.TileEntities.Piping.TileEntityHose - the wooden lubricant hose. When fire consumes it, it and
 * (with 2/3 chance each) the adjacent hoses burn; a filled hose can explode.
 */
public class HoseBlockEntity extends PipingBlockEntity {

	private int lubricant = 0;
	private int burnIn = 0;

	public HoseBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.HOSE.get(), pos, state);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		super.updateEntity(world, pos, state);
		if (world.isClientSide || world.getBlockEntity(pos) != this)
			return;
		if (burnIn > 0) {
			burnIn--;
			if (burnIn == 0)
				this.doBurn();
		}
	}

	@Override
	public PipeType getPipeType() {
		return PipeType.HOSE;
	}

	@Override
	public boolean canConnectToPipe(PipeType p, Direction dir) {
		return p == PipeType.HOSE || p == PipeType.VALVE || p == PipeType.SEPARATION || p == PipeType.SUCTION;
	}

	@Nullable
	@Override
	public Fluid getFluidType() {
		return lubricant > 0 ? RotaryFluids.LUBRICANT.get() : null;
	}

	@Override
	public int getFluidLevel() {
		return lubricant;
	}

	@Override
	protected void setFluid(@Nullable Fluid f) {}

	@Override
	protected void setLevel(int amt) {
		lubricant = amt;
	}

	@Override
	protected boolean interactsWithMachines() {
		return true;
	}

	@Override
	protected void onIntake(BlockEntity te) {}

	@Override
	public boolean isValidFluid(Fluid f) {
		return f.isSame(RotaryFluids.LUBRICANT.get());
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

	/** Original: RotaryEventManager.burnLubricantHose, on BlockConsumedByFireEvent. */
	public void burn() {
		this.burn(true);
	}

	private void burn(boolean immediate) {
		TemperatureEnvironment.ignite(level, worldPosition);
		if (immediate) {
			this.doBurn();
		}
		else {
			int time = this.hasLiquid() ? 5+rand.nextInt(15) : 10+rand.nextInt(30);
			if (burnIn <= 0)
				burnIn = time;
		}
	}

	private void doBurn() {
		for (Direction d : Direction.values()) {
			if (rand.nextInt(3) > 0) {
				if (this.getAdjacentTileEntity(d) instanceof HoseBlockEntity h)
					h.burn(false);
			}
		}
		boolean full = this.hasLiquid();
		level.setBlockAndUpdate(worldPosition, Blocks.FIRE.defaultBlockState());
		if (full && rand.nextInt(4) == 0)
			level.explode(null, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, 2, true, Level.ExplosionInteraction.BLOCK);
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		NBT.putInt("burn", burnIn);
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		burnIn = NBT.getInt("burn");
	}
}
