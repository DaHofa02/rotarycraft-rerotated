/*******************************************************************************
 * Tests for the ported RotaryCraft firework machine (original TileEntityFireworkMachine). Private NeoForge 1.21.1 port of RotaryCraft by
 * Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.FireworkMachineBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FireworkMachineTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/**
	 * A microturbine (meta 1: east; here partly choked by the wall behind it) beside the machine: one launch every
	 * max(1, 300 - 16*log2(omega+1)) ticks. With paper, gunpowder and red dye it makes its own red stars and launches rockets; only non-ingredients are refused, and
	 * without paper there is nothing to craft (a redstone signal).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void fireworkMachineLaunchesRockets(GameTestHelper h) {
		place(h, RotaryBlocks.MICRO_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(100000);
		FireworkMachineBlockEntity te = place(h, RotaryBlocks.FIREWORK_MACHINE.get(), 2, 2, 4, 0, FireworkMachineBlockEntity.class);
		h.assertTrue(te.getInventory().insertItem(0, new ItemStack(Items.STONE), true).getCount() == 1, "stone accepted");
		h.assertTrue(te.getRedstoneOverride() == 15, "no signal when empty");
		te.getInventory().setStackInSlot(0, new ItemStack(Items.PAPER, 16));
		te.getInventory().setStackInSlot(1, new ItemStack(Items.GUNPOWDER, 32));
		te.getInventory().setStackInSlot(2, new ItemStack(Items.RED_DYE, 16));
		h.succeedWhen(() -> {
			h.assertTrue(te.getOperationTime() == (int)Math.max(1, 300-16*Math.log(te.getOmega()+1)/Math.log(2)), "operation time "+te.getOperationTime()+" at "+te.getOmega()+" rad/s");
			h.assertTrue(te.launched >= 2, te.launched+" rockets launched");
			boolean red = false;
			for (FireworkRocketEntity r : h.getLevel().getEntitiesOfClass(FireworkRocketEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(40)))
				red |= r.getItem().has(DataComponents.FIREWORKS)
						&& r.getItem().get(DataComponents.FIREWORKS).explosions().stream().anyMatch(e -> e.colors().contains(net.minecraft.world.item.DyeColor.RED.getFireworkColor()));
			h.assertTrue(red, "no red rocket in flight");
		});
	}

	/** A firework star in the store is launched as it is (with paper and gunpowder); without paper nothing goes up. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void fireworkMachineUsesStars(GameTestHelper h) {
		place(h, RotaryBlocks.MICRO_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(100000);
		FireworkMachineBlockEntity te = place(h, RotaryBlocks.FIREWORK_MACHINE.get(), 2, 2, 4, 0, FireworkMachineBlockEntity.class);
		ItemStack star = new ItemStack(Items.FIREWORK_STAR, 8);
		star.set(DataComponents.FIREWORK_EXPLOSION, new net.minecraft.world.item.component.FireworkExplosion(net.minecraft.world.item.component.FireworkExplosion.Shape.STAR,
				it.unimi.dsi.fastutil.ints.IntList.of(0x123456), it.unimi.dsi.fastutil.ints.IntList.of(), true, false));
		te.getInventory().setStackInSlot(0, star);
		te.getInventory().setStackInSlot(1, new ItemStack(Items.GUNPOWDER, 16));
		h.runAtTickTime(60, () -> {
			h.assertTrue(te.launched == 0 && te.getRedstoneOverride() == 0, "launched "+te.launched+" without paper");
			te.getInventory().setStackInSlot(2, new ItemStack(Items.PAPER, 8));
		});
		h.runAtTickTime(150, () -> {
			h.assertTrue(te.launched >= 1, "no rocket from the star");
			h.succeed();
		});
	}
}
