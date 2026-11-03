/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.fusion.ToroidMagnetBlockEntity.Aim;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: Reika.ReactorCraft.TileEntities.TileEntityFusionMarker - a building aid: with a redstone signal its renderer outlines
 * where the 36 magnets (yellow), 4 injectors (blue) and the solenoid ring (red) of a tokamak centred one block above it go.
 */
public class FusionMarkerBlockEntity extends RotaryCraftBlockEntity {

	public FusionMarkerBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.FUSION_MARKER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Fusion Marker";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	public boolean renderLines() {
		return level != null && level.hasNeighborSignal(worldPosition);
	}

	/** Original getAimPoints: the aim of each of the 40 ring places, starting at the east injector (every 10th is an injector). */
	public static List<Aim> getAimPoints() {
		List<Aim> li = new ArrayList<>();
		Aim[] seq = {Aim.N, Aim.N, Aim.NNW1, Aim.NNW2, Aim.NNW3, Aim.NW, Aim.WNW1, Aim.WNW2, Aim.WNW3, Aim.W, Aim.W, Aim.W, Aim.WSW1, Aim.WSW2,
				Aim.WSW3, Aim.SW, Aim.SSW1, Aim.SSW2, Aim.SSW3, Aim.S, Aim.S, Aim.S, Aim.SSE1, Aim.SSE2, Aim.SSE3, Aim.SE, Aim.ESE1, Aim.ESE2,
				Aim.ESE3, Aim.E, Aim.E, Aim.E, Aim.ENE1, Aim.ENE2, Aim.ENE3, Aim.NE, Aim.NNE1, Aim.NNE2, Aim.NNE3, Aim.N};
		for (Aim a : seq)
			li.add(a);
		return li;
	}
}
