/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.blockentity.MusicBoxBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * The note editor (no slots). Button id = channel << 16 | command: command with bit 15 set adds the packed note in bits 0-14 (length,
 * pitch << 4, instrument << 12); 1 deletes the channel's last note, 2 clears the channel, 3 clears all channels.
 */
public class MusicBoxMenu extends AbstractContainerMenu {

	public final MusicBoxBlockEntity tile;

	public MusicBoxMenu(int id, Inventory inv, MusicBoxBlockEntity te) {
		super(RotaryMenus.MUSIC_BOX.get(), id);
		tile = te;
	}

	public static MusicBoxMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new MusicBoxMenu(id, inv, (MusicBoxBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public static int noteButton(int channel, MusicBoxBlockEntity.NoteLength len, int pitch, MusicBoxBlockEntity.Instrument voice) {
		return channel << 16 | 1 << 15 | len.ordinal() | pitch << 4 | voice.ordinal() << 12;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		int channel = id >>> 16, cmd = id & 0xffff;
		if (tile == null || !this.stillValid(player) || channel >= MusicBoxBlockEntity.CHANNELS)
			return false;
		if ((cmd & 1 << 15) != 0) {
			int len = cmd & 15, pitch = (cmd >> 4) & 255, voice = (cmd >> 12) & 7;
			if (len >= 5 || pitch > MusicBoxBlockEntity.MAX_PITCH || voice >= 7)
				return false;
			tile.addNote(channel, new MusicBoxBlockEntity.Note(MusicBoxBlockEntity.NoteLength.values()[len], pitch, MusicBoxBlockEntity.Instrument.values()[voice]));
		}
		else if (cmd == 1)
			tile.backspace(channel);
		else if (cmd == 2)
			tile.clearChannel(channel);
		else if (cmd == 3)
			tile.clearMusic();
		else
			return false;
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return tile != null && !tile.isRemoved() && tile.isPlayerAccessible(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}
}
