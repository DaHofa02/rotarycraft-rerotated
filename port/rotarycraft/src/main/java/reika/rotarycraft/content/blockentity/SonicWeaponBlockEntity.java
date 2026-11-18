/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The sonic weapon, written for the port from the original's behaviour: with at least 262144 W from below it sends out sound of volume
 * 381.47 * torque (the original's units), every 10 ticks, to everything living within 16 blocks. The sound falls off with the square of the
 * distance; at a creature it causes blindness (from 1807500), damage (from 2971000), nausea and slowness (from 3906200) and death (from 10^8).
 * Players in creative mode or with a helmet are safe. From a volume of 400000 it also shatters glass up to min(20, 6*volume/400000) blocks
 * away (if machines may damage blocks).
 * Changes: no GUI to turn the volume down (it always runs at full volume), no pitch setting.
 */
public class SonicWeaponBlockEntity extends PowerReceiverBlockEntity {

	public static final double VOLUME_PER_TORQUE = 381.469726;
	public static final long EYE = 1807500, LUNG = 2971000, BRAIN = 3906200, KILL = 100000000;
	public static final int RANGE = 16;
	/** Tests switch the glass shattering off (it reaches 20 blocks, into neighbouring tests). */
	public static boolean allowBlockDamage = true;
	public int pulses;
	public int shattered;

	public SonicWeaponBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SONIC_WEAPON.get(), pos, state, 1, 1, 262144);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public double getVolume() {
		return power < MINPOWER ? 0 : VOLUME_PER_TORQUE*torque;
	}

	/** The sound arriving at that distance (blocks). */
	public double volumeAt(double dist) {
		return this.getVolume()/Math.max(1, dist*dist);
	}

	public static boolean isVulnerable(LivingEntity e) {
		return !(e instanceof Player p) || !p.isCreative() && p.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN;
		if (world.isClientSide)
			return;
		this.readPower();
		if (power < MINPOWER || this.getTicksExisted()%10 != 0)
			return;
		pulses++;
		Vec3 c = Vec3.atCenterOf(pos);
		for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RANGE), SonicWeaponBlockEntity::isVulnerable)) {
			double v = this.volumeAt(e.position().distanceTo(c));
			if (v >= KILL) {
				e.hurt(world.damageSources().magic(), Float.MAX_VALUE);
				continue;
			}
			if (v >= EYE)
				e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
			if (v >= LUNG)
				e.hurt(world.damageSources().magic(), 2F);
			if (v >= BRAIN) {
				e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 10));
				e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 3));
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
			}
		}
		double vol = this.getVolume();
		if (vol >= 400000 && allowBlockDamage && RotaryConfig.ATTACKBLOCKS.get()) {
			int r = (int)Math.min(20, 6*vol/400000);
			for (int i = 0; i < r; i++) {
				BlockPos p = pos.offset(world.random.nextInt(2*r+1)-r, world.random.nextInt(2*r+1)-r, world.random.nextInt(2*r+1)-r);
				BlockState s = world.getBlockState(p);
				if (s.is(BlockTags.IMPERMEABLE) || s.is(Blocks.GLASS_PANE) || s.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_PANES)) {
					world.destroyBlock(p, false);
					shattered++;
				}
			}
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}
}
