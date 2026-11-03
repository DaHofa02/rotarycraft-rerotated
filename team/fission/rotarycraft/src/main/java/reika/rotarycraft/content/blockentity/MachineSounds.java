/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;

import reika.rotarycraft.registry.MaterialRegistry;

/** Vanilla sound equivalents for the 1.7.10 sound names the original machines played. */
public final class MachineSounds {

	private MachineSounds() {}

	/** Original: MaterialRegistry.getDamageNoise(): "dig.wood", "dig.stone", otherwise "mob.blaze.hit". */
	public static SoundEvent getDamageNoise(MaterialRegistry mat) {
		return switch(mat) {
			case WOOD -> SoundEvents.WOOD_BREAK;
			case STONE -> SoundEvents.STONE_BREAK;
			default -> SoundEvents.BLAZE_HURT;
		};
	}

	/** Original: RotaryAux.isMuffled - wool (Material.cloth) above and below. */
	public static boolean isMuffled(Level world, BlockPos pos) {
		return isMufflingBlock(world, pos.above()) && isMufflingBlock(world, pos.below());
	}

	public static boolean isMufflingBlock(Level world, BlockPos pos) {
		return world.getBlockState(pos).is(BlockTags.WOOL);
	}
}
