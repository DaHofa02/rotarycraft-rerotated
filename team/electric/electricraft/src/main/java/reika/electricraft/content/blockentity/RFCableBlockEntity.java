/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import reika.electricraft.network.RFNetwork;
import reika.electricraft.registry.ElectriBlockEntities;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: Reika.ElectriCraft.TileEntities.ModInterface.TileEntityRFCable (ElectriCable) - joins its neighbours into an RF network,
 * registers the energy blocks next to it as end points and offers the network buffer (FE) on every side. Drawn connections go to
 * other cables and to every block with an energy capability on that side.
 */
public class RFCableBlockEntity extends RotaryCraftBlockEntity {

	@Nullable
	private RFNetwork network;
	private int RFlimit;

	public RFCableBlockEntity(BlockPos pos, BlockState state) {
		super(ElectriBlockEntities.RF_CABLE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "RF Transfer Cable";
	}

	public void setRFLimit(int limit) {
		if (RFlimit != limit) {
			RFlimit = limit;
			if (network != null)
				network.setIOLimit(limit);
			this.setChanged();
			this.syncAllData();
		}
	}

	public int getRFLimit() {
		return RFlimit;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		if (this.getTicksExisted() == 0 || network == null) {
			this.findAndJoinNetwork(world, pos);
			this.recomputeConnections(world, pos);
		}
		if (network != null && network.getIOLimit() != RFlimit)
			RFlimit = network.getIOLimit();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	public final void findAndJoinNetwork(Level world, BlockPos pos) {
		network = new RFNetwork();
		network.setIOLimit(this.getRFLimit());
		network.addElement(this);
		for (Direction dir : Direction.values()) {
			BlockPos p = pos.relative(dir);
			if (!world.isLoaded(p))
				continue;
			BlockEntity te = world.getBlockEntity(p);
			if (te instanceof RFCableBlockEntity n) {
				if (n.network != null)
					n.network.merge(network);
			}
			else if (world.getCapability(Capabilities.EnergyStorage.BLOCK, p, dir.getOpposite()) != null) {
				network.addConnection(world, p, dir.getOpposite());
			}
		}
	}

	@Nullable
	public RFNetwork getNetwork() {
		return network;
	}

	public void setNetwork(RFNetwork n) {
		network = n;
		network.addElement(this);
	}

	public void resetNetwork() {
		network = null;
	}

	public void removeFromNetwork() {
		if (network != null)
			network.removeElement(this);
	}

	/** Original recomputeConnections/onNetworkUpdate: redraws the arms and registers new energy neighbours. */
	public void recomputeConnections(Level world, BlockPos pos) {
		BlockState state = this.getBlockState();
		BlockState ns = state;
		for (Direction dir : Direction.values()) {
			BlockPos p = pos.relative(dir);
			boolean cable = world.getBlockEntity(p) instanceof RFCableBlockEntity;
			boolean handler = !cable && world.isLoaded(p) && world.getCapability(Capabilities.EnergyStorage.BLOCK, p, dir.getOpposite()) != null;
			if (handler && network != null)
				network.addConnection(world, p, dir.getOpposite());
			ns = ns.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), cable || handler);
		}
		if (ns != state)
			world.setBlock(pos, ns, 3);
	}

	/** FE access for other mods' cables and machines (original IEnergyHandler). */
	public IEnergyStorage getEnergyStorage() {
		return new IEnergyStorage() {
			@Override
			public int receiveEnergy(int max, boolean simulate) {
				return network != null ? network.addEnergy(max, simulate) : 0;
			}

			@Override
			public int extractEnergy(int max, boolean simulate) {
				return network != null ? network.drainEnergy(max, simulate) : 0;
			}

			@Override
			public int getEnergyStored() {
				return 0;
			}

			@Override
			public int getMaxEnergyStored() {
				return Integer.MAX_VALUE;
			}

			@Override
			public boolean canExtract() {
				return true;
			}

			@Override
			public boolean canReceive() {
				return true;
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("limit", RFlimit);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		if (NBT.contains("limit"))
			RFlimit = NBT.getInt("limit");
	}
}
