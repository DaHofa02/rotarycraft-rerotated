/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Farming.TileEntityWoodcutter - at least 16384 W and 64 Nm from behind; when a log stands in
 * the 3x3 in front of it (at its height), it reads the tree (DragonAPI TreeReader: logs and leaves of that kind, 26-connected, up to a
 * depth per kind: oak 18, spruce 48, birch 6, jungle 36, acacia and dark oak 12) and cuts it block by block, leaves first, then the
 * nearest first, every max(1, 40 - 4*log2(omega+1)) ticks (more blocks per operation above 2048 rad/s). The drops go into an inventory
 * below it or fall out at its height; a log cut at its height is replaced by a sapling from its one-slot store, which keeps the tree's
 * saplings (the rest are dropped once it holds a full stack). One log in three also gives 1-4 sawdust. Anything wooden or leafy on top
 * of it is cut too. Touching it while it runs deals 4 grinding damage.
 * Not ported: machine enchantments (silk touch, fortune, efficiency), the INSTACUT option (always on, the original's default; the
 * falling-log mode is not ported), slime trees and jamming (other mods' trees), other mods' woods. The top-of-machine check takes logs and
 * leaves (the original: wood and leaves material). Leftovers that do not fit into the inventory below when the store is emptied are
 * dropped (the original lost them).
 */
public class WoodcutterBlockEntity extends PowerReceiverBlockEntity implements net.minecraft.world.Clearable {

	/** Original ReikaTreeHelper (the six vanilla trees of 1.7.10) with TreeReader.getMaxDepthFromTreeType. */
	public enum TreeType {
		OAK(Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.OAK_SAPLING, 18),
		SPRUCE(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, Blocks.SPRUCE_SAPLING, 48),
		BIRCH(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, Blocks.BIRCH_SAPLING, 6),
		JUNGLE(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_SAPLING, 36),
		ACACIA(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, Blocks.ACACIA_SAPLING, 12),
		DARKOAK(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, Blocks.DARK_OAK_SAPLING, 12);

		public final Block log;
		public final Block leaves;
		public final Block sapling;
		public final int maxDepth;

		TreeType(Block log, Block leaves, Block sapling, int depth) {
			this.log = log;
			this.leaves = leaves;
			this.sapling = sapling;
			maxDepth = depth;
		}

		@Nullable
		public static TreeType getTree(BlockState s) {
			for (TreeType t : values())
				if (s.is(t.log))
					return t;
			return null;
		}
	}

	public static final int DURATION_BASE = 40;
	public static final int DURATION_SCALE = 4;

	private final ItemStackHandler inv = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			WoodcutterBlockEntity.this.setChanged();
		}
	};

	/** The blocks still to cut (TreeReader), in cutting order. */
	private final List<BlockPos> tree = new ArrayList<>();
	@Nullable
	private TreeType type;
	private int logCount;
	private int leafCount;
	private boolean cuttingTree;
	private long lastSoundTick = -1;

	public WoodcutterBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.WOODCUTTER.get(), pos, state, 64, 1, 16384);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Original InertIInv: no automation in or out. */
	@Nullable
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return null;
	}

	public List<BlockPos> getTree() {
		return tree;
	}

	@Nullable
	public TreeType getTreeType() {
		return type;
	}

	private void getIOSides(int meta) {
		read = switch (meta) {
			case 0 -> Direction.EAST;
			case 1 -> Direction.WEST;
			case 2 -> Direction.SOUTH;
			default -> Direction.NORTH;
		};
	}

	/** The block in front of it (the side opposite the input). */
	private BlockPos editPos() {
		return worldPosition.relative(read.getOpposite());
	}

	public boolean isRunning() {
		return power >= MINPOWER && torque >= MINTORQUE;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		tickcount++;
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		if (!this.isRunning())
			return;
		if (tree.isEmpty() && this.hasWood()) {
			this.reset();
			BlockPos edit = this.editPos();
			TreeType t = TreeType.getTree(world.getBlockState(edit));
			if (t == null)
				t = TreeType.getTree(world.getBlockState(edit.relative(read.getOpposite())));
			if (t != null) {
				type = t;
				for (int i = -1; i <= 1; i++)
					for (int j = -1; j <= 1; j++)
						this.addTree(world, edit.offset(i, 0, j));
			}
			if (!tree.isEmpty())
				cuttingTree = true;
			this.checkAndMatchInventory();
			//sortBlocksByHeight + reverse, sortBlocksByDistance, LeafPrioritizer (stable sorts: the last one decides first)
			tree.sort(Comparator.<BlockPos>comparingInt(c -> this.isLeaf(world.getBlockState(c)) ? 0 : 1)
					.thenComparingDouble(c -> c.distSqr(pos))
					.thenComparingInt(c -> -c.getY()));
		}
		BlockPos up = pos.above();
		BlockState b = world.getBlockState(up);
		if (!b.isAir() && (b.is(BlockTags.LOGS) || b.is(BlockTags.LEAVES))) {
			for (ItemStack is : Block.getDrops(b, (ServerLevel)world, up, world.getBlockEntity(up)))
				dropAt(world, pos.getX()+0.5, pos.getY()-0.25, pos.getZ()+0.5, is);
			world.removeBlock(up, false);
		}
		if (!tree.isEmpty() && tickcount >= this.getOperationTime()) {
			tickcount = 0;
			if (!cuttingTree && !this.isValidTree()) {
				this.reset();
				tree.clear();
			}
			else {
				for (int i = 0; i < this.getNumberConsecutiveOperations(); i++) {
					this.cutCoord(world, pos, tree.remove(0));
					if (tree.isEmpty())
						break;
				}
			}
		}
	}

	private void reset() {
		logCount = 0;
		leafCount = 0;
		type = null;
	}

	public boolean isValidTree() {
		return leafCount >= 5 && logCount >= 2;
	}

	/** DragonAPI TreeReader.addTree: breadth-first over the 26 neighbours, up to the tree kind's depth. */
	private void addTree(Level world, BlockPos start) {
		Set<BlockPos> search = new HashSet<>(), failed = new HashSet<>(), next = new HashSet<>();
		Set<BlockPos> have = new HashSet<>(tree);
		this.validateAndAdd(world, start, search, failed);
		int iterations = 0;
		while (!search.isEmpty() && iterations < type.maxDepth) {
			iterations++;
			for (BlockPos c : search) {
				if (have.add(c))
					tree.add(c);
				for (int dx = -1; dx <= 1; dx++)
					for (int dy = -1; dy <= 1; dy++)
						for (int dz = -1; dz <= 1; dz++) {
							if (dx == 0 && dy == 0 && dz == 0)
								continue;
							BlockPos c2 = c.offset(dx, dy, dz);
							if (!search.contains(c2) && !next.contains(c2) && !have.contains(c2) && !failed.contains(c2))
								this.validateAndAdd(world, c2, next, failed);
						}
			}
			search.clear();
			search.addAll(next);
			next.clear();
		}
	}

	private void validateAndAdd(Level world, BlockPos c, Set<BlockPos> search, Set<BlockPos> failed) {
		BlockState s = world.getBlockState(c);
		if (s.is(type.log)) {
			logCount++;
			search.add(c);
		}
		else if (s.is(type.leaves)) {
			leafCount++;
			search.add(c);
		}
		else
			failed.add(c);
	}

	private boolean isLeaf(BlockState s) {
		return s.is(BlockTags.LEAVES);
	}

	private void cutCoord(Level world, BlockPos pos, BlockPos c) {
		BlockState drop = world.getBlockState(c);
		if (drop.isAir())
			return;
		this.cutBlock(world, pos, c, drop);
		if (c.getY() == pos.getY() && type != null && drop.is(type.log)) {
			BlockState sapling = type.sapling.defaultBlockState();
			if (sapling.canSurvive(world, c) && this.shouldPlantSapling()) {
				inv.extractItem(0, 1, false);
				world.setBlockAndUpdate(c, sapling);
			}
		}
	}

	private void cutBlock(Level world, BlockPos pos, BlockPos c, BlockState s) {
		this.dropBlocks(world, pos, c, s);
		world.setBlockAndUpdate(c, Blocks.AIR.defaultBlockState());
		if (lastSoundTick != world.getGameTime()) {
			boolean leafy = this.isLeaf(s) || s.is(BlockTags.REPLACEABLE_BY_TREES);
			world.playSound(null, pos, leafy ? SoundEvents.GRASS_BREAK : SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.5F+world.random.nextFloat()*0.5F, 1.0F);
			lastSoundTick = world.getGameTime();
		}
	}

	private void dropBlocks(Level world, BlockPos pos, BlockPos c, BlockState s) {
		List<ItemStack> drops = new ArrayList<>(Block.getDrops(s, (ServerLevel)world, c, world.getBlockEntity(c)));
		if (type != null && s.is(type.log) && world.random.nextInt(3) == 0)
			drops.add(new ItemStack(RotaryItems.part(CraftingPart.SAWDUST), 1+world.random.nextInt(4)));
		for (ItemStack todrop : drops) {
			if (type != null && todrop.is(type.sapling.asItem())) {
				ItemStack in = inv.getStackInSlot(0);
				if (!in.isEmpty() && in.getCount() >= in.getMaxStackSize()) {
					todrop = this.chestCheck(world, pos, todrop);
					dropAt(world, c.getX()+0.5, pos.getY()-0.25, c.getZ()+0.5, todrop);
				}
				else {
					ItemStack left = inv.insertItem(0, todrop, false);
					dropAt(world, c.getX()+0.5, pos.getY()-0.25, c.getZ()+0.5, left);
				}
			}
			else {
				todrop = this.chestCheck(world, pos, todrop);
				dropAt(world, c.getX()+0.5, pos.getY()-0.25, c.getZ()+0.5, todrop);
			}
		}
	}

	private static void dropAt(Level world, double x, double y, double z, ItemStack is) {
		if (is.isEmpty())
			return;
		ItemEntity e = new ItemEntity(world, x, y, z, is);
		e.setDefaultPickUpDelay();
		world.addFreshEntity(e);
	}

	/** Original chestCheck: into the inventory below, as far as it fits; returns the rest. */
	private ItemStack chestCheck(Level world, BlockPos pos, ItemStack is) {
		IItemHandler below = world.getCapability(Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
		return below == null ? is : ItemHandlerHelper.insertItemStacked(below, is, false);
	}

	private void checkAndMatchInventory() {
		ItemStack in = inv.getStackInSlot(0);
		if (!in.isEmpty() && (type == null || !in.is(type.sapling.asItem()))) {
			inv.setStackInSlot(0, ItemStack.EMPTY);
			ItemStack left = this.chestCheck(level, worldPosition, in);
			dropAt(level, worldPosition.getX()+0.5, worldPosition.getY()+1, worldPosition.getZ()+0.5, left);
		}
	}

	private boolean shouldPlantSapling() {
		ItemStack in = inv.getStackInSlot(0);
		return type != null && !in.isEmpty() && in.is(type.sapling.asItem());
	}

	/** Original hasWood: one of the six vanilla logs in the 3x3 in front of it, at its height. */
	public boolean hasWood() {
		if (read == null || level == null)
			return false;
		BlockPos edit = this.editPos();
		for (int i = -1; i <= 1; i++)
			for (int j = -1; j <= 1; j++)
				if (TreeType.getTree(level.getBlockState(edit.offset(i, 0, j))) != null)
					return true;
		return false;
	}

	/** Original DurationRegistry.WOODCUTTER: max(1, 40 - 4*log2(omega+1)). */
	public int getOperationTime() {
		return (int)Math.max(1, DURATION_BASE-DURATION_SCALE*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	/** Original DurationFormula.getOverrun: above 2^(1+40/4) rad/s, several operations per tick. */
	public int getNumberConsecutiveOperations() {
		int threshold = (int)ReikaMathLibrary.intpow(2, 1+DURATION_BASE/DURATION_SCALE);
		return 1+(omega >= threshold ? (int)(ReikaMathLibrary.logbase(omega, 2)-DURATION_BASE/DURATION_SCALE) : 0);
	}

	public int getContactDamage() {
		return 4;
	}

	public boolean canDealDamage() {
		return this.isRunning();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (this.isRunning())
			phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return this.hasWood() ? 0 : 15;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		if (!inv.getStackInSlot(0).isEmpty())
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(0).copy());
	}

	@Override
	public void clearContent() {
		inv.setStackInSlot(0, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("cutting", cuttingTree);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		cuttingTree = NBT.getBoolean("cutting");
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
