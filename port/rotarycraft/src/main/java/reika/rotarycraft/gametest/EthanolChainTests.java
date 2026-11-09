/*******************************************************************************
 * Regression tests for the ported Fermenter and Centrifuge: sugar + dirt -> yeast, yeast + plants -> sludge, centrifuge
 * sludge -> clean sludge (+ compost), clean sludge smelts to ethanol; canola husks -> lubricant. Values from the original code.
 * Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.CentrifugeBlockEntity;
import reika.rotarycraft.content.blockentity.FermenterBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class EthanolChainTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (!cls.isInstance(be))
			throw new IllegalStateException("no "+cls.getSimpleName()+" at "+p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine (512 rad/s @ 128 Nm) straight into a Fermenter (meta 0 reads from the east), 4000 mB water, kept at 25 °C to start. */
	private static FermenterBlockEntity fermenterRig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 1, 4, 0, EngineBlockEntity.class).addFuel(50000);
		FermenterBlockEntity f = place(h, RotaryBlocks.FERMENTER.get(), 4, 1, 4, 0, FermenterBlockEntity.class);
		f.addWater(4000);
		//held at 25 C: the ambient temperature follows the biome under the test, and with more tests some land in cold biomes (8 C)
		h.onEachTick(() -> f.setTemperature(25));
		return f;
	}

	/** Without redstone: sugar + dirt -> yeast, 50 mB water per yeast. */
	@GameTest(template = PLATFORM, timeoutTicks = 1200)
	public static void fermenterMakesYeast(GameTestHelper h) {
		FermenterBlockEntity f = fermenterRig(h);
		f.getInventory().setStackInSlot(0, new ItemStack(Items.SUGAR, 4));
		f.getInventory().setStackInSlot(1, new ItemStack(Items.DIRT, 4));
		h.succeedWhen(() -> {
			ItemStack out = f.getInventory().getStackInSlot(2);
			h.assertTrue(out.is(RotaryItems.YEAST.get()), "output "+out+" at "+f.getTemperature()+" C, "+f.omega+" rad/s");
			h.assertTrue(f.getWater() == 4000-50*out.getCount(), "water "+f.getWater()+" for "+out.getCount()+" yeast");
			h.assertTrue(h.getBlockState(new BlockPos(4, 1, 4)).getValue(reika.rotarycraft.content.block.FermenterBlock.FACING) == Direction.WEST, "front not west");
		});
	}

	/** Yeast already in the output slot is added to (stack grows by one per operation). */
	@GameTest(template = PLATFORM, timeoutTicks = 1200)
	public static void fermenterStacksYeast(GameTestHelper h) {
		FermenterBlockEntity f = fermenterRig(h);
		f.getInventory().setStackInSlot(0, new ItemStack(Items.SUGAR, 4));
		f.getInventory().setStackInSlot(1, new ItemStack(Items.DIRT, 4));
		f.getInventory().setStackInSlot(2, new ItemStack(RotaryItems.YEAST.get(), 3));
		h.succeedWhen(() -> {
			ItemStack out = f.getInventory().getStackInSlot(2);
			h.assertTrue(out.is(RotaryItems.YEAST.get()) && out.getCount() >= 4, "output "+out+", sugar "+f.getInventory().getStackInSlot(0));
		});
	}

	/** With a redstone signal: yeast + oak leaves (mulch value 2) -> 2 sludge per leaf. */
	@GameTest(template = PLATFORM, timeoutTicks = 1200)
	public static void fermenterMakesSludge(GameTestHelper h) {
		FermenterBlockEntity f = fermenterRig(h);
		h.setBlock(new BlockPos(4, 1, 5), Blocks.REDSTONE_BLOCK);
		f.getInventory().setStackInSlot(0, new ItemStack(RotaryItems.YEAST.get(), 4));
		f.getInventory().setStackInSlot(1, new ItemStack(Items.OAK_LEAVES, 4));
		h.succeedWhen(() -> {
			ItemStack out = f.getInventory().getStackInSlot(2);
			h.assertTrue(out.is(RotaryItems.part(CraftingPart.SLUDGE)) && out.getCount() >= 2 && out.getCount()%2 == 0, "output "+out);
		});
	}

	/** Without water nothing is made. */
	@GameTest(template = PLATFORM, timeoutTicks = 500)
	public static void fermenterNeedsWater(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 1, 4, 0, EngineBlockEntity.class).addFuel(50000);
		FermenterBlockEntity f = place(h, RotaryBlocks.FERMENTER.get(), 4, 1, 4, 0, FermenterBlockEntity.class);
		f.setTemperature(25);
		f.getInventory().setStackInSlot(0, new ItemStack(Items.SUGAR, 4));
		f.getInventory().setStackInSlot(1, new ItemStack(Items.DIRT, 4));
		h.runAtTickTime(450, () -> {
			h.assertTrue(f.getInventory().getStackInSlot(2).isEmpty(), "made "+f.getInventory().getStackInSlot(2)+" without water");
			h.succeed();
		});
	}

	/** Centrifuge at (4,2,4) powered from below: gas engine -> steel gearbox 8:1 speed-up (4096 rad/s @ 16 Nm) -> bevel east->up. */
	private static CentrifugeBlockEntity centrifugeRig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 6, 1, 4, 0, EngineBlockEntity.class).addFuel(50000);
		GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 5, 1, 4, 8, GearboxBlockEntity.class);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.STEEL, 8));
		gbx.setBlockMetadata(8);
		gbx.fillWithLubricant();
		gbx.reduction = false;
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 1, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		return place(h, RotaryBlocks.CENTRIFUGE.get(), 4, 2, 4, 0, CentrifugeBlockEntity.class);
	}

	/** Sludge: two per operation (max_stack 2), each giving clean sludge (80 % and 20 %) and compost (25 %). */
	@GameTest(template = PLATFORM, timeoutTicks = 800)
	public static void centrifugeCleansSludge(GameTestHelper h) {
		CentrifugeBlockEntity c = centrifugeRig(h);
		c.getInventory().setStackInSlot(0, new ItemStack(RotaryItems.part(CraftingPart.SLUDGE), 6));
		h.succeedWhen(() -> {
			h.assertTrue(c.omega == 4096, "centrifuge speed "+c.omega);
			h.assertTrue(c.getInventory().getStackInSlot(0).getCount() == 4, "input "+c.getInventory().getStackInSlot(0)+" (2 per operation)");
			for (int i = 1; i < 10; i++) {
				ItemStack is = c.getInventory().getStackInSlot(i);
				h.assertTrue(is.isEmpty() || is.is(RotaryItems.part(CraftingPart.CLEANSLUDGE)) || is.is(RotaryItems.part(CraftingPart.COMPOST)), "unexpected output "+is);
			}
		});
	}

	/** Canola husks give 90 mB lubricant each at medium difficulty; clean sludge smelts to ethanol. */
	@GameTest(template = PLATFORM, timeoutTicks = 800)
	public static void centrifugeMakesLubricant(GameTestHelper h) {
		CentrifugeBlockEntity c = centrifugeRig(h);
		c.getInventory().setStackInSlot(0, new ItemStack(RotaryItems.CANOLA_HUSKS.get(), 3));
		h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(RotaryItems.part(CraftingPart.CLEANSLUDGE))), h.getLevel())
				.map(r -> r.value().getResultItem(h.getLevel().registryAccess()).is(RotaryItems.ETHANOL.get())).orElse(false), "clean sludge does not smelt to ethanol");
		h.succeedWhen(() -> {
			h.assertTrue(c.getFluid().is(RotaryFluids.LUBRICANT.get()) && c.getFluidLevel() == 90, "tank "+c.getFluidLevel()+" mB "+c.getFluid().getFluid());
			h.assertTrue(c.getInventory().getStackInSlot(0).getCount() == 2, "input "+c.getInventory().getStackInSlot(0));
		});
	}

	/** Fractionation Unit at (4,2,4) powered from below: gas engine -> 16:1 speed-up (8192 rad/s @ 8 Nm = 65536 W) -> bevel east->up. */
	private static reika.rotarycraft.content.blockentity.FractionatorBlockEntity fractionatorRig(GameTestHelper h, boolean tear) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 6, 1, 4, 0, EngineBlockEntity.class).addFuel(50000);
		GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 5, 1, 4, 12, GearboxBlockEntity.class);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.STEEL, 16));
		gbx.setBlockMetadata(12);
		gbx.fillWithLubricant();
		gbx.reduction = false;
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 1, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		var f = place(h, RotaryBlocks.FRACTIONATOR.get(), 4, 2, 4, 0, reika.rotarycraft.content.blockentity.FractionatorBlockEntity.class);
		var inv = f.getInventory();
		inv.setStackInSlot(0, new ItemStack(Items.BLAZE_POWDER, 8));
		inv.setStackInSlot(1, new ItemStack(RotaryItems.part(CraftingPart.COALDUST), 8));
		inv.setStackInSlot(2, new ItemStack(Items.MAGMA_CREAM, 8));
		inv.setStackInSlot(3, new ItemStack(Items.PINK_DYE, 8));
		inv.setStackInSlot(4, new ItemStack(RotaryItems.part(CraftingPart.NETHERRACKDUST), 8));
		inv.setStackInSlot(5, new ItemStack(RotaryItems.part(CraftingPart.TAR), 8));
		if (tear)
			inv.setStackInSlot(6, new ItemStack(Items.GHAST_TEAR));
		f.addEthanol(4000);
		return f;
	}

	/**
	 * One operation (800 - 40*log2(8193) = 280 ticks): 250 mB ethanol and 1-2 ingredients used (medium CONSUMEFRAC 0.25 * 6), jet
	 * fuel = yield(pressure) * 1000-2200 mB; the ghast tear stays.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 700)
	public static void fractionatorMakesJetFuel(GameTestHelper h) {
		var f = fractionatorRig(h, true);
		h.succeedWhen(() -> {
			h.assertTrue(f.omega == 8192 && f.power >= 65536, "input "+f.omega+" rad/s, "+f.power+" W");
			h.assertTrue(f.getFuelLevel() > 0, "no jet fuel (pressure "+f.getPressure()+" kPa)");
			h.assertTrue(f.getInputLevel() == 4000-250, "ethanol "+f.getInputLevel());
			int used = 0;
			for (int i = 0; i < 6; i++)
				used += 8-f.getInventory().getStackInSlot(i).getCount();
			h.assertTrue(used >= 1 && used <= 2, used+" ingredients used");
			h.assertTrue(f.getInventory().getStackInSlot(6).is(Items.GHAST_TEAR), "ghast tear used up");
		});
	}

	/** Without the ghast tear nothing happens. */
	@GameTest(template = PLATFORM, timeoutTicks = 500)
	public static void fractionatorNeedsGhastTear(GameTestHelper h) {
		var f = fractionatorRig(h, false);
		h.runAtTickTime(450, () -> {
			h.assertTrue(f.getFuelLevel() == 0 && f.getInputLevel() == 4000, "worked without a ghast tear");
			h.succeed();
		});
	}

	/** The original yield curve and the pressure build-up from torque: 8 Nm settles low (yield about 0.1). */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void fractionatorYieldCurve(GameTestHelper h) {
		var f = place(h, RotaryBlocks.FRACTIONATOR.get(), 4, 1, 4, 0, reika.rotarycraft.content.blockentity.FractionatorBlockEntity.class);
		int[][] pts = {{180, 100}, {720, 1000}, {1000, 2500}, {0, 10}};
		for (int[] p : pts) {
			f.setPressure(p[0]);
			h.assertTrue(Math.round(f.getYieldRatio()*1000) == p[1], "yield at "+p[0]+" kPa: "+f.getYieldRatio());
		}
		f.setPressure(600);
		h.assertTrue(Math.abs(f.getYieldRatio()-(0.4+100/220D*0.6)) < 1e-4, "yield at 600 kPa: "+f.getYieldRatio());
		h.succeed();
	}

	/** Microturbine (131072 rad/s, jet fuel) -> bevel gears east->up -> Pulse Jet Furnace (4,2,4) with jet fuel. */
	private static reika.rotarycraft.content.blockentity.PulseJetFurnaceBlockEntity pulseRig(GameTestHelper h) {
		place(h, RotaryBlocks.MICRO_ENGINE.get(), 5, 1, 4, 0, EngineBlockEntity.class).addFuel(100000);
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 1, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		var f = place(h, RotaryBlocks.PULSE_JET.get(), 4, 2, 4, 0, reika.rotarycraft.content.blockentity.PulseJetFurnaceBlockEntity.class);
		f.addFuel(8000);
		return f;
	}

	/**
	 * Obsidian -> Blast Glass at 850 C. The turbine needs ~1930 ticks to reach 131072 rad/s (the furnace's minimum speed); the furnace
	 * then heats by max((1000-T)/8, 4) per second; water keeps it below the 1000 C failure point.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 3600)
	public static void pulseJetMakesBlastGlass(GameTestHelper h) {
		var f = pulseRig(h);
		f.addWater(3000);
		f.getInventory().setStackInSlot(0, new ItemStack(Items.OBSIDIAN, 2));
		h.succeedWhen(() -> {
			h.assertTrue(f.getInventory().getStackInSlot(2).is(RotaryBlocks.BLAST_GLASS.get().asItem()),
					"output "+f.getInventory().getStackInSlot(2)+" at "+f.getTemperature()+" C, "+f.omega+" rad/s, fuel "+f.getFuel());
			h.assertTrue(f.getTemperature() >= 850 && f.getTemperature() <= 1000, "temperature "+f.getTemperature());
		});
	}

	/** Hot enough for steel (900 C) but powered by a gas engine (512 rad/s, far below 131072): nothing is smelted. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void pulseJetNeedsSpeed(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 1, 4, 0, EngineBlockEntity.class).addFuel(50000);
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 1, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		var f = place(h, RotaryBlocks.PULSE_JET.get(), 4, 2, 4, 0, reika.rotarycraft.content.blockentity.PulseJetFurnaceBlockEntity.class);
		f.addFuel(8000);
		f.setTemperature(900);
		f.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
		h.runAtTickTime(250, () -> {
			h.assertTrue(f.getInventory().getStackInSlot(2).isEmpty() && f.omega == 512, "smelted at "+f.omega+" rad/s");
			h.succeed();
		});
	}
}
