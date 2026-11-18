/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryItems;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The multi-cannon, written for the port from the original's behaviour: a rotary cannon firing ball bearings, reaching 120 blocks. Every
 * 2 ticks one shot of 3 damage; one ball bearing lasts 10 shots.
 * Changes: own power need (1048576 W from below; the original's is not in its tables) and no spin-up or reload pauses.
 */
public class MultiCannonBlockEntity extends TurretBlockEntity {

	private int shotsLeft;

	public MultiCannonBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.MULTI_CANNON.get(), pos, state, 1, 1, 1048576);
	}

	@Override
	public int getRange() {
		return 120;
	}

	@Override
	public int getOperationTime() {
		return 2;
	}

	@Override
	public boolean isAmmo(ItemStack is) {
		return is.is(RotaryItems.part(CraftingPart.BALLBEARING));
	}

	@Override
	protected boolean canFire() {
		return shotsLeft > 0 || this.findAmmo() >= 0;
	}

	@Override
	protected void fire(Level world, Vec3 muzzle, LivingEntity target) {
		if (shotsLeft <= 0) {
			inv.extractItem(this.findAmmo(), 1, false);
			shotsLeft = 10;
		}
		shotsLeft--;
		target.invulnerableTime = 0; //a stream of small hits
		target.hurt(world.damageSources().magic(), 3F);
	}
}
