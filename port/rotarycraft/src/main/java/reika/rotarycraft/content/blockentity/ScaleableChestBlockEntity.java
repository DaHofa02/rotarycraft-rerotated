/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.content.menu.ScaleableChestMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;

/**
 * Original: Reika.RotaryCraft.TileEntities.Storage.TileEntityScaleableChest - a chest of up to 972 slots (18 pages of 54): with at
 * least 4096 W from its input side (metadata 0 east, 1 west, 2 north, 3 south) it opens 9 + (power - 4096)/128 of them; without power
 * it cannot be opened. Power that keeps switching on and off is dangerous: more than 3 changes within 20 ticks make it smoke and
 * fizz (and it cannot be opened), more than 8 bang, more than 10 blow it up (strength 4; the chest item drops). Broken, it keeps its
 * contents in the item.
 */
public class ScaleableChestBlockEntity extends PowerReceiverBlockEntity {

	public static final int MAXROWS = 6;
	public static final int MAXSIZE = 972;
	public static final int POWERCHANGEAGE = 20;

	private final List<Integer> powerchanges = new ArrayList<>();
	private int numchanges;
	private boolean lastpower;
	public float lidAngle, prevLidAngle;
	public int numUsingPlayers;

	private final ItemStackHandler inv = new ItemStackHandler(MAXSIZE) {
		@Override
		protected void onContentsChanged(int slot) {
			ScaleableChestBlockEntity.this.setChanged();
		}
	};

	public ScaleableChestBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SCALEABLE_CHEST.get(), pos, state, 1, 1, 4096);
	}

	@Override
	public String getName() {
		return "Scaleable Chest";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public int getNumberSlots() {
		int size = power < MINPOWER ? 9 : 9+(int)((power-MINPOWER)/128);
		return Math.min(size, MAXSIZE);
	}

	/** Original getMaxPage: the last page with slots (0-based, as the original counts). */
	public int getMaxPage() {
		int size = this.getNumberSlots()/9/6;
		return Math.min(getMaxPages(), size);
	}

	public static int getMaxPages() {
		return MAXSIZE/9/6;
	}

	public int getSlotsOnPage(int page) {
		int max = this.getMaxPage();
		if (page == max)
			return this.getNumberSlots()-max*54;
		return page < max ? 54 : 0;
	}

	public int getNumPowerChanges() {
		return numchanges;
	}

	@Override
	public boolean isPlayerAccessible(Player ep) {
		return numchanges <= 0 && power >= MINPOWER && super.isPlayerAccessible(ep);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = switch (this.getBlockMetadata()) {
			case 1 -> Direction.WEST;
			case 2 -> Direction.NORTH;
			case 3 -> Direction.SOUTH;
			default -> Direction.EAST;
		};
		if (!world.isClientSide)
			this.readPower();
		if (this.testInconsistentPower(world, pos))
			return;
		if (power < MINPOWER) {
			lidAngle = prevLidAngle = 0;
			return;
		}
		if (!world.isClientSide) {
			int n = 0;
			for (Player ep : world.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(pos).inflate(5)))
				if (ep.containerMenu instanceof ScaleableChestMenu m && m.tile == this)
					n++;
			if (n != numUsingPlayers) {
				numUsingPlayers = n;
				this.syncAllData();
			}
		}
		prevLidAngle = lidAngle;
		if (numUsingPlayers > 0 && lidAngle == 0)
			world.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, rand.nextFloat()*0.1F+0.9F);
		if (numUsingPlayers == 0 && lidAngle > 0 || numUsingPlayers > 0 && lidAngle < 1) {
			float f1 = lidAngle;
			lidAngle = Math.max(0, Math.min(1, lidAngle+(numUsingPlayers > 0 ? 0.1F : -0.1F)));
			if (lidAngle < 0.5F && f1 >= 0.5F)
				world.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, rand.nextFloat()*0.1F+0.9F);
		}
	}

	/** Original testInconsistentPower: true while the power keeps flickering. */
	private boolean testInconsistentPower(Level world, BlockPos pos) {
		for (int i = 0; i < powerchanges.size(); i++)
			powerchanges.set(i, powerchanges.get(i)+1);
		powerchanges.removeIf(c -> c > POWERCHANGEAGE);
		boolean pw = power >= MINPOWER;
		if (pw != lastpower)
			powerchanges.add(0);
		numchanges = powerchanges.size();
		lastpower = pw;
		if (world.isClientSide)
			return numchanges > 3;
		if (numchanges > 10) {
			Containers.dropItemStack(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, new ItemStack(this.getBlockState().getBlock()));
			this.clearContents();
			world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 4, RotaryConfig.BLOCKDAMAGE.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
			return true;
		}
		if (numchanges > 3) {
			((ServerLevel)world).sendParticles(ParticleTypes.SMOKE, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, numchanges/3, 0.3, 0.3, 0.3, 0);
			if (numchanges > 8) {
				if (rand.nextInt(19-numchanges) == 0)
					world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 0, Level.ExplosionInteraction.NONE);
			}
			else if (rand.nextInt(11-numchanges) == 0)
				world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, 1);
			return true;
		}
		return false;
	}

	private void clearContents() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	/** All 972 slots, from any side, as the original IInventory. */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return inv;
	}

	/** Original writeInventoryToItem / readInventoryFromItem (the item placer). */
	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		CompoundTag tag = input.get(RotaryComponents.CHEST_CONTENTS.get());
		if (tag != null && level != null)
			inv.deserializeNBT(level.registryAccess(), tag);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		boolean any = false;
		for (int i = 0; i < inv.getSlots() && !any; i++)
			any = !inv.getStackInSlot(i).isEmpty();
		if (any && level != null)
			components.set(RotaryComponents.CHEST_CONTENTS.get(), inv.serializeNBT(level.registryAccess()));
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("chng", numchanges);
		NBT.putInt("player", numUsingPlayers);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		numchanges = NBT.getInt("chng");
		numUsingPlayers = NBT.getInt("player");
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
