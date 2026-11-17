/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.blockentity.AdvancedGearBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Industrial coil settings (no slots). Button id 2n sets the output speed to n, 2n+1 the output torque to n. */
public class CoilMenu extends AbstractContainerMenu {

	public final AdvancedGearBlockEntity tile;

	public CoilMenu(int id, Inventory inv, AdvancedGearBlockEntity te) {
		super(RotaryMenus.COIL.get(), id);
		tile = te;
	}

	public static CoilMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new CoilMenu(id, inv, (AdvancedGearBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (tile == null || !this.stillValid(player) || id < 0 || !tile.kind.storesEnergy())
			return false;
		if ((id & 1) == 0)
			tile.setReleaseOmega(id >>> 1);
		else
			tile.setReleaseTorque(id >>> 1);
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
