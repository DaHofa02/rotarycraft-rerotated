/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.mojang.authlib.GameProfile;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.content.block.BedrockSliceBlock;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.DifficultyEffects;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Production.TileEntityBedrockBreaker. Needs 16384 Nm and 4194304 W (PowerReceivers.BEDROCKBREAKER).
 * The head advances through soft blocks and destroys ordinary blocks (no drops) until it reaches bedrock, which it grinds as a bedrock
 * slice in 16 steps, then turns into bedrock dust (DifficultyEffects.BEDROCKDUST: 3/2/1). Dust goes into an adjacent inventory, else its
 * own slot, else onto the ground; right-click pops it out. The world's bottom layer (original y=0) only with "Allow Bedrock Breaker to
 * Break Y=0". Not ported: Factorization bedrock, SurrogateBedrock (other mods), the BedrockDigEvent and achievements.
 */
public class BedrockBreakerBlockEntity extends PowerReceiverBlockEntity {

	private Direction facing;
	private int step = 1;

	private double dropx;
	private double dropy;
	private double dropz;

	private final ItemStackHandler inv = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			BedrockBreakerBlockEntity.this.setChanged();
		}
	};

	public BedrockBreakerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.BEDROCK_BREAKER.get(), pos, state, 16384, 1, 4194304);
	}

	@Override
	public String getName() {
		return "Bedrock Breaker";
	}

	public Direction getFacing() {
		return facing != null ? facing : Direction.EAST;
	}

	private boolean isBottomLayer(Level world, int y) {
		return y == world.getMinBuildHeight();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		tickcount++;
		this.getIOSides(pos, this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readBreakerPower();
		if (power >= MINPOWER && torque >= MINTORQUE) {
			int time = this.getOperationTime();
			if (tickcount >= time) {
				this.process(world, pos);
				tickcount = 0;
			}
			BlockPos h = pos.relative(facing, step);
			BlockState b = world.getBlockState(h);
			if (!b.isAir() && world instanceof ServerLevel sl) {
				float f = step+this.getGrindFraction()-0.5F;
				for (int i = 0; i < 4; i++) {
					double px = pos.getX()+0.5+facing.getStepX()*f;
					double py = pos.getY()+0.5+facing.getStepY()*f;
					double pz = pos.getZ()+0.5+facing.getStepZ()*f;
					if (facing.getStepX() == 0)
						px += (rand.nextDouble()*2-1)*0.5;
					if (facing.getStepY() == 0)
						py += (rand.nextDouble()*2-1)*0.5;
					if (facing.getStepZ() == 0)
						pz += (rand.nextDouble()*2-1)*0.5;
					sl.sendParticles(ParticleTypes.CRIT, px, py, pz, 1, 0, 0, 0, 0);
				}
				SoundType st = b.getSoundType(world, h, null);
				world.playSound(null, h, st.getStepSound(), SoundSource.BLOCKS, 0.5F+rand.nextFloat(), 0.5F*rand.nextFloat());
			}
		}
	}

	public void process(Level world, BlockPos pos) {
		if (this.hasInventorySpace()) {
			BlockPos c = this.getHeadLocation();
			if (this.canBreakAt(world, c)) {
				this.grind(world, pos, c);
			}
		}
	}

	public BlockPos getHeadLocation() {
		return worldPosition.relative(this.getFacing(), step);
	}

	/** Original canBreakAt, with ReikaPlayerAPI.playerCanBreakAt: a block-break event as the placer (protection mods can refuse it). */
	private boolean canBreakAt(Level world, BlockPos p) {
		if (p.getY() < world.getMinBuildHeight())
			return false;
		if (p.getY() >= world.getMaxBuildHeight())
			return false;
		if (this.isBottomLayer(world, p.getY()) && !RotaryConfig.VOIDHOLE.get())
			return false;
		if (!(world instanceof ServerLevel sl))
			return true;
		FakePlayer fp = this.getPlacerUUID() != null ? FakePlayerFactory.get(sl, new GameProfile(this.getPlacerUUID(), this.getPlacerName() != null ? this.getPlacerName() : "[RotaryCraft]"))
				: FakePlayerFactory.getMinecraft(sl);
		BlockEvent.BreakEvent evt = new BlockEvent.BreakEvent(world, p, world.getBlockState(p), fp);
		return !NeoForge.EVENT_BUS.post(evt).isCanceled();
	}

	private boolean processBlock(Level world, BlockPos p) {
		BlockState b = world.getBlockState(p);
		return b.is(Blocks.BEDROCK) || b.is(RotaryBlocks.BEDROCK_SLICE.get());
	}

	private ItemStack dust(int n) {
		return new ItemStack(RotaryItems.part(CraftingPart.BEDROCKDUST), n);
	}

	private boolean hasInventorySpace() {
		ItemStack at = inv.getStackInSlot(0);
		if (at.isEmpty())
			return true;
		if (!at.is(RotaryItems.part(CraftingPart.BEDROCKDUST)))
			return false;
		return at.getCount()+DifficultyEffects.BEDROCKDUST.getInt() <= at.getMaxStackSize();
	}

	/** Original getReceptor: the read side, and no power at all on the world's bottom layer unless allowed. */
	private boolean getReceptor(int metadata) {
		if (this.isBottomLayer(level, worldPosition.getY()) && !RotaryConfig.VOIDHOLE.get())
			return false;
		switch (metadata) {
			case 0 -> read = Direction.WEST;
			case 1 -> read = Direction.EAST;
			case 2 -> read = Direction.NORTH;
			case 3 -> read = Direction.SOUTH;
			case 4 -> read = Direction.DOWN;
			case 5 -> read = Direction.UP;
		}
		return true;
	}

	private void readBreakerPower() {
		if (!this.getReceptor(this.getBlockMetadata()))
			return;
		this.readPower();
	}

	public void getIOSides(BlockPos p, int metadata) {
		dropx = p.getX()+0.5;
		dropy = p.getY()+1.25;
		dropz = p.getZ()+0.5;
		switch (metadata) {
			case 0 -> facing = Direction.EAST;
			case 1 -> facing = Direction.WEST;
			case 2 -> facing = Direction.SOUTH;
			case 3 -> facing = Direction.NORTH;
			case 4 -> facing = Direction.UP;
			case 5 -> {
				dropy = p.getY()-0.25;
				facing = Direction.DOWN;
			}
		}
	}

	private void grind(Level world, BlockPos m, BlockPos p) {
		if (this.processBlock(world, p)) {
			BlockState b = world.getBlockState(p);
			if (b.is(Blocks.BEDROCK)) {
				world.playSound(null, p, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.5F, rand.nextFloat()*0.4F+0.8F);
				world.setBlock(p, RotaryBlocks.BEDROCK_SLICE.get().defaultBlockState().setValue(BedrockSliceBlock.FACING, this.getFacing().getOpposite()), 3);
			}
			else {
				int rockmetadata = b.getValue(BedrockSliceBlock.LEVEL);
				if (rockmetadata < 15) {
					world.playSound(null, p, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.5F, rand.nextFloat()*0.4F+0.8F);
					world.setBlock(p, b.setValue(BedrockSliceBlock.LEVEL, rockmetadata+1), 3);
					step--;
					this.incrementStep(world, m);
				}
				else {
					world.playSound(null, p, SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 0.5F, rand.nextFloat()*0.4F+0.8F);
					ItemStack is = this.dust(this.getNumberDust());
					world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
					if (!this.chestCheck(world, is)) {
						if (this.isInventoryFull())
							world.addFreshEntity(new ItemEntity(world, dropx, dropy, dropz, is));
						else
							reika.rotarycraft.lib.data.InventoryHelper.addOrSetStack(is, inv, 0);
					}
				}
			}
		}
		else {
			BlockState b = world.getBlockState(p);
			if (!b.isAir() && b.getDestroySpeed(world, p) >= 0) {
				world.levelEvent(2001, p, net.minecraft.world.level.block.Block.getId(b)); //break sound and particles
				world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
			}
			this.incrementStep(world, m);
		}
	}

	/** Original chestCheck: the whole stack into an adjacent inventory if it fits. */
	private boolean chestCheck(Level world, ItemStack is) {
		for (Direction dir : Direction.values()) {
			IItemHandler h = world.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
			if (h != null && ItemHandlerHelper.insertItemStacked(h, is.copy(), true).isEmpty()) {
				ItemHandlerHelper.insertItemStacked(h, is, false);
				return true;
			}
		}
		return false;
	}

	/** Original incrementStep: the head moves on through soft blocks (air, fluids, replaceable). */
	private void incrementStep(Level world, BlockPos m) {
		int max = step+1;
		for (int i = 1; i < max; i++) {
			BlockPos d = m.relative(facing, i);
			BlockState b = world.getBlockState(d);
			if (!(b.isAir() || b.canBeReplaced() || !b.getFluidState().isEmpty() || b.is(Blocks.VINE))) {
				step = i;
				return;
			}
		}
		step = max;
	}

	/** Original dropInventory (right-click). */
	public void dropInventory() {
		ItemStack at = inv.getStackInSlot(0);
		if (at.isEmpty())
			return;
		ItemEntity itementity = new ItemEntity(level, dropx, dropy, dropz, at.copy());
		itementity.setNoPickUpDelay();
		double vy = this.getBlockMetadata() != 5 ? 0.1+0.2*rand.nextFloat()+0.25*rand.nextFloat()*rand.nextInt(2) : 0;
		itementity.setDeltaMovement(-0.025+0.05*rand.nextFloat(), vy, -0.025+0.05*rand.nextFloat());
		level.addFreshEntity(itementity);
		level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F, ((rand.nextFloat()-rand.nextFloat())*0.7F+1.0F)*2.0F);
		inv.setStackInSlot(0, ItemStack.EMPTY);
	}

	/** Original getNumberDust with the default slice yield of 1. */
	private int getNumberDust() {
		return Math.max(1, DifficultyEffects.BEDROCKDUST.getInt());
	}

	public int getContents() {
		ItemStack at = inv.getStackInSlot(0);
		return at.is(RotaryItems.part(CraftingPart.BEDROCKDUST)) ? at.getCount() : 0;
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (power < MINPOWER || torque < MINTORQUE)
			return;
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	public boolean isInventoryFull() {
		ItemStack at = inv.getStackInSlot(0);
		if (at.isEmpty())
			return false;
		if (!at.is(RotaryItems.part(CraftingPart.BEDROCKDUST)))
			return true;
		return at.getCount() >= at.getMaxStackSize();
	}

	@Override
	public void onEMP() {}

	/** Original DurationRegistry.BEDROCK: DurationFormula(600, 30) = max(1, 600 - 30*log2(omega+1)) ticks. */
	public int getOperationTime() {
		return (int)Math.max(1, 600-30*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("step", step);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		step = NBT.getInt("step");
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

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(0).copy());
	}

	public int getStep() {
		return step;
	}

	/** Original getGrindFraction: how far the bedrock slice at the head is ground (level/16). */
	public float getGrindFraction() {
		if (level == null)
			return 0;
		BlockState b = level.getBlockState(this.getHeadLocation());
		return b.is(RotaryBlocks.BEDROCK_SLICE.get()) ? b.getValue(BedrockSliceBlock.LEVEL)/16F : 0;
	}
}
