/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.config.Actionable;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;

import reika.rotarycraft.content.blockentity.PowerReceiverBlockEntity;

/**
 * ME kinetic input: shaft power into its back is stored in the ME network as kinetic energy, power/20 J a tick less its loss (5 % by
 * default, 1 % less per acceleration card, up to 4 cards), up to the configured speed and torque (more is cut off). What the network cannot
 * take is lost. Needs a channel.
 */
public class KineticInputBlockEntity extends PowerReceiverBlockEntity implements IInWorldGridNodeHost, IUpgradeableObject, reika.rotarycraft.api.interfaces.Screwdriverable {

	public static final int CARD_SLOTS = 4;

	private final MEGridHost me = new MEGridHost(this, 2);
	private final IUpgradeInventory upgrades;
	private double carry;
	public long storedTotal;

	public KineticInputBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryME.KINETIC_INPUT_BE.get(), pos, state, 1, 1, 1);
		upgrades = UpgradeInventories.forMachine(state.getBlock(), CARD_SLOTS, this::setChanged);
	}

	@Override
	public IUpgradeInventory getUpgrades() {
		return upgrades;
	}

	public int getCards() {
		return upgrades.getInstalledUpgrades(RotaryME.speedCard());
	}

	/** Share of the shaft energy lost on the way in. */
	public double getLoss() {
		return Math.max(0, MEConfig.INPUT_LOSS.get()-0.01*this.getCards());
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	/** Power taken after the speed and torque caps. */
	public long getAcceptedPower() {
		long om = Math.min(omega, MEConfig.MAX_SPEED.get());
		long tq = Math.min(torque, MEConfig.MAX_TORQUE.get());
		return om*tq;
	}

	private boolean connectedClient;

	public boolean isConnected() {
		return level != null && level.isClientSide ? connectedClient : me.isActive();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		me.tick(world, read);
		this.readPower();
		carry += this.getAcceptedPower()/20D*(1-this.getLoss());
		long j = (long)carry;
		if (j > 0) {
			long done = me.insert(KineticKey.INSTANCE, j, Actionable.MODULATE);
			storedTotal += done;
			carry -= j;
		}
	}

	@Nullable
	@Override
	public IGridNode getGridNode(Direction dir) {
		return me.getNode(dir);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		me.destroy();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		me.destroy();
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < upgrades.size(); i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), upgrades.getStackInSlot(i).copy());
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		me.save(NBT);
		upgrades.writeToNBT(NBT, "upgrades", registries);
		NBT.putBoolean("connected", me.isActive());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		me.load(NBT);
		upgrades.readFromNBT(NBT, "upgrades", registries);
		connectedClient = NBT.getBoolean("connected");
	}

	/** The screwdriver turns it a quarter round (its in/out boxes show for a moment, like RotaryCraft machines). */
	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		this.setBlockMetadata((this.getBlockMetadata()+1)%4);
		this.getIOSidesDefault(this.getBlockMetadata());
		world.updateNeighborsAt(pos, this.getBlockState().getBlock());
		this.syncAllData();
		return true;
	}

	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		return false;
	}
}
