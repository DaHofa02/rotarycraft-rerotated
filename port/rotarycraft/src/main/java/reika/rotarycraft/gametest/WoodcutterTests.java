/*******************************************************************************
 * Tests for the ported RotaryCraft woodcutter (original TileEntityWoodcutter). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.WoodcutterBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WoodcutterTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine (meta 1: east) into a woodcutter at (2, 2, 4) (meta 1: input west, cuts east), a chest below it. */
	private static WoodcutterBlockEntity rig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		h.setBlock(new BlockPos(2, 1, 4), Blocks.CHEST);
		return place(h, RotaryBlocks.WOODCUTTER.get(), 2, 2, 4, 1, WoodcutterBlockEntity.class);
	}

	/** A four-log oak on dirt two blocks ahead with a 3x3x2 leaf crown (persistent leaves, so nothing decays on its own). */
	private static void oak(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 1, 4), Blocks.DIRT);
		for (int y = 2; y <= 5; y++)
			h.setBlock(new BlockPos(4, y, 4), Blocks.OAK_LOG);
		for (int y = 5; y <= 6; y++)
			for (int x = 3; x <= 5; x++)
				for (int z = 3; z <= 5; z++)
					if (!(x == 4 && z == 4 && y == 5))
						h.setBlock(new BlockPos(x, y, z), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
	}

	/**
	 * 512 rad/s: one block every max(1, 40 - 4*log2(513)) = 3 ticks. The whole tree goes (leaves first), the four logs land in the chest
	 * below, and the bottom log is replaced by an oak sapling from the store. With no log left it gives a redstone signal.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void woodcutterCutsAndReplants(GameTestHelper h) {
		WoodcutterBlockEntity te = rig(h);
		oak(h);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.OAK_SAPLING, 2));
		h.succeedWhen(() -> {
			h.assertTrue(te.getPower() == 65536 && te.getOperationTime() == 3 && te.getNumberConsecutiveOperations() == 1,
					"woodcutter "+te.getPower()+" W, time "+te.getOperationTime());
			h.assertTrue(te.getTree().isEmpty(), te.getTree().size()+" blocks left");
			for (int y = 3; y <= 6; y++)
				for (int x = 3; x <= 5; x++)
					for (int z = 3; z <= 5; z++)
						h.assertBlockPresent(Blocks.AIR, new BlockPos(x, y, z));
			h.assertBlockPresent(Blocks.OAK_SAPLING, new BlockPos(4, 2, 4));
			ChestBlockEntity chest = (ChestBlockEntity)h.getBlockEntity(new BlockPos(2, 1, 4));
			int logs = 0;
			for (int i = 0; i < chest.getContainerSize(); i++)
				if (chest.getItem(i).is(Items.OAK_LOG))
					logs += chest.getItem(i).getCount();
			h.assertTrue(logs == 4, logs+" logs in the chest");
			h.assertTrue(te.getRedstoneOverride() == 15, "no signal without wood");
		});
	}

	/** A stone block in the store is not this tree's sapling: it goes into the chest. A cow on the running woodcutter is hurt. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void woodcutterDumpsAndHurts(GameTestHelper h) {
		WoodcutterBlockEntity te = rig(h);
		oak(h);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.STONE, 3));
		Cow cow = h.spawn(EntityType.COW, new Vec3(2.5, 2.875, 4.5)); //standing on it (with AI: entities are only checked against the blocks they touch when they move)
		float hp = cow.getHealth();
		h.succeedWhen(() -> {
			h.assertTrue(te.hasWood() || te.getTree().size() > 0 || te.getTreeType() != null, "no wood seen");
			h.assertTrue(te.getInventory().getStackInSlot(0).isEmpty(), "store holds "+te.getInventory().getStackInSlot(0));
			ChestBlockEntity chest = (ChestBlockEntity)h.getBlockEntity(new BlockPos(2, 1, 4));
			StringBuilder in = new StringBuilder();
			int stone = 0;
			for (int i = 0; i < chest.getContainerSize(); i++) {
				stone += chest.getItem(i).is(Items.STONE) ? chest.getItem(i).getCount() : 0;
				if (!chest.getItem(i).isEmpty())
					in.append(chest.getItem(i)).append(' ');
			}
			h.assertTrue(stone == 3, "chest holds "+in);
			h.assertTrue(!cow.isAlive() || cow.getHealth() < hp, "cow not hurt");
		});
	}
}
