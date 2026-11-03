/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorSounds;
import reika.rotarycraft.api.power.PowerTransferHelper;
import reika.rotarycraft.api.power.ShaftPowerReceiver;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fission.TileEntityCPU with ReactorControlLayout - every 64 ticks it searches the connected
 * reactor parts (6-connected, within 12 blocks sideways and 4 up/down) for control rods. It needs shaft power from any side, 1024 W per rod;
 * with less (after the first second) it SCRAMs, and also when it gets hotter than 800 C while it has 4 times the needed power. The GUI
 * raises/lowers all rods or toggles one rod stack; the comparator output is 15 times the share of inserted rods. Cores and rods linked with the CPU
 * remote control are temperature checks: each tick one of them, picked at random, also SCRAMs above 800 C.
 */
public class CpuBlockEntity extends ReactorBlockEntity implements ShaftPowerReceiver, Temperatured, ReactorTyped, NeutronTile, ReactorBlock, ReactorBlockEntity.ControlOrCPU {

	public static final int POWERPERROD = 1024;
	public static final int RANGE = 12;

	private final Map<BlockPos, ControlRodBlockEntity> rods = new LinkedHashMap<>();
	/** Rod positions as synced to the client (relative to the CPU); the client looks the rods up in its own world. */
	private final List<BlockPos> rodPositions = new ArrayList<>();

	private int omega;
	private int torque;
	private long power;
	private boolean searched;
	private int redstoneUpdate = 200;
	/** Original temperatureChecks (TemperatureMonitor): linked cores and rods whose temperature also triggers the SCRAM. */
	private final List<BlockPos> temperatureChecks = new ArrayList<>();

	public CpuBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.CPU.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Reactor CPU";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (this.thermalTick() && !world.isClientSide)
			this.updateTemperature(world, pos);
		if (!world.isClientSide) {
			if (world.getGameTime()%64 == 0)
				searched = false;
			if (!searched)
				this.findRods(world, pos);
		}

		if (!PowerTransferHelper.checkPowerFromAllSides(this, true))
			this.noInputMachine();

		if (world.isClientSide)
			return;

		if (power < this.getMinPower() && this.getTicksExisted() > 20)
			this.SCRAM();

		if (!rods.isEmpty()) {
			if ((temperature > this.getMaxTemperature() || (!temperatureChecks.isEmpty() &&
					this.getCheckedTemperature(world, temperatureChecks.get(rand.nextInt(temperatureChecks.size()))) > this.getMaxTemperature()))
					&& power >= this.getMinPower()*4)
				this.SCRAM();
		}

		if (redstoneUpdate > 0) {
			redstoneUpdate--;
			if (redstoneUpdate <= 0)
				world.updateNeighbourForOutputSignal(pos, state.getBlock());
		}
	}

	/** Original BlockArray.recursiveAddCallbackWithBounds over ReactorBlock tiles, then every control rod found joins the layout. */
	private void findRods(Level world, BlockPos pos) {
		rods.clear();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(pos);
		seen.add(pos);
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			if (world.getBlockEntity(p) instanceof ControlRodBlockEntity rod)
				rods.put(p.immutable(), rod);
			for (Direction d : Direction.values()) {
				BlockPos n = p.relative(d);
				if (Math.abs(n.getX()-pos.getX()) > RANGE || Math.abs(n.getY()-pos.getY()) > 4 || Math.abs(n.getZ()-pos.getZ()) > RANGE)
					continue;
				if (seen.add(n) && world.isLoaded(n) && world.getBlockEntity(n) instanceof ReactorBlock)
					queue.add(n);
			}
		}
		searched = true;
		rodPositions.clear();
		for (BlockPos p : rods.keySet())
			rodPositions.add(p.subtract(pos));
		this.syncAllData();
	}

	/** Original TemperatureMonitor.getTemperature (a part that is gone counts as cold here instead of crashing). */
	private int getCheckedTemperature(Level world, BlockPos p) {
		return world.getBlockEntity(p) instanceof LinkableReactorCore te ? te.getTemperature() : 0;
	}

	public void addTemperatureCheck(LinkableReactorCore te) {
		temperatureChecks.add(((net.minecraft.world.level.block.entity.BlockEntity)te).getBlockPos().immutable());
		te.link(this);
		this.setChanged();
	}

	public void removeTemperatureCheck(LinkableReactorCore te) {
		temperatureChecks.remove(((net.minecraft.world.level.block.entity.BlockEntity)te).getBlockPos());
		this.setChanged();
	}

	public int getNumberTemperatureChecks() {
		return temperatureChecks.size();
	}

	/** Original ReactorControlLayout.removeControlRod (called by a broken rod). */
	public void removeControlRod(BlockPos p) {
		rods.remove(p);
	}

	public void SCRAM() {
		int iter = 0;
		for (ControlRodBlockEntity rod : this.getAllRods()) {
			rod.drop(iter == 0);
			iter++;
		}
		if (redstoneUpdate == 0)
			redstoneUpdate = 7;
	}

	public void lowerAllRods() {
		for (ControlRodBlockEntity te : this.getAllRods())
			te.setActive(true, false);
		ReactorSounds.play(level, worldPosition, ReactorSounds.CONTROL.get(), 1, 1.3F);
		redstoneUpdate = 30;
	}

	public void raiseAllRods() {
		for (ControlRodBlockEntity te : this.getAllRods())
			te.setActive(false, false);
		ReactorSounds.play(level, worldPosition, ReactorSounds.CONTROL.get(), 1, 1.3F);
		redstoneUpdate = 30;
	}

	/** The rods still present (the original TileEntityCache drops removed tiles the same way). */
	public List<ControlRodBlockEntity> getAllRods() {
		List<ControlRodBlockEntity> li = new ArrayList<>();
		for (ControlRodBlockEntity te : rods.values())
			if (!te.isRemoved())
				li.add(te);
		return li;
	}

	public int getNumberRods() {
		return rodPositions.size();
	}

	/** Client and server: the rod at that position relative to the CPU, if it belongs to the layout. */
	public ControlRodBlockEntity getControlRodAtRelativePosition(int x, int y, int z) {
		BlockPos rel = new BlockPos(x, y, z);
		if (!rodPositions.contains(rel))
			return null;
		return level.getBlockEntity(worldPosition.offset(rel)) instanceof ControlRodBlockEntity te ? te : null;
	}

	public int getMinX() {
		return rodPositions.stream().mapToInt(BlockPos::getX).min().orElse(Integer.MAX_VALUE);
	}

	public int getMaxX() {
		return rodPositions.stream().mapToInt(BlockPos::getX).max().orElse(Integer.MIN_VALUE);
	}

	public int getMinY() {
		return rodPositions.stream().mapToInt(BlockPos::getY).min().orElse(Integer.MAX_VALUE);
	}

	public int getMaxY() {
		return rodPositions.stream().mapToInt(BlockPos::getY).max().orElse(Integer.MIN_VALUE);
	}

	public int getMinZ() {
		return rodPositions.stream().mapToInt(BlockPos::getZ).min().orElse(Integer.MAX_VALUE);
	}

	public int getMaxZ() {
		return rodPositions.stream().mapToInt(BlockPos::getZ).max().orElse(Integer.MIN_VALUE);
	}

	/** Original ReactorControlLayout.getDisplayColorAtRelativePosition. */
	public int getDisplayColorAtRelativePosition(int x, int y, int z) {
		ControlRodBlockEntity rod = this.getControlRodAtRelativePosition(x, y, z);
		if (rod != null) {
			if (power >= this.getMinPower())
				return rod.isActive() ? 0x00ff00 : 0xff0000;
			return 0xa0a0a0;
		}
		return 0x6a6a6a;
	}

	public long getMinPower() {
		return (long)POWERPERROD*this.getNumberRods();
	}

	public int countLoweredRods() {
		int count = 0;
		for (ControlRodBlockEntity rod : this.getAllRods())
			if (rod.isActive())
				count++;
		return count;
	}

	@Override
	public int getRedstoneOverride() {
		return rods.isEmpty() ? 0 : 15*this.countLoweredRods()/rods.size();
	}

	@Override
	public int getOmega() {
		return omega;
	}

	@Override
	public int getTorque() {
		return torque;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public void setOmega(int omega) {
		this.omega = omega;
	}

	@Override
	public void setTorque(int torque) {
		this.torque = torque;
	}

	@Override
	public void setPower(long power) {
		this.power = power;
	}

	@Override
	public boolean canReadFrom(Direction dir) {
		return true;
	}

	@Override
	public boolean isReceiving() {
		return true;
	}

	@Override
	public void noInputMachine() {
		torque = omega = 0;
		power = 0;
	}

	@Override
	public int getMinTorque(int available) {
		return 1;
	}

	@Override
	public int getIORenderAlpha() {
		return 0;
	}

	@Override
	public void setIORenderAlpha(int io) {}

	@Override
	public int getMaxTemperature() {
		return 800;
	}

	@Override
	public boolean canDumpHeatInto(LiquidStates liq) {
		return liq != LiquidStates.EMPTY;
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		return false;
	}

	@Override
	public ReactorType getReactorType() {
		return ReactorType.FISSION;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("speed", omega);
		NBT.putInt("trq", torque);
		NBT.putLong("pwr", power);
		NBT.putInt("redsu", redstoneUpdate);
		ListTag li = new ListTag();
		for (BlockPos p : rodPositions)
			li.add(LongTag.valueOf(p.asLong()));
		NBT.put("rods", li);
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		ListTag li = new ListTag();
		for (BlockPos p : temperatureChecks)
			li.add(LongTag.valueOf(p.asLong()));
		NBT.put("checks", li);
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		temperatureChecks.clear();
		for (Tag t : NBT.getList("checks", Tag.TAG_LONG))
			temperatureChecks.add(BlockPos.of(((LongTag)t).getAsLong()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		omega = NBT.getInt("speed");
		torque = NBT.getInt("trq");
		power = NBT.getLong("pwr");
		redstoneUpdate = NBT.getInt("redsu");
		rodPositions.clear();
		for (Tag t : NBT.getList("rods", Tag.TAG_LONG))
			rodPositions.add(BlockPos.of(((LongTag)t).getAsLong()));
	}
}
