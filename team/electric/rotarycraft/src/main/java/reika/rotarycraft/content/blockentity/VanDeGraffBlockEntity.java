/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.api.interfaces.Shockable;
import reika.rotarycraft.content.entity.DischargeEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryDamage;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Weaponry.TileEntityVanDeGraff - powered from below, it gains 4*sqrt(P) coulombs per tick; its
 * range is charge/1024 blocks (up to 16). With range it first sparks, chosen at random by weight, into a neighbour (sides and top:
 * Shockable machines 1000, metal blocks 50, water 20, TNT 100 - which it ignites -, otherwise nothing; "no target" weight 1), else into
 * a long-range Shockable 2 or 3 blocks above; otherwise it strikes the closest living thing in range (players in creative excepted):
 * 1 + charge^2/2^25 damage ("electrified", through armour; a full chain suit blocks it, full leather halves it; creepers explode).
 * Every strike uses up the charge. Above 2097152 C it blows up with lightning; in the rain it loses half its charge every tick.
 */
public class VanDeGraffBlockEntity extends PowerReceiverBlockEntity {

	/** In coulombs. */
	private int charge;

	private final List<Direction> sideTargets = new ArrayList<>();
	private final List<Integer> sideWeights = new ArrayList<>();

	public VanDeGraffBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.VAN_DE_GRAFF.get(), pos, state, 0, 0, 0);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	/**
	 * Original updateSidedMappings (on the first tick and on neighbour changes; here every tick, which picks the same way). Metal stands
	 * in for the original Material.iron / anvil (no materials in 1.21).
	 */
	private void updateSidedMappings(Level world, BlockPos pos) {
		sideTargets.clear();
		sideWeights.clear();
		for (Direction dir : new Direction[] {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
			BlockPos p = pos.relative(dir);
			BlockState b = world.getBlockState(p);
			int w = 0;
			if (!b.isAir()) {
				BlockEntity te = world.getBlockEntity(p);
				if (te instanceof VanDeGraffBlockEntity)
					w = 0;
				else if (te instanceof Shockable)
					w = 1000;
				else if (b.getSoundType(world, p, null) == SoundType.METAL || b.getSoundType(world, p, null) == SoundType.ANVIL)
					w = 50;
				else if (b.getFluidState().is(FluidTags.WATER))
					w = 20;
				else if (b.is(Blocks.TNT))
					w = 100;
			}
			sideTargets.add(dir);
			sideWeights.add(w);
		}
		sideTargets.add(null); //original ForgeDirection.UNKNOWN
		sideWeights.add(1);
	}

	/** Original WeightedRandom.getRandomEntry. */
	@Nullable
	private Direction randomSide(Level world) {
		int total = 0;
		for (int w : sideWeights)
			total += w;
		int r = world.random.nextInt(total);
		for (int i = 0; i < sideTargets.size(); i++) {
			r -= sideWeights.get(i);
			if (r < 0)
				return sideTargets.get(i);
		}
		return null;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		read = Direction.DOWN; //original getPowerBelow
		this.readPower();

		charge = (int)(charge+4*Math.sqrt(power));

		int r = this.getRange();

		if (r > 0) {
			this.updateSidedMappings(world, pos);
			Direction dir = this.randomSide(world);
			if (dir != null) {
				this.shock(world, pos, dir);
				return;
			}
			for (int i = 2; i < 4; i++) {
				if (world.getBlockEntity(pos.above(i)) instanceof Shockable s && s.canDischargeLongRange()) {
					this.dischargeToBlock(pos.above(i), s);
					return;
				}
			}
		}
		if (charge <= 0)
			return;

		AABB box = new AABB(pos).inflate(r);
		LivingEntity e = this.getClosestTarget(world, pos.getX()+0.5, pos.getY()+0.75, pos.getZ()+0.5, box);
		if (e != null) {
			world.addFreshEntity(new DischargeEntity(world, pos.getX()+0.5, pos.getY()+0.75, pos.getZ()+0.5, charge, e.getX(), e.getY()+e.getEyeHeight()*0.8, e.getZ()));
			this.shock(e);
			charge = 0;
		}
		if (charge > 2097152)
			this.detonate(world, pos);

		if (world.isRaining() && world.isRainingAt(pos.above()))
			charge *= 0.5;
	}

	/** Original ReikaWorldHelper.getClosestLivingEntityNoPlayers(..., true): players only outside creative. */
	@Nullable
	private LivingEntity getClosestTarget(Level world, double x, double y, double z, AABB box) {
		double d = Double.MAX_VALUE;
		LivingEntity index = null;
		for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, box)) {
			if (!(e instanceof Player) || !((Player)e).getAbilities().instabuild) {
				if (e.isAlive() && e.getHealth() > 0) {
					double dd = e.position().distanceTo(new net.minecraft.world.phys.Vec3(x, y, z));
					if (dd < d) {
						index = e;
						d = dd;
					}
				}
			}
		}
		return index;
	}

	private void shock(Level world, BlockPos pos, Direction dir) {
		BlockPos p = pos.relative(dir);
		BlockState b = world.getBlockState(p);
		if (b.isAir())
			return;
		this.dischargeToBlock(p, world.getBlockEntity(p) instanceof Shockable s ? s : null);
		if (b.is(Blocks.TNT)) {
			world.removeBlock(p, false);
			PrimedTnt e = new PrimedTnt(world, p.getX()+0.5D, p.getY()+0.5D, p.getZ()+0.5D, null);
			world.addFreshEntity(e);
			world.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	private void detonate(Level world, BlockPos pos) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(world);
		if (bolt != null) {
			bolt.moveTo(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5);
			world.addFreshEntity(bolt);
		}
		charge = 0;
		world.removeBlock(pos, false);
		world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 4F, true, Level.ExplosionInteraction.BLOCK);
	}

	public void dischargeToBlock(BlockPos p, @Nullable Shockable s) {
		float dx = 0.5F;
		float dy = 0.5F;
		float dz = 0.5F;
		if (s != null) {
			int min = s.getMinDischarge();
			if (charge < min)
				return;
			s.onDischarge(charge, Math.sqrt(worldPosition.distSqr(p)));
			dx = s.getAimX();
			dy = s.getAimY();
			dz = s.getAimZ();
		}
		RotarySounds.play(level, worldPosition, RotarySounds.SPARK.get(), 0.25F, 1F);
		level.addFreshEntity(new DischargeEntity(level, worldPosition.getX()+0.5, worldPosition.getY()+0.75, worldPosition.getZ()+0.5, charge,
				p.getX()+dx, p.getY()+dy, p.getZ()+dz));
		charge = 0;
	}

	private static boolean fullSuitOf(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> mat) {
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
			ItemStack is = e.getItemBySlot(slot);
			if (!(is.getItem() instanceof ArmorItem a) || !a.getMaterial().equals(mat))
				return false;
		}
		return true;
	}

	private void shock(LivingEntity e) {
		int dmg = this.getAttackDamage();
		if (fullSuitOf(e, ArmorMaterials.CHAIN))
			dmg = 0;
		else if (fullSuitOf(e, ArmorMaterials.LEATHER))
			dmg /= 2;
		if (dmg > 0) {
			e.hurt(RotaryDamage.source(level, RotaryDamage.SHOCK), dmg);
			if (e instanceof Creeper) {
				level.explode(e, e.getX(), e.getY(), e.getZ(), 3F, Level.ExplosionInteraction.MOB);
				e.hurt(level.damageSources().magic(), Integer.MAX_VALUE);
			}
		}
		RotarySounds.play(level, worldPosition, RotarySounds.SPARK.get(), 1.25F, 1F);
	}

	private int getAttackDamage() {
		return 1+(int)(Math.pow(charge, 2)/(4194304*8));
	}

	public int getCharge() {
		return charge;
	}

	public void setCharge(int c) {
		charge = c;
	}

	public int getRange() {
		return Math.min(charge/1024, this.getMaxRange());
	}

	public int getMaxRange() {
		return 16;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("c", charge);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		charge = NBT.getInt("c");
	}
}
