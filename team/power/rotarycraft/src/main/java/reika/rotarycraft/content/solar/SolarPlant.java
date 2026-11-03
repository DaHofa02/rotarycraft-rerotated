/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.solar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.rotarycraft.content.blockentity.MirrorBlockEntity;
import reika.rotarycraft.content.blockentity.SolarTowerBlockEntity;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.RotaryCraft.Auxiliary.SolarPlant - all mirrors and solar tower blocks touching each other (face to face). Every
 * column of tower blocks is a tower; its effective height counts the blocks above the last one with a mirror beside it (up to 32).
 * Towers are ranked by effective height; the first is the primary tower, the only one that makes power. The effective tower blocks
 * are summed with a falloff of 0.72 per further tower and rounded down to 0-6, 8, 10, 12, 16, 20, 24 or 32 (the tower multiplier, at
 * most 96). Every mirror aims at the top of the tower closest to it; the brightness is the share of working mirrors times the light
 * level (sun with weather, at night the moon phase).
 */
public class SolarPlant {

	private static final float TOWER_FALLOFF = 0.72F;
	public static final int MAX_TOWER_HEIGHT = 32;
	public static final int MAX_TOWER_VALUE = 96;

	private static final TreeMap<Integer, Integer> towerRounding = new TreeMap<>();

	/** Sorted by effective height, highest first (original ValueSortedMap). */
	private final List<SolarTower> towers = new ArrayList<>();
	private final Map<BlockPos, SolarTower> towerByColumn = new HashMap<>();
	private final Map<BlockPos, BlockPos> mirrors = new HashMap<>();
	private final Map<BlockPos, Integer> mirrorLevels = new HashMap<>();

	static {
		for (int i = 0; i <= 6; i++)
			towerRounding.put(i, i);
		for (int i = 8; i <= 12; i += 2)
			towerRounding.put(i, i);
		for (int i = 16; i <= 24; i += 4)
			towerRounding.put(i, i);
		towerRounding.put(MAX_TOWER_HEIGHT, MAX_TOWER_HEIGHT);
	}

	private static BlockPos to2D(BlockPos p) {
		return new BlockPos(p.getX(), 0, p.getZ());
	}

	public static SolarPlant build(Level world, BlockPos start) {
		SolarPlant p = new SolarPlant();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		if (world.getBlockEntity(start) instanceof SolarPlantBlock) {
			seen.add(start);
			queue.add(start);
		}
		Map<BlockPos, int[]> towerLocations = new HashMap<>();
		List<BlockPos> li = new ArrayList<>();
		while (!queue.isEmpty()) {
			BlockPos c = queue.poll();
			BlockEntity te = world.getBlockEntity(c);
			((SolarPlantBlock)te).setPlant(p);
			if (te instanceof MirrorBlockEntity) {
				li.add(c);
			}
			else if (te instanceof SolarTowerBlockEntity) {
				int[] get = towerLocations.get(to2D(c));
				int val1 = get != null ? get[0] : Integer.MAX_VALUE;
				int val2 = get != null ? get[1] : Integer.MIN_VALUE;
				towerLocations.put(to2D(c), new int[] {Math.min(val1, c.getY()), Math.max(val2, c.getY())});
			}
			for (Direction d : Direction.values()) {
				BlockPos n = c.relative(d);
				if (!seen.contains(n) && world.isLoaded(n) && world.getBlockEntity(n) instanceof SolarPlantBlock) {
					seen.add(n);
					queue.add(n);
				}
			}
		}
		for (Map.Entry<BlockPos, int[]> e : towerLocations.entrySet()) {
			BlockPos c = e.getKey();
			int[] ys = e.getValue();
			int dy = ys[0];
			int h = 0;
			while (world.getBlockEntity(new BlockPos(c.getX(), dy, c.getZ())) instanceof SolarTowerBlockEntity && dy <= ys[0]+MAX_TOWER_HEIGHT) {
				dy++;
				if (hasAdjacentMirror(world, new BlockPos(c.getX(), dy, c.getZ())))
					h = 0;
				h++;
			}
			SolarTower s = new SolarTower(c, h, ys[0], ys[1]);
			p.towers.add(s);
			p.towerByColumn.put(c, s);
		}
		p.towers.sort((a, b) -> {
			int ret = -Integer.compare(a.effectiveHeight, b.effectiveHeight);
			return ret != 0 ? ret : Integer.compare(a.hashCode(), b.hashCode());
		});
		for (BlockPos c : li)
			p.addMirror(c, getClosestTower(c, towerLocations.keySet()));
		return p;
	}

	/** Original ReikaWorldHelper.checkForAdjBlock with the mirror (only loaded neighbours). */
	private static boolean hasAdjacentMirror(Level world, BlockPos p) {
		for (Direction d : Direction.values()) {
			BlockPos n = p.relative(d);
			if (world.isLoaded(n) && world.getBlockEntity(n) instanceof MirrorBlockEntity)
				return true;
		}
		return false;
	}

	private void addMirror(BlockPos c, @Nullable BlockPos tower) {
		mirrors.put(c, tower);
		Integer get = mirrorLevels.get(to2D(c));
		int val = get != null ? get : Integer.MIN_VALUE; //original -1: 1.21 worlds go below y = 0
		mirrorLevels.put(to2D(c), Math.max(val, c.getY()));
	}

	/** Original getClosestTower: distance from the mirror to the tower column at y=0 (the original's 2D coordinates). */
	@Nullable
	private static BlockPos getClosestTower(BlockPos c, Set<BlockPos> locs) {
		double dist = Double.POSITIVE_INFINITY;
		BlockPos closest = null;
		for (BlockPos c2 : locs) {
			double dd = Math.sqrt(c2.distSqr(c));
			if (dd < dist) {
				dist = dd;
				closest = c2;
			}
		}
		return closest;
	}

	private boolean isHighestMirror(MirrorBlockEntity te) {
		Integer get = mirrorLevels.get(to2D(te.getBlockPos()));
		return get != null && te.getBlockPos().getY() >= get;
	}

	public int towerCount() {
		return towers.size();
	}

	public int mirrorCount() {
		return mirrors.size();
	}

	public int rawTowerBlocks() {
		int ret = 0;
		for (SolarTower val : towers)
			ret += val.effectiveHeight;
		return ret;
	}

	public int getEffectiveTowerBlocks() {
		float f = 1;
		int sum = 0;
		for (SolarTower val : towers) {
			sum = (int)(sum+val.effectiveHeight*f); //original int += float
			f *= TOWER_FALLOFF;
		}
		return towerRounding.floorEntry(sum).getValue();
	}

	/** The 2D position (y = 0) of the primary tower. */
	@Nullable
	public BlockPos getPrimaryTower() {
		return towers.isEmpty() ? null : towers.get(0).location;
	}

	@Nullable
	public BlockPos getAimingPositionForMirror(MirrorBlockEntity te) {
		if (towers.isEmpty())
			return null;
		BlockPos c = mirrors.get(te.getBlockPos());
		if (c == null)
			return null;
		return c.offset(0, towerByColumn.get(c).topBlock, 0);
	}

	public float getOverallBrightness(Level world) {
		float f = 0;
		for (BlockPos c : mirrors.keySet()) {
			if (world.isLoaded(c) && world.getBlockEntity(c) instanceof MirrorBlockEntity tm && tm.isFunctional())
				f++;
		}
		f /= this.mirrorCount();
		return f*this.getLightLevel(world)/15F;
	}

	/** Original getLightLevel: 0 in the nether and end; by day 15*sun intensity, at night by the moon phase. */
	public float getLightLevel(Level world) {
		if (world.dimension() == Level.NETHER || world.dimension() == Level.END || !world.dimensionType().hasSkyLight())
			return 0;
		double sun = AmbientTemperature.getSunIntensity(world, true, 0)*0.8F+0.2F;
		if (sun > 0.21)
			return (int)(15*sun);
		int moon = world.getMoonPhase();
		float phase = switch (moon) {
			case 0 -> 1;
			case 1, 7 -> 0.8F;
			case 2, 6 -> 0.5F;
			case 3, 5 -> 0.2F;
			case 4 -> 0.05F;
			default -> 0;
		};
		return 15*0.2F*phase;
	}

	public int getTowerMultiplier() {
		return Math.min(this.getEffectiveTowerBlocks(), MAX_TOWER_VALUE);
	}

	public void invalidate(Level world) {
		for (BlockPos c : mirrors.keySet()) {
			if (world.isLoaded(c) && world.getBlockEntity(c) instanceof SolarPlantBlock b)
				b.setPlant(null);
		}
		for (SolarTower s : towers) {
			for (int y = s.bottomBlock; y <= s.topBlock; y++) {
				BlockPos p = s.location.offset(0, y, 0);
				if (world.isLoaded(p) && world.getBlockEntity(p) instanceof SolarPlantBlock b)
					b.setPlant(null);
			}
		}
		towers.clear();
		towerByColumn.clear();
		mirrors.clear();
	}

	public boolean canSeeTheSky(MirrorBlockEntity te) {
		return te.getLevel().canSeeSky(te.getBlockPos().above()) && this.isHighestMirror(te);
	}

	private record SolarTower(BlockPos location, int effectiveHeight, int bottomBlock, int topBlock) {}
}
