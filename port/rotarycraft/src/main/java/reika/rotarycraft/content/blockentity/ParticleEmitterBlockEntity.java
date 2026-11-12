/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Decorative.TileEntityParticleEmitter - runs on a wound coil (one charge per 600 ticks times
 * the spring's stiffness) and puts out three particles per tick of the chosen kind in the 1x4x1 column from two blocks above it. The
 * kind is picked in its GUI. The original's useRedstone flag is never set anywhere and is not ported.
 */
public class ParticleEmitterBlockEntity extends SpringPoweredBlockEntity {

	/**
	 * Original ReikaParticleHelper, in its order (the ordinal picks the icon in particles.png). The 1.7.10 particles that no longer
	 * exist are their nearest 1.21 kin: townaura -> mycelium, footstep -> white ash, mobSpell(Ambient) -> entity effect.
	 */
	public enum Kind {
		SMOKE, CRITICAL, ENCHANTMENT, FLAME, REDSTONE, BONEMEAL, BUBBLE, VOID, LARGESMOKE, SNOWBALL, PORTAL, RAIN, DRIPWATER, DRIPLAVA,
		EXPLODE, HEART, CLOUD, NOTE, SGA, LAVA, SPRINT, SLIME, FIREWORK, SUSPEND, MOBSPELL, AMBIENTMOBSPELL, SPELL, INSTANTSPELL, WITCH,
		SNOWSHOVEL, ANGRY;

		/** Original TileEntityParticleEmitter.isParticleValid. */
		public boolean isValid() {
			return this != DRIPLAVA && this != DRIPWATER && this != SUSPEND && this != SNOWSHOVEL;
		}

		public ParticleOptions particle() {
			return switch (this) {
				case SMOKE -> ParticleTypes.SMOKE;
				case CRITICAL -> ParticleTypes.CRIT;
				case ENCHANTMENT -> ParticleTypes.ENCHANTED_HIT;
				case FLAME -> ParticleTypes.FLAME;
				case REDSTONE -> DustParticleOptions.REDSTONE;
				case BONEMEAL -> ParticleTypes.HAPPY_VILLAGER;
				case BUBBLE -> ParticleTypes.BUBBLE;
				case VOID -> ParticleTypes.MYCELIUM;
				case LARGESMOKE -> ParticleTypes.LARGE_SMOKE;
				case SNOWBALL -> ParticleTypes.ITEM_SNOWBALL;
				case PORTAL -> ParticleTypes.PORTAL;
				case RAIN -> ParticleTypes.SPLASH;
				case DRIPWATER -> ParticleTypes.DRIPPING_WATER;
				case DRIPLAVA -> ParticleTypes.DRIPPING_LAVA;
				case EXPLODE -> ParticleTypes.EXPLOSION_EMITTER;
				case HEART -> ParticleTypes.HEART;
				case CLOUD -> ParticleTypes.CLOUD;
				case NOTE -> ParticleTypes.NOTE;
				case SGA -> ParticleTypes.ENCHANT;
				case LAVA -> ParticleTypes.LAVA;
				case SPRINT -> ParticleTypes.WHITE_ASH;
				case SLIME -> ParticleTypes.ITEM_SLIME;
				case FIREWORK -> ParticleTypes.FIREWORK;
				case SUSPEND -> ParticleTypes.UNDERWATER;
				case MOBSPELL -> ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 1F, 1F, 1F);
				case AMBIENTMOBSPELL -> ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0x26FFFFFF);
				case SPELL -> ParticleTypes.EFFECT;
				case INSTANTSPELL -> ParticleTypes.INSTANT_EFFECT;
				case WITCH -> ParticleTypes.WITCH;
				case SNOWSHOVEL -> ParticleTypes.SNOWFLAKE;
				case ANGRY -> ParticleTypes.ANGRY_VILLAGER;
			};
		}
	}

	public Kind particleType = Kind.SMOKE;
	public int particlesPerTick = 3;

	public ParticleEmitterBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.PARTICLE_EMITTER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Particle Emitter";
	}

	@Override
	public int getBaseDischargeTime() {
		return 600;
	}

	public boolean canEmit() {
		return this.hasCoil();
	}

	/** Original PacketHandlerCore PARTICLES: only the kinds the GUI offers. */
	public void setParticleType(Kind k) {
		if (k.isValid() && k != particleType) {
			particleType = k;
			this.syncAllData();
			this.setChanged();
		}
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (this.hasCoil()) {
			tickcount++;
			if (tickcount > this.getUnwindTime()) {
				this.decrementCharge();
				tickcount = 0;
			}
		}
		if (world instanceof ServerLevel sl && this.canEmit()) {
			for (int i = 0; i < particlesPerTick; i++)
				sl.sendParticles(particleType.particle(), pos.getX()+rand.nextDouble(), pos.getY()+2+rand.nextDouble()*4, pos.getZ()+rand.nextDouble(), 1, 0, 0, 0, 0);
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("type", particleType.ordinal());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		int t = NBT.getInt("type");
		particleType = t >= 0 && t < Kind.values().length ? Kind.values()[t] : Kind.SMOKE;
	}
}
