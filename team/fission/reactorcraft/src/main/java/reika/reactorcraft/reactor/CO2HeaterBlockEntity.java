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

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.ReactorCraft.TileEntities.HTGR.TileEntityCO2Heater - heated by pebble beds; from 800 C it turns CO2 into hot CO2
 * (heat capacity 1.168); above 3000 C it explodes (4, scrap).
 */
public class CO2HeaterBlockEntity extends IntermediateBoilerBlockEntity {

	public CO2HeaterBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.CO2_HEATER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "CO2 Heater";
	}

	@Override
	public int getMinimumTemperature() {
		return PebbleBedBlockEntity.MINTEMP;
	}

	@Override
	protected double getFluidHeatCapacity() {
		return 1.168;
	}

	@Override
	public Fluid getInputFluid() {
		return ReactorFluids.get("co2");
	}

	@Override
	protected Fluid getOutputFluid() {
		return ReactorFluids.get("hot_co2");
	}

	@Override
	public int getMaxTemperature() {
		return 3000;
	}

	@Override
	public ReactorType getReactorType() {
		return ReactorType.HTGR;
	}

	@Override
	protected void overheat(Level world, BlockPos pos) {
		world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 4, true, Level.ExplosionInteraction.BLOCK);
		for (int i = 0; i < 4; i++)
			Containers.dropItemStack(world, pos.getX()+rand.nextDouble(), pos.getY()+rand.nextDouble(), pos.getZ()+rand.nextDouble(), new ItemStack(RotaryItems.part(CraftingPart.SCRAP)));
	}
}
