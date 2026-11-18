/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.api.interfaces.ComplexIO;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryBlocks;

/**
 * The portal shaft, written for the port from the original's behaviour. A plain shaft whose output faces a nether or end portal becomes a
 * portal shaft. If the same kind of portal stands at the matching place in the other dimension (nether coordinates are an eighth of the
 * overworld's) and a shaft there takes its input from that portal, that shaft becomes the partner portal shaft and gives out the power that
 * goes into the first one. Portal shafts keep the 3x3 chunks around them loaded. A partner whose source is gone, or a portal shaft whose
 * partner is gone, turns back into a plain shaft; broken, a portal shaft drops its plain shaft.
 * Changes: Mystcraft and Twilight Forest portals are left out; a partner that has not been fed for two ticks gives nothing (the original
 * kept the last values until it was turned back).
 */
public class PortalShaftBlockEntity extends OneDTransmitterBlockEntity {

	public enum PortalType {
		NETHER, END;

		@Nullable
		public static PortalType of(BlockState s) {
			return s.is(Blocks.NETHER_PORTAL) ? NETHER : s.is(Blocks.END_PORTAL) ? END : null;
		}

		/** The dimension on the other side, or null if this portal does not lead anywhere from here. */
		@Nullable
		public ResourceKey<Level> target(ResourceKey<Level> from) {
			if (from == Level.OVERWORLD)
				return this == NETHER ? Level.NETHER : Level.END;
			return (this == NETHER && from == Level.NETHER) || (this == END && from == Level.END) ? Level.OVERWORLD : null;
		}
	}

	private MaterialRegistry material = MaterialRegistry.STEEL;
	@Nullable
	private ResourceKey<Level> linkDim;
	@Nullable
	private BlockPos linkPos;
	private long lastFed = Long.MIN_VALUE;
	private boolean ticketed;

	public PortalShaftBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.PORTAL_SHAFT.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public MaterialRegistry getShaftType() {
		return material;
	}

	@Nullable
	public BlockPos getLinkPos() {
		return linkPos;
	}

	public boolean isLinked() {
		return linkPos != null;
	}

	public void getIOSides(int meta) {
		read = switch (meta) {
			case 0 -> Direction.EAST;
			case 1 -> Direction.WEST;
			case 2 -> Direction.SOUTH;
			case 3 -> Direction.NORTH;
			case 4 -> Direction.DOWN;
			default -> Direction.UP;
		};
		write = read.getOpposite();
	}

	/** Turns the block at pos (a plain shaft) into a portal shaft of the same orientation and material. */
	public static PortalShaftBlockEntity convert(Level world, BlockPos pos, int meta, MaterialRegistry mat) {
		world.setBlock(pos, RotaryBlocks.PORTAL_SHAFT.get().defaultBlockState(), Block.UPDATE_ALL);
		PortalShaftBlockEntity ps = (PortalShaftBlockEntity)world.getBlockEntity(pos);
		ps.setBlockMetadata(meta);
		ps.material = mat;
		ps.getIOSides(meta);
		return ps;
	}

	/** Back to a plain shaft. */
	private void revert(Level world) {
		int meta = this.getBlockMetadata();
		MaterialRegistry mat = material;
		this.release(world);
		world.setBlock(worldPosition, RotaryBlocks.SHAFT.get().defaultBlockState(), Block.UPDATE_ALL);
		if (world.getBlockEntity(worldPosition) instanceof ShaftBlockEntity sh) {
			sh.setBlockMetadata(meta);
			sh.setShaftType(mat);
		}
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		ServerLevel sl = (ServerLevel)world;
		if (!ticketed) {
			this.forceChunks(sl, true);
			ticketed = true;
		}
		PortalType type = PortalType.of(world.getBlockState(pos.relative(write)));
		if (type != null) {
			this.transferPower(world, pos, this.getBlockMetadata());
			this.emit(sl, type);
		}
		else if (world.getGameTime()-lastFed > 2) {
			omega = torque = 0;
			power = 0;
		}
		if (linkPos != null && !(this.linked(sl) instanceof PortalShaftBlockEntity))
			this.revert(world);
	}

	@Nullable
	private BlockEntity linked(ServerLevel sl) {
		ServerLevel other = linkDim == null ? null : sl.getServer().getLevel(linkDim);
		if (other == null || linkPos == null || !other.isLoaded(linkPos))
			return this; //cannot tell: keep
		return other.getBlockEntity(linkPos);
	}

	/** Same-dimension coordinate scaling of the original: overworld to nether divides x and z by 8, nether to overworld multiplies. */
	public static BlockPos scale(BlockPos p, ResourceKey<Level> from, ResourceKey<Level> to) {
		if (from != Level.NETHER && to == Level.NETHER)
			return new BlockPos(p.getX()/8, p.getY(), p.getZ()/8);
		if (from == Level.NETHER && to != Level.NETHER)
			return new BlockPos(p.getX()*8, p.getY(), p.getZ()*8);
		return p;
	}

	private void emit(ServerLevel sl, PortalType type) {
		ResourceKey<Level> dim = type.target(sl.dimension());
		ServerLevel other = dim == null ? null : sl.getServer().getLevel(dim);
		if (other == null)
			return;
		BlockPos portal = scale(worldPosition.relative(write), sl.dimension(), dim);
		if (!other.isLoaded(portal) || PortalType.of(other.getBlockState(portal)) != type)
			return;
		BlockPos far = portal.relative(write);
		BlockEntity te = other.getBlockEntity(far);
		if (te instanceof ShaftBlockEntity sh && !sh.isCross()) {
			sh.getIOSides(sh.getBlockMetadata());
			if (far.relative(sh.getReadDirection()).equals(portal)) {
				PortalShaftBlockEntity ps = convert(other, far, sh.getBlockMetadata(), material);
				ps.linkDim = sl.dimension();
				ps.linkPos = worldPosition;
				linkDim = dim;
				linkPos = far;
				te = ps;
			}
		}
		if (te instanceof PortalShaftBlockEntity ps && far.relative(ps.read).equals(portal)) {
			ps.omega = omega;
			ps.torque = torque;
			ps.power = power;
			ps.lastFed = other.getGameTime();
			if (ps.linkPos == null) {
				ps.linkDim = sl.dimension();
				ps.linkPos = worldPosition;
			}
			if (linkPos == null) {
				linkDim = dim;
				linkPos = far;
			}
		}
	}

	/** Input like a plain shaft's (cross shafts and splitting splitters give nothing). */
	@Override
	protected void transferPower(Level world, BlockPos pos, int meta) {
		omegain = torquein = 0;
		BlockEntity te = this.getAdjacentTileEntity(read);
		if (this.isProvider(te)) {
			if (te instanceof ShaftBlockEntity sh) {
				if (!sh.isCross() && sh.isWritingTo(this)) {
					torquein = sh.torque;
					omegain = sh.omega;
				}
			}
			else if (te instanceof SplitterBlockEntity spl) {
				if (!spl.isSplitting() && spl.isWritingTo(this)) {
					torquein = spl.torque;
					omegain = spl.omega;
				}
			}
			else if (te instanceof SimpleProvider)
				this.copyStandardPower(te);
			else if (te instanceof ComplexIO pwr) {
				omegain = pwr.getSpeedToSide(read.getOpposite());
				torquein = pwr.getTorqueToSide(read.getOpposite());
			}
			else if (te instanceof ShaftPowerEmitter sp && sp.isEmitting() && sp.canWriteTo(read.getOpposite())) {
				torquein = sp.getTorque();
				omegain = sp.getOmega();
			}
		}
		omega = omegain;
		torque = torquein;
		power = (long)omega*(long)torque;
	}

	private void forceChunks(ServerLevel sl, boolean add) {
		int cx = worldPosition.getX() >> 4, cz = worldPosition.getZ() >> 4;
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++)
				ChunkLoaderBlockEntity.TICKETS.forceChunk(sl, worldPosition, cx+dx, cz+dz, add, true);
	}

	private void release(Level world) {
		if (world instanceof ServerLevel sl && ticketed) {
			this.forceChunks(sl, false);
			ticketed = false;
		}
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		this.release(world);
	}

	@Override
	public void setRemoved() {
		if (level != null)
			this.release(level);
		super.setRemoved();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.25);
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putString("mat", material.name());
		if (linkPos != null && linkDim != null) {
			NBT.put("link", NbtUtils.writeBlockPos(linkPos));
			NBT.putString("linkdim", linkDim.location().toString());
		}
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		try {
			material = MaterialRegistry.valueOf(NBT.getString("mat"));
		}
		catch (IllegalArgumentException e) {
			material = MaterialRegistry.STEEL;
		}
		linkPos = NbtUtils.readBlockPos(NBT, "link").orElse(null);
		ResourceLocation d = NBT.contains("linkdim") ? ResourceLocation.tryParse(NBT.getString("linkdim")) : null;
		linkDim = d == null ? null : ResourceKey.create(Registries.DIMENSION, d);
	}
}
