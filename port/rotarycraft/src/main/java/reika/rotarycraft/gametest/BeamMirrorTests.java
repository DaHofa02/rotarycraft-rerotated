/*******************************************************************************
 * Tests for the ported RotaryCraft beam mirror (original TileEntityBeamMirror). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BeamMirrorBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BeamMirrorTests {

	private static final String PLATFORM = "platform_9x8x9";
	private static final BlockPos POS = new BlockPos(1, 2, 4);

	/** A mirror at (1, 2, 4) facing east (meta 0) with a stone block 5 ahead. */
	private static BeamMirrorBlockEntity mirror(GameTestHelper h) {
		h.setBlock(POS, RotaryBlocks.BEAM_MIRROR.get());
		BeamMirrorBlockEntity te = (BeamMirrorBlockEntity)h.getBlockEntity(POS);
		te.setBlockMetadata(0);
		h.setBlock(new BlockPos(6, 2, 4), Blocks.STONE);
		return te;
	}

	private static boolean lit(GameTestHelper h, int x) {
		return h.getBlockState(new BlockPos(x, 2, 4)).is(Blocks.LIGHT);
	}

	/**
	 * By day under open sky the beam reaches the stone (range 5); the clear spots from the second block on get light blocks; a husk
	 * (undead that does not burn in daylight itself) in the beam catches fire, one beside it does not.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void mirrorLightsTheLineAndBurnsUndead(GameTestHelper h) {
		BeamMirrorBlockEntity te = mirror(h);
		Husk in = h.spawn(EntityType.HUSK, new BlockPos(4, 2, 4));
		Husk out = h.spawn(EntityType.HUSK, new BlockPos(4, 2, 7));
		in.setNoAi(true);
		out.setNoAi(true);
		h.succeedWhen(() -> {
			h.assertTrue(te.getRange() == 5, "range "+te.getRange()+" at day time "+h.getLevel().getDayTime());
			h.assertTrue(!lit(h, 2) && lit(h, 3) && lit(h, 4) && lit(h, 5), "lights "+lit(h, 2)+lit(h, 3)+lit(h, 4)+lit(h, 5));
			h.assertBlockPresent(Blocks.STONE, new BlockPos(6, 2, 4));
			h.assertTrue(in.isOnFire() && !out.isOnFire(), "husk in the beam burning "+in.isOnFire()+", beside it "+out.isOnFire());
		});
	}

	/** Covered from the sky the beam goes out; broken, the mirror takes its lights along. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void mirrorNeedsTheSky(GameTestHelper h) {
		BeamMirrorBlockEntity te = mirror(h);
		int[] stage = {0};
		h.succeedWhen(() -> {
			if (stage[0] == 0) {
				h.assertTrue(lit(h, 4), "not lit yet");
				h.setBlock(POS.above(), Blocks.STONE);
				stage[0] = 1;
				h.fail("covered");
			}
			if (stage[0] == 1) {
				h.assertTrue(te.getRange() == 0 && !lit(h, 3) && !lit(h, 4), "still lit under cover");
				h.setBlock(POS.above(), Blocks.AIR);
				stage[0] = 2;
				h.fail("uncovered");
			}
			if (stage[0] == 2) {
				h.assertTrue(lit(h, 4), "not lit again");
				h.setBlock(POS, Blocks.AIR);
				stage[0] = 3;
			}
			h.assertTrue(!lit(h, 3) && !lit(h, 4) && !lit(h, 5), "lights left after breaking");
		});
	}
}
