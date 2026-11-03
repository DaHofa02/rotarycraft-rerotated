/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.blockentity.UpgradeableMachine;
import reika.rotarycraft.content.item.EngineUpgradeItem;
import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotarySounds;

/** Original: Reika.RotaryCraft.TileEntities.Engine.TileEntityDCEngine. Runs on a redstone signal or with the integrated redstone upgrade. */
public class DCEngineBlockEntity extends EngineBlockEntity implements UpgradeableMachine {

	private boolean hasUpgrade;

	public DCEngineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.DC_ENGINE.get(), pos, state, EngineType.DC);
	}

	@Override
	protected void consumeFuel(float scale) {

	}

	@Override
	protected void internalizeFuel() {

	}

	@Override
	protected boolean getRequirements(Level world, BlockPos pos, int meta) {
		return hasUpgrade || world.hasNeighborSignal(pos);
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

	@Override
	public int getFuelLevel() {
		return 0;
	}

	@Override
	protected void affectSurroundings(Level world, BlockPos pos, int meta) {

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
	public void onBlockBroken(Level world, BlockPos pos) {
		super.onBlockBroken(world, pos);
		if (this.hasRedstoneUpgrade())
			this.dropItem(world, pos, new ItemStack(RotaryItems.UPGRADE_REDSTONE.get()));
	}

	public void addRedstoneUpgrade() {
		hasUpgrade = true;
	}

	public boolean hasRedstoneUpgrade() {
		return hasUpgrade;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("redstoneUpgrade", hasUpgrade);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		hasUpgrade = NBT.getBoolean("redstoneUpgrade");
	}
}
