/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.content.blockentity.FlywheelBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.Flywheels;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryItems;

/** Original: Reika.RotaryCraft.Blocks.BlockFlywheel and Items.Placers.ItemFlywheelPlacer. */
public class FlywheelBlock extends MachineBlock {

	public FlywheelBlock(Properties props) {
		super(props, RotaryBlockEntities.FLYWHEEL, Placement.FLYWHEEL, true);
	}

	@Override
	protected List<ItemStack> getHarvestDrops(BlockState state, RotaryCraftBlockEntity te) {
		FlywheelBlockEntity fly = (FlywheelBlockEntity)te;
		if (fly.failed)
			return List.of(new ItemStack(RotaryItems.part(CraftingPart.MOUNT))); //drop mount
		return List.of(flywheelItem(fly.getTypeOrdinal()));
	}

	@Override
	protected List<ItemStack> getPlainDrops(BlockState state, @Nullable RotaryCraftBlockEntity te) {
		return List.of(flywheelItem(te instanceof FlywheelBlockEntity fly ? fly.getTypeOrdinal() : Flywheels.WOOD));
	}

	public static ItemStack flywheelItem(Flywheels type) {
		ItemStack is = new ItemStack(RotaryBlocks.FLYWHEEL.get());
		is.set(RotaryComponents.FLYWHEEL_TYPE.get(), type);
		return is;
	}
}
