/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.reactorcraft.ReactorConfig;
import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.content.entity.NeutronEntity.NeutronType;
import reika.reactorcraft.radiation.RadiationEffects;
import reika.reactorcraft.radiation.RadiationIntensity;
import reika.reactorcraft.radiation.WasteManager;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.lib.math.RandomHelper;

/**
 * Original: Reika.ReactorCraft.Base.TileEntityNuclearCore - 4 fuel slots (a column: slot 3 is the active one) and 8 waste slots, one
 * item each. Fuel moves down a stack of cores (Feedable) and waste moves to the core below. A fissile core sends decay neutrons
 * (1/20 per tick, horizontally). Neutrons keep it active for 2 minutes (and, by option, its 3x3 chunks loaded). Heat flows to the
 * same cores beside it (1/16 of the difference); above 500 C it smokes, above 800 C (cladding) more, above 1400 C hydrogen builds up
 * (explosion past 200), above 1800 C it melts down: same cores within 2 turn to corium, an explosion (8), lethal contamination
 * (radius 32) and a hydrogen explosion (7).
 */
public abstract class NuclearCoreBlockEntity extends ReactorBlockEntity implements LinkableReactorCore, ReactorTyped, NeutronTile, Feedable, ReactorBlock, net.minecraft.world.Clearable {

	public static final int CLADDING = 800;
	public static final int HYDROGEN = 1400;
	public static final int MELTDOWN = 1800;
	private static final int MAX_HYDROGEN = 200;

	protected int hydrogen;
	private int activeTimer;

	protected final ItemStackHandler inv = new ItemStackHandler(12) {
		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return NuclearCoreBlockEntity.this.isItemValidForSlot(slot, is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			NuclearCoreBlockEntity.this.setChanged();
		}
	};

	protected NuclearCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public abstract boolean isFissile();

	protected abstract boolean isItemValidForSlot(int slot, ItemStack is);

	protected abstract boolean canRemoveItem(int slot, ItemStack is);

	protected int getDecayNeutronChance() {
		return 20;
	}

	protected int getWarningTemperature() {
		return 500;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (!world.isClientSide && this.isFissile() && rand.nextInt(this.getDecayNeutronChance()) == 0)
			world.addFreshEntity(new NeutronEntity(world, pos, this.getRandomDirection(false), NeutronType.DECAY));
		if (!world.isClientSide) {
			this.feedWaste(world, pos);
			this.feed(world, pos);
		}
		if (activeTimer > 0) {
			activeTimer--;
			if (activeTimer == 0)
				this.onActivityChange(false);
		}
		if (this.thermalTick() && !world.isClientSide)
			this.updateTemperature(world, pos);
		if (world instanceof ServerLevel sl) {
			if (temperature > CLADDING) {
				if (rand.nextInt(20) == 0)
					world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
				sl.sendParticles(ParticleTypes.SMOKE, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 9, 0.5625, 0.5625, 0.5625, 0);
			}
			else if (temperature > this.getWarningTemperature() && rand.nextInt(5) == 0) {
				if (rand.nextInt(20) == 0)
					world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
				sl.sendParticles(ParticleTypes.SMOKE, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 4, 0.5625, 0.5625, 0.5625, 0);
			}
		}
	}

	private void onActivityChange(boolean active) {
		if (level instanceof ServerLevel sl && ReactorConfig.CHUNKLOADING.get())
			reika.reactorcraft.reactor.ReactorChunkLoading.setLoaded(sl, worldPosition, active);
		this.syncAllData();
	}

	/** Original feedWaste: waste slots move into the free waste slots of the core below. */
	private void feedWaste(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos.below()) instanceof NuclearCoreBlockEntity te) {
			for (int i = 4; i < 12; i++) {
				if (!inv.getStackInSlot(i).isEmpty()) {
					for (int k = 4; k < 12; k++) {
						if (te.inv.getStackInSlot(k).isEmpty()) {
							te.inv.setStackInSlot(k, inv.getStackInSlot(i));
							inv.setStackInSlot(i, ItemStack.EMPTY);
						}
					}
				}
			}
		}
	}

	/** Original feed: the bottom fuel goes into the core below, the column shifts down and takes the bottom fuel of the core above. */
	private void feed(Level world, BlockPos pos) {
		BlockEntity tile = world.getBlockEntity(pos.below());
		if (tile instanceof Feedable f) {
			if (f.feedIn(inv.getStackInSlot(3))) {
				inv.setStackInSlot(3, inv.getStackInSlot(2));
				inv.setStackInSlot(2, inv.getStackInSlot(1));
				inv.setStackInSlot(1, inv.getStackInSlot(0));
				BlockEntity up = world.getBlockEntity(pos.above());
				inv.setStackInSlot(0, up instanceof Feedable fu ? fu.feedOut() : ItemStack.EMPTY);
			}
		}
		this.collapseInventory();
	}

	private void collapseInventory() {
		for (int i = 0; i < 4; i++) {
			for (int k = 3; k > 0; k--) {
				if (inv.getStackInSlot(k).isEmpty() && !inv.getStackInSlot(k-1).isEmpty()) {
					inv.setStackInSlot(k, inv.getStackInSlot(k-1));
					inv.setStackInSlot(k-1, ItemStack.EMPTY);
					return;
				}
			}
		}
	}

	@Override
	public boolean feedIn(ItemStack is) {
		if (is.isEmpty())
			return true;
		if (!this.isItemValidForSlot(0, is))
			return false;
		if (inv.getStackInSlot(0).isEmpty()) {
			inv.setStackInSlot(0, is.copy());
			return true;
		}
		return false;
	}

	@Override
	public ItemStack feedOut() {
		ItemStack is = inv.getStackInSlot(3);
		if (is.isEmpty())
			return ItemStack.EMPTY;
		inv.setStackInSlot(3, ItemStack.EMPTY);
		return is.copy();
	}

	protected final void tryPushSpentFuel(int slot) {
		for (int i = 4; i < 12; i++) {
			if (inv.getStackInSlot(i).isEmpty()) {
				inv.setStackInSlot(i, inv.getStackInSlot(slot));
				inv.setStackInSlot(slot, ItemStack.EMPTY);
				return;
			}
		}
	}

	/** Original checkPoisonedChance: each waste item in the core makes a neutron more likely to be lost (1/(9-n)). */
	protected boolean checkPoisonedChance() {
		int count = 0;
		for (int i = 4; i < 12; i++)
			if (inv.getStackInSlot(i).is(ReactorItems.WASTE.get()))
				count++;
		return rand.nextInt(9-count) == 0;
	}

	protected void addWaste() {
		ItemStack waste = WasteManager.getRandomWasteItem(level.random);
		for (int i = 4; i < 12; i++) {
			ItemStack inslot = inv.getStackInSlot(i);
			if (inslot.isEmpty()) {
				inv.setStackInSlot(i, waste);
				return;
			}
			//original: the same isotope stacks in its slot up to the item's stack size (beyond the inventory limit of 1)
			if (ItemStack.isSameItemSameComponents(waste, inslot) && inslot.getCount()+waste.getCount() <= waste.getMaxStackSize()) {
				inv.setStackInSlot(i, inslot.copyWithCount(inslot.getCount()+waste.getCount()));
				return;
			}
		}
	}

	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		boolean inactive = activeTimer <= 0;
		activeTimer = 2400;
		if (inactive)
			this.onActivityChange(true);
		return false;
	}

	public final boolean isActive() {
		return activeTimer > 0;
	}

	protected final void spawnNeutronBurst(Level world, BlockPos pos) {
		if (world.isClientSide)
			return;
		NeutronType n = this.getReactorType().getNeutronType();
		if (n == null)
			return;
		for (int i = 0; i < 3; i++)
			world.addFreshEntity(new NeutronEntity(world, pos, this.getRandomDirection(ReactorConfig.VERTNEUTRONS.get()), n));
	}

	@Override
	public int getMaxTemperature() {
		return MELTDOWN;
	}

	protected void onMeltdown(Level world, BlockPos pos) {
		if (world.isClientSide)
			return;
		int r = 2;
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r)))
			if (world.getBlockState(p).is(this.getBlockState().getBlock()))
				world.setBlockAndUpdate(p, ReactorBlocks.CORIUM.get().defaultBlockState());
		world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 8, false, Level.ExplosionInteraction.BLOCK);
		RadiationEffects.contaminateArea(world, pos, 32, 8, 2, true, RadiationIntensity.LETHAL);
		HydrogenExplosion.explode((ServerLevel)world, pos, 7);
	}

	@Override
	protected void updateTemperature(Level world, BlockPos pos) {
		super.updateTemperature(world, pos);
		int Tamb = this.getRestingTemperature(world, pos);
		int dT = temperature-Tamb;
		if (dT != 0) {
			int d = isExposedToAir(world, pos) ? 32 : 64;
			d = this.getAmbientHeatLossFactor(d, Tamb);
			temperature -= (1+dT/d);
		}
		if (dT > 0) {
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				if (world.getBlockEntity(pos.relative(dir)) instanceof NuclearCoreBlockEntity te && te.getType() == this.getType()) {
					int dTemp = temperature-te.temperature;
					if (dTemp > 0) {
						int d = 16;
						temperature -= dTemp/d;
						te.temperature += dTemp/d*this.getHeatConductionEfficiency(te);
					}
				}
			}
		}
		if (temperature > MELTDOWN) {
			this.onMeltdown(world, pos);
			return;
		}
		if (temperature > HYDROGEN) {
			hydrogen++;
			if (hydrogen > MAX_HYDROGEN) {
				HydrogenExplosion.explode((ServerLevel)world, pos, 7);
				hydrogen = 0;
			}
		}
		else if (hydrogen > 0) {
			hydrogen--;
		}
		this.setChanged();
	}

	/** Original getRestingTemperature: the temperature the core falls back to (the ambient temperature). */
	protected int getRestingTemperature(Level world, BlockPos pos) {
		return reika.rotarycraft.lib.world.AmbientTemperature.getAmbientTemperatureAt(world, pos);
	}

	/** Original getAmbientHeatLossFactor. */
	protected int getAmbientHeatLossFactor(int base, int Tamb) {
		return base;
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Original canItemEnterFromSide (top) and canItemExitToSide (bottom; only waste and spent fuel). */
	@Nullable
	public IItemHandler getItemHandler(@Nullable Direction side) {
		boolean in = side == Direction.UP, out = side == Direction.DOWN;
		if (side != null && !in && !out)
			return null;
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 12;
			}

			@Override
			public ItemStack getStackInSlot(int slot) {
				return inv.getStackInSlot(slot);
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return in || side == null ? inv.insertItem(slot, stack, simulate) : stack;
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				if (!(out || side == null) || !NuclearCoreBlockEntity.this.canRemoveItem(slot, inv.getStackInSlot(slot)))
					return ItemStack.EMPTY;
				return inv.extractItem(slot, amount, simulate);
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

	/** Original CPU coordinate set by the CPU remote control. */
	@javax.annotation.Nullable
	private BlockPos cpu;

	@Override
	public void link(CpuBlockEntity te) {
		cpu = te.getBlockPos().immutable();
		this.setChanged();
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		if (world instanceof ServerLevel sl && this.isActive())
			ReactorChunkLoading.setLoaded(sl, pos, false);
		if (cpu != null && world.getBlockEntity(cpu) instanceof CpuBlockEntity te)
			te.removeTemperatureCheck(this);
		for (int i = 0; i < inv.getSlots(); i++)
			net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		if (cpu != null)
			NBT.putLong("cpu", cpu.asLong());
		NBT.putInt("h2", hydrogen);
		NBT.putInt("activetick", activeTimer);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		cpu = NBT.contains("cpu") ? BlockPos.of(NBT.getLong("cpu")) : null;
		hydrogen = NBT.getInt("h2");
		activeTimer = NBT.getInt("activetick");
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}

	protected static boolean chance(double c, net.minecraft.util.RandomSource r) {
		return RandomHelper.doWithChance(c, r);
	}
}
