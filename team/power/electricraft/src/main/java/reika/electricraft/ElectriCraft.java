package reika.electricraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.electricraft.registry.ElectriBlockEntities;
import reika.electricraft.registry.ElectriBlocks;
import reika.electricraft.registry.ElectriComponents;

/** Original: Reika.ElectriCraft.ElectriCraft. Private NeoForge 1.21.1 port by way of the RotaryCraft port; not for distribution. */
@Mod(ElectriCraft.MOD_ID)
public final class ElectriCraft {

	public static final String MOD_ID = "electricraft";
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

	static {
		TABS.register("main", () -> CreativeModeTab.builder()
				.title(Component.translatable("itemGroup.electricraft"))
				.icon(() -> new ItemStack(ElectriBlocks.MOTOR.get()))
				.displayItems((params, out) -> {
					for (var b : ElectriBlocks.allWires())
						out.accept(b.get());
					for (var ore : reika.electricraft.registry.ElectriOres.oreList)
						if (ore.isPorted())
							out.accept(ElectriBlocks.ore(ore).get());
					for (var ore : reika.electricraft.registry.ElectriOres.oreList)
						if (ore.isPorted())
							out.accept(ElectriBlocks.ingot(ore).get());
					out.accept(ElectriBlocks.GENERATOR.get());
					out.accept(ElectriBlocks.MOTOR.get());
					out.accept(ElectriBlocks.RESISTOR.get());
					out.accept(ElectriBlocks.PRECISE_RESISTOR.get());
					out.accept(ElectriBlocks.RELAY.get());
					out.accept(ElectriBlocks.METER.get());
					out.accept(ElectriBlocks.TRANSFORMER.get());
					for (var b : reika.electricraft.registry.BatteryType.batteryList)
						out.accept(ElectriBlocks.battery(b).get());
					ItemStack full = new ItemStack(ElectriBlocks.battery(reika.electricraft.registry.BatteryType.STAR).get()); //original: a full auroral battery
					full.set(reika.electricraft.registry.ElectriComponents.BATTERY_ENERGY.get(), reika.electricraft.registry.BatteryType.STAR.maxCapacity);
					out.accept(full);
					for (var b : reika.electricraft.registry.BatteryType.batteryList)
						out.accept(ElectriBlocks.crystal(b).get());
					out.accept(ElectriBlocks.RF_CRYSTAL.get());
					out.accept(ElectriBlocks.RF_CABLE.get());
					out.accept(ElectriBlocks.RF_BATTERY.get());
					for (var t : reika.electricraft.content.block.WirelessPadBlock.Tier.values())
						out.accept(ElectriBlocks.wirelessPad(t).get());
					out.accept(ElectriBlocks.BLUE_DUST.get());
					out.accept(ElectriBlocks.DIAMOND_DUST.get());
					out.accept(ElectriBlocks.QUARTZ_DUST.get());
					out.accept(ElectriBlocks.CRYSTAL_DUST.get());
					for (int lim : reika.electricraft.content.blockentity.FuseBlockEntity.TIERS) { //original getCreativeModeVariants
						ItemStack fuse = new ItemStack(ElectriBlocks.FUSE.get());
						fuse.set(reika.electricraft.registry.ElectriComponents.FUSE_LIMIT.get(), lim);
						out.accept(fuse);
					}
				}).build());
	}

	public ElectriCraft(IEventBus modBus, ModContainer container) {
		ElectriBlocks.BLOCKS.register(modBus);
		ElectriBlocks.ITEMS.register(modBus);
		ElectriBlockEntities.TYPES.register(modBus);
		ElectriComponents.COMPONENTS.register(modBus);
		reika.electricraft.registry.ElectriWorldGen.PLACEMENTS.register(modBus);
		reika.electricraft.registry.ElectriMenus.MENUS.register(modBus);
		modBus.addListener(reika.electricraft.network.ElectriPayloads::register);
		modBus.addListener(ElectriCraft::registerCapabilities);
		TABS.register(modBus);
		container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ElectriConfig.SPEC);
	}

	/** FE (successor of RF) on the RF cable (all sides) and the RF battery (side rules in the block entity). */
	private static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e) {
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, reika.electricraft.registry.ElectriBlockEntities.RF_CABLE.get(),
				(te, side) -> te.getEnergyStorage());
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, reika.electricraft.registry.ElectriBlockEntities.RF_BATTERY.get(),
				(te, side) -> te.getEnergyStorage(side));
		for (var t : reika.electricraft.content.block.WirelessPadBlock.Tier.values())
			e.registerBlock(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
					(level, pos, state, be, side) -> reika.electricraft.content.block.WirelessPadBlock.getEnergyStorage(level, pos, state, side), ElectriBlocks.wirelessPad(t).get());
	}
}
