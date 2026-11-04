/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.registry.RotaryBlocks;

/** Original RotaryEventManager.buildWorktables (see WorktableBuilder): the Worktable built in the world by a (simulated) survival player. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WorktableBuildTests {

	private static final String PLATFORM = "platform_9x4x9";
	/** Centre column: redstone block at y 1, steel block at y 2, crafting table at y 3. */
	private static final BlockPos LOWER = new BlockPos(4, 1, 4);
	private static final BlockPos UPPER = LOWER.above();
	private static final BlockPos TABLE = UPPER.above();

	private static void structure(GameTestHelper h) {
		for (int i = -1; i <= 1; i++) {
			for (int k = -1; k <= 1; k++) {
				if (i == 0 && k == 0)
					continue;
				h.setBlock(LOWER.offset(i, 0, k), Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE));
				h.setBlock(UPPER.offset(i, 0, k), Blocks.BRICKS);
			}
		}
		h.setBlock(LOWER, Blocks.REDSTONE_BLOCK);
		h.setBlock(UPPER, RotaryBlocks.STEEL_BLOCK.get());
	}

	private static void assertBuilt(GameTestHelper h) {
		h.assertBlockPresent(RotaryBlocks.WORKTABLE.get(), LOWER);
		h.assertBlockPresent(Blocks.AIR, TABLE);
		h.assertBlockPresent(Blocks.AIR, UPPER);
		for (int i = -1; i <= 1; i++) {
			for (int k = -1; k <= 1; k++) {
				h.assertBlockPresent(Blocks.AIR, UPPER.offset(i, 0, k));
				if (i != 0 || k != 0)
					h.assertBlockPresent(Blocks.AIR, LOWER.offset(i, 0, k));
			}
		}
	}

	/** Setting the crafting table down onto the steel block builds the Worktable; the table is used up. */
	@GameTest(template = PLATFORM)
	public static void worktableFromPlacedCraftingTable(GameTestHelper h) {
		structure(h);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		p.give(Items.CRAFTING_TABLE, 1);
		p.hold(Items.CRAFTING_TABLE);
		p.look(Direction.NORTH);
		p.useOn(UPPER, Direction.UP);
		assertBuilt(h);
		h.assertTrue(p.count(Items.CRAFTING_TABLE) == 0, "crafting table not used up");
		p.leave();
		h.succeed();
	}

	/** Right-clicking a crafting table that already stands on the structure builds it too. */
	@GameTest(template = PLATFORM)
	public static void worktableFromRightClick(GameTestHelper h) {
		structure(h);
		h.setBlock(TABLE, Blocks.CRAFTING_TABLE);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		p.emptyHand();
		p.use(TABLE);
		assertBuilt(h);
		p.leave();
		h.succeed();
	}

	/** One brick missing (stone instead): nothing happens, the crafting table stays. */
	@GameTest(template = PLATFORM)
	public static void worktableNeedsWholeStructure(GameTestHelper h) {
		structure(h);
		h.setBlock(UPPER.offset(1, 0, 1), Blocks.STONE);
		h.setBlock(TABLE, Blocks.CRAFTING_TABLE);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		p.emptyHand();
		p.use(TABLE);
		h.assertBlockPresent(Blocks.CRAFTING_TABLE, TABLE);
		h.assertBlockPresent(RotaryBlocks.STEEL_BLOCK.get(), UPPER);
		h.assertBlockPresent(Blocks.REDSTONE_BLOCK, LOWER);
		p.closeMenu();
		p.leave();
		h.succeed();
	}
}
