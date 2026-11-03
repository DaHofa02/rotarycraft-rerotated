/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.rotarycraft.content.blockentity.UpgradeableMachine;
import reika.rotarycraft.content.item.EngineUpgradeItem;
import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotarySounds;

/** Original: Reika.RotaryCraft.TileEntities.Engine.TileEntityGasEngine. Burns ethanol (items or fluid); the performance upgrade turns it into a performance engine. */
public class GasEngineBlockEntity extends EngineBlockEntity implements UpgradeableMachine {

	public GasEngineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.GAS_ENGINE.get(), pos, state, EngineType.GAS);
	}

	/** Original upgrade(): the engine's data (tanks, inventory, orientation, temperature) moves into a performance engine. */
	@Override
	public void upgrade(ItemStack is) {
		Level world = level;
		BlockPos pos = worldPosition;
		CompoundTag NBT = this.saveWithoutMetadata(world.registryAccess());
		replacing = true;
		world.setBlock(pos, RotaryBlocks.SPORT_ENGINE.get().defaultBlockState(), 3);
		if (world.getBlockEntity(pos) instanceof EngineBlockEntity te) {
			te.loadWithComponents(NBT, world.registryAccess());
			te.syncAllData();
		}
	}

	@Override
	public boolean canUpgradeWith(ItemStack item) {
		return EngineUpgradeItem.is(item, EngineUpgradeItem.Upgrades.PERFORMANCE);
	}

	@Override
	protected void consumeFuel(float scale) {
		fuel.drain((int)(scale*this.getConsumedFuel()), IFluidHandler.FluidAction.EXECUTE);
	}

	@Override
	protected void internalizeFuel() {
		if (!inv.getStackInSlot(0).isEmpty() && fuel.getFluidAmount()+1000 <= FUELCAP) {
			if (inv.getStackInSlot(0).is(RotaryItems.ethanol())) {
				inv.extractItem(0, 1, false);
				fuel.fill(new FluidStack(RotaryFluids.ETHANOL.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
			}
		}
	}

	@Override
	protected boolean getRequirements(Level world, BlockPos pos, int meta) {
		return !fuel.isEmpty();
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
	public int getFuelLevel() {
		return fuel.getFluidAmount();
	}

	@Override
	protected void affectSurroundings(Level world, BlockPos pos, int meta) {

	}
}
