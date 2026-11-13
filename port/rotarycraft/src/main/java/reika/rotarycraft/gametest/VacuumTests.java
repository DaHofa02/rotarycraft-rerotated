/*******************************************************************************
 * Tests for the ported RotaryCraft item vacuum (original TileEntityVacuum). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.VacuumBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class VacuumTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	private static int count(VacuumBlockEntity te, net.minecraft.world.item.Item item) {
		int n = 0;
		for (int i = 0; i < 54; i++)
			if (te.getInventory().getStackInSlot(i).is(item))
				n += te.getInventory().getStackInSlot(i).getCount();
		return n;
	}

	/** Range 8 + power/4096, at most 32; the screwdriver toggles. */
	@GameTest(template = PLATFORM)
	public static void vacuumFollowsTheOriginalNumbers(GameTestHelper h) {
		VacuumBlockEntity te = place(h, RotaryBlocks.VACUUM.get(), 4, 2, 4, 0, VacuumBlockEntity.class);
		long[][] cases = {{16384, 12}, {65536, 24}, {1L << 30, 32}};
		for (long[] c : cases) {
			te.power = c[0];
			h.assertTrue(te.getRange() == c[1], c[0]+" W: range "+te.getRange());
		}
		te.onShiftRightClick(h.getLevel(), te.getBlockPos(), Direction.UP);
		te.onRightClick(h.getLevel(), te.getBlockPos(), Direction.UP);
		h.assertTrue(!te.equidistant && !te.suckIfFull, "screwdriver toggles");
		h.assertTrue(te.getRedstoneOverride() == 0, "redstone while empty");
		h.succeed();
	}

	/**
	 * Powered by a gas engine (65536 W: whole stacks) it pulls dropped items and XP from across the area into itself, empties the chest
	 * beside it, and gives the XP back out with the button.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "vacuum")
	public static void vacuumCollectsItemsXpAndChests(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
		VacuumBlockEntity te = place(h, RotaryBlocks.VACUUM.get(), 4, 2, 4, 0, VacuumBlockEntity.class);
		h.setBlock(new BlockPos(3, 2, 4), Blocks.CHEST);
		((Container)h.getBlockEntity(new BlockPos(3, 2, 4))).setItem(5, new ItemStack(Items.COBBLESTONE, 20));
		Vec3 far = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 2, 7)));
		h.getLevel().addFreshEntity(new ItemEntity(h.getLevel(), far.x, far.y, far.z, new ItemStack(Items.DIAMOND, 3), 0, 0, 0));
		h.getLevel().addFreshEntity(new ExperienceOrb(h.getLevel(), far.x+2, far.y, far.z, 7));
		h.succeedWhen(() -> {
			h.assertTrue(count(te, Items.DIAMOND) == 3 && count(te, Items.COBBLESTONE) == 20, count(te, Items.DIAMOND)+" diamonds, "+count(te, Items.COBBLESTONE)+" cobblestone, "+te.power+" W");
			h.assertTrue(te.getExperience() == 7, "xp "+te.getExperience());
			h.assertTrue(((Container)h.getBlockEntity(new BlockPos(3, 2, 4))).isEmpty(), "chest not emptied");
			te.spawnXP();
			h.assertTrue(te.getExperience() == 0, "xp not let out");
		});
	}
}
