/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.piping;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.reactor.ReactorBoilerBlockEntity;
import reika.reactorcraft.reactor.ReactorType;
import reika.reactorcraft.reactor.ReactorTyped;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.rotarycraft.content.blockentity.HeatConduction;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.ReactorCraft.TileEntities.TileEntityHeatPipe (ReactorTiles.HEATPIPE, a TileEntityLine on BlockSteamLine like the steam
 * line) - a conductor that moves heat, not fluid. It holds heat energy (3010.8 J per degree; on its first tick the ambient temperature).
 * Every tick, on the server, against each of its six neighbours:
 * <ul>
 * <li>another heat pipe that has existed for 2 ticks: if that one holds more heat, half the difference comes over (and this pipe takes
 * over that pipe's record of reactor types);</li>
 * <li>a machine that conducts heat ({@link HeatConduction}: RotaryCraft TemperatureTE machines and ReactorCraft reactor parts) and allows
 * being heated or drained: with heat above ambient (net heat) H = joules per degree x (T - ambient) on both sides, if the machine holds
 * more heat and is hotter and allows extraction, or holds less, is cooler and allows external heating, the quarter d = (H_pipe - H_machine)/4
 * flows: the pipe loses d and the machine's temperature is <b>set</b> to ambient + max(1, d / its joules per degree). Original quirk kept:
 * when heat is drawn out (d &lt; 0) the machine thus drops straight to ambient + 1, while the pipe gains only a quarter of the
 * difference. Heat drawn from a typed reactor part is noted per reactor type; heating a reactor boiler hands it these types.</li>
 * </ul>
 * Original quirk kept: its venting method (ventHeat, a 1/96 of the excess per call) is never called and its temperature field stays 0,
 * so a heat pipe never loses heat to the air, never glows (the brightness starts at 250 C) and never burns what touches it. As in the
 * original the first tick after loading sets the heat back to ambient.
 */
public class HeatPipeBlockEntity extends RotaryCraftBlockEntity {

	/** Original HEAT_CAPACITY, joules per degree. */
	public static final double HEAT_CAPACITY = 3010.8;

	private double heatEnergy;
	/** Original Proportionality&lt;ReactorType&gt; reactorTypes; shared with the pipe it was taken from, like the original reference. */
	private Map<ReactorType, Double> reactorTypes = new EnumMap<>(ReactorType.class);

	public HeatPipeBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.HEAT_PIPE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Heat Pipe";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (this.getTicksExisted() == 0)
			this.onFirstTick(world, pos);
		if (!world.isClientSide)
			this.balanceHeat(world, pos);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	private void onFirstTick(Level world, BlockPos pos) {
		heatEnergy = AmbientTemperature.getAmbientTemperatureAt(world, pos)*HEAT_CAPACITY;
	}

	/** Original canConnectToMachine: a heat conducting machine that allows being heated or drained. */
	public static boolean canConnectToMachine(@Nullable BlockEntity te) {
		return te instanceof HeatConduction h && (h.allowExternalHeating() || h.allowHeatExtraction());
	}

	/** Original isConnectedOnSideAt (what the renderer draws an arm to): another heat pipe or a machine it exchanges heat with. */
	public static boolean connectsTo(Level world, BlockPos pos, Direction dir) {
		BlockEntity te = world.getBlockEntity(pos.relative(dir));
		return te instanceof HeatPipeBlockEntity || canConnectToMachine(te);
	}

	private void balanceHeat(Level world, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			BlockEntity te = world.getBlockEntity(pos.relative(dir));
			if (te instanceof HeatPipeBlockEntity tile) {
				this.balanceWith(tile);
			}
			else if (te != null && canConnectToMachine(te)) {
				HeatConduction hc = (HeatConduction)te;
				double theirheat = getNetHeat(hc);
				double ourheat = this.getNetHeatEnergy();
				double theirtemp = getNetTemperature(hc);
				double ourtemp = this.getNetHeatEnergy()/HEAT_CAPACITY;
				boolean intake = theirheat > ourheat;
				boolean valid = intake ? hc.allowHeatExtraction() && ourtemp < theirtemp : hc.allowExternalHeating() && ourtemp > theirtemp;
				if (valid) {
					double diff = ourheat-theirheat;
					diff /= 4;
					int put = getTemperatureForHeat(diff, hc);
					hc.setTemperature(put+hc.getAmbientTemperature());
					te.setChanged();
					heatEnergy -= diff;
					this.setChanged();
					if (diff < 0) {
						if (te instanceof ReactorTyped tb && tb.getReactorType() != null)
							reactorTypes.merge(tb.getReactorType(), diff, Double::sum);
					}
					else if (diff > 0 && te instanceof ReactorBoilerBlockEntity tb) {
						for (Map.Entry<ReactorType, Double> e : reactorTypes.entrySet())
							tb.setReactorType(e.getKey(), e.getValue());
					}
				}
			}
		}
	}

	private void balanceWith(HeatPipeBlockEntity ts) {
		if (ts.getTicksExisted() >= 2) {
			double diff = ts.heatEnergy-heatEnergy;
			if (diff > 0) {
				diff /= 2;
				ts.heatEnergy -= diff;
				heatEnergy += diff;
				reactorTypes = ts.reactorTypes;
				ts.setChanged();
				this.setChanged();
			}
		}
	}

	/** Original getNetTemperature: degrees above the machine's ambient temperature. */
	public static double getNetTemperature(HeatConduction hc) {
		return hc.getTemperature()-hc.getAmbientTemperature();
	}

	/** Original getNetHeat: the machine's heat above ambient in joules. */
	public static double getNetHeat(HeatConduction hc) {
		return hc.heatEnergyPerDegree()*getNetTemperature(hc);
	}

	/** Original getTemperatureForHeat: at least 1. */
	public static int getTemperatureForHeat(double heat, HeatConduction hc) {
		return (int)Math.max(1, heat/hc.heatEnergyPerDegree());
	}

	/** Original getNetHeatEnergy: the heat above that of the ambient temperature. */
	public double getNetHeatEnergy() {
		return heatEnergy-(level == null ? 0 : AmbientTemperature.getAmbientTemperatureAt(level, worldPosition)*HEAT_CAPACITY);
	}

	public double getHeatEnergy() {
		return heatEnergy;
	}

	public void setHeatEnergy(double e) {
		heatEnergy = e;
	}

	/** Original getTemperatureForPipe(this, net). */
	public double getPipeTemperature(boolean net) {
		return (net ? this.getNetHeatEnergy() : heatEnergy)/HEAT_CAPACITY;
	}

	public Map<ReactorType, Double> getReactorTypes() {
		return reactorTypes;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putDouble("heat", heatEnergy);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		heatEnergy = NBT.getDouble("heat");
	}
}
