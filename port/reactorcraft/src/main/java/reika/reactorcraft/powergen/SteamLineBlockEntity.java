/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.reactor.ReactorBoilerBlockEntity;
import reika.reactorcraft.reactor.ReactorType;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: Reika.ReactorCraft.TileEntities.PowerGen.TileEntitySteamLine with TileEntityLine - takes all steam of the boiler under it
 * (once the boiler is older than 5 ticks) and levels with neighbouring lines of the same working fluid (half the difference +1 per
 * tick); it remembers which reactor types the steam came from. Connects to other lines, a boiler below and steam grates (the other
 * original targets - high pressure turbine, steam diffuser, pipe pump - are not ported yet). The pressure limit option (default
 * unlimited) is not ported.
 */
public class SteamLineBlockEntity extends RotaryCraftBlockEntity implements SteamTile, reika.rotarycraft.api.interfaces.PumpablePipe {

	private int steam;
	private WorkingFluid fluid = WorkingFluid.EMPTY;
	private final Map<ReactorType, Double> source = new EnumMap<>(ReactorType.class);

	public SteamLineBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.STEAM_LINE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Steam Line";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.drawFromBoiler(world, pos);
		this.getPipeSteam(world, pos);
		if (steam <= 0) {
			fluid = WorkingFluid.EMPTY;
			source.clear();
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	private void drawFromBoiler(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos.below()) instanceof ReactorBoilerBlockEntity te) {
			if (te.getTicksExisted() > 5 && this.canTakeInWorkingFluid(te.getWorkingFluid())) {
				fluid = te.getWorkingFluid();
				int s = te.removeSteam();
				steam += s;
				for (Map.Entry<ReactorType, Double> e : te.getReactorTypeFractions().entrySet()) {
					ReactorType rt = e.getKey();
					if (rt == null || rt == ReactorType.NONE)
						rt = te.getReactorType();
					if (rt == null || rt == ReactorType.NONE)
						rt = ReactorType.FISSION;
					source.merge(rt, s*e.getValue(), Double::sum);
				}
			}
		}
	}

	private boolean canTakeInWorkingFluid(WorkingFluid f) {
		if (f == WorkingFluid.EMPTY)
			return false;
		return fluid == WorkingFluid.EMPTY || fluid == f;
	}

	private void getPipeSteam(Level world, BlockPos pos) {
		for (Direction d : Direction.values()) {
			if (world.getBlockEntity(pos.relative(d)) instanceof SteamLineBlockEntity tile && this.canTakeInWorkingFluid(tile.fluid))
				this.readPipe(tile);
		}
	}

	private void readPipe(SteamLineBlockEntity te) {
		int dS = te.steam-steam;
		if (dS > 0) {
			int amt = dS/2+1;
			float frac = amt/(float)te.steam;
			steam += amt;
			te.steam -= amt;
			fluid = te.fluid;
			for (ReactorType r : new ArrayList<>(te.source.keySet())) {
				double val = te.source.get(r)*frac;
				if (Double.isNaN(val) || Double.isInfinite(val))
					continue;
				source.merge(r, val, Double::sum);
				te.source.merge(r, -val, Double::sum);
			}
		}
	}

	@Override
	public int getSteam() {
		return steam;
	}

	public void removeSteam(int amt) {
		steam -= amt;
	}

	public WorkingFluid getWorkingFluid() {
		return fluid;
	}

	public Map<ReactorType, Double> getSourceReactorTypes() {
		return new EnumMap<>(source);
	}

	/** Original canTransferTo (pipe pump): into another line with the same working fluid or none. */
	@Override
	public boolean canTransferTo(reika.rotarycraft.api.interfaces.PumpablePipe p, Direction dir) {
		return p instanceof SteamLineBlockEntity te && (te.fluid == WorkingFluid.EMPTY || te.fluid == fluid);
	}

	@Override
	public int getFluidLevel() {
		return steam;
	}

	/** Original transferFrom (pipe pump): steam, working fluid and the matching share of the source reactor types. */
	@Override
	public void transferFrom(reika.rotarycraft.api.interfaces.PumpablePipe from, int amt) {
		SteamLineBlockEntity te = (SteamLineBlockEntity)from;
		float frac = (float)amt/te.steam;
		te.steam -= amt;
		fluid = te.fluid;
		steam += amt;
		for (ReactorType r : new ArrayList<>(te.source.keySet())) {
			double val = te.source.get(r)*frac;
			if (Double.isNaN(val) || Double.isInfinite(val))
				continue;
			source.merge(r, val, Double::sum);
			te.source.merge(r, -val, Double::sum);
		}
	}

	/** Original isConnected: other lines, a boiler below, steam grates, a steam diffuser facing this line, a pipe pump along its axis. */
	public static boolean connectsTo(Level world, BlockPos pos, Direction dir) {
		BlockEntity te = world.getBlockEntity(pos.relative(dir));
		if (te instanceof SteamLineBlockEntity || te instanceof SteamGrateBlockEntity)
			return true;
		if (te instanceof SteamDiffuserBlockEntity d)
			return d.getFacing().getOpposite() == dir;
		if (te instanceof reika.rotarycraft.content.blockentity.PipePumpBlockEntity pump)
			return pump.connectsOnSide(dir);
		return te instanceof ReactorBoilerBlockEntity && dir == Direction.DOWN;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("energy", steam);
		fluid.saveToNBT(NBT);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		steam = NBT.getInt("energy");
		fluid = WorkingFluid.getFromNBT(NBT);
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		CompoundTag t = new CompoundTag();
		for (Map.Entry<ReactorType, Double> e : source.entrySet())
			t.putDouble(e.getKey().name(), e.getValue());
		NBT.put("sources", t);
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		source.clear();
		CompoundTag t = NBT.getCompound("sources");
		for (ReactorType r : ReactorType.values())
			if (t.contains(r.name()))
				source.put(r, t.getDouble(r.name()));
	}
}
