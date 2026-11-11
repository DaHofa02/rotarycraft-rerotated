/*******************************************************************************
 * Smoke detector: beeps and gives a redstone signal while fire burns nearby. Ported from Reika.RotaryCraft.TileEntities.TileEntitySmokeDetector
 * (RotaryCraft 1.7.10 by Reika Kalseki); private NeoForge 1.21.1 port, not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotarySounds;

public class SmokeDetectorBlockEntity extends SpringPoweredBlockEntity {

	/** Original: ReikaWorldHelper.findNearBlock(world, x, y, z, 8, Blocks.fire) - a cube of 17 blocks edge length. */
	public static final int SEARCH_RANGE = 8;

	private boolean isAlarm = false;
	private boolean isLowBatt = false;
	private int unwindtick = 0;
	public int soundDelay = -1;

	public SmokeDetectorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SMOKE_DETECTOR.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Smoke Detector";
	}

	public boolean isAlarming() {
		return isAlarm;
	}

	public boolean isLowBattery() {
		return isLowBatt;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		tickcount++;
		unwindtick++;
		if (world.isClientSide || !this.hasCoil())
			return;
		if (unwindtick >= this.getUnwindTime()) {
			this.decrementCharge();
			unwindtick = 0;
		}
		boolean fire = findFire(world, pos);
		if (fire != isAlarm) {
			isAlarm = fire;
			world.updateNeighborsAt(pos, state.getBlock());
			this.setChanged();
		}
		isLowBatt = this.lowBattery();
		soundDelay = isAlarm ? 4 : isLowBatt ? 600 : -1;
		if (tickcount >= soundDelay && soundDelay != -1) {
			tickcount = 0;
			RotarySounds.play(world, pos, RotarySounds.SMOKE.get(), 0.1F, 1);
		}
	}

	private static boolean findFire(Level world, BlockPos pos) {
		for (BlockPos c : BlockPos.betweenClosed(pos.offset(-SEARCH_RANGE, -SEARCH_RANGE, -SEARCH_RANGE), pos.offset(SEARCH_RANGE, SEARCH_RANGE, SEARCH_RANGE)))
			if (world.isLoaded(c) && world.getBlockState(c).is(BlockTags.FIRE))
				return true;
		return false;
	}

	/** Original RangedEffect.getRange: log2(charge^2), at most 8 (shown by the tools only; the search itself always covers 8 blocks). */
	public int getRange() {
		if (!this.hasCoil())
			return 0;
		long dmg = this.getCoilCharge();
		return Math.min(8, (int)ReikaMathLibrary.logbase(dmg*dmg, 2));
	}

	public boolean lowBattery() {
		return this.hasCoil() && !isCreativeMode && this.getCoilCharge() <= 8;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public int getBaseDischargeTime() {
		return 1200;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("alarm", isAlarm);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		isAlarm = NBT.getBoolean("alarm");
	}
}
