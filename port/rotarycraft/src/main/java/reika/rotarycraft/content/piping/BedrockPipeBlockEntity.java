/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.piping;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: Reika.RotaryCraft.TileEntities.Piping.TileEntityBedrockPipe - carries every fluid, never bursts, melts only at 5000 °C. */
public class BedrockPipeBlockEntity extends PipeBlockEntity {

	public BedrockPipeBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.BEDROCK_PIPE.get(), pos, state);
	}

	@Override
	public int getMaxPressure() {
		return Integer.MAX_VALUE;
	}

	@Override
	public int getMaxTemperature() {
		return 5000;
	}

	@Override
	public PipeType getPipeType() {
		return PipeType.BEDROCK_PIPE;
	}

	@Override
	public boolean isValidFluid(Fluid f) {
		return true;
	}
}
