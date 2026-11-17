/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The item filter, written for the port from the original's behaviour. Slot 0 holds a template item, slot 1 is the buffer, slots 2-17 a
 * blacklist. While it gets 1024 W (from any side, all at the same speed) the buffer takes in only items that pass the template's test -
 * and with a redstone signal only those that fail it; blacklisted items never. Anything may take items out of the buffer; it signals 15
 * while the buffer is empty. The test has one setting per criterion - match, mismatch or ignore: same item, same data components, same mod,
 * and each of the template's item tags.
 * Changes: the original's real job, pulling matching items out of an Applied Energistics network, needs AE2 and is left out (so is its
 * Java class test); data components and item tags stand in for metadata, NBT and the ore dictionary.
 */
public class ItemFilterBlockEntity extends PowerReceiverBlockEntity {

	public enum MatchType {
		MATCH, MISMATCH, IGNORE;

		public boolean check(boolean match) {
			return switch (this) {
				case MATCH -> match;
				case MISMATCH -> !match;
				case IGNORE -> true;
			};
		}

		public MatchType next() {
			return values()[(this.ordinal()+1)%3];
		}
	}

	public enum Criterion {
		ITEM, COMPONENTS, MOD, TAGS
	}

	private final ItemStackHandler inv = new ItemStackHandler(18) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return slot != 1 || ItemFilterBlockEntity.this.accepts(is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			if (slot == 0)
				ItemFilterBlockEntity.this.templateChanged();
			ItemFilterBlockEntity.this.setChanged();
		}
	};
	private final MatchType[] settings = {MatchType.MATCH, MatchType.MATCH, MatchType.MATCH, MatchType.MATCH};
	private final List<ResourceLocation> tags = new ArrayList<>();
	private final List<MatchType> tagSettings = new ArrayList<>();

	public ItemFilterBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.ITEM_FILTER.get(), pos, state, 1, 1, 1024);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Capability: items go into the buffer only (through the test), and only the buffer gives items out. */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new net.neoforged.neoforge.items.wrapper.RangedWrapper(inv, 1, 2);
	}

	public MatchType getSetting(Criterion c) {
		return settings[c.ordinal()];
	}

	public void cycle(Criterion c) {
		settings[c.ordinal()] = settings[c.ordinal()].next();
		this.syncAllData();
	}

	public List<ResourceLocation> getTags() {
		return tags;
	}

	public MatchType getTagSetting(int i) {
		return tagSettings.get(i);
	}

	public void cycleTag(int i) {
		if (i >= 0 && i < tagSettings.size()) {
			tagSettings.set(i, tagSettings.get(i).next());
			this.syncAllData();
		}
	}

	/** A new template: its tags are listed afresh, all set to "match"; the other settings stay. */
	private void templateChanged() {
		List<ResourceLocation> now = new ArrayList<>();
		ItemStack t = inv.getStackInSlot(0);
		if (!t.isEmpty())
			t.getTags().forEach(k -> now.add(k.location()));
		now.sort(null);
		if (!now.equals(tags)) {
			tags.clear();
			tags.addAll(now);
			tagSettings.clear();
			now.forEach(k -> tagSettings.add(MatchType.MATCH));
		}
	}

	public boolean isBlacklisted(ItemStack is) {
		for (int i = 2; i < 18; i++)
			if (ItemStack.isSameItemSameComponents(inv.getStackInSlot(i), is))
				return true;
		return false;
	}

	/** The template's test (false without a template). */
	public boolean matches(ItemStack is) {
		ItemStack t = inv.getStackInSlot(0);
		if (t.isEmpty() || is.isEmpty())
			return false;
		if (!settings[Criterion.ITEM.ordinal()].check(is.is(t.getItem())))
			return false;
		if (!settings[Criterion.COMPONENTS.ordinal()].check(Objects.equals(is.getComponentsPatch(), t.getComponentsPatch())))
			return false;
		String mod = BuiltInRegistries.ITEM.getKey(is.getItem()).getNamespace();
		if (!settings[Criterion.MOD.ordinal()].check(mod.equals(BuiltInRegistries.ITEM.getKey(t.getItem()).getNamespace())))
			return false;
		if (settings[Criterion.TAGS.ordinal()] != MatchType.IGNORE) {
			for (int i = 0; i < tags.size(); i++)
				if (!tagSettings.get(i).check(is.is(TagKey.create(BuiltInRegistries.ITEM.key(), tags.get(i)))))
					return false;
		}
		return true;
	}

	/** Whether the buffer takes this item now. */
	public boolean accepts(ItemStack is) {
		if (power < MINPOWER || isBlacklisted(is) || inv.getStackInSlot(0).isEmpty())
			return false;
		boolean inverted = level != null && level.hasNeighborSignal(worldPosition);
		return this.matches(is) != inverted;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.readPowerSummative();
	}

	@Override
	public int getRedstoneOverride() {
		return inv.getStackInSlot(1).isEmpty() ? 15 : 0;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < 18; i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
		for (Criterion c : Criterion.values())
			NBT.putInt("m_"+c.name(), settings[c.ordinal()].ordinal());
		ListTag li = new ListTag();
		for (int i = 0; i < tags.size(); i++) {
			CompoundTag t = new CompoundTag();
			t.putString("tag", tags.get(i).toString());
			t.putInt("m", tagSettings.get(i).ordinal());
			li.add(t);
		}
		NBT.put("tags", li);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
		for (Criterion c : Criterion.values())
			settings[c.ordinal()] = MatchType.values()[Math.floorMod(NBT.getInt("m_"+c.name()), 3)];
		tags.clear();
		tagSettings.clear();
		for (Tag t : NBT.getList("tags", Tag.TAG_COMPOUND)) {
			CompoundTag c = (CompoundTag)t;
			ResourceLocation rl = ResourceLocation.tryParse(c.getString("tag"));
			if (rl != null) {
				tags.add(rl);
				tagSettings.add(MatchType.values()[Math.floorMod(c.getInt("m"), 3)]);
			}
		}
	}
}
