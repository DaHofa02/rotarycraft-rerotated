/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.blockentity.SpyCamBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: GuiSpyCam's CoreContainer (no slots), opened through a screen. Valid while the camera stands and runs, however far away
 * (the screen may be up to 64 blocks from it).
 */
public class SpyCamViewMenu extends AbstractContainerMenu {

	public final SpyCamBlockEntity tile;

	public SpyCamViewMenu(int id, Inventory inv, SpyCamBlockEntity te) {
		super(RotaryMenus.SPY_CAM_VIEW.get(), id);
		tile = te;
	}

	public static SpyCamViewMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new SpyCamViewMenu(id, inv, (SpyCamBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean stillValid(Player player) {
		return tile != null && !tile.isRemoved() && tile.isOn();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}
}
