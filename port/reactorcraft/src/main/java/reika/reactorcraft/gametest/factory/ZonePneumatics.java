/*******************************************************************************
 * Practice factory zone (see Factory): the port's air compressor and pneumatic engine (optional PneumaticCraft integration) set up by a
 * simulated survival player. Not part of the original mods.
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
import reika.rotarycraft.content.blockentity.AirCompressorBlockEntity;
import reika.rotarycraft.content.blockentity.PneumaticEngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryBlocks;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZonePneumatics {

	public static final List<String> BLOCKS = List.of("rotarycraft:air_compressor", "rotarycraft:pneumatic_engine");

	private static final String PLATFORM = "platform_9x8x9";

	/** Gas engine -> air compressor -> pneumatic engine, all driving west; the player sets the engine to 256 rad/s in its GUI. */
	@GameTest(template = PLATFORM, timeoutTicks = 400, batch = "factory")
	public static void compressorDrivesAPneumaticEngine(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 8, 8);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		p.give(new ItemStack(RotaryBlocks.GAS_ENGINE.get()));
		p.give(new ItemStack(RotaryBlocks.AIR_COMPRESSOR.get()));
		p.give(new ItemStack(RotaryBlocks.PNEUMATIC_ENGINE.get()));
		BlockPos gas = new BlockPos(6, 2, 4), comp = new BlockPos(5, 2, 4), eng = new BlockPos(4, 2, 4);
		p.placeSneaking(gas, new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.EAST); //drives west
		((EngineBlockEntity)h.getBlockEntity(gas)).addFuel(50000); //what a fuel line would bring
		p.placeSneaking(comp, new ItemStack(RotaryBlocks.AIR_COMPRESSOR.get()), Direction.EAST); //power from the east, air out west
		p.placeSneaking(eng, new ItemStack(RotaryBlocks.PNEUMATIC_ENGINE.get()), Direction.EAST); //air in at the east, drives west
		h.assertTrue(p.open(eng) != null, "pneumatic engine opens no GUI");
		for (int i = 0; i < 9; i++)
			p.player.containerMenu.clickMenuButton(p.player, 1);
		p.closeMenu();
		AirCompressorBlockEntity c = (AirCompressorBlockEntity)h.getBlockEntity(comp);
		PneumaticEngineBlockEntity e = (PneumaticEngineBlockEntity)h.getBlockEntity(eng);
		h.succeedWhen(() -> {
			h.assertTrue(e.getOmega() == 256, "engine "+e.getOmega()+" rad/s, air "+e.getAir()+"; compressor "+c.getPower()+" W, pumped "+c.pumped);
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, 3, 8));
			p.leave();
		});
	}
}
