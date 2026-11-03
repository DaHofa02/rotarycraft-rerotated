/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.reactorcraft.content.item.FuelItem;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorComponents;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.lib.math.RandomHelper;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.ReactorCraft.TileEntities.HTGR.TileEntityPebbleBed with PebbleBedArrangement - 47 single-item slots of fuel pebbles,
 * fed in at the top and passed down a stack; the bottom (first) slot can be taken out in a 40 of 80 ticks window (cooldown 10),
 * spent pebbles at any time. Each tick a reactor with pebbles reacts with 1-6 % by its size (pebble beds within 3 blocks of each other
 * form one reactor: 1-5 beds 1 %, 6+ 2 %, 12+ 4 %, 24+ 6 %, 36+ 4 %, 48+ 3 %, 72+ 2 %, 128+ 1 %): +20 C and with 3 % the last pebble
 * depletes one of 25 steps (then it is spent). It loses heat towards ambient (1+dT/24 in the open, 1+dT/96 enclosed) and shares 1/16
 * of the difference with beds beside it. From 1200 C it gets damaged (more often the hotter and smaller the reactor; it recovers
 * 1 in 800 ticks); 100 damage or 4400 C melt it into lava.
 */
public class PebbleBedBlockEntity extends ReactorBlockEntity implements Temperatured, ReactorTyped, ReactorBlock, Feedable, net.minecraft.world.Clearable {

	public static final int MINTEMP = 800;
	public static final int OVERTEMP = 1200;
	public static final int FAILTEMP = 4400;
	public static final int SIZE = 47;
	public static final int ARRANGEMENT_RANGE = 3;

	private final ItemStackHandler inv = new ItemStackHandler(SIZE) {
		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return is.is(ReactorItems.PELLET.get()) || is.is(ReactorItems.OLD_PELLET.get());
		}

		@Override
		protected void onContentsChanged(int slot) {
			PebbleBedBlockEntity.this.setChanged();
		}
	};

	private int damage;
	private int cycleCooldown;
	private int reactorSize = 1;

	public PebbleBedBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.PEBBLE_BED.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Pebble Bed Reactor";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (!world.isClientSide && (this.getTicksExisted()%64 == 1))
			reactorSize = this.computeReactorSize(world, pos);
		if (!world.isClientSide && this.isFissile() && RandomHelper.doWithChance(this.getFissionChance()/100D, rand))
			this.runDecayCycle();
		if (!world.isClientSide)
			this.feed(world, pos);
		if (this.thermalTick() && !world.isClientSide) {
			this.updateTemperature(world, pos);
			if (this.isRemoved())
				return;
		}
		if (damage > 0 && rand.nextInt(800) == 0)
			damage--;
		if (cycleCooldown > 0)
			cycleCooldown--;
	}

	/** Original PebbleBedArrangement: beds within 3 blocks of a member join its reactor (here searched again every 64 ticks). */
	private int computeReactorSize(Level world, BlockPos pos) {
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(pos);
		queue.add(pos);
		int r = ARRANGEMENT_RANGE;
		while (!queue.isEmpty() && seen.size() < 4096) {
			BlockPos p = queue.poll();
			for (BlockPos q : BlockPos.betweenClosed(p.offset(-r, -r, -r), p.offset(r, r, r))) {
				if (!seen.contains(q) && world.isLoaded(q) && world.getBlockEntity(q) instanceof PebbleBedBlockEntity) {
					BlockPos im = q.immutable();
					seen.add(im);
					queue.add(im);
				}
			}
		}
		return seen.size();
	}

	public int getReactorSize() {
		return reactorSize;
	}

	public double getFissionChance() {
		int size = reactorSize;
		if (size >= 128)
			return 1;
		else if (size >= 72)
			return 2;
		else if (size >= 48)
			return 3;
		else if (size >= 36)
			return 4;
		else if (size >= 24)
			return 6;
		else if (size >= 12)
			return 4;
		else if (size >= 6)
			return 2;
		return 1;
	}

	private void runDecayCycle() {
		int slot = -1;
		for (int i = SIZE-1; i >= 0; i--) {
			if (inv.getStackInSlot(i).is(ReactorItems.PELLET.get())) {
				slot = i;
				break;
			}
		}
		if (slot != -1) {
			if (RandomHelper.doWithChance(3, rand))
				inv.setStackInSlot(slot, this.getFissionProduct(inv.getStackInSlot(slot)));
			temperature += 20;
		}
	}

	private ItemStack getFissionProduct(ItemStack is) {
		int d = FuelItem.getDepletion(is);
		if (d >= ReactorItems.PELLET.get().steps-1)
			return new ItemStack(ReactorItems.OLD_PELLET.get());
		ItemStack out = is.copy();
		out.set(ReactorComponents.DEPLETION.get(), d+1);
		return out;
	}

	@Override
	protected void updateTemperature(Level world, BlockPos pos) {
		super.updateTemperature(world, pos);
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		int dT = temperature-Tamb;
		if (dT != 0) {
			int f = isExposedToAir(world, pos) ? 24 : 96;
			temperature -= (1+dT/f);
		}
		if (dT > 0) {
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				if (world.getBlockEntity(pos.relative(dir)) instanceof PebbleBedBlockEntity te) {
					int dTemp = temperature-te.temperature;
					if (dTemp > 0) {
						temperature -= dTemp/16;
						te.temperature += dTemp/16;
					}
				}
			}
		}
		if (temperature >= this.getMaxTemperature()) {
			world.setBlockAndUpdate(pos, Fluids.FLOWING_LAVA.getFlowing(8, false).createLegacyBlock());
		}
		else if (temperature >= OVERTEMP) {
			int chance = 5+(FAILTEMP-temperature)/10/Math.max(1, reactorSize);
			if (rand.nextInt(chance) == 0) {
				world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, 0.5F);
				if (world instanceof ServerLevel sl)
					sl.sendParticles(ParticleTypes.SMOKE, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 9, 0.5625, 0.5625, 0.5625, 0);
				damage++;
				if (damage >= 100)
					this.melt(world, pos);
			}
		}
		this.setChanged();
	}

	private void melt(Level world, BlockPos pos) {
		this.clearContent();
		world.setBlockAndUpdate(pos, Fluids.FLOWING_LAVA.getFlowing(8, false).createLegacyBlock());
		world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 2, 0.1F);
		world.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1, 0.2F);
		if (world instanceof ServerLevel sl)
			sl.sendParticles(ParticleTypes.LAVA, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 12, 0.5625, 0.5625, 0.5625, 0);
	}

	public boolean isFissile() {
		for (int i = 0; i < SIZE; i++)
			if (inv.getStackInSlot(i).is(ReactorItems.PELLET.get()))
				return true;
		return false;
	}

	private void feed(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos.below()) instanceof PebbleBedBlockEntity tile) {
			if (tile.feedIn(inv.getStackInSlot(SIZE-1))) {
				for (int i = SIZE-1; i > 0; i--)
					inv.setStackInSlot(i, inv.getStackInSlot(i-1));
				if (world.getBlockEntity(pos.above()) instanceof PebbleBedBlockEntity up)
					inv.setStackInSlot(0, up.feedOut());
				else
					inv.setStackInSlot(0, ItemStack.EMPTY);
			}
		}
		this.collapseInventory();
	}

	/** Original collapseInventory: one item moves one slot on per tick. */
	private void collapseInventory() {
		for (int k = SIZE-1; k > 0; k--) {
			if (inv.getStackInSlot(k).isEmpty() && !inv.getStackInSlot(k-1).isEmpty()) {
				inv.setStackInSlot(k, inv.getStackInSlot(k-1));
				inv.setStackInSlot(k-1, ItemStack.EMPTY);
				return;
			}
		}
	}

	@Override
	public boolean feedIn(ItemStack is) {
		if (is.isEmpty())
			return true;
		if (!inv.isItemValid(0, is))
			return false;
		if (inv.getStackInSlot(0).isEmpty()) {
			inv.setStackInSlot(0, is.copy());
			return true;
		}
		return false;
	}

	@Override
	public ItemStack feedOut() {
		ItemStack is = inv.getStackInSlot(SIZE-1).copy();
		inv.setStackInSlot(SIZE-1, ItemStack.EMPTY);
		return is;
	}

	private boolean canRemoveItem(int slot, ItemStack is) {
		if (is.is(ReactorItems.OLD_PELLET.get()))
			return true;
		if (slot == 0 && cycleCooldown == 0 && this.getTicksExisted()%80 < 40) {
			cycleCooldown = 10;
			return true;
		}
		return false;
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Pebbles go in at the top and come out at the bottom. */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return SIZE;
			}

			@Override
			public ItemStack getStackInSlot(int slot) {
				return inv.getStackInSlot(slot);
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return side == Direction.UP || side == null ? inv.insertItem(slot, stack, simulate) : stack;
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				if (!(side == Direction.DOWN || side == null) || inv.getStackInSlot(slot).isEmpty())
					return ItemStack.EMPTY;
				if (simulate)
					return inv.getStackInSlot(slot).is(ReactorItems.OLD_PELLET.get()) || slot == 0 && cycleCooldown == 0 && PebbleBedBlockEntity.this.getTicksExisted()%80 < 40
							? inv.extractItem(slot, amount, true) : ItemStack.EMPTY;
				if (!PebbleBedBlockEntity.this.canRemoveItem(slot, inv.getStackInSlot(slot)))
					return ItemStack.EMPTY;
				return inv.extractItem(slot, amount, false);
			}

			@Override
			public int getSlotLimit(int slot) {
				return 1;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return inv.isItemValid(slot, stack);
			}
		};
	}

	@Override
	public int getMaxTemperature() {
		return FAILTEMP;
	}

	@Override
	public boolean canDumpHeatInto(LiquidStates liq) {
		return false;
	}

	@Override
	public ReactorType getReactorType() {
		return ReactorType.HTGR;
	}

	public int getDamage() {
		return damage;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < SIZE; i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < SIZE; i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
		NBT.putInt("dmg", damage);
		NBT.putInt("size", reactorSize);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
		damage = NBT.getInt("dmg");
		reactorSize = Math.max(1, NBT.getInt("size"));
	}
}
