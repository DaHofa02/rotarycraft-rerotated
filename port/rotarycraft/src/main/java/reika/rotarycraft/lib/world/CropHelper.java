/*******************************************************************************
 * @author Reika Kalseki (original DragonAPI). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.lib.world;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Original: Reika.DragonAPI.Libraries.Registry.ReikaCropHelper (WHEAT, CARROT, POTATO, NETHERWART, COCOA: ripe at their last growth stage,
 * harvested back to the first one, one seed less in the drops - CropType.CropMethods.removeOneSeed) and the "growing" kinds of
 * ReikaPlantHelper (cactus, mushroom, sapling, sugar cane, crops and stems, nether wart, vines). Vanilla 1.21 crops the 1.7.10 original
 * did not know (beetroot, torchflower) are crops here through CropBlock, the class all of them share now. Mod crops (ModCropList) are not
 * ported.
 */
public final class CropHelper {

	private CropHelper() {}

	/** Original ReikaCropHelper.isCrop. */
	public static boolean isCrop(BlockState s) {
		Block b = s.getBlock();
		return b instanceof CropBlock || b instanceof NetherWartBlock || b instanceof CocoaBlock;
	}

	/** Original CropType.isRipe: the last growth stage. */
	public static boolean isRipe(BlockState s) {
		Block b = s.getBlock();
		if (b instanceof CropBlock c)
			return c.isMaxAge(s);
		if (b instanceof NetherWartBlock)
			return s.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
		if (b instanceof CocoaBlock)
			return s.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
		return false;
	}

	/** Original CropType.setHarvested: back to the first stage (the original reset the cocoa's facing too; here it keeps it). */
	@Nullable
	public static BlockState getHarvestedState(BlockState s) {
		Block b = s.getBlock();
		if (b instanceof CropBlock c)
			return c.getStateForAge(0);
		if (b instanceof NetherWartBlock)
			return s.setValue(NetherWartBlock.AGE, 0);
		if (b instanceof CocoaBlock)
			return s.setValue(CocoaBlock.AGE, 0);
		return null;
	}

	/** The crop's seed (what replants it): the crop block's item, e.g. wheat seeds, carrot, nether wart, cocoa beans. */
	public static Item getSeed(BlockState s) {
		return s.getBlock().asItem();
	}

	/**
	 * Harvests a ripe crop in place: its drops minus one seed (CropMethods.removeOneSeed) and the crop set back to its first stage.
	 * Returns the drops (empty if the crop was not ripe).
	 */
	public static List<ItemStack> harvest(ServerLevel world, BlockPos pos, BlockState s) {
		if (!isCrop(s) || !isRipe(s))
			return List.of();
		List<ItemStack> li = new java.util.ArrayList<>(Block.getDrops(s, world, pos, world.getBlockEntity(pos)));
		Item seed = getSeed(s);
		for (java.util.Iterator<ItemStack> it = li.iterator(); it.hasNext();) {
			ItemStack is = it.next();
			if (is.is(seed)) {
				if (is.getCount() > 1)
					is.shrink(1);
				else
					it.remove();
				break;
			}
		}
		world.setBlock(pos, getHarvestedState(s), Block.UPDATE_ALL);
		return li;
	}

	/** Original ReikaPlantHelper.getPlant(b).grows(): plants a random tick makes grow. */
	public static boolean isGrowingPlant(BlockState s) {
		Block b = s.getBlock();
		return b instanceof CactusBlock || b instanceof MushroomBlock || b instanceof SaplingBlock || b instanceof SugarCaneBlock
				|| b instanceof CropBlock || b instanceof StemBlock || b instanceof NetherWartBlock || b instanceof VineBlock;
	}
}
