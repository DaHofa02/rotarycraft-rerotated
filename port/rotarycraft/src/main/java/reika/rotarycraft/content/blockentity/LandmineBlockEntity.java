/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The land mine, written for the port from the original's behaviour: goes off when a living thing stands on it (on the ground, not
 * sneaking, within 2 blocks above) or an arrow hits it. Its blast is 2 per gunpowder in its four charge slots; its four modifier slots add
 * fire (blaze powder), poison for the creatures near it (spider eye), shrapnel that hurts everything within 8 blocks (glass) and chain
 * detonation of the land mines next to it (TNT). Without gunpowder it does nothing.
 * Changes: the original's ageing spring that could set it off by itself is left out.
 */
public class LandmineBlockEntity extends RotaryCraftBlockEntity {

	/** Tests switch the real explosion off and count detonations. */
	public static boolean allowExplosion = true;

	private final ItemStackHandler inv = new ItemStackHandler(8) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return slot < 4 ? is.is(Items.GUNPOWDER) : isModifier(is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			LandmineBlockEntity.this.setChanged();
		}
	};
	public int detonations;
	public float lastPower;
	private int chainDelay = -1;

	public LandmineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.LANDMINE.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public static boolean isModifier(ItemStack is) {
		return is.is(Items.BLAZE_POWDER) || is.is(Items.SPIDER_EYE) || is.is(Items.GLASS) || is.is(Items.TNT);
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public IItemHandler getItemHandler(@Nullable Direction side) {
		return inv;
	}

	private boolean has(net.minecraft.world.item.Item i) {
		for (int s = 4; s < 8; s++)
			if (inv.getStackInSlot(s).is(i))
				return true;
		return false;
	}

	public float getExplosionPower() {
		int n = 0;
		for (int s = 0; s < 4; s++)
			if (inv.getStackInSlot(s).is(Items.GUNPOWDER))
				n++;
		return 2F*n;
	}

	public boolean isTriggered(Level world, BlockPos pos) {
		AABB above = new AABB(pos.above()).expandTowards(0, 1, 0);
		for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, above))
			if (e.onGround() && !e.isShiftKeyDown())
				return true;
		return !world.getEntitiesOfClass(AbstractArrow.class, new AABB(pos).inflate(0.1)).isEmpty();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide || this.getExplosionPower() <= 0)
			return;
		if (chainDelay > 0 && --chainDelay == 0) {
			this.detonate(world, pos);
			return;
		}
		if (chainDelay < 0 && this.isTriggered(world, pos))
			this.detonate(world, pos);
	}

	/** Sets the mine off a few ticks later (chain detonation). */
	public void chainTrigger() {
		if (chainDelay < 0 && this.getExplosionPower() > 0)
			chainDelay = 5;
	}

	public void detonate(Level world, BlockPos pos) {
		float p = this.getExplosionPower();
		boolean fire = this.has(Items.BLAZE_POWDER), poison = this.has(Items.SPIDER_EYE), shrapnel = this.has(Items.GLASS), chain = this.has(Items.TNT);
		lastPower = p;
		detonations++;
		Vec3 c = Vec3.atCenterOf(pos);
		if (poison)
			for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(4)))
				e.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
		if (shrapnel)
			for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(8))) {
				double d = e.position().distanceTo(c);
				if (d < 8)
					e.hurt(world.damageSources().explosion(null, null), (float)(8-d)*2);
			}
		if (chain)
			for (Direction dir : Direction.values())
				if (world.getBlockEntity(pos.relative(dir)) instanceof LandmineBlockEntity other)
					other.chainTrigger();
		for (int s = 0; s < 8; s++)
			inv.setStackInSlot(s, ItemStack.EMPTY);
		chainDelay = -1;
		if (allowExplosion) {
			world.removeBlock(pos, false);
			world.explode(null, c.x, c.y, c.z, p, fire, Level.ExplosionInteraction.BLOCK);
		}
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
		NBT.putInt("chain", chainDelay);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
		chainDelay = NBT.contains("chain") ? NBT.getInt("chain") : -1;
	}
}
