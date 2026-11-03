/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorSounds;
import reika.rotarycraft.lib.math.RandomHelper;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fission.TileEntityControlRod - the rods move one step per tick between -5 (inserted) and 20
 * (retracted), a SCRAM drops them 7 per tick. Only fully inserted rods absorb neutrons (60 %). Toggling one rod by hand (CPU GUI) toggles
 * the whole vertical stack. The CPU it belongs to forgets it when it is broken (the CPU searches again every 64 ticks anyway).
 */
public class ControlRodBlockEntity extends ReactorBlockEntity implements LinkableReactorCore, ReactorTyped, NeutronTile, ReactorBlock, ReactorBlockEntity.ControlOrCPU {

	public static final int MINOFFSET = -5;
	public static final int MAXOFFSET = 20;

	private boolean lowered = true;
	private Motions motion;
	private int rodOffset = MINOFFSET;

	public ControlRodBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.CONTROL_ROD.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Control Rods";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.moveRods();
		if (this.thermalTick() && !world.isClientSide)
			this.updateTemperature(world, pos);
	}

	private void moveRods() {
		if (motion != null)
			rodOffset += motion.stepHeight;
		if (rodOffset <= MINOFFSET || rodOffset >= MAXOFFSET) {
			motion = null;
			rodOffset = Math.max(MINOFFSET, rodOffset);
			rodOffset = Math.min(MAXOFFSET, rodOffset);
			lowered = rodOffset == MINOFFSET;
		}
	}

	public void toggle(boolean sound, boolean spread) {
		motion = lowered ? Motions.RAISING : Motions.LOWERING;
		if (spread) {
			for (Direction d : new Direction[]{Direction.UP, Direction.DOWN}) {
				BlockPos p = worldPosition.relative(d);
				while (level.getBlockEntity(p) instanceof ControlRodBlockEntity tc) {
					tc.toggle(false, false);
					p = p.relative(d);
				}
			}
		}
		if (sound)
			ReactorSounds.play(level, worldPosition, ReactorSounds.CONTROL.get(), 1, 1.3F);
		this.syncAllData();
	}

	public void setActive(boolean active, boolean sound) {
		motion = active ? Motions.LOWERING : Motions.RAISING;
		if (sound)
			ReactorSounds.play(level, worldPosition, ReactorSounds.CONTROL.get(), 1, 1.3F);
		this.syncAllData();
	}

	public void drop(boolean sound) {
		if (sound && rodOffset > MINOFFSET && motion != Motions.SCRAM)
			ReactorSounds.play(level, worldPosition, ReactorSounds.SCRAM.get(), 1, 1F);
		motion = Motions.SCRAM;
		this.syncAllData();
	}

	public boolean isActive() {
		return lowered && rodOffset == MINOFFSET;
	}

	public int getRodPosition() {
		return rodOffset;
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		return this.isActive() && RandomHelper.doWithChance(60, rand);
	}

	@Override
	public int getMaxTemperature() {
		return 0;
	}

	@Override
	public boolean canDumpHeatInto(LiquidStates liq) {
		return liq.isWater();
	}

	@Override
	public ReactorType getReactorType() {
		return ReactorType.FISSION;
	}

	/** Original CPU coordinate set by the CPU remote control. */
	@javax.annotation.Nullable
	private BlockPos cpu;

	@Override
	public void link(CpuBlockEntity te) {
		cpu = te.getBlockPos().immutable();
		this.setChanged();
	}

	/** Original breakBlock: leaves the linked CPU's layout and temperature checks. */
	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		if (cpu != null && world.getBlockEntity(cpu) instanceof CpuBlockEntity te) {
			te.removeControlRod(pos);
			te.removeTemperatureCheck(this);
		}
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		if (cpu != null)
			NBT.putLong("cpu", cpu.asLong());
		NBT.putBoolean("down", lowered);
		if (motion != null)
			NBT.putInt("motion", motion.ordinal());
		NBT.putInt("rodoffset", rodOffset);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		cpu = NBT.contains("cpu") ? BlockPos.of(NBT.getLong("cpu")) : null;
		lowered = NBT.getBoolean("down");
		motion = NBT.contains("motion") ? Motions.values()[NBT.getInt("motion")] : null;
		rodOffset = NBT.getInt("rodoffset");
	}

	private enum Motions {
		RAISING(1),
		LOWERING(-1),
		SCRAM(-7);

		public final int stepHeight;

		Motions(int dh) {
			stepHeight = dh;
		}
	}
}
