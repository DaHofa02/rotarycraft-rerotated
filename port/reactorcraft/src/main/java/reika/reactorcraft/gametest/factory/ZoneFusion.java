/*******************************************************************************
 * Practice factory zone (see Factory): the ReactorCraft fusion multiblocks and parts, built by a simulated survival player from
 * infinite chests. Not part of the original mods.
 ******************************************************************************/
package reika.reactorcraft.gametest.factory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.fusion.FusionHeaterBlockEntity;
import reika.reactorcraft.fusion.FusionInjectorBlockEntity;
import reika.reactorcraft.fusion.FusionMultiBlock;
import reika.reactorcraft.fusion.GasCollectorBlockEntity;
import reika.reactorcraft.fusion.SolenoidMagnetBlockEntity;
import reika.reactorcraft.fusion.ToroidMagnetBlockEntity;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.rotarycraft.gametest.SimPlayer;
import reika.rotarycraft.registry.RotaryItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneFusion {

	public static final List<String> BLOCKS;

	static {
		Set<String> s = new LinkedHashSet<>();
		for (String bp : List.of("solenoid", "preheater", "injector"))
			for (Block b : Factory.blueprint(bp, BlockPos.ZERO).values())
				s.add(BuiltInRegistries.BLOCK.getKey(b).toString());
		s.addAll(List.of("reactorcraft:toroid_magnet", "reactorcraft:magnetic_pipe", "reactorcraft:gas_collector", "reactorcraft:fusion_marker",
				"reactorcraft:neutron_absorber"));
		BLOCKS = List.copyOf(s);
	}

	/**
	 * The three fusion multiblocks built block by block from infinite chests (each next to what is already there, on the floor), the part
	 * that completes each one last: the solenoid (its magnet then has its multiblock), the hydrogen preheater and a plasma injector frame
	 * (placed looking south, the injector faces north). They cannot run here (a tokamak needs far more power than a test can give), so the
	 * check is that each forms, as a player sees it. Beside them: a toroid magnet aimed west with eight screwdriver clicks, a run of
	 * magnetic pipes that join, a gas collector set on the side of a furnace the player lights with coal and raw iron (it collects CO2), a
	 * fusion marker and a neutron absorber.
	 */
	@GameTest(template = Factory.BIG, timeoutTicks = 400, batch = "factory")
	public static void fusionPartsFormAndWork(GameTestHelper h) {
		SimPlayer.floor(h, 0, 0, 47, 47);
		SimPlayer p = new SimPlayer(h, GameType.SURVIVAL);
		List<String> built = new ArrayList<>();

		BlockPos sc = new BlockPos(24, 3, 24);
		Map<BlockPos, Block> sol = Factory.blueprint("solenoid", sc);
		Factory.buildFromChests(h, p, new BlockPos(1, 2, 46), sol, List.of(sc, sc.offset(7, 0, 0)), Direction.NORTH);
		SolenoidMagnetBlockEntity s = (SolenoidMagnetBlockEntity)h.getBlockEntity(sc);
		h.assertTrue(s.hasMultiBlock(), "the solenoid did not form when its last part was placed");
		h.assertTrue(h.getBlockState(sc.offset(8, 0, 0)).getValue(FusionMultiBlock.FORMED), "solenoid parts not formed");

		BlockPos hc = new BlockPos(2, 2, 2);
		Map<BlockPos, Block> heat = Factory.blueprint("preheater", hc);
		Factory.buildFromChests(h, p, new BlockPos(3, 2, 46), heat, List.of(hc.offset(2, 2, 2), hc.offset(4, 4, 4)), Direction.NORTH);
		h.assertTrue(((FusionHeaterBlockEntity)h.getBlockEntity(hc.offset(2, 2, 2))).hasMultiBlock(), "the preheater did not form");

		BlockPos ic = new BlockPos(2, 3, 38), injector = ic.east(2);
		Map<BlockPos, Block> inj = Factory.blueprint("injector", ic);
		Factory.buildFromChests(h, p, new BlockPos(5, 2, 46), inj, List.of(injector, ic, ic.above(), ic.above(2)), Direction.SOUTH);
		FusionInjectorBlockEntity ib = (FusionInjectorBlockEntity)h.getBlockEntity(injector);
		h.assertTrue(ib.getFacing() == Direction.NORTH && ib.hasMultiBlock(), "injector faces "+ib.getFacing()+", formed "+ib.hasMultiBlock());

		// single parts, from one more chest
		BlockPos chest = new BlockPos(7, 2, 46);
		Factory.infiniteChest(h, chest, new ItemStack(ReactorBlocks.TOROID_MAGNET.get()), new ItemStack(RotaryItems.SCREWDRIVER.get()),
				new ItemStack(ReactorBlocks.MAGNETIC_PIPE.get()), new ItemStack(ReactorBlocks.GAS_COLLECTOR.get()), new ItemStack(Items.FURNACE),
				new ItemStack(Items.COAL), new ItemStack(Items.RAW_IRON), new ItemStack(ReactorBlocks.FUSION_MARKER.get()),
				new ItemStack(ReactorBlocks.NEUTRON_ABSORBER.get()));
		Factory.take(p, chest, ReactorBlocks.TOROID_MAGNET.get().asItem(), 1);
		Factory.take(p, chest, RotaryItems.SCREWDRIVER.get(), 1);
		BlockPos m = new BlockPos(40, 2, 4);
		p.place(m, ReactorBlocks.TOROID_MAGNET.get(), Direction.NORTH);
		for (int i = 0; i < 8; i++)
			p.use(m, RotaryItems.SCREWDRIVER.get());
		h.assertTrue(((ToroidMagnetBlockEntity)h.getBlockEntity(m)).getAim() == ToroidMagnetBlockEntity.Aim.W, "magnet aims "+((ToroidMagnetBlockEntity)h.getBlockEntity(m)).getAim());

		Factory.take(p, chest, ReactorBlocks.MAGNETIC_PIPE.get().asItem(), 3);
		for (int x = 40; x <= 42; x++)
			p.place(new BlockPos(x, 2, 8), ReactorBlocks.MAGNETIC_PIPE.get(), Direction.NORTH);
		h.assertTrue(h.getBlockState(new BlockPos(41, 2, 8)).getValue(PipeBlock.WEST) && h.getBlockState(new BlockPos(41, 2, 8)).getValue(PipeBlock.EAST),
				"magnetic pipes not joined");

		Factory.take(p, chest, Items.FURNACE, 1);
		Factory.take(p, chest, ReactorBlocks.GAS_COLLECTOR.get().asItem(), 1);
		Factory.take(p, chest, Items.COAL, 1);
		Factory.take(p, chest, Items.RAW_IRON, 1);
		BlockPos furnace = new BlockPos(40, 2, 12), collector = furnace.east();
		p.place(furnace, Items.FURNACE, Direction.NORTH);
		p.open(furnace);
		p.putInto(0, Items.RAW_IRON, 8);
		p.putInto(1, Items.COAL, 4);
		p.closeMenu();
		p.hold(new ItemStack(ReactorBlocks.GAS_COLLECTOR.get()));
		p.player.setShiftKeyDown(true);
		p.useOn(furnace, Direction.EAST); //onto the furnace's east side
		p.player.setShiftKeyDown(false);
		GasCollectorBlockEntity gc = (GasCollectorBlockEntity)h.getBlockEntity(collector);

		Factory.take(p, chest, ReactorBlocks.FUSION_MARKER.get().asItem(), 1);
		Factory.take(p, chest, ReactorBlocks.NEUTRON_ABSORBER.get().asItem(), 1);
		p.place(new BlockPos(44, 2, 4), ReactorBlocks.FUSION_MARKER.get(), Direction.NORTH);
		p.place(new BlockPos(44, 2, 8), ReactorBlocks.NEUTRON_ABSORBER.get(), Direction.NORTH);

		h.succeedWhen(() -> {
			h.assertTrue(gc.hasFurnace() && gc.getTank().getFluidAmount() > 0, "gas collector: furnace "+gc.hasFurnace()+", "+gc.getTank().getFluidAmount()+" mB");
			Factory.assertBuilt(h, BLOCKS, new BlockPos(0, 1, 0), new BlockPos(47, 11, 47));
			p.leave();
		});
	}
}
