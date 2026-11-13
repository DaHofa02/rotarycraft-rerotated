/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.rotarycraft.content.blockentity.WoodcutterBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryDamage;

/**
 * Original: MachineRegistry.WOODCUTTER in BlockDMMachine - 4 orientations, 0.875 high and one pixel short on its cutting side; touching
 * it while it runs deals 4 grinding damage (DamagingContact in BlockBasicMultiTE.onEntityCollidedWithBlock; not items and XP orbs).
 */
public class WoodcutterBlock extends MachineBlock {

	private static final VoxelShape[] SHAPES = {Block.box(1, 0, 0, 16, 14, 16), Block.box(0, 0, 0, 15, 14, 16), Block.box(0, 0, 1, 16, 14, 16),
			Block.box(0, 0, 0, 16, 14, 15)};

	public WoodcutterBlock(Properties props) {
		super(props, RotaryBlockEntities.WOODCUTTER, Placement.FOUR_SIDED, false);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		int meta = level.getBlockEntity(pos) instanceof WoodcutterBlockEntity te ? te.getBlockMetadata() : 0;
		return SHAPES[Math.min(3, Math.max(0, meta))];
	}

	@Override
	protected void entityInside(BlockState state, Level world, BlockPos pos, Entity e) {
		if (world.isClientSide || e instanceof ItemEntity || e instanceof ExperienceOrb)
			return;
		if (world.getBlockEntity(pos) instanceof WoodcutterBlockEntity te && te.canDealDamage())
			e.hurt(RotaryDamage.source(world, RotaryDamage.GRIND), te.getContactDamage());
	}
}
