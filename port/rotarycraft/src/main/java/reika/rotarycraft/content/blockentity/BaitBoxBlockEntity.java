/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Farming.TileEntityBaitBox - with at least 32768 W from any side, mobs within 8 +
 * (power - 32768)/4096 blocks (at most 24) whose bait (MobBait) is in its 27 slots are drawn to it; those whose repellent is in it are
 * driven away and drop what they hold. Flying and swimming mobs (blazes, slimes, magma cubes, ghasts, squid, bats) are pushed directly.
 * With an egg in it, water next to it and at least 32768 W, it hatches a chicken now and then (or, with lava next to it too, makes a
 * cooked chicken). Baits as in the original table (1.7.10 items by their 1.21 names). Not ported: Forestry butterflies.
 */
public class BaitBoxBlockEntity extends PowerReceiverBlockEntity implements net.minecraft.world.Clearable {

	public static final int MAXRANGE = 24;

	/** Original MobBait: entity type, attractor, repellent. */
	public record MobBait(EntityType<?> type, Predicate<ItemStack> attract, Predicate<ItemStack> repel) {

		private static MobBait of(EntityType<?> t, Item a, Item r) {
			return new MobBait(t, is -> is.is(a), is -> is.is(r));
		}
	}

	private static List<MobBait> baits;

	public static List<MobBait> getBaits() {
		if (baits == null) {
			baits = List.of(
					MobBait.of(EntityType.CREEPER, Items.TNT, Items.OBSIDIAN),
					MobBait.of(EntityType.ENDERMAN, Items.GRASS_BLOCK, Items.SOUL_SAND),
					new MobBait(EntityType.ZOMBIE, is -> is.is(Items.GOLDEN_APPLE), is -> is.is(Items.POTION)
							&& is.getOrDefault(DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY).is(Potions.HEALING)),
					MobBait.of(EntityType.SKELETON, Items.SADDLE, Items.GLOWSTONE),
					MobBait.of(EntityType.SPIDER, Items.COCOA_BEANS, Items.OAK_FENCE),
					MobBait.of(EntityType.SLIME, Items.DIAMOND, Items.ICE),
					MobBait.of(EntityType.BLAZE, Items.FIRE_CHARGE, Items.WATER_BUCKET),
					MobBait.of(EntityType.ZOMBIFIED_PIGLIN, Items.APPLE, Items.BLAZE_ROD),
					MobBait.of(EntityType.MAGMA_CUBE, Items.BLAZE_POWDER, Items.SNOWBALL),
					MobBait.of(EntityType.GHAST, Items.GLOWSTONE_DUST, Items.BOW),
					MobBait.of(EntityType.SILVERFISH, Items.STONE_BRICKS, Items.WOODEN_PICKAXE),
					MobBait.of(EntityType.VILLAGER, Items.EMERALD, Items.CACTUS),
					MobBait.of(EntityType.IRON_GOLEM, Items.RED_WOOL, Items.LAVA_BUCKET),
					MobBait.of(EntityType.WITCH, Items.NETHER_WART, Items.ENDER_PEARL),
					MobBait.of(EntityType.SNOW_GOLEM, Items.CARROT, Items.TORCH),
					MobBait.of(EntityType.BAT, Items.MELON_SLICE, Items.NOTE_BLOCK),
					MobBait.of(EntityType.COW, Items.WHEAT, Items.STICK),
					MobBait.of(EntityType.PIG, Items.CARROT, Items.STICK),
					MobBait.of(EntityType.SHEEP, Items.WHEAT, Items.STICK),
					MobBait.of(EntityType.CHICKEN, Items.WHEAT_SEEDS, Items.STICK),
					MobBait.of(EntityType.OCELOT, Items.COD, Items.STICK),
					MobBait.of(EntityType.WOLF, Items.PORKCHOP, Items.GRAVEL),
					MobBait.of(EntityType.SQUID, Items.GOLD_INGOT, Items.LILY_PAD));
		}
		return baits;
	}

	@Nullable
	public static MobBait getBait(Mob e) {
		for (MobBait b : getBaits())
			if (b.type() == e.getType())
				return b;
		return null;
	}

	private final ItemStackHandler inv = new ItemStackHandler(27) {
		@Override
		protected void onContentsChanged(int slot) {
			BaitBoxBlockEntity.this.setChanged();
		}
	};

	public BaitBoxBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.BAIT_BOX.get(), pos, state, 1, 1, 32768);
	}

	@Override
	public String getName() {
		return "Bait Box";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public int getRange() {
		return (int)Math.min(MAXRANGE, 8+(power-MINPOWER)/4096);
	}

	private boolean has(Predicate<ItemStack> p) {
		for (int i = 0; i < inv.getSlots(); i++)
			if (!inv.getStackInSlot(i).isEmpty() && p.test(inv.getStackInSlot(i)))
				return true;
		return false;
	}

	private boolean inRange(Mob ent) {
		return power >= MINPOWER && Math.sqrt(ent.distanceToSqr(worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5)) <= this.getRange();
	}

	public boolean canRepel(Mob ent) {
		MobBait b = getBait(ent);
		return b != null && this.inRange(ent) && this.has(b.repel());
	}

	public boolean canAttract(Mob ent) {
		MobBait b = getBait(ent);
		return b != null && this.inRange(ent) && this.has(b.attract());
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.readPowerSummative();
		if (power < MINPOWER)
			return;
		if ((world.getGameTime() & 3) == 0) {
			for (Mob ent : world.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(this.getRange()))) {
				if (this.canRepel(ent))
					this.applyEffect(world, pos, ent, false);
				else if (this.canAttract(ent))
					this.applyEffect(world, pos, ent, true);
			}
		}
		if (rand.nextInt(20) == 0)
			this.doEggIncubation(world, pos);
	}

	private boolean nextTo(Level world, BlockPos pos, net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> tag) {
		for (Direction d : Direction.values())
			if (world.getFluidState(pos.relative(d)).is(tag))
				return true;
		return false;
	}

	private void doEggIncubation(Level world, BlockPos pos) {
		int slot = -1;
		for (int i = 0; i < inv.getSlots() && slot < 0; i++)
			if (inv.getStackInSlot(i).is(Items.EGG))
				slot = i;
		if (slot < 0 || !this.nextTo(world, pos, FluidTags.WATER))
			return;
		if (!this.nextTo(world, pos, FluidTags.LAVA)) {
			Chicken ec = EntityType.CHICKEN.create(world);
			if (ec != null) {
				ec.moveTo(pos.getX()+rand.nextDouble(), pos.getY()+rand.nextDouble()+0.5, pos.getZ()+rand.nextDouble(), rand.nextFloat()*90, rand.nextFloat()*360);
				world.addFreshEntity(ec);
			}
		}
		else {
			ItemStack left = ItemHandlerHelper.insertItemStacked(inv, new ItemStack(Items.COOKED_CHICKEN), false);
			if (!left.isEmpty())
				Containers.dropItemStack(world, pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5, left);
		}
		inv.extractItem(slot, 1, false);
	}

	private void applyEffect(Level world, BlockPos pos, Mob ent, boolean attract) {
		if (ent instanceof TamableAnimal ta && ta.isOrderedToSit())
			return;
		long time = world.getGameTime();
		CompoundTag data = ent.getPersistentData();
		if (time-data.getLong("baitbox") < 20)
			return;
		data.putLong("baitbox", time);
		if (ent instanceof PathfinderMob pm) {
			boolean present = pm.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof MachineGoal mg && mg.box == this && mg.attract == attract);
			if (!present)
				pm.goalSelector.addGoal(-1000000, new MachineGoal(pm, this, attract));
		}
		if (!attract)
			this.dropHeldItem(world, ent);
		double sign = attract ? 1 : -1;
		if (ent instanceof Blaze || ent instanceof Slime || ent instanceof Ghast || ent instanceof Squid) {
			if (!(ent instanceof Slime) || !ent.onGround()) {
				double vx = sign*0.02*(pos.getX()-ent.getX()), vz = sign*0.02*(pos.getZ()-ent.getZ());
				double vy = !(ent instanceof Squid) || ent.isInWater() ? sign*0.02*(pos.getY()-ent.getY()) : ent.getDeltaMovement().y;
				if (ent instanceof Blaze) {
					vx *= 4;
					vz *= 4;
				}
				ent.setDeltaMovement(vx, vy, vz);
				ent.hurtMarked = true;
			}
		}
		if (ent instanceof Bat) {
			ent.setDeltaMovement(sign*0.1*(pos.getX()-ent.getX()), sign*0.1*(pos.getY()-ent.getY()), sign*0.1*(pos.getZ()-ent.getZ()));
			ent.hurtMarked = true;
		}
	}

	private void dropHeldItem(Level world, Mob ent) {
		ItemStack held = ent.getMainHandItem();
		if (held.isEmpty())
			return;
		ent.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		ItemEntity ei = new ItemEntity(world, ent.getX(), ent.getEyeY(), ent.getZ(), held);
		ei.setDeltaMovement(-0.2+0.4*rand.nextFloat(), 0.4*rand.nextFloat(), -0.2+0.4*rand.nextFloat());
		ei.setPickUpDelay(200);
		world.addFreshEntity(ei);
	}

	/** Original AITaskSeekMachine / AITaskAvoidMachine: walk to the box, or away from it, while it attracts or repels the mob. */
	public static class MachineGoal extends Goal {

		private final PathfinderMob mob;
		private final BaitBoxBlockEntity box;
		private final boolean attract;

		public MachineGoal(PathfinderMob mob, BaitBoxBlockEntity box, boolean attract) {
			this.mob = mob;
			this.box = box;
			this.attract = attract;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return !box.isRemoved() && (attract ? box.canAttract(mob) : box.canRepel(mob));
		}

		@Override
		public void tick() {
			if (mob.getNavigation().isDone() || mob.tickCount%20 == 0) {
				BlockPos p = box.getBlockPos();
				if (attract)
					mob.getNavigation().moveTo(p.getX()+0.5, p.getY(), p.getZ()+0.5, 1.0);
				else {
					Vec3 away = DefaultRandomPos.getPosAway(mob, 16, 7, Vec3.atCenterOf(p));
					if (away != null)
						mob.getNavigation().moveTo(away.x, away.y, away.z, 1.0);
				}
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

	/** Items in and out from any side (original canExtractItem: true). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return inv;
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
