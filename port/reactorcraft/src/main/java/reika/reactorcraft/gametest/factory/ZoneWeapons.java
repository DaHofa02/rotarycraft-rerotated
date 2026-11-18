/*******************************************************************************
 * Practice factory zone (see Factory): the port's force field, containment field, air gun and land mine set up by a simulated survival
 * player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.AirGunBlockEntity;
import reika.rotarycraft.content.blockentity.DomeBlockEntity;
import reika.rotarycraft.content.blockentity.FuelEngineBlockEntity;
import reika.rotarycraft.content.blockentity.LandmineBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneWeapons {

	public static final List<String> BLOCKS = List.of("rotarycraft:force_field", "rotarycraft:containment", "rotarycraft:air_gun", "rotarycraft:landmine");

	private static final String PLATFORM = "platform_9x8x9";

	/** The player sets a fuel engine (looking east: drives west) at pos and fills it the way pipes would (lava stands in for a mod's fuel). */
	private static void engine(GameTestHelper h, SimPlayer p, BlockPos pos) {
		FuelEngineBlockEntity.TEST_FUELS.add(Fluids.LAVA);
		p.place(pos, RotaryBlocks.FUEL_ENGINE.get(), Direction.EAST);
		FuelEngineBlockEntity e = (FuelEngineBlockEntity)h.getBlockEntity(pos);
		e.fill(Direction.EAST, new FluidStack(Fluids.LAVA, 20000), IFluidHandler.FluidAction.EXECUTE);
		e.fill(Direction.EAST, new FluidStack(RotaryFluids.LUBRICANT.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
	}

	/** Fuel engine -> bevel gears (placed last, finding east -> up) -> a dome block on top; it comes up. */
	private static void dome(GameTestHelper h, Block dome) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.CREATIVE);
		engine(h, p, new BlockPos(5, 2, 4));
		p.place(new BlockPos(4, 2, 5), Items.STONE, Direction.EAST);
		p.place(new BlockPos(4, 3, 5), Items.STONE, Direction.EAST);
		p.placeSneaking(new BlockPos(4, 3, 4), new ItemStack(dome), Direction.EAST);
		p.placeSneaking(new BlockPos(4, 2, 4), new ItemStack(RotaryBlocks.BEVEL_GEARS.get()), Direction.EAST);
		DomeBlockEntity d = (DomeBlockEntity)h.getBlockEntity(new BlockPos(4, 3, 4));
		h.succeedWhen(() -> {
			h.assertTrue(d.getRange() > 0, "no dome: "+d.getPower()+" W");
			Factory.assertBuilt(h, List.of(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(dome).toString()), new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			h.setBlock(new BlockPos(4, 3, 4), net.minecraft.world.level.block.Blocks.AIR);
			p.leave();
		});
	}

	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "weaponzone")
	public static void playerBuildsAForceField(GameTestHelper h) {
		dome(h, RotaryBlocks.FORCE_FIELD.get());
	}

	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "weaponzone")
	public static void playerBuildsAContainmentField(GameTestHelper h) {
		dome(h, RotaryBlocks.CONTAINMENT.get());
	}

	/**
	 * Fuel engine -> air gun (looking east: power from the east, blasts west); the player fills a land mine with gunpowder through its GUI;
	 * an armor stand on the mine sets it off (explosion switched off here) and the air gun is running.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "weaponzone")
	public static void playerBuildsAnAirGunAndAMine(GameTestHelper h) {
		LandmineBlockEntity.allowExplosion = false;
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		p.give(new ItemStack(RotaryBlocks.FUEL_ENGINE.get()));
		p.give(new ItemStack(RotaryBlocks.AIR_GUN.get()));
		p.give(new ItemStack(RotaryBlocks.LANDMINE.get()));
		p.give(new ItemStack(Items.GUNPOWDER, 2));
		engine(h, p, new BlockPos(7, 2, 4));
		p.placeSneaking(new BlockPos(6, 2, 4), new ItemStack(RotaryBlocks.AIR_GUN.get()), Direction.EAST);
		BlockPos mine = new BlockPos(2, 2, 7);
		p.place(mine, RotaryBlocks.LANDMINE.get(), Direction.EAST);
		h.assertTrue(p.open(mine) != null, "land mine opens no GUI");
		p.putInto(0, Items.GUNPOWDER, 1);
		p.putInto(1, Items.GUNPOWDER, 1);
		p.closeMenu();
		AirGunBlockEntity g = (AirGunBlockEntity)h.getBlockEntity(new BlockPos(6, 2, 4));
		LandmineBlockEntity m = (LandmineBlockEntity)h.getBlockEntity(mine);
		var zb = h.spawn(EntityType.ARMOR_STAND, new Vec3(2.5, 3, 7.5)); //something standing on the mine
		h.succeedWhen(() -> {
			h.assertTrue(g.getPower() >= 16384 && g.getFacing() == Direction.WEST, "air gun "+g.getPower()+" W facing "+g.getFacing());
			h.assertTrue(m.detonations == 1 && m.lastPower == 4, "mine "+m.detonations+" at "+m.lastPower);
			Factory.assertBuilt(h, List.of("rotarycraft:air_gun", "rotarycraft:landmine"), new BlockPos(0, 2, 0), new BlockPos(8, 3, 8));
			zb.discard();
			LandmineBlockEntity.allowExplosion = true;
			p.leave();
		});
	}
}
