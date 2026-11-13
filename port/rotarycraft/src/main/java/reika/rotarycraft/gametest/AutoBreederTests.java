/*******************************************************************************
 * Tests for the ported RotaryCraft auto breeder (original TileEntityAutoBreeder). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.AutoBreederBlockEntity;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AutoBreederTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine (65536 W) -> bevel gears west->up -> auto breeder above them at (4, 3, 4). */
	private static AutoBreederBlockEntity rig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		return place(h, RotaryBlocks.AUTO_BREEDER.get(), 4, 3, 4, 0, AutoBreederBlockEntity.class);
	}

	/** Range 8 + (power - 16384)/2048 (at most 24); the original food table. */
	@GameTest(template = PLATFORM)
	public static void breederFollowsTheOriginalNumbers(GameTestHelper h) {
		AutoBreederBlockEntity te = place(h, RotaryBlocks.AUTO_BREEDER.get(), 4, 3, 4, 0, AutoBreederBlockEntity.class);
		long[][] cases = {{16384, 8}, {32768, 16}, {65536, 24}};
		for (long[] c : cases) {
			te.power = c[0];
			h.assertTrue(te.getRange() == c[1], c[0]+" W: range "+te.getRange());
		}
		h.assertTrue(AutoBreederBlockEntity.getFeedItems(EntityType.COW).contains(Items.WHEAT) && AutoBreederBlockEntity.getFeedItems(EntityType.PIG).contains(RotaryItems.CANOLA_HUSKS.get())
				&& !AutoBreederBlockEntity.getFeedItems(EntityType.PIG).contains(Items.WHEAT) && AutoBreederBlockEntity.getFeedItems(EntityType.HORSE).isEmpty(), "food table");
		h.assertTrue(te.isIdle(), "idle without food");
		te.getInventory().setStackInSlot(0, new ItemStack(Items.WHEAT));
		h.assertFalse(te.isIdle(), "idle with wheat");
		h.succeed();
	}

	/** Two cows next to it (penned in, away from the spinning gears) are put in love for one wheat each and make a calf; the pig, without carrots, is left alone. */
	@GameTest(template = PLATFORM, timeoutTicks = 600, batch = "breeder") //on its own: the beams and blowers of neighbouring tests kill cows
	public static void breederBreedsCows(GameTestHelper h) {
		AutoBreederBlockEntity te = rig(h);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.WHEAT, 8));
		//glass over the bevel gears' free faces (spinning gears hurt mobs) and a pen around the corners next to the breeder
		for (BlockPos g : new BlockPos[] {new BlockPos(4, 2, 3), new BlockPos(5, 2, 4), new BlockPos(4, 2, 5)})
			h.setBlock(g, net.minecraft.world.level.block.Blocks.GLASS);
		for (int i = 2; i <= 6; i++)
			for (int y = 2; y <= 3; y++)
				for (BlockPos g : new BlockPos[] {new BlockPos(2, y, i), new BlockPos(6, y, i), new BlockPos(i, y, 2), new BlockPos(i, y, 6)})
					h.setBlock(g, net.minecraft.world.level.block.Blocks.GLASS);
		Cow a = h.spawn(EntityType.COW, new BlockPos(5, 2, 5));
		Cow b = h.spawn(EntityType.COW, new BlockPos(5, 2, 3));
		Pig pig = h.spawn(EntityType.PIG, new BlockPos(5, 2, 5));
		h.succeedWhen(() -> {
			h.assertTrue(te.power == 65536, "breeder at "+te.power+" W");
			int calves = 0;
			StringBuilder sb = new StringBuilder();
			for (Cow c : h.getLevel().getEntitiesOfClass(Cow.class, new AABB(h.absolutePos(new BlockPos(4, 2, 4))).inflate(8))) {
				calves += c.isBaby() ? 1 : 0;
				sb.append(" [age ").append(c.getAge()).append(" love ").append(c.getInLoveTime()).append(" at ").append(c.blockPosition().subtract(h.absolutePos(BlockPos.ZERO))).append("]");
			}
			h.assertTrue(calves >= 1, "no calf"+sb+"; wheat left "+te.getInventory().getStackInSlot(0).getCount()+", cows in love "+a.isInLove()+" "+b.isInLove());
			h.assertTrue(te.getInventory().getStackInSlot(0).getCount() <= 6, "wheat not used");
			h.assertFalse(pig.isInLove(), "pig in love");
		});
	}
}
