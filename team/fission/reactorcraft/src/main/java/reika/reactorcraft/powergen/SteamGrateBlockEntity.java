/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.content.block.SteamBlock;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: Reika.ReactorCraft.TileEntities.PowerGen.TileEntitySteamGrate - pulls steam from the steam lines around it (a quarter of the
 * difference +1 per tick) and lets one unit out per tick as a steam block above it (steam that can drive a turbine; ammonia steam is
 * marked as such). With the screwdriver (sneaking) it only works with a redstone signal.
 */
public class SteamGrateBlockEntity extends RotaryCraftBlockEntity implements SteamTile, reika.rotarycraft.api.interfaces.Screwdriverable {

	private int steam;
	private boolean requireRedstone;
	private WorkingFluid fluid = WorkingFluid.EMPTY;

	public SteamGrateBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.STEAM_GRATE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Steam Grate";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.getSteam(world, pos);
		if (!world.isClientSide && this.canMakeSteam(world, pos)) {
			steam--;
			world.setBlock(pos.above(), ReactorBlocks.STEAM.get().defaultBlockState().setValue(SteamBlock.FLAGS, this.getSteamMetadata()), 3);
		}
		if (steam <= 0)
			fluid = WorkingFluid.EMPTY;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	private boolean canMakeSteam(Level world, BlockPos pos) {
		if (steam <= 0)
			return false;
		if (world.hasNeighborSignal(pos) != requireRedstone)
			return false;
		return SteamBlock.canMoveInto(world, pos.above());
	}

	private int getSteamMetadata() {
		return fluid == WorkingFluid.AMMONIA ? 7 : 3;
	}

	private boolean canTakeInWorkingFluid(WorkingFluid f) {
		if (f == WorkingFluid.EMPTY)
			return false;
		return fluid == WorkingFluid.EMPTY || fluid == f;
	}

	private void getSteam(Level world, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			if (world.getBlockEntity(pos.relative(dir)) instanceof SteamLineBlockEntity te && this.canTakeInWorkingFluid(te.getWorkingFluid())) {
				fluid = te.getWorkingFluid();
				int ds = te.getSteam()-steam;
				if (ds > 0) {
					int rm = ds/4+1;
					steam += rm;
					te.removeSteam(rm);
				}
			}
		}
	}

	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		requireRedstone = !requireRedstone;
		this.syncAllData();
		return requireRedstone;
	}

	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		return false;
	}

	@Override
	public int getSteam() {
		return steam;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("energy", steam);
		fluid.saveToNBT(NBT);
		NBT.putBoolean("red", requireRedstone);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		steam = NBT.getInt("energy");
		fluid = WorkingFluid.getFromNBT(NBT);
		requireRedstone = NBT.getBoolean("red");
	}
}
