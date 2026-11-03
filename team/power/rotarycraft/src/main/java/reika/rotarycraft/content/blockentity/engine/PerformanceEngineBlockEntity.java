/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotarySounds;

/** Original: Reika.RotaryCraft.TileEntities.Engine.TileEntityPerformanceEngine. Ethanol plus additives; runs as a gas engine when starved of additives. */
public class PerformanceEngineBlockEntity extends EngineBlockEntity {

	/** Used in combustion power */
	public int additives;
	private boolean starvedengine;

	public PerformanceEngineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SPORT_ENGINE.get(), pos, state, EngineType.SPORT);
	}

	@Override
	public int getMaxTemperature() {
		return 240;
	}

	@Override
	protected void consumeFuel(float scale) {
		fuel.drain((int)(scale*this.getConsumedFuel()), IFluidHandler.FluidAction.EXECUTE);
		if (rand.nextInt(30) == 0)
			if (additives > 0)
				additives--;
	}

	@Override
	protected void internalizeFuel() {
		if (!inv.getStackInSlot(0).isEmpty() && fuel.getFluidAmount()+1000 < FUELCAP) {
			if (inv.getStackInSlot(0).is(RotaryItems.ethanol())) {
				inv.extractItem(0, 1, false);
				fuel.fill(new FluidStack(RotaryFluids.ETHANOL.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
			}
		}
		if (!inv.getStackInSlot(1).isEmpty() && additives < FUELCAP/1000) { //additives
			Item id = inv.getStackInSlot(1).getItem();
			if (id == Items.BLAZE_POWDER || id == Items.REDSTONE || id == Items.GUNPOWDER) {
				inv.extractItem(1, 1, false);
				if (id == Items.REDSTONE)
					additives += 1;
				if (id == Items.GUNPOWDER)
					additives += 2;
				if (id == Items.BLAZE_POWDER)
					additives += 4;
			}
		}
	}

	@Override
	protected boolean getRequirements(Level world, BlockPos pos, int meta) {
		if (fuel.isEmpty())
			return false;
		starvedengine = additives <= 0;
		return true;
	}

	@Override
	protected void playSounds(Level world, BlockPos pos, float pitchMultiplier, float volume) {
		soundtick++;
		if (this.isMuffled(world, pos)) {
			volume *= 0.3125F;
		}

		if (soundtick < this.getSoundLength(1F/pitchMultiplier) && soundtick < 2000)
			return;
		soundtick = 0;

		RotarySounds.play(world, pos, RotarySounds.CAR.get(), 0.33F*volume, 0.9F*pitchMultiplier);
	}

	@Override
	protected void offlineCooldown(Level world, BlockPos pos, int Tamb) {
		if (temperature > Tamb+300)
			temperature -= (temperature-Tamb)/100;
		else if (temperature > Tamb+100)
			temperature -= (temperature-Tamb)/50;
		else if (temperature > Tamb+40)
			temperature -= (temperature-Tamb)/10;
		else if (temperature > Tamb+4)
			temperature -= (temperature-Tamb)/2;
		else
			temperature = Tamb;
	}

	@Override
	public void updateTemperature(Level world, BlockPos pos) {
		super.updateTemperature(world, pos);

		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (temperature < Tamb)
			temperature += Math.max((Tamb-temperature)/40, 1);
		if (omega > 0 && torque > 0) { //If engine is on
			temperature += 1;
			if (water.getFluidAmount() > 0 && temperature > Tamb) {
				water.drain(20, IFluidHandler.FluidAction.EXECUTE);
				temperature--;
			}
			ServerLevel sl = world instanceof ServerLevel s ? s : null;
			double x = pos.getX(), y = pos.getY(), z = pos.getZ();
			if (temperature > this.getMaxTemperature()/2) {
				if (rand.nextInt(10) == 0) {
					if (sl != null)
						sl.sendParticles(ParticleTypes.SMOKE, x+0.5, y+0.5, z+0.5, 1, 0, 0, 0, 0);
					world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1F, 1F);
				}
			}
			if (temperature > this.getMaxTemperature()/1.25) {
				if (rand.nextInt(3) == 0) {
					world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1F, 1F);
				}
				if (sl != null) {
					double[][] spots = {{0, 0.5}, {0.5, 0.5}, {1, 0.5}, {0, 0}, {0, 1}, {1, 0}, {1, 1}};
					for (double[] s : spots)
						sl.sendParticles(ParticleTypes.SMOKE, x+s[0], y+1.0625, z+s[1], 1, 0, 0, 0, 0);
				}
			}
		}
		if (temperature > this.getMaxTemperature()) {
			this.overheat(world, pos);
		}
	}

	@Override
	public void overheat(Level world, BlockPos pos) {
		temperature = this.getMaxTemperature();
		//Original: ReikaWorldHelper.overheat(world, x, y, z, scrap, 0, 27, true, 1.5F, true, BLOCKDAMAGE, 6F) - flaming explosion with lava sparks
		world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		world.explode(null, null, null, pos.getX(), pos.getY(), pos.getZ(), 6F, true, RotaryConfig.BLOCKDAMAGE.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
		if (world instanceof ServerLevel sl)
			sl.sendParticles(ParticleTypes.LAVA, pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5, (int)((rand.nextInt(20)+20)*1.5F), 0.5, 0, 0.5, 0);
		int numdrops = rand.nextInt(28);
		for (int i = 0; i < numdrops; i++)
			Containers.dropItemStack(world, pos.getX()+rand.nextFloat(), pos.getY()+0.5, pos.getZ()+rand.nextFloat(), new ItemStack(RotaryItems.part(CraftingPart.SCRAP)));
	}

	@Override
	public int getFuelLevel() {
		return fuel.getFluidAmount();
	}

	public int getAdditivesScaled(int par1) {
		return (additives * par1*1000) / FUELCAP;
	}

	@Override
	protected int getMaxSpeed(Level world, BlockPos pos, int meta) {
		return starvedengine ? EngineType.GAS.getSpeed() : EngineType.SPORT.getSpeed();
	}

	@Override
	protected int getGenTorque(Level world, BlockPos pos, int meta) {
		return starvedengine ? EngineType.GAS.getTorque() : EngineType.SPORT.getTorque();
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("additive", additives);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		additives = NBT.getInt("additive");
	}

	@Override
	protected void affectSurroundings(Level world, BlockPos pos, int meta) {

	}
}
