/*******************************************************************************
 * Tests for the ported RotaryCraft mob radar (original TileEntityMobRadar). Private NeoForge 1.21.1 port of RotaryCraft by Reika
 * Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.api.interfaces.RadarJammer;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.MobRadarBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.menu.MobRadarMenu;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class MobRadarTests {

	private static final String PLATFORM = "platform_9x8x9";
	private static final BlockPos RADAR = new BlockPos(4, 3, 4);

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** An engine at (3, 2, 4) (meta 1: output east) -> bevel gears (west -> up) -> radar at (4, 3, 4). */
	private static MobRadarBlockEntity rig(GameTestHelper h, boolean gas) {
		if (gas)
			place(h, RotaryBlocks.GAS_ENGINE.get(), 3, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		else {
			place(h, RotaryBlocks.DC_ENGINE.get(), 3, 2, 4, 1, RotaryCraftBlockEntity.class);
			h.setBlock(new BlockPos(3, 2, 3), Blocks.REDSTONE_BLOCK);
		}
		BevelGearBlockEntity bev = place(h, RotaryBlocks.BEVEL_GEARS.get(), 4, 2, 4, 0, BevelGearBlockEntity.class);
		bev.direction = BevelGearBlockEntity.getDirectionMap().indexOf(new BevelGearBlockEntity.IOPair(Direction.WEST, Direction.UP));
		return place(h, RotaryBlocks.MOB_RADAR.get(), RADAR.getX(), RADAR.getY(), RADAR.getZ(), 0, MobRadarBlockEntity.class);
	}

	private static boolean lists(MobRadarBlockEntity te, Entity e) {
		return te.getEntities().contains(e);
	}

	/**
	 * Gas engine, 65536 W: range 8 + (65536 - 8192)/1024 = 64. The radar lists animals, monsters and players, each kind switched by
	 * its own flag, and nothing else that lives (an armor stand).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void radarListsAnimalsMonstersAndPlayers(GameTestHelper h) {
		MobRadarBlockEntity te = rig(h, true);
		Entity cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(6, 2, 4));
		Entity creeper = h.spawnWithNoFreeWill(EntityType.CREEPER, new BlockPos(6, 2, 6));
		Entity stand = h.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 2, 6));
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		p.join();
		h.assertTrue(new MobRadarMenu(0, p.player.getInventory(), te).stillValid(p.player), "radar GUI not usable");
		int[] stage = {0};
		h.succeedWhen(() -> {
			h.assertTrue(te.power == 65536 && te.getRange() == 64, "radar at "+te.power+" W, range "+te.getRange());
			if (stage[0] == 0) {
				h.assertTrue(lists(te, cow) && lists(te, creeper) && lists(te, p.player), "radar lists "+te.getEntities());
				h.assertFalse(lists(te, stand), "radar lists an armor stand");
				te.animal = false;
				stage[0] = 1;
				h.fail("animals switched off");
			}
			if (stage[0] == 1) {
				h.assertTrue(te.getEntities().stream().noneMatch(e -> e instanceof Animal), "animals still listed");
				h.assertTrue(lists(te, creeper) && lists(te, p.player), "radar lists "+te.getEntities());
				te.hostile = false;
				stage[0] = 2;
				h.fail("monsters switched off");
			}
			if (stage[0] == 2) {
				h.assertTrue(te.getEntities().stream().noneMatch(e -> e instanceof Enemy), "monsters still listed");
				h.assertTrue(lists(te, p.player), "radar lists "+te.getEntities());
				te.player = false;
				stage[0] = 3;
				h.fail("players switched off");
			}
			h.assertTrue(te.getEntities().stream().noneMatch(e -> e instanceof Player), "players still listed");
			p.leave();
		});
	}

	/**
	 * The original's range has no lower bound at the minimum power of 8192 W: a DC engine's 1024 W give 8 + (1024 - 8192)/1024 = 1.
	 * A cow next to the radar is listed, one three blocks away is not; without power the range is 8 - 8 = 0.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void radarRangeShrinksBelowItsMinimumPower(GameTestHelper h) {
		MobRadarBlockEntity te = rig(h, false);
		h.assertTrue(te.MINPOWER == 8192 && te.getMaxRange() == 256, "minimum power "+te.MINPOWER);
		h.assertTrue(te.getRange() == 0, "range "+te.getRange()+" without power");
		Entity near = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(5, 2, 5));
		Entity far = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(7, 2, 4));
		h.succeedWhen(() -> {
			h.assertTrue(te.power == 1024 && te.getRange() == 1, "radar at "+te.power+" W, range "+te.getRange());
			h.assertTrue(lists(te, near), "cow beside the radar not listed");
			h.assertFalse(lists(te, far), "cow three blocks away listed");
		});
	}

	/** A cow that jams radars, as an entity of another mod would. */
	private static class JammerCow extends Cow implements RadarJammer {

		private boolean jamming = true;

		private JammerCow(Level world) {
			super(EntityType.COW, world);
		}

		@Override
		public boolean jamRadar(Level world, BlockPos radar) {
			return jamming;
		}
	}

	/** A RadarJammer in range blanks the radar for as long as it jams. */
	@GameTest(template = PLATFORM, timeoutTicks = 300)
	public static void radarIsJammedByAJammer(GameTestHelper h) {
		MobRadarBlockEntity te = rig(h, true);
		JammerCow cow = new JammerCow(h.getLevel());
		Vec3 at = h.absoluteVec(new Vec3(6.5, 2, 4.5));
		cow.setPos(at.x, at.y, at.z);
		cow.setNoAi(true);
		h.getLevel().addFreshEntity(cow);
		h.succeedWhen(() -> {
			h.assertTrue(te.getRange() == 64, "radar range "+te.getRange());
			if (cow.jamming) {
				h.assertTrue(te.isJammed(), "radar not jammed");
				cow.jamming = false;
				h.fail("jammer switched off");
			}
			h.assertFalse(te.isJammed(), "radar still jammed");
			h.assertTrue(lists(te, cow), "cow not listed");
			cow.discard();
		});
	}
}
