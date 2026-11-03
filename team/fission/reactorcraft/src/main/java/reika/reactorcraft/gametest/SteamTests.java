/*******************************************************************************
 * Tests for the ported ReactorCraft steam power chain (original TileEntityReactorBoiler, TileEntitySteamLine, TileEntitySteamGrate,
 * BlockSteam, TileEntityTurbineCore). Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.block.SteamBlock;
import reika.reactorcraft.powergen.SteamGrateBlockEntity;
import reika.reactorcraft.powergen.SteamLineBlockEntity;
import reika.reactorcraft.powergen.TurbineCoreBlockEntity;
import reika.reactorcraft.powergen.WorkingFluid;
import reika.reactorcraft.reactor.ReactorBoilerBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SteamTests {

	private static final String PLATFORM = "platform_9x4x9";
	/** The turbine's largest blade circle (radius 3.5) needs more height than the small platform's barrier ceiling allows. */
	private static final String TALL = "platform_9x8x9";

	private static ReactorBoilerBlockEntity boiler(GameTestHelper h, BlockPos p, int water, int temp) {
		h.setBlock(p, ReactorBlocks.BOILER.get());
		ReactorBoilerBlockEntity te = (ReactorBoilerBlockEntity)h.getBlockEntity(p);
		te.getTank().fill(new FluidStack(Fluids.WATER, water), IFluidHandler.FluidAction.EXECUTE);
		te.setTemperature(temp);
		return te;
	}

	/** Loads a steam store into a grate the way a saved world would (the grate has no other way in besides steam lines). */
	private static void giveSteam(GameTestHelper h, BlockEntity te, int steam) {
		CompoundTag t = te.saveWithoutMetadata(h.getLevel().registryAccess());
		t.putInt("energy", steam);
		WorkingFluid.WATER.saveToNBT(t);
		te.loadWithComponents(t, h.getLevel().registryAccess());
	}

	/** A hot boiler (300 C) turns 200 mB water into one unit of steam per tick and cools 5 C each time; the steam line on top takes it. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void boilerFeedsSteamLine(GameTestHelper h) {
		ReactorBoilerBlockEntity b = boiler(h, new BlockPos(4, 1, 4), 6000, 300);
		h.setBlock(new BlockPos(4, 2, 4), ReactorBlocks.STEAM_LINE.get());
		SteamLineBlockEntity line = (SteamLineBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		h.runAtTickTime(40, () -> {
			h.assertTrue(line.getSteam() > 0 && line.getWorkingFluid() == WorkingFluid.WATER, "line: "+line.getSteam()+" "+line.getWorkingFluid());
			h.assertTrue(b.getTank().getFluidAmount() < 6000 && b.getTank().getFluidAmount()%200 == 0, "boiler tank "+b.getTank().getFluidAmount());
			h.assertTrue(h.getBlockState(new BlockPos(4, 2, 4)).getValue(net.minecraft.world.level.block.PipeBlock.DOWN), "line not connected to the boiler");
			h.succeed();
		});
	}

	/** A cold boiler (below 100 C) makes no steam. */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void coldBoilerMakesNoSteam(GameTestHelper h) {
		ReactorBoilerBlockEntity b = boiler(h, new BlockPos(4, 1, 4), 6000, 60);
		h.runAtTickTime(40, () -> {
			h.assertTrue(b.getSteam() == 0 && b.getTank().getFluidAmount() == 6000, "cold boiler boiled: "+b.getSteam());
			h.succeed();
		});
	}

	/** Boiler -> steam line -> steam grate: the grate lets the steam out as steam blocks above it, which rise away. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void grateReleasesSteam(GameTestHelper h) {
		boiler(h, new BlockPos(3, 1, 4), 12000, 400);
		h.setBlock(new BlockPos(3, 2, 4), ReactorBlocks.STEAM_LINE.get());
		h.setBlock(new BlockPos(4, 2, 4), ReactorBlocks.STEAM_GRATE.get());
		h.succeedWhen(() -> h.assertTrue(h.getBlockState(new BlockPos(4, 3, 4)).getBlock() instanceof SteamBlock, "no steam above the grate"));
	}

	/**
	 * Five turbine stages (steam moving west, high enough that the floor is outside the largest blade circle) over a steam grate two
	 * blocks under the first stage: with lubricant the turbine spins up and the
	 * last stage delivers power at full five-stage efficiency.
	 */
	@GameTest(template = TALL, timeoutTicks = 400)
	public static void turbineRunsOnSteam(GameTestHelper h) {
		h.setBlock(new BlockPos(7, 3, 4), ReactorBlocks.STEAM_GRATE.get());
		giveSteam(h, h.getBlockEntity(new BlockPos(7, 3, 4)), 100000);
		TurbineCoreBlockEntity[] t = new TurbineCoreBlockEntity[5];
		for (int i = 0; i < 5; i++) {
			BlockPos p = new BlockPos(7-i, 5, 4);
			h.setBlock(p, ReactorBlocks.TURBINE_CORE.get());
			t[i] = (TurbineCoreBlockEntity)h.getBlockEntity(p);
			t[i].setBlockMetadata(0);
			t[i].addLubricant(10000);
		}
		h.runAtTickTime(300, () -> {
			TurbineCoreBlockEntity last = t[4];
			h.assertTrue(t[0].getStage() == 0 && last.getStage() == 4 && last.getNumberStagesTotal() == 5, "stages "+t[0].getStage()+".."+last.getStage());
			h.assertTrue(last.getOmega() > 0 && last.getPower() > 0, "turbine: "+last.getOmega()+" rad/s, "+last.getPower()+" W, steam "+last.getSteam()+"; head "+t[0].getRawOmega()+" rad/s, steam "+t[0].getSteam()+", lube "+t[0].getLubricant()+", inter "+t[0].getInterference()+"/"+t[4].getInterference()+", below "+h.getBlockState(new BlockPos(7, 4, 4)));
			h.assertTrue(last.getTorque() <= TurbineCoreBlockEntity.TORQUE_CAP, "torque over cap");
			h.succeed();
		});
	}

	/** Without lubricant the turbine does not speed up. */
	@GameTest(template = TALL, timeoutTicks = 200)
	public static void turbineNeedsLubricant(GameTestHelper h) {
		h.setBlock(new BlockPos(7, 3, 4), ReactorBlocks.STEAM_GRATE.get());
		giveSteam(h, h.getBlockEntity(new BlockPos(7, 3, 4)), 100000);
		h.setBlock(new BlockPos(7, 5, 4), ReactorBlocks.TURBINE_CORE.get());
		TurbineCoreBlockEntity t = (TurbineCoreBlockEntity)h.getBlockEntity(new BlockPos(7, 5, 4));
		t.setBlockMetadata(0);
		h.runAtTickTime(150, () -> {
			h.assertTrue(t.getRawOmega() == 0, "dry turbine spins at "+t.getRawOmega());
			h.succeed();
		});
	}

	/** A block inside the blade circle jams the turbine. */
	@GameTest(template = TALL, timeoutTicks = 200)
	public static void turbineJamsOnBlock(GameTestHelper h) {
		h.setBlock(new BlockPos(7, 3, 4), ReactorBlocks.STEAM_GRATE.get());
		giveSteam(h, h.getBlockEntity(new BlockPos(7, 3, 4)), 100000);
		h.setBlock(new BlockPos(7, 5, 4), ReactorBlocks.TURBINE_CORE.get());
		TurbineCoreBlockEntity t = (TurbineCoreBlockEntity)h.getBlockEntity(new BlockPos(7, 5, 4));
		t.setBlockMetadata(0);
		t.addLubricant(10000);
		h.setBlock(new BlockPos(7, 6, 5), net.minecraft.world.level.block.Blocks.STONE);
		h.runAtTickTime(150, () -> {
			h.assertTrue(t.getRawOmega() == 0, "jammed turbine spins at "+t.getRawOmega());
			h.succeed();
		});
	}

	/** A condenser turns the steam block under it into 200 mB of low pressure water; ammonia steam becomes low pressure ammonia. */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void condenserCondensesSteam(GameTestHelper h) {
		h.setBlock(new BlockPos(3, 3, 4), ReactorBlocks.CONDENSER.get());
		h.setBlock(new BlockPos(3, 2, 4), ReactorBlocks.STEAM.get().defaultBlockState().setValue(SteamBlock.FLAGS, 3));
		h.setBlock(new BlockPos(5, 3, 4), ReactorBlocks.CONDENSER.get());
		h.setBlock(new BlockPos(5, 2, 4), ReactorBlocks.STEAM.get().defaultBlockState().setValue(SteamBlock.FLAGS, 7));
		var a = (reika.reactorcraft.powergen.CondenserBlockEntity)h.getBlockEntity(new BlockPos(3, 3, 4));
		var b = (reika.reactorcraft.powergen.CondenserBlockEntity)h.getBlockEntity(new BlockPos(5, 3, 4));
		h.succeedWhen(() -> {
			h.assertTrue(a.getTank().getFluidAmount() == 200 && a.getTank().getFluid().getFluid().isSame(reika.reactorcraft.registry.ReactorFluids.get("low_pressure_water")),
					"water condenser: "+a.getTank().getFluidAmount());
			h.assertTrue(b.getTank().getFluidAmount() == 200 && b.getTank().getFluid().getFluid().isSame(reika.reactorcraft.registry.ReactorFluids.get("low_pressure_ammonia")),
					"ammonia condenser: "+b.getTank().getFluidAmount());
			h.assertBlockNotPresent(ReactorBlocks.STEAM.get(), new BlockPos(3, 2, 4));
		});
	}

	/** The condenser recipe needs the RotaryCraft cooling fin; with the fin ported it is loaded (crafting and worktable). */
	@GameTest(template = PLATFORM)
	public static void condenserRecipeLoaded(GameTestHelper h) {
		var rm = h.getLevel().getRecipeManager();
		h.assertTrue(rm.byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "condenser_worktable")).isPresent(),
				"condenser worktable recipe missing");
		h.succeed();
	}

	/** Builds the blueprint slice for the given index at axial position x (slices lie in the y/z plane), centre (x, cy, cz). */
	private static void buildSlice(GameTestHelper h, int slice, int x, int cy, int cz) {
		String[] rows = reika.reactorcraft.content.block.TurbineMultiBlock.SLICES[slice];
		for (int i = 0; i < rows[0].length(); i++)
			for (int k = 0; k < rows.length; k++) {
				char c = rows[k].charAt(i);
				var b = c == 't' ? ReactorBlocks.TURBINE_BLADES.get() : c == 'h' ? ReactorBlocks.TURBINE_HOUSING.get() : c == 'e' ? ReactorBlocks.STEAM_INJECTOR.get() : null;
				if (b != null)
					h.setBlock(new BlockPos(x, cy+5-k, cz-5+i), b);
			}
	}

	/**
	 * One-stage high pressure turbine: blueprint slices 6 (housing around the core) and 7 (injector ring around the steam inlet), checked
	 * looking east from the exhaust end; formed, the core takes steam from the line behind it and lubricant from the injectors and spins
	 * up; breaking a housing block un-forms it - at more than 2048 rad/s the core flies apart.
	 */
	@GameTest(template = TALL, timeoutTicks = 400, batch = "meltdown")
	public static void hipTurbineMultiblock(GameTestHelper h) {
		int cy = 4, cz = 4;
		h.setBlock(new BlockPos(3, cy, cz), ReactorBlocks.HIP_TURBINE.get());
		var core = (reika.reactorcraft.powergen.HiPTurbineBlockEntity)h.getBlockEntity(new BlockPos(3, cy, cz));
		core.setBlockMetadata(0); //steam moves west, the inlet is east
		buildSlice(h, 6, 3, cy, cz);
		buildSlice(h, 7, 4, cy, cz);
		h.setBlock(new BlockPos(4, cy, cz), ReactorBlocks.STEAM_LINE.get());
		var line = h.getBlockEntity(new BlockPos(4, cy, cz));
		CompoundTag t = line.saveWithoutMetadata(h.getLevel().registryAccess());
		t.putInt("energy", 100000);
		WorkingFluid.WATER.saveToNBT(t);
		CompoundTag src = new CompoundTag();
		src.putDouble("FISSION", 100000);
		t.put("sources", src);
		line.loadWithComponents(t, h.getLevel().registryAccess());
		var inj = (reika.reactorcraft.powergen.SteamInjectorBlockEntity)h.getBlockEntity(new BlockPos(4, cy+1, cz));
		inj.fill(Direction.UP, new FluidStack(reika.rotarycraft.registry.RotaryFluids.LUBRICANT.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
		BlockPos any = h.absolutePos(new BlockPos(3, cy+2, cz));
		h.assertTrue(reika.reactorcraft.content.block.TurbineMultiBlock.checkForFullMultiBlock(h.getLevel(), any, Direction.EAST), "blueprint does not match");
		reika.reactorcraft.content.block.TurbineMultiBlock.onCreateFullMultiBlock(h.getLevel(), any);
		h.assertTrue(core.hasMultiBlock() && h.getBlockState(new BlockPos(3, cy+2, cz)).getValue(reika.reactorcraft.content.block.TurbineMultiBlock.FORMED), "not formed");
		h.runAtTickTime(300, () -> {
			h.assertTrue(core.getRawOmega() > 0 && core.getSteam() > 0 && core.getNumberStagesTotal() == 1,
					"turbine: "+core.getRawOmega()+" rad/s, steam "+core.getSteam()+", lube "+core.getLubricant());
			int speed = core.getRawOmega();
			h.setBlock(new BlockPos(3, cy+2, cz), net.minecraft.world.level.block.Blocks.AIR);
			if (speed > 2048) //original: losing the multiblock at speed blows the core apart
				h.assertBlockNotPresent(ReactorBlocks.HIP_TURBINE.get(), new BlockPos(3, cy, cz));
			else
				h.assertTrue(!core.hasMultiBlock() && !h.getBlockState(new BlockPos(3, cy-2, cz)).getValue(reika.reactorcraft.content.block.TurbineMultiBlock.FORMED), "still formed");
			h.succeed();
		});
	}

	/** A RotaryCraft pipe pump (input side east, microturbine from the south) moves steam from one steam line into the other. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void pipePumpMovesSteam(GameTestHelper h) {
		h.setBlock(new BlockPos(3, 1, 4), ReactorBlocks.STEAM_LINE.get());
		h.setBlock(new BlockPos(5, 1, 4), ReactorBlocks.STEAM_LINE.get());
		SteamLineBlockEntity dst = (SteamLineBlockEntity)h.getBlockEntity(new BlockPos(3, 1, 4));
		SteamLineBlockEntity src = (SteamLineBlockEntity)h.getBlockEntity(new BlockPos(5, 1, 4));
		h.setBlock(new BlockPos(4, 1, 4), reika.rotarycraft.registry.RotaryBlocks.PIPE_PUMP.get());
		((reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4))).setBlockMetadata(0);
		h.setBlock(new BlockPos(4, 1, 5), reika.rotarycraft.registry.RotaryBlocks.MICRO_ENGINE.get());
		var eng = (reika.rotarycraft.content.blockentity.engine.EngineBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 5));
		eng.setBlockMetadata(2);
		eng.addFuel(50000);
		h.onEachTick(() -> giveSteam(h, src, 5000));
		h.succeedWhen(() -> h.assertTrue(dst.getSteam() > 0 && dst.getWorkingFluid() == WorkingFluid.WATER, "west line steam "+dst.getSteam()));
	}
}
