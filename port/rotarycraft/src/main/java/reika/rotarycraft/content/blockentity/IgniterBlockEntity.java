/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.lib.world.TemperatureEnvironment;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.World.TileEntityIgniter - burns fuel from its 18 slots to heat up (wood 400, coal 600, blaze
 * powder 800, lava 1200, thermite = aluminium dust and iron 2500 deg C; the hottest fuel it has all parts of) and sets its surroundings
 * ablaze: every tick 1 + T/50 random spots within range 16 + ceil8(sqrt(2T)) (half that up and down) feel its
 * temperature (fire, glass and melting as for the heat ray; at most 400 deg C without block damage), and from 280 deg C living things in
 * range catch fire. Every 40 ticks the temperature drifts to the ambient one (down by ln(difference), up by a 24th) and a fuel piece is
 * burnt while below the fuel's temperature, closing a quarter of the gap. Needs 32768 W and 1024 rad/s from any side.
 * Change: a lava bucket gives its empty bucket back (the original lost it).
 */
public class IgniterBlockEntity extends PowerReceiverBlockEntity implements TemperatureMachine, net.minecraft.world.Clearable {

	public static final int ANIMALIGNITION = 280;
	public static final int MAXTEMP = 2500;

	private static final TagKey<Item> ALUMINIUM_DUST = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "dusts/aluminum"));
	private static final TagKey<Item> IRON_DUST = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "dusts/iron"));

	/** Original IgnitionFuel: its temperature and the items it needs (one of each). */
	public enum IgnitionFuel {
		WOOD(400, is -> is.is(ItemTags.PLANKS) || is.is(ItemTags.LOGS)),
		COAL(600, is -> is.is(Items.COAL)),
		BLAZE(800, is -> is.is(Items.BLAZE_POWDER)),
		LAVA(1200, is -> is.is(Items.LAVA_BUCKET)),
		THERMITE(2500, is -> is.is(ALUMINIUM_DUST), is -> is.is(Items.IRON_INGOT) || is.is(IRON_DUST));

		public final int temperature;
		private final List<Predicate<ItemStack>> items;

		@SafeVarargs
		IgnitionFuel(int t, Predicate<ItemStack>... items) {
			temperature = t;
			this.items = List.of(items);
		}
	}

	private int temperature;
	@Nullable
	private IgnitionFuel fuel;

	private final ItemStackHandler inv = new ItemStackHandler(18) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return isValidFuel(is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			IgniterBlockEntity.this.setChanged();
		}
	};

	public IgniterBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.IGNITER.get(), pos, state, 1, 1024, 32768);
	}

	@Override
	public String getName() {
		return "Firestarter";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	@Nullable
	public IgnitionFuel getFuel() {
		return fuel;
	}

	public static boolean isValidFuel(ItemStack is) {
		for (IgnitionFuel f : IgnitionFuel.values())
			for (Predicate<ItemStack> m : f.items)
				if (m.test(is))
					return true;
		return false;
	}

	private int find(Predicate<ItemStack> m) {
		for (int k = 0; k < inv.getSlots(); k++)
			if (!inv.getStackInSlot(k).isEmpty() && m.test(inv.getStackInSlot(k)))
				return k;
		return -1;
	}

	/** Original IgnitionFuel.getFromItems: the hottest fuel all of whose items are there. */
	@Nullable
	private IgnitionFuel findFuel() {
		IgnitionFuel[] all = IgnitionFuel.values();
		for (int i = all.length-1; i >= 0; i--) {
			boolean valid = true;
			for (Predicate<ItemStack> m : all[i].items)
				if (this.find(m) < 0)
					valid = false;
			if (valid)
				return all[i];
		}
		return null;
	}

	public int getRange() {
		int s = (int)Math.sqrt(temperature*2);
		return 16+(s+7)/8*8;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		tickcount++;
		if (!world.isClientSide)
			this.readPowerSummative();
		if (power < MINPOWER || omega < MINSPEED)
			return;
		if (tickcount >= 40 && !world.isClientSide) {
			this.updateTemperature(world, pos);
			tickcount = 0;
			fuel = this.findFuel();
			if (fuel != null && temperature < fuel.temperature)
				this.burnFuel(world, pos);
			this.syncAllData();
		}
		if (fuel == null)
			return;
		int spread = this.getRange(), yspread = spread/2;
		int n = 1+temperature/50;
		for (int i = 0; i < n; i++) {
			BlockPos f = pos.offset(rand.nextInt(2*spread+1)-spread, rand.nextInt(2*yspread+1)-yspread, rand.nextInt(2*spread+1)-spread);
			if (world.isClientSide) {
				if (i == 0)
					world.addParticle(ParticleTypes.FLAME, f.getX(), f.getY()+1, f.getZ(), 0, 0, 0);
			}
			else
				TemperatureEnvironment.temperatureEnvironment(world, f, RotaryConfig.ATTACKBLOCKS.get() ? temperature : Math.min(400, temperature));
		}
		if (!world.isClientSide && temperature >= ANIMALIGNITION)
			for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(spread, yspread, spread)))
				e.igniteForSeconds(1);
	}

	private void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (temperature > Tamb)
			temperature -= (int)Math.log(temperature-Tamb);
		if (temperature < Tamb)
			temperature += (Tamb-temperature)/24;
		temperature = Math.min(temperature, MAXTEMP);
	}

	private void burnFuel(Level world, BlockPos pos) {
		for (Predicate<ItemStack> m : fuel.items) {
			int slot = this.find(m);
			if (slot < 0)
				continue;
			ItemStack ret = inv.getStackInSlot(slot).getCraftingRemainingItem();
			inv.extractItem(slot, 1, false);
			if (!ret.isEmpty()) {
				ItemStack left = this.storeAnywhere(ret);
				if (!left.isEmpty())
					Containers.dropItemStack(world, pos.getX()+rand.nextFloat(), pos.getY()+rand.nextFloat(), pos.getZ()+rand.nextFloat(), left);
			}
		}
		temperature += (fuel.temperature-temperature)/4;
	}

	/** Original addToInventoryWithLeftover: the bucket goes into any free slot, whatever the slot accepts. */
	private ItemStack storeAnywhere(ItemStack is) {
		for (int i = 0; i < inv.getSlots() && !is.isEmpty(); i++) {
			if (inv.getStackInSlot(i).isEmpty()) {
				inv.setStackInSlot(i, is);
				return ItemStack.EMPTY;
			}
		}
		return is;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return fuel != null ? 0 : 15;
	}

	@Override
	public void addTemperature(int temp) {
		temperature += temp;
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

	/** Fuel in from any side; nothing comes out (original canExtractItem: false). */
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
		NBT.putInt("fuel", fuel == null ? -1 : fuel.ordinal());
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		temperature = NBT.getInt("temperature");
		int f = NBT.contains("fuel") ? NBT.getInt("fuel") : -1;
		fuel = f >= 0 && f < IgnitionFuel.values().length ? IgnitionFuel.values()[f] : null;
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
