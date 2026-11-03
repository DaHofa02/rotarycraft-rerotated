/*******************************************************************************
 * Test tool of the private NeoForge 1.21.1 ports of RotaryCraft, ReactorCraft and ElectriCraft (not part of the original mods): the op
 * command "/reikadev compat" builds a compatibility test scene at the player (every block of the three mods with a name sign, and the
 * working combinations with vanilla and Mekanism); "/reikadev tokamak" builds the running tokamak of the fusion showcase. With the system
 * property "reactorcraft.showcase.compat" the compat scene is built at (0, -60, 200) in the showcase world on its first tick.
 ******************************************************************************/
package reika.reactorcraft.dev;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.electricraft.content.blockentity.RFBatteryBlockEntity;
import reika.electricraft.registry.ElectriBlocks;
import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.gametest.CompatTests;
import reika.rotarycraft.content.blockentity.CompactorBlockEntity;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.registry.RotaryBlocks;

@EventBusSubscriber(modid = ReactorCraft.MOD_ID)
public final class CompatScene {

	private static final boolean AUTO = System.getProperty("reactorcraft.showcase.compat") != null;
	public static final BlockPos AUTO_ORIGIN = new BlockPos(0, -60, 200);
	private static final int PER_ROW = 15;

	/** Rigs kept running (battery charge, pipe water) after a build. */
	private static BlockPos rigs;
	private static ServerLevel rigLevel;
	private static boolean autoDone;

	private CompatScene() {}

	@SubscribeEvent
	public static void commands(RegisterCommandsEvent e) {
		CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
		d.register(Commands.literal("reikadev").requires(s -> s.hasPermission(2))
				.then(Commands.literal("compat").executes(c -> {
					BlockPos p = BlockPos.containing(c.getSource().getPosition()).offset(2, 0, 2);
					int n = build(c.getSource().getLevel(), p);
					c.getSource().sendSuccess(() -> Component.literal("Compat scene with "+n+" blocks built at "+p.toShortString()), true);
					return 1;
				}))
				.then(Commands.literal("tokamak").executes(c -> {
					BlockPos p = BlockPos.containing(c.getSource().getPosition()).offset(32, 6, 0);
					FusionShowcase.buildAt(c.getSource().getLevel(), p);
					c.getSource().sendSuccess(() -> Component.literal("Tokamak built around "+p.toShortString()+" (kept supplied while the server runs)"), true);
					return 1;
				})));
	}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post e) {
		if (AUTO && !autoDone) {
			autoDone = true;
			ServerLevel w = e.getServer().overworld();
			for (int cx = -1; cx <= 4; cx++)
				for (int cz = (AUTO_ORIGIN.getZ()-24) >> 4; cz <= (AUTO_ORIGIN.getZ()+60) >> 4; cz++)
					w.setChunkForced(cx, cz, true);
			build(w, AUTO_ORIGIN);
		}
		if (rigs == null || rigLevel == null)
			return;
		if (rigLevel.getBlockEntity(rigs) instanceof RFBatteryBlockEntity a && a.getStoredEnergy() < 500000)
			a.setStoredEnergy(1000000);
		if (rigLevel.getBlockEntity(rigs.offset(0, 0, 4)) instanceof PipingBlockEntity p && p.getFluidLevel() < 500)
			p.addFluid(Fluids.WATER, 500);
		IFluidHandler t = rigLevel.getCapability(Capabilities.FluidHandler.BLOCK, rigs.offset(4, 0, 4), Direction.UP);
		if (t != null && t.getFluidInTank(0).getAmount() < 2000)
			t.fill(new FluidStack(Fluids.LAVA, 4000), IFluidHandler.FluidAction.EXECUTE);
	}

	private static Block mek(String id) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mekanism", id));
	}

	static void sign(ServerLevel w, BlockPos p, String... lines) {
		w.setBlock(p, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.NORTH), 3);
		if (w.getBlockEntity(p) instanceof SignBlockEntity s) {
			SignText t = new SignText();
			for (int i = 0; i < Math.min(4, lines.length); i++)
				t = t.setMessage(i, Component.literal(lines[i]));
			s.setText(t, true);
			s.setChanged();
		}
	}

	/** Builds the scene with its north-west corner at {@code o}; returns the number of mod blocks placed. */
	public static int build(ServerLevel w, BlockPos o) {
		List<Block> blocks = CompatTests.modBlocks();
		int rows = (blocks.size()+PER_ROW-1)/PER_ROW;
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-1, 0, -16), o.offset(3*PER_ROW+1, 5, 3*rows+2)))
			w.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
		//every block of the three mods, each on a stone pad with its name on a sign in front (north)
		for (int i = 0; i < blocks.size(); i++) {
			BlockPos p = o.offset(1+3*(i%PER_ROW), 1, 2+3*(i/PER_ROW));
			w.setBlock(p.below(), Blocks.STONE.defaultBlockState(), 3);
			w.setBlock(p, blocks.get(i).defaultBlockState(), 3);
			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blocks.get(i));
			String name = blocks.get(i).getName().getString();
			sign(w, p.north(), id.getNamespace(), name.length() > 15 ? name.substring(0, 15) : name, name.length() > 15 ? name.substring(15, Math.min(30, name.length())) : "", "");
		}
		//compatibility rigs, north of the grid
		BlockPos r = o.offset(0, 0, -14);
		rigs = r;
		rigLevel = w;
		boolean mekanism = mek("basic_universal_cable") != Blocks.AIR;
		//1. FE: ElectriCraft RF battery (redstone) -> Mekanism universal cables -> ElectriCraft RF battery
		w.setBlock(r, ElectriBlocks.RF_BATTERY.get().defaultBlockState(), 3);
		((RFBatteryBlockEntity)w.getBlockEntity(r)).setStoredEnergy(1000000);
		w.setBlock(r.north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		if (mekanism) {
			for (int x = 0; x <= 5; x++)
				w.setBlock(r.offset(x, 1, 0), mek("basic_universal_cable").defaultBlockState(), 3);
			w.setBlock(r.offset(5, 0, 0), mek("basic_universal_cable").defaultBlockState(), 3);
		}
		w.setBlock(r.offset(6, 0, 0), ElectriBlocks.RF_BATTERY.get().defaultBlockState(), 3);
		sign(w, r.offset(3, 0, -1), "FE: RF battery", "-> Mekanism", "cables -> RF", "battery");
		//2. fluids: RotaryCraft pipe with water -> Mekanism fluid tank; Mekanism tank with lava -> RotaryCraft pipe above it
		w.setBlock(r.offset(0, 0, 4), RotaryBlocks.PIPE.get().defaultBlockState(), 3);
		if (mekanism) {
			w.setBlock(r.offset(1, 0, 4), mek("basic_fluid_tank").defaultBlockState(), 3);
			w.setBlock(r.offset(4, 0, 4), mek("basic_fluid_tank").defaultBlockState(), 3);
		}
		w.setBlock(r.offset(4, 1, 4), RotaryBlocks.BEDROCK_PIPE.get().defaultBlockState(), 3);
		sign(w, r.offset(0, 0, 3), "RotaryCraft pipe", "fills Mekanism", "fluid tank");
		sign(w, r.offset(4, 0, 3), "Mekanism tank", "(lava) drained by", "bedrock pipe");
		//3. items: vanilla hoppers into and out of the RotaryCraft compactor
		BlockPos c = r.offset(9, 1, 4);
		w.setBlock(c, RotaryBlocks.COMPACTOR.get().defaultBlockState(), 3);
		((CompactorBlockEntity)w.getBlockEntity(c)).getInventory().setStackInSlot(4, new ItemStack(Items.PACKED_ICE, 3));
		w.setBlock(c.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), 3);
		((HopperBlockEntity)w.getBlockEntity(c.above())).setItem(0, new ItemStack(Items.ICE, 16));
		w.setBlock(c.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), 3);
		w.setBlock(c.below(2), Blocks.CHEST.defaultBlockState(), 3);
		sign(w, c.offset(0, -1, -1), "Hoppers in/out", "of the", "compactor");
		return blocks.size();
	}
}
