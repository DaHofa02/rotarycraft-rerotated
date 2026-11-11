/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.lib.world.TemperatureEnvironment;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Auxiliary.TileEntityHeater - the heater. With at least 8192 W at 16 Nm from below it burns
 * furnace fuel from its 18 slots to reach the temperature set in its GUI: every max(1, 200 - 10*log2(omega+1)) ticks it takes the item
 * with the largest heat (burn time / 25) that is not larger than the missing temperature and rises by 1.5 times that heat. It heats the
 * machine above it (a tenth to all of the difference per tick, see transferHeat), affects the blocks around it like any hot machine,
 * sets mobs on it on fire from 240 deg C and blows up into lava above 2000 deg C. Every second it loses 1/200 of its difference to
 * the ambient temperature (at least 1 deg C). Fuel only goes in; signals 15 while idle (set temperature reached or no usable fuel).
 */
public class HeaterBlockEntity extends PowerReceiverBlockEntity implements TemperatureMachine, net.minecraft.world.Clearable {

	public static final int MAXTEMP = 2000;

	private int temperature;
	private int setTemperature;
	private int tickcount2;
	private boolean idle;

	private final ItemStackHandler inv = new ItemStackHandler(18) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return is.getBurnTime(RecipeType.SMELTING) > 0;
		}

		@Override
		protected void onContentsChanged(int slot) {
			HeaterBlockEntity.this.setChanged();
		}
	};

	public HeaterBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.HEATER.get(), pos, state, 16, 1, 8192);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public int getSetTemperature() {
		return setTemperature;
	}

	/** Original packet HEATER: the value typed into the GUI, taken as it is. */
	public void setSetTemperature(int temp) {
		setTemperature = temp;
		this.syncAllData();
	}

	public boolean isIdle() {
		return idle;
	}

	/** Original DurationRegistry.HEATER (200, 10). */
	public int getOperationTime() {
		return (int)Math.max(1, 200-10*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN;
		if (world.isClientSide)
			return;
		tickcount++;
		tickcount2++;
		this.readPower();
		if (tickcount2 >= 20) {
			this.updateTemperature(world, pos);
			tickcount2 = 0;
		}
		if (power >= MINPOWER) {
			this.testIdle(world, pos);
			if (tickcount >= this.getOperationTime()) {
				this.addHeat(world, pos);
				tickcount = 0;
			}
			if (this.isRemoved() || !world.getBlockState(pos).is(state.getBlock())) //blown up
				return;
			this.transferHeat(world, pos);
			if (temperature >= 240)
				this.ignite(world, pos);
		}
	}

	private void testIdle(Level world, BlockPos pos) {
		boolean was = idle;
		idle = setTemperature <= AmbientTemperature.getAmbientTemperatureAt(world, pos) || this.findHottestUsefulItemTemp(world, pos, setTemperature, false) == -1;
		if (was != idle)
			world.updateNeighborsAt(pos, this.getBlockState().getBlock());
	}

	private void ignite(Level world, BlockPos pos) {
		AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX()+1, pos.getY()+2, pos.getZ()+1);
		for (LivingEntity ent : world.getEntitiesOfClass(LivingEntity.class, box))
			ent.igniteForSeconds(temperature/50);
	}

	private void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		int old = temperature;
		if (temperature > Tamb)
			temperature -= Math.max((temperature-Tamb)/200, 1);
		if (temperature < Tamb)
			temperature += Math.max((Tamb-temperature)/40, 1);
		if (old != temperature)
			this.syncAllData();
	}

	private void addHeat(Level world, BlockPos pos) {
		int tempdiff = setTemperature-temperature;
		if (tempdiff > 0) {
			int deltaT = this.findHottestUsefulItemTemp(world, pos, tempdiff, true);
			if (deltaT != -1)
				temperature = (int)(temperature+deltaT*1.5);
			if (temperature > MAXTEMP) {
				this.overheat(world, pos);
				temperature = MAXTEMP;
			}
			this.syncAllData();
		}
	}

	/**
	 * Original ReikaWorldHelper.overheat(scrap, 0-17, sparks x1.2, flaming explosion 1.0) and then lava in the heater's place; the fuel
	 * drops with the block, into the explosion. The original also resets both temperatures of the tile it has just removed.
	 */
	public void overheat(Level world, BlockPos pos) {
		world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		world.explode(null, pos.getX(), pos.getY(), pos.getZ(), 1.0F, true, Level.ExplosionInteraction.BLOCK);
		if (world instanceof ServerLevel sl)
			sl.sendParticles(ParticleTypes.LAVA, pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5, (int)((rand.nextInt(20)+20)*1.2F), 0.3, 0, 0.3, 0);
		int numdrops = rand.nextInt(18);
		if (numdrops > 0)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(RotaryItems.part(CraftingPart.SCRAP), numdrops));
		world.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
		temperature = 0;
		setTemperature = 0;
	}

	/** The heat (burn time / 25) of the hottest fuel item that is not hotter than maxT, or -1; consume takes one of it. */
	private int findHottestUsefulItemTemp(Level world, BlockPos pos, int maxT, boolean consume) {
		int slot = -1;
		int itemheat = -1;
		for (int i = 0; i < inv.getSlots(); i++) {
			ItemStack fuel = inv.getStackInSlot(i);
			if (!fuel.isEmpty()) {
				int heat = fuel.getBurnTime(RecipeType.SMELTING)/25;
				if (heat > 0 && heat <= maxT && heat > itemheat) {
					itemheat = heat;
					slot = i;
				}
			}
		}
		if (slot == -1)
			return -1;
		if (consume) {
			ItemStack is = inv.getStackInSlot(slot);
			boolean bucket = is.is(Items.LAVA_BUCKET);
			inv.extractItem(slot, 1, false);
			if (bucket) {
				ItemStack rest = this.returnBucket();
				if (!rest.isEmpty()) { //the original drops a lava bucket here - an obvious slip
					ItemEntity ei = new ItemEntity(world, pos.getX()+rand.nextDouble(), pos.getY()+rand.nextDouble(), pos.getZ()+rand.nextDouble(), rest);
					ei.setDeltaMovement(-0.2+0.4*rand.nextDouble(), 0.4*rand.nextDouble(), -0.2+0.4*rand.nextDouble());
					world.addFreshEntity(ei);
				}
			}
		}
		return itemheat;
	}

	/** The empty bucket of a burnt lava bucket stays in the inventory although a bucket is no fuel. */
	private ItemStack returnBucket() {
		for (int i = 0; i < inv.getSlots(); i++) {
			ItemStack is = inv.getStackInSlot(i);
			if (is.is(Items.BUCKET) && is.getCount() < is.getMaxStackSize()) {
				inv.setStackInSlot(i, is.copyWithCount(is.getCount()+1));
				return ItemStack.EMPTY;
			}
		}
		for (int i = 0; i < inv.getSlots(); i++) {
			if (inv.getStackInSlot(i).isEmpty()) {
				inv.setStackInSlot(i, new ItemStack(Items.BUCKET));
				return ItemStack.EMPTY;
			}
		}
		return new ItemStack(Items.BUCKET);
	}

	private void transferHeat(Level world, BlockPos pos) {
		TemperatureEnvironment.temperatureEnvironment(world, pos, temperature);
		if (world.getBlockEntity(pos.above()) instanceof FrictionHeatable th) {
			int tempdiff = temperature-th.getTemperature();
			if (tempdiff <= 0)
				return;
			if (tempdiff > 100)
				th.addTemperature(tempdiff/16);
			else if (tempdiff > 16)
				th.addTemperature(tempdiff/8);
			else if (tempdiff > 8)
				th.addTemperature(tempdiff/4);
			else
				th.addTemperature(tempdiff);
			if (th.getTemperature() > th.getMaxTemperature())
				th.onOverheat(world, pos.above());
		}
	}

	@Override
	public int getRedstoneOverride() {
		return idle ? 15 : 0;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public void addTemperature(int temp) {

	}

	@Override
	public int getTemperature() {
		return temperature;
	}

	@Override
	public void setTemperature(int temp) {
		temperature = temp;
	}

	@Override
	public int getMaxTemperature() {
		return MAXTEMP;
	}

	@Override
	public boolean allowHeatExtraction() {
		return true;
	}

	/** Fuel goes in from any side; nothing comes out (original canExtractItem). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return inv.getSlots();
			}

			@Override
			public ItemStack getStackInSlot(int slot) {
				return inv.getStackInSlot(slot);
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return inv.insertItem(slot, stack, simulate);
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return inv.isItemValid(slot, stack);
			}
		};
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("temperature", temperature);
		NBT.putInt("stemp", setTemperature);
		NBT.putBoolean("idle", idle);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		temperature = NBT.getInt("temperature");
		setTemperature = NBT.getInt("stemp");
		idle = NBT.getBoolean("idle");
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
