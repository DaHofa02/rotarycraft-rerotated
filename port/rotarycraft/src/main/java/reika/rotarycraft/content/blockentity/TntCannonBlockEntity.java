/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The TNT cannon, written for the port from the original's behaviour: with at least 65536 W from below it launches lit TNT from its ammo at
 * the target set in its GUI, up to sqrt(power/67.5) m/s (the original's launch speed), one every 20 ticks.
 * Changes: only the target mode (no manual angle and speed); the arc is aimed with the port's own ballistic solver.
 */
public class TntCannonBlockEntity extends AimedCannonBlockEntity {

	public TntCannonBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.TNT_CANNON.get(), pos, state, 65536);
	}

	@Override
	public boolean isAmmo(ItemStack is) {
		return is.is(Items.TNT);
	}

	@Override
	public double getMaxSpeed() {
		return Math.sqrt(power/67.5)/20;
	}

	@Override
	protected boolean fire(Level world, int slot, Vec3 start, Vec3 v) {
		PrimedTnt tnt = new PrimedTnt(world, start.x, start.y, start.z, null);
		tnt.setDeltaMovement(v);
		tnt.setFuse(80);
		return world.addFreshEntity(tnt);
	}
}
