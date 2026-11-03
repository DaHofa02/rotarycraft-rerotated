/*******************************************************************************
 * Dev-only visual check for the private ReactorCraft port (not part of the original mod; inactive unless the system property
 * "reactorcraft.showcase.tokamak" is set, as the showcase run does): builds a running tokamak in the showcase world with the same
 * structures and power train as the FusionTests GameTest and keeps it supplied (fuel, liquid nitrogen, Van de Graaff charge, preheater
 * gases, pipe charge) every tick.
 ******************************************************************************/
package reika.reactorcraft.dev;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.fusion.FusionHeaterBlockEntity;
import reika.reactorcraft.fusion.FusionInjectorBlockEntity;
import reika.reactorcraft.fusion.FusionMultiBlock;
import reika.reactorcraft.fusion.FusionStructures;
import reika.reactorcraft.fusion.MagneticPipeBlockEntity;
import reika.reactorcraft.fusion.ToroidMagnetBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.HeatRayBlockEntity;
import reika.rotarycraft.content.blockentity.VanDeGraffBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@EventBusSubscriber(modid = ReactorCraft.MOD_ID)
public final class FusionShowcase {

	private static final boolean ACTIVE = System.getProperty("reactorcraft.showcase.tokamak") != null;
	/** The solenoid of the showcase tokamak. */
	public static final BlockPos CENTER = new BlockPos(64, -55, 64);

	private static List<BlockPos> magnets;
	private static BlockPos center = CENTER;
	private static BlockPos heater;
	private static BlockPos jet;
	private static BlockPos turbine;

	private FusionShowcase() {}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post e) {
		if (!ACTIVE && magnets == null)
			return;
		ServerLevel w = e.getServer().overworld();
		BlockPos s = center;
		if (magnets == null) {
			for (int cx = (s.getX()-34) >> 4; cx <= (s.getX()+34) >> 4; cx++)
				for (int cz = (s.getZ()-26) >> 4; cz <= (s.getZ()+26) >> 4; cz++)
					w.setChunkForced(cx, cz, true);
			build(w, s);
		}
		IFluidHandler fh = w.getCapability(Capabilities.FluidHandler.BLOCK, jet, Direction.DOWN);
		if (fh != null)
			fh.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
		fh = w.getCapability(Capabilities.FluidHandler.BLOCK, turbine, Direction.DOWN);
		if (fh != null)
			fh.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
		for (BlockPos p : magnets) {
			if (w.getBlockEntity(p) instanceof ToroidMagnetBlockEntity m)
				m.getFluidHandler(null).fill(new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), 8000), IFluidHandler.FluidAction.EXECUTE);
			if (w.getBlockEntity(p.below(3)) instanceof VanDeGraffBlockEntity v && v.getCharge() < 8192)
				v.setCharge(8192);
		}
		if (w.getBlockEntity(heater.offset(2, 2, 2)) instanceof FusionHeaterBlockEntity h) {
			if (h.getTemperature() < FusionHeaterBlockEntity.PLASMA_TEMP)
				h.setTemperature(FusionHeaterBlockEntity.PLASMA_TEMP+5000000);
			h.getDeuteriumTank().fill(new FluidStack(ReactorFluids.get("deuterium"), 4000), IFluidHandler.FluidAction.EXECUTE);
			h.getTritiumTank().fill(new FluidStack(ReactorFluids.get("tritium"), 4000), IFluidHandler.FluidAction.EXECUTE);
		}
		for (BlockPos p : BlockPos.betweenClosed(s.offset(14, 0, 0), s.offset(26, 6, 0)))
			if (w.getBlockEntity(p) instanceof MagneticPipeBlockEntity mp && mp.getCharge() < 50000)
				mp.setCharge(100000);
	}

	/** "/reikadev tokamak": builds the tokamak around {@code s} and keeps it supplied from then on. */
	public static void buildAt(ServerLevel w, BlockPos s) {
		build(w, s);
		center = s;
	}

	private static void build(ServerLevel w, BlockPos s) {
		for (BlockPos p : BlockPos.betweenClosed(s.offset(-30, -5, -24), s.offset(30, 6, 24)))
			if (!w.getBlockState(p).isAir())
				w.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
		FusionStructures.solenoid(w, s);
		FusionMultiBlock.formSolenoid(w, s.offset(2, 0, 0));
		BlockPos bevP = s.below(), g2 = bevP.east(), g16 = g2.east();
		jet = g16.east();
		w.setBlock(bevP, RotaryBlocks.BEVEL_GEARS.get().defaultBlockState(), 3);
		((BevelGearBlockEntity)w.getBlockEntity(bevP)).direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		for (BlockPos g : new BlockPos[]{g2, g16}) {
			w.setBlock(g, RotaryBlocks.GEARBOX.get().defaultBlockState(), 3);
			GearboxBlockEntity gb = (GearboxBlockEntity)w.getBlockEntity(g);
			int ratio = g == g2 ? 2 : 16;
			gb.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, ratio));
			gb.setBlockMetadata(ratio == 2 ? 0 : 12);
			gb.fillWithLubricant();
			gb.reduction = true;
		}
		w.setBlock(jet, RotaryBlocks.JET_ENGINE.get().defaultBlockState(), 3);
		((EngineBlockEntity)w.getBlockEntity(jet)).setBlockMetadata(0);
		magnets = new ArrayList<>(FusionStructures.ring(w, s));
		Direction[] out = {Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH};
		for (int i = 0; i < 4; i++) {
			BlockPos inj = s.offset(FusionStructures.INJECTORS[i][0], 0, FusionStructures.INJECTORS[i][1]);
			FusionStructures.injectorFrame(w, inj, out[i]);
			BlockPos trig = FusionStructures.injectorTrigger(inj, out[i]);
			if (FusionMultiBlock.checkInjector(w, trig))
				FusionMultiBlock.formInjector(w, trig);
			if (i > 0 && w.getBlockEntity(inj) instanceof FusionInjectorBlockEntity te)
				te.getTank().fill(new FluidStack(ReactorFluids.get("fusion_plasma"), 8000), IFluidHandler.FluidAction.EXECUTE);
		}
		for (BlockPos p : magnets)
			w.setBlock(p.below(3), RotaryBlocks.VAN_DE_GRAFF.get().defaultBlockState(), 3);
		//the preheater feeds the east injector through magnetic pipes; a heat ray (microturbine) points into its lens
		heater = s.offset(22, -6, -2);
		FusionStructures.heater(w, heater);
		for (int x = 21; x <= 23; x++)
			w.setBlock(s.offset(x, 0, 0), ReactorBlocks.MAGNETIC_PIPE.get().defaultBlockState(), 3);
		FusionMultiBlock.formHeater(w, heater);
		BlockPos ray = heater.offset(2, 2, 6);
		w.setBlock(ray, RotaryBlocks.HEAT_RAY.get().defaultBlockState(), 3);
		((HeatRayBlockEntity)w.getBlockEntity(ray)).setBlockMetadata(3);
		turbine = ray.south();
		w.setBlock(turbine, RotaryBlocks.MICRO_ENGINE.get().defaultBlockState(), 3);
		((EngineBlockEntity)w.getBlockEntity(turbine)).setBlockMetadata(2);
		//neutron absorbers outside the ring
		for (int i = 4; i <= 9; i++) {
			w.setBlock(s.offset(17, 0, i), ReactorBlocks.NEUTRON_ABSORBER.get().defaultBlockState(), 3);
			w.setBlock(s.offset(-17, 0, i), ReactorBlocks.NEUTRON_ABSORBER.get().defaultBlockState(), 3);
			w.setBlock(s.offset(i, 0, -17), ReactorBlocks.NEUTRON_ABSORBER.get().defaultBlockState(), 3);
			w.setBlock(s.offset(i, 0, 17), ReactorBlocks.NEUTRON_ABSORBER.get().defaultBlockState(), 3);
		}
	}
}
