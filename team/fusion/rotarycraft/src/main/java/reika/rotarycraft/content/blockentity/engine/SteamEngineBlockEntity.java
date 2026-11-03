/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotarySounds;

/** Original: Reika.RotaryCraft.TileEntities.Engine.TileEntitySteamEngine. Boils water from fire or lava below; explodes when boiled dry and refilled. */
public class SteamEngineBlockEntity extends EngineBlockEntity {

	private int dryTicks = 0;

	public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.STEAM_ENGINE.get(), pos, state, EngineType.STEAM);
	}

	@Override
	public boolean canConsumeFuel() {
		return water.getFluidAmount() > 0 && temperature >= 100;
	}


	/** Original canBeCooledWithFins. */
	@Override
	public boolean canBeCooledWithFins() {
		return true;
	}

	@Override
	public int getMaxTemperature() {
		return 150;
	}

	@Override
	protected void consumeFuel(float scale) {
		water.drain((int)(scale*this.getConsumedFuel()), IFluidHandler.FluidAction.EXECUTE);
	}

	@Override
	protected void offlineCooldown(Level world, BlockPos pos, int Tamb) {

	}

	@Override
	protected int getConsumedFuel() {
		int amt = 10;
		if (temperature >= 130) {
			amt = 75;
		}
		else if (temperature >= 125) {
			amt = 60;
		}
		else if (temperature >= 120) {
			amt = 50;
		}
		else if (temperature >= 110) {
			amt = 25;
		}
		if (level.getBlockState(worldPosition.below()).is(Blocks.LAVA))
			amt *= 4;
		return amt;
	}

	@Override
	protected void internalizeFuel() {
		if (water.isEmpty() && temperature >= 100) {
			dryTicks++;
		}
		else {
			if (dryTicks > 900 && !water.isEmpty()) {
				level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				level.explode(null, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, 6, Level.ExplosionInteraction.BLOCK);
			}
			dryTicks = 0;
		}
	}

	@Override
	protected boolean getRequirements(Level world, BlockPos pos, int meta) {
		if (temperature < 100) //water boiling point
			return false;
		return !water.isEmpty();
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

		RotarySounds.play(world, pos, RotarySounds.STEAM.get(), 0.7F*volume, 1F*pitchMultiplier);
	}

	/** Original: RotaryAux.isAboveFire / isAboveLava (the EnvironmentalHeatSource API hook arrives with the API events). */
	private static boolean isAboveFire(Level world, BlockPos pos) {
		return world.getBlockState(pos.below()).is(Blocks.FIRE);
	}

	private static boolean isAboveLava(Level world, BlockPos pos) {
		return world.getFluidState(pos.below()).is(FluidTags.LAVA);
	}

	@Override
	public void updateTemperature(Level world, BlockPos pos) {
		super.updateTemperature(world, pos);

		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		boolean nether = world.dimension() == Level.NETHER;

		boolean fire = isAboveFire(world, pos);
		boolean lava = isAboveLava(world, pos);
		if (nether)
			Tamb = 101;	//boils water, so 101C
		if (fire)
			temperature++;
		if (fire && nether)
			temperature++; //Nether has 50% hotter fire
		if (lava) {
			temperature += 2;
		}
		if (Tamb < 0 && fire)
			Tamb += 30;
		if (temperature < Tamb)
			temperature += Math.max((Tamb-temperature)/40, 1);
		if (!fire && !lava && temperature > Tamb)
			temperature--;
		if (temperature > Tamb) {
			temperature -= (temperature-Tamb)/96;
		}
		if (temperature > this.getMaxTemperature())
			this.overheat(world, pos);
	}

	@Override
	public void overheat(Level world, BlockPos pos) {
		if (water.isEmpty()) {
			world.setBlock(pos, Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
			world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 2, 1);
			return;
		}
		temperature = this.getMaxTemperature();
		//Original: ReikaWorldHelper.overheat(world, x, y, z, scrap, 0, 17, false, 1F, false, true, 2F)
		world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		world.explode(null, pos.getX(), pos.getY(), pos.getZ(), 2F, Level.ExplosionInteraction.BLOCK);
		int numdrops = rand.nextInt(18);
		for (int i = 0; i < numdrops; i++)
			Containers.dropItemStack(world, pos.getX()+rand.nextFloat(), pos.getY()+0.5, pos.getZ()+rand.nextFloat(), new ItemStack(RotaryItems.part(CraftingPart.SCRAP)));
	}

	@Override
	public int getFuelLevel() {
		return water.getFluidAmount();
	}

	@Override
	protected void affectSurroundings(Level world, BlockPos pos, int meta) {

	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("dry", dryTicks);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		dryTicks = NBT.getInt("dry");
	}

	@Override
	public boolean allowExternalHeating() {
		return true;
	}
}
