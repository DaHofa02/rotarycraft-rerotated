/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * The purifier, written for the port from the original's behaviour: turns other mods' steel (anything tagged c:ingots/steel but RotaryCraft's
 * own) into RotaryCraft steel. It needs 16384 W (at least 64 Nm, from any side) and a temperature of 600 C, which only fire (+200) or lava
 * (+600) next to it bring (water halves the ambient value, ice quarters it and melts); it gains or loses 1-2 C a tick towards that. Every
 * max(1, 800 - 40*log2(speed+1)) ticks it converts one ingot from each of its five input slots, using up its gunpowder 1 time in 25 and its
 * sand 1 time in 5. Above 1000 C it bursts into scrap.
 */
public class PurifierBlockEntity extends PowerReceiverBlockEntity {

	public static final int SMELTTEMP = 600;
	public static final int MAXTEMP = 1000;
	public static final TagKey<Item> STEEL = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/steel"));
	/** Extra items treated as other mods' steel (GameTests run without such a mod). */
	public static final Set<Item> TEST_STEELS = new HashSet<>();
	public static boolean allowOverheat = true;

	private final ItemStackHandler inv = new ItemStackHandler(8) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return switch (slot) {
				case 0 -> is.is(Items.GUNPOWDER);
				case 7 -> is.is(Items.SAND);
				case 6 -> true; //the output: filled by the machine (the capability keeps others out)
				default -> isModSteel(is);
			};
		}

		@Override
		protected void onContentsChanged(int slot) {
			PurifierBlockEntity.this.setChanged();
		}
	};
	public int temperature = Integer.MIN_VALUE;
	public int cookTime;
	public int converted;

	public PurifierBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.PURIFIER.get(), pos, state, 64, 1, 16384);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public static boolean isModSteel(ItemStack is) {
		return !is.isEmpty() && !is.is(RotaryItems.part(CraftingPart.STEELINGOT)) && (is.is(STEEL) || TEST_STEELS.contains(is.getItem()));
	}

	/** Gunpowder, sand and steel go in from any side; only the output comes out. */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new net.neoforged.neoforge.items.wrapper.RangedWrapper(inv, 0, 8) {
			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return slot == 6 ? stack : super.insertItem(slot, stack, simulate);
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return slot == 6 ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}
		};
	}

	public int getOperationTime() {
		return (int)Math.max(1, 800-40*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	public boolean canWork() {
		if (temperature < SMELTTEMP || !inv.getStackInSlot(0).is(Items.GUNPOWDER) || !inv.getStackInSlot(7).is(Items.SAND))
			return false;
		ItemStack out = inv.getStackInSlot(6);
		if (!out.isEmpty() && out.getCount()+5 > out.getMaxStackSize())
			return false;
		for (int i = 1; i < 6; i++)
			if (isModSteel(inv.getStackInSlot(i)))
				return true;
		return false;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.readPowerSummative();
		this.updateTemperature(world, pos);
		if (power < MINPOWER || torque < MINTORQUE || !this.canWork()) {
			cookTime = 0;
			return;
		}
		if (++cookTime >= this.getOperationTime()) {
			cookTime = 0;
			this.convert(world);
		}
	}

	private void convert(Level world) {
		int n = 0;
		for (int i = 1; i < 6; i++)
			if (isModSteel(inv.getStackInSlot(i))) {
				inv.extractItem(i, 1, false);
				n++;
			}
		ItemStack out = inv.getStackInSlot(6);
		inv.setStackInSlot(6, new ItemStack(RotaryItems.part(CraftingPart.STEELINGOT), out.getCount()+n));
		converted += n;
		if (world.random.nextInt(25) == 0)
			inv.extractItem(0, 1, false);
		if (world.random.nextInt(5) == 0)
			inv.extractItem(7, 1, false);
	}

	private static boolean nextTo(Level world, BlockPos pos, Predicate<BlockState> test) {
		for (Direction d : Direction.values())
			if (test.test(world.getBlockState(pos.relative(d))))
				return true;
		return false;
	}

	/** The temperature the surroundings pull it towards. */
	public int getTargetTemperature(Level world, BlockPos pos) {
		int t = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (nextTo(world, pos, s -> s.getFluidState().is(FluidTags.WATER)))
			t /= 2;
		for (Direction d : Direction.values())
			if (world.getBlockState(pos.relative(d)).is(Blocks.ICE)) {
				if (t > 0)
					t /= 4;
				world.setBlockAndUpdate(pos.relative(d), Blocks.WATER.defaultBlockState());
				break;
			}
		if (nextTo(world, pos, s -> s.is(BlockTags.FIRE)))
			t += 200;
		if (nextTo(world, pos, s -> s.getFluidState().is(FluidTags.LAVA)))
			t += 600;
		return t;
	}

	private void updateTemperature(Level world, BlockPos pos) {
		int amb = this.getTargetTemperature(world, pos);
		if (temperature == Integer.MIN_VALUE)
			temperature = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (temperature > amb)
			temperature--;
		if (temperature > amb*2)
			temperature--;
		if (temperature < amb)
			temperature++;
		if (temperature*2 < amb)
			temperature++;
		if (temperature > MAXTEMP && allowOverheat) {
			world.removeBlock(pos, false);
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(RotaryItems.part(CraftingPart.SCRAP), 2+world.random.nextInt(6)));
			world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 2F, true, Level.ExplosionInteraction.BLOCK);
		}
	}

	@Override
	public int getRedstoneOverride() {
		return inv.getStackInSlot(0).isEmpty() ? 15 : 0;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
		NBT.putInt("temp", temperature);
		NBT.putInt("time", cookTime);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
		temperature = NBT.contains("temp") ? NBT.getInt("temp") : Integer.MIN_VALUE;
		cookTime = NBT.getInt("time");
	}
}
