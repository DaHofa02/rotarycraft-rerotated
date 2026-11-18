/*******************************************************************************
 * Practice factory zone (see Factory): the port's turrets set up by a simulated player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
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
import reika.rotarycraft.content.blockentity.FuelEngineBlockEntity;
import reika.rotarycraft.content.blockentity.turret.TurretBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneTurrets {

	public static final List<String> BLOCKS = List.of("rotarycraft:railgun", "rotarycraft:freeze_gun", "rotarycraft:laser_gun",
			"rotarycraft:multi_cannon", "rotarycraft:anti_air_gun", "rotarycraft:flame_turret");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * The player sets up a freeze gun on a fuel engine and bevel gears and loads it with snowballs through its GUI: it freezes a husk. The
	 * other five turrets are placed along the back row without power (each needs far more than one engine here gives).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "turretzone")
	public static void playerBuildsTurrets(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.CREATIVE);
		FuelEngineBlockEntity.TEST_FUELS.add(Fluids.LAVA);
		BlockPos eng = new BlockPos(5, 2, 4), bev = new BlockPos(4, 2, 4), gun = new BlockPos(4, 3, 4);
		p.place(eng, RotaryBlocks.FUEL_ENGINE.get(), Direction.EAST); //drives west
		FuelEngineBlockEntity e = (FuelEngineBlockEntity)h.getBlockEntity(eng);
		e.fill(Direction.EAST, new FluidStack(Fluids.LAVA, 20000), IFluidHandler.FluidAction.EXECUTE); //what pipes would bring
		e.fill(Direction.EAST, new FluidStack(RotaryFluids.LUBRICANT.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
		p.place(new BlockPos(4, 2, 5), Items.STONE, Direction.EAST);
		p.place(new BlockPos(4, 3, 5), Items.STONE, Direction.EAST);
		p.placeSneaking(gun, new ItemStack(RotaryBlocks.FREEZE_GUN.get()), Direction.EAST);
		p.placeSneaking(bev, new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		h.assertTrue(p.open(gun) != null, "freeze gun opens no GUI");
		p.putInto(0, Items.SNOWBALL, 16);
		p.closeMenu();
		int x = 1;
		for (var b : List.of(RotaryBlocks.RAILGUN, RotaryBlocks.LASER_GUN, RotaryBlocks.MULTI_CANNON, RotaryBlocks.ANTI_AIR_GUN, RotaryBlocks.FLAME_TURRET))
			p.place(new BlockPos(x++, 2, 8), b.get(), Direction.EAST);
		var husk = h.spawn(EntityType.HUSK, new Vec3(0.5, 2, 4.5));
		husk.setNoAi(true);
		TurretBlockEntity t = (TurretBlockEntity)h.getBlockEntity(gun);
		h.succeedWhen(() -> {
			h.assertTrue(t.shots > 0 && husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "freeze gun "+t.getPower()+" W, shots "+t.shots);
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			husk.discard();
			h.setBlock(gun, Blocks.AIR);
			p.leave();
		});
	}
}
