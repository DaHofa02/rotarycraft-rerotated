/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.rotarycraft.content.blockentity.GrinderBlockEntity;
import reika.rotarycraft.content.menu.GrinderMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryDamage;

/**
 * Original: MachineRegistry.GRINDER in BlockDMIMachine (4-sided, 13/16 block high, MachineRegistry.getMaxY) and the contact damage of
 * BlockBasicMultiTE.onEntityCollidedWithBlock (DamagingContact: 3 damage while powered; not items and XP orbs).
 */
public class GrinderBlock extends MachineBlock {

	private static final MapCodec<GrinderBlock> CODEC = simpleCodec(GrinderBlock::new);
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 13, 16);

	public GrinderBlock(Properties props) {
		super(props, RotaryBlockEntities.GRINDER, Placement.FOUR_SIDED, false);
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
	protected void entityInside(BlockState state, Level world, BlockPos pos, Entity e) {
		if (world.isClientSide || e instanceof ItemEntity || e instanceof ExperienceOrb)
			return;
		if (world.getBlockEntity(pos) instanceof GrinderBlockEntity te && te.canDealDamage() && te.getContactDamage() > 0)
			e.hurt(RotaryDamage.source(world, RotaryDamage.GRIND), te.getContactDamage());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof GrinderBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new GrinderMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
