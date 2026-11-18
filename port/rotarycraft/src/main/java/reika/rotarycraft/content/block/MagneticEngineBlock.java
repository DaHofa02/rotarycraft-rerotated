/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.MagneticEngineBlockEntity;
import reika.rotarycraft.content.menu.MagneticEngineMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/** The magnetostatic engine: four-sided like the other engines; a right click opens its GUI, a liquid nitrogen bucket fills its coolant. */
public class MagneticEngineBlock extends MachineBlock {

	public MagneticEngineBlock(Properties props) {
		this(props, RotaryBlockEntities.MAGNETIC_ENGINE);
	}

	/** Also the steam turbine, which shares the engine's GUI. */
	public MagneticEngineBlock(Properties props, java.util.function.Supplier<? extends net.minecraft.world.level.block.entity.BlockEntityType<? extends MagneticEngineBlockEntity>> type) {
		super(props, type, Placement.FOUR_SIDED, false);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack is, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		if (is.is(RotaryItems.LIQUID_NITROGEN_BUCKET.get()) && world.getBlockEntity(pos) instanceof MagneticEngineBlockEntity te && te.getCoolant() <= 23000) {
			if (!world.isClientSide) {
				te.addCoolant(1000);
				if (!ep.isCreative())
					ep.setItemInHand(hand, new ItemStack(Items.BUCKET));
				te.syncAllData();
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		return super.useItemOn(is, state, world, pos, ep, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof MagneticEngineBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new MagneticEngineMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
