/*******************************************************************************
 * Tests for the ported RotaryCraft solar plant (original TileEntitySolar, TileEntityMirror, SolarPlant). Expected values follow from
 * the original formulas: torque = brightness * tower multiplier * (mirrors+1), 512 rad/s with water. Private NeoForge 1.21.1 port of
 * RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.MirrorBlockEntity;
import reika.rotarycraft.content.blockentity.SolarTowerBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SolarTests {

	private static final String TALL = "platform_9x8x9";

	private static SolarTowerBlockEntity[] tower(GameTestHelper h, int x, int z, int height) {
		SolarTowerBlockEntity[] t = new SolarTowerBlockEntity[height];
		for (int i = 0; i < height; i++) {
			h.setBlock(new BlockPos(x, 1+i, z), RotaryBlocks.SOLAR_TOWER.get());
			t[i] = (SolarTowerBlockEntity)h.getBlockEntity(new BlockPos(x, 1+i, z));
		}
		return t;
	}

	private static MirrorBlockEntity mirror(GameTestHelper h, int x, int z) {
		h.setBlock(new BlockPos(x, 1, z), RotaryBlocks.MIRROR.get());
		return (MirrorBlockEntity)h.getBlockEntity(new BlockPos(x, 1, z));
	}

	/**
	 * A tower 3 high with 4 mirrors around its foot at noon: brightness 1, effective tower height 3, so 512 rad/s at 1*3*(4+1) = 15 Nm
	 * downwards, drinking 300 mB per tick (10+16*log2(7680) = 202, rounded up to the next hundred).
	 */
	@GameTest(template = TALL, timeoutTicks = 60)
	public static void plantMakesPowerAtNoon(GameTestHelper h) {
		h.getLevel().setDayTime(6000);
		SolarTowerBlockEntity[] t = tower(h, 4, 4, 3);
		MirrorBlockEntity[] m = {mirror(h, 3, 4), mirror(h, 5, 4), mirror(h, 4, 3), mirror(h, 4, 5)};
		h.onEachTick(() -> t[0].fill(Direction.NORTH, new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE));
		h.runAtTickTime(20, () -> {
			h.assertTrue(t[0].getPlant() != null && t[0].getPlant().mirrorCount() == 4 && t[0].getPlant().towerCount() == 1, "plant");
			h.assertTrue(t[0].getPlant().getTowerMultiplier() == 3, "tower multiplier "+t[0].getPlant().getTowerMultiplier());
			for (MirrorBlockEntity mm : m)
				h.assertTrue(mm.isFunctional(), "mirror not working at "+mm.getBlockPos()+": broken "+mm.broken+", plant "+(mm.getPlant() != null)
						+", sky light "+h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.SKY, mm.getBlockPos().above())
						+", surface "+h.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, mm.getBlockPos().getX(), mm.getBlockPos().getZ())
						+", above "+h.getLevel().getBlockState(mm.getBlockPos().above())+", canSeeSky "+h.getLevel().canSeeSky(mm.getBlockPos().above())
						+", plant.canSeeTheSky "+mm.getPlant().canSeeTheSky(mm)+", same plant "+(mm.getPlant() == t[0].getPlant())+", above BE "+h.getLevel().getBlockEntity(mm.getBlockPos().above()));
			h.assertTrue(t[0].getOmega() == 512 && t[0].getTorque() == 15, "tower: "+t[0].getOmega()+" rad/s, "+t[0].getTorque()+" Nm");
			h.assertTrue(t[0].getCurrentConsumption() == 300, "uses "+t[0].getCurrentConsumption()+" mB/t");
			h.assertTrue(t[2].getOmega() == 0, "upper block makes power");
			h.succeed();
		});
	}

	/** Without water, or at midnight under a new moon, nothing; a covered mirror (block on it) does not count. */
	@GameTest(template = TALL, timeoutTicks = 60)
	public static void plantNeedsWaterAndLight(GameTestHelper h) {
		h.getLevel().setDayTime(6000);
		SolarTowerBlockEntity[] t = tower(h, 4, 4, 3);
		mirror(h, 3, 4);
		MirrorBlockEntity covered = mirror(h, 5, 4);
		h.setBlock(new BlockPos(5, 2, 4), RotaryBlocks.SOLAR_TOWER.get());
		h.runAtTickTime(20, () -> {
			h.assertTrue(t[0].getPower() == 0, "made power without water");
			h.assertFalse(covered.isFunctional(), "covered mirror works");
			h.succeed();
		});
	}

	/**
	 * Breaking a mirror (as a heavy falling entity does) takes it out of the brightness; it is repaired with a mirror item. The mass
	 * table decides what breaks it: a pig (100 kg) does, an item (0.25 kg) falling at 0.5 blocks/tick does not.
	 */
	@GameTest(template = TALL, timeoutTicks = 60)
	public static void mirrorBreaksAndIsRepaired(GameTestHelper h) {
		h.getLevel().setDayTime(6000);
		tower(h, 4, 4, 3);
		MirrorBlockEntity m = mirror(h, 3, 4);
		h.assertTrue(MirrorBlockEntity.getEntityMass(EntityType.PIG.create(h.getLevel()))+0.5*20 > 80, "pig too light");
		h.assertFalse(MirrorBlockEntity.getEntityMass(EntityType.ITEM.create(h.getLevel()))+0.5*20 > 80, "item heavy enough");
		m.breakMirror(h.getLevel(), m.getBlockPos());
		h.assertFalse(m.isFunctional(), "broken mirror works");
		Player ep = h.makeMockPlayer(GameType.SURVIVAL);
		ItemStack is = new ItemStack(RotaryItems.part(CraftingPart.MIRROR), 2);
		ep.setItemInHand(InteractionHand.MAIN_HAND, is);
		BlockPos a = h.absolutePos(new BlockPos(3, 1, 4));
		h.getLevel().getBlockState(a).useItemOn(is, h.getLevel(), ep, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(a), Direction.UP, a, false));
		h.assertTrue(!m.broken && is.getCount() == 1, "repair: broken "+m.broken+", mirrors left "+is.getCount());
		h.runAtTickTime(5, () -> {
			h.assertTrue(m.isFunctional(), "repaired mirror does not work");
			h.succeed();
		});
	}

	/**
	 * Two towers (heights 4 and 2; mirrors beside the first two blocks of the higher one, which in the original count loop does not
	 * shorten it): the higher tower is primary and the only one with power; effective blocks 4 + 2*0.72 = 5.44 -> 5.
	 */
	@GameTest(template = TALL, timeoutTicks = 60)
	public static void primaryTowerOnly(GameTestHelper h) {
		h.getLevel().setDayTime(6000);
		SolarTowerBlockEntity[] a = tower(h, 2, 4, 4);
		SolarTowerBlockEntity[] b = tower(h, 6, 4, 2);
		for (int x = 3; x <= 5; x++)
			mirror(h, x, 4);
		h.setBlock(new BlockPos(1, 2, 4), RotaryBlocks.MIRROR.get());
		h.setBlock(new BlockPos(1, 1, 4), RotaryBlocks.MIRROR.get());
		h.onEachTick(() -> {
			a[0].fill(Direction.NORTH, new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
			b[0].fill(Direction.NORTH, new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
		});
		h.runAtTickTime(20, () -> {
			var p = a[0].getPlant();
			h.assertTrue(p != null && p.towerCount() == 2 && p.mirrorCount() == 5, "plant: "+(p == null ? "none" : p.towerCount()+" towers, "+p.mirrorCount()+" mirrors"));
			h.assertTrue(p.getPrimaryTower().getX() == a[0].getBlockPos().getX(), "primary tower at x "+p.getPrimaryTower().getX());
			h.assertTrue(p.getTowerMultiplier() == 5, "multiplier "+p.getTowerMultiplier());
			h.assertTrue(a[0].getOmega() == 512 && b[0].getOmega() == 0, "towers: "+a[0].getOmega()+" / "+b[0].getOmega());
			h.succeed();
		});
	}
}
