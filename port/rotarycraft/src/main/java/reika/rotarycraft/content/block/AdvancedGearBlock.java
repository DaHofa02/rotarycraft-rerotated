/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import reika.rotarycraft.content.blockentity.AdvancedGearBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;

/** The worm gear, high-ratio gear and industrial coils: four-sided; the coils open a GUI for their output speed and torque. */
public class AdvancedGearBlock extends MachineBlock {

	public final AdvancedGearBlockEntity.Kind kind;

	public AdvancedGearBlock(Properties props, AdvancedGearBlockEntity.Kind kind) {
		super(props, RotaryBlockEntities.ADVANCED_GEAR, Placement.FOUR_SIDED, false);
		this.kind = kind;
	}

	@Override
	protected net.minecraft.world.InteractionResult useWithoutItem(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level world,
			net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player ep, net.minecraft.world.phys.BlockHitResult hit) {
		if (!kind.storesEnergy() || ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof AdvancedGearBlockEntity te))
			return net.minecraft.world.InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof net.minecraft.server.level.ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new reika.rotarycraft.content.menu.CoilMenu(id, inv, te), state.getBlock().getName()),
					buf -> buf.writeBlockPos(pos));
		return net.minecraft.world.InteractionResult.sidedSuccess(world.isClientSide);
	}
}
