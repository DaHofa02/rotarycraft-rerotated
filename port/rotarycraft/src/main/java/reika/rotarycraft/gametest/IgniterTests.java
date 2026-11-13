/*******************************************************************************
 * Tests for the ported RotaryCraft igniter (original TileEntityIgniter). Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki;
 * not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.IgniterBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class IgniterTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Microturbine (meta 0: output west, jet fuel) east of an igniter at (4, 2, 4). */
	private static IgniterBlockEntity rig(GameTestHelper h) {
		EngineBlockEntity e = place(h, RotaryBlocks.MICRO_ENGINE.get(), 5, 2, 4, 0, EngineBlockEntity.class);
		IFluidHandler fh = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, e.getBlockPos(), Direction.DOWN);
		fh.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 20000), IFluidHandler.FluidAction.EXECUTE);
		return place(h, RotaryBlocks.IGNITER.get(), 4, 2, 4, 0, IgniterBlockEntity.class);
	}

	private static int count(ItemStackHandler inv, net.minecraft.world.item.Item item) {
		int n = 0;
		for (int i = 0; i < inv.getSlots(); i++)
			if (inv.getStackInSlot(i).is(item))
				n += inv.getStackInSlot(i).getCount();
		return n;
	}

	/** Range 16 + sqrt(2T) rounded up to 8; the fuels and their temperatures; valid items. */
	@GameTest(template = PLATFORM)
	public static void igniterFollowsTheOriginalNumbers(GameTestHelper h) {
		IgniterBlockEntity te = place(h, RotaryBlocks.IGNITER.get(), 4, 2, 4, 0, IgniterBlockEntity.class);
		int[][] cases = {{0, 16}, {280, 40}, {600, 56}, {2500, 88}};
		for (int[] c : cases) {
			te.setTemperature(c[0]);
			h.assertTrue(te.getRange() == c[1], c[0]+" C: range "+te.getRange());
		}
		h.assertTrue(IgniterBlockEntity.IgnitionFuel.COAL.temperature == 600 && IgniterBlockEntity.IgnitionFuel.LAVA.temperature == 1200, "fuel temperatures");
		h.assertTrue(IgniterBlockEntity.isValidFuel(new ItemStack(Items.OAK_PLANKS)) && IgniterBlockEntity.isValidFuel(new ItemStack(Items.BLAZE_POWDER))
				&& !IgniterBlockEntity.isValidFuel(new ItemStack(Items.STICK)), "valid fuels");
		h.assertTrue(te.getRedstoneOverride() == 15, "redstone without fuel");
		h.succeed();
	}

	/** With coal it burns a piece every 40 ticks while below 600 deg C, gets past 280 and sets a cow near it on fire. */
	@GameTest(template = PLATFORM, timeoutTicks = 800)
	public static void igniterHeatsUpAndSetsFires(GameTestHelper h) {
		IgniterBlockEntity te = rig(h);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.COAL, 16));
		Cow cow = h.spawn(EntityType.COW, new BlockPos(2, 2, 6));
		cow.setNoAi(true);
		h.succeedWhen(() -> {
			h.assertTrue(te.getFuel() == IgniterBlockEntity.IgnitionFuel.COAL, "fuel "+te.getFuel()+", "+te.omega+" rad/s");
			h.assertTrue(te.getTemperature() >= 280 && count(te.getInventory(), Items.COAL) < 16, "igniter at "+te.getTemperature()+" C with "+count(te.getInventory(), Items.COAL)+" coal");
			h.assertTrue(cow.isOnFire() || !cow.isAlive(), "cow not burning");
		});
	}

	/** The hottest fuel it has all parts of: blaze powder before coal and planks; a lava bucket gives its bucket back. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void igniterPicksTheHottestFuel(GameTestHelper h) {
		IgniterBlockEntity te = rig(h);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 4));
		te.getInventory().setStackInSlot(1, new ItemStack(Items.COAL, 4));
		te.getInventory().setStackInSlot(2, new ItemStack(Items.BLAZE_POWDER, 4));
		te.getInventory().setStackInSlot(3, new ItemStack(Items.IRON_INGOT, 4)); //thermite also needs aluminium dust
		IgniterBlockEntity lava = place(h, RotaryBlocks.IGNITER.get(), 4, 2, 6, 0, IgniterBlockEntity.class);
		EngineBlockEntity e = place(h, RotaryBlocks.MICRO_ENGINE.get(), 5, 2, 6, 0, EngineBlockEntity.class);
		h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, e.getBlockPos(), Direction.DOWN).fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 20000), IFluidHandler.FluidAction.EXECUTE);
		lava.getInventory().setStackInSlot(0, new ItemStack(Items.LAVA_BUCKET));
		h.succeedWhen(() -> {
			h.assertTrue(te.getFuel() == IgniterBlockEntity.IgnitionFuel.BLAZE && count(te.getInventory(), Items.BLAZE_POWDER) < 4
					&& count(te.getInventory(), Items.COAL) == 4 && count(te.getInventory(), Items.OAK_PLANKS) == 4, "fuel "+te.getFuel());
			h.assertTrue(count(lava.getInventory(), Items.LAVA_BUCKET) == 0 && count(lava.getInventory(), Items.BUCKET) == 1, "lava bucket not burnt or bucket lost");
		});
	}
}
