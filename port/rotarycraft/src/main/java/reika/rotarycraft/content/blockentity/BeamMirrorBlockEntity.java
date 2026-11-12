/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.World.TileEntityBeamMirror - needs no power; under open sky and outside the night (ticks
 * 13500-22500 of the day) it throws sunlight along the side it faces (metadata 0 east, 1 west, 2 south, 3 north): range 2^(7 * sun
 * intensity), at most 64, up to the first opaque block. The clear spots of that line (from the second block on, as the original's
 * addLineOfClear starts one past its start) get level-15 light blocks, and undead in the beam are set on fire for 10 seconds. The mirror
 * turns towards the sun (half a degree a tick). The light block of the original is vanilla's minecraft:light.
 */
public class BeamMirrorBlockEntity extends RotaryCraftBlockEntity {

	public static final int MAXRANGE = 64;

	public float theta;
	private final List<BlockPos> light = new ArrayList<>();
	private int lastRange;

	public BeamMirrorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.BEAM_MIRROR.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Beam Mirror";
	}

	public Direction getFacing() {
		return switch (this.getBlockMetadata()) {
			case 1 -> Direction.WEST;
			case 2 -> Direction.SOUTH;
			case 3 -> Direction.NORTH;
			default -> Direction.EAST;
		};
	}

	public List<BlockPos> getLightSpots() {
		return light;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.adjustAim(world);
		if (world.isClientSide)
			return;
		this.setLight(world, pos);
		this.burnMobs(world, pos);
	}

	/** Original ReikaWorldHelper.getSunAngle / 2 + 12.5. */
	private void adjustAim(Level world) {
		int time = (int)(world.getDayTime()%12000);
		float sun = 0.5F*(float)(90*Math.sin(Math.toRadians(time*90D/6000D)));
		float suntheta = sun/2F+12.5F;
		if (theta < suntheta)
			theta += 0.5F;
		if (theta > suntheta)
			theta -= 0.5F;
	}

	private void burnMobs(Level world, BlockPos pos) {
		int r = this.getRange();
		Direction d = this.getFacing();
		AABB box = new AABB(pos).expandTowards(d.getStepX()*r, 0, d.getStepZ()*r);
		for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, box))
			if (e.getType().is(EntityTypeTags.UNDEAD))
				e.igniteForSeconds(10);
	}

	private void setLight(Level world, BlockPos pos) {
		int r = this.getRange();
		if (lastRange != r) {
			for (BlockPos c : light)
				if (world.getBlockState(c).is(Blocks.LIGHT))
					world.setBlockAndUpdate(c, Blocks.AIR.defaultBlockState());
			light.clear();
			if (r > 0 && world.canSeeSky(pos.above()))
				this.addLineOfClear(world, pos.relative(this.getFacing()), r);
			lastRange = r;
		}
		for (BlockPos c : light)
			if (world.getBlockState(c).isAir())
				world.setBlockAndUpdate(c, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15));
	}

	/** Original BlockArray.addLineOfClear/addIfClear: from one past start, adding free spots, passing through non-opaque ones. */
	private void addLineOfClear(Level world, BlockPos start, int range) {
		Direction d = this.getFacing();
		for (int i = 1; i <= range; i++) {
			BlockPos c = start.relative(d, i);
			BlockState s = world.getBlockState(c);
			if (s.isAir() || s.is(Blocks.LIGHT) || (s.getCollisionShape(world, c).isEmpty() && s.getFluidState().isEmpty()))
				light.add(c);
			else if (s.isSolidRender(world, c))
				return;
		}
	}

	/** Original getRange. */
	public int getRange() {
		if (level == null || !level.canSeeSky(worldPosition.above()))
			return 0;
		int time = (int)(level.getDayTime()%24000);
		if (time > 13500 && time < 22500)
			return 0;
		int ir = Math.min(MAXRANGE, (int)ReikaMathLibrary.doubpow(2, 7*AmbientTemperature.getSunIntensity(level, true, 0)));
		Direction d = this.getFacing();
		for (int i = 1; i < ir; i++) {
			BlockPos c = worldPosition.relative(d, i);
			BlockState s = level.getBlockState(c);
			if (!s.isAir() && s.isSolidRender(level, c))
				return i;
		}
		return ir;
	}

	private void lightsOut(Level world) {
		for (BlockPos c : light)
			if (world.getBlockState(c).is(Blocks.LIGHT))
				world.setBlockAndUpdate(c, Blocks.AIR.defaultBlockState());
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		this.lightsOut(world);
	}
}
