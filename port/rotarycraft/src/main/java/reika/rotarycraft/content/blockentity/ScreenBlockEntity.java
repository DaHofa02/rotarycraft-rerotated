/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Auxiliary.TileEntityScreen (CCTV screen) - with at least 1024 W and 4 Nm from below and three
 * dyes in its slots, sneak-right-clicking it calls up the camera with the same three colours within 64 blocks (the spy cam's map). Without
 * a valid camera it gives a redstone signal.
 * Changes: the camera is searched among the loaded block entities of the chunks in range (the original looked at every block of the
 * 129-block cube); its camera cache keyed by arrays never hit and is left out.
 */
public class ScreenBlockEntity extends PowerReceiverBlockEntity {

	public static final int SEARCH_RANGE = 64;

	private final ItemStackHandler inv = new ItemStackHandler(3) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return is.getItem() instanceof DyeItem;
		}

		@Override
		protected void onContentsChanged(int slot) {
			ScreenBlockEntity.this.setChanged();
		}
	};

	public ScreenBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SCREEN.get(), pos, state, 4, 1, 1024);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Original: dyes in (isItemValidForSlot), nothing out (canExtractItem false). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new net.neoforged.neoforge.items.wrapper.RangedWrapper(inv, 0, 3) {
			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return ItemStack.EMPTY;
			}
		};
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN; //getPowerBelow
		this.readPower();
	}

	public boolean canRun() {
		if (power < MINPOWER || torque < MINTORQUE)
			return false;
		for (int i = 0; i < 3; i++)
			if (!(inv.getStackInSlot(i).getItem() instanceof DyeItem))
				return false;
		return true;
	}

	/** Original activate: the matching camera's view, if it can run. */
	public void activate(ServerPlayer ep) {
		if (!this.canRun())
			return;
		RemoteControlled cam = this.findCamera();
		if (cam != null)
			cam.activate(ep);
	}

	@Nullable
	public RemoteControlled findCamera() {
		if (level == null || !this.canRunColors())
			return null;
		int[] colors = new int[3];
		for (int i = 0; i < 3; i++)
			colors[i] = ((DyeItem)inv.getStackInSlot(i).getItem()).getDyeColor().getId();
		ChunkPos c0 = new ChunkPos(worldPosition.offset(-SEARCH_RANGE, 0, -SEARCH_RANGE)), c1 = new ChunkPos(worldPosition.offset(SEARCH_RANGE, 0, SEARCH_RANGE));
		for (int cx = c0.x; cx <= c1.x; cx++) {
			for (int cz = c0.z; cz <= c1.z; cz++) {
				LevelChunk ch = level.getChunkSource().getChunkNow(cx, cz);
				if (ch == null)
					continue;
				for (BlockEntity te : ch.getBlockEntities().values()) {
					BlockPos p = te.getBlockPos();
					if (te != this && te instanceof RemoteControlled rc && Math.abs(p.getX()-worldPosition.getX()) <= SEARCH_RANGE
							&& Math.abs(p.getY()-worldPosition.getY()) <= SEARCH_RANGE && Math.abs(p.getZ()-worldPosition.getZ()) <= SEARCH_RANGE
							&& java.util.Arrays.equals(rc.getColors(), colors))
						return rc;
				}
			}
		}
		return null;
	}

	private boolean canRunColors() {
		for (int i = 0; i < 3; i++)
			if (!(inv.getStackInSlot(i).getItem() instanceof DyeItem))
				return false;
		return true;
	}

	@Override
	public int getRedstoneOverride() {
		return this.findCamera() != null ? 0 : 15;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < 3; i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
