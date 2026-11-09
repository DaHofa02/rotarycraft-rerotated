/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.waste;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import reika.reactorcraft.content.piping.ReactorPipingBlockEntity;
import reika.reactorcraft.reactor.ThoriumCoreBlockEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;

/**
 * Original: Reika.ReactorCraft.TileEntities.Waste.TileEntityWastePipe with TileEntityReactorPiping - a concrete duct for liquid nuclear
 * waste. It connects to other waste pipes and thorium cores (and the RotaryCraft crystallizer, not ported yet); each tick it takes a
 * quarter (+1) of the level difference from its neighbours (from machines what they would give) and gives a quarter (+1, keeping 5 mB)
 * of its level to lower neighbours and machines that take it (see {@link ReactorPipingBlockEntity}).
 */
public class WastePipeBlockEntity extends ReactorPipingBlockEntity {

	public WastePipeBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.WASTE_PIPE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Waste Pipe";
	}

	@Override
	public boolean isValidFluid(Fluid f) {
		return f.isSame(ReactorFluids.get("nuclear_waste"));
	}

	/** Original isWasteAcceptingBlock: waste pipes and thorium cores. */
	public static boolean connectsTo(@Nullable BlockEntity te) {
		return te instanceof WastePipeBlockEntity || te instanceof ThoriumCoreBlockEntity;
	}

	@Override
	protected boolean isInteractableTile(@Nullable BlockEntity te, Direction dir) {
		return connectsTo(te);
	}
}
