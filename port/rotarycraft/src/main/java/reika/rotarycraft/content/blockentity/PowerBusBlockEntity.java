/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.api.interfaces.ComplexIO;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.GearboxTypes.GearPart;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntityPowerBus - a block of a power bus (chained to a bus controller). Each
 * horizontal side not facing another bus block or the controller holds one gear part (put in through the GUI): a shaft
 * or gear (or a stick) for 1:1, a 2x/4x/8x/16x gear unit for that ratio. Every side with a part (not the input side, not a disabled
 * clutch) gives out an equal share of the controller's torque at its speed, geared by the part - towards torque, or towards speed when
 * the side's mode button says so. A part whose material cannot take the result breaks.
 */
public class PowerBusBlockEntity extends IOMachineBlockEntity implements ComplexIO {

	private final ItemStackHandler inv = new ItemStackHandler(4) {
		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		protected void onContentsChanged(int slot) {
			PowerBusBlockEntity.this.setChanged();
		}
	};
	private boolean[] modes = new boolean[4];
	@Nullable
	private Direction inputSide;
	@Nullable
	private ShaftPowerBus bus;
	@Nullable
	private BlockPos hub;

	public PowerBusBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.POWER_BUS.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	private static int idx(Direction dir) {
		return dir.ordinal()-2;
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		if (bus == null && hub != null && world.getBlockEntity(hub) instanceof BusControllerBlockEntity c && c.getBus() != null)
			this.configureBusData(c.getBus());
		for (Direction dir : Direction.Plane.HORIZONTAL)
			if (this.canHaveItemInSlot(dir) && this.getAdjacentTileEntity(dir) != null)
				this.writeToPowerReceiver(dir, this.getSpeedToSide(dir), this.getTorqueToSide(dir));
	}

	public boolean canHaveItemInSlot(Direction dir) {
		BlockEntity te = level == null ? null : level.getBlockEntity(worldPosition.relative(dir));
		return !(te instanceof BusControllerBlockEntity) && !(te instanceof PowerBusBlockEntity);
	}

	/** Original GearboxTypes.getRatioFromPartItem: stick 1, shaft and gear 1, units 2-16, anything else 0. */
	public static int getRatioFromPartItem(ItemStack is) {
		if (is.isEmpty())
			return 0;
		if (is.is(Items.STICK))
			return 1;
		for (GearboxTypes g : GearboxTypes.typeList) {
			for (GearPart p : GearPart.list) {
				if (is.is(RotaryItems.gearPart(g, p))) {
					return switch (p) {
						case SHAFT, GEAR -> 1;
						case UNIT2 -> 2;
						case UNIT4 -> 4;
						case UNIT8 -> 8;
						case UNIT16 -> 16;
						default -> 0;
					};
				}
			}
		}
		return 0;
	}

	/** Original getMaterialFromCraftingItem: the part's material; a stick (metadata 0) counts as the material of row 0, steel. */
	@Nullable
	public static GearboxTypes getMaterialFromCraftingItem(ItemStack is) {
		if (is.is(Items.STICK))
			return GearboxTypes.STEEL;
		for (GearboxTypes g : GearboxTypes.typeList)
			for (GearPart p : GearPart.list)
				if (is.is(RotaryItems.gearPart(g, p)))
					return g;
		return null;
	}

	public int getAbsRatio(Direction dir) {
		return dir.getAxis() == Direction.Axis.Y ? 0 : getRatioFromPartItem(inv.getStackInSlot(idx(dir)));
	}

	/** Original insertItem: a valid part into that side's empty slot. */
	public boolean insertItem(ItemStack is, Direction side) {
		if (side.getAxis() != Direction.Axis.Y && getRatioFromPartItem(is) > 0 && inv.getStackInSlot(idx(side)).isEmpty()) {
			inv.setStackInSlot(idx(side), is.copyWithCount(1));
			return true;
		}
		return false;
	}

	public boolean isSideSpeedMode(Direction dir) {
		return modes[idx(dir)];
	}

	public void setMode(Direction dir, boolean speed) {
		modes[idx(dir)] = speed;
		this.syncAllData();
	}

	public boolean canOutputToSide(Direction dir) {
		return !this.isReceivingFromSide(dir) && this.getAbsRatio(dir) != 0 && !this.isDisabledClutch(dir);
	}

	private boolean isDisabledClutch(Direction dir) {
		return this.getAdjacentTileEntity(dir) instanceof ClutchBlockEntity c && !c.isOutputEnabled();
	}

	public boolean isReceivingFromSide(Direction dir) {
		return dir == this.getInputSide();
	}

	public long getInputPower() {
		return bus != null ? bus.getInputPower() : 0;
	}

	public int getInputTorque() {
		return bus != null ? bus.getInputTorque() : 0;
	}

	public int getTorquePerSide() {
		return bus != null ? this.getInputTorque()/bus.getTotalOutputSides() : 0;
	}

	@Override
	public int getTorqueToSide(Direction dir) {
		if (dir == null || dir.getAxis() == Direction.Axis.Y)
			return 0;
		int tbase = this.getTorquePerSide();
		int ratio = this.getAbsRatio(dir);
		if (ratio == 0)
			return 0;
		int trq = this.isSideSpeedMode(dir) ? tbase/ratio : tbase*ratio;
		GearboxTypes mat = getMaterialFromCraftingItem(inv.getStackInSlot(idx(dir)));
		if (mat != null && mat.material.getMaxShaftTorque() >= trq)
			return trq;
		this.breakItem(dir);
		return 0;
	}

	@Override
	public int getSpeedToSide(Direction dir) {
		if (bus == null || dir == null || dir.getAxis() == Direction.Axis.Y)
			return 0;
		int sbase = bus.getSpeed();
		int ratio = this.getAbsRatio(dir);
		if (ratio == 0)
			return 0;
		int spd = this.isSideSpeedMode(dir) ? sbase*ratio : sbase/ratio;
		GearboxTypes mat = getMaterialFromCraftingItem(inv.getStackInSlot(idx(dir)));
		if (mat != null && mat.material.getMaxShaftSpeed() >= spd)
			return spd;
		this.breakItem(dir);
		return 0;
	}

	private void breakItem(Direction dir) {
		inv.setStackInSlot(idx(dir), ItemStack.EMPTY);
		if (level != null && !level.isClientSide)
			for (int i = 0; i < 3; i++)
				level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 2, 1);
	}

	public Direction getInputSide() {
		return inputSide != null ? inputSide : Direction.EAST;
	}

	/** Original findNetwork (when set down): joins the bus of an adjacent controller or bus block; that side is its input. */
	public void findNetwork(Level world, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			BlockEntity te = world.getBlockEntity(pos.relative(dir));
			ShaftPowerBus b = te instanceof BusControllerBlockEntity c ? c.getBus() : te instanceof PowerBusBlockEntity p ? p.getBus() : null;
			if (b != null) {
				this.configureBusData(b);
				inputSide = dir;
				this.syncAllData();
				return;
			}
		}
	}

	private void configureBusData(ShaftPowerBus b) {
		b.addBlock(this);
		bus = b;
		hub = b.getController().getBlockPos();
	}

	@Nullable
	public ShaftPowerBus getBus() {
		return bus;
	}

	public void clearBus() {
		bus = null;
		hub = null;
	}

	@Override
	public boolean isWritingTo(PowerSourceTracker te) {
		for (Direction d : Direction.Plane.HORIZONTAL)
			if (this.canOutputToSide(d) && this.matchTile(te, d))
				return true;
		return false;
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		return bus != null && bus.getController() != null ? bus.getController().getPowerSources(io, caller) : new PowerSourceList();
	}

	@Override
	public boolean canProvidePower() {
		return true;
	}

	/** Original breakBlock: drops the parts (DragonAPI) and leaves the bus. */
	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < 4; i++)
			if (!inv.getStackInSlot(i).isEmpty())
				Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
		if (bus != null)
			bus.removeBlock(this);
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		if (dir == read)
			c.add(this.getAdjacentTileEntity(write));
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("in", this.getInputSide().ordinal());
		int m = 0;
		for (int i = 0; i < 4; i++)
			if (modes[i])
				m |= 1 << i;
		NBT.putByte("mode", (byte)m);
		if (hub != null)
			NBT.putLong("hub", hub.asLong());
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		inputSide = Direction.values()[NBT.getInt("in")];
		int m = NBT.getByte("mode");
		for (int i = 0; i < 4; i++)
			modes[i] = (m & (1 << i)) != 0;
		hub = NBT.contains("hub") ? BlockPos.of(NBT.getLong("hub")) : null;
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
