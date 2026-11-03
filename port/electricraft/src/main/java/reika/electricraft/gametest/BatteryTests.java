/*******************************************************************************
 * Tests for the ported ElectriCraft battery (original TileEntityBattery): charging from the network, discharging with a redstone signal
 * at the tier's fixed voltage/current, the charge kept on the dropped item, comparator output.
 * Private NeoForge 1.21.1 port of ElectriCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.electricraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.electricraft.ElectriCraft;
import reika.electricraft.content.block.BatteryBlock;
import reika.electricraft.content.blockentity.BatteryBlockEntity;
import reika.electricraft.content.blockentity.GeneratorBlockEntity;
import reika.electricraft.content.blockentity.MotorBlockEntity;
import reika.electricraft.registry.BatteryType;
import reika.electricraft.registry.ElectriBlocks;
import reika.electricraft.registry.ElectriComponents;
import reika.electricraft.registry.WireType;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ElectriCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BatteryTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static BatteryBlockEntity battery(GameTestHelper h, BatteryType type, BlockPos p) {
		h.setBlock(p, ElectriBlocks.battery(type).get());
		return (BatteryBlockEntity)h.getBlockEntity(p);
	}

	/** Generator (512 V, 2 A) -> one steel wire (64 ohm) -> battery: it charges by 448 V * 2 A per tick (J/20) and shows as charged. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void batteryChargesFromGenerator(GameTestHelper h) {
		RotaryCraftBlockEntity eng = (RotaryCraftBlockEntity)place(h, RotaryBlocks.DC_ENGINE.get(), 8, 4);
		eng.setBlockMetadata(0);
		h.setBlock(new BlockPos(8, 1, 5), Blocks.REDSTONE_BLOCK);
		GearboxBlockEntity gbx = (GearboxBlockEntity)place(h, RotaryBlocks.GEARBOX.get(), 7, 4);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.WOOD, 4));
		gbx.setBlockMetadata(4);
		GeneratorBlockEntity gen = (GeneratorBlockEntity)place(h, ElectriBlocks.GENERATOR.get(), 6, 4);
		gen.setFacing(Direction.EAST);
		h.setBlock(new BlockPos(5, 1, 4), ElectriBlocks.getWire(WireType.STEEL, false));
		BatteryBlockEntity bat = battery(h, BatteryType.REDSTONE, new BlockPos(4, 1, 4));
		long[] first = new long[1];
		h.runAtTickTime(100, () -> first[0] = bat.getStoredEnergy());
		h.runAtTickTime(140, () -> {
			long gain = bat.getStoredEnergy()-first[0];
			//40 ticks * 448 * 2 / 20 = 1792 J
			h.assertTrue(first[0] > 0 && Math.abs(gain-1792) <= 45, "charged "+first[0]+" J, then +"+gain+" J in 40 ticks (expected 1792)");
			h.assertBlockProperty(new BlockPos(4, 1, 4), BatteryBlock.CHARGED, true);
			h.succeed();
		});
	}

	private static Object place(GameTestHelper h, net.minecraft.world.level.block.Block b, int x, int z) {
		BlockPos p = new BlockPos(x, 1, z);
		h.setBlock(p, b);
		return h.getBlockEntity(p);
	}

	/**
	 * A charged redstone battery (256 V, 32 A) with a redstone signal feeds two copper wires (2 ohm each; steel would melt at 32 A) from
	 * its top to a motor: 32*8 = 256 Nm at (256-4)/8 = 31 rad/s, while its energy drops by 256*32 J/20 per tick. Without the signal it
	 * gives nothing.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void batteryDrivesMotorWithRedstone(GameTestHelper h) {
		BatteryBlockEntity bat = battery(h, BatteryType.REDSTONE, new BlockPos(6, 1, 4));
		bat.setStoredEnergy(BatteryType.REDSTONE.maxCapacity);
		h.setBlock(new BlockPos(6, 2, 4), ElectriBlocks.getWire(WireType.COPPER, false));
		h.setBlock(new BlockPos(5, 2, 4), ElectriBlocks.getWire(WireType.COPPER, false));
		h.setBlock(new BlockPos(4, 2, 4), ElectriBlocks.MOTOR.get());
		((MotorBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4))).setFacing(Direction.EAST);
		h.setBlock(new BlockPos(3, 2, 4), RotaryBlocks.DYNAMOMETER.get());
		DynamometerBlockEntity dyna = (DynamometerBlockEntity)h.getBlockEntity(new BlockPos(3, 2, 4));
		dyna.setBlockMetadata(0);
		long[] e = new long[1];
		h.runAtTickTime(80, () -> {
			h.assertTrue(dyna.power == 0, "battery without signal gives "+dyna.power+" W");
			h.assertTrue(bat.getStoredEnergy() == BatteryType.REDSTONE.maxCapacity, "battery lost energy without signal: "+bat.getStoredEnergy());
			h.setBlock(new BlockPos(6, 1, 5), Blocks.REDSTONE_BLOCK);
		});
		h.runAtTickTime(180, () -> {
			h.assertTrue(dyna.omega == 31 && dyna.torque == 256, "expected 31 rad/s @ 256 Nm, got "+dyna.omega+" @ "+dyna.torque);
			e[0] = bat.getStoredEnergy();
		});
		h.runAtTickTime(200, () -> {
			long used = e[0]-bat.getStoredEnergy();
			h.assertTrue(Math.abs(used-20*256*32/20) <= 256*32/20+1, "used "+used+" J in 20 ticks (expected 8192)");
			h.succeed();
		});
	}

	/** Breaking a battery drops it with its charge; the comparator shows 15 * stored/capacity. */
	@GameTest(template = PLATFORM)
	public static void batteryKeepsChargeAndComparator(GameTestHelper h) {
		BlockPos p = new BlockPos(4, 1, 4);
		BatteryBlockEntity bat = battery(h, BatteryType.LAPIS, p);
		bat.setStoredEnergy(BatteryType.LAPIS.maxCapacity/2);
		h.assertTrue(bat.getRedstoneOverride() == 7, "comparator "+bat.getRedstoneOverride());
		h.getLevel().destroyBlock(h.absolutePos(p), true);
		for (ItemEntity ie : h.getLevel().getEntities(EntityType.ITEM, new AABB(h.absolutePos(p)).inflate(1.5), e -> true)) {
			if (ie.getItem().is(ElectriBlocks.battery(BatteryType.LAPIS).get().asItem())) {
				long e = ie.getItem().getOrDefault(ElectriComponents.BATTERY_ENERGY.get(), -1L);
				h.assertTrue(e == BatteryType.LAPIS.maxCapacity/2, "dropped battery holds "+e+" J");
				h.succeed();
				return;
			}
		}
		h.fail("no battery dropped");
	}

	/** Original wire overload: 32 A through a steel wire (16 A) melts it into lava. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void overloadedWireMelts(GameTestHelper h) {
		BatteryBlockEntity bat = battery(h, BatteryType.REDSTONE, new BlockPos(6, 1, 4));
		bat.setStoredEnergy(BatteryType.REDSTONE.maxCapacity);
		h.setBlock(new BlockPos(6, 1, 5), Blocks.REDSTONE_BLOCK);
		h.setBlock(new BlockPos(6, 2, 4), ElectriBlocks.getWire(WireType.STEEL, false));
		h.setBlock(new BlockPos(5, 2, 4), ElectriBlocks.MOTOR.get());
		((MotorBlockEntity)h.getBlockEntity(new BlockPos(5, 2, 4))).setFacing(Direction.EAST);
		h.succeedWhen(() -> h.assertBlockPresent(Blocks.LAVA, new BlockPos(6, 2, 4)));
	}
}
