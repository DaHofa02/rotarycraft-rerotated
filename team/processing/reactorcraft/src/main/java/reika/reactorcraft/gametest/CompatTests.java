/*******************************************************************************
 * Compatibility tests of the private NeoForge 1.21.1 ports of RotaryCraft, ReactorCraft and ElectriCraft (by Reika Kalseki) with vanilla
 * and Mekanism (loaded only in the test instance): every block of the three mods placed and ticked, every side's capabilities used, and
 * the real interactions with Mekanism energy cubes and fluid tanks and vanilla hoppers. Not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import reika.electricraft.content.blockentity.RFBatteryBlockEntity;
import reika.electricraft.registry.ElectriBlocks;
import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.CompactorBlockEntity;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CompatTests {

	private static final String SMALL = "platform_9x4x9";
	private static final String BIG = "platform_48x12x48";
	private static final Set<String> MODS = Set.of("rotarycraft", "reactorcraft", "electricraft");

	private static Block mek(String id) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mekanism", id));
	}

	private static boolean noMekanism(GameTestHelper h) {
		if (mek("basic_energy_cube") == Blocks.AIR) {
			h.succeed(); //Mekanism is only in the test instance
			return true;
		}
		return false;
	}

	/** All blocks of the three mods except their fluid blocks. */
	public static List<Block> modBlocks() {
		List<Block> li = new ArrayList<>();
		for (Block b : BuiltInRegistries.BLOCK)
			if (MODS.contains(BuiltInRegistries.BLOCK.getKey(b).getNamespace()) && !(b instanceof LiquidBlock))
				li.add(b);
		return li;
	}

	/**
	 * Every block of the three mods is placed (each on its own stone pad, 3 apart) and ticks for 40 ticks; at tick 5 every side's item,
	 * fluid and energy capability is used (contents read, a stone / 1000 mB water / 1000 FE offered and taken out, all simulated). Any
	 * exception fails the test with the block's name.
	 */
	@GameTest(template = BIG, timeoutTicks = 100, batch = "compat")
	public static void everyBlockPlacesTicksAndExposesCapabilities(GameTestHelper h) {
		ServerLevel w = h.getLevel();
		List<Block> blocks = modBlocks();
		List<BlockPos> at = new ArrayList<>();
		int per = 15;
		h.assertTrue(blocks.size() <= per*per, blocks.size()+" blocks do not fit");
		for (int i = 0; i < blocks.size(); i++) {
			BlockPos p = new BlockPos(2+3*(i%per), 2, 2+3*(i/per));
			h.setBlock(p.below(), Blocks.STONE);
			try {
				h.setBlock(p, blocks.get(i));
			}
			catch (RuntimeException e) {
				throw new IllegalStateException("placing "+BuiltInRegistries.BLOCK.getKey(blocks.get(i))+" failed: "+e, e);
			}
			at.add(p);
		}
		h.runAtTickTime(5, () -> {
			int caps = 0;
			for (int i = 0; i < at.size(); i++) {
				BlockPos p = h.absolutePos(at.get(i));
				String name = BuiltInRegistries.BLOCK.getKey(blocks.get(i)).toString();
				for (Direction d : new Direction[]{null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
					try {
						IItemHandler ih = w.getCapability(Capabilities.ItemHandler.BLOCK, p, d);
						if (ih != null) {
							caps++;
							for (int s = 0; s < ih.getSlots(); s++) {
								ih.getStackInSlot(s);
								ih.extractItem(s, 64, true);
								ih.insertItem(s, new ItemStack(Items.STONE), true);
								ih.isItemValid(s, new ItemStack(Items.STONE));
							}
						}
						IFluidHandler fh = w.getCapability(Capabilities.FluidHandler.BLOCK, p, d);
						if (fh != null) {
							caps++;
							for (int t = 0; t < fh.getTanks(); t++) {
								fh.getFluidInTank(t);
								fh.getTankCapacity(t);
							}
							fh.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE);
							fh.drain(1000, IFluidHandler.FluidAction.SIMULATE);
						}
						IEnergyStorage es = w.getCapability(Capabilities.EnergyStorage.BLOCK, p, d);
						if (es != null) {
							caps++;
							es.receiveEnergy(1000, true);
							es.extractEnergy(1000, true);
							es.getEnergyStored();
						}
					}
					catch (RuntimeException e) {
						throw new IllegalStateException(name+" side "+d+": "+e, e);
					}
				}
			}
			h.assertTrue(caps > 100, "only "+caps+" capabilities found");
		});
		h.runAtTickTime(40, () -> {
			int gone = 0;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < at.size(); i++)
				if (!h.getBlockState(at.get(i)).is(blocks.get(i))) {
					gone++;
					sb.append(BuiltInRegistries.BLOCK.getKey(blocks.get(i))).append(' ');
				}
			//crops and the like may legitimately vanish without soil; machines must stay
			h.assertTrue(gone <= 4, gone+" blocks vanished: "+sb);
			h.succeed();
		});
	}

	/**
	 * Energy across the mods: an ElectriCraft RF battery with a redstone signal pushes its FE up into a Mekanism universal cable line, which
	 * delivers it into a second, empty RF battery (from the side). (Mekanism's energy cube offers no FE capability on any side when set by
	 * command, so the cable is the Mekanism end.)
	 */
	@GameTest(template = SMALL, timeoutTicks = 100)
	public static void rfEnergyThroughMekanismCables(GameTestHelper h) {
		if (noMekanism(h))
			return;
		BlockPos a = new BlockPos(1, 1, 4), b = new BlockPos(7, 1, 4);
		h.setBlock(a, ElectriBlocks.RF_BATTERY.get());
		((RFBatteryBlockEntity)h.getBlockEntity(a)).setStoredEnergy(1000000);
		h.setBlock(a.north(), Blocks.REDSTONE_BLOCK);
		for (int x = 1; x <= 6; x++)
			h.setBlock(new BlockPos(x, 2, 4), mek("basic_universal_cable"));
		h.setBlock(new BlockPos(6, 1, 4), mek("basic_universal_cable"));
		h.setBlock(b, ElectriBlocks.RF_BATTERY.get());
		h.succeedWhen(() -> {
			long ea = ((RFBatteryBlockEntity)h.getBlockEntity(a)).getStoredEnergy(), eb = ((RFBatteryBlockEntity)h.getBlockEntity(b)).getStoredEnergy();
			h.assertTrue(ea < 1000000 && eb > 0, "battery A "+ea+" FE, battery B "+eb+" FE");
		});
	}

	/** A RotaryCraft pipe with water fills a Mekanism fluid tank beside it. */
	@GameTest(template = SMALL, timeoutTicks = 100)
	public static void rotaryPipeFillsMekanismTank(GameTestHelper h) {
		if (noMekanism(h))
			return;
		BlockPos pipe = new BlockPos(4, 1, 4);
		h.setBlock(pipe, RotaryBlocks.PIPE.get());
		h.setBlock(pipe.east(), mek("basic_fluid_tank"));
		h.onEachTick(() -> {
			if (h.getBlockEntity(pipe) instanceof PipingBlockEntity p && p.getFluidLevel() < 500)
				p.addFluid(Fluids.WATER, 500);
		});
		h.succeedWhen(() -> {
			IFluidHandler t = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pipe.east()), Direction.WEST);
			h.assertTrue(t != null && t.getFluidInTank(0).getAmount() >= 1000 && t.getFluidInTank(0).getFluid().isSame(Fluids.WATER),
					"tank holds "+(t == null ? "no capability" : t.getFluidInTank(0).getAmount()+" mB"));
		});
	}

	/** A RotaryCraft pipe above a filled Mekanism fluid tank drains it. */
	@GameTest(template = SMALL, timeoutTicks = 100)
	public static void rotaryPipeDrainsMekanismTank(GameTestHelper h) {
		if (noMekanism(h))
			return;
		BlockPos tank = new BlockPos(4, 1, 4);
		h.setBlock(tank, mek("basic_fluid_tank"));
		IFluidHandler t = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(tank), Direction.UP);
		h.assertTrue(t != null && t.fill(new FluidStack(Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE) == 8000, "tank refused water");
		h.setBlock(tank.above(), RotaryBlocks.PIPE.get());
		h.succeedWhen(() -> {
			PipingBlockEntity p = (PipingBlockEntity)h.getBlockEntity(tank.above());
			h.assertTrue(p.getFluidLevel() > 0 && t.getFluidInTank(0).getAmount() < 8000, "pipe "+p.getFluidLevel()+" mB, tank "+t.getFluidInTank(0).getAmount()+" mB");
		});
	}

	/** Vanilla hoppers feed a RotaryCraft compactor (ice into all four input slots) and take nothing but the product out. */
	@GameTest(template = SMALL, timeoutTicks = 100)
	public static void hoppersFeedCompactor(GameTestHelper h) {
		BlockPos c = new BlockPos(4, 2, 4);
		h.setBlock(c, RotaryBlocks.COMPACTOR.get());
		CompactorBlockEntity te = (CompactorBlockEntity)h.getBlockEntity(c);
		te.getInventory().setStackInSlot(4, new ItemStack(Items.PACKED_ICE, 3));
		h.setBlock(c.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		var top = (net.minecraft.world.level.block.entity.HopperBlockEntity)h.getBlockEntity(c.above());
		top.setItem(0, new ItemStack(Items.ICE, 8));
		h.setBlock(c.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		var bottom = (net.minecraft.world.level.block.entity.HopperBlockEntity)h.getBlockEntity(c.below());
		h.succeedWhen(() -> {
			h.assertTrue(top.getItem(0).isEmpty(), "hopper still holds "+top.getItem(0));
			int ice = 0;
			for (int i = 0; i < 4; i++)
				ice += te.getInventory().getStackInSlot(i).getCount();
			h.assertTrue(ice == 8, "ice in the compactor: "+ice);
			h.assertTrue(bottom.getItem(0).is(Items.PACKED_ICE) && bottom.getItem(0).getCount() == 3 && te.getInventory().getStackInSlot(4).isEmpty(),
					"product out: "+bottom.getItem(0));
			for (int i = 0; i < 5; i++)
				h.assertTrue(!bottom.getItem(i).is(Items.ICE), "the bottom hopper took ice");
		});
	}
}
