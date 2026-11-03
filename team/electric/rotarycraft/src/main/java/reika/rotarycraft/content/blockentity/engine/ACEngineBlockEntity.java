/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.content.blockentity.MachineSounds;
import reika.rotarycraft.content.blockentity.UpgradeableMachine;
import reika.rotarycraft.content.item.EngineUpgradeItem;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.GearboxTypes.GearPart;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Engine.TileEntityACEngine. Needs alternating redstone (or the integrated redstone upgrade) and a magnetized shaft core
 * (magnetization from the Magnetizer, step 5); the core loses magnetization while running.
 */
public class ACEngineBlockEntity extends EngineBlockEntity implements UpgradeableMachine {

	/** Original: RotaryCraft.Auxiliary.RedstoneCycleTracker(3) */
	private final boolean[] lastPower = new boolean[3];
	private boolean alternating;
	private boolean integratedRedstone;

	public ACEngineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.AC_ENGINE.get(), pos, state, EngineType.AC);
	}

	@Override
	protected void consumeFuel(float scale) {

	}

	@Override
	protected void internalizeFuel() {

	}

	private static boolean isCore(ItemStack is) {
		return is.is(RotaryItems.gearPart(GearboxTypes.STEEL, GearPart.SHAFTCORE)) || is.is(RotaryItems.gearPart(GearboxTypes.TUNGSTEN, GearPart.SHAFTCORE));
	}

	@Override
	protected boolean getRequirements(Level world, BlockPos pos, int meta) {
		ItemStack is = inv.getStackInSlot(0);
		if (is.isEmpty() || !isCore(is))
			return false;
		int magnet = is.getOrDefault(RotaryComponents.MAGNETIZATION.get(), 0);
		if (magnet <= 0)
			return false;

		this.updateRedstone(world, pos);
		boolean ac = alternating;

		if (!world.isClientSide && ac && timer.checkCap("fuel")) {
			if (is.is(RotaryItems.gearPart(GearboxTypes.STEEL, GearPart.SHAFTCORE)) || rand.nextBoolean()) {
				this.magnetize(is, magnet-1);
			}
		}

		return ac;
	}

	/**
	 * Original: RedstoneCycleTracker.update - alternating with the integrated redstone upgrade (while not powered) or when
	 * ReikaRedstoneHelper.isGettingACRedstone sees the signal change within the last three ticks.
	 */
	private void updateRedstone(Level world, BlockPos pos) {
		alternating = (integratedRedstone && !world.hasNeighborSignal(pos)) || this.isGettingACRedstone(world, pos);
		if (integratedRedstone && alternating) {
			if (world.getGameTime()%6 == 0) {
				float f = MachineSounds.isMuffled(world, pos) ? 0.05F : 0.25F;
				world.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, f, 0.66F);
			}
		}
	}

	private boolean isGettingACRedstone(Level world, BlockPos pos) {
		boolean currentpower = world.hasNeighborSignal(pos);
		boolean ac = false;
		for (int i = 0; i < lastPower.length && !ac; i++) {
			if (lastPower[i] != currentpower)
				ac = true;
		}
		for (int i = lastPower.length-1; i > 0; i--) {
			lastPower[i] = lastPower[i-1];
		}
		lastPower[0] = currentpower;
		return ac;
	}

	private void magnetize(ItemStack is, int amt) {
		if (amt > 0)
			is.set(RotaryComponents.MAGNETIZATION.get(), amt);
		else
			is.remove(RotaryComponents.MAGNETIZATION.get());
		this.setChanged();
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

		RotarySounds.play(world, pos, RotarySounds.ELECTRIC.get(), 0.125F*volume, 1F*pitchMultiplier);
	}

	public void magneticInterference(int mag, double dd) {
		torque = (int)(0.0625*ReikaMathLibrary.logbase(mag, 2)*this.getEngineType().getTorque()/dd);
		omega = (int)(0.0625*ReikaMathLibrary.logbase(mag, 2)*this.getEngineType().getSpeed()/dd/4D);
		power = (long)omega*(long)torque;
	}

	@Override
	public int getFuelLevel() {
		return 0;
	}

	@Override
	protected void affectSurroundings(Level world, BlockPos pos, int meta) {

	}

	public void addRedstoneUpgrade() {
		integratedRedstone = true;
	}

	public boolean hasRedstoneUpgrade() {
		return integratedRedstone;
	}

	@Override
	public void upgrade(ItemStack is) {
		this.addRedstoneUpgrade();
	}

	@Override
	public boolean canUpgradeWith(ItemStack item) {
		return !this.hasRedstoneUpgrade() && EngineUpgradeItem.is(item, EngineUpgradeItem.Upgrades.REDSTONE);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("redstoneUpgrade", integratedRedstone);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		integratedRedstone = NBT.getBoolean("redstoneUpgrade");
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		super.onBlockBroken(world, pos);
		if (this.hasRedstoneUpgrade())
			this.dropItem(world, pos, new ItemStack(RotaryItems.UPGRADE_REDSTONE.get()));
	}

	public int getCoreMagnetization() {
		return inv.getStackInSlot(0).getOrDefault(RotaryComponents.MAGNETIZATION.get(), 0);
	}
}
