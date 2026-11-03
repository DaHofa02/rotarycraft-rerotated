/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.BlastFurnaceBlockEntity;
import reika.rotarycraft.content.menu.BlastFurnaceMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: MachineRegistry.BLASTFURNACE in BlockIMachine - a textured cube, blastfurn_front on the side the 4-sided metadata picks
 * (meta 0 west, 1 east, 2 north, 3 south), blastfurn_front_active while it can make something, blastfurn_side elsewhere.
 */
public class BlastFurnaceBlock extends MachineBlock {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	private static final MapCodec<BlastFurnaceBlock> CODEC = simpleCodec(BlastFurnaceBlock::new);

	public BlastFurnaceBlock(Properties props) {
		super(props, RotaryBlockEntities.BLAST_FURNACE, Placement.FOUR_SIDED, false);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.WEST).setValue(ACTIVE, false));
	}

	public static Direction frontFromMeta(int meta) {
		return switch(meta) {
			case 1 -> Direction.EAST;
			case 2 -> Direction.NORTH;
			case 3 -> Direction.SOUTH;
			default -> Direction.WEST;
		};
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(FACING, ACTIVE);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof BlastFurnaceBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new BlastFurnaceMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
