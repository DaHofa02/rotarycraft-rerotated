/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.rotarycraft.api.interfaces.SodiumSolarUpgrades;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.SolarTowerBlockEntity;
import reika.rotarycraft.content.blockentity.TemperatureMachine;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.ReactorCraft.TileEntities.TileEntitySolarTop - the sodium receiver on top of a RotaryCraft solar tower, two blocks
 * high (the lower one is the active part, the upper one the cap). The tower's mirrors heat it by 0.125 C per mirror and brightness every
 * tick, up to 1800 C; every 8 ticks it moves back towards the ambient temperature by 1+dT/16. While it is active the tower takes
 * sodium, which then needs 800 C to give power. The cap shows the temperature of the part below it. (The original's overheat into lava
 * is never called - the temperature is capped - and is not ported.)
 */
public class SolarTopBlockEntity extends RotaryCraftBlockEntity implements TemperatureMachine, SodiumSolarUpgrades.SodiumSolarReceiver {

	public static final int MAXTEMP = 1800;

	private int temperature;

	public SolarTopBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.SOLAR_TOP.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	@Override
	public boolean isActive() {
		return level != null && level.getBlockEntity(worldPosition.above()) instanceof SolarTopBlockEntity
				&& level.getBlockEntity(worldPosition.below()) instanceof SolarTowerBlockEntity;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (this.isActive() && !world.isClientSide && this.getTicksExisted()%8 == 0) {
			int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
			int dT = Tamb-temperature;
			if (dT != 0) {
				int d = 16;
				int diff = 1+dT/d;
				if (Math.abs(diff) <= 1)
					diff = dT/Math.abs(dT);
				temperature += diff;
				this.syncAllData();
			}
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getTemperature() {
		if (this.isActive())
			return temperature;
		if (level != null && level.getBlockEntity(worldPosition.below()) instanceof SolarTopBlockEntity te)
			return te.getTemperature();
		return level != null ? AmbientTemperature.getAmbientTemperatureAt(level, worldPosition) : temperature;
	}

	@Override
	public void setTemperature(int temp) {
		temperature = temp;
	}

	@Override
	public void addTemperature(int temp) {
		temperature += temp;
	}

	@Override
	public int getMaxTemperature() {
		return MAXTEMP;
	}

	@Override
	public void tick(int mirrorCount, float totalBrightness) {
		if (level != null && !level.isClientSide) {
			temperature = (int)(temperature+0.0625*2*mirrorCount*totalBrightness); //original int += double
			temperature = Math.min(temperature, MAXTEMP);
		}
	}

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
}
