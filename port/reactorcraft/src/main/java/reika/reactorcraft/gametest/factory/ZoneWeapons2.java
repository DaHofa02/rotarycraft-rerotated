/*******************************************************************************
 * Practice factory zone (see Factory): the port's self-destruct, sonic weapon, EMP, machine gun, TNT cannon and block cannon set up by a
 * simulated player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.BlockCannonBlockEntity;
import reika.rotarycraft.content.blockentity.EmpBlockEntity;
import reika.rotarycraft.content.blockentity.FuelEngineBlockEntity;
import reika.rotarycraft.content.blockentity.MachineGunBlockEntity;
import reika.rotarycraft.content.blockentity.MagneticEngineBlockEntity;
import reika.rotarycraft.content.blockentity.SelfDestructBlockEntity;
import reika.rotarycraft.content.blockentity.SonicWeaponBlockEntity;
import reika.rotarycraft.content.blockentity.TntCannonBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.network.RotaryNetwork;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneWeapons2 {

	public static final List<String> BLOCKS = List.of("rotarycraft:self_destruct", "rotarycraft:sonic_weapon", "rotarycraft:emp",
			"rotarycraft:machine_gun", "rotarycraft:tnt_cannon", "rotarycraft:block_cannon");

	private static final String PLATFORM = "platform_9x8x9";

	private static FuelEngineBlockEntity fuelEngine(GameTestHelper h, SimPlayer p, BlockPos pos) {
		FuelEngineBlockEntity.TEST_FUELS.add(Fluids.LAVA);
		p.place(pos, RotaryBlocks.FUEL_ENGINE.get(), Direction.EAST); //drives west
		FuelEngineBlockEntity e = (FuelEngineBlockEntity)h.getBlockEntity(pos);
		e.fill(Direction.EAST, new FluidStack(Fluids.LAVA, 20000), IFluidHandler.FluidAction.EXECUTE); //what pipes would bring
		e.fill(Direction.EAST, new FluidStack(RotaryFluids.LUBRICANT.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
		return e;
	}

	/**
	 * The guns: a magnetostatic engine (upgraded to tier 3, set to 2048 rad/s in its GUI; FE as a cable would bring) -> machine gun with arrows put in
	 * through its GUI, shooting a zombie; a fuel engine -> bevel gears -> block cannon with cobblestone, its target typed into the GUI.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "weaponzone")
	public static void playerBuildsGuns(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.CREATIVE);
		BlockPos mag = new BlockPos(8, 2, 1), gun = new BlockPos(7, 2, 1);
		p.place(mag, RotaryBlocks.MAGNETIC_ENGINE.get(), Direction.EAST); //drives west
		MagneticEngineBlockEntity me = (MagneticEngineBlockEntity)h.getBlockEntity(mag);
		for (var u : List.of(reika.rotarycraft.registry.RotaryItems.UPGRADE_MAGNET_1, reika.rotarycraft.registry.RotaryItems.UPGRADE_MAGNET_2, reika.rotarycraft.registry.RotaryItems.UPGRADE_MAGNET_3)) {
			p.hold(u.get());
			p.useOn(mag, Direction.UP); //tier 3: up to 2048 rad/s
		}
		h.assertTrue(p.open(mag) != null, "engine opens no GUI");
		for (int i = 0; i < 12; i++)
			p.player.containerMenu.clickMenuButton(p.player, 1);
		p.closeMenu();
		h.onEachTick(() -> me.getEnergyStorage(null).receiveEnergy(Integer.MAX_VALUE, false));
		p.placeSneaking(gun, new ItemStack(RotaryBlocks.MACHINE_GUN.get()), Direction.EAST); //power from the east, fires west
		h.assertTrue(p.open(gun) != null, "machine gun opens no GUI");
		p.putInto(0, Items.ARROW, 64);
		p.closeMenu();
		Zombie zb = h.spawn(EntityType.ZOMBIE, new Vec3(2.5, 2, 1.5));
		zb.setNoAi(true);
		float hp = zb.getHealth();
		BlockPos bev = new BlockPos(4, 2, 6), cannon = new BlockPos(4, 3, 6);
		fuelEngine(h, p, new BlockPos(5, 2, 6));
		p.place(new BlockPos(4, 2, 7), Items.STONE, Direction.EAST);
		p.place(new BlockPos(4, 3, 7), Items.STONE, Direction.EAST);
		p.placeSneaking(cannon, new ItemStack(RotaryBlocks.BLOCK_CANNON.get()), Direction.EAST);
		p.placeSneaking(bev, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		h.assertTrue(p.open(cannon) != null, "block cannon opens no GUI");
		p.putInto(0, Items.COBBLESTONE, 1);
		RotaryNetwork.applyCannonTarget(p.player, new RotaryNetwork.CannonTarget(h.absolutePos(cannon), 0, h.absolutePos(new BlockPos(0, 2, 6))));
		p.closeMenu();
		MachineGunBlockEntity g = (MachineGunBlockEntity)h.getBlockEntity(gun);
		BlockCannonBlockEntity c = (BlockCannonBlockEntity)h.getBlockEntity(cannon);
		h.succeedWhen(() -> {
			h.assertTrue(g.shots > 0 && zb.getHealth() < hp, "machine gun "+g.getOmega()+" rad/s "+g.getPower()+" W, shots "+g.shots);
			h.assertTrue(c.shots == 1, "block cannon shots "+c.shots+", aim "+c.aim());
			Factory.assertBuilt(h, List.of("rotarycraft:machine_gun", "rotarycraft:block_cannon"), new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			zb.discard();
			p.leave();
		});
	}

	/**
	 * The rest: gas engine -> self-destruct (armed while powered; real blasts off), fuel engine -> bevel gears -> sonic weapon (pulsing; glass
	 * shattering off), a TNT cannon (target in range, no ammo) and an EMP (only placed: it needs 268 MW).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "weaponzone")
	public static void playerBuildsTheRest(GameTestHelper h) {
		SelfDestructBlockEntity.allowExplosion = false;
		SonicWeaponBlockEntity.allowBlockDamage = false;
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.CREATIVE);
		p.place(new BlockPos(8, 2, 1), RotaryBlocks.GAS_ENGINE.get(), Direction.EAST);
		((EngineBlockEntity)h.getBlockEntity(new BlockPos(8, 2, 1))).addFuel(50000);
		p.placeSneaking(new BlockPos(7, 2, 1), new ItemStack(RotaryBlocks.SELF_DESTRUCT.get()), Direction.EAST);
		fuelEngine(h, p, new BlockPos(5, 2, 6));
		p.place(new BlockPos(4, 2, 7), Items.STONE, Direction.EAST);
		p.place(new BlockPos(4, 3, 7), Items.STONE, Direction.EAST);
		p.placeSneaking(new BlockPos(4, 3, 6), new ItemStack(RotaryBlocks.SONIC_WEAPON.get()), Direction.EAST);
		p.placeSneaking(new BlockPos(4, 2, 6), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		p.place(new BlockPos(1, 2, 1), RotaryBlocks.TNT_CANNON.get(), Direction.EAST);
		p.place(new BlockPos(1, 2, 7), RotaryBlocks.EMP.get(), Direction.EAST);
		SelfDestructBlockEntity sd = (SelfDestructBlockEntity)h.getBlockEntity(new BlockPos(7, 2, 1));
		SonicWeaponBlockEntity sw = (SonicWeaponBlockEntity)h.getBlockEntity(new BlockPos(4, 3, 6));
		TntCannonBlockEntity tc = (TntCannonBlockEntity)h.getBlockEntity(new BlockPos(1, 2, 1));
		EmpBlockEntity emp = (EmpBlockEntity)h.getBlockEntity(new BlockPos(1, 2, 7));
		p.moveNear(new BlockPos(1, 2, 1)); //machines answer players within 8 blocks
		RotaryNetwork.applyCannonTarget(p.player, new RotaryNetwork.CannonTarget(h.absolutePos(new BlockPos(1, 2, 1)), 0, h.absolutePos(new BlockPos(6, 2, 3))));
		h.succeedWhen(() -> {
			h.assertTrue(sd.isArmed(), "self-destruct not armed: "+sd.getPower()+" W");
			h.assertTrue(sw.pulses > 0, "sonic weapon silent: "+sw.getPower()+" W");
			h.assertTrue(tc.getTarget() != null && tc.getTarget().equals(h.absolutePos(new BlockPos(6, 2, 3))), "cannon target "+tc.getTarget());
			h.assertTrue(!emp.hasFired() && emp.getEnergy() == 0, "EMP charged without power");
			Factory.assertBuilt(h, List.of("rotarycraft:self_destruct", "rotarycraft:sonic_weapon", "rotarycraft:tnt_cannon", "rotarycraft:emp"), new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			h.setBlock(new BlockPos(7, 2, 1), Blocks.AIR); //disarm before the zone is left
			h.setBlock(new BlockPos(4, 3, 6), Blocks.AIR);
			SelfDestructBlockEntity.allowExplosion = true;
			SonicWeaponBlockEntity.allowBlockDamage = true;
			p.leave();
		});
	}
}
