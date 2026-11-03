/*******************************************************************************
 * Dev-only visual check for the private RotaryCraft port. Not part of the original mod; inactive unless the system property
 * "rotarycraft.showcase" is set (only the showcase run configuration sets it).
 ******************************************************************************/
package reika.rotarycraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;

/** Takes screenshots of the showcase world from fixed viewpoints, then closes the client. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, value = Dist.CLIENT)
public final class DevShowcase {

	private static final boolean ACTIVE = System.getProperty("rotarycraft.showcase") != null;

	/** x, y, z, yaw, pitch */
	private static final double[][] VIEWS = parseViews(System.getProperty("rotarycraft.showcase.views"), new double[][] {
			{4.5, -59, -4.5, 0, 25},
			{7.5, -58.6, -1.6, 0, 40},
			{5.0, -58.4, 4.8, 0, 22},
			{5.0, -58.0, 8.0, 0, 35},
			{6.0, -58.3, 13.2, 0, 22},
			{7.0, -57.6, 24.5, 180, 32},
			{5.0, -57.2, 36.5, 180, 38},
			{4.0, -58.2, 40.5, 180, 30},
			{8.5, -58.6, 41.8, 180, 32},
			{5.0, -58.0, 50.5, 180, 38},
			{5.0, -58.2, 56.5, 0, 30},
	});

	/** Machines whose GUI is opened and captured after the views: gas engine, gearbox, bevel, junction in split mode. */
	private static final BlockPos[] GUIS = parseGuis(System.getProperty("rotarycraft.showcase.guis"), new BlockPos[] {new BlockPos(9, -60, 3), new BlockPos(8, -60, 3), new BlockPos(7, -60, 3), new BlockPos(9, -60, 10), new BlockPos(2, -60, 16), new BlockPos(4, -60, 19), new BlockPos(8, -60, 19), new BlockPos(2, -60, 22), new BlockPos(8, -60, 33), new BlockPos(6, -60, 38), new BlockPos(8, -60, 38), new BlockPos(10, -60, 38), new BlockPos(11, -60, 45)});

	/** Another mod's showcase run (ElectriCraft) passes its own viewpoints: "x,y,z,yaw,pitch;x,y,z,yaw,pitch;..." */
	private static double[][] parseViews(String prop, double[][] def) {
		if (prop == null)
			return def;
		String[] parts = prop.split(";");
		double[][] ret = new double[parts.length][];
		for (int i = 0; i < parts.length; i++)
			ret[i] = java.util.Arrays.stream(parts[i].split(",")).mapToDouble(Double::parseDouble).toArray();
		return ret;
	}

	/** "x,y,z;x,y,z;..." (empty string: no GUIs) */
	private static BlockPos[] parseGuis(String prop, BlockPos[] def) {
		if (prop == null)
			return def;
		if (prop.isBlank())
			return new BlockPos[0];
		String[] parts = prop.split(";");
		BlockPos[] ret = new BlockPos[parts.length];
		for (int i = 0; i < parts.length; i++) {
			String[] c = parts[i].split(",");
			ret[i] = new BlockPos(Integer.parseInt(c[0].trim()), Integer.parseInt(c[1].trim()), Integer.parseInt(c[2].trim()));
		}
		return ret;
	}

	private static int ticks;

	private DevShowcase() {}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post e) {
		if (!ACTIVE)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null)
			return;
		ticks++;
		if (ticks == 20 && mc.getSingleplayerServer() != null) {
			mc.getSingleplayerServer().execute(() -> {
				ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
				if (sp != null)
					sp.getInventory().setItem(sp.getInventory().selected, GearboxBlock.gearboxItem(GearboxTypes.STEEL, 4));
			});
		}
		int view = (ticks-100)/60;
		int phase = (ticks-100)%60;
		if (ticks >= 100 && view >= VIEWS.length) {
			int gui = view-VIEWS.length;
			if (gui >= GUIS.length) {
				mc.stop();
				return;
			}
			if (phase == 0 && mc.getSingleplayerServer() != null) {
				BlockPos p = GUIS[gui];
				mc.getSingleplayerServer().execute(() -> {
					ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
					if (sp != null) {
						sp.closeContainer();
						sp.teleportTo(p.getX()+0.5, p.getY()+1, p.getZ()-1.5); //machines only open for players within 8 blocks

						BlockState st = sp.level().getBlockState(p);
						st.useWithoutItem(sp.level(), sp, new BlockHitResult(p.getCenter(), Direction.UP, p, false));
					}
				});
			}
			if (phase == 50)
				Screenshot.grab(mc.gameDirectory, "showcase_gui_"+gui+".png", mc.getMainRenderTarget(), msg -> RotaryCraft.LOGGER.info("showcase: {}", msg.getString()));
			return;
		}
		if (ticks < 100)
			return;
		double[] v = VIEWS[view];
		if (phase == 0) {
			mc.player.setPos(v[0], v[1], v[2]);
			mc.player.setYRot((float)v[3]);
			mc.player.setXRot((float)v[4]);
			mc.player.getAbilities().flying = true;
		}
		if (phase == 0 && view == 4) {
			//Show the I/O overlay as after a screwdriver click (client-side only)
			for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, -60, 16), new BlockPos(10, -59, 16)))
				if (mc.level.getBlockEntity(p) instanceof reika.rotarycraft.content.blockentity.IOMachineBlockEntity io)
					io.iotick = 512;
		}
		if (phase == 40 && view == 4) {
			if (mc.level.getBlockEntity(new BlockPos(2, -60, 16)) instanceof reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity jet)
				RotaryCraft.LOGGER.info("showcase: client gas turbine: {} rad/s, {} Nm, T {}, afterburning {}", jet.omega, jet.torque, jet.temperature, jet.isAfterburning());
			if (mc.level.getBlockEntity(new BlockPos(8, -59, 16)) instanceof reika.rotarycraft.content.blockentity.engine.EngineBlockEntity eng)
				RotaryCraft.LOGGER.info("showcase: client gas engine on ECU: {} rad/s, {} Nm", eng.omega, eng.torque);
		}
		if (phase == 40 && view == 5) {
			for (BlockPos p : new BlockPos[] {new BlockPos(4, -60, 20), new BlockPos(10, -60, 20)})
				if (mc.level.getBlockEntity(p) instanceof reika.rotarycraft.content.blockentity.FrictionHeaterBlockEntity fh)
					RotaryCraft.LOGGER.info("showcase: client friction heater at {}: {} rad/s, {} Nm, {} C", p, fh.omega, fh.torque, fh.getTemperature());
			if (mc.level.getBlockEntity(new BlockPos(4, -60, 19)) instanceof reika.rotarycraft.content.blockentity.BlastFurnaceBlockEntity bf)
				RotaryCraft.LOGGER.info("showcase: client blast furnace: {} C", bf.getTemperature());
		}
		if (phase == 40 && view == 1) {
			for (BlockPos p : new BlockPos[] {new BlockPos(5, -60, 0), new BlockPos(7, -60, 4)})
				if (mc.level.getBlockEntity(p) instanceof DynamometerBlockEntity d)
					RotaryCraft.LOGGER.info("showcase: client dynamometer at {}: {} rad/s, {} Nm, {} W, phi {}", p, d.omega, d.torque, d.power, d.phi);
			if (mc.level.getBlockEntity(new BlockPos(9, -60, 3)) instanceof reika.rotarycraft.content.blockentity.engine.EngineBlockEntity eng)
				RotaryCraft.LOGGER.info("showcase: client gas engine: {} rad/s, {} Nm, fuel {}", eng.omega, eng.torque, eng.getFuelLevel());
		}
		if (phase == 40 && view == 6) {
			for (BlockPos p : new BlockPos[] {new BlockPos(2, -60, 27), new BlockPos(2, -60, 29), new BlockPos(6, -60, 27), new BlockPos(10, -58, 31)})
				if (mc.level.getBlockEntity(p) instanceof reika.rotarycraft.content.piping.PipingBlockEntity pipe)
					RotaryCraft.LOGGER.info("showcase: client pipe at {}: {} {} mB, state {}", p, pipe.getFluidType(), pipe.getFluidLevel(), pipe.getBlockState());
				else
					RotaryCraft.LOGGER.info("showcase: no pipe at {}: {}", p, mc.level.getBlockState(p));
			if (mc.level.getBlockEntity(new BlockPos(10, -58, 30)) instanceof reika.rotarycraft.content.blockentity.engine.EngineBlockEntity eng)
				RotaryCraft.LOGGER.info("showcase: client steam engine: {} rad/s, {} Nm, {} C, water {}", eng.omega, eng.torque, eng.getTemperature(), eng.getWater());
		}
		if (phase == 40 && view == 8) {
			if (mc.getSingleplayerServer() != null) mc.getSingleplayerServer().execute(() -> {
			if (mc.getSingleplayerServer().overworld().getBlockEntity(new BlockPos(6, -60, 38)) instanceof reika.rotarycraft.content.blockentity.FermenterBlockEntity f)
				RotaryCraft.LOGGER.info("showcase: server fermenter: {} {} {} | {} C, water {}, {} rad/s", f.getInventory().getStackInSlot(0), f.getInventory().getStackInSlot(1), f.getInventory().getStackInSlot(2), f.getTemperature(), f.getWater(), f.omega);
			var lvl = mc.getSingleplayerServer().overworld();
			BlockPos fp = new BlockPos(6, -60, 38);
			RotaryCraft.LOGGER.info("showcase: ambient at fermenter {} C, biome {}, sea level {}, sky {}", reika.rotarycraft.lib.world.AmbientTemperature.getAmbientTemperatureAt(lvl, fp),
					reika.rotarycraft.lib.world.AmbientTemperature.getBiomeTemp(lvl, fp), lvl.getSeaLevel(), lvl.canSeeSky(fp.above()));
			});
		}
		if (phase == 50)
			Screenshot.grab(mc.gameDirectory, "showcase_"+view+".png", mc.getMainRenderTarget(), msg -> RotaryCraft.LOGGER.info("showcase: {}", msg.getString()));
	}
}
