/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.content.item.FuelItem;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorComponents;
import reika.reactorcraft.registry.ReactorItems;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fission.Breeder.TileEntityBreederCore - burns breeder fuel: a neutron is lost to waste
 * poisoning first, then with (25 + T/100) % and the neutron speed's chance it reacts: breeder neutrons convert the fuel one step
 * with 5 % (fast 11 %; +50 C; after 20 steps it becomes plutonium and moves to the waste slots), otherwise +20 C (+30 C from 700 C);
 * each reaction sends 3 breeder neutrons and makes waste with 10 %. Every 10 ticks it passes 1/16 of the temperature difference to
 * sodium heaters beside it. Its rated maximum is 900 C. Plutonium and waste can be taken out.
 */
public class BreederCoreBlockEntity extends NuclearCoreBlockEntity {

	private int timer2;

	public BreederCoreBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.BREEDER_CORE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Breeder Core";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		super.updateEntity(world, pos, state);
		if (this.isRemoved())
			return;
		timer2++;
		if (timer2 >= 10) {
			timer2 = 0;
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				if (world.getBlockEntity(pos.relative(dir)) instanceof SodiumHeaterBlockEntity te) {
					int dTemp = temperature-te.getTemperature();
					if (dTemp > 0) {
						temperature -= dTemp/16;
						te.setTemperature(te.getTemperature()+dTemp/16);
					}
				}
			}
		}
	}

	private int locateBreederFuel() {
		for (int i = 0; i < inv.getSlots(); i++)
			if (inv.getStackInSlot(i).is(ReactorItems.BREEDER_FUEL.get()))
				return i;
		return -1;
	}

	@Override
	public boolean isFissile() {
		return this.locateBreederFuel() != -1;
	}

	@Override
	protected boolean isItemValidForSlot(int i, ItemStack is) {
		if (!inv.getStackInSlot(i).isEmpty())
			return false;
		if (is.is(ReactorItems.BREEDER_FUEL.get()) || is.is(ReactorItems.PLUTONIUM.get()))
			return i < 4;
		return false;
	}

	@Override
	protected boolean canRemoveItem(int slot, ItemStack is) {
		return is.is(ReactorItems.PLUTONIUM.get()) || is.is(ReactorItems.WASTE.get());
	}

	/** Original getMaxTemperature 900 (the limit a CPU watches; the meltdown point stays 1800 C as for all cores). */
	@Override
	public int getMaxTemperature() {
		return 900;
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		super.onNeutron(e, world, pos);
		if (world.isClientSide)
			return false;
		if (this.checkPoisonedChance())
			return true;
		if (chance(25+temperature/100, rand) && this.isFissile() && chance(e.getNeutronSpeed().getInteractionMultiplier(), rand)) {
			int slot = this.locateBreederFuel();
			if (slot != -1) {
				if (e.getNeutronType().canTriggerFuelConversion() && chance(5*e.getNeutronSpeed().getWasteConversionMultiplier(), rand)) {
					ItemStack is = inv.getStackInSlot(slot);
					int dmg = FuelItem.getDepletion(is);
					if (dmg == ReactorItems.BREEDER_FUEL.get().steps-1) {
						inv.setStackInSlot(slot, new ItemStack(ReactorItems.PLUTONIUM.get()));
						this.tryPushSpentFuel(slot);
					}
					else {
						ItemStack out = is.copy();
						out.set(ReactorComponents.DEPLETION.get(), dmg+1);
						inv.setStackInSlot(slot, out);
					}
					temperature += 50;
				}
				else {
					temperature += temperature >= 700 ? 30 : 20;
				}
				this.spawnNeutronBurst(world, pos);
				if (chance(10, rand))
					this.addWaste();
				this.setChanged();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canDumpHeatInto(LiquidStates liq) {
		return liq == LiquidStates.SODIUM;
	}

	@Override
	public ReactorType getReactorType() {
		return ReactorType.BREEDER;
	}
}
