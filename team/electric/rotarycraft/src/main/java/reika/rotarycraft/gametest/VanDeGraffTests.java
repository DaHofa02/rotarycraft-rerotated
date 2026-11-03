/*******************************************************************************
 * Tests for the ported RotaryCraft Van de Graaff generator (original TileEntityVanDeGraff). Private NeoForge 1.21.1 port of RotaryCraft by
 * Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.VanDeGraffBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.entity.DischargeEntity;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryEntities;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class VanDeGraffTests {

	private static final String PLATFORM = "platform_9x4x9";

	/** DC engine (1024 W) -> bevel gears up -> VDG: it gains 4*sqrt(1024) = 128 C per tick. */
	@GameTest(template = PLATFORM, timeoutTicks = 60)
	public static void vdgChargesFromShaftPower(GameTestHelper h) {
		h.setBlock(new BlockPos(5, 1, 4), RotaryBlocks.DC_ENGINE.get());
		((EngineBlockEntity)h.getBlockEntity(new BlockPos(5, 1, 4))).setBlockMetadata(0);
		h.setBlock(new BlockPos(5, 1, 5), Blocks.REDSTONE_BLOCK);
		h.setBlock(new BlockPos(4, 1, 4), RotaryBlocks.BEVEL_GEARS.get());
		BevelGearBlockEntity bev = (BevelGearBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.EAST, Direction.UP));
		h.setBlock(new BlockPos(4, 2, 4), RotaryBlocks.VAN_DE_GRAFF.get());
		VanDeGraffBlockEntity vdg = (VanDeGraffBlockEntity)h.getBlockEntity(new BlockPos(4, 2, 4));
		int[] last = {-1};
		h.succeedWhen(() -> {
			int c = vdg.getCharge();
			h.assertTrue(vdg.getPower() == 1024 && c > 0, "VDG: "+vdg.getPower()+" W, "+c+" C");
			if (last[0] < 0 || c-last[0] != 128) {
				last[0] = c;
				h.fail("not yet two readings 128 C apart: "+c);
			}
			h.assertTrue(vdg.getRange() == Math.min(c/1024, 16), "range "+vdg.getRange());
		});
	}

	/**
	 * Charged to 20000 C (range 16) with an iron golem 3 blocks away: it strikes it for 1 + 20000^2/2^25 = 12 damage, shows a discharge bolt and
	 * loses its charge. An adjacent block of TNT (weight 100 against 1) is set off instead when it is there.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 40, batch = "vdg")
	public static void vdgStrikesLivingAndTnt(GameTestHelper h) {
		h.setBlock(new BlockPos(2, 1, 4), RotaryBlocks.VAN_DE_GRAFF.get());
		VanDeGraffBlockEntity vdg = (VanDeGraffBlockEntity)h.getBlockEntity(new BlockPos(2, 1, 4));
		IronGolem pig = h.spawn(EntityType.IRON_GOLEM, new BlockPos(5, 1, 4));
		pig.setNoAi(true);
		float hp = pig.getHealth();
		vdg.setCharge(20000);
		h.runAtTickTime(3, () -> {
			h.assertTrue(vdg.getCharge() < 20000 && pig.getHealth() <= hp-12+0.01F, "golem "+pig.getHealth()+"/"+hp+", charge "+vdg.getCharge());
			h.setBlock(new BlockPos(2, 1, 3), Blocks.TNT);
			vdg.setCharge(20000);
		});
		h.runAtTickTime(25, () -> {
			h.assertTrue(h.getBlockState(new BlockPos(2, 1, 3)).isAir(), "TNT not set off (weights 100 to 1)");
			var tnt = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(2, 1, 3))).inflate(4));
			h.assertTrue(!tnt.isEmpty(), "no primed TNT, block "+h.getBlockState(new BlockPos(2, 1, 3))+", charge "+vdg.getCharge());
			h.killAllEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class); //would blow up the neighbouring tests later
			h.succeed();
		});
	}
}
