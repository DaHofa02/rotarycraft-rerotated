/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The freeze gun, written for the port from the original's behaviour: needs 262144 W at 256 Nm or more from below, reaches 64 blocks; each
 * shot uses a snowball and freezes the target in place for 10 seconds (put out, unable to move or jump) - a snow block in its slots turns
 * into 4 snowballs, ice into 16, when there is room.
 * Changes: the freeze is the slowness/jump effects at full strength (the original had its own freeze effect).
 */
public class FreezeGunBlockEntity extends TurretBlockEntity {

	public static final int FREEZE_TICKS = 200;

	public FreezeGunBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FREEZE_GUN.get(), pos, state, 256, 1, 262144);
	}

	@Override
	public int getRange() {
		return 64;
	}

	@Override
	public int getOperationTime() {
		return 10;
	}

	@Override
	public boolean isAmmo(ItemStack is) {
		return is.is(Items.SNOWBALL) || is.is(Items.SNOW_BLOCK) || is.is(Items.ICE);
	}

	private int findSnowball() {
		for (int i = 0; i < inv.getSlots(); i++)
			if (inv.getStackInSlot(i).is(Items.SNOWBALL))
				return i;
		return -1;
	}

	/** Breaks one snow or ice block down into snowballs if they fit. */
	private void makeSnowballs() {
		for (int i = 0; i < inv.getSlots(); i++) {
			ItemStack is = inv.getStackInSlot(i);
			int n = is.is(Items.SNOW_BLOCK) ? 4 : is.is(Items.ICE) ? 16 : 0;
			if (n > 0) {
				ItemStack balls = new ItemStack(Items.SNOWBALL, n);
				for (int k = 0; k < inv.getSlots(); k++)
					if (k != i && inv.insertItem(k, balls, true).isEmpty()) {
						inv.extractItem(i, 1, false);
						inv.insertItem(k, balls, false);
						return;
					}
			}
		}
	}

	@Override
	protected boolean canFire() {
		if (this.findSnowball() < 0)
			this.makeSnowballs();
		return this.findSnowball() >= 0;
	}

	@Override
	protected void fire(Level world, Vec3 muzzle, LivingEntity target) {
		inv.extractItem(this.findSnowball(), 1, false);
		target.clearFire();
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_TICKS, 9));
		target.addEffect(new MobEffectInstance(MobEffects.JUMP, FREEZE_TICKS, 128));
		target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze()));
	}
}
