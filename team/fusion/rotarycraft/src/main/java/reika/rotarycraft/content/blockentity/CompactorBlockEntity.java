/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientPressure;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.lib.world.TemperatureEnvironment;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Processing.TileEntityCompactor - presses four equal items (slots 0-3) into the result (slot
 * 4) once its pressure and temperature reach the recipe's (see {@link CompactorRecipes}). Powered from the side its metadata names (at
 * least 4096 Nm and 262144 W). Every second the pressure moves towards the ambient one (down by 1/200 of the excess, 1/600 in the
 * nether; up by 1/40) and, while turning, rises by 128*log2(torque); above 600000 kPa it explodes. The temperature evens out with
 * the surroundings likewise unless a friction heater just heated it, gains 4 beside lava, 2 beside fire, 1 in the nether, is cooled
 * by water (above 600 C, may boil it away), ice and snow (which may melt); above 1000 C it bursts. One operation takes
 * max(1, s*300 - s*15*log2(omega+1)) ticks for stage s (coal 1, anthracite 2, prismane 3, lonsdaleite 4).
 */
public class CompactorBlockEntity extends PowerReceiverBlockEntity implements FrictionHeatable, net.minecraft.world.Clearable {

	public static final int MAXTEMP = 1000;
	public static final int MAXPRESSURE = 600000; //All pressures in kPa, steel yield strength = 250Mpa
	public static final int REQTEMP = 800; //real temp/2
	public static final int REQPRESS = 550000; //real pressure

	public int compactorCookTime;
	private int pressure;
	public int temperature;

	public boolean idle = false;
	private boolean animdir = false;

	private int envirotick = 0;
	private int tempTick;

	private final ItemStackHandler inv = new ItemStackHandler(5) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return CompactorBlockEntity.this.isItemValidForSlot(slot, is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			CompactorBlockEntity.this.setChanged();
		}
	};

	public CompactorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.COMPACTOR.get(), pos, state, 4096, 1, 262144);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public void testIdle() {
		boolean ingred = false;
		boolean invalid = false;
		for (int i = 0; i < 4; i++)
			if (inv.getStackInSlot(i).isEmpty())
				invalid = true;
		if (!invalid) {
			ItemStack a = inv.getStackInSlot(0);
			for (int i = 1; i < 4; i++)
				if (!ItemStack.isSameItemSameComponents(a, inv.getStackInSlot(i)))
					invalid = true;
		}
		if (!invalid && CompactorRecipes.isCompactable(inv.getStackInSlot(0)))
			ingred = true;
		ItemStack out = inv.getStackInSlot(4);
		boolean full = !out.isEmpty() && out.getCount() >= out.getMaxStackSize();
		idle = !ingred || full;
	}

	private void getIOSides(int metadata) {
		read = switch (metadata) {
			case 0 -> Direction.EAST;
			case 1 -> Direction.WEST;
			case 2 -> Direction.SOUTH;
			default -> Direction.NORTH;
		};
	}

	public int getCookProgressScaled(int par1) {
		int time = this.getOperationTime();
		return time > 0 ? (compactorCookTime*par1)/time : 0;
	}

	public int getPressureScaled(int par1) {
		return (pressure*par1)/MAXPRESSURE;
	}

	public int getTemperatureScaled(int par1) {
		return (temperature*par1)/MAXTEMP;
	}

	public void updatePressure(Level world, BlockPos pos) {
		int Pamb = (int)AmbientPressure.getAmbientPressureAt(world, pos);
		boolean hell = world.dimension() == Level.NETHER;
		if (pressure > Pamb && !hell)
			pressure -= Math.max((pressure-Pamb)/200, 1);
		if (pressure > Pamb && hell)
			pressure -= Math.max((pressure-Pamb)/600, 1);
		if (pressure < Pamb)
			pressure += Math.max((Pamb-pressure)/40, 1);
		if (omega > 0)
			pressure += 128*ReikaMathLibrary.logbase(torque, 2);
		if (pressure > MAXPRESSURE)
			this.overpressure(world, pos);
	}

	private boolean isNextTo(Level world, BlockPos pos, java.util.function.Predicate<BlockPos> test) {
		for (Direction d : Direction.values())
			if (test.test(pos.relative(d)))
				return true;
		return false;
	}

	@Nullable
	private Direction adjacent(Level world, BlockPos pos, java.util.function.Predicate<BlockPos> test) {
		for (Direction d : Direction.values())
			if (test.test(pos.relative(d)))
				return d;
		return null;
	}

	public void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (temperature > Tamb)
			temperature -= Math.max((temperature-Tamb)/200, 1);
		if (temperature < Tamb)
			temperature += Math.max((Tamb-temperature)/40, 1);

		if (this.isNextTo(world, pos, p -> world.getFluidState(p).is(FluidTags.LAVA)))
			temperature += 4;
		if (this.isNextTo(world, pos, p -> world.getBlockState(p).is(Blocks.FIRE)))
			temperature += 2;
		if (Tamb == 300) //Fire is 50% hotter in the nether
			temperature++;

		Direction a = this.adjacent(world, pos, p -> world.getFluidState(p).is(FluidTags.WATER));
		if (a != null && temperature > 600) {
			temperature--;
			if (rand.nextInt(4000) == 0)
				world.setBlockAndUpdate(pos.relative(a), Blocks.AIR.defaultBlockState());
		}
		Direction iceside = this.adjacent(world, pos, p -> world.getBlockState(p).is(Blocks.ICE));
		if (iceside != null && temperature > 0) {
			temperature -= 2;
			if (rand.nextInt(200) == 0)
				world.setBlockAndUpdate(pos.relative(iceside), Blocks.WATER.defaultBlockState());
		}
		Direction snowside = this.adjacent(world, pos, p -> world.getBlockState(p).is(Blocks.SNOW_BLOCK));
		if (snowside != null && temperature > -5) {
			temperature -= 2;
			if (rand.nextInt(100) == 0 && iceside != null) //original: melts the ice side (none: nothing)
				world.setBlockAndUpdate(pos.relative(iceside), Blocks.WATER.defaultBlockState());
		}
		TemperatureEnvironment.temperatureEnvironment(world, pos, temperature);

		if (temperature > MAXTEMP) {
			this.overheat(world, pos);
			temperature = MAXTEMP;
		}
	}

	/** Original overheat: ReikaWorldHelper.overheat - gone with a strength 2 explosion and up to 17 scrap. */
	public void overheat(Level world, BlockPos pos) {
		temperature = MAXTEMP;
		world.removeBlock(pos, false);
		world.explode(null, pos.getX(), pos.getY(), pos.getZ(), 2F, false, RotaryConfig.BLOCKDAMAGE.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
		int n = rand.nextInt(18);
		for (int i = 0; i < n; i++) {
			ItemEntity ent = new ItemEntity(world, pos.getX()+rand.nextFloat(), pos.getY()+0.5, pos.getZ()+rand.nextFloat(), new ItemStack(RotaryItems.part(CraftingPart.SCRAP)));
			ent.setDeltaMovement(-0.2+0.4*rand.nextFloat(), 0.5*rand.nextFloat(), -0.2+0.4*rand.nextFloat());
			world.addFreshEntity(ent);
		}
	}

	public void overpressure(Level world, BlockPos pos) {
		world.explode(null, pos.getX(), pos.getY(), pos.getZ(), 4, RotaryConfig.BLOCKDAMAGE.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
		pressure = MAXPRESSURE;
	}

	public int getStage() {
		ItemStack is = inv.getStackInSlot(0);
		if (is.isEmpty() || !CompactorRecipes.isCompactable(is) || is.is(Items.COAL))
			return 1;
		if (is.is(RotaryItems.part(CraftingPart.ANTHRACITE)))
			return 2;
		if (is.is(RotaryItems.part(CraftingPart.PRISMANE)))
			return 3;
		if (is.is(RotaryItems.part(CraftingPart.LONSDA)))
			return 4;
		return 1;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		if (envirotick >= 20) {
			this.updatePressure(world, pos);
			if (tempTick == 0)
				this.updateTemperature(world, pos);
			envirotick = 0;
			if (world.getBlockEntity(pos) != this)
				return;
		}
		this.testIdle();
		envirotick++;
		if (tempTick > 0)
			tempTick--;
		boolean flag1 = false;
		int n = this.getNumberConsecutiveOperations();
		for (int i = 0; i < n; i++)
			flag1 |= this.doOperation();
		if (flag1)
			this.setChanged();
	}

	private boolean doOperation() {
		if (this.canSmelt()) {
			compactorCookTime++;
			if (compactorCookTime >= this.getOperationTime()) {
				compactorCookTime = 0;
				this.smeltItem();
			}
			return true;
		}
		compactorCookTime = 0;
		return false;
	}

	private boolean canSmelt() {
		if (power < MINPOWER || torque < MINTORQUE)
			return false;
		for (int i = 0; i < 4; i++)
			if (inv.getStackInSlot(i).isEmpty())
				return false;
		ItemStack a = inv.getStackInSlot(0);
		for (int i = 1; i < 4; i++)
			if (!ItemStack.isSameItemSameComponents(a, inv.getStackInSlot(i)))
				return false;
		CompactorRecipes.Recipe r = CompactorRecipes.get(a);
		if (r == null)
			return false;
		if (pressure < r.pressure() || temperature < r.temperature())
			return false;
		ItemStack itemstack = r.getOutput();
		ItemStack out = inv.getStackInSlot(4);
		if (out.isEmpty())
			return true;
		if (!ItemStack.isSameItemSameComponents(out, itemstack))
			return false;
		return out.getCount() < out.getMaxStackSize();
	}

	public void smeltItem() {
		if (!this.canSmelt())
			return;
		ItemStack itemstack = CompactorRecipes.get(inv.getStackInSlot(0)).getOutput();
		ItemStack out = inv.getStackInSlot(4);
		if (out.isEmpty())
			inv.setStackInSlot(4, itemstack);
		else
			out.grow(itemstack.getCount());
		for (int i = 0; i < 4; i++)
			inv.getStackInSlot(i).shrink(1);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (phi < 0.5F)
			phi = 1F;
		if (power < MINPOWER || torque < MINTORQUE)
			return;
		if (phi >= 1.5F || phi <= 0.5F)
			if (rand.nextInt(40) > 0)
				return;
		if (animdir)
			phi += 0.03125F;
		else
			phi -= 0.03125F;
		if (phi >= 1.5F || phi <= 0.5F)
			animdir = !animdir;
	}

	public boolean isItemValidForSlot(int slot, ItemStack is) {
		if (slot == 4)
			return false;
		if (is.is(Items.COAL) || is.is(RotaryItems.part(CraftingPart.ANTHRACITE)) || is.is(RotaryItems.part(CraftingPart.PRISMANE)) || is.is(RotaryItems.part(CraftingPart.LONSDA)))
			return true;
		return CompactorRecipes.isCompactable(is);
	}

	public int getOperationTime() {
		int s = this.getStage();
		return (int)Math.max(1, 300*s-15*s*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	public int getNumberConsecutiveOperations() {
		int threshold = (int)ReikaMathLibrary.intpow(2, 1+300/15);
		return 1+(omega >= threshold ? (int)(ReikaMathLibrary.logbase(omega, 2)-300/15) : 0);
	}

	@Override
	public int getRedstoneOverride() {
		return this.canSmelt() ? 0 : 15;
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
	public void setTemperature(int T) {
		temperature = T;
	}

	@Override
	public int getMaxTemperature() {
		return MAXTEMP;
	}

	public int getPressure() {
		return pressure;
	}

	public void setPressure(int p) {
		pressure = p;
	}

	public void addPressure(int press) {
		pressure += press;
	}

	@Override
	public boolean canBeCooledWithFins() {
		return true;
	}

	@Override
	public boolean allowExternalHeating() {
		return true;
	}

	@Override
	public void resetAmbientTemperatureTimer() {
		tempTick = 5;
	}

	@Override
	public float getMultiplier() {
		return 0.75F;
	}

	@Override
	public void onOverheat(Level world, BlockPos pos) {}

	@Override
	public boolean canBeFrictionHeated() {
		return true;
	}

	/** Original ISidedInventory: valid items into slots 0-3 from any side, only the product (slot 4) out. */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 5;
			}

			@Override
			public ItemStack getStackInSlot(int slot) {
				return inv.getStackInSlot(slot);
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return slot < 4 ? inv.insertItem(slot, stack, simulate) : stack;
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return slot == 4 ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return slot < 4 && isItemValidForSlot(slot, stack);
			}
		};
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
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
		NBT.putShort("CookTime", (short)compactorCookTime);
		NBT.putInt("temperature", temperature);
		NBT.putInt("pressure", pressure);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		compactorCookTime = NBT.getShort("CookTime");
		temperature = NBT.getInt("temperature");
		pressure = NBT.getInt("pressure");
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
