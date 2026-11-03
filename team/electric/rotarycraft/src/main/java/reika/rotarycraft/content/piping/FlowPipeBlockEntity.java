/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.piping;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: the flow-control pipes TileEntityValve, TileEntityBypass, TileEntitySeparatorPipe and TileEntitySuctionPipe. All carry any
 * fluid and join liquid pipes, fuel lines and hoses.
 * <ul>
 * <li>Valve: exchanges fluid with machines only while it has a redstone signal (and then only takes from them).</li>
 * <li>Bypass: never touches machines; two bypasses only join where one was placed against the other, so parallel runs stay apart.</li>
 * <li>Separator: takes from pipes on its four sides and gives only downwards (upwards with a redstone signal).</li>
 * <li>Suction pipe: takes from machines and fluid handlers on every side, gives only to pipes, takes nothing from pipes.</li>
 * </ul>
 */
public class FlowPipeBlockEntity extends PipingBlockEntity {

	@Nullable
	private Fluid fluid;
	private int amount;
	private final PipeType kind;

	/** Bypass only: sides joined to another bypass because one was placed against the other. */
	private boolean[] forcedConnection = new boolean[6];
	private boolean[] tryForcedConnection = new boolean[6];

	private FlowPipeBlockEntity(BlockEntityType<?> type, PipeType kind, BlockPos pos, BlockState state) {
		super(type, pos, state);
		this.kind = kind;
	}

	public static FlowPipeBlockEntity valve(BlockPos pos, BlockState state) {
		return new FlowPipeBlockEntity(RotaryBlockEntities.VALVE.get(), PipeType.VALVE, pos, state);
	}

	public static FlowPipeBlockEntity bypass(BlockPos pos, BlockState state) {
		return new FlowPipeBlockEntity(RotaryBlockEntities.BYPASS.get(), PipeType.BYPASS, pos, state);
	}

	public static FlowPipeBlockEntity separation(BlockPos pos, BlockState state) {
		return new FlowPipeBlockEntity(RotaryBlockEntities.SEPARATION.get(), PipeType.SEPARATION, pos, state);
	}

	public static FlowPipeBlockEntity suction(BlockPos pos, BlockState state) {
		return new FlowPipeBlockEntity(RotaryBlockEntities.SUCTION.get(), PipeType.SUCTION, pos, state);
	}

	@Override
	public PipeType getPipeType() {
		return kind;
	}

	private boolean hasRedstoneSignal() {
		return this.getLevel() != null && this.getLevel().hasNeighborSignal(worldPosition);
	}

	@Override
	public boolean canConnectToPipe(PipeType p, Direction dir) {
		return switch (kind) {
			case VALVE -> p.isStandardPipe() || p == PipeType.HOSE || p == PipeType.FUEL_LINE || p == PipeType.SEPARATION;
			case BYPASS -> p.isStandardPipe() || p == PipeType.FUEL_LINE || p == PipeType.HOSE || (p == PipeType.BYPASS && forcedConnection[dir.ordinal()]);
			default -> p.isStandardPipe() || p == PipeType.FUEL_LINE || p == PipeType.HOSE;
		};
	}

	/** Original TileEntityBypass.onPlacedAgainst: placed against another bypass, the two join on that side. */
	public void onPlacedAgainst(Direction dir) {
		if (kind == PipeType.BYPASS && this.getLevel() != null && this.getLevel().getBlockEntity(worldPosition.relative(dir)) instanceof FlowPipeBlockEntity f && f.kind == PipeType.BYPASS)
			tryForcedConnection[dir.ordinal()] = true;
	}

	@Override
	public void recomputeConnections(Level world, BlockPos pos) {
		if (kind == PipeType.BYPASS) {
			forcedConnection = new boolean[6];
			for (Direction dir : Direction.values()) {
				if (tryForcedConnection[dir.ordinal()] && world.getBlockEntity(pos.relative(dir)) instanceof FlowPipeBlockEntity tb && tb.kind == PipeType.BYPASS) {
					tb.tryForcedConnection[dir.getOpposite().ordinal()] = true;
					forcedConnection[dir.ordinal()] = true;
					tb.forcedConnection[dir.getOpposite().ordinal()] = true;
					tb.queueConnectionEvaluation(2);
				}
			}
		}
		super.recomputeConnections(world, pos);
	}

	@Nullable
	@Override
	public Fluid getFluidType() {
		return fluid;
	}

	@Override
	public int getFluidLevel() {
		return amount;
	}

	@Override
	protected void setFluid(@Nullable Fluid f) {
		fluid = f;
	}

	@Override
	protected void setLevel(int amt) {
		amount = amt;
	}

	@Override
	protected boolean interactsWithMachines() {
		return switch (kind) {
			case VALVE -> this.hasRedstoneSignal();
			case SUCTION -> true;
			default -> false;
		};
	}

	@Override
	protected void onIntake(BlockEntity te) {}

	@Override
	public boolean isValidFluid(Fluid f) {
		return true;
	}

	@Override
	public boolean canReceiveFromPipeOn(Direction side) {
		return switch (kind) {
			case SEPARATION -> side.getAxis() != Direction.Axis.Y;
			case SUCTION -> false;
			default -> true;
		};
	}

	@Override
	public boolean canEmitToPipeOn(Direction side) {
		if (kind == PipeType.SEPARATION)
			return side == (this.hasRedstoneSignal() ? Direction.UP : Direction.DOWN);
		return true;
	}

	@Override
	public boolean canIntakeFromIFluidHandler(Direction side) {
		return kind == PipeType.VALVE || kind == PipeType.SUCTION;
	}

	@Override
	public boolean canOutputToIFluidHandler(Direction side) {
		return false;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		if (kind == PipeType.BYPASS)
			NBT.putByte("fconn", bits(forcedConnection));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		if (kind == PipeType.BYPASS)
			forcedConnection = unbits(NBT.getByte("fconn"));
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		if (kind == PipeType.BYPASS)
			NBT.putByte("tfconn", bits(tryForcedConnection));
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		if (kind == PipeType.BYPASS)
			tryForcedConnection = unbits(NBT.getByte("tfconn"));
	}

	private static byte bits(boolean[] b) {
		int r = 0;
		for (int i = 0; i < b.length; i++)
			if (b[i])
				r |= 1 << i;
		return (byte)r;
	}

	private static boolean[] unbits(byte v) {
		boolean[] b = new boolean[6];
		for (int i = 0; i < 6; i++)
			b[i] = (v & (1 << i)) != 0;
		return b;
	}
}
