/*******************************************************************************
 * Tests for the port's music box. Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
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
import reika.rotarycraft.content.blockentity.MusicBoxBlockEntity;
import reika.rotarycraft.content.blockentity.MusicBoxBlockEntity.Instrument;
import reika.rotarycraft.content.blockentity.MusicBoxBlockEntity.Note;
import reika.rotarycraft.content.blockentity.MusicBoxBlockEntity.NoteLength;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class MusicBoxTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** A plain scale step: C, D, E as quarter notes (12 ticks each). */
	private static void scale(MusicBoxBlockEntity te) {
		for (int p : new int[] {24, 26, 28})
			te.addNote(0, new Note(NoteLength.QUARTER, p, Instrument.GUITAR));
	}

	/** Pitch 24 is the sound's own pitch; octaves beyond what a sound can do are folded back; note lengths. */
	@GameTest(template = PLATFORM)
	public static void musicBoxFollowsTheOriginalNumbers(GameTestHelper h) {
		float[][] cases = {{24, 1}, {36, 2}, {12, 0.5F}, {48, 2}, {0, 0.5F}};
		for (float[] c : cases) {
			float f = new Note(NoteLength.WHOLE, (int)c[0], Instrument.GUITAR).playbackPitch();
			h.assertTrue(Math.abs(f-c[1]) < 1e-4, "pitch "+c[0]+": "+f);
		}
		h.assertTrue(NoteLength.WHOLE.ticks == 48 && NoteLength.SIXTEENTH.ticks == 3, "lengths");
		h.succeed();
	}

	/** With a gas engine beside it (65536 W) it loops the three notes; idle without power. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void musicBoxLoopsWithPower(GameTestHelper h) {
		MusicBoxBlockEntity te = place(h, RotaryBlocks.MUSIC_BOX.get(), 4, 2, 4, 0, MusicBoxBlockEntity.class);
		scale(te);
		h.runAtTickTime(20, () -> {
			h.assertTrue(te.notesPlayed == 0, "played without power");
			place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		});
		h.succeedWhen(() -> h.assertTrue(te.notesPlayed >= 6, "only "+te.notesPlayed+" notes at "+te.getPower()+" W"));
	}

	/** Without power, a redstone pulse plays the three notes once and stops. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void musicBoxPlaysOnceOnAPulse(GameTestHelper h) {
		MusicBoxBlockEntity te = place(h, RotaryBlocks.MUSIC_BOX.get(), 4, 2, 4, 0, MusicBoxBlockEntity.class);
		scale(te);
		h.runAtTickTime(5, () -> h.setBlock(new BlockPos(4, 3, 4), Blocks.REDSTONE_BLOCK));
		h.runAtTickTime(8, () -> h.setBlock(new BlockPos(4, 3, 4), Blocks.AIR));
		h.runAtTickTime(120, () -> {
			h.assertTrue(te.notesPlayed == 3 && !te.isPlaying(), te.notesPlayed+" notes, playing "+te.isPlaying());
			h.succeed();
		});
	}
}
