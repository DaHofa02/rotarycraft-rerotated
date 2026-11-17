/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntityChainDrive - a belt hub with a chain (joined with chains): 16384 Nm
 * and 65536 rad/s before it slips. Driven past 65536 rad/s after the slip (input above 65536) the receiving hub blows up (explosion
 * strength 2). The original's chain-snapping branch (torque above the maximum after capping it) can never run and is left out.
 */
public class ChainDriveBlockEntity extends BeltHubBlockEntity {

	public ChainDriveBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.CHAIN_DRIVE.get(), pos, state);
	}

	@Override
	public int copyOmegaFromDriverSide(int input) {
		input = super.copyOmegaFromDriverSide(input);
		if (input > this.getMaxSmoothSpeed()) {
			Level world = level;
			BlockPos p = worldPosition;
			this.resetOther();
			world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
			world.explode(null, p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, 2, Level.ExplosionInteraction.BLOCK);
		}
		return input;
	}

	@Override
	public int getBeltColor() {
		return 0x505050; //ReikaColorAPI.GStoHex(80)
	}

	@Override
	public ItemStack getBeltItem() {
		return new ItemStack(RotaryItems.part(CraftingPart.CHAIN));
	}

	@Override
	public int getMaxTorque() {
		return 16384;
	}

	@Override
	public int getMaxSmoothSpeed() {
		return 65536;
	}
}
