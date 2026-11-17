/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.menu.SpyCamViewMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Surveying.TileEntitySpyCam - runs on a wound coil (one charge per 120 ticks times the
 * stiffness) and keeps a top-down map 49x49 around itself: the highest block below its height in each column, and the mobs standing on
 * that surface (by their 1.7.10 entity ids, for the GUI's icons). Three dyes set its colour code; a screen with the same three dyes opens
 * the map (GuiSpyCam) while the coil runs. Right-clicked directly it shows its coil and dye slots.
 * Changes: the dyes sit in their own 3-slot inventory beside the coil slot (automation reaches only the coil). Entities with no 1.7.10 id
 * are not marked (the original marked only those with an id, too).
 */
public class SpyCamBlockEntity extends SpringPoweredBlockEntity implements RemoteControlled {

	public static final int MAXRANGE = 24;
	private static final int N = MAXRANGE*2+1;

	/** EntityList ids of 1.7.10 (mobicons.png is laid out by them). */
	private static final Map<EntityType<?>, Integer> LEGACY_IDS = Map.ofEntries(Map.entry(EntityType.CREEPER, 50), Map.entry(EntityType.SKELETON, 51),
			Map.entry(EntityType.SPIDER, 52), Map.entry(EntityType.GIANT, 53), Map.entry(EntityType.ZOMBIE, 54), Map.entry(EntityType.SLIME, 55),
			Map.entry(EntityType.GHAST, 56), Map.entry(EntityType.ZOMBIFIED_PIGLIN, 57), Map.entry(EntityType.ENDERMAN, 58), Map.entry(EntityType.CAVE_SPIDER, 59),
			Map.entry(EntityType.SILVERFISH, 60), Map.entry(EntityType.BLAZE, 61), Map.entry(EntityType.MAGMA_CUBE, 62), Map.entry(EntityType.ENDER_DRAGON, 63),
			Map.entry(EntityType.WITHER, 64), Map.entry(EntityType.BAT, 65), Map.entry(EntityType.WITCH, 66), Map.entry(EntityType.PIG, 90),
			Map.entry(EntityType.SHEEP, 91), Map.entry(EntityType.COW, 92), Map.entry(EntityType.CHICKEN, 93), Map.entry(EntityType.SQUID, 94),
			Map.entry(EntityType.WOLF, 95), Map.entry(EntityType.MOOSHROOM, 96), Map.entry(EntityType.SNOW_GOLEM, 97), Map.entry(EntityType.OCELOT, 98),
			Map.entry(EntityType.IRON_GOLEM, 99), Map.entry(EntityType.HORSE, 100), Map.entry(EntityType.VILLAGER, 120));

	private final ItemStackHandler dyes = new ItemStackHandler(3) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return is.getItem() instanceof DyeItem;
		}

		@Override
		protected void onContentsChanged(int slot) {
			SpyCamBlockEntity.this.setChanged();
		}
	};
	private int[] colors = {-1, -1, -1};
	private boolean on;
	private int tickcount2;
	private final BlockState[][] topBlocks = new BlockState[N][N];
	private final int[][] topY = new int[N][N];
	private int[][] mobs = new int[N][N];
	private List<LivingEntity> inzone = new ArrayList<>();

	public SpyCamBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SPY_CAM.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	@Override
	public int getBaseDischargeTime() {
		return 120;
	}

	public ItemStackHandler getDyes() {
		return dyes;
	}

	public static int legacyId(EntityType<?> t) {
		return LEGACY_IDS.getOrDefault(t, 0);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.setColors();
		if (!this.hasCoil()) {
			on = false;
			return;
		}
		on = true;
		tickcount2++;
		if (tickcount2 > this.getUnwindTime()) {
			if (!world.isClientSide)
				this.decrementCharge();
			tickcount2 = 0;
		}
		this.getTopBlocks(world, pos);
		this.getMobs(world, pos);
	}

	/** Original setColors: each dye slot's colour, -1 if empty (the original then also switched it off, but the coil check after it switches it on again). */
	private void setColors() {
		for (int i = 0; i < 3; i++) {
			ItemStack is = dyes.getStackInSlot(i);
			colors[i] = is.getItem() instanceof DyeItem d ? d.getDyeColor().getId() : -1;
		}
	}

	@Override
	public int[] getColors() {
		return colors.clone();
	}

	public boolean isOn() {
		return on;
	}

	private void getMobs(Level world, BlockPos pos) {
		mobs = new int[N][N];
		int range = this.getRange();
		AABB zone = new AABB(pos.getX()-range, world.getMinBuildHeight(), pos.getZ()-range, pos.getX()+1+range, pos.getY()+1, pos.getZ()+1+range);
		inzone = world.getEntitiesOfClass(LivingEntity.class, zone);
		for (LivingEntity ent : inzone) {
			int ex = Mth.floor(ent.getX())-pos.getX();
			int ez = Mth.floor(ent.getZ())-pos.getZ();
			int id = legacyId(ent.getType());
			if (id > 0 && Math.abs(ex) < range+1 && Math.abs(ez) < range+1 && ent.getY() >= findTopBlockBelowY(world, Mth.floor(ent.getX()), pos.getY(), Mth.floor(ent.getZ())))
				mobs[ez+range][ex+range] = id;
		}
	}

	/** DragonAPI ReikaWorldHelper.findTopBlockBelowY: the first non-air block from y down. */
	public static int findTopBlockBelowY(Level world, int x, int y, int z) {
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, y, z);
		while (m.getY() > world.getMinBuildHeight() && world.getBlockState(m).isAir())
			m.move(Direction.DOWN);
		return m.getY();
	}

	private void getTopBlocks(Level world, BlockPos pos) {
		int range = this.getRange();
		for (int i = -range; i <= range; i++)
			for (int j = -range; j <= range; j++) {
				int topy = findTopBlockBelowY(world, pos.getX()+i, pos.getY(), pos.getZ()+j);
				topY[i+range][j+range] = topy;
				topBlocks[i+range][j+range] = world.getBlockState(new BlockPos(pos.getX()+i, topy, pos.getZ()+j));
			}
	}

	public List<LivingEntity> getEntities() {
		return Collections.unmodifiableList(inzone);
	}

	public int[] getBounds() {
		return new int[] {this.getMaxRange()-this.getRange(), this.getMaxRange()+this.getRange()};
	}

	/** Mob id at map row i (z), column j (x). */
	public int getMobAt(int i, int j) {
		return mobs[i][j];
	}

	/** Top block of column (i = x, j = z), its map colour (the original: BlockColorMapper). */
	public int getTopBlockColorAt(int i, int j) {
		BlockState s = topBlocks[i][j];
		return s == null || level == null ? 0 : s.getMapColor(level, worldPosition).col;
	}

	public int getHeightAt(int i, int j) {
		return topY[i][j];
	}

	public int getRange() {
		return this.getMaxRange();
	}

	public int getMaxRange() {
		return MAXRANGE;
	}

	/** Original activate: the map GUI, while it runs. */
	@Override
	public void activate(ServerPlayer ep) {
		if (on)
			ep.openMenu(new SimpleMenuProvider((id, inv, p) -> new SpyCamViewMenu(id, inv, this), this.getBlockState().getBlock().getName()),
					buf -> buf.writeBlockPos(worldPosition));
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		super.onBlockBroken(world, pos);
		for (int i = 0; i < 3; i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), dyes.getStackInSlot(i).copy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putIntArray("color", colors);
		NBT.put("dyes", dyes.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		int[] c = NBT.getIntArray("color");
		if (c.length == 3)
			colors = c;
		dyes.deserializeNBT(registries, NBT.getCompound("dyes"));
	}
}
