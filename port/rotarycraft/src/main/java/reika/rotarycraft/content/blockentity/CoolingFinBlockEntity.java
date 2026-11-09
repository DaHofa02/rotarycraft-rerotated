/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.api.interfaces.Screwdriverable;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Auxiliary.TileEntityCoolingFin - attached to the block it was placed against; every 20
 * (half: 40, quarter: 80) ticks it moves its own temperature towards the ambient temperature (up 1, down 2; water next to it -5,
 * ice -15, lava makes it 2600) and, if the target machine is hotter and can be cooled with fins, takes 1 C from it. The screwdriver
 * shows the target; sneaking switches the setting. The IC2 reactor cooling is not ported (no IC2).
 */
public class CoolingFinBlockEntity extends RotaryCraftBlockEntity implements TemperatureMachine, Screwdriverable {

	public enum FinSettings {
		FULL(20),
		HALF(40),
		QUARTER(80);

		public final int tickRate;
		public final float rawMultiplier;

		FinSettings(int n) {
			tickRate = n;
			rawMultiplier = 20F/tickRate;
		}

		public FinSettings next() {
			return values()[(this.ordinal()+1)%values().length];
		}
	}

	private BlockPos target = BlockPos.ZERO;
	private int temperature;
	private int finTick;
	public int ticks = 512;
	public FinSettings setting = FinSettings.FULL;

	public CoolingFinBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.COOLING_FIN.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Cooling Fin";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		finTick++;
		if (ticks > 0)
			ticks -= 8;
		target = pos.relative(getTargetDirection(this.getBlockMetadata()));
		if (finTick < setting.tickRate)
			return;
		finTick = 0;
		this.updateTemperature(world, pos);
		BlockEntity te = world.getBlockEntity(target);
		if (te instanceof TemperatureMachine tr && tr.canBeCooledWithFins()) {
			int temp = tr.getTemperature();
			if (temp > temperature) {
				temperature++;
				tr.addTemperature(-1);
			}
		}
	}

	/** Original getTargetSide: 0 below, 1 above, 2 north, 3 west, 4 south, 5 east. */
	public static Direction getTargetDirection(int meta) {
		return switch (meta) {
			case 1 -> Direction.UP;
			case 2 -> Direction.NORTH;
			case 3 -> Direction.WEST;
			case 4 -> Direction.SOUTH;
			case 5 -> Direction.EAST;
			default -> Direction.DOWN;
		};
	}

	private void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		boolean water = false, lava = false, ice = false;
		for (Direction d : Direction.values()) {
			BlockState s = world.getBlockState(pos.relative(d));
			water |= s.getFluidState().is(FluidTags.WATER);
			lava |= s.getFluidState().is(FluidTags.LAVA);
			ice |= s.is(Blocks.ICE) || s.is(Blocks.FROSTED_ICE);
		}
		if (water)
			Tamb -= 5;
		if (lava)
			Tamb = 2600;
		if (ice)
			Tamb -= 15;
		if (Tamb > temperature)
			temperature++;
		else
			temperature = Math.max(Tamb, temperature-2);
	}

	public BlockPos getTarget() {
		return target;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public void addTemperature(int temp) {
		temperature += temp;
	}

	@Override
	public int getTemperature() {
		return temperature;
	}

	@Override
	public void setTemperature(int temp) {
		temperature = temp;
	}

	@Override
	public int getMaxTemperature() {
		return 2500;
	}

	@Override
	public boolean canBeCooledWithFins() {
		return false;
	}

	/** Original ItemScrewdriver: sneaking switches the setting; otherwise the fin turns through its six sides. Both show the target. */
	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		ticks = 512;
		setting = setting.next();
		this.syncAllData();
		return true;
	}

	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		ticks = 512;
		int meta = this.getBlockMetadata();
		this.setBlockMetadata(meta < 5 ? meta+1 : 0);
		return true;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("ticks", ticks);
		NBT.putInt("setting", setting.ordinal());
		NBT.putInt("temp", temperature);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		ticks = NBT.getInt("ticks");
		setting = FinSettings.values()[Math.min(NBT.getInt("setting"), FinSettings.values().length-1)];
		temperature = NBT.getInt("temp");
	}

	/** Original allowHeatExtraction (HeatConduction): TileEntityCoolingFin, true. */
	@Override
	public boolean allowHeatExtraction() {
		return true;
	}
}
