/*******************************************************************************
 * Tests for the ported ReactorCraft radiation cleaner and iron finder (original ItemRadiationCleaner, ItemIronFinder). Private NeoForge
 * 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import java.util.Set;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.entity.RadiationEntity;
import reika.reactorcraft.content.item.IronFinderItem;
import reika.reactorcraft.content.item.RadiationCleanerItem;
import reika.reactorcraft.radiation.RadiationIntensity;
import reika.reactorcraft.registry.ReactorComponents;
import reika.reactorcraft.registry.ReactorItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ToolTests {

	private static final String PLATFORM = "platform_9x4x9";

	/** Right-clicking a water source within 5 blocks takes 1000 mB into the cleaner and removes the source. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void cleanerTakesWater(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 1, 6), Blocks.WATER);
		Player ep = h.makeMockPlayer(GameType.SURVIVAL);
		ep.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 2, 6))));
		ep.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(h.absolutePos(new BlockPos(4, 1, 6))));
		ItemStack is = new ItemStack(ReactorItems.CLEANER.get());
		ep.setItemInHand(InteractionHand.MAIN_HAND, is);
		ReactorItems.CLEANER.get().use(h.getLevel(), ep, InteractionHand.MAIN_HAND);
		var hit = ep.pick(5, 1F, true);
		h.assertTrue(RadiationCleanerItem.getWater(is) == 1000, "water "+RadiationCleanerItem.getWater(is)+", hit "+hit.getType()+" "+hit.getLocation()
				+(hit instanceof net.minecraft.world.phys.BlockHitResult b ? " "+b.getBlockPos()+" "+h.getLevel().getBlockState(b.getBlockPos()) : "")+", eye "+ep.getEyePosition()+", rot "+ep.getXRot()+"/"+ep.getYRot());
		h.assertTrue(h.getBlockState(new BlockPos(4, 1, 6)).isAir(), "source not taken: "+h.getBlockState(new BlockPos(4, 1, 6)));
		h.succeed();
	}

	/**
	 * Spraying shrinks radiation clouds 0.5..4.5 blocks ahead every 5th tick; letting go costs 1 kJ per 5 ticks and 25 mB per tick, and
	 * the use time is min(5 ticks per kJ, water/25).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void cleanerShrinksRadiation(GameTestHelper h) {
		Player ep = h.makeMockPlayer(GameType.SURVIVAL);
		ep.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 1, 1))));
		Vec3 eye = ep.getEyePosition();
		ep.lookAt(EntityAnchorArgument.Anchor.EYES, eye.add(0, 0, 10));
		RadiationEntity rad = new RadiationEntity(h.getLevel(), 50, RadiationIntensity.LOWLEVEL);
		rad.moveTo(eye.x, eye.y, eye.z+2.5);
		h.getLevel().addFreshEntity(rad);
		ItemStack is = RadiationCleanerItem.charged(ReactorItems.CLEANER.get());
		RadiationCleanerItem item = ReactorItems.CLEANER.get();
		h.assertTrue(item.getUseDuration(is, ep) == Math.min(32000*5, 32000/25), "use time "+item.getUseDuration(is, ep));
		for (int i = 0; i < 100; i++)
			item.onUseTick(h.getLevel(), ep, is, i);
		h.assertTrue(rad.getRange() < 50 && rad.getRange() >= 30, "cloud range "+rad.getRange()+" after 20 sprays");
		item.releaseUsing(is, h.getLevel(), ep, item.getUseDuration(is, ep)-100);
		h.assertTrue(RadiationCleanerItem.getCharge(is) == 32000-20 && RadiationCleanerItem.getWater(is) == 32000-2500,
				"charge "+RadiationCleanerItem.getCharge(is)+", water "+RadiationCleanerItem.getWater(is));
		ItemStack dry = new ItemStack(ReactorItems.CLEANER.get());
		dry.set(ReactorComponents.CHARGE.get(), 1000);
		h.assertTrue(item.getUseDuration(dry, ep) == 0, "usable without water");
		h.succeed();
	}

	/** The finder lists iron (also deepslate), redstone and magnetite ores (c:ores tags) within the range, not other blocks. */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void ironFinderFindsMagneticOres(GameTestHelper h) {
		h.setBlock(new BlockPos(2, 1, 2), Blocks.IRON_ORE);
		h.setBlock(new BlockPos(3, 1, 2), Blocks.DEEPSLATE_IRON_ORE);
		h.setBlock(new BlockPos(4, 1, 2), Blocks.REDSTONE_ORE);
		h.setBlock(new BlockPos(5, 1, 2), reika.reactorcraft.registry.ReactorBlocks.ore(reika.reactorcraft.registry.ReactorOres.MAGNETITE).get());
		h.setBlock(new BlockPos(6, 1, 2), Blocks.GOLD_ORE);
		h.setBlock(new BlockPos(7, 1, 2), Blocks.COAL_ORE);
		Player ep = h.makeMockPlayer(GameType.SURVIVAL);
		ep.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 1, 4))));
		Set<BlockPos> found = IronFinderItem.findOreNearby(ep, 8);
		for (int x = 2; x <= 5; x++)
			h.assertTrue(found.contains(h.absolutePos(new BlockPos(x, 1, 2))), "missed "+h.getBlockState(new BlockPos(x, 1, 2)));
		h.assertTrue(found.size() == 4, "found "+found.size()+": "+found);
		h.succeed();
	}

	/** A Geiger counter and a wound spring (500 kJ) alone in the RotaryCraft worktable: they swap charges (counter 500, spring 0). */
	@GameTest(template = PLATFORM, timeoutTicks = 20)
	public static void worktableChargesTool(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 1, 4), reika.rotarycraft.registry.RotaryBlocks.WORKTABLE.get());
		var wt = (reika.rotarycraft.content.blockentity.WorktableBlockEntity)h.getBlockEntity(new BlockPos(4, 1, 4));
		wt.getInventory().setStackInSlot(0, new ItemStack(ReactorItems.GEIGER.get()));
		wt.getInventory().setStackInSlot(4, reika.rotarycraft.content.item.CoilItem.withCharge(new ItemStack(reika.rotarycraft.registry.RotaryItems.SPRING.get()), 500));
		h.runAtTickTime(10, () -> {
			ItemStack tool = wt.getInventory().getStackInSlot(9), coil = wt.getInventory().getStackInSlot(10);
			h.assertTrue(tool.is(ReactorItems.GEIGER.get()) && reika.reactorcraft.content.item.GeigerCounterItem.getCharge(tool) == 500, "tool "+tool);
			h.assertTrue(coil.is(reika.rotarycraft.registry.RotaryItems.SPRING.get()) && reika.rotarycraft.content.item.CoilItem.getCharge(coil) == 0, "coil "+coil);
			h.succeed();
		});
	}
}
