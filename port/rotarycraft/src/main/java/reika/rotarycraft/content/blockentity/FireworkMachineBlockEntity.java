/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Decorative.TileEntityFireworkMachine - with 65536 W and 4096 rad/s (any side) it launches a
 * firework every max(1, 300 - 16*log2(omega+1)) ticks from its 27-slot store: from a firework star there (with paper and gunpowder), or
 * else a random star of its own (a random dye colour, sometimes diamond for a trail, glowstone for twinkling, a random shape from fire
 * charges, gold nuggets, feathers and heads, sometimes a fade colour) - as the vanilla recipes would make them; 1 to 3 gunpowder give the
 * flight. Ingredients are used up only one time in 1 + (P / 65536) / 8. Without the ingredients for a rocket it gives a redstone signal.
 * Not ported: the silk touch enchantment (nothing used up).
 */
public class FireworkMachineBlockEntity extends PowerReceiverBlockEntity {

	public static final int DURATION_BASE = 300, DURATION_SCALE = 16;

	private final ItemStackHandler inv = new ItemStackHandler(27) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return isFireworkIngredient(is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			FireworkMachineBlockEntity.this.setChanged();
		}
	};
	public boolean idle;
	/** Rockets launched (for tests and the GUI; not saved). */
	public int launched;

	public FireworkMachineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FIREWORK_MACHINE.get(), pos, state, 1, 4096, 65536);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Original: ingredients in from any side (ReikaItemHelper.isFireworkIngredient), nothing out. */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new net.neoforged.neoforge.items.wrapper.RangedWrapper(inv, 0, 27) {
			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return ItemStack.EMPTY;
			}
		};
	}

	public static boolean isFireworkIngredient(ItemStack is) {
		return is.is(Items.DIAMOND) || is.getItem() instanceof DyeItem || is.is(Items.GLOWSTONE_DUST) || is.is(Items.FEATHER) || is.is(Items.GOLD_NUGGET)
				|| is.is(Items.FIRE_CHARGE) || is.is(ItemTags.SKULLS) || is.is(Items.FIREWORK_STAR) || is.is(Items.PAPER) || is.is(Items.GUNPOWDER);
	}

	/** Original DurationRegistry.FIREWORK. */
	public int getOperationTime() {
		return (int)Math.max(1, DURATION_BASE-DURATION_SCALE*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		tickcount++;
		if (world.isClientSide)
			return;
		this.readPowerSummative();
		if (power < MINPOWER || omega < MINSPEED || tickcount < this.getOperationTime())
			return;
		tickcount = 0;
		if (!this.canCraftARocket()) {
			idle = true;
			return;
		}
		idle = false;
		ItemStack rocket = ItemStack.EMPTY;
		if (!this.has(Items.FIREWORK_STAR)) {
			ItemStack star = this.randomRecipe();
			if (star.isEmpty())
				return;
			rocket = this.starToRocket(star);
		}
		else if (this.canMakeRocketFromStar()) {
			int slot;
			do
				slot = world.random.nextInt(inv.getSlots());
			while (!inv.getStackInSlot(slot).is(Items.FIREWORK_STAR));
			ItemStack star = inv.getStackInSlot(slot).copyWithCount(1);
			if (this.consumeChance())
				inv.extractItem(slot, 1, false);
			rocket = this.starToRocket(star);
		}
		if (!rocket.isEmpty()) {
			world.addFreshEntity(new FireworkRocketEntity(world, pos.getX()+0.5, pos.getY()+1.25, pos.getZ()+0.5, rocket));
			launched++;
		}
	}

	private boolean has(Item i) {
		for (int s = 0; s < inv.getSlots(); s++)
			if (inv.getStackInSlot(s).is(i))
				return true;
		return false;
	}

	private int count(Item i) {
		int n = 0;
		for (int s = 0; s < inv.getSlots(); s++)
			if (inv.getStackInSlot(s).is(i))
				n += inv.getStackInSlot(s).getCount();
		return n;
	}

	private boolean hasDye() {
		for (int s = 0; s < inv.getSlots(); s++)
			if (inv.getStackInSlot(s).getItem() instanceof DyeItem)
				return true;
		return false;
	}

	private boolean consumeChance() {
		int excess = (int)(power/MINPOWER);
		return level.random.nextInt(1+excess/8) == 0;
	}

	public boolean canCraftARocket() {
		return this.has(Items.PAPER) && this.hasDye() && this.count(Items.GUNPOWDER) >= 2 || this.has(Items.GUNPOWDER) && this.has(Items.FIREWORK_STAR);
	}

	private boolean canMakeRocketFromStar() {
		return this.has(Items.PAPER) && this.has(Items.GUNPOWDER) && this.has(Items.FIREWORK_STAR);
	}

	private boolean getIngredient(Item id, boolean decr) {
		for (int s = 0; s < inv.getSlots(); s++) {
			if (inv.getStackInSlot(s).is(id)) {
				if (decr)
					inv.extractItem(s, 1, false);
				return true;
			}
		}
		return false;
	}

	/** Original starToRocket: 1-3 gunpowder (as far as there is any) and paper make it a rocket; vanilla needs both. */
	private ItemStack starToRocket(ItemStack star) {
		int gunpowder = 0;
		if (this.getIngredient(Items.GUNPOWDER, this.canMakeRocketFromStar() && this.consumeChance()))
			gunpowder++;
		int numgunpowder = level.random.nextInt(3)+1;
		if (numgunpowder >= 2 && this.getIngredient(Items.GUNPOWDER, this.consumeChance()))
			gunpowder++;
		if (numgunpowder >= 3 && this.getIngredient(Items.GUNPOWDER, this.consumeChance()))
			gunpowder++;
		boolean paper = this.getIngredient(Items.PAPER, this.consumeChance());
		if (gunpowder < 1 || !paper)
			return ItemStack.EMPTY;
		ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
		FireworkExplosion e = star.get(DataComponents.FIREWORK_EXPLOSION);
		rocket.set(DataComponents.FIREWORKS, new Fireworks(gunpowder, e != null ? List.of(e) : List.of()));
		return rocket;
	}

	@Nullable
	private DyeColor pickRandomColor(boolean decr) {
		List<DyeColor> have = new ArrayList<>();
		for (int s = 0; s < inv.getSlots(); s++)
			if (inv.getStackInSlot(s).getItem() instanceof DyeItem d && !have.contains(d.getDyeColor()))
				have.add(d.getDyeColor());
		if (have.isEmpty())
			return null;
		DyeColor c = have.get(level.random.nextInt(have.size()));
		if (decr) {
			for (int s = 0; s < inv.getSlots(); s++)
				if (inv.getStackInSlot(s).getItem() instanceof DyeItem d && d.getDyeColor() == c) {
					inv.extractItem(s, 1, false);
					break;
				}
		}
		return c;
	}

	private boolean hasSkull() {
		for (int s = 0; s < inv.getSlots(); s++)
			if (inv.getStackInSlot(s).is(ItemTags.SKULLS))
				return true;
		return false;
	}

	/** Original getShape: a random shape among the shape items there (fire charge, gold nugget, feather, head), a small ball if none. */
	private FireworkExplosion.Shape getShape() {
		boolean[] hasShape = {this.has(Items.FIRE_CHARGE), this.has(Items.GOLD_NUGGET), this.has(Items.FEATHER), this.hasSkull()};
		if (!hasShape[0] && !hasShape[1] && !hasShape[2] && !hasShape[3])
			return FireworkExplosion.Shape.SMALL_BALL;
		int shape;
		do
			shape = level.random.nextInt(4);
		while (!hasShape[shape]);
		if (this.consumeChance()) {
			if (shape == 3) {
				for (int s = 0; s < inv.getSlots(); s++)
					if (inv.getStackInSlot(s).is(ItemTags.SKULLS)) {
						inv.extractItem(s, 1, false);
						break;
					}
			}
			else
				this.getIngredient(shape == 0 ? Items.FIRE_CHARGE : shape == 1 ? Items.GOLD_NUGGET : Items.FEATHER, true);
		}
		return switch (shape) {
			case 0 -> FireworkExplosion.Shape.LARGE_BALL;
			case 1 -> FireworkExplosion.Shape.STAR;
			case 2 -> FireworkExplosion.Shape.BURST;
			default -> FireworkExplosion.Shape.CREEPER;
		};
	}

	/** Original randomRecipe: a firework star as the vanilla star recipe would make it from a random choice of what is there. */
	private ItemStack randomRecipe() {
		DyeColor dye = this.pickRandomColor(this.consumeChance() && this.has(Items.GUNPOWDER));
		FireworkExplosion.Shape shape = this.getShape();
		if (dye == null)
			return ItemStack.EMPTY;
		if (!this.getIngredient(Items.GUNPOWDER, this.consumeChance()))
			return ItemStack.EMPTY;
		boolean trail = level.random.nextInt(2) == 0 && this.getIngredient(Items.DIAMOND, this.consumeChance());
		boolean twinkle = level.random.nextInt(2) == 0 && this.getIngredient(Items.GLOWSTONE_DUST, this.consumeChance());
		IntList fade = IntList.of();
		if (level.random.nextInt(2) == 0) {
			DyeColor dye2 = this.pickRandomColor(this.consumeChance());
			if (dye2 != null)
				fade = IntList.of(dye2.getFireworkColor());
		}
		ItemStack star = new ItemStack(Items.FIREWORK_STAR);
		star.set(DataComponents.FIREWORK_EXPLOSION, new FireworkExplosion(shape, IntList.of(dye.getFireworkColor()), fade, trail, twinkle));
		return star;
	}

	@Override
	public int getRedstoneOverride() {
		return this.canCraftARocket() ? 0 : 15;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
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
}
