/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import reika.rotarycraft.registry.MaterialRegistry;

/** Original: MaterialRegistry.isHarvestablePickaxe - which tool can harvest a shaft or gearbox of a material. */
public final class MaterialHarvest {

	private MaterialHarvest() {}

	public static boolean isHarvestablePickaxe(MaterialRegistry mat, ItemStack tool) {
		if (mat.harvestLevel < 0)
			return true;
		if (tool.isEmpty())
			return false;
		boolean pick = tool.is(ItemTags.PICKAXES);
		return switch (mat) {
			case STONE -> pick || tool.isCorrectToolForDrops(Blocks.STONE.defaultBlockState());
			case STEEL -> tool.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState());
			case TUNGSTEN, DIAMOND -> tool.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState());
			case BEDROCK -> tool.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState());
			default -> false;
		};
	}

	public static boolean canHarvest(Player ep, MaterialRegistry mat) {
		return !ep.isCreative() && isHarvestablePickaxe(mat, ep.getMainHandItem());
	}

	/** Original BlockShaft/BlockGearbox.getPlayerRelativeBlockHardness: 0.2/(ordinal+1) per tick with a suitable tool, else 0.01/(ordinal+1). */
	public static float getDestroyProgress(Player ep, MaterialRegistry mat, int ordinal) {
		return canHarvest(ep, mat) ? 0.2F/(ordinal+1) : 0.01F/(ordinal+1);
	}
}
