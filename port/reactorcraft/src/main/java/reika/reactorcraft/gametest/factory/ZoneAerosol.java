/*******************************************************************************
 * Practice factory zone (see Factory): the RotaryCraft aerosolizer built and run by a simulated survival player. Not part of the
 * original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.AerosolizerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneAerosol {

	public static final List<String> BLOCKS = List.of("rotarycraft:aerosolizer");

	private static final String PLATFORM = "platform_9x8x9";

	/** Ethanol barrel -> fuel lines -> gas engine -> aerosolizer. The player puts a potion of swiftness in and gets Speed from it. */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "aerosolizer")
	public static void aerosolizerSpeedsThePlayer(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		ItemStack swift = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.AEROSOLIZER.get()), new ItemStack(Items.DIRT), swift);
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.AEROSOLIZER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.POTION, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST);
		BlockPos pos = new BlockPos(5, 3, 5);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.AEROSOLIZER.get(), Direction.EAST);
		AerosolizerBlockEntity te = (AerosolizerBlockEntity)h.getBlockEntity(pos);
		h.assertTrue(p.open(pos) != null, "aerosolizer opens no GUI");
		p.putInto(0, Items.POTION, 1);
		p.closeMenu();
		p.join(); //only players in the level are found in the room

		h.succeedWhen(() -> {
			h.assertTrue(te.getPotionLevel(0) == 1, "store "+te.getPotionLevel(0));
			h.assertTrue(p.player.hasEffect(MobEffects.MOVEMENT_SPEED), "player has no speed; "+te.power+" W");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			p.leave();
		});
	}
}
