/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.RotaryConfig;

/**
 * Original: Reika.RotaryCraft.Base.TileEntity.RotaryCraftTileEntity together with the parts of
 * Reika.DragonAPI.Base.TileEntityBase it relies on (tick bookkeeping, owner, delta sync every 5 ticks).
 * The sync tag ({@link #writeSyncTag}) is what the client sees; the full tag adds {@link #writeAdditional}.
 */
public abstract class RotaryCraftBlockEntity extends BlockEntity {

	/** Original: DragonAPI getPacketDelay() with SLOWSYNC off. */
	private static final int PACKET_DELAY = 5;

	/** Shared by all machines on the client and the server thread, so it must be the thread-safe variant. */
	protected static final RandomSource rand = RandomSource.createThreadSafe();

	protected int tickcount = 0;
	/** Rotational position, for rendering. */
	public float phi = 0;

	/** For EMP */
	private boolean disabled;

	public boolean isFlipped = false;

	/** Orientation state. The original kept this in the tile entity (DragonAPI setBlockMetadata), not in the world block metadata. */
	private int blockMetadata;

	private int ticksExisted;
	@Nullable
	private UUID placerUUID;
	private String placer = "";

	@Nullable
	private CompoundTag lastSentSync;

	protected RotaryCraftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** Called every tick on both sides, like the original updateEntity(World, x, y, z, meta). */
	protected abstract void updateEntity(Level world, BlockPos pos, BlockState state);

	/** Client-side animation, like the original animateWithTick. */
	protected abstract void animateWithTick(Level world, BlockPos pos);

	/** Display name for chat output and tools. */
	public abstract String getName();

	public static void tick(Level level, BlockPos pos, BlockState state, RotaryCraftBlockEntity te) {
		te.updateEntity(level, pos, state);
		if (level.isClientSide) {
			te.animateWithTick(level, pos);
		}
		else {
			if (te.ticksExisted < 20 && te.ticksExisted%4 == 0)
				te.sendSync(true);
			else if (level.getGameTime()%PACKET_DELAY == 0)
				te.sendSync(false);
		}
		te.ticksExisted++;
	}

	private void sendSync(boolean force) {
		CompoundTag tag = new CompoundTag();
		this.writeSyncTag(tag, level.registryAccess());
		if (force || !tag.equals(lastSentSync)) {
			lastSentSync = tag;
			level.sendBlockUpdated(worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
			this.setChanged();
		}
	}

	/** Forces the next sync to go out (original: syncAllData). */
	public final void syncAllData() {
		lastSentSync = null;
		if (level != null && !level.isClientSide)
			this.sendSync(true);
	}

	public int getBlockMetadata() {
		return blockMetadata;
	}

	public void setBlockMetadata(int meta) {
		blockMetadata = meta;
		this.setChanged();
	}

	public final int getTicksExisted() {
		return ticksExisted;
	}

	public int getTick() {
		return tickcount;
	}

	/** Comparator output (original getRedstoneOverride). */
	public int getRedstoneOverride() {
		return 0;
	}

	public boolean isShutdown() {
		return disabled;
	}

	public void onEMP() {
		disabled = true;
	}

	public void setPlacer(Player ep) {
		placer = ep.getGameProfile().getName();
		placerUUID = ep.getUUID();
	}

	public String getPlacerName() {
		return placer;
	}

	@Nullable
	public UUID getPlacerUUID() {
		return placerUUID;
	}

	@Nullable
	public Player getPlacer() {
		return placerUUID == null || level == null ? null : level.getPlayerByUUID(placerUUID);
	}

	public boolean isPlayerAccessible(Player ep) {
		if (RotaryConfig.LOCKMACHINES.get() && placerUUID != null && !ep.getUUID().equals(placerUUID))
			return false;
		return ep.distanceToSqr(worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5) <= 64;
	}

	/**
	 * Original: DragonAPI BlockTEBase.breakBlock - drops the inventory and runs BreakAction.breakBlock when the block is removed
	 * (server side, not when the block is replaced by the same block).
	 */
	public void onBlockBroken(Level world, BlockPos pos) {

	}

	@Nullable
	public final BlockEntity getAdjacentTileEntity(@Nullable Direction dir) {
		return level == null || dir == null ? null : level.getBlockEntity(worldPosition.relative(dir));
	}

	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		NBT.putInt("meta", blockMetadata);
		if (isFlipped)
			NBT.putBoolean("flip", isFlipped);
		if (!placer.isEmpty())
			NBT.putString("place", placer);
		if (placerUUID != null)
			NBT.putUUID("placeUUID", placerUUID);
	}

	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		blockMetadata = NBT.getInt("meta");
		isFlipped = NBT.getBoolean("flip");
		placer = NBT.getString("place");
		placerUUID = NBT.hasUUID("placeUUID") ? NBT.getUUID("placeUUID") : null;
	}

	/** Server-only persistent data (original: writeToNBT minus the sync part). */
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		NBT.putInt("tick", tickcount);
		NBT.putBoolean("emp", disabled);
	}

	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		tickcount = NBT.getInt("tick");
		disabled = NBT.getBoolean("emp");
	}

	@Override
	protected final void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		this.writeSyncTag(tag, registries);
		this.writeAdditional(tag, registries);
	}

	@Override
	protected final void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.readSyncTag(tag, registries);
		this.readAdditional(tag, registries);
	}

	@Override
	public final CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		this.writeSyncTag(tag, registries);
		return tag;
	}

	@Override
	public final Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public final void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
		this.readSyncTag(pkt.getTag(), registries);
	}

	@Override
	public final void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		this.readSyncTag(tag, registries);
	}
}
