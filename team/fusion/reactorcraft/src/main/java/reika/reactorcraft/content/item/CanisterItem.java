/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.item;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import reika.reactorcraft.registry.ReactorItems;

/**
 * Original: ItemCanister registered as a 1000 mB FluidContainerRegistry container for each fluid (stack size 1; in crafting a filled
 * canister leaves an empty one). Tanks and pipes fill or empty it through the item fluid handler below.
 */
public class CanisterItem extends Item {

	@Nullable
	private final Supplier<? extends Fluid> fluid;

	public CanisterItem(Properties props, @Nullable Supplier<? extends Fluid> fluid) {
		super(props);
		this.fluid = fluid;
	}

	public Fluid getFluid() {
		return fluid != null ? fluid.get() : Fluids.EMPTY;
	}

	/** Fixed 1000 mB contents; filling an empty canister turns it into the canister of that fluid, draining it back into the empty one. */
	public static class Handler implements IFluidHandlerItem {

		private ItemStack container;

		public Handler(ItemStack stack) {
			container = stack;
		}

		private Fluid held() {
			return container.getItem() instanceof CanisterItem c ? c.getFluid() : Fluids.EMPTY;
		}

		@Override
		public ItemStack getContainer() {
			return container;
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			Fluid f = this.held();
			return f == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(f, 1000);
		}

		@Override
		public int getTankCapacity(int tank) {
			return 1000;
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return ReactorItems.canisterFor(stack.getFluid()) != null;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (container.getCount() != 1 || resource.getAmount() < 1000 || this.held() != Fluids.EMPTY)
				return 0;
			Item filled = ReactorItems.canisterFor(resource.getFluid());
			if (filled == null)
				return 0;
			if (action.execute())
				container = new ItemStack(filled);
			return 1000;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			if (resource.isEmpty() || !resource.getFluid().isSame(this.held()))
				return FluidStack.EMPTY;
			return this.drain(resource.getAmount(), action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			Fluid f = this.held();
			if (container.getCount() != 1 || maxDrain < 1000 || f == Fluids.EMPTY)
				return FluidStack.EMPTY;
			if (action.execute())
				container = new ItemStack(ReactorItems.EMPTY_CANISTER.get());
			return new FluidStack(f, 1000);
		}
	}
}
