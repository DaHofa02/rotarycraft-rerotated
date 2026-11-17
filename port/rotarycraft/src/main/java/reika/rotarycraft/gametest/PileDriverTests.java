/*******************************************************************************
 * Tests for the ported RotaryCraft pile driver (original TileEntityPileDriver). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.block.ShaftBlock;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.PileDriverBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.ShaftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PileDriverTests {

	private static final String PLATFORM = "platform_9x8x9";
	private static final int Y = 4;

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, Y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	private static void gearbox(GameTestHelper h, int z) {
		GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 4, z, 14, GearboxBlockEntity.class);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16));
		gbx.setBlockMetadata(14);
		gbx.fillWithLubricant();
	}

	/**
	 * Gas turbine choked to 16384 rad/s @ 1024 Nm, two bedrock shafts and two bedrock 16:1 gearboxes -> 64 rad/s @ 262144 Nm into the pile
	 * driver (meta 0: axis north-south) at height 4. It drives its pile down through two empty layers to the floor: the floor's 5x5 cross
	 * (stone, first cobblestone, then gone) is cleared, its corners stay; the glass pane within 5 blocks of the blow breaks; the pile is
	 * mining pipe.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600, batch = "piledriver")
	public static void pileDriverSmashesTheFloor(GameTestHelper h) {
		place(h, RotaryBlocks.JET_ENGINE.get(), 4, 7, 2, JetEngineBlockEntity.class).addFuel(100000);
		h.setBlock(new BlockPos(4, Y, 8), Blocks.COBBLESTONE_WALL);
		for (int z = 6; z >= 5; z--)
			place(h, RotaryBlocks.SHAFT.get(), 4, z, 2, ShaftBlockEntity.class).applyComponentsFromItemStack(ShaftBlock.shaftItem(MaterialRegistry.BEDROCK, false));
		gearbox(h, 4);
		gearbox(h, 3);
		PileDriverBlockEntity te = place(h, RotaryBlocks.PILE_DRIVER.get(), 4, 2, 0, PileDriverBlockEntity.class);
		BlockPos pane = new BlockPos(7, 2, 6);
		h.setBlock(pane, Blocks.GLASS_PANE);
		h.succeedWhen(() -> {
			h.assertTrue(te.getTorque() == 262144 && te.getOmega() == 64, "driver at "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			h.assertTrue(te.step >= 3, "step "+te.step);
			for (BlockPos p : new BlockPos[] {new BlockPos(4, 1, 2), new BlockPos(2, 1, 1), new BlockPos(6, 1, 3), new BlockPos(4, 1, 0)})
				h.assertBlockPresent(Blocks.AIR, p);
			h.assertBlockPresent(Blocks.STONE, new BlockPos(2, 1, 0));
			h.assertBlockPresent(Blocks.STONE, new BlockPos(6, 1, 4));
			h.assertBlockNotPresent(Blocks.GLASS_PANE, pane);
		});
	}

	/** With too little torque (a gas engine's 128 Nm, far below 80000) nothing happens. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void pileDriverNeedsTorque(GameTestHelper h) {
		PileDriverBlockEntity te = place(h, RotaryBlocks.PILE_DRIVER.get(), 4, 4, 0, PileDriverBlockEntity.class);
		place(h, RotaryBlocks.GAS_ENGINE.get(), 4, 5, 2, reika.rotarycraft.content.blockentity.engine.EngineBlockEntity.class).addFuel(50000);
		h.runAtTickTime(60, () -> {
			h.assertTrue(te.getPower() == 65536 && te.step == 0, "driver "+te.getPower()+" W, step "+te.step);
			h.assertBlockPresent(Blocks.AIR, new BlockPos(4, Y-1, 4));
			h.succeed();
		});
	}
}
