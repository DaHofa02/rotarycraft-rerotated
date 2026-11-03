/*******************************************************************************
 * Tests for the ported RotaryCraft coil winder and coils (original TileEntityWinder, ItemCoil). Private NeoForge 1.21.1 port of
 * RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.content.blockentity.WinderBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WinderTests {

	private static final String PLATFORM = "platform_9x4x9";

	/** DC engine (256 rad/s, 4 Nm) into the winder (metadata 0: input east): the wind spring is wound up to torque/stiffness = 4 kJ. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void winderWindsSpring(GameTestHelper h) {
		h.setBlock(new BlockPos(5, 1, 4), RotaryBlocks.DC_ENGINE.get());
		((EngineBlockEntity)h.getBlockEntity(new BlockPos(5, 1, 4))).setBlockMetadata(0);
		h.setBlock(new BlockPos(5, 1, 5), Blocks.REDSTONE_BLOCK);
		h.setBlock(new BlockPos(4, 1, 4), RotaryBlocks.WINDER.get());
		WinderBlockEntity w = (WinderBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		w.setBlockMetadata(0);
		w.getInventory().setStackInSlot(0, new ItemStack(RotaryItems.SPRING.get()));
		h.runAtTickTime(150, () -> {
			ItemStack is = w.getInventory().getStackInSlot(0);
			h.assertTrue(w.getTorque() == 4 && w.getMaxWind() == 4, "winder "+w.getOmega()+" rad/s "+w.getTorque()+" Nm, max "+w.getMaxWind());
			h.assertTrue(is.isEmpty() || CoilItem.getCharge(is) == 4, "spring at "+CoilItem.getCharge(is)+" kJ"); //a wound spring may snap (1 % on easy)
			h.succeed();
		});
	}

	/** Unwinding a wound spring gives 1024 rad/s at 8 Nm out of the input side, read by a dynamometer; a strong coil gives 4096 at 32. */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void winderUnwindsIntoShaft(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 1, 4), RotaryBlocks.WINDER.get());
		WinderBlockEntity w = (WinderBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		w.setBlockMetadata(0);
		w.getInventory().setStackInSlot(0, CoilItem.withCharge(new ItemStack(RotaryItems.SPRING.get()), 100));
		w.toggleMode();
		h.setBlock(new BlockPos(5, 1, 4), RotaryBlocks.DYNAMOMETER.get());
		DynamometerBlockEntity d = (DynamometerBlockEntity)h.getBlockEntity(new BlockPos(5, 1, 4));
		d.setBlockMetadata(1);
		h.runAtTickTime(25, () -> {
			h.assertTrue(w.getOmega() == 1024 && w.getTorque() == 8, "winder "+w.getOmega()+"/"+w.getTorque());
			h.assertTrue(d.getOmega() == 1024 && d.getTorque() == 8, "dynamometer "+d.getOmega()+"/"+d.getTorque());
			h.assertTrue(CoilItem.getCharge(w.getInventory().getStackInSlot(0)) == 99, "charge after 20 ticks "+CoilItem.getCharge(w.getInventory().getStackInSlot(0)));
			w.getInventory().setStackInSlot(0, CoilItem.withCharge(new ItemStack(RotaryItems.STRONGCOIL.get()), 100));
		});
		h.runAtTickTime(30, () -> {
			h.assertTrue(w.getOmega() == 4096 && w.getTorque() == 32, "strong coil "+w.getOmega()+"/"+w.getTorque());
			h.succeed();
		});
	}
}
