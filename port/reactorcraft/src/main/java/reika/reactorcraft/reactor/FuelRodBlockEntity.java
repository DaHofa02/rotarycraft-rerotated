/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.content.item.FuelItem;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorComponents;
import reika.reactorcraft.registry.ReactorItems;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fission.TileEntityFuelRod with ReactorFuel - a neutron that can cause fission (with the
 * neutron speed's interaction chance) is lost to waste poisoning 1/(9-waste) of the time, else causes fission with the fuel's chance
 * (uranium 25 %, plutonium 30 % + 0.025 % per degree above 100 C): then with the consume chance (3 % / 4 %) the pellet depletes one
 * step (after 100 steps uranium becomes depleted uranium and moves to the waste slots, plutonium is used up) and with the waste chance
 * (5 % / 10 %) a waste isotope is made; every fission sends 3 neutrons and heats the core (20 C / 30 C).
 */
public class FuelRodBlockEntity extends NuclearCoreBlockEntity {

	/** Original: Reika.ReactorCraft.Registry.ReactorFuel. */
	public enum ReactorFuel {
		URANIUM(25, 3, 5, 20, 0),
		PLUTONIUM(30, 4, 10, 30, 0.025F);

		public final int fissionChance;
		public final int consumeChance;
		public final int wasteChance;
		public final int temperatureStep;
		public final float voidCoefficient;

		ReactorFuel(int fiss, int con, int waste, int temp, float v) {
			fissionChance = fiss;
			consumeChance = con;
			wasteChance = waste;
			temperatureStep = temp;
			voidCoefficient = v;
		}

		@javax.annotation.Nullable
		public static ReactorFuel getFrom(ItemStack is) {
			if (is.is(ReactorItems.FUEL.get()))
				return URANIUM;
			if (is.is(ReactorItems.PLUTONIUM.get()))
				return PLUTONIUM;
			return null;
		}

		/** Original getFissionProduct: the next depletion step; at the end uranium becomes depleted uranium and plutonium is gone. */
		public ItemStack getFissionProduct(ItemStack is) {
			int d = FuelItem.getDepletion(is);
			if (d >= ((FuelItem)is.getItem()).steps-1)
				return this == URANIUM ? new ItemStack(ReactorItems.DEPLETED.get()) : ItemStack.EMPTY;
			ItemStack out = is.copy();
			out.set(ReactorComponents.DEPLETION.get(), d+1);
			return out;
		}
	}

	public FuelRodBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.FUEL_ROD.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Fuel Core";
	}

	@Override
	protected boolean isItemValidForSlot(int i, ItemStack is) {
		if (!inv.getStackInSlot(i).isEmpty())
			return false;
		if (ReactorFuel.getFrom(is) != null || is.is(ReactorItems.DEPLETED.get()))
			return i < 4;
		return false;
	}

	@Override
	protected boolean canRemoveItem(int i, ItemStack is) {
		return is.is(ReactorItems.WASTE.get()) || is.is(ReactorItems.DEPLETED.get());
	}

	@Override
	public boolean isFissile() {
		return ReactorFuel.getFrom(inv.getStackInSlot(3)) != null;
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		super.onNeutron(e, world, pos);
		if (world.isClientSide)
			return false;
		if (e.getNeutronType().canTriggerFission(rand) && chance(e.getNeutronSpeed().getInteractionMultiplier(), rand)) {
			if (this.checkPoisonedChance())
				return true;
			if (this.isFissile()) {
				ReactorFuel f = ReactorFuel.getFrom(inv.getStackInSlot(3));
				if (chance(f.fissionChance+f.voidCoefficient*(temperature-100), rand)) {
					if (chance(f.consumeChance, rand)) {
						ItemStack is = inv.getStackInSlot(3);
						ItemStack next = f.getFissionProduct(is);
						inv.setStackInSlot(3, next);
						if (!next.isEmpty() && next.getItem() != is.getItem())
							this.tryPushSpentFuel(3);
						if (chance(f.wasteChance, rand))
							this.addWaste();
					}
					this.spawnNeutronBurst(world, pos);
					temperature += f.temperatureStep;
					this.setChanged();
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean canDumpHeatInto(LiquidStates liq) {
		return liq.isWater();
	}

	@Override
	public ReactorType getReactorType() {
		return ReactorType.FISSION;
	}
}
