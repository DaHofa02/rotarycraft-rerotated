/*******************************************************************************
 * Tests for the port's turrets. Each runs in its own batch: their ranges (up to 256 blocks) reach other tests' mobs. Private NeoForge 1.21.1
 * port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.turret.FlameTurretBlockEntity;
import reika.rotarycraft.content.blockentity.turret.TurretBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class TurretTests {

	private static final String PLATFORM = "platform_9x8x9";

	/** Magnetostatic engine (tier, step) or a fuel engine (tier < 0) at (2, 2, 4) -> bevel gears -> the turret at (3, 3, 4). */
	private static <T extends TurretBlockEntity> T rig(GameTestHelper h, Block turret, Class<T> cls, int tier, int step) {
		if (tier < 0)
			FluidMachineTests.fuelEngine(h, 2);
		else
			WeaponTests2.fedEngine(h, 2, tier, step);
		FluidMachineTests.bevelUp(h, 3);
		h.setBlock(new BlockPos(3, 3, 4), turret);
		T te = cls.cast(h.getBlockEntity(new BlockPos(3, 3, 4)));
		((RotaryCraftBlockEntity)te).setBlockMetadata(0);
		return te;
	}

	private static <M extends Mob> M target(GameTestHelper h, EntityType<M> type, double x, double y, double z) {
		M m = h.spawn(type, new Vec3(x, y, z));
		m.setNoAi(true);
		m.setNoGravity(true);
		return m;
	}

	private static void done(GameTestHelper h, Mob m) {
		m.discard();
		h.setBlock(new BlockPos(3, 3, 4), Blocks.AIR);
	}

	/** Railgun (tier-5 engine at 512 rad/s: 4194304 W) with iron slugs: 10 damage a shot. */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "turret_railgun")
	public static void railgunShoots(GameTestHelper h) {
		TurretBlockEntity t = rig(h, RotaryBlocks.RAILGUN.get(), TurretBlockEntity.class, 5, 9);
		t.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
		var zb = target(h, EntityType.HUSK, 7.5, 3, 4.5);
		h.succeedWhen(() -> {
			h.assertTrue(t.shots > 0 && t.getInventory().getStackInSlot(0).getCount() < 4 && zb.getHealth() <= 10, "railgun "+t.getPower()+" W, shots "+t.shots+", zombie "+zb.getHealth());
			done(h, zb);
		});
	}

	/** Freeze gun (fuel engine: 2048 Nm, 524288 W) with a snow block: it becomes snowballs, and the zombie is frozen. */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "turret_freeze")
	public static void freezeGunFreezes(GameTestHelper h) {
		TurretBlockEntity t = rig(h, RotaryBlocks.FREEZE_GUN.get(), TurretBlockEntity.class, -1, 0);
		t.getInventory().setStackInSlot(0, new ItemStack(Items.SNOW_BLOCK));
		var zb = target(h, EntityType.HUSK, 7.5, 3, 4.5);
		h.succeedWhen(() -> {
			h.assertTrue(t.shots > 0 && zb.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "freeze gun "+t.getPower()+" W, shots "+t.shots+", inv "+t.getInventory().getStackInSlot(0));
			done(h, zb);
		});
	}

	/** Laser gun (tier-5 engine at 1024 rad/s: 8388608 W), no ammunition: the zombie burns. */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "turret_laser")
	public static void laserGunBurns(GameTestHelper h) {
		TurretBlockEntity t = rig(h, RotaryBlocks.LASER_GUN.get(), TurretBlockEntity.class, 5, 10);
		var zb = target(h, EntityType.HUSK, 7.5, 3, 4.5);
		float hp = zb.getHealth();
		h.succeedWhen(() -> {
			h.assertTrue(t.shots > 0 && zb.isOnFire() && zb.getHealth() < hp, "laser "+t.getPower()+" W, shots "+t.shots);
			done(h, zb);
		});
	}

	/** Multi-cannon (tier-5 engine at 128 rad/s: 1048576 W) with one ball bearing: ten shots of 3, then it is empty. */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "turret_multi")
	public static void multiCannonFiresBursts(GameTestHelper h) {
		TurretBlockEntity t = rig(h, RotaryBlocks.MULTI_CANNON.get(), TurretBlockEntity.class, 5, 7);
		t.getInventory().setStackInSlot(0, new ItemStack(RotaryItems.part(CraftingPart.BALLBEARING)));
		var zb = target(h, EntityType.HUSK, 7.5, 3, 4.5);
		zb.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
		zb.setHealth(100);
		h.succeedWhen(() -> {
			h.assertTrue(t.shots == 10 && zb.getHealth() == 70 && t.getInventory().getStackInSlot(0).isEmpty(), "multi-cannon shots "+t.shots+", zombie "+zb.getHealth());
			done(h, zb);
		});
	}

	/** Anti-air gun (fuel engine) with scrap: it ignores the zombie on the ground and hits the phantom in the air. */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "turret_aa")
	public static void antiAirGunHitsFliers(GameTestHelper h) {
		TurretBlockEntity t = rig(h, RotaryBlocks.ANTI_AIR_GUN.get(), TurretBlockEntity.class, -1, 0);
		t.getInventory().setStackInSlot(0, new ItemStack(RotaryItems.part(CraftingPart.SCRAP), 8));
		var zb = target(h, EntityType.HUSK, 6.5, 3, 4.5);
		var ph = target(h, EntityType.PHANTOM, 3.5, 6, 7.5);
		float zhp = zb.getHealth(), php = ph.getHealth();
		h.succeedWhen(() -> {
			h.assertTrue(t.shots > 0 && ph.getHealth() < php, "AA shots "+t.shots+", phantom "+ph.getHealth()+", target "+t.lastTarget);
			h.assertTrue(zb.getHealth() == zhp, "shot the zombie");
			ph.discard();
			done(h, zb);
		});
	}

	/** Flame turret (tier-2 engine at 512 rad/s: 65536 W) with ethanol: range 1.2*32 = 38, the zombie burns, 5 mB a shot. */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "turret_flame")
	public static void flameTurretBurns(GameTestHelper h) {
		FlameTurretBlockEntity t = rig(h, RotaryBlocks.FLAME_TURRET.get(), FlameTurretBlockEntity.class, 2, 9);
		t.getTank().fill(new FluidStack(RotaryFluids.ETHANOL.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
		h.assertTrue(t.getRange() == 38, "range "+t.getRange());
		var zb = target(h, EntityType.HUSK, 7.5, 3, 4.5);
		h.succeedWhen(() -> {
			h.assertTrue(t.shots > 0 && zb.isOnFire() && t.getTank().getFluidAmount() == 1000-5*t.shots, "flame turret "+t.getPower()+" W at "+t.getOmega()+" rad/s, shots "+t.shots+", tank "+t.getTank().getFluidAmount());
			done(h, zb);
		});
	}
}
