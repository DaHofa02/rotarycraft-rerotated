/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Farming.TileEntityAutoBreeder - with at least 16384 W from below, animals within
 * 8 + (power - 16384)/2048 blocks (at most 24) that it has food for in its 18 slots are led to it once a second and, once grown up,
 * ready and within 2.4 blocks of it, put in love for one food item (tamed only, for wolves and ocelots). Food as in the original's
 * table: wheat or canola husks for sheep and cows, seeds or husks for chickens, carrots or husks for pigs, meat and fish for wolves, fish
 * for ocelots. Not ported: villagers (1.21 villagers cannot be set to mate), Twilight Forest animals.
 */
public class AutoBreederBlockEntity extends PowerReceiverBlockEntity implements net.minecraft.world.Clearable {

	public static final int MAXRANGE = 24;

	private static Map<EntityType<?>, List<Item>> feedItems;

	private final ItemStackHandler inv = new ItemStackHandler(18) {
		@Override
		protected void onContentsChanged(int slot) {
			AutoBreederBlockEntity.this.setChanged();
			AutoBreederBlockEntity.this.syncAllData();
		}
	};

	public AutoBreederBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.AUTO_BREEDER.get(), pos, state, 1, 1, 16384);
	}

	@Override
	public String getName() {
		return "Auto Breeder";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Original feedItems (by exact entity class). */
	public static List<Item> getFeedItems(EntityType<?> type) {
		if (feedItems == null) {
			Item husks = RotaryItems.CANOLA_HUSKS.get();
			feedItems = Map.of(
					EntityType.SHEEP, List.of(Items.WHEAT, husks),
					EntityType.COW, List.of(Items.WHEAT, husks),
					EntityType.CHICKEN, List.of(Items.WHEAT_SEEDS, husks),
					EntityType.PIG, List.of(Items.CARROT, husks),
					EntityType.WOLF, List.of(Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.BEEF, Items.COOKED_BEEF, Items.CHICKEN, Items.COOKED_CHICKEN, Items.COD, Items.COOKED_COD),
					EntityType.OCELOT, List.of(Items.COD, Items.COOKED_COD));
		}
		return feedItems.getOrDefault(type, List.of());
	}

	/** Original IdleComparator: no food for anything in the inventory. */
	public boolean isIdle() {
		for (int i = 0; i < inv.getSlots(); i++) {
			Item it = inv.getStackInSlot(i).getItem();
			for (EntityType<?> t : List.of(EntityType.SHEEP, EntityType.COW, EntityType.CHICKEN, EntityType.PIG, EntityType.WOLF, EntityType.OCELOT))
				if (getFeedItems(t).contains(it))
					return false;
		}
		return true;
	}

	public int getRange() {
		return (int)Math.min(MAXRANGE, 8+(power-MINPOWER)/2048);
	}

	private int getFeedSlot(Animal ent) {
		for (Item item : getFeedItems(ent.getType()))
			for (int i = 0; i < inv.getSlots(); i++)
				if (inv.getStackInSlot(i).is(item))
					return i;
		return -1;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (!(world instanceof ServerLevel sl))
			return;
		tickcount++;
		read = Direction.DOWN;
		this.readPower();
		if (power < MINPOWER)
			return;
		boolean pathing = tickcount >= 20;
		if (pathing)
			tickcount = 0;
		for (Animal ent : world.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(this.getRange()))) {
			if (this.getFeedSlot(ent) < 0)
				continue;
			if (pathing && !(ent instanceof TamableAnimal ta && ta.isOrderedToSit())) {
				if (!ent.isInLove() && !ent.isBaby() && ent.getAge() == 0)
					ent.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
				else
					ent.getNavigation().stop();
			}
			double dd = Math.sqrt(ent.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()));
			boolean tameOk = !(ent instanceof TamableAnimal ta) || ta.isTame();
			if (!ent.isBaby() && ent.getAge() <= 0 && dd <= 2.4 && tameOk && !ent.isInLove()) {
				ent.setInLoveTime(600);
				inv.extractItem(this.getFeedSlot(ent), 1, false);
				sl.sendParticles(ParticleTypes.HEART, ent.getX(), ent.getY()+0.5+ent.getBbHeight()/2, ent.getZ(), 1+rand.nextInt(3), ent.getBbWidth(), ent.getBbHeight()/2, ent.getBbWidth(), 0.02);
			}
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	/** Food in from any side; nothing comes out (original canExtractItem: false). */
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
				return true;
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
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
