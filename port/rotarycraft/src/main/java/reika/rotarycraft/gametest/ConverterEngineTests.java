/*******************************************************************************
 * Tests for the port's magnetostatic engine and dynamo (FE converters). Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.DynamoBlockEntity;
import reika.rotarycraft.content.blockentity.MagneticEngineBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ConverterEngineTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/**
	 * FE fed into its back each tick: at speed step 8 a tier-0 engine runs up to 256 rad/s at 8 Nm and uses ceil(floor(2048/520)/0.9) = 4 FE a tick;
	 * the magnet upgrade 1 makes it tier 1 (32 Nm); with "without signal" a redstone block stops it; energy only goes in at the back.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void magneticEngineTurnsFEIntoShaftPower(GameTestHelper h) {
		MagneticEngineBlockEntity te = place(h, RotaryBlocks.MAGNETIC_ENGINE.get(), 4, 2, 4, 0, MagneticEngineBlockEntity.class);
		te.setSpeedStep(8);
		h.assertTrue(te.getConsumption() == 4, "uses "+te.getConsumption());
		int[] stage = {0};
		h.onEachTick(() -> {
			var e = te.getEnergyStorage(Direction.EAST);
			if (e != null)
				e.receiveEnergy(100, false);
		});
		h.succeedWhen(() -> {
			h.assertTrue(te.getEnergyStorage(Direction.WEST) == null && te.getEnergyStorage(Direction.EAST) != null, "energy side");
			switch (stage[0]) {
				case 0 -> {
					h.assertTrue(te.getOmega() == 256 && te.getTorque() == 8, "tier 0: "+te.getOmega()+" rad/s "+te.getTorque()+" Nm");
					h.assertTrue(te.canUpgradeWith(new ItemStack(RotaryItems.UPGRADE_MAGNET_1.get())) && !te.canUpgradeWith(new ItemStack(RotaryItems.UPGRADE_MAGNET_2.get())), "upgrade order");
					te.upgrade(new ItemStack(RotaryItems.UPGRADE_MAGNET_1.get()));
					stage[0] = 1;
					h.fail("upgraded");
				}
				case 1 -> {
					h.assertTrue(te.getTier() == 1 && te.getTorque() == 32, "tier 1: "+te.getTorque()+" Nm");
					te.stepRedstoneMode();
					te.stepRedstoneMode();
					h.setBlock(new BlockPos(4, 3, 4), Blocks.REDSTONE_BLOCK);
					stage[0] = 2;
					h.fail("signal");
				}
				default -> h.assertTrue(te.getOmega() == 0, "stopped by the signal: "+te.getOmega());
			}
		});
	}

	/**
	 * Gas engine -> dynamo -> magnetostatic engine: the dynamo (input from the west) pushes min(torque, 1024)*min(speed, 8192)/520 FE a tick
	 * into the engine behind it, which then runs; the flux upgrade is accepted once.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void dynamoFeedsAMagneticEngine(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		DynamoBlockEntity dyn = place(h, RotaryBlocks.DYNAMO.get(), 2, 2, 4, 1, DynamoBlockEntity.class);
		MagneticEngineBlockEntity te = place(h, RotaryBlocks.MAGNETIC_ENGINE.get(), 3, 2, 4, 1, MagneticEngineBlockEntity.class);
		te.setSpeedStep(8);
		h.succeedWhen(() -> {
			h.assertTrue(dyn.getOutputSide() == Direction.EAST, "dynamo writes "+dyn.getOutputSide());
			long expect = (long)Math.min(dyn.getTorque(), 1024)*Math.min(dyn.getOmega(), 8192)/520;
			h.assertTrue(dyn.getPower() > 0 && dyn.getGeneratedFE() == expect, "dynamo "+dyn.getGeneratedFE()+" FE/t at "+dyn.getPower()+" W");
			h.assertTrue(dyn.delivered > 0 && te.getOmega() == 256, "engine "+te.getOmega()+" rad/s, delivered "+dyn.delivered);
			h.assertTrue(dyn.canUpgradeWith(new ItemStack(RotaryItems.UPGRADE_FLUX.get())), "flux upgrade");
			dyn.upgrade(new ItemStack(RotaryItems.UPGRADE_FLUX.get()));
			h.assertTrue(!dyn.canUpgradeWith(new ItemStack(RotaryItems.UPGRADE_FLUX.get())), "only once");
		});
	}

	/**
	 * Tier 5 at 8192 rad/s (67 MW): without coolant it reaches its 500 C limit at once (the explosion is switched off here); with liquid
	 * nitrogen it stays below and the nitrogen boils off.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void magneticEngineNeedsCoolant(GameTestHelper h) {
		MagneticEngineBlockEntity.allowOverheat = false;
		MagneticEngineBlockEntity hot = place(h, RotaryBlocks.MAGNETIC_ENGINE.get(), 2, 2, 2, 0, MagneticEngineBlockEntity.class);
		MagneticEngineBlockEntity cold = place(h, RotaryBlocks.MAGNETIC_ENGINE.get(), 2, 2, 6, 0, MagneticEngineBlockEntity.class);
		for (MagneticEngineBlockEntity te : new MagneticEngineBlockEntity[] {hot, cold}) {
			te.setTier(5);
			te.setSpeedStep(13);
		}
		cold.addCoolant(24000);
		h.onEachTick(() -> {
			hot.getEnergyStorage(null).receiveEnergy(Integer.MAX_VALUE, false);
			cold.getEnergyStorage(null).receiveEnergy(Integer.MAX_VALUE, false);
		});
		h.runAfterDelay(300, () -> {
			MagneticEngineBlockEntity.allowOverheat = true;
			h.assertTrue(hot.getOmega() == 8192 && hot.getTorque() == 8192, "tier 5: "+hot.getOmega()+" rad/s "+hot.getTorque()+" Nm");
			h.assertTrue(hot.getTemperature() > MagneticEngineBlockEntity.MAXTEMP, "no coolant: "+hot.getTemperature()+" C");
			h.assertTrue(cold.getTemperature() < MagneticEngineBlockEntity.MAXTEMP && cold.getCoolant() < 24000, "cooled: "+cold.getTemperature()+" C, "+cold.getCoolant()+" mB");
			h.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
			h.setBlock(new BlockPos(2, 2, 6), Blocks.AIR);
			h.succeed();
		});
	}
}
