/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.effect;

import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import reika.reactorcraft.radiation.RadiationEffects;
import reika.reactorcraft.radiation.RadiationIntensity;

/**
 * Original: PotionRadiation ("Radiation Sickness") with its player tick handler. Once a second (tick 5 of 20) a chance of
 * health/maxHealth * 50 % (75 % on peaceful; *1.1 at high level, *1.25 lethal) to take 1 (peaceful 2) radiation damage; players get their
 * food capped to max(1, 8-2*level) and saturation to max(0, 4-level), lose every helpful effect, get nausea, poison and weaker jumps;
 * walk speed 0.075 (instead of 0.1) and jump boost -2 become attribute modifiers of the effect (-25 % speed, -0.1 jump strength; in the
 * port they apply to every entity, not only players). Milk does not cure it (original: no curative items).
 */
public class RadiationEffect extends MobEffect {

	private static final Set<Holder<MobEffect>> REMOVED = Set.of(MobEffects.REGENERATION, MobEffects.SATURATION, MobEffects.MOVEMENT_SPEED,
			MobEffects.DIG_SPEED, MobEffects.DAMAGE_BOOST, MobEffects.HEAL, MobEffects.JUMP, MobEffects.FIRE_RESISTANCE, MobEffects.DAMAGE_RESISTANCE,
			MobEffects.NIGHT_VISION);

	public RadiationEffect() {
		super(MobEffectCategory.HARMFUL, 0x111111);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity e, int level) {
		if (e.level().isClientSide)
			return true;
		if (e instanceof Player ep)
			playerTick(ep, level);
		if (e.tickCount%20 != 5)
			return true;
		boolean p = e.level().getDifficulty() == Difficulty.PEACEFUL;
		double c = p ? 75 : 50;
		if (level >= RadiationIntensity.HIGHLEVEL.ordinal())
			c *= 1.1;
		if (level >= RadiationIntensity.LETHAL.ordinal())
			c *= 1.25;
		if (e.getRandom().nextDouble()*100 < e.getHealth()/e.getMaxHealth()*c)
			e.hurt(RadiationEffects.damage(e.level()), p ? 2 : 1);
		if (e instanceof Player ep) {
			int food = ep.getFoodData().getFoodLevel();
			ep.getFoodData().setFoodLevel(Math.min(food, Math.max(1, 8-2*level)));
			ep.getFoodData().setSaturation(Math.min(ep.getFoodData().getSaturationLevel(), Math.max(0, 4-level)));
		}
		return true;
	}

	/** Original PotionMapCMEAvoidance (every player tick). */
	private static void playerTick(Player e, int level) {
		for (Holder<MobEffect> h : REMOVED)
			if (e.hasEffect(h))
				e.removeEffect(h);
		if (!e.hasEffect(MobEffects.CONFUSION))
			e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
		//re-applied every tick as in the original: the timer never reaches a damage tick, the hearts only look poisoned
		e.addEffect(new MobEffectInstance(MobEffects.POISON, 20, 0));
	}
}
