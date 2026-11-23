/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

import appeng.api.AECapabilities;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.upgrades.Upgrades;

import reika.rotarycraft.registry.RotaryBlocks;

/**
 * RotaryCraft ME Kinetics: stores RotaryCraft shaft energy as kinetic energy in an Applied Energistics 2 network. The kinetic input turns shaft
 * power into stored joules, the kinetic output turns them back into shaft power (with a loss and lubricant use); kinetic cells 1k to 1M hold
 * them at 1024 J per bit. Also RotaryCraft's bundled bus (ME export switched by Project Red bundled cables, optional).
 */
@Mod(RotaryME.MOD_ID)
public class RotaryME {

	public static final String MOD_ID = "rotaryme";

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

	public static final DeferredBlock<KineticInputBlock> KINETIC_INPUT = BLOCKS.register("kinetic_input", () -> new KineticInputBlock(RotaryBlocks.machineProperties()));
	public static final DeferredBlock<KineticOutputBlock> KINETIC_OUTPUT = BLOCKS.register("kinetic_output", () -> new KineticOutputBlock(RotaryBlocks.machineProperties()));

	public static final DeferredBlock<BundledBusBlock> BUNDLED_BUS = BLOCKS.register("bundled_bus", () -> new BundledBusBlock(RotaryBlocks.machineProperties()));

	public static final Supplier<BlockEntityType<BundledBusBlockEntity>> BUNDLED_BUS_BE = BLOCK_ENTITIES.register("bundled_bus",
			() -> BlockEntityType.Builder.of(BundledBusBlockEntity::new, BUNDLED_BUS.get()).build(null));
	public static final Supplier<BlockEntityType<KineticInputBlockEntity>> KINETIC_INPUT_BE = BLOCK_ENTITIES.register("kinetic_input",
			() -> BlockEntityType.Builder.of(KineticInputBlockEntity::new, KINETIC_INPUT.get()).build(null));
	public static final Supplier<BlockEntityType<KineticOutputBlockEntity>> KINETIC_OUTPUT_BE = BLOCK_ENTITIES.register("kinetic_output",
			() -> BlockEntityType.Builder.of(KineticOutputBlockEntity::new, KINETIC_OUTPUT.get()).build(null));

	public static final Supplier<MenuType<BundledBusMenu>> BUNDLED_BUS_MENU = MENUS.register("bundled_bus", () -> IMenuTypeExtension.create(BundledBusMenu::fromNetwork));
	public static final Supplier<MenuType<KineticInputMenu>> KINETIC_INPUT_MENU = MENUS.register("kinetic_input", () -> IMenuTypeExtension.create(KineticInputMenu::fromNetwork));
	public static final Supplier<MenuType<KineticOutputMenu>> KINETIC_OUTPUT_MENU = MENUS.register("kinetic_output", () -> IMenuTypeExtension.create(KineticOutputMenu::fromNetwork));

	public static final DeferredItem<BlockItem> KINETIC_INPUT_ITEM = ITEMS.registerSimpleBlockItem(KINETIC_INPUT);
	public static final DeferredItem<BlockItem> KINETIC_OUTPUT_ITEM = ITEMS.registerSimpleBlockItem(KINETIC_OUTPUT);
	public static final DeferredItem<BlockItem> BUNDLED_BUS_ITEM = ITEMS.registerSimpleBlockItem(BUNDLED_BUS);
	public static final DeferredItem<Item> CELL_HOUSING = ITEMS.registerSimpleItem("kinetic_cell_housing");
	public static final DeferredItem<Item> COMPONENT_1M = ITEMS.registerSimpleItem("kinetic_component_1m");
	/** Only the picture of kinetic energy in terminals; not obtainable. */
	public static final DeferredItem<Item> ENERGY_ICON = ITEMS.registerSimpleItem("kinetic_energy");
	public static final int[] CELL_SIZES = {1, 4, 16, 64, 256, 1024};
	@SuppressWarnings("unchecked")
	public static final DeferredItem<KineticCellItem>[] CELLS = new DeferredItem[CELL_SIZES.length];

	static {
		for (int i = 0; i < CELL_SIZES.length; i++) {
			int kb = CELL_SIZES[i];
			CELLS[i] = ITEMS.register("kinetic_cell_"+cellName(kb), () -> new KineticCellItem(kb, new Item.Properties()));
		}
	}

	public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.rotaryme")).icon(() -> new ItemStack(KINETIC_INPUT.get()))
			.displayItems((params, out) -> {
				out.accept(KINETIC_INPUT.get());
				out.accept(KINETIC_OUTPUT.get());
				out.accept(BUNDLED_BUS.get());
				out.accept(CELL_HOUSING.get());
				out.accept(COMPONENT_1M.get());
				for (DeferredItem<KineticCellItem> c : CELLS)
					out.accept(c.get());
			}).build());

	public static String cellName(int kb) {
		return kb >= 1024 ? (kb/1024)+"m" : kb+"k";
	}

	public RotaryME(IEventBus modBus, ModContainer container) {
		container.registerConfig(ModConfig.Type.COMMON, MEConfig.SPEC);
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
		BLOCK_ENTITIES.register(modBus);
		MENUS.register(modBus);
		TABS.register(modBus);
		modBus.addListener(this::registerKeyType);
		modBus.addListener(this::registerCapabilities);
		modBus.addListener(this::registerPayloads);
		modBus.addListener(this::setup);
	}

	/** AE2's acceleration (speed) card. */
	public static Item speedCard() {
		return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ae2", "speed_card"));
	}

	/** AE2's redstone card: in the kinetic output it lets a redstone signal switch the output on or off. */
	public static Item redstoneCard() {
		return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ae2", "redstone_card"));
	}

	private void setup(FMLCommonSetupEvent e) {
		e.enqueueWork(() -> {
			Upgrades.add(speedCard(), KINETIC_INPUT.get(), 4, "gui.rotaryme.cards_input");
			Upgrades.add(speedCard(), KINETIC_OUTPUT.get(), 4, "gui.rotaryme.cards_output");
			Upgrades.add(redstoneCard(), KINETIC_OUTPUT.get(), 1, "gui.rotaryme.cards_output");
			if (reika.rotaryme.compat.OptionalMods.projectRed()) //optional: bundled cables connect to the bundled bus
				reika.rotaryme.compat.ProjectRedCompat.registerInteraction();
		});
	}

	private void registerKeyType(RegisterEvent e) {
		if (e.getRegistryKey().equals(AEKeyType.REGISTRY_KEY))
			AEKeyTypes.register(KineticKeyType.INSTANCE);
	}

	private void registerCapabilities(RegisterCapabilitiesEvent e) {
		e.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, KINETIC_INPUT_BE.get(), (te, v) -> te);
		e.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, KINETIC_OUTPUT_BE.get(), (te, v) -> te);
		e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, KINETIC_OUTPUT_BE.get(), (te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, BUNDLED_BUS_BE.get(), (te, v) -> te);
	}

	private void registerPayloads(RegisterPayloadHandlersEvent e) {
		e.registrar("1").playToServer(SetOutputPayload.TYPE, SetOutputPayload.CODEC, SetOutputPayload::handle);
	}

	/** 1234567 J -> "1.23 MJ". */
	public static String formatJoules(long j) {
		String[] u = {"J", "kJ", "MJ", "GJ", "TJ", "PJ"};
		double v = j;
		int i = 0;
		while (v >= 1000 && i < u.length-1) {
			v /= 1000;
			i++;
		}
		return i == 0 ? j+" J" : String.format("%.2f %s", v, u[i]);
	}
}
