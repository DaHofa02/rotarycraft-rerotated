/*******************************************************************************
 * Tests for the ported RotaryCraft particle emitter (original TileEntityParticleEmitter). Private NeoForge 1.21.1 port of RotaryCraft by
 * Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.ParticleEmitterBlockEntity;
import reika.rotarycraft.content.blockentity.ParticleEmitterBlockEntity.Kind;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.content.menu.ParticleEmitterMenu;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ParticleEmitterTests {

	private static final String PLATFORM = "platform_9x8x9";
	private static final BlockPos POS = new BlockPos(4, 2, 4);

	private static ParticleEmitterBlockEntity emitter(GameTestHelper h, int charge) {
		h.setBlock(POS, RotaryBlocks.PARTICLE_EMITTER.get());
		ParticleEmitterBlockEntity te = (ParticleEmitterBlockEntity)h.getBlockEntity(POS);
		if (charge > 0)
			te.getInventory().setStackInSlot(0, CoilItem.withCharge(new ItemStack(RotaryItems.SPRING.get()), charge));
		return te;
	}

	/** 31 kinds as in ReikaParticleHelper, 27 offered (no dripping water or lava, no suspend or snowshovel); smoke and 3 per tick by default. */
	@GameTest(template = PLATFORM)
	public static void emitterOffersTheOriginalParticles(GameTestHelper h) {
		h.assertTrue(Kind.values().length == 31 && ParticleEmitterMenu.KINDS.size() == 27, Kind.values().length+" kinds, "+ParticleEmitterMenu.KINDS.size()+" offered");
		h.assertTrue(Kind.FLAME.ordinal() == 3 && Kind.ANGRY.ordinal() == 30, "order changed");
		for (Kind k : new Kind[] {Kind.DRIPWATER, Kind.DRIPLAVA, Kind.SUSPEND, Kind.SNOWSHOVEL})
			h.assertFalse(ParticleEmitterMenu.KINDS.contains(k), k+" offered");
		for (Kind k : Kind.values())
			h.assertTrue(k.particle() != null, k+" has no particle");
		ParticleEmitterBlockEntity te = emitter(h, 0);
		h.assertTrue(te.particleType == Kind.SMOKE && te.particlesPerTick == 3 && !te.canEmit(), "defaults");
		h.succeed();
	}

	/** One charge lasts 600 ticks with a plain spring; it emits only while the coil has charge. */
	@GameTest(template = PLATFORM, timeoutTicks = 700)
	public static void emitterRunsOnItsCoil(GameTestHelper h) {
		ParticleEmitterBlockEntity te = emitter(h, 1);
		h.runAtTickTime(590, () -> h.assertTrue(te.canEmit() && te.getCoilCharge() == 1, "after 590 ticks: charge "+te.getCoilCharge()));
		h.runAtTickTime(610, () -> {
			h.assertTrue(!te.canEmit() && te.getCoilCharge() == 0, "after 610 ticks: charge "+te.getCoilCharge());
			h.succeed();
		});
	}

	/** GUI button i picks the i-th offered kind; the kind is kept when the block is saved and loaded. */
	@GameTest(template = PLATFORM)
	public static void emitterKindIsPickedInTheGui(GameTestHelper h) {
		ParticleEmitterBlockEntity te = emitter(h, 5);
		ParticleEmitterMenu menu = new ParticleEmitterMenu(1, h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL).getInventory(), te);
		int heart = ParticleEmitterMenu.KINDS.indexOf(Kind.HEART);
		h.assertTrue(heart == 13, "heart is button "+heart);
		h.assertTrue(menu.clickMenuButton(null, heart) && te.particleType == Kind.HEART, "heart not picked: "+te.particleType);
		h.assertFalse(menu.clickMenuButton(null, 27), "button 27 exists");
		te.setParticleType(Kind.DRIPLAVA);
		h.assertTrue(te.particleType == Kind.HEART, "dripping lava set");
		CompoundTag tag = te.saveWithoutMetadata(h.getLevel().registryAccess());
		h.setBlock(POS, Blocks.AIR);
		ParticleEmitterBlockEntity copy = emitter(h, 0);
		copy.loadWithComponents(tag, h.getLevel().registryAccess());
		h.assertTrue(copy.particleType == Kind.HEART && copy.getCoilCharge() == 5, "loaded "+copy.particleType+", charge "+copy.getCoilCharge());
		h.succeed();
	}
}
