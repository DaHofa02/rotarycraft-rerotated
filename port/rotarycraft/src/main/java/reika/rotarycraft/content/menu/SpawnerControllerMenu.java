/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.blockentity.SpawnerControllerBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: GuiSpawnerController's container (no slots). Button 0 disables; button 1+n enables with a delay of n ticks (the SPAWNERTIMER packet). */
public class SpawnerControllerMenu extends AbstractContainerMenu {

	public static final int MAX_DELAY = 99999;

	public final SpawnerControllerBlockEntity tile;

	public SpawnerControllerMenu(int id, Inventory inv, SpawnerControllerBlockEntity te) {
		super(RotaryMenus.SPAWNER_CONTROLLER.get(), id);
		tile = te;
	}

	public static SpawnerControllerMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new SpawnerControllerMenu(id, inv, (SpawnerControllerBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (tile == null || !this.stillValid(player) || id < 0 || id > MAX_DELAY+1)
			return false;
		tile.handleGui(id == 0 ? -1 : id-1);
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
