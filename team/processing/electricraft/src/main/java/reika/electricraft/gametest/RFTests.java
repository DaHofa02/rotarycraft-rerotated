/*******************************************************************************
 * Tests for the ported ElectriCraft RF parts (original RFNetwork, TileEntityRFCable, TileEntityRFBattery) on NeoForge FE.
 * Private NeoForge 1.21.1 port of ElectriCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.electricraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.electricraft.ElectriCraft;
import reika.electricraft.content.blockentity.RFBatteryBlockEntity;
import reika.electricraft.content.blockentity.RFCableBlockEntity;
import reika.electricraft.registry.ElectriBlocks;

@GameTestHolder(ElectriCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RFTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static RFBatteryBlockEntity battery(GameTestHelper h, BlockPos p, long energy) {
		h.setBlock(p, ElectriBlocks.RF_BATTERY.get());
		RFBatteryBlockEntity b = (RFBatteryBlockEntity)h.getBlockEntity(p);
		b.setStoredEnergy(energy);
		return b;
	}

	/**
	 * Battery A (top face extracts) -> two cables -> battery B (side receives). With the network limit 0 nothing moves; at 1000 FE/t the
	 * network moves at most 1000 per tick, and what B gains A loses (up to the network buffer).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void cableMovesEnergyUpToLimit(GameTestHelper h) {
		RFBatteryBlockEntity a = battery(h, new BlockPos(2, 1, 4), 1000000);
		h.setBlock(new BlockPos(2, 2, 4), ElectriBlocks.RF_CABLE.get());
		h.setBlock(new BlockPos(3, 2, 4), ElectriBlocks.RF_CABLE.get());
		RFBatteryBlockEntity b = battery(h, new BlockPos(4, 2, 4), 0);
		RFCableBlockEntity cable = (RFCableBlockEntity)h.getBlockEntity(new BlockPos(2, 2, 4));
		long[] mark = new long[2];
		h.runAtTickTime(40, () -> {
			h.assertTrue(b.getStoredEnergy() == 0 && a.getStoredEnergy() == 1000000, "energy moved with limit 0: "+a.getStoredEnergy()+" / "+b.getStoredEnergy());
			cable.setRFLimit(1000);
		});
		h.runAtTickTime(60, () -> {
			mark[0] = b.getStoredEnergy();
			h.assertTrue(((RFCableBlockEntity)h.getBlockEntity(new BlockPos(3, 2, 4))).getRFLimit() == 1000, "limit not shared by the network");
		});
		h.runAtTickTime(100, () -> {
			long gain = b.getStoredEnergy()-mark[0];
			h.assertTrue(gain > 0 && gain <= 40*1000, "gain in 40 ticks: "+gain);
			long lost = 1000000-a.getStoredEnergy();
			h.assertTrue(lost >= b.getStoredEnergy() && lost-b.getStoredEnergy() <= 1000, "A lost "+lost+", B has "+b.getStoredEnergy());
			h.succeed();
		});
	}

	/** Side rules: no input through the top, no output through the sides; with a redstone signal it pushes into the block above. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void rfBatterySidesAndRedstone(GameTestHelper h) {
		RFBatteryBlockEntity a = battery(h, new BlockPos(4, 1, 4), 5000);
		IEnergyStorage top = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(new BlockPos(4, 1, 4)), Direction.UP);
		IEnergyStorage side = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, h.absolutePos(new BlockPos(4, 1, 4)), Direction.NORTH);
		h.assertTrue(top != null && side != null, "no FE capability");
		h.assertTrue(top.receiveEnergy(100, true) == 0 && side.extractEnergy(100, true) == 0, "side rules");
		h.assertTrue(side.receiveEnergy(100, false) == 100 && top.extractEnergy(50, false) == 50 && a.getStoredEnergy() == 5050, "stored "+a.getStoredEnergy());
		RFBatteryBlockEntity b = battery(h, new BlockPos(4, 2, 4), 0);
		h.runAtTickTime(20, () -> {
			h.assertTrue(b.getStoredEnergy() == 0, "pushed without signal");
			h.setBlock(new BlockPos(3, 1, 4), Blocks.REDSTONE_BLOCK);
		});
		h.runAtTickTime(40, () -> {
			h.assertTrue(b.getStoredEnergy() == 5050 && a.getStoredEnergy() == 0, "after the push: "+a.getStoredEnergy()+" / "+b.getStoredEnergy());
			h.succeed();
		});
	}

	/**
	 * A charged RF battery with a redstone signal pushes into a basic wireless pad above it (input from below), which passes 40 % of at
	 * most 80 FE/t into the RF battery two blocks in front of it: the target gains 32 per tick, the source loses 80.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void wirelessPadTransfers(GameTestHelper h) {
		RFBatteryBlockEntity a = battery(h, new BlockPos(2, 1, 4), 1000000);
		h.setBlock(new BlockPos(2, 2, 4), ElectriBlocks.wirelessPad(reika.electricraft.content.block.WirelessPadBlock.Tier.BASIC).get().defaultBlockState()
				.setValue(reika.electricraft.content.block.WirelessPadBlock.FACING, Direction.EAST));
		RFBatteryBlockEntity b = battery(h, new BlockPos(4, 2, 4), 0);
		h.setBlock(new BlockPos(1, 1, 4), Blocks.REDSTONE_BLOCK);
		long[] mark = new long[2];
		h.runAtTickTime(20, () -> {
			mark[0] = a.getStoredEnergy();
			mark[1] = b.getStoredEnergy();
		});
		h.runAtTickTime(40, () -> {
			long lost = mark[0]-a.getStoredEnergy(), gained = b.getStoredEnergy()-mark[1];
			h.assertTrue(gained == 20*32 && lost == 20*80, "20 ticks: source lost "+lost+", target gained "+gained);
			h.succeed();
		});
	}
}
