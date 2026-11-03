/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotarySounds;

/** Original: Reika.RotaryCraft.TileEntities.Engine.TileEntityMicroturbine. Burns jet fuel. */
public class MicroturbineBlockEntity extends EngineBlockEntity {

	public MicroturbineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.MICRO_ENGINE.get(), pos, state, EngineType.MICRO);
	}

	@Override
	protected void consumeFuel(float scale) {
		fuel.drain((int)(scale*this.getConsumedFuel()), IFluidHandler.FluidAction.EXECUTE);
	}

	@Override
	protected void internalizeFuel() {

	}

	@Override
	protected boolean getRequirements(Level world, BlockPos pos, int meta) {
		return this.getFuelLevel() > 0;
	}

	@Override
	public int getFuelLevel() {
		return fuel.getFluidAmount();
	}

	@Override
	protected void playSounds(Level world, BlockPos pos, float pitchMultiplier, float volume) {
		soundtick++;
		if (this.isMuffled(world, pos)) {
			volume *= 0.3125F;
		}

		if (soundtick < this.getSoundLength(1F/pitchMultiplier) && soundtick < 2000)
			return;
		soundtick = 0;

		volume *= 0.125F;
		RotarySounds.play(world, pos, RotarySounds.MICRO.get(), volume, pitchMultiplier);
	}

	@Override
	protected void affectSurroundings(Level world, BlockPos pos, int meta) {

	}
}
