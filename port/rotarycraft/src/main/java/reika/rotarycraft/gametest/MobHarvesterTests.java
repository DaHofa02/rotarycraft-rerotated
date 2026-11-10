/*******************************************************************************
 * Tests for the ported RotaryCraft mob harvester (original TileEntityMobHarvester). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.MobHarvesterBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class MobHarvesterTests {

	/** The stone floor is at y = 1; the rigs stand on it at y = 2. */
	private static final String PLATFORM = "platform_9x8x9";

	/** An engine at (5, 2, z) (meta 0: output west) into a harvester at (4, 2, z), set down by the player (its placer) or by nobody. */
	private static MobHarvesterBlockEntity rig(GameTestHelper h, SimPlayer placer, int z, boolean gas) {
		BlockPos pos = new BlockPos(4, 2, z);
		if (placer != null)
			placer.place(pos, RotaryBlocks.MOB_HARVESTER.get(), Direction.EAST);
		else
			h.setBlock(pos, RotaryBlocks.MOB_HARVESTER.get());
		BlockPos e = new BlockPos(5, 2, z);
		h.setBlock(e, gas ? RotaryBlocks.GAS_ENGINE.get() : RotaryBlocks.DC_ENGINE.get());
		EngineBlockEntity eng = (EngineBlockEntity)h.getBlockEntity(e);
		eng.setBlockMetadata(0);
		if (gas)
			eng.addFuel(50000);
		else
			h.setBlock(e.above(), Blocks.REDSTONE_BLOCK);
		return (MobHarvesterBlockEntity)h.getBlockEntity(pos);
	}

	private static SimPlayer onlinePlayer(GameTestHelper h) {
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		p.join();
		p.give(new net.minecraft.world.item.ItemStack(RotaryBlocks.MOB_HARVESTER.get(), 2));
		return p;
	}

	private static <T extends Mob> T mob(GameTestHelper h, EntityType<T> type, int z) {
		T m = h.spawn(type, new BlockPos(4, 3, z));
		m.setNoAi(true); //stands still in the column
		return m;
	}

	/** 65536 W: (int)log2((2 + 0.5 * 16)^6) = 19 damage per hit, column 2 high (one more per 524288 W). An iron golem loses 19 at a time. */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void harvesterDamageFollowsPower(GameTestHelper h) {
		SimPlayer p = onlinePlayer(h);
		MobHarvesterBlockEntity te = rig(h, p, 4, true);
		Mob[] golem = {null};
		h.succeedWhen(() -> {
			h.assertTrue(te.getPower() == 65536, "harvester at "+te.getPower()+" W");
			if (golem[0] == null) {
				golem[0] = mob(h, EntityType.IRON_GOLEM, 4);
				h.fail("golem set into the column");
			}
			AABB box = te.getBox();
			h.assertTrue(te.getDamage() == 19 && te.getHeight() == 2 && box.getYsize() == 2 && box.getXsize() == 1, "damage "+te.getDamage()+", height "+te.getHeight());
			float lost = 100-golem[0].getHealth();
			h.assertTrue(lost >= 19 && lost%19 == 0, "golem lost "+lost);
			h.assertTrue(te.laser, "laser off with a mob in the column");
			p.leave();
		});
	}

	/** The hits are player attacks by the placer: a cow dies and leaves experience. */
	@GameTest(template = PLATFORM, timeoutTicks = 600)
	public static void harvesterKillsInThePlacersName(GameTestHelper h) {
		SimPlayer p = onlinePlayer(h);
		MobHarvesterBlockEntity te = rig(h, p, 4, true);
		Mob[] cow = {null};
		h.succeedWhen(() -> {
			h.assertTrue(te.getPower() == 65536, "harvester at "+te.getPower()+" W");
			if (cow[0] == null) {
				cow[0] = mob(h, EntityType.COW, 4);
				h.fail("cow set into the column");
			}
			h.assertTrue(!cow[0].isAlive(), "cow alive with "+cow[0].getHealth());
			boolean xp = p.player.totalExperience > 0
					|| !h.getLevel().getEntitiesOfClass(ExperienceOrb.class, new AABB(h.absolutePos(new BlockPos(4, 3, 4))).inflate(8)).isEmpty();
			h.assertTrue(xp, "no experience from the kill");
			p.leave();
		});
	}

	/** Villagers are spared (and do not light the laser); without its placer online the harvester holds a mob but does not hurt it. */
	@GameTest(template = PLATFORM, timeoutTicks = 800)
	public static void harvesterSparesVillagersAndNeedsItsPlacer(GameTestHelper h) {
		SimPlayer p = onlinePlayer(h);
		MobHarvesterBlockEntity own = rig(h, p, 6, true);
		MobHarvesterBlockEntity nobodys = rig(h, null, 2, true);
		Mob[] mobs = {null, null};
		long[] at = {0};
		h.succeedWhen(() -> {
			h.assertTrue(own.getPower() == 65536 && nobodys.getPower() == 65536, "harvesters at "+own.getPower()+" W, "+nobodys.getPower()+" W");
			if (mobs[0] == null) {
				mobs[0] = mob(h, EntityType.VILLAGER, 6);
				mobs[1] = mob(h, EntityType.COW, 2);
				at[0] = h.getTick();
				h.fail("mobs set into the columns");
			}
			h.assertTrue(h.getTick()-at[0] >= 100, "waiting");
			h.assertTrue(mobs[0].getHealth() == mobs[0].getMaxHealth() && !own.laser, "villager at "+mobs[0].getHealth()+", laser "+own.laser);
			h.assertTrue(mobs[1].getHealth() == mobs[1].getMaxHealth() && nobodys.laser, "cow at "+mobs[1].getHealth()+", laser "+nobodys.laser);
			p.leave();
		});
	}

	/** A DC engine's 1024 W are below the 4096 W minimum: nothing happens. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void harvesterNeedsItsMinimumPower(GameTestHelper h) {
		SimPlayer p = onlinePlayer(h);
		MobHarvesterBlockEntity te = rig(h, p, 4, false);
		Mob cow = mob(h, EntityType.COW, 4);
		h.runAtTickTime(200, () -> {
			h.assertTrue(te.getPower() == 1024, "harvester at "+te.getPower()+" W");
			h.assertTrue(cow.getHealth() == cow.getMaxHealth() && !te.laser, "cow at "+cow.getHealth()+", laser "+te.laser);
			p.leave();
			h.succeed();
		});
	}
}
