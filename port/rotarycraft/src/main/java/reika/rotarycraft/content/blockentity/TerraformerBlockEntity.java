/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The terraformer, written for the port with its own transformation table. While it gets a redstone signal and at least 1024 W (from any
 * side, all at the same speed), every max(1, 800 - 40*log2(speed+1)) ticks it turns one more 4x4 biome cell of the square around it (radius
 * set in its GUI, up to 32 blocks) into the chosen target biome - if it has that target's power, its item (used up per cell) and water.
 * Oceans, rivers and mushroom fields are left alone; cells already of the target are skipped.
 * Targets: forest (oak sapling, 32768 W), plains (wheat seeds, 32768 W), desert (sand, 65536 W), snowy plains (snowball, 65536 W),
 * swamp (lily pad and 500 mB water, 131072 W), jungle (jungle sapling and 250 mB water, 131072 W).
 * Changes: the original took its area from BuildCraft landmarks or the target selector and had a large table of biome-to-biome steps;
 * the block changes of its "modify blocks" option are left out.
 */
public class TerraformerBlockEntity extends PowerReceiverBlockEntity implements PipeConnector {

	public static final int MAX_RADIUS = 32;
	public static final int WATER_CAPACITY = 16000;

	public enum Target {
		FOREST(Biomes.FOREST, Items.OAK_SAPLING, 0, 32768),
		PLAINS(Biomes.PLAINS, Items.WHEAT_SEEDS, 0, 32768),
		DESERT(Biomes.DESERT, Items.SAND, 0, 65536),
		SNOWY_PLAINS(Biomes.SNOWY_PLAINS, Items.SNOWBALL, 0, 65536),
		SWAMP(Biomes.SWAMP, Items.LILY_PAD, 500, 131072),
		JUNGLE(Biomes.JUNGLE, Items.JUNGLE_SAPLING, 250, 131072);

		public final ResourceKey<Biome> biome;
		public final Item item;
		public final int water;
		public final long power;

		Target(ResourceKey<Biome> b, Item i, int w, long p) {
			biome = b;
			item = i;
			water = w;
			power = p;
		}
	}

	private final ItemStackHandler inv = new ItemStackHandler(18) {
		@Override
		protected void onContentsChanged(int slot) {
			TerraformerBlockEntity.this.setChanged();
		}
	};
	private final FluidTank tank = new FluidTank(WATER_CAPACITY, fs -> fs.is(net.minecraft.world.level.material.Fluids.WATER));
	private Target target = Target.FOREST;
	private int radius = 4;
	private int tickcount;
	public int changed;

	public TerraformerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.TERRAFORMER.get(), pos, state, 1, 1, 1024);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public IItemHandler getItemHandler(@Nullable Direction side) {
		return inv;
	}

	public Target getTarget() {
		return target;
	}

	public void setTarget(Target t) {
		target = t;
		this.syncAllData();
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int r) {
		radius = Math.max(0, Math.min(MAX_RADIUS, r));
		this.syncAllData();
	}

	public int getWater() {
		return tank.getFluidAmount();
	}

	public void addWater(int amt) {
		tank.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, amt), IFluidHandler.FluidAction.EXECUTE);
	}

	public int getOperationTime() {
		return (int)Math.max(1, 800-40*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	public static boolean isChangeable(Holder<Biome> b) {
		return !b.is(BiomeTags.IS_OCEAN) && !b.is(BiomeTags.IS_RIVER) && !b.is(Biomes.MUSHROOM_FIELDS) && b.is(BiomeTags.IS_OVERWORLD);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.readPowerSummative();
		tickcount++;
		if (!world.hasNeighborSignal(pos) || power < MINPOWER || tickcount < this.getOperationTime())
			return;
		tickcount = 0;
		this.operate((ServerLevel)world);
	}

	/** The cells (by their lowest corner) within the radius that are not yet the target, in a random order of the level's random. */
	private List<BlockPos> openCells(ServerLevel world, Holder<Biome> to) {
		List<BlockPos> li = new ArrayList<>();
		int y = worldPosition.getY();
		for (int x = (worldPosition.getX()-radius) >> 2; x <= (worldPosition.getX()+radius) >> 2; x++)
			for (int z = (worldPosition.getZ()-radius) >> 2; z <= (worldPosition.getZ()+radius) >> 2; z++) {
				BlockPos p = new BlockPos(x << 2, y, z << 2);
				Holder<Biome> b = cellBiome(world, p);
				if (!b.is(to) && isChangeable(b) && world.isLoaded(p))
					li.add(p);
			}
		return li;
	}

	private int findItem(Item item) {
		for (int i = 0; i < inv.getSlots(); i++)
			if (inv.getStackInSlot(i).is(item))
				return i;
		return -1;
	}

	private void operate(ServerLevel world) {
		if (power < target.power || tank.getFluidAmount() < target.water)
			return;
		int slot = this.findItem(target.item);
		if (slot < 0)
			return;
		Holder<Biome> to = world.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(target.biome);
		List<BlockPos> cells = this.openCells(world, to);
		if (cells.isEmpty())
			return;
		BlockPos cell = cells.get(world.random.nextInt(cells.size()));
		setBiomeColumn(world, cell, to);
		inv.extractItem(slot, 1, false);
		if (target.water > 0)
			tank.drain(target.water, IFluidHandler.FluidAction.EXECUTE);
		changed++;
	}

	/** The biome of the 4x4 cell itself (Level.getBiome blurs between neighbouring cells). */
	public static Holder<Biome> cellBiome(Level world, BlockPos p) {
		return world.getNoiseBiome(QuartPos.fromBlock(p.getX()), QuartPos.fromBlock(p.getY()), QuartPos.fromBlock(p.getZ()));
	}

	/** Sets the whole 4x4 biome column (every height) at the cell to the biome and sends the chunk's biomes to the players. */
	public static void setBiomeColumn(ServerLevel world, BlockPos cell, Holder<Biome> to) {
		LevelChunk chunk = world.getChunkAt(cell);
		int qx = QuartPos.fromBlock(cell.getX()), qz = QuartPos.fromBlock(cell.getZ());
		chunk.fillBiomesFromNoise((x, y, z, sampler) -> x == qx && z == qz ? to : chunk.getNoiseBiome(x, y, z), world.getChunkSource().randomState().sampler());
		chunk.setUnsaved(true);
		world.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return Flow.INPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return tank.fill(resource, action);
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return tank;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
		NBT.putInt("water", tank.getFluidAmount());
		NBT.putInt("target", target.ordinal());
		NBT.putInt("radius", radius);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
		int w = NBT.getInt("water");
		tank.setFluid(w > 0 ? new FluidStack(net.minecraft.world.level.material.Fluids.WATER, w) : FluidStack.EMPTY);
		target = Target.values()[Math.floorMod(NBT.getInt("target"), Target.values().length)];
		radius = NBT.contains("radius") ? NBT.getInt("radius") : 4;
	}
}
