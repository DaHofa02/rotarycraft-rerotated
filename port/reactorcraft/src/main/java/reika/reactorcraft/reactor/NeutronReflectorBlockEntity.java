/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;

/**
 * Original: Reika.ReactorCraft.TileEntities.TileEntityNeutronReflector (ReactorTiles.REFLECTOR, ReactorBlocks.REACTOR, a ReactorCoreTE
 * that does not tick). Every neutron entering it is moderated (thermal); one in four is sent back - its horizontal motion reversed - and
 * flies on, otherwise half of them are absorbed. A neutron that is not absorbed goes on through the usual block checks (NeutronEntity).
 */
public class NeutronReflectorBlockEntity extends ReactorBlockEntity implements NeutronTile {

	public NeutronReflectorBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.NEUTRON_REFLECTOR.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Neutron Reflector";
	}

	/** Original updateEntity: empty (isTickingTE false). */
	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {}

	/** Original onNeutron: moderate; 1 in 4 reflected (motionX and motionZ negated, velocityChanged), else absorbed with 50 %. */
	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		e.moderate();
		if (rand.nextInt(4) == 0) {
			Vec3 m = e.getDeltaMovement();
			e.setDeltaMovement(-m.x, m.y, -m.z);
			e.hurtMarked = true;
			return false;
		}
		return rand.nextBoolean();
	}
}
