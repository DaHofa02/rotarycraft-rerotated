/*******************************************************************************
 * Tests for the ported RotaryCraft obsidian maker (original TileEntityObsidianMaker). Private NeoForge 1.21.1 port of RotaryCraft by
 * Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.ObsidianMakerBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ObsidianMakerTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Microturbine -> bedrock gearbox 16:1 (8192 rad/s @ 256 Nm) -> bevel gears east->up -> obsidian maker above them at (5, 3, z). */
	private static ObsidianMakerBlockEntity rig(GameTestHelper h, int z) {
		EngineBlockEntity e = place(h, RotaryBlocks.MICRO_ENGINE.get(), 7, 2, z, 0, EngineBlockEntity.class);
		h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, e.getBlockPos(), Direction.DOWN).fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 20000), IFluidHandler.FluidAction.EXECUTE);
		GearboxBlockEntity g = place(h, RotaryBlocks.GEARBOX.get(), 6, 2, z, 12, GearboxBlockEntity.class);
		g.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16));
		g.setBlockMetadata(12);
		g.fillWithLubricant();
		g.reduction = true;
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 5, 2, z, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		return place(h, RotaryBlocks.OBSIDIAN_MAKER.get(), 5, 3, z, 0, ObsidianMakerBlockEntity.class);
	}

	private static int count(ItemStackHandler inv, net.minecraft.world.item.Item item) {
		int n = 0;
		for (int i = 0; i < inv.getSlots(); i++)
			if (inv.getStackInSlot(i).is(item))
				n += inv.getStackInSlot(i).getCount();
		return n;
	}

	/** Obsidian from 550 to 750 deg C, cobblestone below 500 and above 900; DurationRegistry.OBSIDIAN: max(1, 800 - 60*log2(omega+1)). */
	@GameTest(template = PLATFORM)
	public static void makerFollowsTheOriginalNumbers(GameTestHelper h) {
		ObsidianMakerBlockEntity te = place(h, RotaryBlocks.OBSIDIAN_MAKER.get(), 5, 3, 4, 0, ObsidianMakerBlockEntity.class);
		int[][] temps = {{550, 1}, {650, 1}, {750, 1}, {499, 0}, {901, 0}, {25, 0}};
		for (int[] t : temps) {
			te.setTemperature(t[0]);
			for (int i = 0; i < 20; i++)
				h.assertTrue(te.makesObsidian() == (t[1] == 1), t[0]+" deg C");
		}
		int[][] cases = {{2048, 139}, {8192, 19}};
		for (int[] c : cases) {
			te.omega = c[0];
			h.assertTrue(te.getOperationTime() == c[1], c[0]+" rad/s: "+te.getOperationTime());
		}
		h.succeed();
	}

	/** At 600 deg C it makes obsidian from 1000 mB lava and 2500 mB water each; cold it makes cobblestone and keeps its lava. */
	@GameTest(template = PLATFORM, timeoutTicks = 1200)
	public static void makerMakesObsidianWhenHotAndCobblestoneWhenCold(GameTestHelper h) {
		ObsidianMakerBlockEntity hot = rig(h, 2);
		ObsidianMakerBlockEntity cold = rig(h, 6);
		for (ObsidianMakerBlockEntity te : new ObsidianMakerBlockEntity[] {hot, cold}) {
			te.addWater(20000);
			te.addLava(5000);
		}
		hot.setTemperature(600);
		h.succeedWhen(() -> {
			h.assertTrue(hot.omega >= hot.MINSPEED, "rig at "+hot.omega+" rad/s");
			int obs = count(hot.getInventory(), Items.OBSIDIAN), cob = count(cold.getInventory(), Items.COBBLESTONE);
			h.assertTrue(obs >= 2 && hot.getLava() == 5000-1000*obs && hot.getWater() == 20000-2500*obs, obs+" obsidian, "+hot.getLava()+" mB lava, "+hot.getWater()+" mB water, "+hot.getTemperature()+" C");
			h.assertTrue(cob >= 2 && cold.getLava() == 5000 && cold.getWater() == 20000-1000*cob && count(cold.getInventory(), Items.OBSIDIAN) == 0, cob+" cobblestone, "+cold.getLava()+" mB lava at "+cold.getTemperature()+" C");
		});
	}

	/** With lava but no water it heats to 1000 deg C and melts into lava; liquids only from the sides; nothing goes in, products come out. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void makerMeltsWithoutWater(GameTestHelper h) {
		ObsidianMakerBlockEntity te = place(h, RotaryBlocks.OBSIDIAN_MAKER.get(), 5, 3, 4, 0, ObsidianMakerBlockEntity.class);
		BlockPos pos = new BlockPos(5, 3, 4);
		IFluidHandler up = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), Direction.UP);
		IFluidHandler side = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), Direction.NORTH);
		h.assertTrue(up.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "lava from above");
		h.assertTrue(side.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000, "lava from the side refused");
		IItemHandler items = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), Direction.UP);
		h.assertTrue(items.insertItem(0, new ItemStack(Items.COBBLESTONE), false).getCount() == 1, "cobblestone went in");
		te.getInventory().setStackInSlot(4, new ItemStack(Items.OBSIDIAN, 2));
		h.assertTrue(items.extractItem(4, 2, true).getCount() == 2, "obsidian does not come out");
		te.getInventory().setStackInSlot(4, ItemStack.EMPTY);
		te.setTemperature(998);
		h.succeedWhen(() -> h.assertBlockPresent(Blocks.LAVA, pos));
	}
}
