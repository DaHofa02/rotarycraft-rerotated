/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.electricraft.content.block.WireBlock;
import reika.electricraft.network.NetworkTile;
import reika.electricraft.network.Overloadable;
import reika.electricraft.network.WireEmitter;
import reika.electricraft.network.WireReceiver;
import reika.electricraft.network.WrappableWireSource;
import reika.electricraft.network.WrappedSource;
import reika.electricraft.registry.ElectriBlockEntities;
import reika.electricraft.registry.WireType;

/**
 * Original: Reika.ElectriCraft.TileEntities.TileEntityWire - networks on every side; its type sets resistance and current limit, too much
 * current melts it into lava. The drawn connections (block state) go to every wire and to every network part that accepts that side.
 */
public class WireBlockEntity extends WiringBlockEntity implements Overloadable {

	private boolean shouldMelt;

	public WireBlockEntity(BlockPos pos, BlockState state) {
		super(ElectriBlockEntities.WIRE.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public WireType getWireType() {
		return this.getBlockState().getBlock() instanceof WireBlock wb ? wb.type : WireType.STEEL;
	}

	public boolean isInsulated() {
		return this.getBlockState().getBlock() instanceof WireBlock wb && wb.insulated;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		super.updateEntity(world, pos, state);
		if (world.isClientSide)
			return;
		if (this.getTicksExisted() == 0)
			this.recomputeConnections(world, pos);
		if (shouldMelt)
			this.melt(world, pos);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public final boolean canNetworkOnSide(Direction dir) {
		return true;
	}

	/** Original isConnectedOnSideAt: other wires always, network parts on the sides they accept. */
	public boolean isConnectedOnSide(Level world, BlockPos pos, Direction dir) {
		BlockEntity te = world.getBlockEntity(pos.relative(dir));
		if (te instanceof WireBlockEntity)
			return true;
		boolean flag = false;
		if (te instanceof WiringBlockEntity w)
			flag = w.canNetworkOnSide(dir.getOpposite());
		if (te instanceof WireEmitter e)
			flag = flag || e.canEmitPowerToSide(dir.getOpposite());
		if (te instanceof WireReceiver r)
			flag = flag || r.canReceivePowerFromSide(dir.getOpposite());
		if (te instanceof WrappableWireSource w)
			flag = flag || w.canConnectToSide(dir.getOpposite());
		return flag;
	}

	@Override
	protected void onJoinNetwork() {
		super.onJoinNetwork();
		this.checkForWrappables();
	}

	@Override
	public void onNetworkChanged() {
		super.onNetworkChanged();
		this.checkForWrappables();
	}

	/** Original checkForWrappables: adjacent wrappable sources (the ReactorCraft generator) feed this wire's network. */
	private void checkForWrappables() {
		if (network == null)
			return;
		for (Direction dir : Direction.values()) {
			//never load a chunk from here: this runs while networks rebuild, also when chunks unload
			if (level != null && level.isLoaded(worldPosition.relative(dir)) && this.getAdjacentTileEntity(dir) instanceof WrappableWireSource w)
				network.addElement(new WrappedSource(w));
		}
	}

	public void recomputeConnections(Level world, BlockPos pos) {
		BlockState state = this.getBlockState();
		BlockState ns = state;
		for (Direction dir : Direction.values())
			ns = ns.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), this.isConnectedOnSide(world, pos, dir));
		if (ns != state)
			world.setBlock(pos, ns, 3);
		this.checkForWrappables();
	}

	public int getMaxCurrent() {
		return this.getWireType().maxCurrent;
	}

	public void melt(Level world, BlockPos pos) {
		world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
		if (world instanceof ServerLevel sl)
			sl.sendParticles(ParticleTypes.LAVA, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 12, 0.5, 0.5, 0.5, 0);
		world.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
	}

	@Override
	public void overload(int current) {
		shouldMelt = true;
	}

	@Override
	public int getResistance() {
		return this.getWireType().resistance;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("melt", shouldMelt);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		shouldMelt = NBT.getBoolean("melt");
	}
}
