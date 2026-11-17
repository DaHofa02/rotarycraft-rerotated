/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.World.TileEntityDefoliator - with 16384 W from below it uses poison (4000 mB; a potion of
 * poison in the top slot gives 1000 mB, the bottle goes to the bottom slot) to kill vegetation: 2*sqrt(omega) times a tick a random block
 * within 8*log2(torque) blocks (at most 128) is tested; leaves, plants, vines, cactus, logs and saplings break (dropping their items),
 * every living thing within 3 blocks of it is poisoned (50 ticks, level IV) and takes half a heart of magic damage, and 1 mB is used.
 * Changes: there is no poison fluid in the port; the poison is kept as a level, filled from potions or by pipe with ReactorCraft's
 * chlorine (the original's other accepted fluid). Other mods' woods and the particle packet are left out.
 */
public class DefoliatorBlockEntity extends PowerReceiverBlockEntity implements PipeConnector {

	public static final int CAPACITY = 4000;
	private static final ResourceLocation CHLORINE = ResourceLocation.fromNamespaceAndPath("reactorcraft", "chlorine");

	private final ItemStackHandler inv = new ItemStackHandler(2) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return slot == 0 && isPoisonPotion(is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			DefoliatorBlockEntity.this.setChanged();
		}
	};
	private int poison;

	public DefoliatorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.DEFOLIATOR.get(), pos, state, 1, 1, 16384);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public static boolean isPoisonPotion(ItemStack is) {
		PotionContents pc = is.get(DataComponents.POTION_CONTENTS);
		return is.is(Items.POTION) && pc != null && pc.is(Potions.POISON);
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Original: poison potions in, only empty bottles out. */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new net.neoforged.neoforge.items.wrapper.RangedWrapper(inv, 0, 2) {
			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return inv.getStackInSlot(slot).is(Items.GLASS_BOTTLE) ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}
		};
	}

	public int getPoison() {
		return poison;
	}

	public void setPoison(int amt) {
		poison = Math.max(0, Math.min(CAPACITY, amt));
	}

	public int getPoisonScaled(int i) {
		return poison*i/CAPACITY;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN; //getPowerBelow
		if (world.isClientSide)
			return;
		this.readPower();
		this.consumePotions();
		if (poison > 0) {
			int r = this.getRange();
			int n = this.getNumberPasses();
			for (int i = 0; i < n && poison > 0; i++)
				this.decay(world, pos.offset(world.random.nextInt(2*r+1)-r, world.random.nextInt(2*r+1)-r, world.random.nextInt(2*r+1)-r));
		}
	}

	public int getNumberPasses() {
		return power < MINPOWER ? 0 : 2*(int)Math.sqrt(omega);
	}

	private void consumePotions() {
		ItemStack is = inv.getStackInSlot(0);
		if (!is.isEmpty() && CAPACITY-poison >= 1000 && isPoisonPotion(is)) {
			ItemStack out = inv.getStackInSlot(1);
			if (!out.isEmpty() && (!out.is(Items.GLASS_BOTTLE) || out.getCount() >= out.getMaxStackSize()))
				return;
			poison += 1000;
			inv.extractItem(0, 1, false);
			inv.setStackInSlot(1, new ItemStack(Items.GLASS_BOTTLE, out.getCount()+1));
		}
	}

	/** Original decay's block list: leaves, plants, vines, cactus, logs and saplings. */
	public static boolean isVegetation(BlockState s) {
		if (s.isAir() || !s.getFluidState().isEmpty())
			return false;
		return s.is(BlockTags.LEAVES) || s.is(BlockTags.OVERWORLD_NATURAL_LOGS) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.CROPS)
				|| s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(Blocks.VINE) || s.is(Blocks.CACTUS) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.LILY_PAD)
				|| s.is(Blocks.BROWN_MUSHROOM) || s.is(Blocks.RED_MUSHROOM) || s.is(Blocks.COCOA);
	}

	private void decay(Level world, BlockPos p) {
		BlockState id = world.getBlockState(p);
		if (!isVegetation(id))
			return;
		List<ItemStack> li = Block.getDrops(id, (ServerLevel)world, p, world.getBlockEntity(p));
		world.destroyBlock(p, false);
		for (ItemStack is : li)
			Containers.dropItemStack(world, p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, is);
		for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(p).inflate(3))) {
			e.addEffect(new MobEffectInstance(MobEffects.POISON, 50, 3));
			e.hurt(world.damageSources().magic(), 0.5F);
		}
		poison--;
		this.setChanged();
	}

	public int getRange() {
		int r = (int)(8*ReikaMathLibrary.logbase(Math.max(torque, 1), 2));
		return Math.min(r, this.getMaxRange());
	}

	public int getMaxRange() {
		return 128;
	}

	private static boolean isValidFluid(FluidStack fs) {
		return CHLORINE.equals(BuiltInRegistries.FLUID.getKey(fs.getFluid()));
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p) && side.getAxis() != Direction.Axis.Y;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side.getAxis() != Direction.Axis.Y ? Flow.INPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from != null && from.getAxis() == Direction.Axis.Y || !isValidFluid(resource))
			return 0;
		int amt = Math.min(resource.getAmount(), CAPACITY-poison);
		if (amt > 0 && action.execute()) {
			poison += amt;
			this.setChanged();
		}
		return amt;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 1;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return FluidStack.EMPTY;
			}

			@Override
			public int getTankCapacity(int t) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return isValidFluid(stack);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return DefoliatorBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return FluidStack.EMPTY;
			}
		};
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < 2; i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("poison", poison);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		poison = NBT.getInt("poison");
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
