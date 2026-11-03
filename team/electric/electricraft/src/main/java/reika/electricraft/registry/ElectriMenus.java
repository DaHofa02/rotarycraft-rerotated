/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.electricraft.ElectriCraft;
import reika.electricraft.content.menu.TransformerMenu;

/** Original: ElectriGuiHandler. */
public final class ElectriMenus {

	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ElectriCraft.MOD_ID);

	public static final Supplier<MenuType<TransformerMenu>> TRANSFORMER = MENUS.register("transformer", () -> IMenuTypeExtension.create(TransformerMenu::fromNetwork));

	public static final Supplier<MenuType<reika.electricraft.content.menu.RFCableMenu>> RF_CABLE = MENUS.register("rf_cable", () -> IMenuTypeExtension.create(reika.electricraft.content.menu.RFCableMenu::fromNetwork));

	private ElectriMenus() {}
}
