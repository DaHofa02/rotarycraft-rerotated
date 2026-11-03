/*******************************************************************************
 * Live scenarios of the private ports with GregTech CEu Modern (not part of the original mods; only registered when GregTech is loaded,
 * run by the client run gregtechTests - see reactorcraft/build.gradle): a simulated player connects ElectriCraft's FE (RF) power to
 * GregTech's EU, in survival and in creative.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import reika.electricraft.content.blockentity.RFBatteryBlockEntity;
import reika.electricraft.registry.ElectriBlocks;
import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.gametest.SimPlayer;

@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = ReactorCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class GregTechTests {

	@SubscribeEvent
	public static void register(RegisterGameTestsEvent e) {
		if (ModList.get().isLoaded("gtceu"))
			e.register(GregTechTests.class);
	}

	public static Item gt(String path) {
		Item i = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gtceu", path));
		if (i == Items.AIR)
			throw new IllegalStateException("no gtceu:"+path);
		return i;
	}

	/** A GregTech tool item of that type ("mallet", "wire_cutter"), of the preferred material if there is one. */
	public static Item tool(String type, String material) {
		Item any = null;
		for (Item i : BuiltInRegistries.ITEM) {
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(i);
			if (id.getNamespace().equals("gtceu") && id.getPath().endsWith("_"+type) && !id.getPath().startsWith("lv_") && !id.getPath().startsWith("hv_")
					&& !id.getPath().startsWith("iv_")) {
				if (id.getPath().equals(material+"_"+type))
					return i;
				if (any == null)
					any = i;
			}
		}
		if (any == null)
			throw new IllegalStateException("no GregTech "+type);
		return any;
	}

	/**
	 * A charged ElectriCraft RF battery (redstone on) under a GregTech LV energy converter, which the player switches to FE -> EU with
	 * the soft mallet; its front (it faces the player: west) feeds tin cables to an LV electric furnace, which smelts the raw iron a hopper
	 * on it gives it, and through a branch of the cable a second RF battery (GregTech cables give EU to FE blocks natively, 1 EU = 4 FE):
	 * both work on the converted power. GregTech cables connect only to a cable they are set onto (GT6 style, the default); the player
	 * opens the other sides with the wire cutter, as in the game.
	 */
	private static void feToEu(GameTestHelper h, GameType mode) {
		SimPlayer p = new SimPlayer(h, mode);
		SimPlayer.floor(h, 0, 0, 8, 8);
		Item converter = gt("lv_1a_energy_converter"), cable = gt("tin_single_cable"), furnace = gt("lv_electric_furnace"), mallet = tool("mallet", "rubber"), cutter = tool("wire_cutter", "iron");
		if (p.survival()) {
			p.give(ElectriBlocks.RF_BATTERY.get().asItem(), 2);
			p.give(converter, 1);
			p.give(cable, 3);
			p.give(furnace, 1);
			p.give(Items.HOPPER, 1);
			p.give(Items.RAW_IRON, 4);
			p.give(Items.REDSTONE_BLOCK, 1);
			p.give(mallet, 1);
			p.give(cutter, 1);
		}
		BlockPos a = new BlockPos(6, 2, 4), b = new BlockPos(5, 2, 2), f = new BlockPos(4, 2, 4), c = a.above();
		p.place(a, new ItemStack(ElectriBlocks.RF_BATTERY.get()), Direction.EAST);
		((RFBatteryBlockEntity)h.getBlockEntity(a)).setStoredEnergy(1000000); //a charged battery (charging it is tested elsewhere)
		p.place(a.south(), new ItemStack(Items.REDSTONE_BLOCK), Direction.EAST);
		p.placeSneaking(c, new ItemStack(converter), Direction.EAST);
		p.use(c, mallet);
		BlockPos k1 = new BlockPos(5, 2, 4), k2 = k1.above(), k3 = k1.north();
		p.place(k1, new ItemStack(cable), Direction.EAST);
		p.place(k2, new ItemStack(cable), Direction.EAST); //set onto the first cable: joined to it
		p.place(f, new ItemStack(furnace), Direction.EAST);
		p.place(k3, new ItemStack(cable), Direction.EAST);
		p.place(b, new ItemStack(ElectriBlocks.RF_BATTERY.get()), Direction.EAST);
		p.hold(cutter);
		p.useOn(k2, Direction.EAST); //to the converter
		p.useOn(k1, Direction.WEST); //to the furnace
		p.useOn(k3, Direction.SOUTH); //branch onto the line
		p.useOn(k3, Direction.NORTH); //to the second battery
		p.placeSneaking(f.above(), new ItemStack(Items.HOPPER), Direction.EAST);
		p.open(f.above());
		p.putInto(0, Items.RAW_IRON, 4);
		p.closeMenu();
		if (p.survival())
			h.assertTrue(p.count(converter) == 0 && p.count(cable) == 0 && p.count(Items.RAW_IRON) == 0,
					"items not used up: converter "+p.count(converter)+", cable "+p.count(cable)+", raw iron "+p.count(Items.RAW_IRON));
		h.assertTrue(h.getBlockState(c).getBlock() != Blocks.AIR, "no converter");
		h.succeedWhen(() -> {
			IItemHandler fh = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(f), Direction.WEST);
			int ingots = 0;
			if (fh != null)
				for (int i = 0; i < fh.getSlots(); i++)
					if (fh.getStackInSlot(i).is(Items.IRON_INGOT))
						ingots += fh.getStackInSlot(i).getCount();
			h.assertTrue(ingots > 0, "the electric furnace smelted nothing: "+state(h, a, c, f, b));
			long eb = ((RFBatteryBlockEntity)h.getBlockEntity(b)).getStoredEnergy();
			h.assertTrue(eb > 0, "no EU reached the second RF battery as FE: "+state(h, a, c, f, b));
			p.leave();
		});
	}

	private static String eu(GameTestHelper h, BlockPos p) {
		StringBuilder sb = new StringBuilder();
		for (Direction d : Direction.values()) {
			var e = h.getLevel().getCapability(com.gregtechceu.gtceu.api.capability.GTCapability.CAPABILITY_ENERGY_CONTAINER, h.absolutePos(p), d);
			if (e != null) {
				sb.append(d).append('=').append(e.getEnergyStored()).append('/').append(e.getEnergyCapacity()).append(e.inputsEnergy(d) ? " in" : "").append(e.outputsEnergy(d) ? " out" : "").append(' ');
			}
		}
		return sb.toString();
	}

	private static String items(GameTestHelper h, BlockPos p) {
		IItemHandler ih = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(p), Direction.UP);
		if (ih == null)
			return "-";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ih.getSlots(); i++)
			if (!ih.getStackInSlot(i).isEmpty())
				sb.append(i).append(':').append(ih.getStackInSlot(i)).append(' ');
		return sb.toString();
	}

	private static String state(GameTestHelper h, BlockPos a, BlockPos c, BlockPos f, BlockPos b) {
		return "battery A "+((RFBatteryBlockEntity)h.getBlockEntity(a)).getStoredEnergy()+" FE; converter "+h.getBlockState(c)+" EU "+eu(h, c)
				+"; cables "+eu(h, a.west())+" | "+eu(h, a.west().above())+" | "+eu(h, a.west().north())+"; furnace "+h.getBlockState(f)+" EU "+eu(h, f)+" items "+items(h, f)+"; hopper "+items(h, f.above())
				+"; battery B "+((RFBatteryBlockEntity)h.getBlockEntity(b)).getStoredEnergy()+" FE";
	}

	@GameTest(templateNamespace = ReactorCraft.MOD_ID, template = "platform_9x8x9", batch = "gregtech", timeoutTicks = 600)
	public static void survivalFeToEu(GameTestHelper h) {
		feToEu(h, GameType.SURVIVAL);
	}

	@GameTest(templateNamespace = ReactorCraft.MOD_ID, template = "platform_9x8x9", batch = "gregtech", timeoutTicks = 600)
	public static void creativeFeToEu(GameTestHelper h) {
		feToEu(h, GameType.CREATIVE);
	}
}
