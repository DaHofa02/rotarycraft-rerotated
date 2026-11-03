/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.rotarycraft.content.blockentity.FrictionHeaterBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryDamage;

/**
 * Original: MachineRegistry.FRICTION in BlockDMMachine (4-sided, 15/16 block high, MachineRegistry.getMaxY) and the heat contact
 * damage of BlockBasicMultiTE.onEntityCollidedWithBlock (dealsHeatDamage: anything but items and XP, set on fire for 6 s).
 */
public class FrictionHeaterBlock extends MachineBlock {

	private static final MapCodec<FrictionHeaterBlock> CODEC = simpleCodec(FrictionHeaterBlock::new);
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

	public FrictionHeaterBlock(Properties props) {
		super(props, RotaryBlockEntities.FRICTION_HEATER, Placement.FOUR_SIDED, false);
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
		if (world.getBlockEntity(pos) instanceof FrictionHeaterBlockEntity te) {
			int dmg = te.getThermalDamage();
			if (dmg > 0) {
				e.hurt(RotaryDamage.source(world, RotaryDamage.HEAT), dmg);
				e.igniteForSeconds(6);
			}
		}
	}
}
