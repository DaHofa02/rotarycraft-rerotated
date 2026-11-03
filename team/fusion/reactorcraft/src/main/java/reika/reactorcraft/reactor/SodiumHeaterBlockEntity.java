/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.lib.math.RandomHelper;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fission.Breeder.TileEntitySodiumHeater - heated by breeder cores; from 301 C it turns liquid
 * sodium into hot sodium (heat capacity 1.23); above 2000 C it explodes (4, scrap and iron scrap). With sodium in it, it absorbs
 * fission, fusion, waste and thorium neutrons with 90 %.
 */
public class SodiumHeaterBlockEntity extends IntermediateBoilerBlockEntity {

	public static final int MIN_TEMPERATURE = 301;
	public static final double SODIUM_HEAT = 1.23;

	public SodiumHeaterBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.SODIUM_HEATER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Sodium Heater";
	}

	@Override
	public int getMinimumTemperature() {
		return MIN_TEMPERATURE;
	}

	@Override
	protected double getFluidHeatCapacity() {
		return SODIUM_HEAT;
	}

	@Override
	public Fluid getInputFluid() {
		return ReactorFluids.get("sodium");
	}

	@Override
	protected Fluid getOutputFluid() {
		return ReactorFluids.get("hot_sodium");
	}

	@Override
	public int getMaxTemperature() {
		return 2000;
	}

	@Override
	public ReactorType getReactorType() {
		return ReactorType.BREEDER;
	}

	@Override
	protected void overheat(Level world, BlockPos pos) {
		world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 4, true, Level.ExplosionInteraction.BLOCK);
		for (int i = 0; i < 4; i++) {
			Containers.dropItemStack(world, pos.getX()+rand.nextDouble(), pos.getY()+rand.nextDouble(), pos.getZ()+rand.nextDouble(), new ItemStack(RotaryItems.part(CraftingPart.SCRAP)));
			Containers.dropItemStack(world, pos.getX()+rand.nextDouble(), pos.getY()+rand.nextDouble(), pos.getZ()+rand.nextDouble(), new ItemStack(RotaryItems.part(CraftingPart.IRONSCRAP)));
		}
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		return !tank.isEmpty() && RandomHelper.doWithChance(e.getNeutronType().getSodiumBoilerAbsorptionChance(), rand);
	}

	/** Bucket and canister by hand (original BlockReactorTile): a sodium canister adds 1000 mB. */
	public boolean addSodium(int amt) {
		if (tank.getFluidAmount()+amt > CAPACITY)
			return false;
		tank.fill(new FluidStack(ReactorFluids.get("sodium"), amt), IFluidHandler.FluidAction.EXECUTE);
		this.setChanged();
		return true;
	}
}
