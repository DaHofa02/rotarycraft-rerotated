/*******************************************************************************
 * Tests for the ported RotaryCraft item cannon (original TileEntityItemCannon). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.ItemCannonBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ItemCannonTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** An engine at x=1 (east) -> bevel gears (west -> up) -> the firing cannon on top; the target cannon stands unpowered at x=6. */
	private static ItemCannonBlockEntity[] rig(GameTestHelper h, Block engine) {
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 2, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		ItemCannonBlockEntity a = place(h, RotaryBlocks.ITEM_CANNON.get(), 2, 3, 4, 0, ItemCannonBlockEntity.class);
		ItemCannonBlockEntity b = place(h, RotaryBlocks.ITEM_CANNON.get(), 6, 2, 4, 0, ItemCannonBlockEntity.class);
		BlockEntity e = place(h, engine, 1, 2, 4, 1, BlockEntity.class);
		if (e instanceof EngineBlockEntity eng)
			eng.addFuel(100000);
		a.setTarget(Level.OVERWORLD, h.absolutePos(new BlockPos(6, 2, 4)));
		return new ItemCannonBlockEntity[] {a, b};
	}

	/**
	 * A gas engine gives 65536 W (128 Nm, just the minimum torque): below 524288 W one item every 8 ticks goes to the target. Only the
	 * bottom side gives items out; it signals 15 when empty.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void itemCannonShootsSingleItems(GameTestHelper h) {
		ItemCannonBlockEntity[] c = rig(h, RotaryBlocks.GAS_ENGINE.get());
		ItemCannonBlockEntity a = c[0], b = c[1];
		h.assertTrue(a.getRedstoneOverride() == 15, "no signal when empty");
		a.getInventory().setStackInSlot(4, new ItemStack(Items.COBBLESTONE, 10));
		h.assertTrue(a.getItemHandler(Direction.NORTH).extractItem(4, 1, true).isEmpty() && !a.getItemHandler(Direction.DOWN).extractItem(4, 1, true).isEmpty(), "extraction sides");
		h.assertTrue(a.getRedstoneOverride() == 0, "signal with items");
		int[] seen = {-1, 0};
		h.onEachTick(() -> {
			int n = b.getInventory().getStackInSlot(0).getCount();
			if (n == 1 && seen[0] < 0)
				seen[0] = (int)h.getTick();
			if (n > seen[1] + 1)
				h.fail("more than one item at once: "+n);
			seen[1] = n;
		});
		h.succeedWhen(() -> {
			h.assertTrue(a.getPower() == 65536, "cannon at "+a.getPower()+" W");
			h.assertTrue(b.getInventory().getStackInSlot(0).getCount() == 5 && a.getInventory().getStackInSlot(4).getCount() == 5, "moved "+b.getInventory().getStackInSlot(0));
			h.assertTrue(seen[0] > 0 && h.getTick()-seen[0] >= 4*8-1, "5 items took "+(h.getTick()-seen[0])+" ticks after the first");
		});
	}

	/**
	 * A gas turbine (far above 524288 W once it has spun up; before that the cannon sends single items) sends whole stacks; a target that
	 * cannot take the whole stack gets nothing.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void itemCannonShootsStacks(GameTestHelper h) {
		ItemCannonBlockEntity[] c = rig(h, RotaryBlocks.JET_ENGINE.get());
		((JetEngineBlockEntity)h.getBlockEntity(new BlockPos(1, 2, 4))).addFuel(100000);
		ItemCannonBlockEntity a = c[0], b = c[1];
		for (int i = 0; i < 8; i++)
			b.getInventory().setStackInSlot(i, new ItemStack(Items.STONE, 64));
		b.getInventory().setStackInSlot(8, new ItemStack(Items.DIRT, 30)); //room for 34 only
		long[] at = {-1};
		h.succeedWhen(() -> {
			if (at[0] < 0) {
				h.assertTrue(a.getPower() >= ItemCannonBlockEntity.STACKPOWER, "cannon at "+a.getPower()+" W");
				a.getInventory().setStackInSlot(0, new ItemStack(Items.DIRT, 40));
				at[0] = h.getTick();
				h.fail("loaded");
			}
			if (h.getTick() < at[0]+40)
				h.fail("waiting");
			if (b.getInventory().getStackInSlot(8).getCount() == 30) {
				h.assertTrue(a.getInventory().getStackInSlot(0).getCount() == 40, "fired into a full target: "+a.getInventory().getStackInSlot(0));
				b.getInventory().setStackInSlot(8, ItemStack.EMPTY);
				h.fail("target emptied");
			}
			h.assertTrue(a.isEmpty() && b.getInventory().getStackInSlot(8).getCount() == 40, "stack not moved: "+b.getInventory().getStackInSlot(8));
		});
	}
}
