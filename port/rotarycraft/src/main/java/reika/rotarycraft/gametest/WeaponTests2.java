/*******************************************************************************
 * Tests for the port's self-destruct, sonic weapon, EMP, machine gun, TNT cannon and block cannon. Private NeoForge 1.21.1 port of
 * RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BlockCannonBlockEntity;
import reika.rotarycraft.content.blockentity.EmpBlockEntity;
import reika.rotarycraft.content.blockentity.MachineGunBlockEntity;
import reika.rotarycraft.content.blockentity.MagneticEngineBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.SelfDestructBlockEntity;
import reika.rotarycraft.content.blockentity.SonicWeaponBlockEntity;
import reika.rotarycraft.content.blockentity.TntCannonBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class WeaponTests2 {

	private static final String PLATFORM = "platform_9x8x9";

	private static <T extends BlockEntity> T place(GameTestHelper h, Block block, int x, int y, int z, int meta, Class<T> cls) {
		BlockPos p = new BlockPos(x, y, z);
		h.setBlock(p, block);
		BlockEntity be = h.getBlockEntity(p);
		if (be instanceof RotaryCraftBlockEntity r)
			r.setBlockMetadata(meta);
		return cls.cast(be);
	}

	/** A magnetostatic engine at (x, 2, 4) writing east at that tier and speed step, fed with FE every tick. */
	static MagneticEngineBlockEntity fedEngine(GameTestHelper h, int x, int tier, int step) {
		MagneticEngineBlockEntity e = place(h, RotaryBlocks.MAGNETIC_ENGINE.get(), x, 2, 4, 1, MagneticEngineBlockEntity.class);
		e.setTier(tier);
		e.setSpeedStep(step);
		h.onEachTick(() -> e.getEnergyStorage(null).receiveEnergy(Integer.MAX_VALUE, false));
		return e;
	}

	/** Gas engine -> self-destruct (armed by the power), a second one beside it: the engine removed, both count down 32 small blasts and finish. */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "weapons")
	public static void selfDestructIsADeadMansSwitch(GameTestHelper h) {
		SelfDestructBlockEntity.allowExplosion = false;
		place(h, RotaryBlocks.GAS_ENGINE.get(), 2, 2, 4, 1, EngineBlockEntity.class).addFuel(50000);
		SelfDestructBlockEntity a = place(h, RotaryBlocks.SELF_DESTRUCT.get(), 3, 2, 4, 0, SelfDestructBlockEntity.class);
		SelfDestructBlockEntity b = place(h, RotaryBlocks.SELF_DESTRUCT.get(), 3, 2, 5, 0, SelfDestructBlockEntity.class);
		int[] stage = {0};
		h.succeedWhen(() -> {
			if (stage[0] == 0) {
				h.assertTrue(a.isArmed() && !a.isCountingDown(), "not armed: "+a.getPower()+" W");
				h.setBlock(new BlockPos(2, 2, 4), Blocks.AIR);
				stage[0] = 1;
				h.fail("power cut");
			}
			h.assertTrue(a.finished && a.smallBlasts == SelfDestructBlockEntity.COUNTDOWN, "a: "+a.smallBlasts+" blasts, finished "+a.finished);
			h.assertTrue(b.finished, "neighbour did not go off");
			SelfDestructBlockEntity.allowExplosion = true;
		});
	}

	/**
	 * Magnetostatic engine (tier 5, 64 rad/s: 8192 Nm, 524288 W) -> bevel gears -> sonic weapon: volume 381.47*8192 = 3125000; a zombie
	 * 1.12 blocks from it gets 3125000/1.25 = 2500000 - blindness, but not yet damage (2971000).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "weapons")
	public static void sonicWeaponBlindsNearbyMobs(GameTestHelper h) {
		SonicWeaponBlockEntity.allowBlockDamage = false;
		fedEngine(h, 2, 5, 6);
		FluidMachineTests.bevelUp(h, 3);
		SonicWeaponBlockEntity s = place(h, RotaryBlocks.SONIC_WEAPON.get(), 3, 3, 4, 0, SonicWeaponBlockEntity.class);
		var zb = h.spawn(EntityType.HUSK, new Vec3(4.5, 3, 4.5));
		zb.setNoAi(true);
		zb.setNoGravity(true);
		float hp = zb.getHealth();
		h.succeedWhen(() -> {
			h.assertTrue(s.getVolume() == SonicWeaponBlockEntity.VOLUME_PER_TORQUE*8192 && s.pulses > 0, "volume "+s.getVolume()+" at "+s.getTorque()+" Nm");
			h.assertTrue(zb.hasEffect(MobEffects.BLINDNESS) && zb.getHealth() == hp, "zombie blind "+zb.hasEffect(MobEffects.BLINDNESS)+", health "+zb.getHealth()+", effects "+zb.getActiveEffects()+", dist "+zb.position().distanceTo(Vec3.atCenterOf(s.getBlockPos()))+", there "+s.volumeAt(zb.position().distanceTo(Vec3.atCenterOf(s.getBlockPos())))+", pulses "+s.pulses);
			zb.discard();
			h.setBlock(new BlockPos(3, 3, 4), Blocks.AIR);
			SonicWeaponBlockEntity.allowBlockDamage = true;
		});
	}

	/** EMP charged to 4.184 GJ (by hand: no 268 MW source here): it fires once and shuts down the gas engine beside it. Own batch (64-block reach). */
	@GameTest(template = PLATFORM, timeoutTicks = 100, batch = "emp")
	public static void empShutsDownMachines(GameTestHelper h) {
		EngineBlockEntity eng = place(h, RotaryBlocks.GAS_ENGINE.get(), 2, 2, 4, 1, EngineBlockEntity.class);
		eng.addFuel(50000);
		EmpBlockEntity emp = place(h, RotaryBlocks.EMP.get(), 6, 2, 4, 0, EmpBlockEntity.class);
		h.assertTrue(emp.MINPOWER == 268435456L, "minimum power "+emp.MINPOWER);
		emp.addEnergy(EmpBlockEntity.BLAST_ENERGY);
		h.succeedWhen(() -> {
			h.assertTrue(emp.hasFired() && emp.disabled >= 1, "fired "+emp.hasFired()+", disabled "+emp.disabled);
			h.assertTrue(eng.isShutdown(), "engine still running");
			h.setBlock(new BlockPos(6, 2, 4), Blocks.AIR);
		});
	}

	/** Magnetostatic engine (tier 3, 2048 rad/s: 512 Nm) -> machine gun facing east with arrows: it shoots the zombie in front. */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "weapons")
	public static void machineGunShootsTargets(GameTestHelper h) {
		fedEngine(h, 0, 3, 11);
		MachineGunBlockEntity g = place(h, RotaryBlocks.MACHINE_GUN.get(), 1, 2, 4, 1, MachineGunBlockEntity.class);
		g.getInventory().setStackInSlot(0, new ItemStack(Items.ARROW, 64));
		Zombie zb = h.spawn(EntityType.ZOMBIE, new Vec3(6.5, 2, 4.5));
		zb.setNoAi(true);
		float hp = zb.getHealth();
		h.succeedWhen(() -> {
			h.assertTrue(g.getPower() >= 65536 && g.getOmega() >= 1024, "machine gun "+g.getOmega()+" rad/s "+g.getPower()+" W");
			h.assertTrue(g.shots > 0 && zb.getHealth() < hp, "shots "+g.shots+", zombie "+zb.getHealth());
			zb.discard();
		});
	}

	/** Fuel engine -> bevel gears -> TNT cannon aimed 6 blocks east: it launches lit TNT that comes down near the target (removed before it blows). */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "weapons")
	public static void tntCannonLobsAtTheTarget(GameTestHelper h) {
		FluidMachineTests.fuelEngine(h, 0);
		FluidMachineTests.bevelUp(h, 1);
		TntCannonBlockEntity c = place(h, RotaryBlocks.TNT_CANNON.get(), 1, 3, 4, 0, TntCannonBlockEntity.class);
		c.getInventory().setStackInSlot(0, new ItemStack(Items.TNT, 1));
		BlockPos target = h.absolutePos(new BlockPos(7, 2, 4));
		c.setTarget(target);
		h.succeedWhen(() -> {
			h.assertTrue(c.aim() != null, "out of reach at "+c.getPower()+" W, max "+c.getMaxSpeed());
			var tnt = h.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(16));
			h.assertTrue(c.shots == 1 && !tnt.isEmpty(), "shots "+c.shots);
			PrimedTnt t = tnt.get(0);
			h.assertTrue(t.onGround(), "still flying");
			Vec3 at = t.position();
			for (PrimedTnt x : tnt)
				x.discard();
			h.assertTrue(Math.abs(at.x-(target.getX()+0.5)) < 1.5 && Math.abs(at.z-(target.getZ()+0.5)) < 1.5, "landed at "+at+", target "+target);
		});
	}

	/** Fuel engine -> bevel gears -> block cannon aimed 6 blocks east: the cobblestone it shoots lands as a block at the target. */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "weapons")
	public static void blockCannonPlacesBlocksAtTheTarget(GameTestHelper h) {
		FluidMachineTests.fuelEngine(h, 0);
		FluidMachineTests.bevelUp(h, 1);
		BlockCannonBlockEntity c = place(h, RotaryBlocks.BLOCK_CANNON.get(), 1, 3, 4, 0, BlockCannonBlockEntity.class);
		c.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 1));
		h.assertTrue(c.getRequiredTorque(new ItemStack(Items.COBBLESTONE)) == 48 && !c.isAmmo(new ItemStack(Items.BEDROCK)), "torque rule");
		c.setTarget(h.absolutePos(new BlockPos(7, 2, 4)));
		h.succeedWhen(() -> {
			h.assertTrue(c.shots == 1, "shots "+c.shots+" at "+c.getTorque()+" Nm");
			boolean found = false;
			for (BlockPos p : BlockPos.betweenClosed(new BlockPos(6, 2, 3), new BlockPos(8, 2, 5)))
				found |= h.getBlockState(p).is(Blocks.COBBLESTONE);
			h.assertTrue(found, "no cobblestone landed at the target");
		});
	}
}
