/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.block;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Original: BlockChargePad + TileEntityWirelessCharger - takes FE (originally RF) on every side but its front and passes it, at the tier's
 * efficiency and throughput, into the energy block two blocks in front of it (into that block's side facing the pad). It faces the
 * player who places it (looking more than 60 degrees down: up, up: down).
 */
public class WirelessPadBlock extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	public enum Tier {
		BASIC(0.4F, 80),
		IMPROVED(0.6F, 800),
		ADVANCED(0.8F, 6000),
		HIGHTECH(0.9F, 40000),
		SUPERCONDUCTING(1F, Integer.MAX_VALUE);

		public final float efficiency;
		public final int maxThroughput;

		Tier(float f, int th) {
			efficiency = f;
			maxThroughput = th;
		}

		public String getName() {
			return this.name().toLowerCase(Locale.ENGLISH);
		}
	}

	public final Tier tier;

	public WirelessPadBlock(Properties props, Tier tier) {
		super(props);
		this.tier = tier;
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	/** Original ItemElectriPlacer with RotaryAux.get6SidedMetadataFromPlayerLook and the pad's metadata-to-side table. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction d;
		if (ctx.getPlayer() == null)
			d = Direction.UP;
		else if (Math.abs(ctx.getPlayer().getXRot()) < 60)
			d = ctx.getHorizontalDirection().getOpposite();
		else
			d = ctx.getPlayer().getXRot() > 0 ? Direction.UP : Direction.DOWN;
		return this.defaultBlockState().setValue(FACING, d);
	}

	@Nullable
	public static IEnergyStorage getEnergyStorage(Level world, BlockPos pos, BlockState state, @Nullable Direction side) {
		if (!(state.getBlock() instanceof WirelessPadBlock pad))
			return null;
		Direction facing = state.getValue(FACING);
		if (side == facing)
			return null;
		Tier t = pad.tier;
		return new IEnergyStorage() {
			@Nullable
			private IEnergyStorage target() {
				BlockPos p = pos.relative(facing, 2);
				return world.isLoaded(p) ? world.getCapability(Capabilities.EnergyStorage.BLOCK, p, facing.getOpposite()) : null;
			}

			@Override
			public int receiveEnergy(int maxReceive, boolean simulate) {
				IEnergyStorage ier = this.target();
				float f = t.efficiency;
				maxReceive = Math.min(maxReceive, t.maxThroughput);
				return ier != null ? (int)(ier.receiveEnergy((int)(maxReceive*f), simulate)/f) : 0;
			}

			@Override
			public int extractEnergy(int maxExtract, boolean simulate) {
				return 0;
			}

			@Override
			public int getEnergyStored() {
				IEnergyStorage ier = this.target();
				return ier != null ? ier.getEnergyStored() : 0;
			}

			@Override
			public int getMaxEnergyStored() {
				IEnergyStorage ier = this.target();
				return ier != null ? ier.getMaxEnergyStored() : 0;
			}

			@Override
			public boolean canExtract() {
				return false;
			}

			@Override
			public boolean canReceive() {
				return this.target() != null;
			}
		};
	}
}
