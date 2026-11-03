/*******************************************************************************
 * Live scenarios of the private NeoForge 1.21.1 port of ReactorCraft: a simulated player builds a fission core and the fusion
 * multiblocks block by block from items, fills and uses them by hand, and links ElectriCraft/RotaryCraft blocks with Mekanism ones - in
 * survival (crafting, items used up) and in creative. Not part of the original mod; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.electricraft.content.blockentity.RFBatteryBlockEntity;
import reika.electricraft.registry.ElectriBlocks;
import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.fusion.FusionHeaterBlockEntity;
import reika.reactorcraft.fusion.FusionInjectorBlockEntity;
import reika.reactorcraft.fusion.FusionMultiBlock;
import reika.reactorcraft.fusion.SolenoidMagnetBlockEntity;
import reika.reactorcraft.fusion.ToroidMagnetBlockEntity;
import reika.reactorcraft.reactor.FuelRodBlockEntity;
import reika.reactorcraft.reactor.LiquidStates;
import reika.reactorcraft.reactor.WaterCellBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorItems;
import reika.reactorcraft.structure.Blueprints;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class LiveReactorTests {

	private static final String SMALL = "platform_9x8x9";
	private static final String BIG = "platform_48x12x48";

	private static ResourceLocation id(String ns, String path) {
		return ResourceLocation.fromNamespaceAndPath(ns, path);
	}

	/** Survival: crafting table -> worktable at (3, 2, 1), opened. */
	private static void worktable(SimPlayer p) {
		p.give(Items.CRAFTING_TABLE, 1);
		p.place(new BlockPos(1, 2, 1), Items.CRAFTING_TABLE, Direction.NORTH);
		p.open(new BlockPos(1, 2, 1));
		p.gatherFor(id("rotarycraft", "worktable"));
		ItemStack wt = p.craft(id("rotarycraft", "worktable"), 1, 0);
		p.closeMenu();
		p.place(new BlockPos(3, 2, 1), wt.getItem(), Direction.NORTH);
		p.open(new BlockPos(3, 2, 1));
	}

	private static ItemStack craft(SimPlayer p, String recipe, int times) {
		ItemStack out = ItemStack.EMPTY;
		for (int i = 0; i < times; i++) {
			p.gatherFor(id(ReactorCraft.MOD_ID, recipe));
			out = p.craft(id(ReactorCraft.MOD_ID, recipe), 0, 13);
		}
		return out;
	}

	/**
	 * A small fission core: nine fuel cores (survival: crafted in the worktable) set down in a 3x3, each opened and given four fuel
	 * pellets through its GUI; a coolant cell beside it filled by right-clicking it with a water bucket (the bucket comes back empty; the
	 * water boils off as it cools the hot core, as in the original). Decay neutrons start fission: the centre core becomes active and heats up.
	 */
	private static void fissionCore(GameTestHelper h, GameType mode) {
		SimPlayer p = new SimPlayer(h, mode);
		SimPlayer.floor(h, 0, 0, 8, 8);
		ItemStack core = new ItemStack(ReactorBlocks.FUEL_ROD.get()), cell = new ItemStack(ReactorBlocks.WATER_CELL.get());
		if (p.survival()) {
			worktable(p);
			core = craft(p, "fuel_rod_worktable", 9);
			cell = craft(p, "coolant_cell_worktable", 1);
			p.closeMenu();
			p.give(ReactorItems.FUEL.get(), 36);
			p.give(Items.WATER_BUCKET, 1);
		}
		List<BlockPos> cores = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			BlockPos c = new BlockPos(3+i%3, 2, 4+i/3);
			p.place(c, core, Direction.NORTH);
			cores.add(c);
		}
		for (BlockPos c : cores) {
			p.open(c);
			for (int s = 0; s < 4; s++)
				p.putInto(s, ReactorItems.FUEL.get(), 1);
			p.closeMenu();
		}
		BlockPos w = new BlockPos(6, 2, 5);
		p.place(w, cell, Direction.NORTH);
		p.use(w, Items.WATER_BUCKET);
		h.assertTrue(((WaterCellBlockEntity)h.getBlockEntity(w)).getLiquidState() == LiquidStates.WATER, "the water bucket did not fill the coolant cell");
		if (p.survival())
			h.assertTrue(p.count(ReactorItems.FUEL.get()) == 0 && p.count(Items.BUCKET) == 1, "fuel left "+p.count(ReactorItems.FUEL.get())+", buckets "+p.count(Items.BUCKET));
		FuelRodBlockEntity mid = (FuelRodBlockEntity)h.getBlockEntity(cores.get(4));
		int[] peak = {0};
		h.onEachTick(() -> peak[0] = Math.max(peak[0], mid.getTemperature()));
		h.succeedWhen(() -> {
			h.assertTrue(mid.isActive() && peak[0] >= 40, "centre core: active "+mid.isActive()+", peak "+peak[0]+" C");
			p.leave();
		});
	}

	@GameTest(template = SMALL, timeoutTicks = 1500)
	public static void survivalFissionCore(GameTestHelper h) {
		fissionCore(h, GameType.SURVIVAL);
	}

	@GameTest(template = SMALL, timeoutTicks = 1500)
	public static void creativeFissionCore(GameTestHelper h) {
		fissionCore(h, GameType.CREATIVE);
	}

	private static Map<BlockPos, Block> blueprint(String bp, BlockPos at) {
		for (Blueprints.Blueprint b : Blueprints.all())
			if (b.id().equals(bp)) {
				Map<BlockPos, Block> m = new HashMap<>();
				b.blocks().forEach((k, v) -> m.put(k.offset(at), v));
				return m;
			}
		throw new IllegalArgumentException(bp);
	}

	/** Survival: the parts are in the player's inventory (crafted elsewhere); the machines at the heart are crafted here. */
	private static void supply(SimPlayer p, Map<BlockPos, Block> bp) {
		if (!p.survival())
			return;
		Map<Block, Integer> n = new HashMap<>();
		for (Block b : bp.values())
			n.merge(b, 1, Integer::sum);
		n.forEach((b, c) -> {
			for (int left = c; left > 0; left -= 64)
				p.give(new ItemStack(b, Math.min(64, left)));
		});
	}

	/**
	 * The fusion multiblocks built block by block from items, as a player does (each next to what is already there, on a floor):
	 * the solenoid (its last part completes and forms it - the solenoid magnet then has its multiblock), the hydrogen preheater (formed,
	 * the heater inside sees it) and a plasma injector frame (the coil column placed last forms it; the injector faces the way the player
	 * looked when placing it, reversed). A toroid magnet turned eight times with the screwdriver points west.
	 */
	private static void fusionBuild(GameTestHelper h, GameType mode) {
		SimPlayer p = new SimPlayer(h, mode);
		SimPlayer.floor(h, 0, 0, 47, 47);
		//solenoid, centre (24, 3, 24): its lower rails stand on the floor
		BlockPos sc = new BlockPos(24, 3, 24);
		Map<BlockPos, Block> sol = blueprint("solenoid", sc);
		supply(p, sol);
		p.build(sol, List.of(sc, sc.offset(7, 0, 0)), Direction.NORTH);
		SolenoidMagnetBlockEntity s = (SolenoidMagnetBlockEntity)h.getBlockEntity(sc);
		h.assertTrue(s.hasMultiBlock(), "the solenoid did not form when its last part was placed");
		h.assertTrue(h.getBlockState(sc.offset(8, 0, 0)).getValue(FusionMultiBlock.FORMED), "solenoid parts not formed");
		//preheater, corner (2, 2, 2)
		BlockPos hc = new BlockPos(2, 2, 2);
		Map<BlockPos, Block> heat = blueprint("preheater", hc);
		supply(p, heat);
		p.build(heat, List.of(hc.offset(2, 2, 2), hc.offset(4, 4, 4)), Direction.NORTH);
		FusionHeaterBlockEntity hb = (FusionHeaterBlockEntity)h.getBlockEntity(hc.offset(2, 2, 2));
		h.assertTrue(hb.hasMultiBlock(), "the preheater did not form");
		//injector frame along +x from (2, 3, 40); the injector placed looking south, so it faces north
		BlockPos ic = new BlockPos(2, 3, 40);
		Map<BlockPos, Block> inj = blueprint("injector", ic);
		supply(p, inj);
		BlockPos injector = ic.east(2);
		p.build(inj, List.of(injector, ic, ic.above(), ic.above(2)), Direction.SOUTH);
		FusionInjectorBlockEntity ib = (FusionInjectorBlockEntity)h.getBlockEntity(injector);
		h.assertTrue(ib.getFacing() == Direction.NORTH, "injector faces "+ib.getFacing());
		h.assertTrue(ib.hasMultiBlock(), "the injector frame did not form");
		//a toroid magnet, aimed with the screwdriver
		BlockPos m = new BlockPos(40, 2, 4);
		if (p.survival())
			p.give(ReactorBlocks.TOROID_MAGNET.get().asItem(), 1);
		p.place(m, new ItemStack(ReactorBlocks.TOROID_MAGNET.get()), Direction.NORTH);
		if (p.survival())
			p.give(RotaryItems.SCREWDRIVER.get(), 1);
		for (int i = 0; i < 8; i++)
			p.use(m, RotaryItems.SCREWDRIVER.get());
		ToroidMagnetBlockEntity tm = (ToroidMagnetBlockEntity)h.getBlockEntity(m);
		h.assertTrue(tm.getAim() == ToroidMagnetBlockEntity.Aim.W, "magnet aims "+tm.getAim());
		p.leave();
		h.succeed();
	}

	@GameTest(template = BIG, timeoutTicks = 100)
	public static void survivalFusionBuild(GameTestHelper h) {
		fusionBuild(h, GameType.SURVIVAL);
	}

	@GameTest(template = BIG, timeoutTicks = 100)
	public static void creativeFusionBuild(GameTestHelper h) {
		fusionBuild(h, GameType.CREATIVE);
	}

	private static Block mek(String path) {
		return BuiltInRegistries.BLOCK.get(id("mekanism", path));
	}

	/**
	 * With Mekanism: the player sets down an ElectriCraft RF battery (with a redstone block), a line of Mekanism universal cables from its
	 * top to a second RF battery's side, and the energy flows; then a Mekanism fluid tank, filled with a water bucket, under a RotaryCraft
	 * pipe the player lays on it: the pipe takes the water.
	 */
	private static void mekanismBase(GameTestHelper h, GameType mode) {
		if (mek("basic_universal_cable") == Blocks.AIR) {
			h.succeed();
			return;
		}
		SimPlayer p = new SimPlayer(h, mode);
		SimPlayer.floor(h, 0, 0, 8, 8);
		ItemStack cable = new ItemStack(mek("basic_universal_cable")), tank = new ItemStack(mek("basic_fluid_tank"));
		if (p.survival()) {
			p.give(ElectriBlocks.RF_BATTERY.get().asItem(), 2);
			p.give(cable.copyWithCount(8));
			p.give(tank);
			p.give(RotaryBlocks.PIPE.get().asItem(), 1);
			p.give(Items.REDSTONE_BLOCK, 1);
			p.give(Items.WATER_BUCKET, 1);
		}
		BlockPos a = new BlockPos(1, 2, 4), b = new BlockPos(7, 2, 4);
		p.place(a, new ItemStack(ElectriBlocks.RF_BATTERY.get()), Direction.NORTH);
		((RFBatteryBlockEntity)h.getBlockEntity(a)).setStoredEnergy(1000000); //a charged battery (charging it is tested elsewhere)
		p.place(a.north(), new ItemStack(Items.REDSTONE_BLOCK), Direction.NORTH);
		for (int x = 1; x <= 6; x++)
			p.place(new BlockPos(x, 3, 4), cable, Direction.NORTH);
		p.place(new BlockPos(6, 2, 4), cable, Direction.NORTH);
		p.place(b, new ItemStack(ElectriBlocks.RF_BATTERY.get()), Direction.NORTH);
		BlockPos t = new BlockPos(4, 2, 7);
		p.place(t, tank, Direction.NORTH);
		p.use(t, Items.WATER_BUCKET);
		IFluidHandler th = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(t), Direction.UP);
		h.assertTrue(th != null && th.getFluidInTank(0).getFluid().isSame(Fluids.WATER) && th.getFluidInTank(0).getAmount() >= 1000,
				"the water bucket did not fill the Mekanism tank: "+(th == null ? "no capability" : th.getFluidInTank(0).getAmount()+" mB"));
		p.placeSneaking(t.above(), new ItemStack(RotaryBlocks.PIPE.get()), Direction.NORTH);
		h.succeedWhen(() -> {
			long eb = ((RFBatteryBlockEntity)h.getBlockEntity(b)).getStoredEnergy();
			h.assertTrue(eb > 0, "no energy reached the second battery");
			h.assertTrue(((PipingBlockEntity)h.getBlockEntity(t.above())).getFluidLevel() > 0, "the pipe took no water from the tank");
			p.leave();
		});
	}

	@GameTest(template = SMALL, timeoutTicks = 200)
	public static void survivalMekanismBase(GameTestHelper h) {
		mekanismBase(h, GameType.SURVIVAL);
	}

	@GameTest(template = SMALL, timeoutTicks = 200)
	public static void creativeMekanismBase(GameTestHelper h) {
		mekanismBase(h, GameType.CREATIVE);
	}
}
