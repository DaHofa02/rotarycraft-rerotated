/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.block.FluoriteBlock;
import reika.reactorcraft.content.entity.RadiationEntity;
import reika.reactorcraft.registry.ReactorEffects;

/** Original: Reika.ReactorCraft.Auxiliary.RadiationEffects. */
public final class RadiationEffects {

	public static final ResourceKey<DamageType> RADIATION = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "radiation"));

	private RadiationEffects() {}

	/** Original radiationDamage: "died of radiation poisoning", bypasses armour and is absolute. */
	public static DamageSource damage(Level world) {
		return new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(RADIATION));
	}

	public static boolean isEntityImmuneToAll(LivingEntity e) {
		return e instanceof Player p && (p.isCreative() || p.isSpectator());
	}

	/** Original applyEffects: gives radiation sickness of that level unless the entity already has it. */
	public static boolean applyEffects(LivingEntity e, RadiationIntensity ri) {
		if (ri.causesHarm() && !e.hasEffect(ReactorEffects.RADIATION) && !isEntityImmuneToAll(e) && !ri.hasSufficientShielding(e)) {
			e.addEffect(getRadiationEffect(ri.potionDuration, ri));
			return true;
		}
		return false;
	}

	/** Original applyPulseEffects (a neutron passing through): one second of sickness. */
	public static void applyPulseEffects(LivingEntity e, RadiationIntensity ri) {
		if (!e.hasEffect(ReactorEffects.RADIATION) && !isEntityImmuneToAll(e) && !ri.hasSufficientShielding(e))
			e.addEffect(getRadiationEffect(20, ri));
	}

	private static MobEffectInstance getRadiationEffect(int duration, RadiationIntensity ri) {
		return new MobEffectInstance(ReactorEffects.RADIATION, duration, ri.ordinal());
	}

	private static int randomPlusMinus(RandomSource r, int base, int range) {
		return base-range+r.nextInt(2*range+1);
	}

	/**
	 * Original contaminateArea: sqrt(range)*density radiation clouds at random spots within range (with line of sight: the shielding
	 * blocks on the way may deflect it). Returns the fraction of clouds that landed farther than force from the centre.
	 */
	public static double contaminateArea(Level world, BlockPos c, int range, float density, double force, boolean los, RadiationIntensity ri) {
		if (world.isClientSide)
			return 1;
		RandomSource r = world.random;
		double frac = 1;
		int num = Math.max(1, (int)(Math.sqrt(range)*density));
		for (int i = 0; i < num; i++) {
			BlockPos p = new BlockPos(randomPlusMinus(r, c.getX(), range), randomPlusMinus(r, c.getY(), range), randomPlusMinus(r, c.getZ(), range));
			int tries = 0;
			while (los && !isValidRadiationPosition(world, c, p, 2) && tries++ < 1000)
				p = new BlockPos(randomPlusMinus(r, c.getX(), range), randomPlusMinus(r, c.getY(), range), randomPlusMinus(r, c.getZ(), range));
			if (Math.sqrt(p.distSqr(c)) <= force)
				frac -= 1D/num;
			RadiationEntity rad = new RadiationEntity(world, range, ri);
			rad.moveTo(p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, 0, 0);
			world.addFreshEntity(rad);
		}
		return frac;
	}

	private static boolean isValidRadiationPosition(Level world, BlockPos c, BlockPos d, double forceDist) {
		if (Math.sqrt(d.distSqr(c)) <= forceDist)
			return true;
		double chance = 1;
		Vec3 from = Vec3.atCenterOf(c), to = Vec3.atCenterOf(d);
		Vec3 step = to.subtract(from);
		int n = (int)Math.ceil(step.length()*2);
		BlockPos last = null;
		for (int i = 1; i <= n; i++) {
			BlockPos p = BlockPos.containing(from.add(step.scale(i/(double)n)));
			if (p.equals(last))
				continue;
			last = p;
			RadiationShield rs = RadiationShield.getFrom(world.getBlockState(p));
			if (rs != null)
				chance *= 1-rs.radiationDeflectChance/100D;
		}
		return chance > 0 && world.random.nextDouble() < chance;
	}

	/** Original transformBlock: high radiation kills plants (to dead bushes or dropping them), turns grass to dirt, activates fluorite. */
	public static void transformBlock(Level world, BlockPos p, RadiationIntensity ri) {
		if (world.isClientSide)
			return;
		BlockState s = world.getBlockState(p);
		if (s.isAir() || s.is(Blocks.DEAD_BUSH))
			return;
		if (ri.isAtLeast(RadiationIntensity.HIGHLEVEL)) {
			if (s.is(BlockTags.LEAVES))
				world.removeBlock(p, false);
			else if (s.is(Blocks.SUGAR_CANE) || s.is(Blocks.VINE) || s.is(Blocks.LILY_PAD) || s.is(BlockTags.SMALL_FLOWERS) || s.is(Blocks.WHEAT)
					|| s.is(Blocks.CARROTS) || s.is(Blocks.POTATOES) || s.is(Blocks.CACTUS) || s.is(Blocks.PUMPKIN) || s.is(Blocks.PUMPKIN_STEM)
					|| s.is(Blocks.MELON) || s.is(Blocks.MELON_STEM) || s.is(Blocks.COCOA))
				world.destroyBlock(p, true);
			else if (s.is(Blocks.SHORT_GRASS) || s.is(BlockTags.SAPLINGS))
				world.setBlockAndUpdate(p, Blocks.DEAD_BUSH.defaultBlockState());
			else if (s.is(Blocks.MOSSY_COBBLESTONE))
				world.setBlockAndUpdate(p, Blocks.COBBLESTONE.defaultBlockState());
			else if (s.is(Blocks.GRASS_BLOCK))
				world.setBlockAndUpdate(p, Blocks.DIRT.defaultBlockState());
			else if (s.getBlock() instanceof net.minecraft.world.level.block.InfestedBlock ib)
				world.setBlockAndUpdate(p, ib.getHostBlock().defaultBlockState());
		}
		if (s.getBlock() instanceof FluoriteBlock && !s.getValue(FluoriteBlock.ACTIVATED))
			world.setBlockAndUpdate(p, s.setValue(FluoriteBlock.ACTIVATED, true));
	}
}
