/*******************************************************************************
 * Tests for the port's terraformer. Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.TerraformerBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TerraformerTests {

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
	 * A gas engine beside the terraformer (65536 W: 800 - 40*log2(513) = 439 ticks a cell), a redstone block on it, radius 0, oak saplings: its
	 * own biome cell becomes forest and a sapling is used. Swamp (131072 W) is beyond the engine. The old biome is put back afterwards.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 1100, batch = "terraformer")
	public static void terraformerPlantsAForest(GameTestHelper h) {
		TerraformerBlockEntity te = place(h, RotaryBlocks.TERRAFORMER.get(), 4, 2, 4, 0, TerraformerBlockEntity.class);
		BlockPos at = h.absolutePos(new BlockPos(4, 2, 4));
		Holder<Biome> before = TerraformerBlockEntity.cellBiome(h.getLevel(), at);
		h.assertTrue(TerraformerBlockEntity.isChangeable(before) && !before.is(Biomes.FOREST), "test biome "+before.getRegisteredName());
		place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		h.setBlock(new BlockPos(4, 3, 4), Blocks.REDSTONE_BLOCK);
		te.setRadius(0);
		te.getInventory().setStackInSlot(0, new ItemStack(Items.OAK_SAPLING, 4));
		te.getInventory().setStackInSlot(1, new ItemStack(Items.LILY_PAD, 4));
		te.addWater(2000);
		te.setTarget(TerraformerBlockEntity.Target.SWAMP);
		h.runAtTickTime(500, () -> {
			h.assertTrue(te.getPower() == 65536 && te.getOperationTime() == 439, te.getPower()+" W, "+te.getOperationTime()+" ticks");
			h.assertTrue(te.changed == 0 && TerraformerBlockEntity.cellBiome(h.getLevel(), at).is(before), "swamp made with 65536 W");
			te.setTarget(TerraformerBlockEntity.Target.FOREST);
		});
		h.succeedWhen(() -> {
			h.assertTrue(h.getTick() > 500 && TerraformerBlockEntity.cellBiome(h.getLevel(), at).is(Biomes.FOREST), "biome "+TerraformerBlockEntity.cellBiome(h.getLevel(), at).getRegisteredName()+", changed "+te.changed+", target "+te.getTarget()+", "+te.getPower()+" W, saplings "+te.getInventory().getStackInSlot(0)+", signal "+h.getLevel().hasNeighborSignal(at));
			h.assertTrue(te.getInventory().getStackInSlot(0).getCount() == 3 && te.changed == 1, "saplings "+te.getInventory().getStackInSlot(0));
			h.setBlock(new BlockPos(4, 3, 4), Blocks.AIR);
			TerraformerBlockEntity.setBiomeColumn(h.getLevel(), at, before);
		});
	}
}
