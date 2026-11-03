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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import reika.electricraft.content.blockentity.BatteryBlockEntity;
import reika.electricraft.registry.BatteryType;
import reika.electricraft.registry.ElectriBlockEntities;
import reika.electricraft.registry.ElectriComponents;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.lib.math.ReikaEngLibrary;
import reika.rotarycraft.lib.math.ReikaMathLibrary;

/**
 * Original: BlockElectricBattery + BatteryRenderer + ItemBatteryPlacer - one block per tier (hardness 2, resistance 10, any tool), sides
 * with the tier's glow overlay, which is drawn full-bright while charged ({@link #CHARGED}); drops itself with its charge.
 */
public class BatteryBlock extends BaseEntityBlock {

	public static final BooleanProperty CHARGED = BooleanProperty.create("charged");

	public final BatteryType type;
	private final MapCodec<BatteryBlock> codec;

	public BatteryBlock(Properties props, BatteryType type) {
		super(props);
		this.type = type;
		codec = simpleCodec(p -> new BatteryBlock(p, type));
		this.registerDefaultState(this.stateDefinition.any().setValue(CHARGED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return codec;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CHARGED);
	}

	public static void setCharged(Level world, BlockPos pos, boolean charged) {
		BlockState s = world.getBlockState(pos);
		if (s.hasProperty(CHARGED) && s.getValue(CHARGED) != charged)
			world.setBlock(pos, s.setValue(CHARGED, charged), 3);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BatteryBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> given) {
		return createTickerHelper(given, ElectriBlockEntities.BATTERY.get(), RotaryCraftBlockEntity::tick);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
		return world.getBlockEntity(pos) instanceof BatteryBlockEntity b ? b.getRedstoneOverride() : 0;
	}

	@Override
	public void appendHoverText(ItemStack is, Item.TooltipContext ctx, List<Component> li, TooltipFlag flag) {
		long e = is.getOrDefault(ElectriComponents.BATTERY_ENERGY.get(), 0L);
		long max = type.maxCapacity;
		li.add(Component.literal(String.format("Stored Energy: %.1f %sJ/%.1f %sJ", ReikaMathLibrary.getThousandBase(e), ReikaEngLibrary.getSIPrefix(e),
				ReikaMathLibrary.getThousandBase(max), ReikaEngLibrary.getSIPrefix(max))));
		int a = type.outputCurrent;
		int v = type.outputVoltage;
		long power = (long)a*(long)v;
		li.add(Component.literal(String.format("Emits %dA at %dV (%.3f%sW)", a, v, ReikaMathLibrary.getThousandBase(power), ReikaEngLibrary.getSIPrefix(power))));
	}
}
