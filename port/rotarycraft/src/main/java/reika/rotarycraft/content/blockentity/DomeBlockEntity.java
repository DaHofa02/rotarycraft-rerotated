/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Base of the force field and the containment field, written for the port from the original's behaviour: power from below, a sphere of range
 * 2 + (power - minimum)/falloff blocks (at most 64) around the block, and only with nothing light-blocking above it within that range.
 */
public abstract class DomeBlockEntity extends PowerReceiverBlockEntity {

	public static final int MAX_RANGE = 64;

	private final long falloff;
	private int range;

	protected DomeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, long minPower, long falloff) {
		super(type, pos, state, 1, 1, minPower);
		this.falloff = falloff;
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	/** Range from the power alone. */
	public int getPowerRange() {
		return power < MINPOWER ? 0 : (int)Math.min(MAX_RANGE, 2+(power-MINPOWER)/falloff);
	}

	public int getRange() {
		return range;
	}

	public boolean isClear(Level world, BlockPos pos, int r) {
		for (int i = 1; i <= r; i++) {
			BlockPos p = pos.above(i);
			BlockState s = world.getBlockState(p);
			if (!s.isAir() && s.getLightBlock(world, p) > 0)
				return false;
		}
		return true;
	}

	public Vec3 center() {
		return Vec3.atCenterOf(worldPosition);
	}

	public AABB box() {
		return new AABB(worldPosition).inflate(range);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN;
		if (world.isClientSide)
			return;
		this.readPower();
		int r = this.getPowerRange();
		int old = range;
		range = r > 0 && this.isClear(world, pos, r) ? r : 0;
		if (range != old)
			this.syncAllData();
		if (range > 0)
			this.act(world, pos);
	}

	/** What the dome does to the entities in it, every tick while it is up. */
	protected abstract void act(Level world, BlockPos pos);

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(net.minecraft.nbt.CompoundTag NBT, net.minecraft.core.HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("range", range);
	}

	@Override
	protected void readSyncTag(net.minecraft.nbt.CompoundTag NBT, net.minecraft.core.HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		range = NBT.getInt("range");
	}
}
