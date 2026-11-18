/*******************************************************************************
 * Practice factory zone (see Factory): the port's portal shaft, built by simulated survival players on both sides of an end portal. Not
 * part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.content.block.ShaftBlock;
import reika.rotarycraft.content.blockentity.PortalShaftBlockEntity;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZonePortalShaft {

	public static final List<String> BLOCKS = List.of("rotarycraft:portal_shaft");

	private static final String PLATFORM = "platform_9x8x9";

	/**
	 * High above the zone (y = 100; the end has no blocks below 0) an end portal block stands in the overworld and at the same place in the
	 * end, each beside a stone ledge. A player in the end puts a steel shaft behind the end portal, taking its input from it; a player in the
	 * overworld builds a gas engine on fuel lines and a steel shaft into the portal. The shafts become portal shafts and the end one turns.
	 */
	@GameTest(template = PLATFORM, timeoutTicks = 300, batch = "portalshaftzone")
	public static void portalShaftReachesTheEnd(GameTestHelper h) {
		ServerLevel ow = h.getLevel(), end = ow.getServer().getLevel(Level.END);
		h.assertTrue(end != null, "no end dimension");
		int up = 100-h.absolutePos(BlockPos.ZERO).getY();
		BlockPos engine = new BlockPos(3, up, 4), shaft = engine.east(), portal = engine.east(2), far = engine.east(3);
		BlockPos aPortal = h.absolutePos(portal), aFar = h.absolutePos(far);
		ChunkPos c = new ChunkPos(aPortal);
		end.setChunkForced(c.x, c.z, true);
		//the ledges and the two found portals
		for (int x = 0; x <= 4; x++)
			h.setBlock(new BlockPos(x, up-2, 4), Blocks.STONE);
		h.setBlock(shaft.below(), Blocks.STONE);
		ow.setBlock(aPortal, Blocks.END_PORTAL.defaultBlockState(), 2);
		end.setBlock(aPortal, Blocks.END_PORTAL.defaultBlockState(), 2);
		end.setBlock(aFar.below(), Blocks.STONE.defaultBlockState(), 2);

		ItemStack steel = ShaftBlock.shaftItem(MaterialRegistry.STEEL, false);
		SimPlayer q = new SimPlayer(end, aFar.west(4), GameType.SURVIVAL, new GameProfile(UUID.randomUUID(), "end_builder"));
		q.give(steel.copy());
		q.placeSneaking(new BlockPos(4, 0, 0), steel.copy(), Direction.WEST); //aFar; looking west: input from the west (the portal)

		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		BlockPos chest = new BlockPos(0, 2, 8);
		Factory.infiniteChest(h, chest, new ItemStack(RotaryBlocks.FUEL_LINE.get()), new ItemStack(RotaryBlocks.GAS_ENGINE.get()), steel);
		Factory.take(p, chest, RotaryBlocks.FUEL_LINE.get().asItem(), 3);
		Factory.take(p, chest, RotaryBlocks.GAS_ENGINE.get().asItem(), 1);
		Factory.take(p, chest, steel, 1);
		for (int x = 1; x <= 3; x++)
			p.place(new BlockPos(x, up-1, 4), RotaryBlocks.FUEL_LINE.get(), Direction.WEST);
		p.placeSneaking(engine, new ItemStack(RotaryBlocks.GAS_ENGINE.get()), Direction.WEST); //looking west: drives east
		p.placeSneaking(shaft, steel.copy(), Direction.WEST); //input from the engine, output into the portal
		Factory.fluidBarrel(h, new BlockPos(1, up, 4), RotaryFluids.ETHANOL.get());

		h.succeedWhen(() -> {
			h.assertTrue(h.getBlockEntity(shaft) instanceof PortalShaftBlockEntity, "overworld shaft not a portal shaft: "+h.getBlockState(shaft));
			h.assertTrue(end.getBlockEntity(aFar) instanceof PortalShaftBlockEntity ps && ps.getOmega() == 512 && ps.getTorque() == 128,
					"end shaft: "+end.getBlockState(aFar)+(end.getBlockEntity(aFar) instanceof PortalShaftBlockEntity ps2 ? " at "+ps2.getOmega()+" rad/s" : ""));
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 2, 0), new BlockPos(8, up+1, 8));
			//tidy up the sky and the end
			for (int x = 0; x <= 5; x++)
				for (int y = up-2; y <= up; y++)
					h.setBlock(new BlockPos(x, y, 4), Blocks.AIR);
			ow.setBlock(aPortal, Blocks.AIR.defaultBlockState(), 2);
			end.setBlock(aFar, Blocks.AIR.defaultBlockState(), 3);
			end.setBlock(aFar.below(), Blocks.AIR.defaultBlockState(), 2);
			end.setBlock(aPortal, Blocks.AIR.defaultBlockState(), 2);
			end.setChunkForced(c.x, c.z, false);
			p.leave();
			q.leave();
		});
	}
}
