/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.menu.UProcessorMenu;

/** Original: ReactorGuiHandler. */
public final class ReactorMenus {

	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ReactorCraft.MOD_ID);

	public static final Supplier<MenuType<UProcessorMenu>> PROCESSOR = MENUS.register("processor", () -> IMenuTypeExtension.create(UProcessorMenu::fromNetwork));

	public static final Supplier<MenuType<reika.reactorcraft.content.menu.ElectrolyzerMenu>> ELECTROLYZER = MENUS.register("electrolyzer",
			() -> IMenuTypeExtension.create(reika.reactorcraft.content.menu.ElectrolyzerMenu::fromNetwork));
	public static final Supplier<MenuType<reika.reactorcraft.content.menu.SynthesizerMenu>> SYNTHESIZER = MENUS.register("synthesizer",
			() -> IMenuTypeExtension.create(reika.reactorcraft.content.menu.SynthesizerMenu::fromNetwork));

	public static final Supplier<MenuType<reika.reactorcraft.content.menu.IsoCentrifugeMenu>> ISO_CENTRIFUGE = MENUS.register("isocentrifuge",
			() -> IMenuTypeExtension.create(reika.reactorcraft.content.menu.IsoCentrifugeMenu::fromNetwork));

	public static final Supplier<MenuType<reika.reactorcraft.content.menu.NuclearCoreMenu>> NUCLEAR_CORE = MENUS.register("nuclear_core",
			() -> IMenuTypeExtension.create(reika.reactorcraft.content.menu.NuclearCoreMenu::fromNetwork));

	public static final Supplier<MenuType<reika.reactorcraft.content.menu.CpuMenu>> CPU = MENUS.register("cpu",
			() -> IMenuTypeExtension.create(reika.reactorcraft.content.menu.CpuMenu::fromNetwork));

	public static final Supplier<MenuType<reika.reactorcraft.content.menu.WasteMenus.Container>> WASTE_CONTAINER = MENUS.register("waste_container",
			() -> IMenuTypeExtension.create(reika.reactorcraft.content.menu.WasteMenus.Container::fromNetwork));
	public static final Supplier<MenuType<reika.reactorcraft.content.menu.WasteMenus.Storage>> WASTE_STORAGE = MENUS.register("waste_storage",
			() -> IMenuTypeExtension.create(reika.reactorcraft.content.menu.WasteMenus.Storage::fromNetwork));
	public static final Supplier<MenuType<reika.reactorcraft.content.menu.WasteMenus.Decayer>> WASTE_DECAYER = MENUS.register("waste_decayer",
			() -> IMenuTypeExtension.create(reika.reactorcraft.content.menu.WasteMenus.Decayer::fromNetwork));

	public static final Supplier<MenuType<reika.reactorcraft.content.menu.WasteMenus.PebbleBed>> PEBBLE_BED = MENUS.register("pebble_bed",
			() -> IMenuTypeExtension.create(reika.reactorcraft.content.menu.WasteMenus.PebbleBed::fromNetwork));

	public static final Supplier<MenuType<reika.reactorcraft.content.menu.ThoriumCoreMenu>> THORIUM_CORE = MENUS.register("thorium_core",
			() -> IMenuTypeExtension.create(reika.reactorcraft.content.menu.ThoriumCoreMenu::fromNetwork));

	private ReactorMenus() {}
}
