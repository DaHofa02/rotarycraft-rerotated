/*******************************************************************************
 * Tests for the ported RotaryCraft multi-clutch (original TileEntityMultiClutch). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.GrinderBlockEntity;
import reika.rotarycraft.content.blockentity.MultiClutchBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.menu.MultiClutchMenu;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class MultiClutchTests {

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
	 * Gas engine (512 rad/s @ 128 Nm) east of a multi-clutch (meta 0: input east). Without redstone (level 0) it drives the grinder west
	 * of it; with a redstone block beside it (level 15) the grinder north of it.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void multiClutchPicksTheSideByRedstoneLevel(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
		MultiClutchBlockEntity mc = place(h, RotaryBlocks.MULTI_CLUTCH.get(), 4, 2, 4, 0, MultiClutchBlockEntity.class);
		GrinderBlockEntity west = place(h, RotaryBlocks.GRINDER.get(), 3, 2, 4, 0, GrinderBlockEntity.class); //meta 0: input east
		GrinderBlockEntity north = place(h, RotaryBlocks.GRINDER.get(), 4, 2, 3, 2, GrinderBlockEntity.class); //meta 2: input south
		mc.setSideOfState(0, 4);
		mc.setSideOfState(15, 2);
		int[] stage = {0};
		h.succeedWhen(() -> {
			if (stage[0] == 0) {
				h.assertTrue(mc.getRedstoneLevel() == 0 && mc.getWriteDirection() == Direction.WEST, "level "+mc.getRedstoneLevel()+", out "+mc.getWriteDirection());
				h.assertTrue(west.omega == 512 && west.torque == 128 && north.omega == 0, "level 0: west "+west.omega+", north "+north.omega);
				h.setBlock(new BlockPos(4, 2, 5), Blocks.REDSTONE_BLOCK);
				stage[0] = 1;
				h.fail("redstone block set");
			}
			h.assertTrue(mc.getRedstoneLevel() == 15 && mc.getWriteDirection() == Direction.NORTH, "level "+mc.getRedstoneLevel()+", out "+mc.getWriteDirection());
			h.assertTrue(north.omega == 512 && north.torque == 128 && west.omega == 0, "level 15: west "+west.omega+", north "+north.omega);
		});
	}

	/** All levels start at down; GUI button i moves level i on through down, up, north, south, west, east and back; kept on save and load. */
	@GameTest(template = PLATFORM)
	public static void multiClutchSidesAreSetInTheGui(GameTestHelper h) {
		MultiClutchBlockEntity mc = place(h, RotaryBlocks.MULTI_CLUTCH.get(), 4, 2, 4, 0, MultiClutchBlockEntity.class);
		for (int i = 0; i < 16; i++)
			h.assertTrue(mc.getSideOfState(i) == 0, "level "+i+" starts at "+mc.getSideOfState(i));
		MultiClutchMenu menu = new MultiClutchMenu(1, h.makeMockPlayer(GameType.SURVIVAL).getInventory(), mc);
		Direction[] order = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN};
		for (Direction d : order) {
			h.assertTrue(menu.clickMenuButton(null, 7), "button 7 refused");
			h.assertTrue(MultiClutchBlockEntity.side(mc.getSideOfState(7)) == d, "level 7 at "+MultiClutchBlockEntity.side(mc.getSideOfState(7))+", not "+d);
		}
		h.assertFalse(menu.clickMenuButton(null, 16), "button 16 exists");
		mc.setSideOfState(3, 5);
		mc.setSideOfState(3, 6);
		h.assertTrue(mc.getSideOfState(3) == 5, "side 6 accepted");
		CompoundTag tag = mc.saveWithoutMetadata(h.getLevel().registryAccess());
		h.setBlock(new BlockPos(4, 2, 4), Blocks.AIR);
		MultiClutchBlockEntity copy = place(h, RotaryBlocks.MULTI_CLUTCH.get(), 4, 2, 4, 0, MultiClutchBlockEntity.class);
		copy.loadWithComponents(tag, h.getLevel().registryAccess());
		h.assertTrue(copy.getSideOfState(3) == 5 && copy.getSideOfState(7) == 0, "loaded sides "+copy.getSideOfState(3)+", "+copy.getSideOfState(7));
		h.succeed();
	}
}
