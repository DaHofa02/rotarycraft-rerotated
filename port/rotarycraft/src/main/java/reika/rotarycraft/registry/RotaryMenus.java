/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.menu.BlastFurnaceMenu;
import reika.rotarycraft.content.menu.ExtractorMenu;
import reika.rotarycraft.content.menu.MachineMenu;
import reika.rotarycraft.content.menu.WorktableMenu;
import reika.rotarycraft.content.menu.MachineMenu.Layout;

/** Original: GuiHandler / GuiRegistry.MACHINE - one menu type per container layout. */
public final class RotaryMenus {

	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RotaryCraft.MOD_ID);

	private static final Map<Layout, Supplier<MenuType<MachineMenu>>> TYPES = new EnumMap<>(Layout.class);

	static {
		for (Layout l : Layout.values()) {
			Supplier<MenuType<MachineMenu>>[] self = new Supplier[1];
			self[0] = MENUS.register(l.name().toLowerCase(java.util.Locale.ROOT), () -> IMenuTypeExtension.create((id, inv, buf) -> MachineMenu.fromNetwork(self[0].get(), id, inv, l, buf)));
			TYPES.put(l, self[0]);
		}
	}

	public static final Supplier<MenuType<BlastFurnaceMenu>> BLAST_FURNACE = MENUS.register("blast_furnace", () -> IMenuTypeExtension.create(BlastFurnaceMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.GrinderMenu>> GRINDER = MENUS.register("grinder", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.GrinderMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.WinderMenu>> WINDER = MENUS.register("winder", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.WinderMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.FillingStationMenu>> FILLING_STATION = MENUS.register("filling_station", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.FillingStationMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.CompactorMenu>> COMPACTOR = MENUS.register("compactor", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.CompactorMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.RefrigeratorMenu>> REFRIGERATOR = MENUS.register("refrigerator", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.RefrigeratorMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.DryingBedMenu>> DRYING_BED = MENUS.register("drying_bed", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.DryingBedMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.FermenterMenu>> FERMENTER = MENUS.register("fermenter", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.FermenterMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.CentrifugeMenu>> CENTRIFUGE = MENUS.register("centrifuge", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.CentrifugeMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.FractionatorMenu>> FRACTIONATOR = MENUS.register("fractionator", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.FractionatorMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.PulseJetFurnaceMenu>> PULSE_JET = MENUS.register("pulse_jet_furnace", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.PulseJetFurnaceMenu::fromNetwork));
	public static final Supplier<MenuType<ExtractorMenu>> EXTRACTOR = MENUS.register("extractor", () -> IMenuTypeExtension.create(ExtractorMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.BorerMenu>> BORER = MENUS.register("borer", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.BorerMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.PlayerDetectorMenu>> PLAYER_DETECTOR = MENUS.register("player_detector", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.PlayerDetectorMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.MobRadarMenu>> MOB_RADAR = MENUS.register("mob_radar", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.MobRadarMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.LavaMakerMenu>> LAVA_MAKER = MENUS.register("lava_maker", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.LavaMakerMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.BaitBoxMenu>> BAIT_BOX = MENUS.register("bait_box", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.BaitBoxMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.AutoBreederMenu>> AUTO_BREEDER = MENUS.register("auto_breeder", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.AutoBreederMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.ObsidianMakerMenu>> OBSIDIAN_MAKER = MENUS.register("obsidian_maker", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.ObsidianMakerMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.IgniterMenu>> IGNITER = MENUS.register("firestarter", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.IgniterMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.FertilizerMenu>> FERTILIZER = MENUS.register("fertilizer", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.FertilizerMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.SortingMenu>> SORTING = MENUS.register("sorting", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.SortingMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.BucketFillerMenu>> BUCKET_FILLER = MENUS.register("bucket_filler", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.BucketFillerMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.LineBuilderMenu>> LINE_BUILDER = MENUS.register("line_builder", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.LineBuilderMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.BigFurnaceMenu>> BIG_FURNACE = MENUS.register("big_furnace", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.BigFurnaceMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.MultiClutchMenu>> MULTI_CLUTCH = MENUS.register("multi_clutch", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.MultiClutchMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.ParticleEmitterMenu>> PARTICLE_EMITTER = MENUS.register("particle_emitter", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.ParticleEmitterMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.CrystallizerMenu>> CRYSTALLIZER = MENUS.register("crystallizer", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.CrystallizerMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.ComposterMenu>> COMPOSTER = MENUS.register("composter", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.ComposterMenu::fromNetwork));
	public static final Supplier<MenuType<reika.rotarycraft.content.menu.HeaterMenu>> HEATER = MENUS.register("heater", () -> IMenuTypeExtension.create(reika.rotarycraft.content.menu.HeaterMenu::fromNetwork));
	public static final Supplier<MenuType<WorktableMenu>> WORKTABLE = MENUS.register("worktable", () -> IMenuTypeExtension.create(WorktableMenu::fromNetwork));

	private RotaryMenus() {}

	public static MenuType<MachineMenu> get(Layout l) {
		return TYPES.get(l).get();
	}
}
