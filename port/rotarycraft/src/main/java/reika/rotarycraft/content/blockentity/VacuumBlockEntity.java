/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.api.interfaces.Screwdriverable;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.TileEntityVacuum - with at least 16384 W from any side it pulls dropped items and XP orbs
 * within 8 + power/4096 blocks (at most 32; only 2 up and down when not equidistant) towards itself every second tick, swallows those
 * that touch it into its 54 slots (XP is stored), and pulls items out of inventories beside it (whole stacks from 4 times its minimum
 * power, else one item at a time). The stored XP is let out with the GUI button or when it is broken. The screwdriver toggles the
 * equidistant range (sneaking) and whether it keeps sucking when full. Not ported: the XP fluid, ChromatiCraft's lizard doggos.
 */
public class VacuumBlockEntity extends PowerReceiverBlockEntity implements Screwdriverable, net.minecraft.world.Clearable {

	public static final int FALLOFF = 4096;
	public static final int MAXRANGE = 32;

	private int experience;
	public boolean equidistant = true;
	public boolean suckIfFull = true;
	private boolean isFull;

	private final ItemStackHandler inv = new ItemStackHandler(54) {
		@Override
		protected void onContentsChanged(int slot) {
			isFull = false;
			VacuumBlockEntity.this.setChanged();
		}
	};

	public VacuumBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.VACUUM.get(), pos, state, 1, 1, 16384);
	}

	@Override
	public String getName() {
		return "Item Vacuum";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public int getExperience() {
		return experience;
	}

	public void addExperience(int xp) {
		experience += xp;
	}

	public int getRange() {
		return (int)Math.min(8+power/FALLOFF, MAXRANGE);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.readPowerSummative();
		tickcount++;
		if (power < MINPOWER || tickcount < 2)
			return;
		tickcount = 0;
		if (suckIfFull || !isFull) {
			this.suck(world, pos);
			this.absorb(world, pos);
			this.transfer(world, pos);
		}
	}

	private void transfer(Level world, BlockPos pos) {
		boolean stacks = power/MINPOWER >= 4;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			if (world.getBlockEntity(pos.relative(d)) instanceof VacuumBlockEntity)
				continue;
			IItemHandler ii = world.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(d), d.getOpposite());
			if (ii == null)
				continue;
			for (int k = 0; k < ii.getSlots(); k++) {
				ItemStack in = ii.extractItem(k, stacks ? 64 : 1, true);
				if (in.isEmpty())
					continue;
				ItemStack left = ItemHandlerHelper.insertItemStacked(inv, in, true);
				int n = in.getCount()-left.getCount();
				if (stacks && !left.isEmpty() || n <= 0)
					continue;
				ItemHandlerHelper.insertItemStacked(inv, ii.extractItem(k, n, false), false);
			}
		}
	}

	private void suck(Level world, BlockPos pos) {
		int r = this.getRange();
		AABB box = equidistant ? new AABB(pos).inflate(r) : new AABB(pos).inflate(r, 2, r);
		double v = Math.max(1, power/1048576D);
		for (Entity ent : world.getEntitiesOfClass(Entity.class, box, e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) {
			if (ent.tickCount <= 5)
				continue;
			double dx = pos.getX()+0.5-ent.getX(), dy = pos.getY()+0.5-ent.getY(), dz = pos.getZ()+0.5-ent.getZ();
			double ddt = Math.sqrt(dx*dx+dy*dy+dz*dz);
			if (ent.tickCount > 50 && ddt > 1.5 && ent.tickCount%400 < 80) {
				double t = world.getGameTime()/25D;
				dx += 2.875*Math.cos(t);
				dz += 2.875*Math.sin(t);
			}
			double vx = Mth.clamp(v*dx/ddt/ddt, -0.125, 0.125), vy = Mth.clamp(v*dy/ddt/ddt/2, -0.125, 0.125), vz = Mth.clamp(v*dz/ddt/ddt, -0.125, 0.125);
			Vec3 m = ent.getDeltaMovement().add(vx, vy, vz);
			if (ent.getY() < pos.getY())
				m = m.add(0, 0.125, 0);
			ent.setDeltaMovement(m);
			ent.hurtMarked = true;
		}
	}

	private void absorb(Level world, BlockPos pos) {
		AABB close = new AABB(pos).inflate(0.25);
		boolean suck = false;
		for (ItemEntity ent : world.getEntitiesOfClass(ItemEntity.class, close)) {
			suck = true;
			if (ent.hasPickUpDelay())
				continue;
			ItemStack left = ItemHandlerHelper.insertItemStacked(inv, ent.getItem().copy(), false);
			if (left.isEmpty())
				ent.discard();
			else
				ent.setItem(left);
			world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.1F+0.5F*rand.nextFloat(), rand.nextFloat());
		}
		isFull = !suck;
		for (ExperienceOrb xp : world.getEntitiesOfClass(ExperienceOrb.class, close)) {
			experience += xp.getValue();
			xp.discard();
			world.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.1F, 0.5F*((rand.nextFloat()-rand.nextFloat())*0.7F+1.8F));
		}
	}

	/** Original spawnXP (the GUI button): the stored XP as orbs around it. */
	public void spawnXP() {
		if (level instanceof ServerLevel sl && experience > 0)
			ExperienceOrb.award(sl, new Vec3(worldPosition.getX()-1+2*rand.nextFloat(), worldPosition.getY()+2*rand.nextFloat(), worldPosition.getZ()-1+2*rand.nextFloat()), experience);
		experience = 0;
		this.syncAllData();
	}

	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		equidistant = !equidistant;
		this.syncAllData();
		return true;
	}

	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		suckIfFull = !suckIfFull;
		this.syncAllData();
		return true;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	/** Original: Container.calcRedstoneFromInventory. */
	@Override
	public int getRedstoneOverride() {
		float f = 0;
		int n = 0;
		for (int i = 0; i < inv.getSlots(); i++) {
			ItemStack is = inv.getStackInSlot(i);
			if (!is.isEmpty()) {
				f += is.getCount()/(float)Math.min(inv.getSlotLimit(i), is.getMaxStackSize());
				n++;
			}
		}
		return Mth.floor(f/inv.getSlots()*14)+(n > 0 ? 1 : 0);
	}

	/** In and out from any side (original canExtractItem: true). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return inv;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
		this.spawnXP();
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("xp", experience);
		NBT.putBoolean("equi", equidistant);
		NBT.putBoolean("full", suckIfFull);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		experience = NBT.getInt("xp");
		equidistant = NBT.getBoolean("equi");
		suckIfFull = !NBT.contains("full") || NBT.getBoolean("full");
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
