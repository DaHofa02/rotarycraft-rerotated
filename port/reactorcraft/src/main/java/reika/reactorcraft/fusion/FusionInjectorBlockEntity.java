/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.reactor.NeutronTile;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fusion.TileEntityFusionInjector - part of the toroid ring, facing along it (the way the
 * placing player looked, reversed). While its multiblock is complete, it holds fusion plasma (8000 mB, only from an adjacent magnetic
 * pipe), it is enabled and has no redstone signal, it makes one plasma ball per tick (2 mB) heading the way it faces. The next ring
 * part is 2 blocks ahead.
 */
public class FusionInjectorBlockEntity extends RotaryCraftBlockEntity implements PipeConnector, FusionMultiBlockTile, FusionReactorToroidPart, NeutronTile {

	public static final int CAPACITY = 8000;

	private final FluidTank tank = new FluidTank(CAPACITY, fs -> fs.is(ReactorFluids.get("fusion_plasma")));

	@Nullable
	private Direction facing;

	private boolean hasMultiBlock;

	private boolean enabled = true;

	public FusionInjectorBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.FUSION_INJECTOR.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Fusion Injector";
	}

	@Override
	public boolean hasMultiBlock() {
		return hasMultiBlock;
	}

	@Override
	public void setHasMultiBlock(boolean has) {
		hasMultiBlock = has;
		this.syncAllData();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		if (this.canMake(world, pos))
			this.make(world, pos);
	}

	public void setFacing(Direction dir) {
		facing = dir;
		this.setChanged();
	}

	private boolean canMake(Level world, BlockPos pos) {
		if (!hasMultiBlock)
			return false;
		if (tank.isEmpty())
			return false;
		if (!enabled)
			return false;
		return !world.hasNeighborSignal(pos);
	}

	private void make(Level world, BlockPos pos) {
		PlasmaEntity e = new PlasmaEntity(world, pos, this.getPlacerName());
		e.setTarget(pos.getX()+this.getFacing().getStepX(), pos.getZ()+this.getFacing().getStepZ());
		world.addFreshEntity(e);
		tank.drain(2, IFluidHandler.FluidAction.EXECUTE);
	}

	public BlockPos getTarget() {
		return worldPosition.relative(this.getFacing());
	}

	@Override
	@Nullable
	public FusionReactorToroidPart getNextPart(Level world, BlockPos pos) {
		BlockEntity te = world.getBlockEntity(worldPosition.relative(this.getFacing(), 2));
		return te instanceof FusionReactorToroidPart p ? p : null;
	}

	public Direction getFacing() {
		return facing != null ? facing : Direction.EAST;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enable) {
		enabled = enable;
		this.syncAllData();
	}

	public FluidTank getTank() {
		return tank;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return true;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return Flow.INPUT;
	}

	/** Original canFill: plasma, and only from a magnetic pipe on that side. */
	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from == null || !resource.is(ReactorFluids.get("fusion_plasma")) || !(level.getBlockEntity(worldPosition.relative(from)) instanceof MagneticPipeBlockEntity))
			return 0;
		int n = tank.fill(resource, action);
		if (n > 0 && action.execute())
			this.setChanged();
		return n;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		return false;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		FusionMultiBlock.breakAround(world, pos);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("face", this.getFacing().get3DDataValue());
		NBT.putBoolean("multi", hasMultiBlock);
		NBT.putBoolean("t_enable", enabled);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		if (NBT.contains("face"))
			facing = Direction.from3DDataValue(NBT.getInt("face"));
		hasMultiBlock = NBT.getBoolean("multi");
		if (NBT.contains("t_enable"))
			enabled = NBT.getBoolean("t_enable");
	}
}
