/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.rotarycraft.content.blockentity.ParticleEmitterBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: OneSlotContainer(player, te, 28) for GuiParticle - the coil slot (80,35) and the player inventory 28 pixels lower than
 * usual. Button i of the GUI picks the i-th valid particle kind (original PacketRegistry.PARTICLES).
 */
public class ParticleEmitterMenu extends AbstractContainerMenu {

	public static final List<ParticleEmitterBlockEntity.Kind> KINDS = new ArrayList<>();
	static {
		for (ParticleEmitterBlockEntity.Kind k : ParticleEmitterBlockEntity.Kind.values())
			if (k.isValid())
				KINDS.add(k);
	}

	public final ParticleEmitterBlockEntity tile;

	public ParticleEmitterMenu(int id, Inventory inv, ParticleEmitterBlockEntity te) {
		super(RotaryMenus.PARTICLE_EMITTER.get(), id);
		tile = te;
		this.addSlot(new SlotItemHandler(te.getInventory(), 0, 80, 35));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 112 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 170));
	}

	public static ParticleEmitterMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new ParticleEmitterMenu(id, inv, (ParticleEmitterBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id < 0 || id >= KINDS.size())
			return false;
		tile.setParticleType(KINDS.get(id));
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return tile != null && !tile.isRemoved() && tile.isPlayerAccessible(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index == 0) {
			if (!this.moveItemStackTo(stack, 1, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 0, 1, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
