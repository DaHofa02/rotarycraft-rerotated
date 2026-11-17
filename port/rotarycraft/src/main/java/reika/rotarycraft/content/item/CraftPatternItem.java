/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import reika.rotarycraft.content.menu.CraftPatternMenu;
import reika.rotarycraft.registry.RotaryComponents;

/**
 * A crafting pattern for the auto crafter: right-click opens a 3x3 grid of "ghost" slots (items put there are only copied); the crafting
 * table recipe they make is stored with its output. Written for the port; the original's worktable and blast furnace pattern modes are left out.
 */
public class CraftPatternItem extends Item {

	public CraftPatternItem(Properties props) {
		super(props);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player ep, InteractionHand hand) {
		if (!world.isClientSide && ep instanceof ServerPlayer sp)
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftPatternMenu(id, inv, hand), Component.translatable(this.getDescriptionId())),
					buf -> buf.writeBoolean(hand == InteractionHand.MAIN_HAND));
		return InteractionResultHolder.sidedSuccess(ep.getItemInHand(hand), world.isClientSide);
	}

	/** The 3x3 grid (9 stacks, empty where nothing is). */
	public static NonNullList<ItemStack> getGrid(ItemStack pattern) {
		NonNullList<ItemStack> li = NonNullList.withSize(10, ItemStack.EMPTY);
		ItemContainerContents c = pattern.get(RotaryComponents.CRAFT_PATTERN.get());
		if (c != null)
			c.copyInto(li);
		NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
		for (int i = 0; i < 9; i++)
			grid.set(i, li.get(i));
		return grid;
	}

	public static ItemStack getOutput(ItemStack pattern) {
		NonNullList<ItemStack> li = NonNullList.withSize(10, ItemStack.EMPTY);
		ItemContainerContents c = pattern.get(RotaryComponents.CRAFT_PATTERN.get());
		if (c != null)
			c.copyInto(li);
		return li.get(9);
	}

	/** Stores the grid and the crafting-table result it makes (empty if none). */
	public static void setGrid(ItemStack pattern, List<ItemStack> grid, Level world) {
		NonNullList<ItemStack> li = NonNullList.withSize(10, ItemStack.EMPTY);
		for (int i = 0; i < 9; i++)
			li.set(i, grid.get(i).copyWithCount(1));
		li.set(9, craft(li.subList(0, 9), world));
		pattern.set(RotaryComponents.CRAFT_PATTERN.get(), ItemContainerContents.fromItems(li));
	}

	public static ItemStack craft(List<ItemStack> grid, Level world) {
		CraftingInput in = CraftingInput.of(3, 3, grid);
		return world.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, in, world).map(r -> r.value().assemble(in, world.registryAccess())).orElse(ItemStack.EMPTY);
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext ctx, List<Component> li, TooltipFlag flag) {
		ItemStack out = getOutput(is);
		li.add(out.isEmpty() ? Component.literal("No recipe").withStyle(ChatFormatting.GRAY)
				: Component.literal("Makes "+out.getCount()+" ").append(out.getHoverName()).withStyle(ChatFormatting.GREEN));
	}
}
