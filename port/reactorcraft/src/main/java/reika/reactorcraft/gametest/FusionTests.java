/*******************************************************************************
 * Tests for the ported ReactorCraft fusion reactor (original TileEntityToroidMagnet, TileEntitySolenoidMagnet, TileEntityFusionInjector,
 * TileEntityFusionHeater, TileEntityNeutronAbsorber, TileEntityMagneticPipe, EntityPlasma, EntityFusion and the fusion multiblocks).
 * Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.fusion.FusionEntity;
import reika.reactorcraft.fusion.FusionHeaterBlockEntity;
import reika.reactorcraft.fusion.FusionInjectorBlockEntity;
import reika.reactorcraft.fusion.FusionMultiBlock;
import reika.reactorcraft.fusion.FusionStructures;
import reika.reactorcraft.fusion.MagneticPipeBlockEntity;
import reika.reactorcraft.fusion.NeutronAbsorberBlockEntity;
import reika.reactorcraft.fusion.PlasmaEntity;
import reika.reactorcraft.fusion.SolenoidMagnetBlockEntity;
import reika.reactorcraft.fusion.ToroidMagnetBlockEntity;
import reika.reactorcraft.fusion.ToroidMagnetBlockEntity.Aim;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.HeatRayBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.VanDeGraffBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FusionTests {

	private static final String SMALL = "platform_9x4x9";
	private static final String LONG = "platform_16x8x9";
	private static final String BIG = "platform_48x12x48";

	private static <T> T be(GameTestHelper h, BlockPos rel, Class<T> c) {
		return c.cast(h.getBlockEntity(rel));
	}

	private static void meta(GameTestHelper h, BlockPos rel, int m) {
		((RotaryCraftBlockEntity)h.getBlockEntity(rel)).setBlockMetadata(m);
	}

	/** The screwdriver turns a toroid magnet one aim step on (right-click) or back (shift); N is followed by NNW1 and preceded by NNE3. */
	@GameTest(template = SMALL, timeoutTicks = 20)
	public static void magnetAimTurns(GameTestHelper h) {
		BlockPos p = new BlockPos(4, 1, 4);
		h.setBlock(p, ReactorBlocks.TOROID_MAGNET.get());
		ToroidMagnetBlockEntity m = be(h, p, ToroidMagnetBlockEntity.class);
		h.assertTrue(m.getAim() == Aim.N, "start aim "+m.getAim());
		m.onRightClick(h.getLevel(), m.getBlockPos(), Direction.UP);
		h.assertTrue(m.getAim() == Aim.NNW1 && m.getAlpha() == 512, "after right-click "+m.getAim());
		m.onShiftRightClick(h.getLevel(), m.getBlockPos(), Direction.UP);
		m.onShiftRightClick(h.getLevel(), m.getBlockPos(), Direction.UP);
		h.assertTrue(m.getAim() == Aim.NNE3, "after two shift clicks "+m.getAim());
		h.assertTrue(m.getTarget().equals(m.getBlockPos().offset(2, 0, 0)), "NNE3 points at "+m.getTarget());
		h.succeed();
	}

	/** The 36 magnets and 4 injectors of the debug tokamak close the ring: every magnet sees its way back to itself. */
	@GameTest(template = BIG, timeoutTicks = 60)
	public static void ringIsClosed(GameTestHelper h) {
		ServerLevel w = h.getLevel();
		BlockPos s = h.absolutePos(new BlockPos(21, 6, 24));
		List<BlockPos> mags = FusionStructures.ring(w, s);
		h.runAtTickTime(30, () -> {
			h.assertTrue(mags.size() == 36, "magnets "+mags.size());
			for (BlockPos p : mags) {
				ToroidMagnetBlockEntity m = (ToroidMagnetBlockEntity)w.getBlockEntity(p);
				h.assertTrue(m.hasNext(), "ring open at "+p+" (aim "+m.getAim()+")");
			}
			FusionInjectorBlockEntity inj = (FusionInjectorBlockEntity)w.getBlockEntity(s.offset(14, 0, 0));
			h.assertTrue(inj.getNextPart(w, inj.getBlockPos()) == w.getBlockEntity(mags.get(0)), "east injector does not lead to the first magnet");
			h.succeed();
		});
	}

	/** The solenoid multiblock forms around the solenoid magnet; a missing spoke prevents it, removing a part un-forms it again. */
	@GameTest(template = BIG, timeoutTicks = 40)
	public static void solenoidMultiblock(GameTestHelper h) {
		ServerLevel w = h.getLevel();
		BlockPos s = h.absolutePos(new BlockPos(21, 6, 24));
		FusionStructures.solenoid(w, s);
		SolenoidMagnetBlockEntity sol = (SolenoidMagnetBlockEntity)w.getBlockEntity(s);
		w.removeBlock(s.offset(4, 0, 0), false);
		h.assertTrue(!FusionMultiBlock.checkSolenoid(w, s.offset(5, 0, 0)), "formed with a spoke missing");
		w.setBlock(s.offset(4, 0, 0), ReactorBlocks.SOLENOID_MULTI.get(4).get().defaultBlockState(), 3);
		h.assertTrue(FusionMultiBlock.checkSolenoid(w, s.offset(4, 0, 0)), "complete solenoid not recognised");
		FusionMultiBlock.formSolenoid(w, s.offset(4, 0, 0));
		h.assertTrue(sol.hasMultiBlock() && w.getBlockState(s.offset(8, 0, 0)).getValue(FusionMultiBlock.FORMED), "not formed");
		w.removeBlock(s.offset(-8, 1, 0), false);
		h.assertTrue(!sol.hasMultiBlock() && !w.getBlockState(s.offset(8, 0, 0)).getValue(FusionMultiBlock.FORMED), "still formed after breaking a rail");
		h.succeed();
	}

	/** The injector frame forms when its coil column is placed; with plasma and no redstone the injector shoots one plasma ball per tick (2 mB). */
	@GameTest(template = LONG, timeoutTicks = 40)
	public static void injectorMakesPlasma(GameTestHelper h) {
		ServerLevel w = h.getLevel();
		BlockPos inj = h.absolutePos(new BlockPos(4, 3, 4));
		FusionStructures.injectorFrame(w, inj, Direction.EAST);
		FusionInjectorBlockEntity te = (FusionInjectorBlockEntity)w.getBlockEntity(inj);
		reika.reactorcraft.fusion.FusionMachineBlock.setInjectorFacing(w, te, Direction.NORTH);
		BlockPos trig = FusionStructures.injectorTrigger(inj, Direction.EAST);
		h.assertTrue(FusionMultiBlock.checkInjector(w, trig), "injector frame not recognised");
		FusionMultiBlock.formInjector(w, trig);
		h.assertTrue(te.hasMultiBlock(), "injector without multiblock");
		h.assertTrue(te.fill(Direction.EAST, new FluidStack(ReactorFluids.get("fusion_plasma"), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "plasma not taken from the pipe side");
		h.assertTrue(te.fill(Direction.UP, new FluidStack(ReactorFluids.get("fusion_plasma"), 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "plasma taken without a pipe");
		h.runAtTickTime(10, () -> {
			// only the balls on this injector's line: the 12-block box can also catch plasma of a neighbouring fusion test
			List<PlasmaEntity> all = w.getEntitiesOfClass(PlasmaEntity.class, new AABB(inj).inflate(12));
			List<PlasmaEntity> li = all.stream().filter(e -> Math.abs(e.getX()-(inj.getX()+0.5)) < 0.01 && Math.abs(e.getY()-(inj.getY()+0.5)) < 1).toList();
			h.assertTrue(li.size() >= 8 && li.size() <= 11, "plasma balls "+li.size()+" (in the box: "+all.size()+")");
			h.assertTrue(te.getTank().getFluidAmount() == 1000-2*li.size() || te.getTank().getFluidAmount() <= 1000-16, "plasma left "+te.getTank().getFluidAmount());
			for (PlasmaEntity e : li)
				h.assertTrue(e.getDeltaMovement().z < -0.7 && Math.abs(e.getX()-(inj.getX()+0.5)) < 0.01, "plasma not flying north: "+e.getDeltaMovement());
			h.succeed();
		});
	}

	/** Preheater: forms, and at 150 million degrees with deuterium and tritium makes 100 mB plasma per tick, sent up the (charged) magnetic pipes. */
	@GameTest(template = BIG, timeoutTicks = 80)
	public static void preheaterMakesPlasma(GameTestHelper h) {
		ServerLevel w = h.getLevel();
		BlockPos c = h.absolutePos(new BlockPos(10, 1, 10));
		FusionStructures.heater(w, c);
		h.assertTrue(FusionMultiBlock.checkHeater(w, c), "preheater not recognised");
		FusionMultiBlock.formHeater(w, c);
		FusionHeaterBlockEntity te = (FusionHeaterBlockEntity)w.getBlockEntity(c.offset(2, 2, 2));
		h.assertTrue(te.hasMultiBlock(), "heater without multiblock");
		te.setTemperature(FusionHeaterBlockEntity.PLASMA_TEMP+1000000);
		IFluidHandler cap = w.getCapability(Capabilities.FluidHandler.BLOCK, c.offset(2, 2, 2), Direction.WEST);
		h.assertTrue(cap.fill(new FluidStack(ReactorFluids.get("deuterium"), 4000), IFluidHandler.FluidAction.EXECUTE) == 4000, "deuterium refused");
		h.assertTrue(cap.fill(new FluidStack(ReactorFluids.get("tritium"), 4000), IFluidHandler.FluidAction.EXECUTE) == 4000, "tritium refused");
		h.onEachTick(() -> {
			for (int y = 3; y <= 6; y++)
				if (w.getBlockEntity(c.offset(2, y, 2)) instanceof MagneticPipeBlockEntity p)
					p.setCharge(100000);
		});
		h.runAtTickTime(40, () -> {
			h.assertTrue(te.getDeuteriumTank().getFluidAmount() < 4000-1000 && te.getTritiumTank().getFluidAmount() == te.getDeuteriumTank().getFluidAmount(),
					"D "+te.getDeuteriumTank().getFluidAmount()+", T "+te.getTritiumTank().getFluidAmount());
			MagneticPipeBlockEntity top = (MagneticPipeBlockEntity)w.getBlockEntity(c.offset(2, 6, 2));
			h.assertTrue(top != null && top.getFluidType() != null && top.getFluidType().isSame(ReactorFluids.get("fusion_plasma")) && top.getFluidLevel() > 0,
					"no plasma at the top of the pipe");
			h.assertTrue(te.getTemperature() < FusionHeaterBlockEntity.PLASMA_TEMP+1000000, "no heat loss: "+te.getTemperature());
			h.succeed();
		});
	}

	/**
	 * A heat ray (2 MW from a microturbine) firing through the lens heats the formed preheater by 640*log2(2^21) = 13440 degrees per tick
	 * (less 1+dT/16384 loss) and stops at it.
	 */
	@GameTest(template = BIG, timeoutTicks = 2600)
	public static void heatRayHeatsPreheater(GameTestHelper h) {
		ServerLevel w = h.getLevel();
		BlockPos c = h.absolutePos(new BlockPos(10, 1, 10));
		FusionStructures.heater(w, c);
		FusionMultiBlock.formHeater(w, c);
		FusionHeaterBlockEntity te = (FusionHeaterBlockEntity)w.getBlockEntity(c.offset(2, 2, 2));
		BlockPos ray = c.offset(2, 2, 6);
		w.setBlock(ray, RotaryBlocks.HEAT_RAY.get().defaultBlockState(), 3);
		((HeatRayBlockEntity)w.getBlockEntity(ray)).setBlockMetadata(3);
		BlockPos eng = ray.south();
		w.setBlock(eng, RotaryBlocks.MICRO_ENGINE.get().defaultBlockState(), 3);
		((EngineBlockEntity)w.getBlockEntity(eng)).setBlockMetadata(2);
		IFluidHandler fh = w.getCapability(Capabilities.FluidHandler.BLOCK, eng, Direction.DOWN);
		fh.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 20000), IFluidHandler.FluidAction.EXECUTE);
		int[] last = {Integer.MIN_VALUE};
		h.succeedWhen(() -> {
			HeatRayBlockEntity hr = (HeatRayBlockEntity)w.getBlockEntity(ray);
			h.assertTrue(hr.getPower() >= 2097152, "heat ray at "+hr.getPower()+" W");
			int t = te.getTemperature();
			int d = t-last[0];
			last[0] = t;
			h.assertTrue(d >= 13440-1-t/16384-2 && d <= 13440, "heating per tick "+d+" at "+t);
		});
	}

	/** An uncharged magnetic pipe holding plasma melts into lava; a charged one keeps it and shares its charge with the next pipe. */
	@GameTest(template = SMALL, timeoutTicks = 20)
	public static void magneticPipeNeedsCharge(GameTestHelper h) {
		ServerLevel w = h.getLevel();
		BlockPos a = new BlockPos(2, 1, 4), b = new BlockPos(3, 1, 4), c = new BlockPos(6, 1, 4);
		h.setBlock(a, ReactorBlocks.MAGNETIC_PIPE.get());
		h.setBlock(b, ReactorBlocks.MAGNETIC_PIPE.get());
		h.setBlock(c, ReactorBlocks.MAGNETIC_PIPE.get());
		MagneticPipeBlockEntity pa = be(h, a, MagneticPipeBlockEntity.class);
		MagneticPipeBlockEntity pb = be(h, b, MagneticPipeBlockEntity.class);
		be(h, c, MagneticPipeBlockEntity.class).setFluid(ReactorFluids.get("fusion_plasma"), 500);
		pa.setCharge(10000);
		pa.setFluid(ReactorFluids.get("fusion_plasma"), 500);
		pa.onDischarge(1000, 1);
		h.assertTrue(pa.getCharge() == 10000+(int)Math.pow(1000, 1.1), "discharge gave "+pa.getCharge());
		h.runAtTickTime(3, () -> {
			h.assertBlockPresent(Blocks.LAVA, c);
			h.assertBlockPresent(ReactorBlocks.MAGNETIC_PIPE.get(), a);
			h.assertTrue(pb.getCharge() > 0 && pb.getFluidLevel() > 0, "neighbour charge "+pb.getCharge()+", plasma "+pb.getFluidLevel());
			h.assertTrue(w.getBlockState(h.absolutePos(a)).getValue(MagneticPipeBlockEntity.CHARGED), "charged pipe does not glow");
			h.succeed();
		});
	}

	/** A powered Van de Graaff 3 blocks below a toroid magnet sparks into it once it holds 8192 C; the magnet then loses 20 % per second. */
	@GameTest(template = LONG, timeoutTicks = 200)
	public static void vdgChargesToroid(GameTestHelper h) {
		BlockPos vdgPos = new BlockPos(4, 2, 4);
		h.setBlock(new BlockPos(5, 1, 4), RotaryBlocks.DC_ENGINE.get());
		meta(h, new BlockPos(5, 1, 4), 0);
		h.setBlock(new BlockPos(5, 1, 5), Blocks.REDSTONE_BLOCK);
		h.setBlock(new BlockPos(4, 1, 4), RotaryBlocks.BEVEL_GEARS.get());
		BevelGearBlockEntity bev = be(h, new BlockPos(4, 1, 4), BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		h.setBlock(vdgPos, RotaryBlocks.VAN_DE_GRAFF.get());
		h.setBlock(new BlockPos(4, 5, 4), ReactorBlocks.TOROID_MAGNET.get());
		ToroidMagnetBlockEntity m = be(h, new BlockPos(4, 5, 4), ToroidMagnetBlockEntity.class);
		h.succeedWhen(() -> h.assertTrue(m.getCharge() >= 8192*0.8, "magnet charge "+m.getCharge()+", VDG "+be(h, vdgPos, VanDeGraffBlockEntity.class).getCharge()));
	}

	/** 20 plasma balls in one place fuse: a fusion flash with 3 fusion neutrons, which neutron absorbers around catch (+40 degrees each). */
	@GameTest(template = SMALL, timeoutTicks = 100)
	public static void plasmaFusesAndAbsorbersHeat(GameTestHelper h) {
		ServerLevel w = h.getLevel();
		BlockPos mid = h.absolutePos(new BlockPos(4, 2, 4));
		for (Direction d : Direction.Plane.HORIZONTAL)
			for (int s = -1; s <= 1; s++)
				w.setBlock(mid.relative(d, 3).relative(d.getClockWise(), s), ReactorBlocks.NEUTRON_ABSORBER.get().defaultBlockState(), 3);
		for (int i = 0; i < 20; i++) {
			PlasmaEntity e = new PlasmaEntity(w, mid, "");
			e.setDeltaMovement(0.0001, 0, 0);
			w.addFreshEntity(e);
		}
		boolean[] seen = {false, false};
		h.onEachTick(() -> {
			if (!w.getEntitiesOfClass(FusionEntity.class, new AABB(mid).inflate(3)).isEmpty())
				seen[0] = true;
			for (NeutronEntity n : w.getEntitiesOfClass(NeutronEntity.class, new AABB(mid).inflate(4)))
				if (n.getNeutronType() == NeutronEntity.NeutronType.FUSION)
					seen[1] = true;
		});
		h.succeedWhen(() -> {
			h.assertTrue(seen[0] && seen[1], "fusion "+seen[0]+", fusion neutrons "+seen[1]);
			int hot = 0;
			for (Direction d : Direction.Plane.HORIZONTAL)
				for (int s = -1; s <= 1; s++)
					if (w.getBlockEntity(mid.relative(d, 3).relative(d.getClockWise(), s)) instanceof NeutronAbsorberBlockEntity a && a.getTemperature() >= 30)
						hot++;
			h.assertTrue(hot > 0, "no absorber heated");
		});
	}

	/**
	 * The whole tokamak: jet engine -> bedrock gearboxes 16:1 and 2:1 (2048 rad/s at 32768 Nm) -> bevel gears -> the formed solenoid; the
	 * ring of 36 magnets (liquid nitrogen kept topped up, each charged by a Van de Graaff 3 blocks below, whose charge the test keeps at
	 * 8192 C as a powered generator would) and the 4 injectors in their formed frames with plasma. The solenoid reaches every magnet,
	 * the plasma runs round the ring, fuses and sends out fusion neutrons.
	 */
	@GameTest(template = BIG, timeoutTicks = 4000)
	public static void tokamakFuses(GameTestHelper h) {
		ServerLevel w = h.getLevel();
		BlockPos s = h.absolutePos(new BlockPos(21, 6, 24));
		FusionStructures.solenoid(w, s);
		FusionMultiBlock.formSolenoid(w, s.offset(2, 0, 0));
		SolenoidMagnetBlockEntity sol = (SolenoidMagnetBlockEntity)w.getBlockEntity(s);
		h.assertTrue(sol.hasMultiBlock(), "solenoid not formed");
		//power train below the solenoid
		BlockPos bevP = s.below(), g2 = bevP.east(), g16 = g2.east(), jetP = g16.east();
		w.setBlock(bevP, RotaryBlocks.BEVEL_GEARS.get().defaultBlockState(), 3);
		BevelGearBlockEntity bev = (BevelGearBlockEntity)w.getBlockEntity(bevP);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		for (BlockPos g : new BlockPos[]{g2, g16}) {
			w.setBlock(g, RotaryBlocks.GEARBOX.get().defaultBlockState(), 3);
			GearboxBlockEntity gb = (GearboxBlockEntity)w.getBlockEntity(g);
			int ratio = g == g2 ? 2 : 16;
			gb.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, ratio));
			gb.setBlockMetadata(ratio == 2 ? 0 : 12);
			gb.fillWithLubricant();
			gb.reduction = true;
		}
		w.setBlock(jetP, RotaryBlocks.JET_ENGINE.get().defaultBlockState(), 3);
		EngineBlockEntity jet = (EngineBlockEntity)w.getBlockEntity(jetP);
		jet.setBlockMetadata(0);
		//ring, injector frames, the VDGs under the magnets
		List<BlockPos> mags = FusionStructures.ring(w, s);
		Direction[] out = {Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH};
		for (int i = 0; i < 4; i++) {
			BlockPos inj = s.offset(FusionStructures.INJECTORS[i][0], 0, FusionStructures.INJECTORS[i][1]);
			FusionStructures.injectorFrame(w, inj, out[i]);
			BlockPos trig = FusionStructures.injectorTrigger(inj, out[i]);
			h.assertTrue(FusionMultiBlock.checkInjector(w, trig), "injector frame "+i+" not recognised");
			FusionMultiBlock.formInjector(w, trig);
			FusionInjectorBlockEntity te = (FusionInjectorBlockEntity)w.getBlockEntity(inj);
			te.getTank().fill(new FluidStack(ReactorFluids.get("fusion_plasma"), 8000), IFluidHandler.FluidAction.EXECUTE);
		}
		for (BlockPos p : mags)
			w.setBlock(p.below(3), RotaryBlocks.VAN_DE_GRAFF.get().defaultBlockState(), 3);
		int[] fusions = {0};
		boolean[] neutrons = {false};
		h.onEachTick(() -> {
			IFluidHandler fh = w.getCapability(Capabilities.FluidHandler.BLOCK, jetP, Direction.DOWN);
			if (fh != null)
				fh.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
			for (BlockPos p : mags) {
				ToroidMagnetBlockEntity m = (ToroidMagnetBlockEntity)w.getBlockEntity(p);
				m.getFluidHandler(null).fill(new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), 8000), IFluidHandler.FluidAction.EXECUTE);
				VanDeGraffBlockEntity v = (VanDeGraffBlockEntity)w.getBlockEntity(p.below(3));
				if (v.getCharge() < 8192)
					v.setCharge(8192);
			}
			fusions[0] += w.getEntitiesOfClass(FusionEntity.class, new AABB(s).inflate(18, 3, 18), e -> e.tickCount == 1).size();
			if (!neutrons[0])
				for (NeutronEntity n : w.getEntitiesOfClass(NeutronEntity.class, new AABB(s).inflate(20, 3, 20)))
					if (n.getNeutronType() == NeutronEntity.NeutronType.FUSION)
						neutrons[0] = true;
		});
		h.succeedWhen(() -> {
			h.assertTrue(sol.getOmega() == 2048 && sol.getTorque() == 32768 && sol.arePowerReqsMet(), "solenoid input "+sol.getOmega()+" rad/s @ "+sol.getTorque()+" Nm");
			for (BlockPos p : mags) {
				ToroidMagnetBlockEntity m = (ToroidMagnetBlockEntity)w.getBlockEntity(p);
				h.assertTrue(m.hasSolenoid() && m.hasNext() && m.getCharge() > ToroidMagnetBlockEntity.MIN_CHARGE, "magnet "+p+": solenoid "+m.hasSolenoid()+", next "+m.hasNext()+", charge "+m.getCharge());
			}
			int plasma = w.getEntitiesOfClass(PlasmaEntity.class, new AABB(s).inflate(18, 3, 18)).size();
			h.assertTrue(fusions[0] >= 5 && neutrons[0], "fusion events "+fusions[0]+", fusion neutrons "+neutrons[0]+", plasma "+plasma);
		});
	}
}
