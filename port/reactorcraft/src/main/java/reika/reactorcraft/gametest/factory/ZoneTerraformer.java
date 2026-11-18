/*******************************************************************************
 * Practice factory zone (see Factory): the port's terraformer built and set by a simulated survival player. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.blockentity.TerraformerBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneTerraformer {

	public static final List<String> BLOCKS = List.of("rotarycraft:terraformer");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * Ethanol barrel -> fuel lines -> gas engine, the terraformer beside it. The player sets the target to desert and the radius to 0, puts
	 * sand in and a redstone block on top: its biome cell turns into desert (and is put back afterwards).
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 700, batch = "terraformerzone")
	public static void terraformerMakesADesert(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()),
				new ItemStack(RotaryBlocks.TERRAFORMER.get()), new ItemStack(Items.DIRT), new ItemStack(Items.SAND), new ItemStack(Items.REDSTONE_BLOCK));
		Factory.fluidBarrel(h, new BlockPos(8, 3, 5), RotaryFluids.ETHANOL.get());
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, RotaryBlocks.TERRAFORMER.get().asItem(), 1);
		Factory.takeExactly(p, chest, Items.DIRT, 1);
		Factory.takeExactly(p, chest, Items.SAND, 4);
		Factory.takeExactly(p, chest, Items.REDSTONE_BLOCK, 1);
		for (int x = 8; x >= 6; x--)
			p.place(new BlockPos(x, 2, 5), RotaryBlocks.FUEL_LINE.get(), Direction.EAST);
		p.placeSneaking(new BlockPos(6, 3, 5), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST); //looking east: drives west
		BlockPos pos = new BlockPos(5, 3, 5);
		BlockPos at = h.absolutePos(pos);
		Holder<Biome> before = TerraformerBlockEntity.cellBiome(h.getLevel(), at);
		p.place(pos.below(), Items.DIRT, Direction.EAST);
		p.place(pos, RotaryBlocks.TERRAFORMER.get(), Direction.EAST);
		h.assertTrue(p.open(pos) != null, "terraformer opens no GUI");
		p.player.containerMenu.clickMenuButton(p.player, 0);
		p.player.containerMenu.clickMenuButton(p.player, 0); //forest -> plains -> desert
		for (int i = 0; i < 4; i++)
			p.player.containerMenu.clickMenuButton(p.player, 1); //radius 4 -> 0
		p.putInto(0, Items.SAND, 4);
		p.closeMenu();
		p.placeSneaking(pos.above(), new ItemStack(Items.REDSTONE_BLOCK), Direction.EAST); //sneaking: a plain click opens the GUI
		TerraformerBlockEntity te = (TerraformerBlockEntity)h.getBlockEntity(pos);

		h.succeedWhen(() -> {
			h.assertTrue(te.getTarget() == TerraformerBlockEntity.Target.DESERT && te.getRadius() == 0, "target "+te.getTarget()+", radius "+te.getRadius());
			h.assertTrue(TerraformerBlockEntity.cellBiome(h.getLevel(), at).is(Biomes.DESERT), "biome "+TerraformerBlockEntity.cellBiome(h.getLevel(), at).getRegisteredName()+" at "+te.getPower()+" W");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 4, 8));
			h.setBlock(pos.above(), Blocks.AIR);
			TerraformerBlockEntity.setBiomeColumn(h.getLevel(), at, before);
			p.leave();
		});
	}
}
