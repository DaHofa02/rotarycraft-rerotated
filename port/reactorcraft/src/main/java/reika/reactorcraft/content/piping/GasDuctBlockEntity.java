/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.piping;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.capabilities.Capabilities;

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;

/**
 * Original: Reika.ReactorCraft.TileEntities.TileEntityGasDuct (ReactorTiles.GASPIPE, BlockDuct, drawn by DuctRenderer with a hardened clay
 * frame and glass windows) - a duct for any gaseous fluid (original Fluid.isGaseous, see {@link ReactorFluids#isGaseous}). It uses the
 * default TileEntityReactorPiping partners: other gas ducts and every fluid handler except other mods' pipes and conduits (recognised,
 * as in the original, by "pipe" or "conduit" in the class name). Fluid moves as in {@link ReactorPipingBlockEntity}: from machines on all
 * sides, into plain fluid handlers only sideways ({@link reika.rotarycraft.content.piping.PipeConnector} machines decide themselves).
 */
public class GasDuctBlockEntity extends ReactorPipingBlockEntity {

	private static final Map<Class<?>, Boolean> interactableClasses = new ConcurrentHashMap<>();

	public GasDuctBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.GAS_DUCT.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Gas Duct";
	}

	@Override
	public boolean isValidFluid(Fluid f) {
		return ReactorFluids.isGaseous(f);
	}

	@Override
	protected boolean isInteractableTile(@Nullable BlockEntity te, Direction dir) {
		return te != null && isInteractable(te.getLevel(), te, dir);
	}

	/** Original isConnected / isInteractableTile: another gas duct, or a fluid handler that is not another mod's pipe or conduit. */
	public static boolean connectsTo(Level world, BlockPos pos, Direction dir) {
		return isInteractable(world, world.getBlockEntity(pos.relative(dir)), dir);
	}

	private static boolean isInteractable(@Nullable Level world, @Nullable BlockEntity te, Direction dir) {
		if (te == null || world == null)
			return false;
		if (te.getClass() == GasDuctBlockEntity.class)
			return true;
		if (world.getCapability(Capabilities.FluidHandler.BLOCK, te.getBlockPos(), dir.getOpposite()) == null)
			return false;
		return interactableClasses.computeIfAbsent(te.getClass(), c -> {
			String name = c.getSimpleName().toLowerCase(Locale.ENGLISH);
			return !name.contains("conduit") && !name.contains("pipe");
		});
	}
}
