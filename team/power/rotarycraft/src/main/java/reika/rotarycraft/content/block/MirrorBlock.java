/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.MirrorBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: MachineRegistry.MIRROR in BlockSolar (BlockBasicMultiTE.onBlockActivated, MachineRegistry.getBrokenProducts) - a broken
 * mirror is repaired by right-clicking it with a mirror (used up outside creative); mined while broken it drops 2 base panels, a
 * circuit board and a steel gear instead of itself.
 */
public class MirrorBlock extends MachineBlock {

	public MirrorBlock(Properties props) {
		super(props, RotaryBlockEntities.MIRROR, Placement.NONE, false);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		if (world.getBlockEntity(pos) instanceof MirrorBlockEntity tm && tm.broken && held.is(RotaryItems.part(CraftingPart.MIRROR))) {
			if (!world.isClientSide) {
				tm.repair(world, pos);
				if (!ep.getAbilities().instabuild)
					held.shrink(1);
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		return super.useItemOn(held, state, world, pos, ep, hand, hit);
	}

	@Override
	protected List<ItemStack> getHarvestDrops(BlockState state, RotaryCraftBlockEntity te) {
		if (te instanceof MirrorBlockEntity tm && tm.broken) {
			ItemStack gear = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("rotarycraft", "steel_gear")));
			return List.of(new ItemStack(RotaryItems.part(CraftingPart.BASEPANEL), 2), new ItemStack(RotaryItems.part(CraftingPart.PCB)), gear);
		}
		return super.getHarvestDrops(state, te);
	}
}
