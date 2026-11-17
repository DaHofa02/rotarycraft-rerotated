/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.blockentity.DistributionClutchBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: GuiDistributionClutch's CoreContainer (no slots). Instead of the original's DISTRIBCLUTCH and DISTRIBCLUTCHPOWER packets
 * the GUI sends vanilla menu button clicks: 0-3 switch a side (north, south, west, east), 4 + side + 4 * torque sets a side's request.
 */
public class DistributionClutchMenu extends AbstractContainerMenu {

	public static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
	public static final int MAX_REQUEST = (Integer.MAX_VALUE-8)/4;

	public final DistributionClutchBlockEntity tile;

	public DistributionClutchMenu(int id, Inventory inv, DistributionClutchBlockEntity te) {
		super(RotaryMenus.DISTRIBUTION_CLUTCH.get(), id);
		tile = te;
	}

	public static DistributionClutchMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new DistributionClutchMenu(id, inv, (DistributionClutchBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	public static int requestButton(int side, int torque) {
		return 4+side+4*Math.max(0, Math.min(MAX_REQUEST, torque));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (tile == null || !this.stillValid(player) || id < 0)
			return false;
		if (id < 4) {
			Direction d = SIDES[id];
			if (tile.isFixedSide(d))
				return false;
			tile.setSideEnabled(d, !tile.isSideEnabled(d));
		}
		else
			tile.setTorqueRequest(SIDES[(id-4)%4], (id-4)/4);
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
