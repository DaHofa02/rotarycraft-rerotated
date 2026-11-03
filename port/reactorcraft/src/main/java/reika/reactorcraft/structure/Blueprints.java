/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.structure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

import reika.reactorcraft.content.block.TurbineMultiBlock;
import reika.reactorcraft.fusion.FusionStructures;
import reika.reactorcraft.powergen.ReactorGeneratorBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;

/**
 * Original: Reika.ReactorCraft.Registry.ReactorStructures with GeneratorStructure, FlywheelStructure, TurbineStructure, SolenoidStructure,
 * InjectorStructure and PreheaterStructure (the handbook's structure pages, drawn by DragonAPI's StructureRenderer) - here the block
 * layouts of every ReactorCraft multiblock for the JEI multiblock viewer; plus the whole tokamak (ItemReactorPlacer.placeFusionReactor).
 */
public final class Blueprints {

	public record Blueprint(String id, String name, Block machine, Map<BlockPos, Block> blocks) {}

	private Blueprints() {}

	private static Blueprint make(String id, String name, Block machine, Consumer<FusionStructures.Sink> build) {
		Map<BlockPos, Block> m = new LinkedHashMap<>();
		build.accept((p, b) -> m.put(p.immutable(), b));
		return new Blueprint(id, name, machine, m);
	}

	public static List<Blueprint> all() {
		List<Blueprint> li = new ArrayList<>();
		li.add(make("hip_turbine", "High-Pressure Turbine (7 stages)", ReactorBlocks.HIP_TURBINE.get(), s -> {
			for (int i = 0; i < TurbineMultiBlock.SLICES.length; i++) {
				String[] rows = TurbineMultiBlock.SLICES[i];
				for (int c = 0; c < rows[0].length(); c++)
					for (int k = 0; k < rows.length; k++) {
						char ch = rows[k].charAt(c);
						Block b = ch == 't' ? ReactorBlocks.TURBINE_BLADES.get() : ch == 'h' ? ReactorBlocks.TURBINE_HOUSING.get() : ch == 'e' ? ReactorBlocks.STEAM_INJECTOR.get() : null;
						if (b != null)
							s.set(new BlockPos(i, 5-k, c-5), b);
					}
				s.set(new BlockPos(i, 0, 0), i < 7 ? ReactorBlocks.HIP_TURBINE.get() : ReactorBlocks.STEAM_LINE.get());
			}
		}));
		li.add(make("generator", "Turbine Generator", ReactorBlocks.GENERATOR.get(), s -> {
			Block core = ReactorBlocks.GENERATOR_CORE.get(), wind = ReactorBlocks.GENERATOR_WINDING.get(), house = ReactorBlocks.GENERATOR_HOUSING.get(),
					bypass = ReactorBlocks.STEAM_BYPASS.get();
			int l = ReactorGeneratorBlockEntity.GENERATOR_LENGTH-1;
			Direction dir = Direction.EAST, left = dir.getCounterClockWise();
			for (int i = 0; i < l; i++) {
				Block b = i < 2 ? bypass : wind;
				BlockPos d = BlockPos.ZERO.relative(dir, i);
				for (int k = -1; k <= 1; k++) {
					s.set(d.above(k).relative(left), b);
					s.set(d.above(k).relative(left, -1), b);
					s.set(d.above(k), b);
				}
			}
			for (int i = 0; i < l; i++) {
				BlockPos d = BlockPos.ZERO.relative(dir, i);
				for (int k = -2; k <= 2; k += 4) {
					s.set(d.above(k).relative(left), house);
					s.set(d.above(k).relative(left, -1), house);
					s.set(d.above(k), i == 1 && k == 2 ? bypass : house);
				}
				for (int k = -1; k <= 1; k++) {
					s.set(d.above(k).relative(left, 2), house);
					s.set(d.above(k).relative(left, -2), house);
				}
			}
			BlockPos e = BlockPos.ZERO.relative(dir, l);
			for (int k = -2; k <= 2; k++)
				for (int m = -2; m <= 2; m++)
					if ((Math.abs(k) != 2 || Math.abs(m) != 2) && (k != 0 || m != 0))
						s.set(e.above(k).relative(left, m), house);
			for (int i = 0; i < 2; i++) {
				BlockPos d = BlockPos.ZERO.relative(dir, i);
				for (int k : new int[]{2, -2}) {
					s.set(d.above(k).relative(left, 2), house);
					s.set(d.above(k).relative(left, -2), house);
				}
			}
			for (int i = 0; i < l; i++)
				s.set(BlockPos.ZERO.relative(dir, i), core);
			s.set(e, ReactorBlocks.GENERATOR.get());
		}));
		li.add(make("flywheel", "Turbine Flywheel", ReactorBlocks.FLYWHEEL.get(), s -> {
			Block[] b = {ReactorBlocks.FLYWHEEL_CORE.get(), ReactorBlocks.FLYWHEEL_DAMPENER.get(), ReactorBlocks.FLYWHEEL_FRAME.get()};
			Direction left = Direction.NORTH;
			BlockPos m = BlockPos.ZERO;
			s.set(m, ReactorBlocks.FLYWHEEL.get());
			for (int i = 1; i <= 2; i++) {
				Block x = i == 1 ? b[0] : b[2];
				s.set(m.relative(left, i), x);
				s.set(m.relative(left, -i), x);
				s.set(m.below(i), x);
				s.set(m.above(i), x);
			}
			for (int sgn : new int[]{1, -1}) {
				s.set(m.relative(left, sgn).above(), b[1]);
				s.set(m.relative(left, sgn).below(), b[1]);
				s.set(m.relative(left, sgn).above(2), b[2]);
				s.set(m.relative(left, sgn).below(2), b[2]);
				s.set(m.relative(left, 2*sgn).above(), b[2]);
				s.set(m.relative(left, 2*sgn).below(), b[2]);
			}
		}));
		li.add(make("solenoid", "Solenoid Magnet", ReactorBlocks.SOLENOID.get(), s -> FusionStructures.solenoid(s, BlockPos.ZERO)));
		li.add(make("injector", "Fusion Plasma Injector", ReactorBlocks.FUSION_INJECTOR.get(), s -> {
			FusionStructures.injectorFrameBlocks(s, BlockPos.ZERO.east(2), Direction.EAST);
			s.set(BlockPos.ZERO.east(2), ReactorBlocks.FUSION_INJECTOR.get());
			for (int i = 0; i <= 2; i++)
				s.set(BlockPos.ZERO.above(i), ReactorBlocks.INJECTOR_MULTI.get(5).get());
		}));
		li.add(make("preheater", "Hydrogen Preheater", ReactorBlocks.FUSION_HEATER.get(), s -> FusionStructures.heater(s, BlockPos.ZERO)));
		li.add(make("tokamak", "Tokamak (solenoid, ring, injectors)", ReactorBlocks.TOROID_MAGNET.get(), s -> {
			FusionStructures.solenoid(s, BlockPos.ZERO);
			FusionStructures.ringBlocks(s, BlockPos.ZERO);
			Direction[] out = {Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH};
			for (int i = 0; i < 4; i++) {
				BlockPos inj = new BlockPos(FusionStructures.INJECTORS[i][0], 0, FusionStructures.INJECTORS[i][1]);
				FusionStructures.injectorFrameBlocks(s, inj, out[i]);
				for (int k = 0; k <= 2; k++)
					s.set(inj.relative(out[i], -2).above(k), ReactorBlocks.INJECTOR_MULTI.get(5).get());
			}
		}));
		return li;
	}
}
