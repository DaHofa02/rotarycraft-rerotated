/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import reika.rotarycraft.content.blockentity.AdvancedGearBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;

/** The worm gear, high-ratio gear, industrial coils and CVT: four-sided; coils and CVT open a GUI; lubricant buckets fill the high-ratio gear and CVT. */
public class AdvancedGearBlock extends MachineBlock {

	public final AdvancedGearBlockEntity.Kind kind;

	public AdvancedGearBlock(Properties props, AdvancedGearBlockEntity.Kind kind) {
		super(props, RotaryBlockEntities.ADVANCED_GEAR, Placement.FOUR_SIDED, false);
		this.kind = kind;
	}

	/** A lubricant bucket fills the high-ratio gear or the CVT (1000 mB). */
	@Override
	protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack is, net.minecraft.world.level.block.state.BlockState state,
			net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player ep, net.minecraft.world.InteractionHand hand,
			net.minecraft.world.phys.BlockHitResult hit) {
		if (!kind.isLubricated() || !is.is(reika.rotarycraft.registry.RotaryFluids.LUBRICANT.get().getBucket()) || !(world.getBlockEntity(pos) instanceof AdvancedGearBlockEntity te))
			return super.useItemOn(is, state, world, pos, ep, hand, hit);
		if (!world.isClientSide && te.getLubricant()+1000 <= AdvancedGearBlockEntity.LUBE_CAPACITY) {
			te.addLubricantFromBucket(1000);
			if (!ep.getAbilities().instabuild)
				ep.setItemInHand(hand, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
		}
		return net.minecraft.world.ItemInteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	protected net.minecraft.world.InteractionResult useWithoutItem(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level world,
			net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player ep, net.minecraft.world.phys.BlockHitResult hit) {
		boolean gui = kind.storesEnergy() || kind == AdvancedGearBlockEntity.Kind.CVT;
		if (!gui || ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof AdvancedGearBlockEntity te))
			return net.minecraft.world.InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof net.minecraft.server.level.ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> kind == AdvancedGearBlockEntity.Kind.CVT
					? new reika.rotarycraft.content.menu.CvtMenu(id, inv, te) : new reika.rotarycraft.content.menu.CoilMenu(id, inv, te), state.getBlock().getName()),
					buf -> buf.writeBlockPos(pos));
		return net.minecraft.world.InteractionResult.sidedSuccess(world.isClientSide);
	}
}
