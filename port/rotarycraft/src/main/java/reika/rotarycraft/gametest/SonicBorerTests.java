/*******************************************************************************
 * Tests for the ported RotaryCraft sonic borer (original TileEntitySonicBorer, EntitySonicShot). Private NeoForge 1.21.1 port of
 * RotaryCraft by Reika Kalseki; not for distribution.
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
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.ShaftBlockEntity;
import reika.rotarycraft.content.blockentity.SonicBorerBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SonicBorerTests {

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

	/**
	 * Gas turbine choked to 16384 rad/s @ 1024 Nm, three bedrock shafts, a bedrock 16:1 gearbox -> 1024 rad/s @ 16384 Nm into the borer
	 * (meta 2: reads south, fires north), all at height 4. Pressure builds by 16777216 / 65536 = 256 kPa a tick; at 400 it fires at the
	 * 7x7 plane in front (the floor is in it), breaking it: a stone marker and the floor blocks there drop.
	 */
	private static SonicBorerBlockEntity rig(GameTestHelper h) {
		place(h, RotaryBlocks.JET_ENGINE.get(), 4, 7, 2, JetEngineBlockEntity.class).addFuel(100000);
		h.setBlock(new BlockPos(4, Y, 8), Blocks.COBBLESTONE_WALL);
		for (int z = 6; z >= 4; z--)
			place(h, RotaryBlocks.SHAFT.get(), 4, z, 2, ShaftBlockEntity.class).applyComponentsFromItemStack(ShaftBlock.shaftItem(MaterialRegistry.BEDROCK, false));
		GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 4, 3, 14, GearboxBlockEntity.class);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16));
		gbx.setBlockMetadata(14);
		gbx.fillWithLubricant();
		return place(h, RotaryBlocks.SONIC_BORER.get(), 4, 2, 2, SonicBorerBlockEntity.class);
	}

	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "sonicborer")
	public static void sonicBorerBreaksAPlane(GameTestHelper h) {
		SonicBorerBlockEntity te = rig(h);
		BlockPos marker = new BlockPos(4, Y+1, 1);
		h.setBlock(marker, Blocks.STONE);
		h.assertTrue(te.getDistanceToSurface() == 1, "distance "+te.getDistanceToSurface());
		h.succeedWhen(() -> {
			h.assertTrue(te.getPower() == 16777216 && te.getTorque() == 16384, "borer at "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			h.assertBlockPresent(Blocks.AIR, marker);
			h.assertBlockPresent(Blocks.AIR, new BlockPos(2, 1, 1));
			h.assertBlockPresent(Blocks.AIR, new BlockPos(7, Y+3, 1));
		});
	}

	/**
	 * A shield block in the plane cannot be broken: the borer sends no pulse (it still lets off its 400 kPa each time, as in the original,
	 * so the pressure stays low) and the marker stays. Pushed past 1000 kPa it explodes: glass beside it is blown away (the borer itself
	 * withstands strength 4).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "sonicborer")
	public static void sonicBorerBlockedAndOverpressure(GameTestHelper h) {
		SonicBorerBlockEntity te = rig(h);
		BlockPos marker = new BlockPos(4, Y+1, 1);
		h.setBlock(marker, Blocks.STONE);
		h.setBlock(new BlockPos(6, Y+2, 1), RotaryBlocks.SHIELD_BLOCK.get());
		BlockPos glass = new BlockPos(3, Y, 2);
		h.setBlock(glass, Blocks.GLASS);
		h.assertTrue(te.getDistanceToSurface() == -1, "distance "+te.getDistanceToSurface());
		h.runAtTickTime(100, () -> {
			h.assertTrue(te.getPower() >= 65536 && te.getPressure() < SonicBorerBlockEntity.MAXPRESSURE, "pressure "+te.getPressure()+" at "+te.getPower()+" W");
			h.assertBlockPresent(Blocks.STONE, marker);
			h.assertBlockPresent(Blocks.GLASS, glass);
			te.addPressure(2000);
		});
		h.runAtTickTime(103, () -> {
			h.assertBlockNotPresent(Blocks.GLASS, glass);
			h.succeed();
		});
	}
}
