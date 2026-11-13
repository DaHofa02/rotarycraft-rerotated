/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.World.TileEntityAerosolizer - empties beneficial potions put into its 9 slots into 9 stores
 * (1 unit per bottle, 3 for long potions, at most 64; the bottles stay behind) and, with at least 16384 W from any side, gives everyone
 * in the room around it (up to 64 blocks along each axis, walled in by opaque blocks) the stored effect for 5 seconds, once a second,
 * more often and one level higher for every further store holding the same effect. Every 2 minutes each store loses one unit.
 * Change: the room's lower bound is measured from its own height (the original used its x coordinate there). Not ported: the potion
 * fluid handler and other mods' potions.
 */
public class AerosolizerBlockEntity extends PowerReceiverBlockEntity implements net.minecraft.world.Clearable {

	public static final int MAXRANGE = 64;
	public static final int CAPACITY = 64;

	private final ResourceLocation[] potions = new ResourceLocation[9];
	private final int[] potionLevel = new int[9];
	private int tickcount2;
	public boolean idle;

	private final ItemStackHandler inv = new ItemStackHandler(9) {
		@Override
		protected void onContentsChanged(int slot) {
			AerosolizerBlockEntity.this.setChanged();
		}
	};

	public AerosolizerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.AEROSOLIZER.get(), pos, state, 1, 1, 16384);
	}

	@Override
	public String getName() {
		return "Aerosolizer";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public int getPotionLevel(int slot) {
		return potionLevel[slot];
	}

	@Nullable
	public Holder<MobEffect> getEffect(int slot) {
		return potions[slot] == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(potions[slot]).orElse(null);
	}

	public int getPotionColor(int slot) {
		Holder<MobEffect> e = this.getEffect(slot);
		return e == null ? 0xFF000000 : 0xFF000000 | e.value().getColor();
	}

	/** Original getEffectFromItem: the first beneficial effect of a potion, and how much one bottle stores (3 for long potions). */
	@Nullable
	public static Holder<MobEffect> getEffectFromItem(ItemStack is) {
		if (!(is.getItem() instanceof PotionItem))
			return null;
		PotionContents pc = is.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		for (MobEffectInstance p : pc.getAllEffects())
			if (p.getEffect().value().isBeneficial())
				return p.getEffect();
		return null;
	}

	public static int getAmount(ItemStack is) {
		PotionContents pc = is.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		return pc.potion().map(p -> p.unwrapKey().map(k -> k.location().getPath().startsWith("long_")).orElse(false)).orElse(false) ? 3 : 1;
	}

	public int getMultiplier(int i) {
		if (potions[i] == null)
			return 0;
		int c = 0;
		for (ResourceLocation p : potions)
			if (Objects.equals(p, potions[i]))
				c++;
		return c;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.readPowerSummative();
		tickcount++;
		tickcount2++;
		this.consumeBottlesAndStorePotions();
		if (power < MINPOWER)
			return;
		idle = true;
		for (ResourceLocation p : potions)
			if (p != null)
				idle = false;
		boolean decay = tickcount >= 2400;
		for (int i = 0; i < 9; i++) {
			if (tickcount2 >= 20/Math.max(this.getMultiplier(i), 1) && potionLevel[i] > 0)
				this.dispense(world, pos, i);
			if (decay && potionLevel[i] > 0)
				potionLevel[i]--;
			if (potionLevel[i] <= 0) {
				potionLevel[i] = 0;
				potions[i] = null;
			}
		}
		if (tickcount2 >= 20)
			tickcount2 = 0;
		if (decay)
			tickcount = 0;
	}

	private void consumeBottlesAndStorePotions() {
		boolean changed = false;
		for (int i = 0; i < 9; i++) {
			ItemStack in = inv.getStackInSlot(i);
			Holder<MobEffect> eff = getEffectFromItem(in);
			if (eff == null)
				continue;
			ResourceLocation id = eff.unwrapKey().get().location();
			int num = in.getCount()*getAmount(in);
			if ((potions[i] == null || potions[i].equals(id)) && potionLevel[i]+num <= CAPACITY) {
				potions[i] = id;
				potionLevel[i] += num;
				inv.setStackInSlot(i, new ItemStack(Items.GLASS_BOTTLE, in.getCount()));
				changed = true;
			}
		}
		if (changed)
			this.syncAllData();
	}

	/** Original getRoom: from itself along each axis until an opaque block, up to the range. */
	public AABB getRoom(Level world, BlockPos pos) {
		int[] lo = new int[3], hi = new int[3];
		Direction[] dirs = {Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};
		int[] reach = new int[6];
		for (int d = 0; d < 6; d++) {
			int r = 0;
			for (int i = 1; i < MAXRANGE; i++) {
				BlockPos c = pos.relative(dirs[d], i);
				if (world.getBlockState(c).isSolidRender(world, c))
					break;
				r = i;
			}
			reach[d] = r;
		}
		return new AABB(pos.getX()-reach[0], pos.getY()-reach[2], pos.getZ()-reach[4], pos.getX()+1+reach[1], pos.getY()+1+reach[3], pos.getZ()+1+reach[5]);
	}

	private void dispense(Level world, BlockPos pos, int i) {
		Holder<MobEffect> eff = this.getEffect(i);
		if (eff == null)
			return;
		int bonus = this.getMultiplier(i)-1;
		for (LivingEntity mob : world.getEntitiesOfClass(LivingEntity.class, this.getRoom(world, pos)))
			mob.addEffect(new MobEffectInstance(eff, 100, bonus));
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	/** Potions in from any side; only empty bottles come out (original canExtractItem). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 9;
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
				return inv.getStackInSlot(slot).is(Items.GLASS_BOTTLE) ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return true;
			}
		};
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < 9; i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < 9; i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		for (int i = 0; i < 9; i++)
			if (potions[i] != null)
				NBT.putString("potion_"+i, potions[i].toString());
		NBT.putIntArray("levels", potionLevel);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		for (int i = 0; i < 9; i++)
			potions[i] = NBT.contains("potion_"+i) ? ResourceLocation.tryParse(NBT.getString("potion_"+i)) : null;
		int[] lv = NBT.getIntArray("levels");
		for (int i = 0; i < 9; i++)
			potionLevel[i] = i < lv.length ? lv[i] : 0;
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
