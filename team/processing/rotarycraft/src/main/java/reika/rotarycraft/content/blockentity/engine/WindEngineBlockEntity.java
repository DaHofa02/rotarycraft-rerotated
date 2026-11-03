/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.engine;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Engine.TileEntityWindEngine. Needs a clear 3x3 rotor area; speed falls with obstructions
 * up to 32 blocks behind it. The Galacticraft atmosphere/planet wind factors (DragonAPI AtmosphereHandler, PlanetDimensionHandler) are not ported.
 */
public class WindEngineBlockEntity extends EngineBlockEntity {

	private WindClearanceCheck clearance;

	public WindEngineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.WIND_ENGINE.get(), pos, state, EngineType.WIND);
	}

	private void dealBladeDamage(Level world, BlockPos pos, int meta) {
		int c = 0; int d = 0;
		int a = 0; int b = 0;
		if (meta < 2)
			b = 1;
		else
			a = 1;
		switch (meta) {
			case 0 -> c = 1;
			case 1 -> c = -1;
			case 2 -> d = 1;
			case 3 -> d = -1;
		}
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		AABB box = new AABB(x+c, y, z+d, x+1+c, y+1, z+1+d).inflate(a, 1, b);
		List<LivingEntity> in = world.getEntitiesOfClass(LivingEntity.class, box);
		for (LivingEntity ent : in) {
			ent.hurt(world.damageSources().generic(), 1);
		}
	}

	@Override
	protected void consumeFuel(float scale) {

	}

	@Override
	protected void internalizeFuel() {

	}

	private float getWindFactor(Level world) {
		if (clearance == null)
			clearance = new WindClearanceCheck(this, 32, 3);
		clearance.tick(world);

		float f = 1F-clearance.getPenalty();
		return Math.min(1, f);
	}

	@Override
	protected boolean getRequirements(Level world, BlockPos pos, int meta) {
		int c = 0; int d = 0;
		int a = 0; int b = 0;
		if (meta < 2)
			b = 1;
		else
			a = 1;
		switch (meta) {
			case 0 -> c = 1;
			case 1 -> c = -1;
			case 2 -> d = 1;
			case 3 -> d = -1;
		}
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (!world.getBlockState(pos.offset(a*i+c, j, b*i+d)).canBeReplaced()) {
					omega = 0;
					return false;
				}
			}
		}
		return true;
	}

	@Override
	protected void playSounds(Level world, BlockPos pos, float pitchMultiplier, float volume) {
		soundtick++;
		if (this.isMuffled(world, pos)) {
			volume *= 0.3125F;
		}

		if (soundtick < this.getSoundLength(1F/pitchMultiplier) && soundtick < 2000)
			return;
		soundtick = 0;

		RotarySounds.play(world, pos, RotarySounds.WIND.get(), 1.1F*volume, 1F*pitchMultiplier);
	}

	@Override
	public int getFuelLevel() {
		return 0;
	}

	@Override
	protected int getMaxSpeed(Level world, BlockPos pos, int meta) {
		return (int)(EngineType.WIND.getSpeed()*this.getWindFactor(world));
	}

	@Override
	protected void affectSurroundings(Level world, BlockPos pos, int meta) {
		this.dealBladeDamage(world, pos, meta);
	}

	@Override
	public void onRotate() {
		clearance = null;
	}

	private static class WindClearanceCheck {

		private final BlockPos engine;
		private final Direction direction;

		private final int stepRatio;
		private final int searchDistance;

		private int step = 1;

		private final double[] blockFraction;

		private boolean dirty;

		private float penalty;

		private WindClearanceCheck(WindEngineBlockEntity te, int search, int step) {
			engine = te.getBlockPos();
			direction = te.getWriteDirection().getOpposite();

			searchDistance = search;
			stepRatio = step;

			blockFraction = new double[searchDistance];

			for (int i = 1; i <= searchDistance; i++) {
				this.scanRow(te.getLevel(), i);
			}
		}

		private void tick(Level world) {
			this.scanRow(world, step);

			step++;
			if (step > searchDistance)
				step = 1;
		}

		private void scanRow(Level world, int step) {
			int r = 1+((step-1)/stepRatio);
			Direction dir = direction.getClockWise();

			int blocked = 0;
			for (int i = -r; i <= r; i++) {
				BlockPos p = engine.relative(direction, step).relative(dir, i);
				if (!world.getBlockState(p).canBeReplaced()) {
					blocked++;
				}
			}

			double frac = blocked/(r*2D+1);
			if (blockFraction[step-1] != frac) {
				blockFraction[step-1] = frac;
				dirty = true;
			}
		}

		private void calculatePenalty() {
			penalty = 0;
			double max = 0;
			for (int i = 0; i < blockFraction.length; i++) {
				double pow = 1.2D-(i/(double)searchDistance);
				double rowValue = Math.pow(pow, 12);
				penalty += rowValue*blockFraction[i];
				max += rowValue;
			}
			penalty /= Math.sqrt(max);
			penalty = (float)Math.sqrt(penalty);
		}

		public float getPenalty() {
			if (dirty) {
				this.calculatePenalty();
				dirty = false;
			}
			return penalty;
		}
	}
}
