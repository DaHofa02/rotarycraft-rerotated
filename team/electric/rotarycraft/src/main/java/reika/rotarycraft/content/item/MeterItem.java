/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.item;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.api.power.ShaftPowerReceiver;
import reika.rotarycraft.content.blockentity.IOMachineBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.SplitterBlockEntity;
import reika.rotarycraft.content.blockentity.TemperatureMachine;
import reika.rotarycraft.content.blockentity.TransmissionMachineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.power.PowerSourceTracker;

/** Original: Reika.RotaryCraft.Items.Tools.ItemMeter (Angular Transducer) - reports power, temperature and fuel in chat. */
public class MeterItem extends Item {

	public MeterItem(Properties props) {
		super(props);
	}

	private static void send(Player ep, String s) {
		ep.sendSystemMessage(Component.literal(s));
	}

	private static String label(String key) {
		return Component.translatable("label.rotarycraft."+key).getString();
	}

	private static void sendLocalized(Player ep, String key) {
		ep.sendSystemMessage(Component.translatable("message.rotarycraft."+key));
	}

	/** Original: ReikaDateHelper.getSecondsAsClock */
	private static String clock(int time) {
		int hour = time*20/72000;
		int minute = (time*20-hour*72000)/1200;
		int second = (time*20-hour*72000-minute*1200)/20;
		return String.format("%02d:%02d:%02d", hour, minute, second);
	}

	private static String side(Direction d) {
		String n = d.getName();
		return Character.toUpperCase(n.charAt(0))+n.substring(1);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level world = ctx.getLevel();
		Player ep = ctx.getPlayer();
		if (ep == null)
			return InteractionResult.PASS;
		if (world.isClientSide)
			return InteractionResult.SUCCESS;
		BlockEntity tile = world.getBlockEntity(ctx.getClickedPos());
		String name = tile instanceof RotaryCraftBlockEntity r ? r.getName() : tile != null ? tile.getClass().getSimpleName() : "null";

		if (tile instanceof IOMachineBlockEntity io)
			io.iotick = 512;

		if (tile instanceof TemperatureMachine th)
			send(ep, String.format("%s %s: %.0fC", name, label("temperature"), (double)th.getTemperature()));

		if (tile instanceof PowerSourceTracker pst) {
			send(ep, "Power is being received from:");
			for (String s : pst.getPowerSources(pst, null).getMessages())
				send(ep, s);
		}

		if (tile instanceof EngineBlockEntity te) {
			send(ep, String.format("%s producing %s", name, PowerFormat.formatPowerIO(te.omega, te.power)));
			if (te.getEngineType().isAirBreathing() && te.isDrowned(world, ctx.getClickedPos()))
				sendLocalized(ep, "drowning");
			if (te.getEngineType().burnsFuel())
				send(ep, String.format("%s: %s", label("fuel"), clock(te.getFuelDuration())));
			if (te.getEngineType().requiresLubricant())
				send(ep, String.format("Lubricant: %.0fmB", (double)te.getLube()));
			if (te.getEngineType().isWaterPiped())
				send(ep, String.format("Water: %.0fmB", (double)te.getWater()));
		}
		else if (tile instanceof ShaftPowerEmitter sp) {
			send(ep, String.format("%s producing %s", name, PowerFormat.formatPowerIO(sp.getOmega(), sp.getPower())));
		}
		if (tile instanceof ShaftPowerReceiver sp) {
			send(ep, String.format("%s receiving %s", name, PowerFormat.formatPowerIO(sp.getOmega(), sp.getPower())));
		}

		if (tile instanceof SplitterBlockEntity te) {
			if (te.isSplitting()) {
				send(ep, String.format("Input side %s receiving %s", side(te.getReadDirection()), PowerFormat.formatPowerIO(te.omega, (long)te.omega*te.torque)));
				send(ep, String.format("Output to side %s: %s", side(te.getWriteDirection()), PowerFormat.formatPowerIO(te.omega, (long)te.omega*te.torqueOut1)));
				send(ep, String.format("Output to side %s: %s", side(te.getWriteDirection2()), PowerFormat.formatPowerIO(te.omega, (long)te.omega*te.torqueOut2)));
			}
			else {
				send(ep, String.format("Input side %s receiving %s", side(te.getReadDirection()), PowerFormat.formatPowerIO(te.getInputSpeed1(), (long)te.getInputSpeed1()*te.getInputTorque1())));
				send(ep, String.format("Input side %s receiving %s", side(te.getReadDirection2()), PowerFormat.formatPowerIO(te.getInputSpeed2(), (long)te.getInputSpeed2()*te.getInputTorque2())));
				send(ep, String.format("Output to side %s: %s", side(te.getWriteDirection()), PowerFormat.formatPowerIO(te.omega, te.power)));
			}
		}
		else if (tile instanceof TransmissionMachineBlockEntity te) {
			send(ep, String.format("%s transmitting %s", name, PowerFormat.formatPowerIO(te.omega, te.power)));
		}
		return InteractionResult.CONSUME;
	}
}
