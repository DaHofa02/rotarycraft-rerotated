/*******************************************************************************
 * Test tool of the private NeoForge 1.21.1 ports of RotaryCraft, ReactorCraft and ElectriCraft (not part of the original mods): the op
 * command "/reikadev base" has a simulated pair of hands (with the caller's profile, so the caller owns what is placed) build a small
 * working base in front of the caller, in creative, the way the live GameTests build it - every block placed from its item while looking
 * the right way, filled by bucket and through its GUI: a shaft line with a lubricated gearbox and a dynamometer, a grinder on a gas
 * engine, a fermenter fed from a reservoir by pipes, an ElectriCraft generator - wire - motor line with a battery, a 3x3 fission core
 * (unfuelled; the fuel is in the chest beside it) with a coolant cell, the three fusion multiblocks (formed) and a toroid magnet, and with
 * Mekanism an RF battery - universal cable - RF battery line and a fluid tank under a RotaryCraft pipe, and with GregTech an RF battery
 * feeding an LV energy converter (FE -> EU) that runs an LV electric furnace and charges a second RF battery over tin cables. With the system property
 * "reactorcraft.showcase.base" it is built at (0, -61, 300) in the showcase world on its first tick.
 ******************************************************************************/
package reika.reactorcraft.dev;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import reika.electricraft.content.blockentity.RFBatteryBlockEntity;
import reika.electricraft.registry.BatteryType;
import reika.electricraft.registry.ElectriBlocks;
import reika.electricraft.registry.WireType;
import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorItems;
import reika.reactorcraft.structure.Blueprints;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.block.ShaftBlock;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@EventBusSubscriber(modid = ReactorCraft.MOD_ID)
public final class BaseScene {

	private static final boolean AUTO = System.getProperty("reactorcraft.showcase.base") != null;
	public static final BlockPos AUTO_ORIGIN = new BlockPos(0, -61, 300);
	private static final int SIZE_X = 60, SIZE_Z = 44;

	private static boolean autoDone;

	private BaseScene() {}

	@SubscribeEvent
	public static void commands(RegisterCommandsEvent e) {
		CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
		d.register(Commands.literal("reikadev").requires(s -> s.hasPermission(2))
				.then(Commands.literal("base").executes(c -> {
					BlockPos p = BlockPos.containing(c.getSource().getPosition()).offset(2, -1, 2);
					ServerPlayer sp = c.getSource().getPlayer();
					build(c.getSource().getLevel(), p, sp != null ? sp.getGameProfile() : null);
					c.getSource().sendSuccess(() -> Component.literal("Base built at "+p.toShortString()+" ("+SIZE_X+" x "+SIZE_Z+")"), true);
					return 1;
				})));
	}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post e) {
		if (AUTO && !autoDone) {
			autoDone = true;
			ServerLevel w = e.getServer().overworld();
			for (int cx = (AUTO_ORIGIN.getX()-8) >> 4; cx <= (AUTO_ORIGIN.getX()+SIZE_X+8) >> 4; cx++)
				for (int cz = (AUTO_ORIGIN.getZ()-8) >> 4; cz <= (AUTO_ORIGIN.getZ()+SIZE_Z+8) >> 4; cz++)
					w.setChunkForced(cx, cz, true);
			build(w, AUTO_ORIGIN, null);
		}
	}

	private static Block mek(String id) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mekanism", id));
	}

	private static Map<BlockPos, Block> blueprint(String id, BlockPos at) {
		for (Blueprints.Blueprint b : Blueprints.all())
			if (b.id().equals(id)) {
				Map<BlockPos, Block> m = new HashMap<>();
				b.blocks().forEach((k, v) -> m.put(k.offset(at), v));
				return m;
			}
		throw new IllegalArgumentException(id);
	}

	/** Builds the base with its floor's north-west corner at {@code o} (floor at y = o, machines on it); {@code as} owns it. */
	public static void build(ServerLevel w, BlockPos o, GameProfile as) {
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-1, 1, -2), o.offset(SIZE_X, 14, SIZE_Z)))
			w.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-1, 0, -2), o.offset(SIZE_X, 0, SIZE_Z)))
			w.setBlock(p, Blocks.SMOOTH_STONE.defaultBlockState(), 2);
		SimPlayer p = new SimPlayer(w, o, GameType.CREATIVE, as != null ? as : new GameProfile(UUID.randomUUID(), "base"));
		try {
			rotary(w, o, p);
			electric(w, o, p);
			fission(w, o, p);
			fusion(w, o, p);
			mekanism(w, o, p);
			gregtech(w, o, p);
			connected(w, o, p);
		}
		finally {
			p.leave();
		}
	}

	/** RotaryCraft: the power line, the grinder and the fermenter of LiveBaseTests. */
	private static void rotary(ServerLevel w, BlockPos o, SimPlayer p) {
		//shaft line (z = 2): DC engine (redstone) -> steel shaft -> steel 2:1 gearbox (lubricant bucket) -> dynamometer
		p.place(new BlockPos(6, 1, 2), new ItemStack(RotaryBlocks.DC_ENGINE.get()), Direction.EAST);
		p.place(new BlockPos(5, 1, 2), ShaftBlock.shaftItem(MaterialRegistry.STEEL, false), Direction.EAST);
		p.place(new BlockPos(4, 1, 2), GearboxBlock.gearboxItem(GearboxTypes.STEEL, 2), Direction.EAST);
		p.place(new BlockPos(3, 1, 2), new ItemStack(RotaryBlocks.DYNAMOMETER.get()), Direction.EAST);
		p.place(new BlockPos(6, 1, 3), new ItemStack(Items.REDSTONE_BLOCK), Direction.EAST);
		p.use(new BlockPos(4, 1, 2), RotaryItems.LUBRICANT_BUCKET.get());
		CompatScene.sign(w, o.offset(5, 1, 1), "Shaft line", "DC engine, 2:1", "gearbox (oiled),", "dynamometer");
		//grinder (z = 6) on a gas engine with ethanol, seeds in it
		p.place(new BlockPos(6, 1, 6), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		p.place(new BlockPos(5, 1, 6), new ItemStack(RotaryBlocks.GRINDER.get()), Direction.EAST);
		p.open(new BlockPos(6, 1, 6));
		p.putInto(0, RotaryItems.ETHANOL.get(), 32);
		p.closeMenu();
		p.open(new BlockPos(5, 1, 6));
		p.putInto(0, RotaryItems.CANOLA_SEEDS.get(), 32);
		p.closeMenu();
		CompatScene.sign(w, o.offset(5, 1, 5), "Grinder", "on a gas engine", "(ethanol)", "canola -> husks");
		//fermenter (z = 10): DC engine behind it, water from a reservoir on a pipe line
		p.place(new BlockPos(6, 1, 10), new ItemStack(RotaryBlocks.DC_ENGINE.get()), Direction.EAST);
		p.place(new BlockPos(6, 1, 11), new ItemStack(Items.REDSTONE_BLOCK), Direction.EAST);
		p.place(new BlockPos(5, 1, 10), new ItemStack(RotaryBlocks.FERMENTER.get()), Direction.EAST);
		p.place(new BlockPos(2, 1, 10), new ItemStack(Items.COBBLESTONE), Direction.EAST);
		p.place(new BlockPos(2, 2, 10), new ItemStack(RotaryBlocks.RESERVOIR.get()), Direction.EAST);
		p.mine(new BlockPos(2, 1, 10));
		for (int x = 2; x <= 4; x++)
			p.place(new BlockPos(x, 1, 10), new ItemStack(RotaryBlocks.PIPE.get()), Direction.EAST);
		for (int i = 0; i < 4; i++)
			p.use(new BlockPos(2, 2, 10), Items.WATER_BUCKET);
		p.open(new BlockPos(5, 1, 10));
		p.putInto(0, Items.SUGAR, 32);
		p.putInto(1, Items.DIRT, 32);
		p.closeMenu();
		CompatScene.sign(w, o.offset(4, 1, 9), "Fermenter", "reservoir water", "by pipe; sugar +", "dirt -> yeast");
	}

	/** ElectriCraft: the generator - wire - motor line of LiveElectricTests, with a battery on a branch of the wire. */
	private static void electric(ServerLevel w, BlockPos o, SimPlayer p) {
		int z = 15;
		p.place(new BlockPos(8, 1, z), new ItemStack(RotaryBlocks.DC_ENGINE.get()), Direction.EAST);
		p.place(new BlockPos(8, 1, z+1), new ItemStack(Items.REDSTONE_BLOCK), Direction.EAST);
		p.place(new BlockPos(7, 1, z), GearboxBlock.gearboxItem(GearboxTypes.WOOD, 4), Direction.EAST);
		p.place(new BlockPos(6, 1, z), new ItemStack(ElectriBlocks.GENERATOR.get()), Direction.EAST);
		p.place(new BlockPos(5, 1, z), new ItemStack(ElectriBlocks.getWire(WireType.STEEL, false)), Direction.EAST);
		p.place(new BlockPos(4, 1, z), new ItemStack(ElectriBlocks.MOTOR.get()), Direction.EAST);
		p.place(new BlockPos(3, 1, z), new ItemStack(RotaryBlocks.DYNAMOMETER.get()), Direction.EAST);
		p.place(new BlockPos(5, 1, z+1), new ItemStack(ElectriBlocks.getWire(WireType.STEEL, false)), Direction.EAST);
		p.place(new BlockPos(5, 1, z+2), new ItemStack(ElectriBlocks.battery(BatteryType.REDSTONE).get()), Direction.EAST);
		CompatScene.sign(w, o.offset(6, 1, z-1), "ElectriCraft", "generator, wire,", "motor; battery", "on the branch");
	}

	/** ReactorCraft fission: a 3x3 of fuel cores with a coolant cell (filled by bucket); fuel for them in the chest. */
	private static void fission(ServerLevel w, BlockPos o, SimPlayer p) {
		for (int i = 0; i < 9; i++)
			p.place(new BlockPos(16+i%3, 1, 3+i/3), new ItemStack(ReactorBlocks.FUEL_ROD.get()), Direction.NORTH);
		BlockPos cell = new BlockPos(19, 1, 4);
		p.place(cell, new ItemStack(ReactorBlocks.WATER_CELL.get()), Direction.NORTH);
		p.use(cell, Items.WATER_BUCKET);
		BlockPos chest = new BlockPos(14, 1, 4);
		p.place(chest, new ItemStack(Items.CHEST), Direction.NORTH);
		if (w.getBlockEntity(chest.offset(o)) instanceof ChestBlockEntity c)
			c.setItem(0, new ItemStack(ReactorItems.FUEL.get(), 36));
		CompatScene.sign(w, o.offset(17, 1, 2), "Fission core", "4 fuel (chest)", "into each core:", "it starts itself");
	}

	/** ReactorCraft fusion: solenoid, preheater and injector built part by part (each forms with its last part), a toroid magnet. */
	private static void fusion(ServerLevel w, BlockPos o, SimPlayer p) {
		BlockPos sc = new BlockPos(44, 2, 28);
		p.build(blueprint("solenoid", sc), List.of(sc, sc.offset(7, 0, 0)), Direction.NORTH);
		CompatScene.sign(w, o.offset(44, 1, 19), "Solenoid", "magnet (formed)", "", "");
		BlockPos hc = new BlockPos(24, 1, 2);
		p.build(blueprint("preheater", hc), List.of(hc.offset(2, 2, 2), hc.offset(4, 4, 4)), Direction.NORTH);
		CompatScene.sign(w, o.offset(26, 1, 1), "Hydrogen", "preheater", "(formed)", "");
		BlockPos ic = new BlockPos(16, 2, 26);
		BlockPos injector = ic.east(2);
		p.build(blueprint("injector", ic), List.of(injector, ic, ic.above(), ic.above(2)), Direction.SOUTH);
		CompatScene.sign(w, o.offset(20, 1, 24), "Plasma injector", "(formed)", "", "");
		BlockPos m = new BlockPos(30, 1, 18);
		p.place(m, new ItemStack(ReactorBlocks.TOROID_MAGNET.get()), Direction.NORTH);
		CompatScene.sign(w, o.offset(30, 1, 17), "Toroid magnet", "screwdriver", "turns its aim", "");
	}

	/** Mekanism (if present): RF battery - universal cables - RF battery, and a fluid tank (water bucket) under a RotaryCraft pipe. */
	private static void mekanism(ServerLevel w, BlockPos o, SimPlayer p) {
		if (mek("basic_universal_cable") == Blocks.AIR)
			return;
		ItemStack cable = new ItemStack(mek("basic_universal_cable"));
		int z = 36;
		BlockPos a = new BlockPos(2, 1, z), b = new BlockPos(8, 1, z);
		p.place(a, new ItemStack(ElectriBlocks.RF_BATTERY.get()), Direction.NORTH);
		if (w.getBlockEntity(a.offset(o)) instanceof RFBatteryBlockEntity ba)
			ba.setStoredEnergy(1000000);
		p.place(a.north(), new ItemStack(Items.REDSTONE_BLOCK), Direction.NORTH);
		for (int x = 2; x <= 7; x++)
			p.place(new BlockPos(x, 2, z), cable, Direction.NORTH);
		p.place(new BlockPos(7, 1, z), cable, Direction.NORTH);
		p.place(b, new ItemStack(ElectriBlocks.RF_BATTERY.get()), Direction.NORTH);
		CompatScene.sign(w, o.offset(5, 1, z-1), "FE: RF battery", "-> Mekanism", "cables -> RF", "battery");
		BlockPos t = new BlockPos(12, 1, z);
		p.place(t, new ItemStack(mek("basic_fluid_tank")), Direction.NORTH);
		for (int i = 0; i < 8; i++)
			p.use(t, Items.WATER_BUCKET);
		p.placeSneaking(t.above(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.NORTH);
		CompatScene.sign(w, o.offset(12, 1, z-1), "Mekanism tank", "(water) under a", "RotaryCraft pipe", "");
	}

	/** Connected textures (rotarycraft/client/ConnectedTextures): a 3x3 wall and a 3x3 floor of each of a few plain blocks. */
	private static void connected(ServerLevel w, BlockPos o, SimPlayer p) {
		String[] ids = {"graphite_block", "flywheel_frame", "heater_housing_edge", "generator_core", "injector_side_panel"};
		for (int k = 0; k < ids.length; k++) {
			Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, ids[k]));
			if (b == Blocks.AIR)
				continue;
			int x0 = 34+4*k;
			for (int y = 1; y <= 3; y++)
				for (int x = x0; x < x0+3; x++)
					p.place(new BlockPos(x, y, 7), new ItemStack(b), Direction.NORTH);
			for (int z = 10; z < 13; z++)
				for (int x = x0; x < x0+3; x++)
					p.place(new BlockPos(x, 1, z), new ItemStack(b), Direction.NORTH);
		}
		CompatScene.sign(w, o.offset(33, 1, 6), "Connected", "textures", "", "");
	}

	/**
	 * GregTech (if present), as in GregTechTests: a charged RF battery (redstone) under an LV energy converter switched to FE -> EU with
	 * the soft mallet, tin cables (sides opened with the wire cutter) to an LV electric furnace with a hopper of raw iron and to a second
	 * RF battery.
	 */
	private static void gregtech(ServerLevel w, BlockPos o, SimPlayer p) {
		if (!net.neoforged.fml.ModList.get().isLoaded("gtceu"))
			return;
		net.minecraft.world.item.Item converter = reika.reactorcraft.gametest.GregTechTests.gt("lv_1a_energy_converter");
		net.minecraft.world.item.Item cable = reika.reactorcraft.gametest.GregTechTests.gt("tin_single_cable");
		net.minecraft.world.item.Item furnace = reika.reactorcraft.gametest.GregTechTests.gt("lv_electric_furnace");
		int x = 20, z = 38;
		BlockPos a = new BlockPos(x+6, 1, z), b = new BlockPos(x+5, 1, z-2), f = new BlockPos(x+4, 1, z), c = a.above();
		p.place(a, new ItemStack(ElectriBlocks.RF_BATTERY.get()), Direction.EAST);
		if (w.getBlockEntity(a.offset(o)) instanceof RFBatteryBlockEntity ba)
			ba.setStoredEnergy(1000000);
		p.place(a.south(), new ItemStack(Items.REDSTONE_BLOCK), Direction.EAST);
		p.placeSneaking(c, new ItemStack(converter), Direction.EAST);
		p.use(c, reika.reactorcraft.gametest.GregTechTests.tool("mallet", "rubber"));
		BlockPos k1 = new BlockPos(x+5, 1, z), k2 = k1.above(), k3 = k1.north();
		p.place(k1, new ItemStack(cable), Direction.EAST);
		p.place(k2, new ItemStack(cable), Direction.EAST);
		p.place(f, new ItemStack(furnace), Direction.EAST);
		p.place(k3, new ItemStack(cable), Direction.EAST);
		p.place(b, new ItemStack(ElectriBlocks.RF_BATTERY.get()), Direction.EAST);
		p.hold(reika.reactorcraft.gametest.GregTechTests.tool("wire_cutter", "iron"));
		p.useOn(k2, Direction.EAST);
		p.useOn(k1, Direction.WEST);
		p.useOn(k3, Direction.SOUTH);
		p.useOn(k3, Direction.NORTH);
		p.placeSneaking(f.above(), new ItemStack(Items.HOPPER), Direction.EAST);
		p.open(f.above());
		p.putInto(0, Items.RAW_IRON, 64);
		p.closeMenu();
		CompatScene.sign(w, o.offset(x+3, 1, z-1), "GregTech: RF", "battery -> FE/EU", "converter -> LV", "furnace, battery");
	}
}
