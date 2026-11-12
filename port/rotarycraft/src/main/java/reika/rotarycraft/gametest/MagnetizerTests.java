/*******************************************************************************
 * Tests for the ported RotaryCraft magnetizer (original TileEntityMagnetizer). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.MagnetizerBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.GearboxTypes.GearPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class MagnetizerTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	private static ItemStack core(GearboxTypes t) {
		return new ItemStack(RotaryItems.gearPart(t, GearPart.SHAFTCORE));
	}

	/** Microturbine (131072 rad/s @ 16 Nm) -> bedrock gearbox 16:1 (8192 rad/s @ 256 Nm) -> magnetizer at (5, 1, z) (meta 0: input east). */
	private static MagnetizerBlockEntity rig(GameTestHelper h, int z) {
		EngineBlockEntity e = place(h, RotaryBlocks.MICRO_ENGINE.get(), 7, 1, z, 0, EngineBlockEntity.class);
		IFluidHandler fh = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, e.getBlockPos(), Direction.DOWN);
		fh.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), 20000), IFluidHandler.FluidAction.EXECUTE);
		GearboxBlockEntity g = place(h, RotaryBlocks.GEARBOX.get(), 6, 1, z, 12, GearboxBlockEntity.class);
		g.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.BEDROCK, 16));
		g.setBlockMetadata(12);
		g.fillWithLubricant();
		g.reduction = true;
		return place(h, RotaryBlocks.MAGNETIZER.get(), 5, 1, z, 0, MagnetizerBlockEntity.class);
	}

	/** DurationRegistry.MAGNETIZER: max(2, 400 - 20*log2(omega+1)); only single shaft cores go in; redstone 15 without a core. */
	@GameTest(template = PLATFORM)
	public static void magnetizerFollowsTheOriginalNumbers(GameTestHelper h) {
		MagnetizerBlockEntity te = place(h, RotaryBlocks.MAGNETIZER.get(), 5, 1, 4, 0, MagnetizerBlockEntity.class);
		int[][] cases = {{2048, 179}, {8192, 139}, {1048576, 2}};
		for (int[] c : cases) {
			te.omega = c[0];
			h.assertTrue(te.getOperationTime() == c[1], c[0]+" rad/s: "+te.getOperationTime()+" ticks");
		}
		h.assertTrue(te.getRedstoneOverride() == 15, "redstone without a core");
		h.assertTrue(te.getInventory().insertItem(0, new ItemStack(Items.IRON_INGOT), false).getCount() == 1, "iron went in");
		ItemStack two = core(GearboxTypes.STEEL);
		two.setCount(2);
		h.assertTrue(te.getInventory().insertItem(0, two, false).getCount() == 1, "two cores went in");
		h.assertTrue(te.hasCore() && te.getRedstoneOverride() == 0, "core not in");
		h.assertTrue(te.canUpgradeWith(new ItemStack(RotaryItems.UPGRADE_REDSTONE.get())), "no redstone upgrade");
		h.succeed();
	}

	/**
	 * From 2048 rad/s (the turbine spinning up to 8192) a tungsten core gains 1 uT every 140-180 ticks while the redstone signal alternates (a redstone block set and taken away
	 * every 2 ticks); without an alternating signal nothing happens. With the integrated redstone upgrade it runs while unpowered.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1400)
	public static void magnetizerChargesCoresWithAlternatingRedstone(GameTestHelper h) {
		MagnetizerBlockEntity clock = rig(h, 1);
		MagnetizerBlockEntity still = rig(h, 4);
		MagnetizerBlockEntity upgraded = rig(h, 7);
		clock.getInventory().setStackInSlot(0, core(GearboxTypes.TUNGSTEN));
		still.getInventory().setStackInSlot(0, core(GearboxTypes.TUNGSTEN));
		upgraded.getInventory().setStackInSlot(0, core(GearboxTypes.TUNGSTEN));
		upgraded.upgrade(new ItemStack(RotaryItems.UPGRADE_REDSTONE.get()));
		BlockPos red = new BlockPos(5, 1, 0);
		h.onEachTick(() -> h.setBlock(red, h.getTick()/2%2 == 0 ? Blocks.REDSTONE_BLOCK : Blocks.AIR));
		h.succeedWhen(() -> {
			h.assertTrue(clock.omega >= clock.MINSPEED && clock.power >= clock.MINPOWER, "rig at "+clock.omega+" rad/s"); //the microturbine spins up for about 1900 ticks
			h.assertTrue(clock.getCoreMagnetization() >= 2, "clocked core at "+clock.getCoreMagnetization()+" uT");
			h.assertTrue(upgraded.getCoreMagnetization() >= 2, "upgraded core at "+upgraded.getCoreMagnetization()+" uT");
			h.assertTrue(still.getCoreMagnetization() == 0 && !still.isAlternating(), "core without a signal at "+still.getCoreMagnetization()+" uT");
			ItemStack c = clock.getInventory().getStackInSlot(0);
			h.assertTrue(c.get(RotaryComponents.MAGNETIZATION.get()) == clock.getCoreMagnetization(), "item data");
		});
	}
}
