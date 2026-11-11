/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.TileEntityPlayerDetector (MachineRegistry.PLAYERDETECTOR). Power from below; emits a
 * redstone signal of 15 once a player has been within range for the reaction time. Range: power / {@link #FALLOFF}, at most
 * {@link #MAX_RANGE} and at most the range set in the GUI; reaction time: {@link #BASESPEED} - omega / {@link #SPEEDFACTOR} ticks,
 * at least 1.
 * <p>
 * Deviations: spectators (unknown to 1.7.10) are not detected; neighbours get a block update when the signal changes instead of every
 * tick.
 */
public class PlayerDetectorBlockEntity extends PowerReceiverBlockEntity {

	public static final int FALLOFF = 128;
	public static final int SPEEDFACTOR = 32;
	public static final int BASESPEED = 100;
	/** Original Math.max(64, ConfigRegistry.DETECTORRANGE), default 128. */
	public static final int MAX_RANGE = 128;

	/** Original field without a setter in the original's GUI: count the players (0-15) into {@link #powerLevel} instead of switching. */
	public boolean analog = false;
	private boolean isActive = false;
	public int powerLevel = 0;
	public int selectedrange;
	private int ticksdetected = 0;

	public PlayerDetectorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.PLAYER_DETECTOR.get(), pos, state, 1, 1, 1);
	}

	@Override
	public String getName() {
		return "Player Detector";
	}

	public boolean isActive() {
		return isActive;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		read = Direction.DOWN; //original getPowerBelow
		this.readPower();
		boolean was = isActive;
		if (power < MINPOWER) {
			isActive = false;
			ticksdetected = 0;
		}
		else if (!analog) {
			powerLevel = 0;
			if (this.countPlayers(world, pos) > 0) {
				ticksdetected++;
				if (ticksdetected >= this.getReactionTime())
					isActive = true;
			}
			else {
				isActive = false;
				ticksdetected = 0;
			}
		}
		else {
			isActive = false;
			int level = Math.min(15, this.countPlayers(world, pos));
			if (level > 0) {
				ticksdetected++;
				if (ticksdetected >= this.getReactionTime())
					powerLevel = level;
			}
			else {
				powerLevel = 0;
				ticksdetected = 0;
			}
		}
		if (was != isActive) {
			world.updateNeighborsAt(pos, state.getBlock());
			this.setChanged();
		}
	}

	public int getReactionTime() {
		return Math.max(1, BASESPEED-omega/SPEEDFACTOR);
	}

	private int countPlayers(Level world, BlockPos pos) {
		return world.getEntitiesOfClass(Player.class, new AABB(pos).inflate(this.getRange()), p -> !p.isSpectator()).size();
	}

	public int getRange() {
		return Math.min(this.getMaxRange(), selectedrange);
	}

	public int getMaxRange() {
		return (int)Math.min(power/FALLOFF, MAX_RANGE);
	}

	/** Original PacketRegistry.DETECTOR: the range typed into the GUI (up to three digits). */
	public void setSelectedRange(int range) {
		selectedrange = Math.max(0, Math.min(999, range));
		this.setChanged();
		this.syncAllData();
	}

	@Override
	protected void writeSyncTag(CompoundTag tag, HolderLookup.Provider regs) {
		super.writeSyncTag(tag, regs);
		tag.putInt("range", selectedrange);
	}

	@Override
	protected void readSyncTag(CompoundTag tag, HolderLookup.Provider regs) {
		super.readSyncTag(tag, regs);
		selectedrange = tag.getInt("range");
	}

	@Override
	public int getRedstoneOverride() {
		return isActive ? 15 : 0;
	}
}
