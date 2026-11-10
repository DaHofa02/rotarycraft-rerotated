/*******************************************************************************
 * Tests for the ported RotaryCraft fan (original TileEntityFan). Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for
 * distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.FanBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class FanTests {

	private static final String PLATFORM = "platform_9x4x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** Moves the template's stone floor from y = 1 down to y = 0 and builds a wall at x = 8 that keeps blown mobs in the test. */
	private static void floor(GameTestHelper h) {
		for (int x = 0; x < 9; x++) {
			for (int z = 0; z < 9; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
				h.setBlock(new BlockPos(x, 1, z), Blocks.AIR);
			}
		}
		for (int z = 0; z < 9; z++)
			for (int y = 1; y <= 3; y++)
				h.setBlock(new BlockPos(8, y, z), Blocks.STONE);
	}

	/** Gas engine (512 rad/s @ 128 Nm = 65536 W, meta 1: output east) into a fan at (2, 1, 4) (meta 1: input west, blows east). */
	private static FanBlockEntity gasRig(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 1, 4, 1, EngineBlockEntity.class).addFuel(50000);
		return place(h, RotaryBlocks.FAN.get(), 2, 1, 4, 1, FanBlockEntity.class);
	}

	private static Cow cow(GameTestHelper h, int x, int z) {
		return h.spawn(EntityType.COW, new BlockPos(x, 1, z));
	}

	/** 65536 W: range 8 + (65536 - 1024)/1024 = 71; a cow in the beam is blown east with at least 1 block per tick. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void fanBlowsMobsAlongTheBeam(GameTestHelper h) {
		floor(h);
		FanBlockEntity fan = gasRig(h);
		Cow c = cow(h, 3, 4);
		double x0 = c.getX();
		h.succeedWhen(() -> {
			h.assertTrue(fan.getPower() == 65536 && fan.getRange() == 71, "fan "+fan.getPower()+" W, range "+fan.getRange());
			h.assertTrue(c.getX()-x0 > 3, "cow moved only "+(c.getX()-x0));
		});
	}

	/** A DC engine gives exactly the minimum 1024 W: range 8. A stone block three blocks ahead stops the air; an armor stand behind it stays. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void fanIsStoppedBySolidBlocks(GameTestHelper h) {
		floor(h);
		place(h, RotaryBlocks.DC_ENGINE.get(), 1, 1, 4, 1, EngineBlockEntity.class);
		h.setBlock(new BlockPos(1, 1, 3), Blocks.REDSTONE_BLOCK);
		FanBlockEntity fan = place(h, RotaryBlocks.FAN.get(), 2, 1, 4, 1, FanBlockEntity.class);
		h.setBlock(new BlockPos(5, 1, 4), Blocks.STONE);
		Entity c = h.spawn(EntityType.ARMOR_STAND, new BlockPos(7, 1, 4)); //stands still on its own (a cow would wander)
		double x0 = c.getX();
		h.runAtTickTime(150, () -> {
			h.assertTrue(fan.getPower() == 1024 && fan.getRange() == 8, "fan "+fan.getPower()+" W, range "+fan.getRange());
			h.assertTrue(Math.abs(c.getX()-x0) < 0.1, "armor stand behind the stone moved "+(c.getX()-x0));
			h.succeed();
		});
	}

	/**
	 * 512 rad/s: ripe wheat in the beam and beside it is harvested (1 in 600 - 25*log2(512) = 375 per tick and block) and replanted;
	 * the drops lose one seed; a cobweb one block above the beam is blown away (from 256 rad/s); unripe wheat stays.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 4000)
	public static void fanHarvestsRipeCrops(GameTestHelper h) {
		floor(h);
		FanBlockEntity fan = gasRig(h);
		BlockPos[] ripe = {new BlockPos(4, 1, 4), new BlockPos(5, 1, 3), new BlockPos(6, 1, 5)};
		for (BlockPos p : ripe) {
			h.setBlock(p.below(), Blocks.FARMLAND);
			h.setBlock(p, ((CropBlock)Blocks.WHEAT).getStateForAge(7));
		}
		BlockPos young = new BlockPos(7, 1, 4);
		h.setBlock(young.below(), Blocks.FARMLAND);
		h.setBlock(young, ((CropBlock)Blocks.WHEAT).getStateForAge(0));
		BlockPos web = new BlockPos(5, 2, 4);
		h.setBlock(web, Blocks.COBWEB);
		h.succeedWhen(() -> {
			h.assertTrue(fan.getHarvestingRand() == 375, "harvest chance 1 in "+fan.getHarvestingRand());
			for (BlockPos p : ripe) {
				h.assertBlockPresent(Blocks.WHEAT, p);
				h.assertTrue(h.getBlockState(p).getValue(CropBlock.AGE) < 7, "not harvested at "+p); //replanted at 0 (may grow again)
			}
			h.assertBlockPresent(Blocks.WHEAT, young);
			h.assertBlockNotPresent(Blocks.COBWEB, web);
			int wheat = 0;
			for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(40)))
				if (e.getItem().is(Items.WHEAT))
					wheat += e.getItem().getCount();
			h.assertTrue(wheat >= 3, "only "+wheat+" wheat dropped");
		});
	}

	/** With the diffuser the fan blows wide: range 8 + 64512/2048 = 39, and a cow one block beside the beam is blown too. */
	@GameTest(template = PLATFORM, timeoutTicks = 200)
	public static void fanDiffuserMakesItWide(GameTestHelper h) {
		floor(h);
		FanBlockEntity fan = gasRig(h);
		ItemStack diffuser = new ItemStack(RotaryItems.part(CraftingPart.DIFFUSER));
		h.assertTrue(fan.canUpgradeWith(diffuser), "fan takes no diffuser");
		fan.upgrade(diffuser);
		h.assertFalse(fan.canUpgradeWith(diffuser), "fan takes a second diffuser");
		Cow c = cow(h, 3, 5);
		double x0 = c.getX();
		h.succeedWhen(() -> {
			h.assertTrue(fan.getPower() == 65536 && fan.getRange() == 39, "fan "+fan.getPower()+" W, range "+fan.getRange());
			h.assertTrue(c.getX()-x0 > 3, "cow beside the beam moved only "+(c.getX()-x0));
		});
	}
}
