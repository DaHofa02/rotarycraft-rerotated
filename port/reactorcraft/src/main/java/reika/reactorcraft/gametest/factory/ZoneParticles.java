/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft particle emitter built and run by a simulated survival player. Not part of the
 * original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.ParticleEmitterBlockEntity;
import reika.rotarycraft.content.item.CoilItem;
import reika.rotarycraft.content.menu.ParticleEmitterMenu;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneParticles {

	public static final List<String> BLOCKS = List.of("rotarycraft:particle_emitter");

	private static final String PLATFORM = "platform_9x8x9";

	/** The player sets a particle emitter down, puts a wound coil in through its GUI and picks flames with a GUI button. */
	@GameTest(template = PLATFORM, timeoutTicks = 200, batch = "factory")
	public static void emitterShowsFlames(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.PARTICLE_EMITTER.get()), CoilItem.withCharge(new ItemStack(RotaryItems.SPRING.get()), 100));
		Factory.take(p, chest, RotaryBlocks.PARTICLE_EMITTER.get().asItem(), 1);
		Factory.takeExactly(p, chest, RotaryItems.SPRING.get(), 1);
		BlockPos pos = new BlockPos(4, 2, 4);
		p.place(pos, RotaryBlocks.PARTICLE_EMITTER.get(), Direction.EAST);
		ParticleEmitterBlockEntity te = (ParticleEmitterBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(p.open(pos) instanceof ParticleEmitterMenu, "emitter opens no particle GUI: "+p.player.containerMenu);
		p.putInto(0, RotaryItems.SPRING.get(), 1);
		h.assertTrue(p.player.containerMenu.clickMenuButton(p.player, ParticleEmitterMenu.KINDS.indexOf(ParticleEmitterBlockEntity.Kind.FLAME)), "button refused");
		p.closeMenu();
		h.succeedWhen(() -> {
			h.assertTrue(te.getCoilCharge() == 100 && te.canEmit(), "emitter: charge "+te.getCoilCharge());
			h.assertTrue(te.particleType == ParticleEmitterBlockEntity.Kind.FLAME, "emitter shows "+te.particleType);
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
