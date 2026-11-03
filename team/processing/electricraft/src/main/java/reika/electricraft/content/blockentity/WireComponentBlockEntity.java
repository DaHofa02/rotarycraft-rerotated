/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Original: Reika.ElectriCraft.Base.TileEntityWireComponent - an in-line part of a wire run (relay, resistor, fuse) that connects on
 * its facing side and the opposite one; the screwdriver turns it to the next horizontal side.
 */
public abstract class WireComponentBlockEntity extends WiringBlockEntity {

	private Direction facing;

	protected WireComponentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public final boolean canNetworkOnSide(Direction dir) {
		return dir == this.getFacing() || dir == this.getFacing().getOpposite();
	}

	public final Direction getFacing() {
		return facing != null ? facing : Direction.EAST;
	}

	public final void setFacing(Direction dir) {
		facing = dir;
		this.setChanged();
	}

	/** Original incrementFacing (screwdriver). */
	public void incrementFacing() {
		int o = this.getFacing().ordinal();
		this.setFacing(o == 5 ? Direction.values()[2] : Direction.values()[o+1]);
		this.rebuildNetwork();
		this.syncAllData();
	}

	public abstract float getHeight();

	public abstract float getWidth();

	/** Original getAABB: full length along the facing axis, getWidth() across, getHeight() tall (from the top when flipped). */
	public final VoxelShape getShape() {
		float miny = 0;
		float maxy = this.getHeight();
		float w = this.getWidth()/2F;
		Direction dir = this.getFacing();
		float maxx = dir.getStepX() != 0 ? 1 : 0.5F+w;
		float minx = dir.getStepX() != 0 ? 0 : 0.5F-w;
		float maxz = dir.getStepZ() != 0 ? 1 : 0.5F+w;
		float minz = dir.getStepZ() != 0 ? 0 : 0.5F-w;
		if (isFlipped) {
			miny = 1-this.getHeight();
			maxy = 1;
		}
		return Shapes.box(minx, miny, minz, maxx, maxy, maxz);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("face", this.getFacing().ordinal());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		facing = NBT.contains("face") ? Direction.values()[NBT.getInt("face")] : null;
	}
}
