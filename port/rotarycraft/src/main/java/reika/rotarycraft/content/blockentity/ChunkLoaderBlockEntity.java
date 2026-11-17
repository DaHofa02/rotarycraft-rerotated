/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.TileEntityChunkLoader - with at least 2097152 rad/s from below it keeps the square of chunks
 * around it loaded: radius (power - 2097152) / 524288 chunks (the original subtracts the speed from the power), at most 8 (the config
 * default); at radius 0 just its own chunk. Below that speed, or when broken, the chunks are released. Client: ender particles spiral up.
 * Changes: DragonAPI's ChunkManager is a NeoForge ticket controller; tickets are dropped on a world load and asked for again by running
 * loaders. A change of radius while running is applied at once (the original kept the first square until the loader stopped).
 */
public class ChunkLoaderBlockEntity extends PowerReceiverBlockEntity {

	public static final int BASE_RADIUS = 0;
	public static final int FALLOFF = 524288;
	public static final int MAX_RADIUS = 8;

	public static final TicketController TICKETS = new TicketController(ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "chunk_loader"),
			(level, helper) -> helper.getBlockTickets().keySet().forEach(helper::removeAllTickets));

	/** The radius currently held, -1 for none. */
	private int loaded = -1;

	public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.CHUNK_LOADER.get(), pos, state, 1, 2097152, 1);
	}

	public static void registerTickets(RegisterTicketControllersEvent e) {
		e.register(TICKETS);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN; //getPowerBelow
		if (world.isClientSide) {
			if (omega >= MINSPEED)
				this.doParticles(world, pos);
			return;
		}
		this.readPower();
		if (omega >= MINSPEED)
			this.load((ServerLevel)world, this.getLoadingRadius());
		else
			this.unload((ServerLevel)world);
	}

	private void doParticles(Level world, BlockPos pos) {
		double[] r = {0.3125, 0.25, 0.1875, 0.125};
		double[] h = {0.25, 0.4375, 0.625, 0.8125};
		for (int i = 0; i < 4; i++) {
			double a = (4-i)*phi+40;
			double dr = r[i]+0.0625;
			for (int k = 0; k < 2; k++, a += 180)
				world.addParticle(ParticleTypes.PORTAL, pos.getX()+0.5+dr*Math.cos(Math.toRadians(a)), pos.getY()+h[i], pos.getZ()+0.5+dr*Math.sin(Math.toRadians(a)), 0, 0.0625, 0);
		}
	}

	public int getLoadingRadius() {
		return Math.min(MAX_RADIUS, BASE_RADIUS+(int)(power-MINSPEED)/FALLOFF);
	}

	public boolean isLoading() {
		return loaded >= 0;
	}

	public int getLoadedRadius() {
		return loaded;
	}

	private void load(ServerLevel world, int r) {
		r = Math.max(0, r);
		if (loaded == r)
			return;
		this.unload(world);
		this.force(world, r, true);
		loaded = r;
	}

	private void unload(ServerLevel world) {
		if (loaded < 0)
			return;
		this.force(world, loaded, false);
		loaded = -1;
	}

	/** Original ChunkManager.getChunkSquare: the (2r+1)x(2r+1) chunks around the block. */
	private void force(ServerLevel world, int r, boolean add) {
		int cx = worldPosition.getX() >> 4, cz = worldPosition.getZ() >> 4;
		for (int dx = -r; dx <= r; dx++)
			for (int dz = -r; dz <= r; dz++)
				TICKETS.forceChunk(world, worldPosition, cx+dx, cz+dz, add, true);
	}

	public int getRange() {
		return this.getLoadingRadius()*16;
	}

	public int getMaxRange() {
		return MAX_RADIUS*16;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (omega > 0)
			phi -= (float)(0.25*ReikaMathLibrary.logbase(omega+1, 2));
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		if (world instanceof ServerLevel sl)
			this.unload(sl);
	}

	@Override
	public void setRemoved() {
		if (level instanceof ServerLevel sl)
			this.unload(sl);
		super.setRemoved();
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("loaded", loaded);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		if (level == null || level.isClientSide)
			loaded = NBT.getInt("loaded");
	}
}
