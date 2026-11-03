/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import reika.rotarycraft.api.interfaces.Fillable;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.Items.Tools.ItemIntegratedGearbox (ItemRegistry.GEARUPGRADE, metadata 0-4) - an engine upgrade with a
 * built-in gear. The empty frame (level 0) takes bedrock gears for 2x, 4x, 8x or 16x (levels 1-4); filled in the filling station with
 * 500 mB of lubricant it gears for torque (torque times the ratio, speed divided), with liquid nitrogen for speed (the other way round).
 * Used on a stopped engine without one, it is built in (the engine drops an unfilled one when broken). The fluid sits in a data
 * component (original NBT "lvl" and fluid name); the sprite shows empty, lubricant or nitrogen.
 */
public class IntegratedGearboxItem extends Item implements Fillable {

	public static final int CAPACITY = 500;

	/** 0 = empty frame, 1-4 = 2x, 4x, 8x, 16x. */
	public final int level;

	public IntegratedGearboxItem(Properties props, int level) {
		super(props.stacksTo(64));
		this.level = level;
	}

	/** Original getIntegratedGearItem: the item for a gear ratio (positive torque, negative speed), filled with the fluid if given. */
	@Nullable
	public static ItemStack getIntegratedGearItem(int ratio, @Nullable Fluid f) {
		if (ratio == 0)
			return null;
		int lvl = Integer.numberOfTrailingZeros(Math.abs(ratio));
		ItemStack is = new ItemStack(RotaryItems.GEAR_UPGRADES.get(lvl).get());
		if (f != null)
			((IntegratedGearboxItem)is.getItem()).addFluid(is, f, CAPACITY);
		return is;
	}

	/** Original getRatioFromIntegratedGearItem: 0 for the frame (or, when it must be filled, while not full). */
	public static int getRatioFromIntegratedGearItem(ItemStack is, boolean requireFill) {
		if (!(is.getItem() instanceof IntegratedGearboxItem i) || i.level == 0)
			return 0;
		if (requireFill && !i.isFull(is))
			return 0;
		Fluid f = i.getCurrentFluid(is);
		boolean pos = !requireFill || (f != null && f.isSame(RotaryFluids.LUBRICANT.get()));
		int ratio = 1 << i.level;
		return pos ? ratio : -ratio;
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level world = ctx.getLevel();
		ItemStack is = ctx.getItemInHand();
		if (world.getBlockEntity(ctx.getClickedPos()) instanceof EngineBlockEntity te) {
			if (world.isClientSide)
				return InteractionResult.SUCCESS;
			if (te.applyIntegratedGear(is)) {
				if (ctx.getPlayer() == null || !ctx.getPlayer().isCreative())
					is.shrink(1);
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.PASS;
	}

	/** For the item model: 0 empty or partly filled, 0.5 full of lubricant, 1 full of liquid nitrogen (original getItemSpriteIndex). */
	public static float fillState(ItemStack is) {
		if (!(is.getItem() instanceof IntegratedGearboxItem i) || !i.isFull(is))
			return 0;
		Fluid f = i.getCurrentFluid(is);
		if (f != null && f.isSame(RotaryFluids.LUBRICANT.get()))
			return 0.5F;
		if (f != null && f.isSame(RotaryFluids.LIQUID_NITROGEN.get()))
			return 1;
		return 0;
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext ctx, List<Component> li, TooltipFlag flag) {
		if (level > 0) {
			int ratio = getRatioFromIntegratedGearItem(is, false);
			li.add(Component.literal("Ratio: "+Math.abs(ratio)+"x"));
			ratio = getRatioFromIntegratedGearItem(is, true);
			if (ratio > 0)
				li.add(Component.literal("Torque Mode"));
			else if (ratio < 0)
				li.add(Component.literal("Speed Mode"));
			else {
				li.add(Component.literal("Requires Fluid"));
				Fluid f = this.getCurrentFluid(is);
				if (f != null) {
					int amt = this.getCurrentFillLevel(is);
					li.add(Component.literal("Is "+(amt*100F/CAPACITY)+"% full of ").append(new FluidStack(f, amt).getHoverName()));
				}
			}
		}
		else {
			li.add(Component.literal("Requires Gears and Fluid"));
		}
	}

	private static FluidStack content(ItemStack is) {
		SimpleFluidContent c = is.get(RotaryComponents.GEAR_FLUID.get());
		return c == null ? FluidStack.EMPTY : c.copy();
	}

	@Override
	public boolean isValidFluid(Fluid f, ItemStack is) {
		Fluid has = this.getCurrentFluid(is);
		return has != null ? f.isSame(has) : isValidFluid(f);
	}

	private static boolean isValidFluid(Fluid f) {
		return f.isSame(RotaryFluids.LIQUID_NITROGEN.get()) || f.isSame(RotaryFluids.LUBRICANT.get());
	}

	@Override
	public int getCapacity(ItemStack is) {
		return CAPACITY;
	}

	@Override
	public int getCurrentFillLevel(ItemStack is) {
		return content(is).getAmount();
	}

	@Override
	public int addFluid(ItemStack is, Fluid f, int amt) {
		if (!isValidFluid(f))
			return 0;
		FluidStack has = content(is);
		if (!has.isEmpty() && !has.getFluid().isSame(f))
			return 0;
		int liq = has.getAmount();
		int toadd = Math.min(amt, CAPACITY-liq);
		if (toadd <= 0 && !has.isEmpty())
			return 0;
		is.set(RotaryComponents.GEAR_FLUID.get(), SimpleFluidContent.copyOf(new FluidStack(f, liq+toadd)));
		return toadd;
	}

	@Override
	public boolean isFull(ItemStack is) {
		return this.getCurrentFillLevel(is) >= CAPACITY;
	}

	@Override
	@Nullable
	public Fluid getCurrentFluid(ItemStack is) {
		FluidStack fs = content(is);
		return fs.isEmpty() ? null : fs.getFluid();
	}
}
