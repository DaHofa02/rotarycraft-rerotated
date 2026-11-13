/*******************************************************************************
 * Tests for the ported RotaryCraft floodlight (original TileEntityFloodlight). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.FloodlightBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FloodlightTests {

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
	 * A DC engine gives exactly the minimum 1024 W (meta 1: east) to a floodlight (meta 1: input west, shines east): light blocks of
	 * level 15 up to the stone three blocks ahead (range 4, counting the stone), none behind it. Without the redstone signal the engine
	 * stops and the lights go out.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void floodlightLightsTheLine(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class);
		BlockPos red = new BlockPos(1, 2, 3);
		h.setBlock(red, Blocks.REDSTONE_BLOCK);
		FloodlightBlockEntity te = place(h, RotaryBlocks.FLOODLIGHT.get(), 2, 2, 4, 1, FloodlightBlockEntity.class);
		h.setBlock(new BlockPos(6, 2, 4), Blocks.STONE);
		boolean[] off = {false};
		h.succeedWhen(() -> {
			if (!off[0]) {
				h.assertTrue(te.getPower() == 1024 && te.getRange() == 4, "floodlight "+te.getPower()+" W, range "+te.getRange());
				for (int x = 3; x <= 5; x++)
					h.assertBlockPresent(Blocks.LIGHT, new BlockPos(x, 2, 4));
				h.assertBlockNotPresent(Blocks.LIGHT, new BlockPos(7, 2, 4));
				int lit = h.getLevel().getBrightness(LightLayer.BLOCK, h.absolutePos(new BlockPos(4, 2, 4)));
				h.assertTrue(lit == 15, "block light "+lit);
				h.setBlock(red, Blocks.AIR);
				off[0] = true;
				h.fail("lit; now without power");
			}
			h.assertTrue(te.getRange() == 0, "range without power "+te.getRange());
			for (int x = 3; x <= 5; x++)
				h.assertBlockPresent(Blocks.AIR, new BlockPos(x, 2, 4));
		});
	}

	/**
	 * With a lens (one only) the light becomes a cone at most 24 deep: one block at distance 1, 3x3 at 2 and 3, 5x5 at 4, not into the
	 * floor. Breaking the floodlight puts the lights out and drops the lens.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void floodlightLensMakesACone(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 3, 4, 1, EngineBlockEntity.class).addFuel(50000);
		BlockPos pos = new BlockPos(2, 3, 4);
		FloodlightBlockEntity te = place(h, RotaryBlocks.FLOODLIGHT.get(), 2, 3, 4, 1, FloodlightBlockEntity.class);
		ItemStack lens = new ItemStack(RotaryItems.part(CraftingPart.LENS));
		h.assertTrue(te.canUpgradeWith(lens), "floodlight takes no lens");
		te.upgrade(lens);
		h.assertFalse(te.canUpgradeWith(lens), "floodlight takes a second lens");
		h.assertTrue(te.getMaxRange() == 24, "max range "+te.getMaxRange());
		BlockPos[] lit = {new BlockPos(3, 3, 4), new BlockPos(4, 2, 3), new BlockPos(4, 4, 5), new BlockPos(5, 2, 5), new BlockPos(6, 5, 6), new BlockPos(6, 2, 2)};
		boolean[] broken = {false};
		h.succeedWhen(() -> {
			if (!broken[0]) {
				for (BlockPos p : lit)
					h.assertBlockPresent(Blocks.LIGHT, p);
				h.assertBlockNotPresent(Blocks.LIGHT, new BlockPos(3, 3, 5)); //distance 1 is a single block
				h.assertBlockPresent(Blocks.STONE, new BlockPos(6, 1, 4)); //the floor stays
				h.destroyBlock(pos);
				broken[0] = true;
				h.fail("cone lit; now broken");
			}
			for (BlockPos p : lit)
				h.assertBlockNotPresent(Blocks.LIGHT, p);
			boolean dropped = false;
			for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(pos)).inflate(3)))
				dropped |= e.getItem().is(lens.getItem());
			h.assertTrue(dropped, "no lens dropped");
		});
	}
}
