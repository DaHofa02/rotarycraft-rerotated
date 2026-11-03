/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.content.recipe.GrinderRecipe;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.DifficultyEffects;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotaryRecipeTypes;

/**
 * Original: Reika.RotaryCraft.TileEntities.Processing.TileEntityGrinder. Grinds one item per operation into its
 * {@code rotarycraft:grinder} result (min. 128 Nm, 4096 W; time max(1, 840 - 60*log2(omega+1)) ticks, several operations per tick
 * above 32768 rad/s). Canola seeds also give lubricant (DifficultyEffects.CANOLA, random per seed) into a 4000 mB tank, which
 * fills buckets in slot 2 and flows out to hoses on every side but the top. Slots: 0 input, 1 output, 2 bucket.
 * Not ported: machine enchantments (fortune for more lubricant, looting etc. for the contact damage), other mods' seeds (creepvine).
 */
public class GrinderBlockEntity extends PowerReceiverBlockEntity implements PipeConnector, net.minecraft.world.Clearable {

	public static final int MAXLUBE = 4000;
	/** Original DurationRegistry.GRINDER. */
	private static final int DURATION_BASE = 840, DURATION_SCALE = 60;

	public int grinderCookTime;
	public boolean idle = false;

	private final FluidTank tank = new FluidTank(MAXLUBE, fs -> fs.is(RotaryFluids.LUBRICANT.get()));

	private final ItemStackHandler inv = new ItemStackHandler(3) {
		@Override
		protected void onContentsChanged(int slot) {
			GrinderBlockEntity.this.setChanged();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return GrinderBlockEntity.this.isItemValidForSlot(slot, stack);
		}
	};

	public GrinderBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.GRINDER.get(), pos, state, 128, 1, 4096);
	}

	@Override
	public String getName() {
		return "Grinder";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Original grindableSeeds: canola seeds with factor 1 (other mods' seeds are not ported). */
	public static float getSeedFactor(ItemStack is) {
		return is.is(RotaryItems.CANOLA_SEEDS.get()) ? 1F : 0F;
	}

	public static boolean isGrindableSeed(ItemStack is) {
		return getSeedFactor(is) > 0;
	}

	private Optional<RecipeHolder<GrinderRecipe>> getRecipe(ItemStack is) {
		if (level == null || is.isEmpty())
			return Optional.empty();
		return level.getRecipeManager().getRecipeFor(RotaryRecipeTypes.GRINDER.get(), new SingleRecipeInput(is), level);
	}

	public ItemStack getGrindingResult(ItemStack is) {
		return this.getRecipe(is).map(r -> r.value().assemble(new SingleRecipeInput(is), level.registryAccess())).orElse(ItemStack.EMPTY);
	}

	public void testIdle() {
		idle = !this.canGrind();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.testIdle();
		tickcount++;
		this.readPower();
		if (power < MINPOWER || torque < MINTORQUE) {
			grinderCookTime = 0;
			return;
		}
		int n = this.getNumberConsecutiveOperations();
		boolean flag1 = false;
		for (int i = 0; i < n; i++)
			flag1 |= this.doOperation(n > 1);
		if (flag1)
			this.setChanged();
		ItemStack b = inv.getStackInSlot(2);
		if (!b.isEmpty() && tank.getFluidAmount() >= 1000) {
			if (b.is(Items.BUCKET) && b.getCount() == 1) {
				inv.setStackInSlot(2, new ItemStack(RotaryItems.LUBRICANT_BUCKET.get()));
				tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
			}
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (power < MINPOWER || torque < MINTORQUE)
			return;
		phi += 0.85F*(float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	private boolean doOperation(boolean multiple) {
		if (this.canGrind()) {
			grinderCookTime++;
			if (multiple || grinderCookTime >= this.getOperationTime()) {
				grinderCookTime = 0;
				tickcount = 0;
				this.grind();
			}
			return true;
		}
		else {
			grinderCookTime = 0;
			return false;
		}
	}

	private boolean canGrind() {
		ItemStack in = inv.getStackInSlot(0);
		if (in.isEmpty())
			return false;
		boolean flag = false;
		if (isGrindableSeed(in)) {
			flag = true;
			if (MAXLUBE-tank.getFluidAmount() < DifficultyEffects.CANOLA.getAverageAmount())
				return false;
		}
		ItemStack out = this.getGrindingResult(in);
		if (flag && out.isEmpty())
			return true;
		if (out.isEmpty())
			return false;
		ItemStack has = inv.getStackInSlot(1);
		if (has.isEmpty())
			return true;
		if (!ItemStack.isSameItemSameComponents(has, out))
			return false;
		return has.getCount()+out.getCount() <= Math.min(64, out.getMaxStackSize());
	}

	private void grind() {
		ItemStack is = inv.getStackInSlot(0);
		if (isGrindableSeed(is)) {
			float num = getSeedFactor(is);
			tank.fill(new FluidStack(RotaryFluids.LUBRICANT.get(), (int)(DifficultyEffects.CANOLA.getInt()*num)), IFluidHandler.FluidAction.EXECUTE);
		}
		ItemStack out = this.getGrindingResult(is);
		if (!out.isEmpty()) {
			ItemStack has = inv.getStackInSlot(1);
			if (has.isEmpty())
				inv.setStackInSlot(1, out.copy());
			else if (has.getItem() == out.getItem())
				inv.setStackInSlot(1, has.copyWithCount(has.getCount()+out.getCount()));
		}
		inv.extractItem(0, 1, false);
	}

	public int getCookProgressScaled(int par1) {
		return (grinderCookTime * par1)/2 / this.getOperationTime();
	}

	public int getLubricantScaled(int par1) {
		return tank.getFluidAmount()*par1/MAXLUBE;
	}

	/** Original DurationRegistry.GRINDER.getOperationTime: max(1, 840 - 60*log2(omega+1)). */
	public int getOperationTime() {
		return (int)Math.max(1, DURATION_BASE-DURATION_SCALE*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	/** Original DurationFormula.getOverrun: above 2^(1+840/60) rad/s, several operations per tick. */
	public int getNumberConsecutiveOperations() {
		int threshold = (int)ReikaMathLibrary.intpow(2, 1+DURATION_BASE/DURATION_SCALE);
		return 1+(omega >= threshold ? (int)(ReikaMathLibrary.logbase(omega, 2)-DURATION_BASE/DURATION_SCALE) : 0);
	}

	public boolean isItemValidForSlot(int slot, ItemStack is) {
		if (slot == 1)
			return false;
		if (slot == 2)
			return is.is(Items.BUCKET);
		return is.is(RotaryItems.CANOLA_SEEDS.get()) || is.is(RotaryItems.DENSE_CANOLA_SEEDS.get()) || is.is(RotaryItems.CANOLA_HUSKS.get())
				|| !this.getGrindingResult(is).isEmpty();
	}

	@Override
	public int getRedstoneOverride() {
		return this.canGrind() ? 0 : 15;
	}

	/** Original DamagingContact: 3 damage while powered. */
	public int getContactDamage() {
		return 3;
	}

	public boolean canDealDamage() {
		return power >= MINPOWER && torque >= MINTORQUE;
	}

	public int getLubricant() {
		return tank.getFluidAmount();
	}

	public void setLubricant(int amt) {
		tank.setFluid(amt > 0 ? new FluidStack(RotaryFluids.LUBRICANT.get(), amt) : FluidStack.EMPTY);
	}

	@Override
	public boolean canConnectToPipe(PipeType m) {
		return m == PipeType.HOSE || m == PipeType.BEDROCK_PIPE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side != Direction.DOWN;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side != Direction.UP ? Flow.OUTPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	/** Original canDrain: from every side but the top. */
	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from == Direction.UP)
			return FluidStack.EMPTY;
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
				return MAXLUBE;
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
				return resource.is(RotaryFluids.LUBRICANT.get()) ? GrinderBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return GrinderBlockEntity.this.drain(side, maxDrain, action);
			}
		};
	}

	/** Original ISidedInventory: insertion by isItemValidForSlot, extraction from the output only. */
	public IItemHandler getAutomationHandler() {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 3;
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
				return GrinderBlockEntity.this.isItemValidForSlot(slot, stack);
			}
		};
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putShort("CookTime", (short)grinderCookTime);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		grinderCookTime = NBT.getShort("CookTime");
		tank.readFromNBT(registries, NBT.getCompound("tank"));
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	public void onEMP() {}
}
