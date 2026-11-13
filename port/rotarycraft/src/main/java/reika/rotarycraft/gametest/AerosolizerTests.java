/*******************************************************************************
 * Tests for the ported RotaryCraft aerosolizer (original TileEntityAerosolizer). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.AerosolizerBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AerosolizerTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	private static ItemStack potion(net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> p, int n) {
		ItemStack is = PotionContents.createItemStack(Items.POTION, p);
		is.setCount(n);
		return is;
	}

	/** Beneficial potions only; a long potion stores 3, a normal one 1. */
	@GameTest(template = PLATFORM)
	public static void aerosolizerReadsPotions(GameTestHelper h) {
		h.assertTrue(AerosolizerBlockEntity.getEffectFromItem(potion(Potions.SWIFTNESS, 1)).is(MobEffects.MOVEMENT_SPEED), "swiftness");
		h.assertTrue(AerosolizerBlockEntity.getAmount(potion(Potions.SWIFTNESS, 1)) == 1 && AerosolizerBlockEntity.getAmount(potion(Potions.LONG_SWIFTNESS, 1)) == 3, "amounts");
		h.assertTrue(AerosolizerBlockEntity.getEffectFromItem(potion(Potions.POISON, 1)) == null, "poison accepted");
		h.assertTrue(AerosolizerBlockEntity.getEffectFromItem(potion(Potions.WATER, 1)) == null, "water accepted");
		h.assertTrue(AerosolizerBlockEntity.getEffectFromItem(new ItemStack(Items.APPLE)) == null, "apple accepted");
		h.succeed();
	}

	/**
	 * Powered by a gas engine it stores the potions (bottles left behind) and gives the cow in its room Speed II (two stores with the
	 * same effect); a cow behind a stone wall gets nothing.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "aerosolizer")
	public static void aerosolizerFillsTheRoom(GameTestHelper h) {
		place(h, RotaryBlocks.GAS_ENGINE.get(), 5, 2, 4, 0, EngineBlockEntity.class).addFuel(50000);
		AerosolizerBlockEntity te = place(h, RotaryBlocks.AEROSOLIZER.get(), 4, 2, 4, 0, AerosolizerBlockEntity.class);
		te.getInventory().setStackInSlot(0, potion(Potions.SWIFTNESS, 2));
		te.getInventory().setStackInSlot(1, potion(Potions.LONG_SWIFTNESS, 1));
		for (int y = 2; y <= 7; y++)
			for (int z = 0; z <= 8; z++)
				h.setBlock(new BlockPos(1, y, z), Blocks.STONE);
		Cow in = h.spawn(EntityType.COW, new BlockPos(4, 2, 6));
		Cow out = h.spawn(EntityType.COW, new BlockPos(0, 2, 4));
		in.setNoAi(true);
		out.setNoAi(true);
		h.succeedWhen(() -> {
			h.assertTrue(te.getPotionLevel(0) == 2 && te.getPotionLevel(1) == 3 && te.getInventory().getStackInSlot(0).is(Items.GLASS_BOTTLE), "stores "+te.getPotionLevel(0)+" "+te.getPotionLevel(1));
			MobEffectInstance e = in.getEffect(MobEffects.MOVEMENT_SPEED);
			h.assertTrue(e != null && e.getAmplifier() == 1, "cow in the room has "+e);
			h.assertTrue(out.getEffect(MobEffects.MOVEMENT_SPEED) == null, "cow behind the wall has speed");
		});
	}
}
