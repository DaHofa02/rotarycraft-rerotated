/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The music box, written for the port from the original's behaviour: 16 channels of notes (pitch, length, instrument) entered in its GUI.
 * With 1024 W (from any side, all at the same speed) it plays them in a loop; a redstone pulse plays them once. All channels start together,
 * each note holds its channel for its length (whole 48 ticks down to sixteenth 3), rests are silent notes.
 * Changes: vanilla note block sounds (the original had its own samples), and no songs come with it - no demo, MIDI import or music discs.
 */
public class MusicBoxBlockEntity extends PowerReceiverBlockEntity {

	public static final int CHANNELS = 16;
	public static final int LOOPPOWER = 1024;
	/** Pitch 0-48: two octaves below to two above the middle (pitch 24 plays at the sound's own pitch). */
	public static final int MAX_PITCH = 48;

	public enum Instrument {
		REST(null), GUITAR(SoundEvents.NOTE_BLOCK_HARP), BASS(SoundEvents.NOTE_BLOCK_BASS), PLING(SoundEvents.NOTE_BLOCK_PLING),
		BASSDRUM(SoundEvents.NOTE_BLOCK_BASEDRUM), SNARE(SoundEvents.NOTE_BLOCK_SNARE), CLAVE(SoundEvents.NOTE_BLOCK_HAT);

		public final Holder<SoundEvent> sound;

		Instrument(Holder<SoundEvent> s) {
			sound = s;
		}
	}

	public enum NoteLength {
		WHOLE(48), HALF(24), QUARTER(12), EIGHTH(6), SIXTEENTH(3);

		public final int ticks;

		NoteLength(int t) {
			ticks = t;
		}
	}

	public record Note(NoteLength length, int pitch, Instrument voice) {

		public boolean isRest() {
			return voice == Instrument.REST;
		}

		/** Pitch factor 2^((pitch-24)/12), folded by octaves into the 0.5-2 a sound can be played at. */
		public float playbackPitch() {
			float f = (float)Math.pow(2, (pitch-24)/12D);
			while (f < 0.5F)
				f *= 2;
			while (f > 2F)
				f /= 2;
			return f;
		}

		int pack() {
			return length.ordinal() | pitch << 4 | voice.ordinal() << 12;
		}

		static Note unpack(int v) {
			return new Note(NoteLength.values()[(v & 15)%5], Math.min(MAX_PITCH, (v >> 4) & 255), Instrument.values()[((v >> 12) & 15)%7]);
		}
	}

	@SuppressWarnings("unchecked")
	private final List<Note>[] channels = new List[CHANNELS];
	private final int[] playIndex = new int[CHANNELS];
	private final int[] playDelay = new int[CHANNELS];
	private boolean oneTime;
	private boolean lastSignal;
	public int notesPlayed;

	public MusicBoxBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.MUSIC_BOX.get(), pos, state, 1, 1, LOOPPOWER);
		for (int i = 0; i < CHANNELS; i++)
			channels[i] = new ArrayList<>();
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public List<Note> getChannel(int c) {
		return channels[c];
	}

	public void addNote(int channel, Note n) {
		channels[channel].add(n);
		this.syncAllData();
	}

	public void backspace(int channel) {
		if (!channels[channel].isEmpty())
			channels[channel].remove(channels[channel].size()-1);
		this.syncAllData();
	}

	public void clearChannel(int channel) {
		channels[channel].clear();
		this.syncAllData();
	}

	public void clearMusic() {
		for (List<Note> c : channels)
			c.clear();
		this.restart();
		this.syncAllData();
	}

	private void restart() {
		for (int i = 0; i < CHANNELS; i++) {
			playIndex[i] = 0;
			playDelay[i] = 0;
		}
	}

	public boolean isPlaying() {
		return oneTime || power >= LOOPPOWER;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.readPowerSummative();
		boolean signal = world.hasNeighborSignal(pos);
		if (signal && !lastSignal) { //positive redstone edge: play once from the start
			oneTime = true;
			this.restart();
		}
		lastSignal = signal;
		if (!this.isPlaying()) {
			this.restart();
			return;
		}
		boolean any = false;
		for (int i = 0; i < CHANNELS; i++) {
			if (playDelay[i] > 0)
				playDelay[i]--;
			if (playDelay[i] == 0 && playIndex[i] < channels[i].size()) {
				this.play((ServerLevel)world, pos, i, channels[i].get(playIndex[i]));
			}
			any |= playDelay[i] > 0 || playIndex[i] < channels[i].size();
		}
		if (!any) { //every channel finished: loop, or stop after a one-time play
			this.restart();
			oneTime = false;
		}
	}

	private void play(ServerLevel world, BlockPos pos, int channel, Note n) {
		if (!n.isRest()) {
			world.playSound(null, pos, n.voice().sound.value(), SoundSource.RECORDS, 2F, n.playbackPitch());
			world.sendParticles(ParticleTypes.NOTE, pos.getX()+0.5, pos.getY()+1.2, pos.getZ()+0.5, 0, n.pitch()/24D, 0, 0, 1);
			notesPlayed++;
		}
		playDelay[channel] = n.length().ticks;
		playIndex[channel]++;
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		for (int i = 0; i < CHANNELS; i++)
			NBT.putIntArray("ch"+i, channels[i].stream().mapToInt(Note::pack).toArray());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		for (int i = 0; i < CHANNELS; i++) {
			channels[i].clear();
			for (int v : NBT.getIntArray("ch"+i))
				channels[i].add(Note.unpack(v));
		}
	}
}
