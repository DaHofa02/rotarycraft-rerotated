/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.blockentity.MobRadarBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: GuiMobRadar's CoreContainer (no slots). The range typed into the GUI reaches the server as a vanilla menu button
 * click whose button id is the range (0-999), instead of the original PacketRegistry.DETECTOR data packet.
 */
public class MobRadarMenu extends AbstractContainerMenu {

	public final MobRadarBlockEntity tile;

	public MobRadarMenu(int id, Inventory inv, MobRadarBlockEntity te) {
		super(RotaryMenus.MOB_RADAR.get(), id);
		tile = te;
	}

	public static MobRadarMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new MobRadarMenu(id, inv, (MobRadarBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
