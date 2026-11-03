/*******************************************************************************
 * Live scenarios of the private NeoForge 1.21.1 port of ElectriCraft: a simulated player builds and uses an electric line, in survival
 * (crafting, items used up, mining with tools) and in creative. Not part of the original mod; not for distribution.
 ******************************************************************************/
package reika.electricraft.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.electricraft.ElectriCraft;
import reika.electricraft.content.blockentity.BatteryBlockEntity;
import reika.electricraft.registry.BatteryType;
import reika.electricraft.registry.ElectriBlocks;
import reika.electricraft.registry.ElectriComponents;
import reika.electricraft.registry.WireType;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.DifficultyEffects;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ElectriCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class LiveElectricTests {

	private static ResourceLocation id(String ns, String path) {
		return ResourceLocation.fromNamespaceAndPath(ns, path);
	}

	private static ItemStack craft(SimPlayer p, String ns, String recipe) {
		p.gatherFor(id(ns, recipe));
		return p.craft(id(ns, recipe), 0, 13);
	}

	/**
	 * Looking east the player sets down a DC engine (with a redstone block), a wooden 4:1 gearbox, an ElectriCraft generator, one steel
	 * wire, a motor and a dynamometer: 512 V at 2 A over 64 ohm -> the motor gives (512-64)/8 = 56 rad/s at 16 Nm. Breaking the wire
	 * stops the motor, laying it again starts it. A wire and a redstone battery added beside the line charge the battery; mined with a
	 * pickaxe (survival) it drops with its charge.
	 */
	private static void electricLine(GameTestHelper h, GameType mode) {
		SimPlayer p = new SimPlayer(h, mode);
		SimPlayer.floor(h, 0, 0, 8, 8);
		int d = DifficultyEffects.getDifficulty().ordinal()+1;
		ItemStack eng, gbx, gen, wire, motor, dyna, battery;
		if (p.survival()) {
			p.give(Items.CRAFTING_TABLE, 1);
			p.place(new BlockPos(1, 2, 1), Items.CRAFTING_TABLE, Direction.NORTH);
			p.open(new BlockPos(1, 2, 1));
			p.gatherFor(id("rotarycraft", "worktable"));
			ItemStack wt = p.craft(id("rotarycraft", "worktable"), 1, 0);
			p.closeMenu();
			p.place(new BlockPos(3, 2, 1), wt.getItem(), Direction.NORTH);
			p.open(new BlockPos(3, 2, 1));
			eng = craft(p, "rotarycraft", "dc_engine_worktable");
			gbx = craft(p, "rotarycraft", "gearbox_wood_4_worktable");
			gen = craft(p, ElectriCraft.MOD_ID, "generator_worktable");
			wire = craft(p, ElectriCraft.MOD_ID, "steel_wire_difficulty_"+d+"_worktable");
			motor = craft(p, ElectriCraft.MOD_ID, "motor_worktable");
			dyna = craft(p, "rotarycraft", "dynamometer_worktable");
			battery = craft(p, ElectriCraft.MOD_ID, "redstone_battery_worktable");
			p.closeMenu();
			p.give(Items.REDSTONE_BLOCK, 1);
			p.give(Items.IRON_PICKAXE, 1);
		}
		else {
			eng = new ItemStack(RotaryBlocks.DC_ENGINE.get());
			gbx = GearboxBlock.gearboxItem(GearboxTypes.WOOD, 4);
			gen = new ItemStack(ElectriBlocks.GENERATOR.get());
			wire = new ItemStack(ElectriBlocks.getWire(WireType.STEEL, false));
			motor = new ItemStack(ElectriBlocks.MOTOR.get());
			dyna = new ItemStack(RotaryBlocks.DYNAMOMETER.get());
			battery = new ItemStack(ElectriBlocks.battery(BatteryType.REDSTONE).get());
		}
		BlockPos w = new BlockPos(5, 2, 4);
		p.place(new BlockPos(8, 2, 4), eng, Direction.EAST);
		p.place(new BlockPos(8, 2, 5), new ItemStack(Items.REDSTONE_BLOCK), Direction.EAST);
		p.place(new BlockPos(7, 2, 4), gbx, Direction.EAST);
		p.place(new BlockPos(6, 2, 4), gen, Direction.EAST);
		p.place(w, wire, Direction.EAST);
		p.place(new BlockPos(4, 2, 4), motor, Direction.EAST);
		p.place(new BlockPos(3, 2, 4), dyna, Direction.EAST);
		DynamometerBlockEntity dy = (DynamometerBlockEntity)h.getBlockEntity(new BlockPos(3, 2, 4));
		h.runAtTickTime(60, () -> {
			h.assertTrue(dy.omega == 56 && dy.torque == 16, "motor: "+dy.omega+" rad/s @ "+dy.torque+" Nm");
			List<ItemStack> drops = p.mine(w, p.survival() ? Items.IRON_PICKAXE : Items.AIR);
			if (p.survival())
				h.assertTrue(!drops.isEmpty(), "the wire dropped nothing");
		});
		h.runAtTickTime(100, () -> {
			h.assertTrue(dy.power == 0, "motor still runs without the wire: "+dy.omega+" rad/s");
			p.place(w, wire, Direction.EAST);
		});
		h.runAtTickTime(140, () -> {
			h.assertTrue(dy.omega == 56 && dy.torque == 16, "after relaying the wire: "+dy.omega+" rad/s @ "+dy.torque+" Nm");
			p.place(new BlockPos(5, 2, 5), wire, Direction.EAST);
			p.place(new BlockPos(5, 2, 6), battery, Direction.EAST);
		});
		h.runAtTickTime(220, () -> {
			BatteryBlockEntity bat = (BatteryBlockEntity)h.getBlockEntity(new BlockPos(5, 2, 6));
			long e = bat.getStoredEnergy();
			h.assertTrue(e > 0, "the battery did not charge");
			List<ItemStack> drops = p.mine(new BlockPos(5, 2, 6), p.survival() ? Items.IRON_PICKAXE : Items.AIR);
			if (p.survival())
				h.assertTrue(drops.size() == 1 && drops.get(0).getOrDefault(ElectriComponents.BATTERY_ENERGY.get(), 0L) > 0, "battery drop: "+drops);
			else
				h.assertTrue(drops.isEmpty(), "creative mining dropped "+drops);
			p.leave();
			h.succeed();
		});
	}

	@GameTest(template = "platform_9x4x9", timeoutTicks = 300)
	public static void survivalElectricLine(GameTestHelper h) {
		electricLine(h, GameType.SURVIVAL);
	}

	@GameTest(template = "platform_9x4x9", timeoutTicks = 300)
	public static void creativeElectricLine(GameTestHelper h) {
		electricLine(h, GameType.CREATIVE);
	}
}
