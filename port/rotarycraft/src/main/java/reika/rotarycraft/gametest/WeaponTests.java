/*******************************************************************************
 * Tests for the port's force field, containment field, air gun and land mine. Private NeoForge 1.21.1 port of RotaryCraft; not for
 * distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.AirGunBlockEntity;
import reika.rotarycraft.content.blockentity.ContainmentBlockEntity;
import reika.rotarycraft.content.blockentity.ForceFieldBlockEntity;
import reika.rotarycraft.content.blockentity.LandmineBlockEntity;
import reika.rotarycraft.content.blockentity.MagneticEngineBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WeaponTests {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	private static Zombie zombie(GameTestHelper h, double x, double y, double z) {
		Zombie zb = h.spawn(EntityType.ZOMBIE, new Vec3(x, y, z));
		zb.setNoAi(true);
		return zb;
	}

	/**
	 * Fuel engine (524288 W) -> bevel gears -> force field: range 2 + 0 = 2; a zombie inside is pushed out, an arrow nobody shot is removed;
	 * a block above it switches it off.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "weapons")
	public static void forceFieldKeepsMobsAndShotsOut(GameTestHelper h) {
		FluidMachineTests.fuelEngine(h, 2);
		FluidMachineTests.bevelUp(h, 3);
		ForceFieldBlockEntity ff = place(h, RotaryBlocks.FORCE_FIELD.get(), 3, 3, 4, 0, ForceFieldBlockEntity.class);
		int[] stage = {0};
		Zombie[] zb = {null};
		Arrow[] arrow = {null};
		h.succeedWhen(() -> {
			switch (stage[0]) {
				case 0 -> {
					h.assertTrue(ff.getRange() == 2, "range "+ff.getRange()+" at "+ff.getPower()+" W");
					zb[0] = zombie(h, 3.5, 3, 3.5);
					arrow[0] = h.spawn(EntityType.ARROW, new Vec3(4.5, 4.5, 4.5));
					arrow[0].setNoGravity(true);
					stage[0] = 1;
					h.fail("spawned");
				}
				case 1 -> {
					h.assertTrue(ff.repelled > 0 && zb[0].getDeltaMovement().horizontalDistance() > 0.3, "zombie not pushed: "+zb[0].getDeltaMovement());
					h.assertTrue(ff.stopped > 0 && arrow[0].isRemoved(), "arrow not stopped");
					zb[0].discard();
					h.setBlock(new BlockPos(3, 5, 4), Blocks.STONE);
					stage[0] = 2;
					h.fail("covered");
				}
				default -> {
					h.assertTrue(ff.getRange() == 0, "still up under a block: "+ff.getRange());
					h.setBlock(new BlockPos(3, 3, 4), Blocks.AIR);
				}
			}
		});
	}

	/**
	 * Magnetostatic engine (tier 2, 1024 rad/s: 131072 W) -> bevel gears -> containment field: range 2; a zombie 2.3 blocks from the middle is
	 * pulled back in.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "weapons")
	public static void containmentPullsMobsBack(GameTestHelper h) {
		MagneticEngineBlockEntity e = place(h, RotaryBlocks.MAGNETIC_ENGINE.get(), 2, 2, 4, 1, MagneticEngineBlockEntity.class);
		e.setTier(2);
		e.setSpeedStep(10);
		h.onEachTick(() -> e.getEnergyStorage(null).receiveEnergy(4000, false));
		FluidMachineTests.bevelUp(h, 3);
		ContainmentBlockEntity c = place(h, RotaryBlocks.CONTAINMENT.get(), 3, 3, 4, 0, ContainmentBlockEntity.class);
		int[] stage = {0};
		Zombie[] zb = {null};
		h.succeedWhen(() -> {
			if (stage[0] == 0) {
				h.assertTrue(c.getRange() == 2, "range "+c.getRange()+" at "+c.getPower()+" W");
				zb[0] = zombie(h, 3.5+2.3, 3, 4.5);
				zb[0].setNoGravity(true);
				stage[0] = 1;
				h.fail("spawned");
			}
			h.assertTrue(c.pulled > 0 && zb[0].getDeltaMovement().x < -0.2, "zombie not pulled: "+zb[0].getDeltaMovement()+", pulled "+c.pulled);
			zb[0].discard();
			h.setBlock(new BlockPos(3, 3, 4), Blocks.AIR);
		});
	}

	/** Fuel engine (2048 Nm) -> air gun facing east: range 10 + 2*11 = 32; an armor stand on the ground in front is blasted east at 11/4 blocks a tick. */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "weapons")
	public static void airGunBlastsMobsAway(GameTestHelper h) {
		FluidMachineTests.fuelEngine(h, 0);
		AirGunBlockEntity g = place(h, RotaryBlocks.AIR_GUN.get(), 1, 2, 4, 1, AirGunBlockEntity.class);
		for (int x = 2; x <= 8; x++)
			for (int z = 3; z <= 5; z++)
				if (z != 4 || x == 8)
					h.setBlock(new BlockPos(x, 3, z), Blocks.GLASS);
		var zb = h.spawn(EntityType.ARMOR_STAND, new Vec3(3.5, 2, 4.5)); //a living thing that keeps still (AI-less mobs never touch the ground)
		h.succeedWhen(() -> {
			h.assertTrue(g.getFacing() == Direction.EAST && g.getRange() == 32, "facing "+g.getFacing()+", range "+g.getRange());
			h.assertTrue(g.blasts > 0 && zb.getX() > h.absoluteVec(new Vec3(5, 0, 0)).x, "blasts "+g.blasts+", target at "+zb.position());
			zb.discard();
		});
	}

	/**
	 * Land mine with 2 gunpowder (blast 4) and TNT, next to a second mine with gunpowder: an armor stand on it sets it off and the
	 * neighbour follows by chain detonation (real explosions switched off).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "weapons")
	public static void landmineGoesOffUnderfoot(GameTestHelper h) {
		LandmineBlockEntity.allowExplosion = false;
		LandmineBlockEntity a = place(h, RotaryBlocks.LANDMINE.get(), 4, 1, 4, 0, LandmineBlockEntity.class);
		LandmineBlockEntity b = place(h, RotaryBlocks.LANDMINE.get(), 5, 1, 4, 0, LandmineBlockEntity.class);
		a.getInventory().setStackInSlot(0, new ItemStack(Items.GUNPOWDER));
		a.getInventory().setStackInSlot(1, new ItemStack(Items.GUNPOWDER));
		a.getInventory().setStackInSlot(4, new ItemStack(Items.TNT));
		b.getInventory().setStackInSlot(0, new ItemStack(Items.GUNPOWDER));
		h.assertTrue(a.getExplosionPower() == 4 && !a.getInventory().isItemValid(0, new ItemStack(Items.STONE)), "blast "+a.getExplosionPower());
		var zb = h.spawn(EntityType.ARMOR_STAND, new Vec3(4.5, 2, 4.5));
		h.succeedWhen(() -> {
			h.assertTrue(a.detonations == 1 && a.lastPower == 4, "mine "+a.detonations+" at "+a.lastPower+", stand on ground "+zb.onGround());
			h.assertTrue(b.detonations == 1, "no chain detonation");
			zb.discard();
			LandmineBlockEntity.allowExplosion = true;
		});
	}
}
