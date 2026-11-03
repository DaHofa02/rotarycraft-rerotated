/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import reika.reactorcraft.content.entity.RadiationEntity;
import reika.reactorcraft.radiation.RadiationEffects;
import reika.reactorcraft.radiation.RadiationIntensity;

/**
 * Original: BlockCoriumFlowing (BlockFluidClassic) - molten core flows like a slow liquid (Forge tick rate viscosity/200 = 40), drops
 * what it flows over, and where it flows: with a 0.02 % / 0.1 % chance lethal contamination (8 / 1 range) unless 24 clouds are near;
 * ice next to it melts, water next to it boils away with a fizz and smoke.
 */
public final class CoriumFluid {

	private CoriumFluid() {}

	static void onSpread(LevelAccessor la, BlockPos pos) {
		if (!(la instanceof Level world) || world.isClientSide)
			return;
		var r = world.random;
		if (world.getEntitiesOfClass(RadiationEntity.class, new AABB(pos).inflate(12)).size() < 24) {
			if (r.nextDouble() < 0.0002)
				RadiationEffects.contaminateArea(world, pos.above(r.nextInt(3)), 8, 1, 0, false, RadiationIntensity.LETHAL);
			if (r.nextDouble() < 0.001)
				RadiationEffects.contaminateArea(world, pos.above(r.nextInt(3)), 1, 1, 0, false, RadiationIntensity.LETHAL);
		}
		for (Direction d : Direction.values()) {
			BlockPos p = pos.relative(d);
			BlockState s = world.getBlockState(p);
			if (s.is(Blocks.ICE)) {
				world.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
				break;
			}
		}
		for (Direction d : Direction.values()) {
			BlockPos p = pos.relative(d);
			if (world.getFluidState(p).is(FluidTags.WATER)) {
				world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
				world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
				if (world instanceof ServerLevel sl)
					sl.sendParticles(ParticleTypes.SMOKE, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 8, 0.5, 0.5, 0.5, 0);
				break;
			}
		}
	}

	public static class Source extends BaseFlowingFluid.Source {
		public Source(Properties p) {
			super(p);
		}

		@Override
		protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction dir, FluidState fs) {
			super.spreadTo(level, pos, state, dir, fs);
			onSpread(level, pos);
		}
	}

	public static class Flowing extends BaseFlowingFluid.Flowing {
		public Flowing(Properties p) {
			super(p);
		}

		@Override
		protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction dir, FluidState fs) {
			super.spreadTo(level, pos, state, dir, fs);
			onSpread(level, pos);
		}
	}
}
