/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.block;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.electricraft.content.blockentity.RFBatteryBlockEntity;
import reika.electricraft.registry.ElectriBlockEntities;
import reika.electricraft.registry.ElectriComponents;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.lib.math.ReikaEngLibrary;
import reika.rotarycraft.lib.math.ReikaMathLibrary;

/** Original: BlockRFBattery / BatteryBlock (hardness 2, resistance 10, any tool, drops itself with its charge). */
public class RFBatteryBlock extends BaseEntityBlock {

	private static final MapCodec<RFBatteryBlock> CODEC = simpleCodec(RFBatteryBlock::new);

	public RFBatteryBlock(Properties props) {
		super(props);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RFBatteryBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
		return createTickerHelper(given, ElectriBlockEntities.RF_BATTERY.get(), RotaryCraftBlockEntity::tick);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
		return world.getBlockEntity(pos) instanceof RFBatteryBlockEntity b ? b.getRedstoneOverride() : 0;
	}

	/** Original ItemRFBatteryPlacer tooltip. */
	@Override
	public void appendHoverText(ItemStack is, Item.TooltipContext ctx, List<Component> li, TooltipFlag flag) {
		long e = is.getOrDefault(ElectriComponents.BATTERY_ENERGY.get(), 0L);
		long max = RFBatteryBlockEntity.CAPACITY;
		li.add(Component.literal(String.format("Stored Energy: %.3f %sRF/%.3f %sRF", ReikaMathLibrary.getThousandBase(e), ReikaEngLibrary.getSIPrefix(e),
				ReikaMathLibrary.getThousandBase(max), ReikaEngLibrary.getSIPrefix(max))));
	}
}
