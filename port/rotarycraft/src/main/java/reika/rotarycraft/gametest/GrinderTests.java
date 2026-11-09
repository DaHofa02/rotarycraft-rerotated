/*******************************************************************************
 * Regression tests for the ported Grinder, canola and lubricant chain. Expected values follow from the original code
 * (PowerReceivers.GRINDER, DurationRegistry.GRINDER, DifficultyEffects.CANOLA, BlockCanola).
 * Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.CanolaBlock;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.GrinderBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class GrinderTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (!cls.isInstance(be))
			throw new IllegalStateException("no "+cls.getSimpleName()+" at "+p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Gas engine (512 rad/s @ 128 Nm, meta 0: output west) directly into a Grinder (meta 0: reads from the east). */
	private static GrinderBlockEntity grinderRig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 1, 4, 0, EngineBlockEntity.class).addFuel(50000);
		return place(h, RotaryBlocks.GRINDER.get(), 4, 1, 4, 0, GrinderBlockEntity.class);
	}

	/**
	 * Two canola seeds: each operation (840 - 60*log2(513) = 299 ticks at 512 rad/s) gives one husk and 64-160 mB lubricant
	 * (medium difficulty); a hose on the Grinder's side carries the lubricant to a gearbox.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 900)
	public static void grinderMakesLubricant(GameTestHelper h) {
		GrinderBlockEntity gr = grinderRig(h);
		gr.getInventory().setStackInSlot(0, new ItemStack(RotaryItems.CANOLA_SEEDS.get(), 2));
		place(h, RotaryBlocks.HOSE.get(), 4, 1, 3, 0, BlockEntity.class);
		GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 4, 1, 2, 0, GearboxBlockEntity.class);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.STEEL, 2));
		gbx.setLubricant(0);
		h.succeedWhen(() -> {
			h.assertTrue(gr.omega == 512 && gr.torque == 128, "grinder input "+gr.omega+" rad/s @ "+gr.torque+" Nm");
			ItemStack out = gr.getInventory().getStackInSlot(1);
			h.assertTrue(out.is(RotaryItems.CANOLA_HUSKS.get()) && out.getCount() == 2, "output "+out);
			//the hose between them holds some lubricant too (pipes keep a remainder); without it two low rolls (64 + 64) fell short of 128
			int total = gr.getLubricant()+gbx.getLubricant()+((reika.rotarycraft.content.piping.PipingBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 3))).getFluidLevel();
			h.assertTrue(gbx.getLubricant() > 0, "no lubricant reached the gearbox (grinder "+gr.getLubricant()+")");
			h.assertTrue(total >= 2*64 && total <= 2*160, "lubricant "+total+" mB (2 seeds, 64-160 each)");
		});
	}

	/** Plain grinding recipe: cobblestone -> gravel. */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void grinderGrindsCobblestone(GameTestHelper h) {
		GrinderBlockEntity gr = grinderRig(h);
		gr.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
		h.succeedWhen(() -> h.assertTrue(gr.getInventory().getStackInSlot(1).is(Items.GRAVEL), "output "+gr.getInventory().getStackInSlot(1)));
	}

	/** Not enough torque: a DC engine (4 Nm) does not run the Grinder (128 Nm). */
	@GameTest(template = PLATFORM, timeoutTicks = 400)
	public static void grinderNeedsTorque(GameTestHelper h) {
		place(h, RotaryBlocks.DC_ENGINE.get(), 5, 1, 4, 0, EngineBlockEntity.class);
		h.setBlock(new BlockPos(5, 2, 4), Blocks.REDSTONE_BLOCK);
		GrinderBlockEntity gr = place(h, RotaryBlocks.GRINDER.get(), 4, 1, 4, 0, GrinderBlockEntity.class);
		gr.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
		h.runAtTickTime(350, () -> {
			h.assertTrue(gr.getInventory().getStackInSlot(1).isEmpty() && gr.grinderCookTime == 0, "ground at "+gr.torque+" Nm");
			h.succeed();
		});
	}

	/** With 1000 mB in the tank an empty bucket in slot 2 becomes a lubricant bucket. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void grinderFillsBucket(GameTestHelper h) {
		GrinderBlockEntity gr = grinderRig(h);
		gr.setLubricant(1500);
		gr.getInventory().setStackInSlot(2, new ItemStack(Items.BUCKET));
		h.succeedWhen(() -> {
			h.assertTrue(gr.getInventory().getStackInSlot(2).is(RotaryItems.LUBRICANT_BUCKET.get()), "slot 2 "+gr.getInventory().getStackInSlot(2));
			h.assertTrue(gr.getLubricant() == 500, "tank "+gr.getLubricant());
		});
	}

	/** Canola on moist farmland grows through its ten stages; on dry farmland it pops off as a seed. */
	@GameTest(template = PLATFORM, timeoutTicks = 40)
	public static void canolaGrows(GameTestHelper h) {
		BlockPos soil = new BlockPos(4, 1, 4);
		h.setBlock(new BlockPos(5, 1, 4), Blocks.WATER);
		h.setBlock(soil, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
		BlockPos crop = soil.above();
		h.setBlock(crop, RotaryBlocks.CANOLA.get());
		ServerLevel level = h.getLevel();
		BlockPos abs = h.absolutePos(crop);
		h.assertTrue(CanolaBlock.canGrowAt(level, abs), "canola cannot grow here: light "+level.getMaxLocalRawBrightness(abs));
		for (int i = 0; i < 300 && h.getBlockState(crop).getValue(CanolaBlock.AGE) < CanolaBlock.GROWN; i++) {
			BlockState s = level.getBlockState(abs);
			s.randomTick(level, abs, level.random);
		}
		h.assertTrue(h.getBlockState(crop).getValue(CanolaBlock.AGE) == CanolaBlock.GROWN, "not grown: "+h.getBlockState(crop));
		BlockPos dry = new BlockPos(2, 1, 2);
		h.setBlock(dry, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
		h.setBlock(dry.above(), RotaryBlocks.CANOLA.get());
		h.runAfterDelay(2, () -> {
			h.assertBlockNotPresent(RotaryBlocks.CANOLA.get(), dry.above());
			h.assertItemEntityPresent(RotaryItems.CANOLA_SEEDS.get(), dry.above(), 1.5);
			h.succeed();
		});
	}

	/** Fully grown canola drops (1-2)*(2..14) seeds without fortune, a young plant one seed (BlockCanola.getDrops). */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void canolaDrops(GameTestHelper h) {
		int min = Integer.MAX_VALUE, max = 0;
		for (int i = 0; i < 500; i++) {
			int n = CanolaBlock.getDrops(CanolaBlock.GROWN, 0, h.getLevel().random).getCount();
			min = Math.min(min, n);
			max = Math.max(max, n);
		}
		h.assertTrue(min >= 2 && max <= 28 && max > min, "grown drops "+min+".."+max);
		h.assertTrue(CanolaBlock.getDrops(4, 0, h.getLevel().random).getCount() == 1, "young plant drop");
		h.succeed();
	}

	/**
	 * Short grass loot with the canola modifier: wheat seeds 1/8 of the time, of which 2/12 become canola seeds (original grass seed
	 * weights 10 : 2), i.e. about 1 in 48 breaks gives canola.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void grassDropsCanolaSeeds(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		BlockPos p = h.absolutePos(new BlockPos(4, 1, 4));
		BlockState grass = Blocks.SHORT_GRASS.defaultBlockState();
		int canola = 0, wheat = 0;
		for (int i = 0; i < 4800; i++) {
			for (ItemStack is : Block.getDrops(grass, level, p, null)) {
				if (is.is(RotaryItems.CANOLA_SEEDS.get()))
					canola += is.getCount();
				else if (is.is(Items.WHEAT_SEEDS))
					wheat += is.getCount();
			}
		}
		h.assertTrue(canola >= 50 && canola <= 160, "canola seeds from 4800 grass: "+canola+" (expected ~100)");
		h.assertTrue(wheat >= 400 && wheat <= 620, "wheat seeds from 4800 grass: "+wheat+" (expected ~500)");
		h.succeed();
	}
}
