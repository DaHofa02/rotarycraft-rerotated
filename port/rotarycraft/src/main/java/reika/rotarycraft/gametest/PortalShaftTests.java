/*******************************************************************************
 * Tests for the port's portal shaft. Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.PortalShaftBlockEntity;
import reika.rotarycraft.content.blockentity.ShaftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PortalShaftTests {

	private static final String PLATFORM = "platform_9x8x9";

	/** Coordinates: overworld to nether divide x and z by 8, back multiply; the end is not scaled. */
	@GameTest(template = PLATFORM)
	public static void portalShaftScalesCoordinates(GameTestHelper h) {
		BlockPos p = new BlockPos(800, 70, -160);
		h.assertTrue(PortalShaftBlockEntity.scale(p, Level.OVERWORLD, Level.NETHER).equals(new BlockPos(100, 70, -20)), "to nether");
		h.assertTrue(PortalShaftBlockEntity.scale(new BlockPos(100, 70, -20), Level.NETHER, Level.OVERWORLD).equals(p), "from nether");
		h.assertTrue(PortalShaftBlockEntity.scale(p, Level.OVERWORLD, Level.END).equals(p), "end");
		h.assertTrue(PortalShaftBlockEntity.PortalType.END.target(Level.OVERWORLD) == Level.END
				&& PortalShaftBlockEntity.PortalType.NETHER.target(Level.NETHER) == Level.OVERWORLD
				&& PortalShaftBlockEntity.PortalType.NETHER.target(Level.END) == null, "portal targets");
		h.succeed();
	}

	private static void shaft(ServerLevel w, BlockPos p, int meta, MaterialRegistry mat) {
		w.setBlock(p, RotaryBlocks.SHAFT.get().defaultBlockState(), 3);
		ShaftBlockEntity sh = (ShaftBlockEntity)w.getBlockEntity(p);
		sh.setBlockMetadata(meta);
		sh.setShaftType(mat);
	}

	/**
	 * Above the test (y = 100), in the overworld: gas engine -> steel shaft -> end portal block. The shaft turns into a portal shaft. At the
	 * same place in the end: end portal block -> steel shaft taking its input from the portal: it becomes the partner and gives out the
	 * engine's 512 rad/s at 128 Nm. With the overworld portal gone, the partner gives nothing and both turn back into plain shafts.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "portalshaft")
	public static void portalShaftCarriesPowerToTheEnd(GameTestHelper h) {
		ServerLevel ow = h.getLevel(), end = ow.getServer().getLevel(Level.END);
		h.assertTrue(end != null, "no end dimension");
		BlockPos base = h.absolutePos(new BlockPos(2, 0, 4)).atY(100);
		BlockPos engine = base, sh = base.east(), portal = base.east(2), far = base.east(3);
		ChunkPos c = new ChunkPos(base);
		end.setChunkForced(c.x, c.z, true);
		end.setBlock(portal, Blocks.END_PORTAL.defaultBlockState(), 2);
		shaft(end, far, 1, MaterialRegistry.STEEL); //meta 1: input from the west (the portal), output east
		ow.setBlock(portal, Blocks.END_PORTAL.defaultBlockState(), 2);
		shaft(ow, sh, 1, MaterialRegistry.STEEL);
		ow.setBlock(engine, RotaryBlocks.GAS_ENGINE.get().defaultBlockState(), 3);
		EngineBlockEntity e = (EngineBlockEntity)ow.getBlockEntity(engine);
		e.setBlockMetadata(1);
		e.addFuel(50000);
		int[] phase = {0};
		h.succeedWhen(() -> {
			if (phase[0] == 0) {
				h.assertTrue(ow.getBlockEntity(sh) instanceof PortalShaftBlockEntity, "overworld shaft not converted: "+ow.getBlockState(sh));
				h.assertTrue(end.getBlockEntity(far) instanceof PortalShaftBlockEntity ps && ps.getOmega() == 512 && ps.getTorque() == 128
						&& ps.getShaftType() == MaterialRegistry.STEEL,
						"end side: "+end.getBlockEntity(far)+(end.getBlockEntity(far) instanceof PortalShaftBlockEntity ps2 ? " "+ps2.getOmega()+" rad/s" : ""));
				ow.setBlock(portal, Blocks.AIR.defaultBlockState(), 3);
				phase[0] = 1;
				h.fail("portal removed");
			}
			if (phase[0] == 1) {
				h.assertTrue(end.getBlockEntity(far) instanceof PortalShaftBlockEntity ps && ps.getOmega() == 0, "partner still turns without the portal");
				end.setBlock(far, Blocks.AIR.defaultBlockState(), 3); //the partner broken: the source turns back into a shaft
				phase[0] = 2;
				h.fail("partner removed");
			}
			h.assertTrue(ow.getBlockEntity(sh) instanceof ShaftBlockEntity s2 && s2.getShaftType() == MaterialRegistry.STEEL, "source not reverted: "+ow.getBlockEntity(sh));
			for (BlockPos p : new BlockPos[] {engine, sh})
				ow.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
			end.setBlock(portal, Blocks.AIR.defaultBlockState(), 3);
			end.setChunkForced(c.x, c.z, false);
		});
	}
}
