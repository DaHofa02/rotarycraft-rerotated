/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import reika.reactorcraft.registry.ReactorBlocks;
import reika.rotarycraft.registry.RotaryBlocks;

/**
 * Original: Reika.ReactorCraft.Auxiliary.Structure.SolenoidStructure, InjectorStructure and PreheaterStructure (the handbook's structure
 * blueprints) and ItemReactorPlacer.placeFusionReactor (the creative debug tokamak: 4 injectors 14 blocks around the solenoid, 36
 * magnets with their aims) - here they build the structures in the world (for the tests and the showcase). The multiblock parts are
 * placed unformed; forming is up to the caller (as placing a part by hand would).
 */
public final class FusionStructures {

	private FusionStructures() {}

	/** Where a blueprint goes: the world, or a map for the JEI multiblock viewer. */
	public interface Sink {
		void set(BlockPos p, Block b);
	}

	public static Sink of(Level world) {
		return (p, b) -> world.setBlock(p, b.defaultBlockState(), 3);
	}

	private static void set(Sink world, BlockPos p, Block b) {
		world.set(p, b);
	}

	private static Block sol(int m) {
		return ReactorBlocks.SOLENOID_MULTI.get(m).get();
	}

	private static Block inj(int m) {
		return ReactorBlocks.INJECTOR_MULTI.get(m).get();
	}

	private static Block hea(int m) {
		return ReactorBlocks.HEATER_MULTI.get(m).get();
	}

	/** SolenoidStructure around the solenoid magnet at {@code c} (which it places too). */
	public static void solenoid(Level world, BlockPos c) {
		solenoid(of(world), c);
	}

	public static void solenoid(Sink world, BlockPos c) {
		for (int i = -1; i <= 1; i++)
			for (int j = 0; j <= 1; j++)
				for (int k = -1; k <= 1; k++)
					if (i != 0 || j != 0 || k != 0)
						set(world, c.offset(i, j, k), sol(5));
		for (int i = 2; i <= 7; i++) {
			set(world, c.offset(i, 0, 0), sol(4));
			set(world, c.offset(-i, 0, 0), sol(4));
			set(world, c.offset(0, 0, i), sol(4));
			set(world, c.offset(0, 0, -i), sol(4));
			if (i < 6) {
				set(world, c.offset(i, 0, i), sol(4));
				set(world, c.offset(-i, 0, i), sol(4));
				set(world, c.offset(i, 0, -i), sol(4));
				set(world, c.offset(-i, 0, -i), sol(4));
			}
		}
		for (int sx = -6; sx <= 6; sx += 12) {
			for (int sz = -6; sz <= 6; sz += 12) {
				set(world, c.offset(sx, 1, sz), sol(1));
				set(world, c.offset(sx, 0, sz), sol(3));
				set(world, c.offset(sx, -1, sz), sol(1));
			}
		}
		for (int y = -1; y <= 1; y++) {
			for (int i = -5; i <= 5; i++) {
				int d = Math.abs(i) >= 4 ? 7 : 8;
				int m = y == 0 ? (Math.abs(i) >= 3 ? 3 : 2) : (Math.abs(i) >= 3 ? 1 : 0);
				set(world, c.offset(-d, y, i), sol(m));
				set(world, c.offset(d, y, i), sol(m));
				set(world, c.offset(i, y, d), sol(m));
				set(world, c.offset(i, y, -d), sol(m));
			}
		}
		set(world, c, ReactorBlocks.SOLENOID.get());
	}

	/** The positions of the 4 injectors (east, north, west, south of the solenoid) and the way each faces (placeFusionReactor). */
	public static final int[][] INJECTORS = {{14, 0}, {0, -14}, {-14, 0}, {0, 14}};
	public static final Direction[] INJECTOR_FACING = {Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST};

	/** placeFusionReactor: the 4 injectors and the 36 magnets around the solenoid at {@code s}; returns the magnets in ring order. */
	public static List<BlockPos> ring(Level world, BlockPos s) {
		for (int i = 0; i < 4; i++) {
			BlockPos p = s.offset(INJECTORS[i][0], 0, INJECTORS[i][1]);
			set(of(world), p, ReactorBlocks.FUSION_INJECTOR.get());
			if (world.getBlockEntity(p) instanceof FusionInjectorBlockEntity te)
				FusionMachineBlock.setInjectorFacing(world, te, INJECTOR_FACING[i]);
		}
		List<BlockPos> li = new ArrayList<>();
		int dx = s.getX()+14, dz = s.getZ()-2;
		ToroidMagnetBlockEntity.Aim a = ToroidMagnetBlockEntity.Aim.W;
		for (int k = 0; k < 4; k++) {
			for (int i = 0; i < 9; i++) {
				BlockPos p = new BlockPos(dx, s.getY(), dz);
				set(of(world), p, ReactorBlocks.TOROID_MAGNET.get());
				if (world.getBlockEntity(p) instanceof ToroidMagnetBlockEntity te)
					te.setAim(a);
				li.add(p);
				dx += a.xOffset;
				dz += a.zOffset;
				a = a.getNext();
			}
			a = a.getPrev();
			dx += a.xOffset;
			dz += a.zOffset;
		}
		return li;
	}

	/** The ring's places (injectors and magnets) without aims or facings, for the viewer. */
	public static void ringBlocks(Sink world, BlockPos s) {
		for (int[] i : INJECTORS)
			set(world, s.offset(i[0], 0, i[1]), ReactorBlocks.FUSION_INJECTOR.get());
		int dx = s.getX()+14, dz = s.getZ()-2;
		ToroidMagnetBlockEntity.Aim a = ToroidMagnetBlockEntity.Aim.W;
		for (int k = 0; k < 4; k++) {
			for (int i = 0; i < 9; i++) {
				set(world, new BlockPos(dx, s.getY(), dz), ReactorBlocks.TOROID_MAGNET.get());
				dx += a.xOffset;
				dz += a.zOffset;
				a = a.getNext();
			}
			a = a.getPrev();
			dx += a.xOffset;
			dz += a.zOffset;
		}
	}

	/** InjectorStructure for the injector at {@code injector}, its frame running along {@code dir} (away from the ring). */
	public static void injectorFrame(Level world, BlockPos injector, Direction dir) {
		injectorFrameBlocks(of(world), injector, dir);
		if (!(world.getBlockEntity(injector) instanceof FusionInjectorBlockEntity))
			set(of(world), injector, ReactorBlocks.FUSION_INJECTOR.get());
		BlockPos x = injector.relative(dir, -2);
		for (int i = 0; i <= 2; i++) //the induction coil column last (placing it is what checks the structure)
			set(of(world), x.above(i), inj(5));
	}

	/** The frame without the injector and the coil column. */
	public static void injectorFrameBlocks(Sink world, BlockPos injector, Direction dir) {
		BlockPos x = injector.relative(dir, -2);
		Direction left = dir.getCounterClockWise();
		for (int s = -1; s <= 1; s += 2) {
			Direction l = s > 0 ? left : left.getOpposite();
			for (int i = 0; i <= 4; i++)
				set(world, x.relative(dir, i).above(3).relative(l), inj(4));
			for (int i = 5; i <= 6; i++)
				set(world, x.relative(dir, i).above(2).relative(l), inj(4));
			for (int i = 7; i <= 8; i++)
				set(world, x.relative(dir, i).above(1).relative(l), inj(4));
			for (int i = 0; i <= 8; i++)
				set(world, x.relative(dir, i).below().relative(l), inj(1));
			for (int k = 0; k <= 2; k++)
				set(world, x.above(k).relative(l), inj(6));
			set(world, x.relative(dir, 8).relative(l), inj(6));
			set(world, x.relative(dir, 1).relative(l), inj(2));
			for (int i = 3; i <= 7; i++)
				set(world, x.relative(dir, i).relative(l), inj(2));
			for (int i = 1; i <= 6; i++)
				set(world, x.relative(dir, i).above(1).relative(l), inj(2));
			for (int i = 1; i <= 4; i++)
				set(world, x.relative(dir, i).above(2).relative(l), inj(2));
		}
		for (int i = 0; i <= 4; i++)
			set(world, x.relative(dir, i).above(3), inj(3));
		for (int i = 5; i <= 6; i++)
			set(world, x.relative(dir, i).above(2), inj(3));
		for (int i = 7; i <= 8; i++)
			set(world, x.relative(dir, i).above(1), inj(3));
		for (int i = 0; i <= 8; i++)
			set(world, x.relative(dir, i).below(), inj(0));
		set(world, x.relative(dir, 1), inj(7));
		for (int i = 1; i <= 6; i++)
			set(world, x.relative(dir, i).above(1), inj(7));
		for (int i = 1; i <= 4; i++)
			set(world, x.relative(dir, i).above(2), inj(7));
		for (int i = 3; i <= 8; i++)
			set(world, x.relative(dir, i), ReactorBlocks.MAGNETIC_PIPE.get());
	}

	/** The column part placed last by {@link #injectorFrame} - the one whose placement checks the structure. */
	public static BlockPos injectorTrigger(BlockPos injector, Direction dir) {
		return injector.relative(dir, -2);
	}

	/** PreheaterStructure with its lower north-west corner at {@code c}: the lens faces south (+z), the plasma pipe goes up 6 to y+6. */
	public static void heater(Level world, BlockPos c) {
		heater(of(world), c);
	}

	public static void heater(Sink world, BlockPos c) {
		for (int i = 0; i < 5; i++) {
			for (int k = 0; k < 5; k++) {
				for (int h = 0; h < 5; h++) {
					boolean corner = (i == 0 || i == 4) && (k == 0 || k == 4) && (h == 0 || h == 4);
					boolean edge = i == 0 || i == 4 || k == 0 || k == 4;
					Block b = hea(corner ? 2 : edge ? 3 : 4);
					if (h > 0 && !edge) {
						b = hea(1);
						if (i == 2 && k == 2 && h >= 2)
							b = h > 2 ? ReactorBlocks.MAGNETIC_PIPE.get() : ReactorBlocks.FUSION_HEATER.get();
					}
					set(world, c.offset(i, h, k), b);
				}
			}
		}
		for (int i = 0; i < 3; i++) {
			for (int k = 0; k < 3; k++) {
				boolean corner = (i == 0 || i == 2) && (k == 0 || k == 2);
				boolean edge = i == 0 || i == 2 || k == 0 || k == 2;
				set(world, c.offset(1+i, 5, 1+k), hea(corner ? 2 : edge ? 3 : 4));
			}
		}
		for (int i = 1; i <= 3; i++) {
			for (int k = 1; k <= 3; k++) {
				set(world, c.offset(i, k, 0), hea(4));
				set(world, c.offset(i, k, 4), hea(4));
				set(world, c.offset(0, k, i), hea(4));
				set(world, c.offset(4, k, i), hea(4));
			}
		}
		set(world, c.offset(2, 5, 2), ReactorBlocks.MAGNETIC_PIPE.get());
		set(world, c.offset(2, 6, 2), ReactorBlocks.MAGNETIC_PIPE.get());
		for (int i = 0; i < 5; i++)
			if (i != 2)
				set(world, c.offset(i, 2, 2), RotaryBlocks.PIPE.get());
		set(world, c.offset(2, 2, 3), hea(0));
		set(world, c.offset(2, 2, 4), hea(0));
	}
}
