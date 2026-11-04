/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: RotaryEventManager.buildWorktables (PlayerPlaceBlockEvent and PlayerInteractEvent RIGHT_CLICK_BLOCK) and
 * checkAndBuildWorktable - the Worktable built in the world, besides its recipe. A crafting table on a steel block that is surrounded by
 * 8 bricks, above a redstone block surrounded by 8 double stone slabs (3x3x2 under the table); placing the crafting table there, or
 * right-clicking it, turns the whole structure into air and puts a Worktable at the centre of the lower layer, with the CRAFT sound. The
 * right-click is consumed (no crafting GUI).
 * Port notes: "crafting table" is the vanilla one or any block in c:player_workstations/crafting_tables (the original also took any
 * BlockWorkbench and the Tinkers' Construct table); 1.7.10's double_stone_slab:0 is 1.21's smooth stone slab of type double. The world is
 * changed on the server only (the original also ran it on the client, which the server sync now replaces).
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID)
public final class WorktableBuilder {

	private static final TagKey<Block> CRAFTING_TABLES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "player_workstations/crafting_tables"));

	private WorktableBuilder() {}

	@SubscribeEvent
	public static void onPlace(BlockEvent.EntityPlaceEvent evt) {
		if (evt.getEntity() instanceof Player && evt.getLevel() instanceof Level world && !world.isClientSide && isCraftingTable(evt.getPlacedBlock()))
			build(world, evt.getPos());
	}

	@SubscribeEvent
	public static void onRightClick(PlayerInteractEvent.RightClickBlock evt) {
		Level world = evt.getLevel();
		if (!world.isClientSide && isCraftingTable(world.getBlockState(evt.getPos())) && build(world, evt.getPos()))
			evt.setCanceled(true);
	}

	public static boolean isCraftingTable(BlockState s) {
		return s.is(Blocks.CRAFTING_TABLE) || s.is(CRAFTING_TABLES);
	}

	/** Original checkAndBuildWorktable, the checks: {@code table} is the crafting table's position. */
	public static boolean matches(Level world, BlockPos table) {
		if (!world.getBlockState(table.below()).is(RotaryBlocks.STEEL_BLOCK.get()) || !world.getBlockState(table.below(2)).is(Blocks.REDSTONE_BLOCK))
			return false;
		for (int i = -1; i <= 1; i++) {
			for (int k = -1; k <= 1; k++) {
				if (i == 0 && k == 0)
					continue;
				if (!world.getBlockState(table.offset(i, -1, k)).is(Blocks.BRICKS))
					return false;
				BlockState slab = world.getBlockState(table.offset(i, -2, k));
				if (!slab.is(Blocks.SMOOTH_STONE_SLAB) || slab.getValue(SlabBlock.TYPE) != SlabType.DOUBLE)
					return false;
			}
		}
		return true;
	}

	/** Original checkAndBuildWorktable: builds the Worktable if the structure is complete; true if it did. */
	public static boolean build(Level world, BlockPos table) {
		if (!matches(world, table))
			return false;
		for (int i = -1; i <= 1; i++) {
			for (int k = -1; k <= 1; k++) {
				world.setBlockAndUpdate(table.offset(i, -1, k), Blocks.AIR.defaultBlockState());
				world.setBlockAndUpdate(table.offset(i, -2, k), Blocks.AIR.defaultBlockState());
			}
		}
		world.setBlockAndUpdate(table, Blocks.AIR.defaultBlockState());
		RotarySounds.play(world, table, RotarySounds.CRAFT.get(), 1F, 1F);
		world.setBlockAndUpdate(table.below(2), RotaryBlocks.WORKTABLE.get().defaultBlockState());
		return true;
	}
}
