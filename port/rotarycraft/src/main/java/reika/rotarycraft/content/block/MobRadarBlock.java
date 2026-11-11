/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.rotarycraft.content.blockentity.MobRadarBlockEntity;
import reika.rotarycraft.content.menu.MobRadarMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The Player Detector (MachineRegistry.PLAYERDETECTOR): 0.6875 high, weak redstone power 15 to all sides while a player is detected
 * (original BlockBasicMultiTE.isProvidingWeakPower); a right click opens the range GUI (GuiMobRadar).
 */
public class MobRadarBlock extends MachineBlock {

	private static final MapCodec<MobRadarBlock> CODEC = simpleCodec(MobRadarBlock::new);
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

	public MobRadarBlock(Properties props) {
		super(props, RotaryBlockEntities.MOB_RADAR, Placement.NONE, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof MobRadarBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new MobRadarMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
