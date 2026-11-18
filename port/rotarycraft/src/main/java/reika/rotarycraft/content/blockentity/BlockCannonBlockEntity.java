/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The block cannon, written for the port from the original's behaviour: with at least 65536 W from below it shoots the blocks in its ammo
 * slots as falling blocks at the target set in its GUI (they land there and settle like sand), up to sqrt(power/67.5) m/s, one every 20
 * ticks. Heavier blocks need more torque: 16*(hardness+1) Nm (blocks without hardness, like bedrock, cannot be shot).
 * Changes: the original's density table, spawner and fluid ammo are replaced by the hardness rule.
 */
public class BlockCannonBlockEntity extends AimedCannonBlockEntity {

	public BlockCannonBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.BLOCK_CANNON.get(), pos, state, 65536);
	}

	public static float hardness(ItemStack is) {
		return is.getItem() instanceof BlockItem bi ? bi.getBlock().defaultDestroyTime() : -1;
	}

	public int getRequiredTorque(ItemStack is) {
		float h = hardness(is);
		return h < 0 ? Integer.MAX_VALUE : (int)Math.ceil(16*(h+1));
	}

	@Override
	public boolean isAmmo(ItemStack is) {
		return hardness(is) >= 0;
	}

	@Override
	public double getMaxSpeed() {
		return Math.sqrt(power/67.5)/20;
	}

	@Override
	protected boolean fire(Level world, int slot, Vec3 start, Vec3 v) {
		ItemStack is = inv.getStackInSlot(slot);
		if (torque < this.getRequiredTorque(is) || !world.getBlockState(BlockPos.containing(start)).isAir())
			return false; //(FallingBlockEntity.fall takes the block at its spawn spot out of the world: only fire into air)
		BlockState st = ((BlockItem)is.getItem()).getBlock().defaultBlockState();
		FallingBlockEntity fb = FallingBlockEntity.fall(world, BlockPos.containing(start), st);
		fb.setPos(start.x, start.y, start.z);
		fb.setDeltaMovement(v);
		fb.hurtMarked = true;
		return true;
	}
}
