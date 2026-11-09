/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import net.minecraft.world.level.block.entity.BlockEntity;

import reika.rotarycraft.content.blockentity.HeatConduction;

/**
 * Original: Reika.ReactorCraft.Auxiliary.Temperatured - reactor parts that exchange heat. Like the original (Temperatured extends
 * HeatConduction) they can be heated or drained by a heat pipe, with the defaults of TileEntityReactorBase.
 */
public interface Temperatured extends HeatConduction {

	@Override
	int getTemperature();

	@Override
	void setTemperature(int T);

	int getMaxTemperature();

	boolean canDumpHeatInto(LiquidStates liq);

	/** Original TileEntityReactorBase.allowExternalHeating: not for HTGR and fusion reactor parts. */
	@Override
	default boolean allowExternalHeating() {
		return reactorBaseAllowsExternalHeating(this);
	}

	/** Original TileEntityReactorBase.allowHeatExtraction. */
	@Override
	default boolean allowHeatExtraction() {
		return true;
	}

	/** Original TileEntityReactorBase.heatEnergyPerDegree: the default, 50 times for reactor cores (ReactorCoreTE) and the heat exchanger. */
	@Override
	default double heatEnergyPerDegree() {
		return reactorBaseHeatEnergyPerDegree((BlockEntity)this);
	}

	static boolean reactorBaseAllowsExternalHeating(Object te) {
		if (!(te instanceof ReactorTyped t))
			return true;
		ReactorType r = t.getReactorType();
		return r != ReactorType.HTGR && r != ReactorType.FUSION;
	}

	static double reactorBaseHeatEnergyPerDegree(BlockEntity te) {
		double base = HeatConduction.baseHeatEnergyPerDegree(te);
		return isReactorCore(te) ? base*50 : base;
	}

	/**
	 * Original ReactorTiles.isReactorCore (the tile implements ReactorCoreTE): water cell, neutron absorber, the boilers (TileEntityNuclearBoiler
	 * and its intermediate boilers), the linkable cores (fuel, breeder and thorium cores, control rods) - not the CPU or the pebble bed.
	 */
	static boolean isReactorCore(BlockEntity te) {
		return te instanceof WaterCellBlockEntity || te instanceof reika.reactorcraft.fusion.NeutronAbsorberBlockEntity || te instanceof ReactorBoilerBlockEntity
				|| te instanceof IntermediateBoilerBlockEntity || te instanceof LinkableReactorCore;
	}
}
