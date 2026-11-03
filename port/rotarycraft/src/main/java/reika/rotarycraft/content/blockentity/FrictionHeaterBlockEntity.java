/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.content.recipe.FrictionHeaterRecipe;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.DifficultyEffects;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryRecipeTypes;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Auxiliary.TileEntityFurnaceHeater (Friction Heater). Shaft power (min. 32 Nm, 8192 W) heats it
 * up to 2000 C while a heatable machine sits on its front. It heats RotaryCraft machines such as the Blast Furnace, or drives a vanilla
 * furnace: burn time and much faster cooking from the temperature, plus its own recipes ({@link FrictionHeaterRecipe}). At 2000 C a
 * furnace can melt (with block damage enabled). Not ported: other mods' machines (Tinkers' smeltery, Thaumcraft alchemical furnace,
 * Thaumic Tinkerer transvectors) and the heat-ripple shader.
 */
public class FrictionHeaterBlockEntity extends PowerReceiverBlockEntity implements TemperatureMachine {

	public static final int MAXTEMP = 2000;

	@Nullable
	private FrictionHeaterRecipe activeRecipe;
	@Nullable
	private BlockPos furnaceLocation;

	private int temperature;
	private int smeltTime = 0;
	private int soundtick = 0;

	public FrictionHeaterBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FRICTION_HEATER.get(), pos, state, 32, 1, 8192); //PowerReceivers.FRICTION
	}

	@Override
	public String getName() {
		return "Friction Heater";
	}

	public void updateTemperature(Level world, BlockPos pos) {
		if (torque >= MINTORQUE && power >= MINPOWER && omega > 0 && this.hasHeatableMachine(world)) {
			temperature += 3*ReikaMathLibrary.logbase(omega, 2)*ReikaMathLibrary.logbase(torque, 2);
		}
		int Tamb = power > MINPOWER && torque > MINTORQUE ? 30 : AmbientTemperature.getAmbientTemperatureAt(world, pos); //prevent nether exploit
		if (temperature > Tamb) {
			temperature -= (temperature-Tamb)/5;
		}
		else {
			temperature += (temperature-Tamb)/5;
		}
		if (temperature - Tamb <= 4 && temperature > Tamb)
			temperature--;
		if (temperature > MAXTEMP)
			temperature = MAXTEMP;
		if (temperature >= MAXTEMP)
			if (!world.isClientSide && RotaryConfig.BLOCKDAMAGE.get() && rand.nextInt(DifficultyEffects.FURNACEMELT.getInt()) == 0)
				this.meltFurnace(world);
		if (temperature < Tamb)
			temperature = Tamb;
	}

	/** Original hasHeatableMachine: a vanilla furnace or a friction-heatable machine on the front. */
	private boolean hasHeatableMachine(Level world) {
		if (furnaceLocation == null)
			return false;
		BlockState id = world.getBlockState(furnaceLocation);
		if (id.isAir())
			return false;
		if (id.is(Blocks.FURNACE))
			return true;
		BlockEntity te = world.getBlockEntity(furnaceLocation);
		return te instanceof FrictionHeatable fh && fh.canBeFrictionHeated();
	}

	/** Original getThermalDamage (contact damage while running). */
	public int getThermalDamage() {
		return power > 0 ? temperature*5/1200 : 0;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (power < MINPOWER)
			return;
		if (torque < MINTORQUE)
			return;
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		activeRecipe = null;
		tickcount++;
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		furnaceLocation = pos.relative(read.getOpposite());
		if (tickcount >= 20) {
			tickcount = 0;
			this.updateTemperature(world, pos);
		}
		if (!this.isActive())
			return;

		if (this.hasFurnace()) {
			this.hijackFurnace(world, pos, this.getBlockMetadata());
		}
		else if (world.getBlockEntity(furnaceLocation) instanceof FrictionHeatable te) {
			this.heatMachine(world, pos, te);
		}
	}

	public boolean isActive() {
		return power >= MINPOWER && torque >= MINTORQUE;
	}

	private void heatMachine(Level world, BlockPos pos, FrictionHeatable te) {
		if (te.canBeFrictionHeated()) {
			int tdiff = Math.min(te.getMaxTemperature(), temperature)-te.getTemperature();
			if (tdiff > 0 || (tdiff == 0 && temperature == te.getMaxTemperature())) {
				te.addTemperature(Math.max(1, (int)(tdiff*te.getMultiplier())));
				te.resetAmbientTemperatureTimer();
			}

			if (te.getTemperature() > te.getMaxTemperature()) {
				te.onOverheat(world, furnaceLocation);
			}

			soundtick++;
			if (soundtick > 49) {
				RotarySounds.play(world, pos, RotarySounds.FRICTION.get(), MachineSounds.isMuffled(world, pos) ? 0.1F : 0.5F, 1);
				soundtick = 0;
			}
		}
	}

	private static boolean canTileMake(FurnaceBlockEntity tile, ItemStack is) {
		ItemStack out = tile.items.get(2);
		if (out.isEmpty())
			return true;
		return ItemStack.isSameItemSameComponents(is, out) && is.getCount()+out.getCount() <= is.getMaxStackSize();
	}

	@Nullable
	private FrictionHeaterRecipe getSmelting(Level world, ItemStack in) {
		Optional<RecipeHolder<FrictionHeaterRecipe>> o = world.getRecipeManager().getRecipeFor(RotaryRecipeTypes.FRICTION.get(), new SingleRecipeInput(in), world);
		return o.isPresent() && temperature >= o.get().value().temperature() ? o.get().value() : null;
	}

	/** Original hijackFurnace: keeps the furnace lit from the heat and replaces its cook timer with the heater's own, faster one. */
	private void hijackFurnace(Level world, BlockPos pos, int meta) {
		FurnaceBlockEntity tile = (FurnaceBlockEntity)world.getBlockEntity(furnaceLocation);
		boolean flag = tile.litTime > 0;
		int burn = Math.max(this.getBurnTimeFromTemperature(), tile.litTime);
		tile.litDuration = burn;
		tile.litTime = burn;
		if (burn > 0 && !flag) {
			BlockState fs = world.getBlockState(furnaceLocation);
			if (fs.hasProperty(AbstractFurnaceBlock.LIT))
				world.setBlock(furnaceLocation, fs.setValue(AbstractFurnaceBlock.LIT, true), 3);
		}
		ItemStack in = tile.items.get(0);
		BlockPos f = furnaceLocation;
		if (!in.isEmpty()) {
			ItemStack out = tile.items.get(2);
			@SuppressWarnings("unchecked")
			RecipeHolder<? extends AbstractCookingRecipe> smelt = tile.quickCheck.getRecipeFor(new SingleRecipeInput(in), world).orElse(null);
			FrictionHeaterRecipe special = this.getSmelting(world, in);
			if (special != null && !canTileMake(tile, special.result()))
				special = null;
			if (smelt != null || special != null) {
				activeRecipe = special;
				int factor = this.getSpeedFactorFromTemperature();
				if (special != null)
					factor *= this.getAccelerationFactor(special);
				smeltTime += 1+factor;
				int max = special != null ? special.duration() : 200;
				tile.cookingProgress = Math.min(smeltTime, max-5)*200/max;
				if (smeltTime >= max) {
					int xp = 0;
					if (smelt != null && FurnaceBlockEntity.canBurn(world.registryAccess(), smelt, tile.items, tile.getMaxStackSize(), tile)) {
						FurnaceBlockEntity.burn(world.registryAccess(), smelt, tile.items, tile.getMaxStackSize(), tile);
						xp = Mth.ceil(smelt.value().getExperience());
					}
					else if (special != null) {
						ItemStack out2 = special.result().copy();
						in.shrink(1);
						int amt = !out.isEmpty() ? out.getCount()+out2.getCount() : out2.getCount();
						tile.items.set(2, out2.copyWithCount(amt));
						xp = 1;
					}
					if (xp > 0 && RotaryConfig.FRICTIONXP.get() && world instanceof ServerLevel sl) {
						ExperienceOrb.award(sl, net.minecraft.world.phys.Vec3.atLowerCornerWithOffset(f, 0.5, 0.6, 0.5), xp);
					}
					tile.setChanged();
					smeltTime = 0;
				}
			}
			else {
				tile.cookingProgress = 0;
			}
		}
		else {
			tile.cookingProgress = 0;
		}
		soundtick++;
		if (soundtick > 49) {
			RotarySounds.play(world, pos, RotarySounds.FRICTION.get(), MachineSounds.isMuffled(world, pos) ? 0.1F : 0.5F, 1);
			soundtick = 0;
		}
		if (world instanceof ServerLevel sl) {
			double px, pz;
			switch(meta) {
				case 0 -> { px = pos.getX(); pz = f.getZ()+rand.nextDouble(); }
				case 1 -> { px = pos.getX()+1; pz = f.getZ()+rand.nextDouble(); }
				case 2 -> { px = f.getX()+rand.nextDouble(); pz = pos.getZ(); }
				default -> { px = f.getX()+rand.nextDouble(); pz = pos.getZ()+1; }
			}
			sl.sendParticles(ParticleTypes.CRIT, px, f.getY()+rand.nextDouble(), pz, 1, 0, 0, 0, 0.2);
		}
	}

	private float getAccelerationFactor(FrictionHeaterRecipe rec) {
		float fac = temperature/(float)rec.temperature();
		return Math.min(1, (fac*fac)-1);
	}

	private void meltFurnace(Level world) {
		if (furnaceLocation == null || !world.getBlockState(furnaceLocation).is(Blocks.FURNACE))
			return;
		world.explode(null, furnaceLocation.getX()+0.5, furnaceLocation.getY()+0.5, furnaceLocation.getZ()+0.5, 1F, false, Level.ExplosionInteraction.NONE);
		world.setBlockAndUpdate(furnaceLocation, Blocks.AIR.defaultBlockState());
	}

	public boolean hasFurnace() {
		return furnaceLocation != null && level != null && level.getBlockEntity(furnaceLocation) instanceof FurnaceBlockEntity;
	}

	private int getBurnTimeFromTemperature() {
		if (temperature < 300)
			return 0;
		return (temperature-300)*2;
	}

	private int getSpeedFactorFromTemperature() {
		if (temperature < 500)
			return 1;
		if (temperature == 2000)
			return 2000;
		return 1+(int)Math.sqrt((Math.pow(2, ((temperature-500)/100F))));
	}

	@Nullable
	public FrictionHeaterRecipe getActiveRecipe() {
		return activeRecipe;
	}

	@Override
	public int getRedstoneOverride() {
		return temperature/100;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("temp", temperature);
		if (furnaceLocation != null)
			NBT.putLong("furnLoc", furnaceLocation.asLong());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		temperature = NBT.getInt("temp");
		furnaceLocation = NBT.contains("furnLoc") ? BlockPos.of(NBT.getLong("furnLoc")) : null;
	}

	@Override
	public void addTemperature(int temp) {
		temperature += temp;
	}

	@Override
	public int getTemperature() {
		return temperature;
	}

	@Override
	public void setTemperature(int temp) {
		temperature = temp;
	}

	@Override
	public int getMaxTemperature() {
		return MAXTEMP;
	}

	@Override
	public boolean allowExternalHeating() {
		return false;
	}

	@Override
	public void onEMP() {}
}
