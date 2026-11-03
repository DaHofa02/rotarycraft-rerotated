/*******************************************************************************
 * Tests for the ported ReactorCraft CPU remote control and the CPU's temperature checks (original ItemRemoteControl,
 * LinkableReactorCore, TileEntityCPU.TemperatureMonitor). Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for
 * distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.item.RemoteControlItem;
import reika.reactorcraft.reactor.ControlRodBlockEntity;
import reika.reactorcraft.reactor.CpuBlockEntity;
import reika.reactorcraft.reactor.NuclearCoreBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorComponents;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RemoteTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static ItemStack remote(int charge) {
		ItemStack is = new ItemStack(ReactorItems.REMOTE.get());
		is.set(ReactorComponents.CHARGE.get(), charge);
		return is;
	}

	/** Right-clicks the block at p with the remote in the mock player's main hand (Item.useOn). */
	private static void useOn(GameTestHelper h, Player ep, ItemStack is, BlockPos p) {
		ep.setItemInHand(InteractionHand.MAIN_HAND, is);
		BlockPos a = h.absolutePos(p);
		ReactorItems.REMOTE.get().useOn(new UseOnContext(ep, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(a), Direction.UP, a, false)));
	}

	/**
	 * Microturbine -> CPU with one control rod; the remote links the CPU, then a fuel core away from the reactor. With the rod raised and
	 * the CPU powered 4 times over, the linked core getting hotter than 800 C makes the CPU SCRAM. Breaking the core removes its check.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void linkedCoreTemperatureScrams(GameTestHelper h) {
		h.setBlock(new BlockPos(6, 1, 4), RotaryBlocks.MICRO_ENGINE.get());
		EngineBlockEntity eng = (EngineBlockEntity)h.getBlockEntity(new BlockPos(6, 1, 4));
		eng.setBlockMetadata(0);
		eng.addFuel(50000);
		h.setBlock(new BlockPos(5, 1, 4), ReactorBlocks.CPU.get());
		CpuBlockEntity cpu = (CpuBlockEntity)h.getBlockEntity(new BlockPos(5, 1, 4));
		h.setBlock(new BlockPos(4, 1, 4), ReactorBlocks.CONTROL_ROD.get());
		ControlRodBlockEntity rod = (ControlRodBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		h.setBlock(new BlockPos(1, 1, 1), ReactorBlocks.FUEL_ROD.get());
		NuclearCoreBlockEntity core = (NuclearCoreBlockEntity)h.getBlockEntity(new BlockPos(1, 1, 1));

		Player ep = h.makeMockPlayer(GameType.SURVIVAL);
		ItemStack is = remote(RemoteControlItem.CREATIVE_CHARGE);
		useOn(h, ep, is, new BlockPos(4, 1, 1)); //nothing linked yet: no effect
		h.assertTrue(RemoteControlItem.getLink(is) == null, "linked to a non-CPU");
		useOn(h, ep, is, new BlockPos(1, 1, 1));
		h.assertTrue(cpu.getNumberTemperatureChecks() == 0, "core linked without a CPU");
		useOn(h, ep, is, new BlockPos(5, 1, 4));
		h.assertTrue(RemoteControlItem.getLink(is) != null && RemoteControlItem.getLink(is).pos().equals(h.absolutePos(new BlockPos(5, 1, 4))), "CPU not linked");
		useOn(h, ep, is, new BlockPos(1, 1, 1));
		h.assertTrue(cpu.getNumberTemperatureChecks() == 1, "core not linked: "+cpu.getNumberTemperatureChecks());

		int[] phase = {0, 0};
		h.onEachTick(() -> {
			if (phase[0] == 0 && cpu.getPower() >= cpu.getMinPower()*4 && cpu.getNumberRods() == 1) {
				cpu.raiseAllRods();
				phase[0] = 1;
				phase[1] = (int)h.getTick();
			}
			else if (phase[0] == 1 && h.getTick() >= phase[1]+40) {
				h.assertTrue(!rod.isActive() && rod.getRodPosition() == ControlRodBlockEntity.MAXOFFSET, "cool reactor: rod not raised, at "+rod.getRodPosition());
				phase[0] = 2;
			}
			if (phase[0] == 2)
				core.setTemperature(900);
		});
		h.succeedWhen(() -> {
			h.assertTrue(phase[0] == 2 && rod.isActive(), "phase "+phase[0]+", rod at "+rod.getRodPosition()+", CPU "+cpu.getPower()+" W");
			h.setBlock(new BlockPos(1, 1, 1), Blocks.AIR);
			h.assertTrue(cpu.getNumberTemperatureChecks() == 0, "broken core still checked");
		});
	}

	/** Range 4*log2(charge) blocks around the linked CPU, nothing without charge, only in the CPU's dimension; tooltip data in components. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void remoteRangeAndCharge(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 1, 4), ReactorBlocks.CPU.get());
		Player ep = h.makeMockPlayer(GameType.SURVIVAL);
		ItemStack is = remote(32000);
		useOn(h, ep, is, new BlockPos(4, 1, 4));
		RemoteControlItem item = ReactorItems.REMOTE.get();
		h.assertTrue(RemoteControlItem.getRange(is) == 56, "range "+RemoteControlItem.getRange(is));
		BlockPos cpu = h.absolutePos(new BlockPos(4, 1, 4));
		ep.setPos(cpu.getX()+50.5, cpu.getY(), cpu.getZ()+0.5);
		h.assertTrue(item.canUse(is, h.getLevel(), ep), "not usable 50 blocks away");
		ep.setPos(cpu.getX()+60.5, cpu.getY(), cpu.getZ()+0.5);
		h.assertFalse(item.canUse(is, h.getLevel(), ep), "usable 60 blocks away");
		ItemStack low = remote(16);
		low.set(ReactorComponents.LINKED_CPU.get(), RemoteControlItem.getLink(is));
		h.assertTrue(RemoteControlItem.getRange(low) == 16, "range at 16 kJ "+RemoteControlItem.getRange(low));
		ItemStack empty = remote(0);
		empty.set(ReactorComponents.LINKED_CPU.get(), RemoteControlItem.getLink(is));
		ep.setPos(cpu.getX()+0.5, cpu.getY()+1, cpu.getZ()+0.5);
		h.assertFalse(item.canUse(empty, h.getLevel(), ep), "usable without charge");
		h.succeed();
	}
}
