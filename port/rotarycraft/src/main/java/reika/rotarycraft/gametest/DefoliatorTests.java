/*******************************************************************************
 * Tests for the ported RotaryCraft defoliator (original TileEntityDefoliator). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.DefoliatorBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.GearboxTypes;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DefoliatorTests {

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
	 * Gas engine -> steel 16:1 gearbox as speed-up (8192 rad/s @ 8 Nm = 65536 W) -> bevel gears up -> defoliator: range 8*log2(8) = 24,
	 * 2*sqrt(8192) = 180 random blocks a tick. A potion of poison becomes 1000 mB (the bottle goes below); a block of persistent oak leaves
	 * around it is eaten away, 1 mB per block.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 600, batch = "defoliator")
	public static void defoliatorKillsLeaves(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 1, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		GearboxBlockEntity gbx = place(h, RotaryBlocks.GEARBOX.get(), 2, 2, 4, 13, GearboxBlockEntity.class);
		gbx.applyComponentsFromItemStack(GearboxBlock.gearboxItem(GearboxTypes.STEEL, 16));
		gbx.setBlockMetadata(13);
		gbx.reduction = false;
		gbx.fillWithLubricant();
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 3, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		DefoliatorBlockEntity te = place(h, RotaryBlocks.DEFOLIATOR.get(), 3, 3, 4, 0, DefoliatorBlockEntity.class);
		te.getInventory().setStackInSlot(0, PotionContents.createItemStack(Items.POTION, Potions.POISON));
		int n = 0;
		for (int x = 4; x <= 7; x++)
			for (int y = 2; y <= 6; y++)
				for (int z = 2; z <= 6; z++) {
					h.setBlock(new BlockPos(x, y, z), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
					n++;
				}
		int total = n;
		h.succeedWhen(() -> {
			h.assertTrue(te.getRange() == 24 && te.getNumberPasses() == 180, "range "+te.getRange()+", passes "+te.getNumberPasses()+" at "+te.getOmega()+" rad/s @ "+te.getTorque()+" Nm");
			h.assertTrue(te.getInventory().getStackInSlot(1).is(Items.GLASS_BOTTLE), "no bottle");
			int left = 0;
			for (int x = 4; x <= 7; x++)
				for (int y = 2; y <= 6; y++)
					for (int z = 2; z <= 6; z++)
						if (h.getBlockState(new BlockPos(x, y, z)).is(Blocks.OAK_LEAVES))
							left++;
			h.assertTrue(total-left >= 10 && te.getPoison() == 1000-(total-left), (total-left)+" leaves gone, poison "+te.getPoison());
		});
	}

	/** Only plain poison potions are taken; stone and the like are not vegetation. */
	@GameTest(template = PLATFORM)
	public static void defoliatorKnowsItsTargets(GameTestHelper h) {
		h.assertTrue(DefoliatorBlockEntity.isPoisonPotion(PotionContents.createItemStack(Items.POTION, Potions.POISON)), "poison refused");
		h.assertFalse(DefoliatorBlockEntity.isPoisonPotion(PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS)), "swiftness taken");
		h.assertTrue(DefoliatorBlockEntity.isVegetation(Blocks.OAK_LOG.defaultBlockState()) && DefoliatorBlockEntity.isVegetation(Blocks.SHORT_GRASS.defaultBlockState())
				&& DefoliatorBlockEntity.isVegetation(Blocks.CACTUS.defaultBlockState()), "vegetation not recognised");
		h.assertFalse(DefoliatorBlockEntity.isVegetation(Blocks.STONE.defaultBlockState()) || DefoliatorBlockEntity.isVegetation(Blocks.WATER.defaultBlockState()), "stone or water killed");
		h.succeed();
	}
}
