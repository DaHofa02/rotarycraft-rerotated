/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.lib.world.CropHelper;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Farming.TileEntityLawnSprinkler - a sprinkler fed from a pipe below (5 mB, 1 used a tick).
 * Three times a tick it picks a spot within a radius drawn with weights 8:5:3:1:1 for 1, 2, 3, 4 and 8 blocks and looks down from its
 * own level for up to 4 blocks: farmland gets one step wetter; on the way, plants that grow are given a random tick with a chance of
 * 1/8; an opaque block ends the look. Likewise it puts out fire (not on netherrack) within its range. From 800 000 kPa it hurts mobs it can see
 * (4 points from 2 000 000). Not ported: wetting machines below (no ported machine is Wettable), ReactorCraft radiation clean-up, the atmosphere check of other mods.
 */
public class LawnSprinklerBlockEntity extends SprinklerBaseBlockEntity {

	public static final int PRESSURE_TO_HURT = 800000;
	public static final int PRESSURE_TO_KILL = 2000000;
	/** Original GROWTH_PATTERN: weights of the radii 1, 2, 3, 4 and the full range. */
	private static final int[] GROWTH_PATTERN = {8, 5, 3, 1, 1};

	private int speed;

	public LawnSprinklerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.LAWN_SPRINKLER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Lawn Sprinkler";
	}

	@Override
	public int getCapacity() {
		return 5;
	}

	@Override
	public int getWaterConsumption() {
		return 1;
	}

	@Override
	public Direction getPipeDirection() {
		return Direction.DOWN;
	}

	@Override
	protected void doAnimations() {
		if (level != null && level.isClientSide) {
			if (this.canPerformEffects()) {
				if (speed < 24)
					speed++;
			}
			else if (speed > 0)
				speed--;
		}
		phi += speed;
	}

	@Override
	protected void performEffects(Level world, BlockPos pos) {
		if (world instanceof ServerLevel sl) {
			for (int k = 0; k < 3; k++) {
				this.accelerateGrowth(sl, pos);
				this.extinguishFire(sl, pos);
			}
			if (this.getPressure() > PRESSURE_TO_HURT)
				this.damageMobs(sl, pos, this.getPressure() >= PRESSURE_TO_KILL ? 4 : 1);
		}
		else
			this.spreadWater(world, pos);
	}

	private int calcRange() {
		int total = 0;
		for (int w : GROWTH_PATTERN)
			total += w;
		int pick = rand.nextInt(total);
		for (int i = 0; i < GROWTH_PATTERN.length; i++) {
			pick -= GROWTH_PATTERN[i];
			if (pick < 0)
				return i < GROWTH_PATTERN.length-1 ? i+1 : this.getMaxRange();
		}
		return this.getMaxRange();
	}

	private void accelerateGrowth(ServerLevel world, BlockPos pos) {
		int r = this.calcRange();
		int rx = pos.getX()-r+rand.nextInt(2*r+1);
		int rz = pos.getZ()-r+rand.nextInt(2*r+1);
		for (int i = pos.getY(); i > pos.getY()-4; i--) {
			BlockPos c = new BlockPos(rx, i, rz);
			BlockState s = world.getBlockState(c);
			if (s.is(Blocks.FARMLAND)) {
				int m = s.getValue(FarmBlock.MOISTURE);
				if (m < FarmBlock.MAX_MOISTURE)
					world.setBlock(c, s.setValue(FarmBlock.MOISTURE, m+1), 3);
				return;
			}
			if (!s.isAir() && s.isSolidRender(world, c))
				return;
			if (rand.nextInt(8) == 0 && (CropHelper.isGrowingPlant(s) || s.getBlock() instanceof BonemealableBlock))
				s.randomTick(world, c, world.random);
		}
	}

	private void extinguishFire(ServerLevel world, BlockPos pos) {
		int r = this.getRange();
		int rx = pos.getX()-r+rand.nextInt(2*r+1);
		int rz = pos.getZ()-r+rand.nextInt(2*r+1);
		for (int i = pos.getY(); i > pos.getY()-4; i--) {
			BlockPos c = new BlockPos(rx, i, rz);
			BlockState s = world.getBlockState(c);
			if (s.is(Blocks.FIRE)) {
				if (!world.getBlockState(c.below()).is(Blocks.NETHERRACK)) {
					world.removeBlock(c, false);
					world.playSound(null, c, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
				}
			}
			else if (!s.isAir() && s.isSolidRender(world, c))
				return;
		}
	}

	private void damageMobs(ServerLevel world, BlockPos pos, int dmg) {
		int r = this.getRange();
		AABB box = new AABB(pos).move(0, 1, 0).inflate(r, 1, r);
		Vec3 from = Vec3.atCenterOf(pos);
		for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, box)) {
			Vec3 to = new Vec3(e.getX(), e.getY()+0.5, e.getZ());
			if (world.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS)
				e.hurt(world.damageSources().drown(), 0.5F*dmg);
		}
	}

	/** Original spreadWater: splash particles in three streams turning with the head (client side). */
	private void spreadWater(Level world, BlockPos pos) {
		int r = this.getRange();
		for (int s = 0; s < 3; s++) {
			double a = Math.toRadians(phi+s*120+60);
			double dx = 0.6*Math.sin(a), dz = 0.6*Math.cos(a);
			for (int i = 0; i < 6; i++) {
				double v = rand.nextInt((1+r)*10)/72D;
				world.addParticle(ParticleTypes.SPLASH, pos.getX()+0.5+dx, pos.getY()+0.75, pos.getZ()+0.5+dz, dx*v-0.025+0.05*rand.nextDouble(), 0, dz*v-0.025+0.05*rand.nextDouble());
			}
		}
	}
}
