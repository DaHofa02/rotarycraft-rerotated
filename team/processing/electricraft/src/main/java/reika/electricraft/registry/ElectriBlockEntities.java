/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.registries.DeferredRegister;

import reika.electricraft.ElectriCraft;
import reika.electricraft.content.blockentity.FuseBlockEntity;
import reika.electricraft.content.blockentity.GeneratorBlockEntity;
import reika.electricraft.content.blockentity.MeterBlockEntity;
import reika.electricraft.content.blockentity.RelayBlockEntity;
import reika.electricraft.content.blockentity.ResistorBlockEntity;
import reika.electricraft.content.blockentity.MotorBlockEntity;
import reika.electricraft.content.blockentity.WireBlockEntity;

public final class ElectriBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElectriCraft.MOD_ID);

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<WireBlockEntity>> WIRE = TYPES.register("wire",
			() -> BlockEntityType.Builder.of(WireBlockEntity::new, ElectriBlocks.allWires().stream().map(d -> (Block)d.get()).toArray(Block[]::new)).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<GeneratorBlockEntity>> GENERATOR = TYPES.register("generator",
			() -> BlockEntityType.Builder.of(GeneratorBlockEntity::new, ElectriBlocks.GENERATOR.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<MotorBlockEntity>> MOTOR = TYPES.register("motor",
			() -> BlockEntityType.Builder.of(MotorBlockEntity::new, ElectriBlocks.MOTOR.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<RelayBlockEntity>> RELAY = TYPES.register("relay",
			() -> BlockEntityType.Builder.of(RelayBlockEntity::new, ElectriBlocks.RELAY.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<ResistorBlockEntity>> RESISTOR = TYPES.register("resistor",
			() -> BlockEntityType.Builder.of(ResistorBlockEntity::resistor, ElectriBlocks.RESISTOR.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<ResistorBlockEntity>> PRECISE_RESISTOR = TYPES.register("precise_resistor",
			() -> BlockEntityType.Builder.of(ResistorBlockEntity::precise, ElectriBlocks.PRECISE_RESISTOR.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<FuseBlockEntity>> FUSE = TYPES.register("fuse",
			() -> BlockEntityType.Builder.of(FuseBlockEntity::new, ElectriBlocks.FUSE.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<MeterBlockEntity>> METER = TYPES.register("meter",
			() -> BlockEntityType.Builder.of(MeterBlockEntity::new, ElectriBlocks.METER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.electricraft.content.blockentity.BatteryBlockEntity>> BATTERY = TYPES.register("battery",
			() -> BlockEntityType.Builder.of(reika.electricraft.content.blockentity.BatteryBlockEntity::new, ElectriBlocks.allBatteries().stream().map(d -> (Block)d.get()).toArray(Block[]::new)).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.electricraft.content.blockentity.TransformerBlockEntity>> TRANSFORMER = TYPES.register("transformer",
			() -> BlockEntityType.Builder.of(reika.electricraft.content.blockentity.TransformerBlockEntity::new, ElectriBlocks.TRANSFORMER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.electricraft.content.blockentity.RFCableBlockEntity>> RF_CABLE = TYPES.register("rf_cable",
			() -> BlockEntityType.Builder.of(reika.electricraft.content.blockentity.RFCableBlockEntity::new, ElectriBlocks.RF_CABLE.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.electricraft.content.blockentity.RFBatteryBlockEntity>> RF_BATTERY = TYPES.register("rf_battery",
			() -> BlockEntityType.Builder.of(reika.electricraft.content.blockentity.RFBatteryBlockEntity::new, ElectriBlocks.RF_BATTERY.get()).build(null));

	private ElectriBlockEntities() {}
}
