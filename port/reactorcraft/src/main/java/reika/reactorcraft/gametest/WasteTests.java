/*******************************************************************************
 * Tests for the ported ReactorCraft waste handling (original TileEntityWasteContainer, TileEntityWasteStorage, TileEntityWasteDecayer).
 * Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.item.NuclearWasteItem;
import reika.reactorcraft.radiation.Isotopes;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorEffects;
import reika.reactorcraft.waste.WasteContainerBlockEntity;
import reika.reactorcraft.waste.WasteDecayerBlockEntity;
import reika.reactorcraft.waste.WasteStorageBlockEntity;
import reika.reactorcraft.waste.WasteUnitBlockEntity;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WasteTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static ItemStack waste(Isotopes i, int n) {
		return NuclearWasteItem.of(i).copyWithCount(n);
	}

	/** The container takes only short-lived waste (half-life up to 6 Minecraft years), the storage drum only long-lived waste; the drum gives nothing back. */
	@GameTest(template = PLATFORM)
	public static void wasteSorting(GameTestHelper h) {
		h.setBlock(new BlockPos(2, 2, 4), ReactorBlocks.WASTE_CONTAINER.get());
		h.setBlock(new BlockPos(6, 2, 4), ReactorBlocks.WASTE_STORAGE.get());
		IItemHandler c = ((WasteContainerBlockEntity)h.getBlockEntity(new BlockPos(2, 2, 4))).getItemHandler(Direction.UP);
		IItemHandler s = ((WasteStorageBlockEntity)h.getBlockEntity(new BlockPos(6, 2, 4))).getItemHandler(Direction.UP);
		h.assertTrue(WasteUnitBlockEntity.isLongLivedWaste(Isotopes.Cs137) && !WasteUnitBlockEntity.isLongLivedWaste(Isotopes.Cs134), "half-life split");
		h.assertTrue(c.insertItem(0, waste(Isotopes.Cs134, 1), false).isEmpty(), "container refused Cs-134");
		h.assertTrue(!c.insertItem(1, waste(Isotopes.Cs137, 1), false).isEmpty(), "container took Cs-137");
		h.assertTrue(s.insertItem(0, waste(Isotopes.Cs137, 16), false).isEmpty(), "storage refused Cs-137");
		h.assertTrue(!s.insertItem(1, waste(Isotopes.Cs134, 1), false).isEmpty(), "storage took Cs-134");
		h.assertTrue(s.extractItem(0, 1, false).isEmpty(), "storage gave waste back");
		h.succeed();
	}

	/** Decay heat: 10 items warm the container by about 3.2 C each per second. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void containerHeatsUp(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 2, 4), ReactorBlocks.WASTE_CONTAINER.get());
		WasteContainerBlockEntity te = (WasteContainerBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		for (int i = 0; i < 10; i++)
			te.getInventory().setStackInSlot(i, waste(Isotopes.Cs134, 1));
		int[] start = {0};
		h.runAtTickTime(21, () -> start[0] = te.getTemperature());
		h.runAtTickTime(81, () -> {
			int dT = te.getTemperature()-start[0];
			//32 C per second for 10 items; an item may decay away during the test (about 3 %), leaving 9 (28 C)
			h.assertTrue(dT >= 3*28 && dT <= 3*33, "heated by "+dT+" C in 3 s");
			h.succeed();
		});
	}

	/** A hot container (over 100 C) boils away a water source next to it and loses 278 C. */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void containerBoilsWater(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 2, 4), ReactorBlocks.WASTE_CONTAINER.get());
		h.setBlock(new BlockPos(5, 2, 4), Blocks.WATER);
		WasteContainerBlockEntity te = (WasteContainerBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		te.setTemperature(500);
		h.runAtTickTime(25, () -> {
			h.assertTrue(!h.getBlockState(new BlockPos(5, 2, 4)).getFluidState().isSource(), "water source still there"); //water that already flowed out may run back
			h.assertTrue(te.getTemperature() <= 500-278, "container at "+te.getTemperature()+" C");
			h.succeed();
		});
	}

	/** A storage drum with 16 waste items (range 4) gives a pig in sight moderate radiation sickness. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void storageIrradiatesInSight(GameTestHelper h) {
		h.setBlock(new BlockPos(2, 2, 4), ReactorBlocks.WASTE_STORAGE.get());
		WasteStorageBlockEntity te = (WasteStorageBlockEntity)h.getBlockEntity(new BlockPos(2, 2, 4));
		te.getInventory().setStackInSlot(11, waste(Isotopes.Tc99, 16));
		Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 2, 4));
		h.succeedWhen(() -> h.assertTrue(pig.hasEffect(ReactorEffects.RADIATION), "pig not irradiated (range "+te.getRange()+", waste "+te.countWaste()
				+", pig at "+pig.position()+" alive "+pig.isAlive()+", sees "+WasteUnitBlockEntity.canBlockSee(h.getLevel(), te.getBlockPos(), pig.position(), 3)+")"));
	}

	/** Forced fission: a long-lived Cs-137 becomes its lighter decay product Cs-134 (original computeDecayData). */
	@GameTest(template = PLATFORM)
	public static void decayerConvertsWaste(GameTestHelper h) {
		h.assertTrue(Isotopes.Cs137.getDecay().product().isotope() == Isotopes.Cs134, "Cs-137 decays into "+Isotopes.Cs137.getDecay().product().symbol());
		h.setBlock(new BlockPos(4, 2, 4), ReactorBlocks.WASTE_DECAYER.get());
		WasteDecayerBlockEntity te = (WasteDecayerBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		te.getInventory().setStackInSlot(14, waste(Isotopes.Cs137, 1));
		h.assertTrue(te.tryDecay(), "nothing decayed");
		boolean found = false;
		for (int i = 0; i < 15; i++)
			found |= WasteUnitBlockEntity.getIsotope(te.getInventory().getStackInSlot(i)) == Isotopes.Cs134;
		h.assertTrue(found && WasteUnitBlockEntity.getIsotope(te.getInventory().getStackInSlot(14)) != Isotopes.Cs137, "no Cs-134 product");
		h.succeed();
	}

	/** Saved inventories (as written by setblock or a world save) load with their isotope component. */
	@GameTest(template = PLATFORM)
	public static void inventoryLoadsFromNbt(GameTestHelper h) throws Exception {
		h.setBlock(new BlockPos(4, 2, 4), ReactorBlocks.WASTE_CONTAINER.get());
		WasteContainerBlockEntity te = (WasteContainerBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.TagParser.parseTag("{inv:{Size:27,Items:[{Slot:26,id:\"reactorcraft:waste\",count:1,components:{\"reactorcraft:isotope\":10}}]}}");
		te.loadWithComponents(tag, h.getLevel().registryAccess());
		h.assertTrue(WasteUnitBlockEntity.getIsotope(te.getInventory().getStackInSlot(26)) == Isotopes.Cs134, "slot 26: "+te.getInventory().getStackInSlot(26));
		h.succeed();
	}

	/** The same through the setblock command (as the dev showcase world does). */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void inventoryLoadsFromSetblock(GameTestHelper h) {
		BlockPos p = h.absolutePos(new BlockPos(4, 2, 4));
		var src = h.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
		h.getLevel().getServer().getCommands().performPrefixedCommand(src, "setblock "+p.getX()+" "+p.getY()+" "+p.getZ()
				+" reactorcraft:waste_container{inv:{Size:27,Items:[{Slot:26,id:\"reactorcraft:waste\",count:1,components:{\"reactorcraft:isotope\":10}}]}} replace");
		h.runAtTickTime(20, () -> {
			WasteContainerBlockEntity te = (WasteContainerBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
			h.assertTrue(WasteUnitBlockEntity.getIsotope(te.getInventory().getStackInSlot(26)) == Isotopes.Cs134, "slot 26 after 20 ticks: "+te.getInventory().getStackInSlot(26));
			h.succeed();
		});
	}
}
