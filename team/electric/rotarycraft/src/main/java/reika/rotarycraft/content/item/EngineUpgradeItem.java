/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.UpgradeableMachine;
import reika.rotarycraft.registry.RotaryBlocks;

/**
 * Original: Reika.RotaryCraft.Items.Tools.ItemEngineUpgrade (one item with 11 metadata variants, stack size 16) and the right-click in
 * ItemBasic.onItemUse. Each variant is its own item here. Ported: the variants whose machines exist (PERFORMANCE, AFTERBURNER, REDSTONE);
 * the magnetostatic tiers, efficiency, flux and lodestone upgrades come with the converter engines and the Magnetizer.
 */
public class EngineUpgradeItem extends Item {

	/** Original: ItemEngineUpgrade.Upgrades (only the ported ones). */
	public enum Upgrades {
		PERFORMANCE,
		AFTERBURNER,
		REDSTONE;
	}

	private final Upgrades type;

	public EngineUpgradeItem(Upgrades type, Properties props) {
		super(props.stacksTo(16));
		this.type = type;
	}

	public Upgrades getUpgrade() {
		return type;
	}

	public static boolean is(ItemStack is, Upgrades u) {
		return is.getItem() instanceof EngineUpgradeItem e && e.type == u;
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level world = ctx.getLevel();
		BlockEntity te = world.getBlockEntity(ctx.getClickedPos());
		ItemStack is = ctx.getItemInHand();
		if (te instanceof UpgradeableMachine u && u.canUpgradeWith(is)) {
			if (!world.isClientSide) {
				u.upgrade(is);
				if (te instanceof RotaryCraftBlockEntity r && !r.isRemoved())
					r.syncAllData();
				if (ctx.getPlayer() == null || !ctx.getPlayer().isCreative())
					is.shrink(1);
			}
			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		return InteractionResult.PASS;
	}

	/** Original: Upgrades.getDescription(). */
	@Override
	public void appendHoverText(ItemStack is, TooltipContext ctx, List<Component> li, TooltipFlag flag) {
		switch(type) {
			case PERFORMANCE -> li.add(Component.literal("Upgrades ").append(Component.translatable(RotaryBlocks.GAS_ENGINE.get().getDescriptionId()))
					.append("s to ").append(Component.translatable(RotaryBlocks.SPORT_ENGINE.get().getDescriptionId())).append("s"));
			case AFTERBURNER -> li.add(Component.literal("Adds afterburner capability to the ").append(Component.translatable(RotaryBlocks.JET_ENGINE.get().getDescriptionId())));
			case REDSTONE -> li.add(Component.literal("Adds integrated redstone clock"));
		}
	}
}
