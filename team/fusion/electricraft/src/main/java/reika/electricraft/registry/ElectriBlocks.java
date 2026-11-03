/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.electricraft.ElectriCraft;
import reika.electricraft.content.block.ElectriMachineBlock;
import reika.electricraft.content.block.WireBlock;
import reika.rotarycraft.content.item.MachineBlockItem;
import reika.rotarycraft.registry.RotaryBlocks;

/** Original: ElectriBlocks / ElectriTiles - the ported blocks. */
public final class ElectriBlocks {

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElectriCraft.MOD_ID);
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElectriCraft.MOD_ID);

	private static final Map<WireType, DeferredBlock<WireBlock>> WIRES = new EnumMap<>(WireType.class);
	private static final Map<WireType, DeferredBlock<WireBlock>> INSULATED = new EnumMap<>(WireType.class);

	static {
		for (WireType w : WireType.wireList) {
			if (w == WireType.SUPERCONDUCTOR)
				continue; //needs liquid nitrogen (not ported)
			WIRES.put(w, wire(w, false));
			INSULATED.put(w, wire(w, true));
		}
	}

	private static final Map<ElectriOres, DeferredBlock<net.minecraft.world.level.block.Block>> ORES = new EnumMap<>(ElectriOres.class);
	private static final Map<ElectriOres, DeferredItem<Item>> INGOTS = new EnumMap<>(ElectriOres.class);

	/** Original BlockElectriOre (hardness 2, resistance 5, drops itself) and ElectriItems.INGOTS. */
	static {
		for (ElectriOres ore : ElectriOres.oreList) {
			if (!ore.isPorted())
				continue;
			ORES.put(ore, BLOCKS.registerSimpleBlock(ore.getName()+"_ore", BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
					.instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2F, 5F)));
			ITEMS.registerSimpleBlockItem(ORES.get(ore));
			INGOTS.put(ore, ITEMS.registerSimpleItem(ore.getName()+"_ingot"));
		}
	}

	private static final Map<BatteryType, DeferredBlock<reika.electricraft.content.block.BatteryBlock>> BATTERIES = new EnumMap<>(BatteryType.class);
	private static final Map<BatteryType, DeferredItem<Item>> CRYSTALS = new EnumMap<>(BatteryType.class);
	/** Original ElectriCrafting: lapis, diamond, quartz and energy crystal dust. */
	public static final DeferredItem<Item> BLUE_DUST = ITEMS.registerSimpleItem("bluedust");
	public static final DeferredItem<Item> DIAMOND_DUST = ITEMS.registerSimpleItem("diamonddust");
	public static final DeferredItem<Item> QUARTZ_DUST = ITEMS.registerSimpleItem("quartzdust");
	public static final DeferredItem<Item> CRYSTAL_DUST = ITEMS.registerSimpleItem("crystaldust");

	/** Original BlockElectricBattery (one block per BatteryType) and ElectriItems.CRYSTAL. */
	static {
		for (BatteryType b : BatteryType.batteryList) {
			String name = b.getName()+"_battery";
			DeferredBlock<reika.electricraft.content.block.BatteryBlock> blk = BLOCKS.register(name, () -> new reika.electricraft.content.block.BatteryBlock(
					BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2F, 10F).sound(SoundType.METAL), b));
			BATTERIES.put(b, blk);
			ITEMS.registerSimpleBlockItem(blk);
			CRYSTALS.put(b, ITEMS.registerSimpleItem(b.getName()+"_crystal"));
		}
	}

	/** Original ElectriTiles.CABLE (BlockRFCable) and RFBATTERY, here with NeoForge FE; ElectriItems.CRYSTAL "RF Energy Crystal". */
	public static final DeferredBlock<reika.electricraft.content.block.RFCableBlock> RF_CABLE = BLOCKS.register("rf_cable", () -> new reika.electricraft.content.block.RFCableBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5F, 15F).sound(SoundType.METAL).noOcclusion().dynamicShape()));
	public static final DeferredBlock<reika.electricraft.content.block.RFBatteryBlock> RF_BATTERY = BLOCKS.register("rf_battery", () -> new reika.electricraft.content.block.RFBatteryBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2F, 10F).sound(SoundType.METAL)));
	public static final DeferredItem<Item> RF_CRYSTAL = ITEMS.registerSimpleItem("rf_crystal");

	private static final Map<reika.electricraft.content.block.WirelessPadBlock.Tier, DeferredBlock<reika.electricraft.content.block.WirelessPadBlock>> PADS = new EnumMap<>(reika.electricraft.content.block.WirelessPadBlock.Tier.class);

	static {
		for (var t : reika.electricraft.content.block.WirelessPadBlock.Tier.values()) {
			DeferredBlock<reika.electricraft.content.block.WirelessPadBlock> b = BLOCKS.register(t.getName()+"_wireless_pad", () -> new reika.electricraft.content.block.WirelessPadBlock(
					BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2F, 10F).sound(SoundType.METAL), t));
			ITEMS.registerSimpleBlockItem(b);
			PADS.put(t, b);
		}
		ITEMS.registerSimpleBlockItem(RF_CABLE);
		ITEMS.registerSimpleBlockItem(RF_BATTERY);
	}

	public static final DeferredBlock<ElectriMachineBlock> GENERATOR = machine("generator", ElectriBlockEntities.GENERATOR);
	public static final DeferredBlock<ElectriMachineBlock> MOTOR = machine("motor", ElectriBlockEntities.MOTOR);
	public static final DeferredBlock<ElectriMachineBlock> RESISTOR = machine("resistor", ElectriBlockEntities.RESISTOR);
	public static final DeferredBlock<ElectriMachineBlock> RELAY = machine("relay", ElectriBlockEntities.RELAY);
	public static final DeferredBlock<ElectriMachineBlock> METER = machine("meter", ElectriBlockEntities.METER);
	public static final DeferredBlock<ElectriMachineBlock> FUSE = machine("fuse", ElectriBlockEntities.FUSE);
	public static final DeferredBlock<ElectriMachineBlock> TRANSFORMER = machine("transformer", ElectriBlockEntities.TRANSFORMER);
	public static final DeferredBlock<ElectriMachineBlock> PRECISE_RESISTOR = machine("precise_resistor", ElectriBlockEntities.PRECISE_RESISTOR);

	private ElectriBlocks() {}

	private static DeferredBlock<WireBlock> wire(WireType w, boolean ins) {
		String name = (ins ? "insulated_" : "")+w.getIconTexture()+"_wire";
		DeferredBlock<WireBlock> b = BLOCKS.register(name, () -> new WireBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL)
				.strength(0.05F, 2F).sound(SoundType.WOOL).noOcclusion().dynamicShape(), w, ins));
		ITEMS.registerSimpleBlockItem(name, b);
		return b;
	}

	private static DeferredBlock<ElectriMachineBlock> machine(String name, java.util.function.Supplier<? extends net.minecraft.world.level.block.entity.BlockEntityType<? extends reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity>> type) {
		DeferredBlock<ElectriMachineBlock> b = BLOCKS.register(name, () -> new ElectriMachineBlock(RotaryBlocks.machineProperties().dynamicShape(), type));
		ITEMS.register(name, () -> new MachineBlockItem(b.get(), new Item.Properties()));
		return b;
	}

	public static DeferredBlock<net.minecraft.world.level.block.Block> ore(ElectriOres ore) {
		return ORES.get(ore);
	}

	public static DeferredItem<Item> ingot(ElectriOres ore) {
		return INGOTS.get(ore);
	}

	public static DeferredBlock<reika.electricraft.content.block.WirelessPadBlock> wirelessPad(reika.electricraft.content.block.WirelessPadBlock.Tier t) {
		return PADS.get(t);
	}

	public static DeferredBlock<reika.electricraft.content.block.BatteryBlock> battery(BatteryType b) {
		return BATTERIES.get(b);
	}

	public static List<DeferredBlock<reika.electricraft.content.block.BatteryBlock>> allBatteries() {
		return new ArrayList<>(BATTERIES.values());
	}

	public static DeferredItem<Item> crystal(BatteryType b) {
		return CRYSTALS.get(b);
	}

	public static WireBlock getWire(WireType w, boolean insulated) {
		return (insulated ? INSULATED : WIRES).get(w).get();
	}

	public static List<DeferredBlock<WireBlock>> allWires() {
		List<DeferredBlock<WireBlock>> li = new ArrayList<>(WIRES.values());
		li.addAll(INSULATED.values());
		return li;
	}
}
