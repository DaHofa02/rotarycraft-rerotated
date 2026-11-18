/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.blockentity.MagneticEngineBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** No slots; buttons: 0 slower, 1 faster, 2 next redstone mode. The values shown come from the synced block entity. */
public class MagneticEngineMenu extends AbstractContainerMenu {

	public final MagneticEngineBlockEntity tile;

	public MagneticEngineMenu(int id, Inventory inv, MagneticEngineBlockEntity te) {
		super(RotaryMenus.MAGNETIC_ENGINE.get(), id);
		tile = te;
	}

	public static MagneticEngineMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new MagneticEngineMenu(id, inv, (MagneticEngineBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		switch (id) {
			case 0 -> tile.setSpeedStep(tile.getSpeedStep()-1);
			case 1 -> tile.setSpeedStep(tile.getSpeedStep()+1);
			case 2 -> tile.stepRedstoneMode();
			default -> {
				return false;
			}
		}
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
