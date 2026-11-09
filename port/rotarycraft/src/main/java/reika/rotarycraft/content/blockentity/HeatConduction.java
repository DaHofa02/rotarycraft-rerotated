/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.RotaryCraft.Auxiliary.Interfaces.HeatConduction (with DragonAPI's ThermalTile) - a machine whose heat can be moved by
 * a conductor (the ReactorCraft heat pipe). It may allow being heated from outside and/or having heat taken out; its heat capacity is
 * {@link #heatEnergyPerDegree()} joules per degree above ambient. Only block entities implement it.
 */
public interface HeatConduction {

	int getTemperature();

	void setTemperature(int temp);

	/** Whether a conductor may heat this machine. */
	boolean allowExternalHeating();

	/** Whether a conductor may take heat out of this machine. */
	boolean allowHeatExtraction();

	/** Original RotaryCraftTileEntity / TileEntityReactorBase.heatEnergyPerDegree: 0.481 J/(g K) times the block volume times 7800 g. */
	default double heatEnergyPerDegree() {
		return baseHeatEnergyPerDegree((BlockEntity)this);
	}

	/** Original TileEntityBase.getAmbientTemperature. */
	default int getAmbientTemperature() {
		BlockEntity te = (BlockEntity)this;
		return te.getLevel() == null ? 25 : AmbientTemperature.getAmbientTemperatureAt(te.getLevel(), te.getBlockPos());
	}

	/** 0.481 * volume * 7800, the volume being that of the block's bounds (original ReikaBlockHelper.getBlockVolume). */
	static double baseHeatEnergyPerDegree(BlockEntity te) {
		return 0.481*blockVolume(te)*7800;
	}

	static double blockVolume(BlockEntity te) {
		if (te.getLevel() == null)
			return 1;
		VoxelShape s = te.getBlockState().getShape(te.getLevel(), te.getBlockPos());
		if (s.isEmpty())
			return 1;
		AABB box = s.bounds();
		return box.getXsize()*box.getYsize()*box.getZsize();
	}
}
