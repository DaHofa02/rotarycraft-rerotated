/*******************************************************************************
 * Tests for the ported RotaryCraft belt hub, chain drive and take-off belt (original TileEntityBeltHub, TileEntityChainDrive,
 * TileEntitySplitBelt). Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BeltHubBlockEntity;
import reika.rotarycraft.content.blockentity.FanBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BeltTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Two hubs of a kind at (2, 2, 4) (driving) and (2, 2, 7) (receiving), both with this metadata, joined. */
	private static BeltHubBlockEntity[] pair(GameTestHelper h, Block hub, int meta) {
		BeltHubBlockEntity drive = place(h, hub, 2, 2, 4, meta, BeltHubBlockEntity.class);
		BeltHubBlockEntity recv = place(h, hub, 2, 2, 7, meta, BeltHubBlockEntity.class);
		recv.isReceivingEnd = true;
		h.assertTrue(drive.tryConnect(recv.getBlockPos()) && recv.tryConnect(drive.getBlockPos()), "hubs do not connect");
		return new BeltHubBlockEntity[] {drive, recv};
	}

	/** Gas engine (meta 1: east) into the driving hub (meta 1: input west); the receiving hub gives it west to a fan (meta 0: input east). */
	private static FanBlockEntity gasRig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		return place(h, RotaryBlocks.FAN.get(), 1, 2, 7, 0, FanBlockEntity.class);
	}

	/** Gas turbine choked by a wall (16384 rad/s @ 1024 Nm, meta 0: output west) into the driving hub (meta 0: input east); fan meta 1 (input west). */
	private static FanBlockEntity jetRig(GameTestHelper h) {
		place(h, RotaryBlocks.JET_ENGINE.get(), 3, 2, 4, 0, JetEngineBlockEntity.class).addFuel(100000);
		h.setBlock(new BlockPos(4, 2, 4), Blocks.COBBLESTONE_WALL);
		return place(h, RotaryBlocks.FAN.get(), 3, 2, 7, 1, FanBlockEntity.class);
	}

	/**
	 * 512 rad/s @ 128 Nm go through the belt unchanged (below 8192 of each). A block in the way breaks the connection; taken away again,
	 * the power flows; breaking the driving hub gives back the two belts between them.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void beltCarriesPower(GameTestHelper h) {
		FanBlockEntity fan = gasRig(h);
		BeltHubBlockEntity[] p = pair(h, RotaryBlocks.BELT_HUB.get(), 1);
		h.assertFalse(p[0].tryConnect(new BlockPos(2, 2, 7)), "a second connection");
		int[] phase = {0};
		BlockPos gap = new BlockPos(2, 2, 5);
		h.succeedWhen(() -> {
			switch (phase[0]) {
				case 0 -> {
					h.assertTrue(p[1].getOmega() == 512 && p[1].getTorque() == 128 && fan.getPower() == 65536,
							"receiver "+p[1].getOmega()+" rad/s @ "+p[1].getTorque()+" Nm, fan "+fan.getPower()+" W");
					h.setBlock(gap, Blocks.STONE);
					phase[0] = 1;
					h.fail("running; now blocked");
				}
				case 1 -> {
					h.assertTrue(!p[1].hasValidConnection() && p[1].getPower() == 0 && fan.getPower() == 0, "still running through stone");
					h.setBlock(gap, Blocks.AIR);
					phase[0] = 2;
					h.fail("blocked; now clear");
				}
				case 2 -> {
					h.assertTrue(fan.getPower() == 65536, "fan "+fan.getPower()+" W after clearing");
					h.destroyBlock(new BlockPos(2, 2, 4));
					phase[0] = 3;
					h.fail("now broken");
				}
				default -> {
					int belts = 0;
					for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(2, 2, 4))).inflate(3)))
						if (e.getItem().is(RotaryItems.part(CraftingPart.BELT)))
							belts += e.getItem().getCount();
					h.assertTrue(belts == 2, belts+" belts dropped");
					h.assertTrue(p[1].getConnection() == null, "receiver still connected");
				}
			}
		});
	}

	/** 16384 rad/s: a belt slips above 8192 rad/s (8192 + sqrt(8192) = 8282 rad/s); a chain carries all of it. Wet, the belt slips from 2048. */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void beltSlipsChainHolds(GameTestHelper h) {
		FanBlockEntity fan = jetRig(h);
		BeltHubBlockEntity[] p = pair(h, RotaryBlocks.BELT_HUB.get(), 0);
		boolean[] chain = {false};
		BeltHubBlockEntity[][] cur = {p};
		h.succeedWhen(() -> {
			BeltHubBlockEntity r = cur[0][1];
			if (!chain[0]) {
				h.assertTrue(r.getOmega() == 8282 && r.getTorque() == 1024 && r.isSlipping(), "belt gives "+r.getOmega()+" rad/s @ "+r.getTorque()+" Nm");
				h.assertTrue(r.copyOmegaFromDriverSide(16384) == 8282, "slip formula");
				r.makeWet(1);
				h.assertTrue(r.copyOmegaFromDriverSide(16384) == 2048+(int)Math.sqrt(16384-2048) && r.copyTorqueFromDriverSide(4096) == 2048, "wet belt");
				cur[0] = pair(h, RotaryBlocks.CHAIN_DRIVE.get(), 0);
				chain[0] = true;
				h.fail("belt slips; now a chain");
			}
			h.assertTrue(r.getOmega() == 16384 && r.getTorque() == 1024 && !r.isSlipping() && fan.getPower() == 16777216L,
					"chain gives "+r.getOmega()+" rad/s @ "+r.getTorque()+" Nm, fan "+fan.getPower());
		});
	}

	/**
	 * Take-off belt: the driving hub passes 128 - 64 = 64 Nm straight on (fan east of it), the belt carries 64 Nm; the receiving hub gets
	 * nothing from behind, so the speeds average to (0 + 512) / 2 = 256 rad/s, and gives it east to a second fan.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void takeOffBeltSplitsPower(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		FanBlockEntity through = place(h, RotaryBlocks.FAN.get(), 3, 2, 4, 1, FanBlockEntity.class);
		FanBlockEntity off = place(h, RotaryBlocks.FAN.get(), 3, 2, 7, 1, FanBlockEntity.class);
		BeltHubBlockEntity[] p = pair(h, RotaryBlocks.SPLIT_BELT.get(), 1);
		h.succeedWhen(() -> {
			h.assertTrue(through.getOmega() == 512 && through.getTorque() == 64, "straight on "+through.getOmega()+" rad/s @ "+through.getTorque()+" Nm");
			h.assertTrue(p[1].getOmega() == 256 && p[1].getTorque() == 64 && off.getPower() == 256*64, "take-off "+p[1].getOmega()+" rad/s @ "+p[1].getTorque()+" Nm, fan "+off.getPower());
		});
	}
}
