/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.api.interfaces.RefrigeratorAttachment;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryDamage;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Production.TileEntityRefrigerator - turns ice (slot 0) into liquid nitrogen. Powered from the
 * side its metadata names (min. 2048 Nm, 32768 W); one cycle takes max(1, 1000 - 80*log2(omega+1)) ticks (more cycles per tick past
 * 2^13 rad/s), uses one ice and makes min(2000, 100*(torque/2048)^2) mB of LN2 into its 12000 mB tank, and one time in four 1 (often),
 * 2 or 4 dry ice into slot 1. Attachments (the ReactorCraft gas collector) are told of every cycle. The LN2 is drained from any side;
 * only the dry ice comes out of the item slots. Breaking it with more than 10 % in the tank hurts everything within 5 blocks
 * (4 hearts at full) with a fizz and a cold cloud.
 */
public class RefrigeratorBlockEntity extends PowerReceiverBlockEntity implements PipeConnector, net.minecraft.world.Clearable {

	public static final int CAPACITY = 12000;
	/** Original DurationRegistry.REFRIGERATOR. */
	private static final int DURATION_BASE = 1000, DURATION_SCALE = 80;

	public int time;
	private int timer;
	private int soundTimer;

	private final RefrigeratorAttachment[] attachments = new RefrigeratorAttachment[6];

	private final FluidTank tank = new FluidTank(CAPACITY, fs -> fs.is(RotaryFluids.LIQUID_NITROGEN.get())) {
		@Override
		protected void onContentsChanged() {
			RefrigeratorBlockEntity.this.setChanged();
		}
	};

	private final ItemStackHandler inv = new ItemStackHandler(2) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return slot == 0 && is.is(Items.ICE);
		}

		@Override
		protected void onContentsChanged(int slot) {
			RefrigeratorBlockEntity.this.setChanged();
		}
	};

	public RefrigeratorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.REFRIGERATOR.get(), pos, state, 2048, 1, 32768);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public FluidTank getTank() {
		return tank;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = switch (this.getBlockMetadata()) {
			case 0 -> Direction.EAST;
			case 1 -> Direction.WEST;
			case 2 -> Direction.SOUTH;
			default -> Direction.NORTH;
		};
		if (world.isClientSide)
			return;
		this.readPower();
		if (this.canProgress()) {
			soundTimer++;
			if (soundTimer >= 20) {
				soundTimer = 0;
				RotarySounds.play(world, pos, RotarySounds.FRIDGE.get(), MachineSounds.isMuffled(world, pos) ? 0.25F : 1, 0.88F);
			}
		}
		else {
			soundTimer = 0;
		}
		int n = this.getNumberConsecutiveOperations();
		for (int i = 0; i < n; i++)
			this.doOperation(n > 1);
		if (time != timer) {
			time = timer;
			if (timer%20 == 0)
				this.syncAllData();
		}
	}

	private void doOperation(boolean multiple) {
		if (this.canProgress()) {
			timer++;
			if (multiple || timer >= this.getOperationTime()) {
				timer = 0;
				this.cycle();
			}
		}
		else {
			timer = 0;
		}
	}

	private boolean canProgress() {
		if (power < MINPOWER || torque < MINTORQUE)
			return false;
		if (!inv.getStackInSlot(0).is(Items.ICE))
			return false;
		if (tank.getFluidAmount()+this.getProducedLN2() > CAPACITY)
			return false;
		ItemStack out = inv.getStackInSlot(1);
		return out.isEmpty() || out.getCount() < out.getMaxStackSize();
	}

	private void cycle() {
		inv.getStackInSlot(0).shrink(1);
		int amt = this.getProducedLN2();
		tank.fill(new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), amt), IFluidHandler.FluidAction.EXECUTE);
		if (amt > 0) {
			for (RefrigeratorAttachment r : attachments)
				if (r != null)
					r.onCompleteCycle(amt);
			if (rand.nextInt(4) == 0) {
				int n = rand.nextInt(20) == 0 ? 4 : (rand.nextInt(4) == 0 ? 2 : 1);
				ItemStack has = inv.getStackInSlot(1);
				ItemStack dry = new ItemStack(RotaryItems.part(CraftingPart.DRYICE));
				if (!has.isEmpty())
					n = Math.min(n, dry.getMaxStackSize()-has.getCount());
				if (has.isEmpty())
					inv.setStackInSlot(1, dry.copyWithCount(n));
				else if (ItemStack.isSameItemSameComponents(has, dry))
					has.grow(n);
			}
		}
		this.syncAllData();
	}

	public int getProducedLN2() {
		int over = torque/MINTORQUE;
		return Math.min(2000, 100*over*over);
	}

	public int getProgressScaled(int a) {
		return time*a/this.getOperationTime();
	}

	public int getLiquidScaled(int a) {
		return tank.getFluidAmount()*a/CAPACITY;
	}

	public int getOperationTime() {
		return (int)Math.max(1, DURATION_BASE-DURATION_SCALE*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	public int getNumberConsecutiveOperations() {
		int threshold = (int)ReikaMathLibrary.intpow(2, 1+DURATION_BASE/DURATION_SCALE);
		return 1+(omega >= threshold ? (int)(ReikaMathLibrary.logbase(omega, 2)-DURATION_BASE/DURATION_SCALE) : 0);
	}

	public void addAttachment(RefrigeratorAttachment te, Direction dir) {
		attachments[dir.ordinal()] = te;
	}

	/** Original onPlacedNextToThis: a block placed beside it clears that side's attachment until it registers itself again. */
	public void clearAttachment(Direction dir) {
		attachments[dir.ordinal()] = null;
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return Flow.OUTPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		FluidStack fs = tank.drain(maxDrain, action);
		if (!fs.isEmpty() && action.execute())
			this.setChanged();
		return fs;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 1;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return tank.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return false;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return 0;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return resource.is(RotaryFluids.LIQUID_NITROGEN.get()) ? RefrigeratorBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return RefrigeratorBlockEntity.this.drain(side, max, action);
			}
		};
	}

	/** Ice in (slot 0) from any side; only the dry ice (slot 1) comes out (original canExtractItem). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 2;
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
				return slot == 1 ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
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

	/** Original breakBlock (BreakAction) plus the FRIDGEBREAK packet's cloud (EntityBlurFX 0xBFB2FF; here coloured dust). */
	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
		float f = tank.getFluidAmount()/(float)CAPACITY;
		if (f > 0.1) {
			world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.2F, 0.8F);
			float hearts = f*4;
			for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(5)))
				e.hurt(RotaryDamage.source(world, RotaryDamage.FREEZE), hearts*2);
			if (world instanceof ServerLevel sl)
				sl.sendParticles(new DustParticleOptions(new Vector3f(0xBF/255F, 0xB2/255F, 1F), 3F), pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5,
						15, 0.5, 0.25, 0.5, 0.04);
		}
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("timer", time);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		time = NBT.getInt("timer");
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
