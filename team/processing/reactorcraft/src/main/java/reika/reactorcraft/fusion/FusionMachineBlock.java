/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import reika.rotarycraft.content.block.MachineBlock;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: BlockReactorTile / BlockReactorTileModelled with ItemReactorPlacer for the fusion machines - 4-sided like every ReactorCraft
 * machine. Placing it sets the injector's facing (the way the player looks, reversed; property "facing" for its texture), the gas
 * collector's side (the clicked face) and the heater's and absorber's starting temperature (the ambient one).
 */
public class FusionMachineBlock extends MachineBlock {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	private static final ThreadLocal<Direction> FACE = new ThreadLocal<>();

	private final MapCodec<FusionMachineBlock> codec;
	private final boolean facingProperty;

	public FusionMachineBlock(Properties props, Supplier<? extends BlockEntityType<? extends RotaryCraftBlockEntity>> type, boolean facingProperty) {
		super(props, type, Placement.FOUR_SIDED, false);
		this.facingProperty = facingProperty;
		codec = simpleCodec(p -> new FusionMachineBlock(p, type, facingProperty));
		if (facingProperty)
			this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.EAST));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return codec;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		FACE.set(ctx.getClickedFace());
		return super.getStateForPlacement(ctx);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		var te = world.getBlockEntity(pos);
		if (te instanceof FusionInjectorBlockEntity inj && placer != null)
			setInjectorFacing(world, inj, placer.getDirection().getOpposite());
		if (te instanceof GasCollectorBlockEntity gc && FACE.get() != null)
			gc.setBlockMetadata(FACE.get().get3DDataValue());
		if (te instanceof FusionHeaterBlockEntity h)
			h.addTemperature(AmbientTemperature.getAmbientTemperatureAt(world, pos));
		if (te instanceof NeutronAbsorberBlockEntity a)
			a.setTemperature(AmbientTemperature.getAmbientTemperatureAt(world, pos));
		FACE.remove();
	}

	public static void setInjectorFacing(Level world, FusionInjectorBlockEntity te, Direction dir) {
		te.setFacing(dir);
		BlockState s = world.getBlockState(te.getBlockPos());
		if (s.hasProperty(FACING) && dir.getAxis().isHorizontal())
			world.setBlock(te.getBlockPos(), s.setValue(FACING, dir), 3);
		te.syncAllData();
	}
}
