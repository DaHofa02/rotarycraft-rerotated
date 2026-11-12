/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.item.EngineUpgradeItem;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.GearboxTypes.GearPart;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Processing.TileEntityMagnetizer - charges the shaft core in its slot (for the AC engine).
 * Needs 16384 W and 2048 rad/s and an alternating redstone signal (or the integrated redstone upgrade while unpowered); every
 * max(2, 400 - 20*log2(omega+1)) ticks it raises the core's magnetization by 1 uT, with a chance of 1/2 for a steel core and always for
 * a tungsten one, up to omega/2 uT (RecipesMagnetizer: 2 rad/s per uT). Only single cores go in. Not ported: the lodestone upgrade
 * and the magnetostatic engine upgrade recipe (neither item exists in the port).
 */
public class MagnetizerBlockEntity extends PowerReceiverBlockEntity implements UpgradeableMachine, net.minecraft.world.Clearable {

	/** Original RecipesMagnetizer: {minSpeed, speedPeruT, timeFactor}. */
	private record Recipe(int minSpeed, int speedPeruT, int timeFactor) {}

	private static final Recipe STEEL = new Recipe(0, 2, 2), TUNGSTEN = new Recipe(0, 2, 1);

	private final boolean[] lastPower = new boolean[3];
	private boolean integratedRedstone;
	private boolean alternating;

	private final ItemStackHandler inv = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return getRecipe(is) != null;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		protected void onContentsChanged(int slot) {
			MagnetizerBlockEntity.this.setChanged();
			MagnetizerBlockEntity.this.syncAllData();
		}
	};

	public MagnetizerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.MAGNETIZER.get(), pos, state, 1, 2048, 16384);
	}

	@Override
	public String getName() {
		return "Magnetizer";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	@Nullable
	private static Recipe getRecipe(ItemStack is) {
		if (is.is(RotaryItems.gearPart(GearboxTypes.STEEL, GearPart.SHAFTCORE)))
			return STEEL;
		if (is.is(RotaryItems.gearPart(GearboxTypes.TUNGSTEN, GearPart.SHAFTCORE)))
			return TUNGSTEN;
		return null;
	}

	public boolean hasCore() {
		return getRecipe(inv.getStackInSlot(0)) != null;
	}

	public int getCoreMagnetization() {
		return inv.getStackInSlot(0).getOrDefault(RotaryComponents.MAGNETIZATION.get(), 0);
	}

	public boolean isAlternating() {
		return alternating;
	}

	/** Original DurationRegistry.MAGNETIZER (400, 20), at least 2. */
	public int getOperationTime() {
		return Math.max(2, (int)Math.max(1, 400-20*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2)));
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		if (power < MINPOWER || omega < MINSPEED) {
			tickcount = 0;
			return;
		}
		this.updateRedstone(world, pos);
		if (!alternating)
			return;
		tickcount++;
		if (tickcount >= this.getOperationTime()) {
			tickcount = 0;
			Recipe r = getRecipe(inv.getStackInSlot(0));
			if (r != null && omega >= r.minSpeed())
				this.magnetize(r);
		}
	}

	private void magnetize(Recipe r) {
		if (rand.nextInt(r.timeFactor()) > 0)
			return;
		ItemStack is = inv.getStackInSlot(0).copy();
		int m = is.getOrDefault(RotaryComponents.MAGNETIZATION.get(), 0);
		if (m == 0)
			m = 1;
		else if (m < omega/r.speedPeruT())
			m++;
		is.set(RotaryComponents.MAGNETIZATION.get(), m);
		inv.setStackInSlot(0, is);
	}

	/** Original RedstoneCycleTracker(3).update, as in the AC engine. */
	private void updateRedstone(Level world, BlockPos pos) {
		boolean currentpower = world.hasNeighborSignal(pos);
		boolean ac = false;
		for (boolean b : lastPower)
			if (b != currentpower)
				ac = true;
		for (int i = lastPower.length-1; i > 0; i--)
			lastPower[i] = lastPower[i-1];
		lastPower[0] = currentpower;
		alternating = (integratedRedstone && !currentpower) || ac;
		if (integratedRedstone && alternating && world.getGameTime()%6 == 0)
			world.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.25F, 0.66F);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (level == null)
			phi = 0;
		else
			phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return this.hasCore() ? 0 : 15;
	}

	public boolean hasRedstoneUpgrade() {
		return integratedRedstone;
	}

	@Override
	public void upgrade(ItemStack is) {
		integratedRedstone = true;
		this.syncAllData();
	}

	@Override
	public boolean canUpgradeWith(ItemStack item) {
		return !integratedRedstone && EngineUpgradeItem.is(item, EngineUpgradeItem.Upgrades.REDSTONE);
	}

	/** One core in from any side; out from any side. */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return inv;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(0).copy());
		if (integratedRedstone)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(RotaryItems.UPGRADE_REDSTONE.get()));
	}

	@Override
	public void clearContent() {
		inv.setStackInSlot(0, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("redstoneUpgrade", integratedRedstone);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		integratedRedstone = NBT.getBoolean("redstoneUpgrade");
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
