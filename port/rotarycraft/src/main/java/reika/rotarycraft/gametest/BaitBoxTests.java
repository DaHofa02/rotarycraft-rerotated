/*******************************************************************************
 * Tests for the ported RotaryCraft bait box (original TileEntityBaitBox). Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki;
 * not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BaitBoxBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class BaitBoxTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine (65536 W, meta 0: output west) east of a bait box at (4, 2, 4): range 16. */
	private static BaitBoxBlockEntity rig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
		return place(h, RotaryBlocks.BAIT_BOX.get(), 4, 2, 4, 0, BaitBoxBlockEntity.class);
	}

	private static double dist(GameTestHelper h, Cow c) {
		BlockPos p = h.absolutePos(new BlockPos(4, 2, 4));
		return Math.sqrt(c.distanceToSqr(p.getX()+0.5, p.getY(), p.getZ()+0.5));
	}

	/** Range 8 + (power - 32768)/4096, at most 24; the original bait table. */
	@GameTest(template = PLATFORM)
	public static void baitBoxFollowsTheOriginalNumbers(GameTestHelper h) {
		BaitBoxBlockEntity te = place(h, RotaryBlocks.BAIT_BOX.get(), 4, 2, 4, 0, BaitBoxBlockEntity.class);
		long[][] cases = {{32768, 8}, {65536, 16}, {2097152, 24}};
		for (long[] c : cases) {
			te.power = c[0];
			h.assertTrue(te.getRange() == c[1], c[0]+" W: range "+te.getRange());
		}
		h.assertTrue(BaitBoxBlockEntity.getBaits().size() == 23, BaitBoxBlockEntity.getBaits().size()+" baits");
		Cow cow = h.spawn(EntityType.COW, new BlockPos(2, 2, 2));
		BaitBoxBlockEntity.MobBait b = BaitBoxBlockEntity.getBait(cow);
		h.assertTrue(b != null && b.attract().test(new ItemStack(Items.WHEAT)) && b.repel().test(new ItemStack(Items.STICK)) && !b.attract().test(new ItemStack(Items.CARROT)), "cow bait");
		h.succeed();
	}

	/** With wheat, a cow 4 blocks off comes to the box. */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "baitbox")
	public static void baitBoxDrawsCows(GameTestHelper h) {
		BaitBoxBlockEntity te = rig(h);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.WHEAT));
		Cow cow = h.spawn(EntityType.COW, new BlockPos(4, 2, 8));
		h.succeedWhen(() -> h.assertTrue(te.canAttract(cow) && dist(h, cow) < 2, "cow "+dist(h, cow)+" away, power "+te.power));
	}

	/** With a stick, a cow next to the box walks away. */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "baitbox")
	public static void baitBoxDrivesCowsAway(GameTestHelper h) {
		BaitBoxBlockEntity te = rig(h);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.STICK));
		Cow cow = h.spawn(EntityType.COW, new BlockPos(4, 2, 5));
		h.succeedWhen(() -> h.assertTrue(te.canRepel(cow) && dist(h, cow) > 3, "cow only "+dist(h, cow)+" away"));
	}

	/** An egg beside water hatches a chicken; with lava beside it too, it becomes a cooked chicken instead. */
	@GameTest(template = PLATFORM, timeoutTicks = 600, batch = "baitbox")
	public static void baitBoxHatchesEggs(GameTestHelper h) {
		BaitBoxBlockEntity te = rig(h);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.EGG, 2));
		h.setBlock(new BlockPos(4, 2, 3), Blocks.WATER);
		for (BlockPos g : new BlockPos[] {new BlockPos(3, 2, 3), new BlockPos(5, 2, 3), new BlockPos(4, 2, 2)})
			h.setBlock(g, Blocks.GLASS);
		BaitBoxBlockEntity hot = place(h, RotaryBlocks.BAIT_BOX.get(), 1, 2, 7, 0, BaitBoxBlockEntity.class);
		place(h, RotaryBlocks.GAS_ENGINE.get(), 2, 2, 7, 0, EngineBlockEntity.class).addFuel(50000);
		hot.getInventory().setStackInSlot(0, new ItemStack(Items.EGG, 1));
		for (BlockPos g : new BlockPos[] {new BlockPos(0, 2, 8), new BlockPos(2, 2, 8), new BlockPos(0, 2, 6), new BlockPos(2, 2, 6), new BlockPos(1, 2, 5)})
			h.setBlock(g, Blocks.GLASS);
		h.setBlock(new BlockPos(1, 2, 8), Blocks.WATER); //water and lava each in glass, apart from each other
		h.setBlock(new BlockPos(1, 2, 6), Blocks.LAVA);
		h.succeedWhen(() -> {
			int chickens = h.getLevel().getEntitiesOfClass(Chicken.class, new AABB(h.absolutePos(new BlockPos(4, 2, 4))).inflate(3)).size();
			h.assertTrue(chickens >= 1 && te.getInventory().getStackInSlot(0).getCount() < 2, chickens+" chickens hatched");
			h.assertTrue(hot.getInventory().getStackInSlot(0).isEmpty() || !hot.getInventory().getStackInSlot(0).is(Items.EGG), "hot box egg left");
			boolean cooked = false;
			for (int i = 0; i < 27; i++)
				cooked |= hot.getInventory().getStackInSlot(i).is(Items.COOKED_CHICKEN);
			h.assertTrue(cooked, "no cooked chicken");
		});
	}
}
