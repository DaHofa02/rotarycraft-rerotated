/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.HydroEngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.GearboxTypes.GearPart;
import reika.rotarycraft.registry.RotaryItems;

/** Original: Reika.RotaryCraft.Blocks.BlockEngine and Items.Placers.ItemEnginePlacer (4-sided, can be flipped). One block per engine type. */
public class EngineBlock extends MachineBlock {

	private final EngineType engine;

	public EngineBlock(Properties props, Supplier<? extends BlockEntityType<? extends EngineBlockEntity>> type, EngineType engine) {
		super(props, type, Placement.FOUR_SIDED, true);
		this.engine = engine;
	}

	public EngineType getEngineType() {
		return engine;
	}

	/**
	 * Original BlockEngine.onBlockActivated: turbine / compressor items repair a damaged gas turbine (fully / one FOD step), a bedrock
	 * shaft upgrades the hydrokinetic engine, water buckets fill water-cooled engines (RotaryCraft fuel buckets come with the buckets, step 4).
	 */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		ItemInteractionResult tool = super.useItemOn(held, state, world, pos, ep, hand, hit);
		if (tool != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
			return tool;
		if (world.getBlockEntity(pos) instanceof JetEngineBlockEntity jet && jet.FOD > 0 && (held.is(RotaryItems.part(CraftingPart.TURBINE)) || held.is(RotaryItems.part(CraftingPart.COMPRESSOR)))) {
			if (!world.isClientSide) {
				if (held.is(RotaryItems.part(CraftingPart.TURBINE)))
					jet.repairJet();
				else
					jet.repairJetPartial();
				if (!ep.isCreative())
					held.shrink(1);
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		if (world.getBlockEntity(pos) instanceof HydroEngineBlockEntity hy && held.is(RotaryItems.gearPart(GearboxTypes.BEDROCK, GearPart.SHAFT)) && !hy.isBedrock()) {
			if (!world.isClientSide) {
				hy.makeBedrock();
				if (!ep.isCreative())
					held.shrink(1);
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		if (world.getBlockEntity(pos) instanceof EngineBlockEntity te && held.getCount() == 1 && held.is(Items.WATER_BUCKET) && te.getEngineType().needsWater()) {
			if (!world.isClientSide) {
				if (te.getWater() <= EngineBlockEntity.CAPACITY-1000) {
					if (!ep.isCreative())
						ep.setItemInHand(hand, new ItemStack(Items.BUCKET));
					te.addWater(1000);
				}
				else {
					ep.sendSystemMessage(Component.literal("Engine is too full to add water!"));
				}
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	/** Original BlockEngine.harvestBlock: a wrecked gas turbine (FOD 8) leaves 1-5 steel gears and 16-32 scrap instead of itself. */
	@Override
	protected List<ItemStack> getHarvestDrops(BlockState state, RotaryCraftBlockEntity te) {
		if (te instanceof JetEngineBlockEntity jet && jet.FOD >= 8) {
			net.minecraft.util.RandomSource r = te.getLevel().random;
			return List.of(new ItemStack(RotaryItems.gearPart(GearboxTypes.STEEL, GearPart.GEAR), 1+r.nextInt(5)), new ItemStack(RotaryItems.part(CraftingPart.SCRAP), 16+r.nextInt(17)));
		}
		ItemStack is = new ItemStack(this);
		is.applyComponents(te.collectComponents());
		return List.of(is);
	}

	/** Original BlockEngine.getDrops: a gas turbine keeps its damage on the item. */
	@Override
	protected List<ItemStack> getPlainDrops(BlockState state, @javax.annotation.Nullable RotaryCraftBlockEntity te) {
		ItemStack is = new ItemStack(this);
		if (te instanceof JetEngineBlockEntity jet && jet.FOD > 0)
			is.set(reika.rotarycraft.registry.RotaryComponents.JET_DAMAGE.get(), jet.FOD);
		return List.of(is);
	}
}
