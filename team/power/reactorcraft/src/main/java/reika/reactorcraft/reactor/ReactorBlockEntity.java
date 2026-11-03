/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.lib.world.TemperatureEnvironment;

/**
 * Original: Reika.ReactorCraft.Base.TileEntityReactorBase (its temperature part). Once a second: towards the ambient temperature
 * (sampled with more variation the hotter it is, capped at 95 C outside the Nether; cooling only 1 C per second, heating 1+dT/32 in the
 * open or 1+dT/64 enclosed), heats the surroundings, then takes heat from every hotter neighbouring reactor part: the neighbour loses
 * dT/fraction, this part gains dT/fraction times the efficiency (0 unless this part is a typed reactor part, 1 for the same reactor type,
 * else the type mismatch efficiency; control rods and CPUs use their own value).
 */
public abstract class ReactorBlockEntity extends RotaryCraftBlockEntity {

	protected int temperature;
	protected int thermalTicker;

	protected ReactorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static boolean isExposedToAir(Level world, BlockPos pos) {
		for (Direction d : Direction.values()) {
			BlockPos p = pos.relative(d);
			if (!world.isLoaded(p))
				continue;
			BlockState s = world.getBlockState(p);
			if (s.isAir() || s.getCollisionShape(world, p).isEmpty() || !s.isSolid())
				return true;
		}
		return false;
	}

	/** Thermal ticker of the original (StepTimer 20). */
	protected final boolean thermalTick() {
		thermalTicker++;
		if (thermalTicker >= 20) {
			thermalTicker = 0;
			return true;
		}
		return false;
	}

	protected void updateTemperature(Level world, BlockPos pos) {
		float af = 1+1.5F*Mth.clamp((temperature-100)/500F, 0, 1);
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos, af);
		if (world.dimension() != Level.NETHER)
			Tamb = Math.min(Tamb, 95);
		int dT = Tamb-temperature;
		if (dT != 0) {
			int d = isExposedToAir(world, pos) ? 32 : 64;
			int diff = 1+dT/d;
			if (diff <= 1)
				diff = dT/Math.abs(dT);
			temperature += diff;
		}
		TemperatureEnvironment.temperatureEnvironment(world, pos, Math.min(temperature, 1000));
		if (this.afterAmbientStep(world, pos, Tamb))
			return;
		for (Direction dir : Direction.values()) {
			BlockPos p = pos.relative(dir);
			BlockEntity be = world.getBlockEntity(p);
			if (be instanceof ReactorBlockEntity te && te instanceof Temperatured tr) {
				int Tamb_loc = AmbientTemperature.getAmbientTemperatureAt(world, p);
				int T = tr.getTemperature();
				dT = (T-temperature)-Math.max(0, Tamb-Tamb_loc);
				float f = this instanceof ReactorTyped && this instanceof Temperatured ? te.getHeatConductionThroughput(this) : 1;
				dT *= f;
				if (dT > 0) {
					int dd = te instanceof ReactorTyped ? this.getHeatConductionFraction(te) : 4;
					int newT = T-dT/dd;
					float e = this instanceof ReactorTyped && this instanceof Temperatured ? te.getHeatConductionEfficiency(this) : 0;
					double add = dT/dd*e;
					temperature += add;
					tr.setTemperature(newT);
					this.onHeatReceived(te, add);
				}
			}
		}
	}

	/** Original: the reactor boiler check inside updateTemperature (it explodes in hot surroundings); true = this part is gone. */
	protected boolean afterAmbientStep(Level world, BlockPos pos, int Tamb) {
		return false;
	}

	/** Boilers record which reactor type their heat came from. */
	protected void onHeatReceived(ReactorBlockEntity from, double amount) {}

	/** For transferring heat FROM that reactor block. */
	protected int getHeatConductionFraction(ReactorBlockEntity other) {
		return 4;
	}

	/** For transferring heat TO that reactor block. */
	protected float getHeatConductionThroughput(ReactorBlockEntity other) {
		return 1;
	}

	/** Original getHeatConductionEfficiency (heat going to the other, typed part). */
	protected float getHeatConductionEfficiency(ReactorBlockEntity other) {
		if (other instanceof ControlOrCPU)
			return this.getControlCPUHeatEfficiency();
		ReactorType r1 = this instanceof ReactorTyped t ? t.getReactorType() : null;
		ReactorType r2 = other instanceof ReactorTyped t ? t.getReactorType() : null;
		if (r1 == r2)
			return 1;
		if (r1 == null || r2 == null)
			return 0;
		return r1.getTypeMismatchHeatEfficiency();
	}

	protected float getControlCPUHeatEfficiency() {
		return 1;
	}

	public int getTemperature() {
		return temperature;
	}

	public void setTemperature(int T) {
		temperature = T;
	}

	public Direction getRandomDirection(boolean allowVertical) {
		return allowVertical ? Direction.values()[rand.nextInt(6)] : Direction.values()[2+rand.nextInt(4)];
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("temp", temperature);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		temperature = NBT.getInt("temp");
	}

	/** Marker for control rods and CPUs (original getHeatConductionEfficiency special case). */
	public interface ControlOrCPU {}
}
