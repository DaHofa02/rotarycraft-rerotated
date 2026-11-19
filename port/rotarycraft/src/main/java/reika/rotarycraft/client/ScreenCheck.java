/*******************************************************************************
 * Dev-only check for the private RotaryCraft port. Not part of the original mod; inactive unless the system property
 * "rotarycraft.screencheck" is set (only the screenCheck run configurations set it).
 ******************************************************************************/
package reika.rotarycraft.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import reika.rotarycraft.RotaryCraft;

/**
 * Opens the screen of every block of the ports in a real client: places each block (with a block entity) of RotaryCraft, ReactorCraft,
 * ElectriCraft and the ME addon next to the player in turn, right-clicks it with an empty hand the way a player does, waits for the
 * screen, lets it render for a few frames and closes it; then opens each mod's handbook the same way. Results go to the log ("SCREENCHECK ...") and to run/screencheck.txt; a screen
 * that cannot be opened shows up as "Failed to open a screen" in the log, one that throws while drawing crashes the run. Then it quits.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, value = Dist.CLIENT)
public final class ScreenCheck {

	private static final boolean ACTIVE = System.getProperty("rotarycraft.screencheck") != null;
	private static final Set<String> MODS = Set.of("rotarycraft", "reactorcraft", "electricraft", "rotaryme");
	/** Ticks between placing and clicking (the block change must reach the client first), and from the click to the check. */
	private static final int SETTLE = 5, WAIT = 12;

	private static List<ResourceLocation> blocks;
	private static final List<String> results = new ArrayList<>();
	private static int index = -1;
	private static int ticks;
	private static int waiting;
	private static int timer;
	private static BlockPos base;

	private ScreenCheck() {}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post e) {
		if (!ACTIVE)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.getSingleplayerServer() == null) {
			if (++waiting%100 == 0) //tells what the client is stuck on before a world is open
				RotaryCraft.LOGGER.info("SCREENCHECK waiting for a world, screen {}", mc.screen == null ? null : mc.screen.getClass().getName());
			return;
		}
		ticks++;
		if (ticks < 60)
			return;
		if (blocks == null) {
			blocks = new ArrayList<>();
			for (Block b : BuiltInRegistries.BLOCK) {
				ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
				if (MODS.contains(id.getNamespace()) && b instanceof EntityBlock)
					blocks.add(id);
			}
			base = mc.player.blockPosition().offset(3, 0, 0);
			mc.getSingleplayerServer().execute(() -> {
				ServerPlayer sp = player(mc);
				sp.setGameMode(GameType.CREATIVE);
				sp.getInventory().clearContent();
			});
			RotaryCraft.LOGGER.info("SCREENCHECK {} blocks with block entities", blocks.size());
			timer = 0;
			return;
		}
		timer++;
		boolean onBlock = index >= 0 && index < blocks.size();
		if (onBlock && timer == SETTLE) {
			ResourceLocation id = blocks.get(index);
			mc.getSingleplayerServer().execute(() -> click(mc, id));
		}
		if (onBlock && timer == SETTLE+WAIT) {
			Screen s = mc.screen;
			String r = s == null ? "no screen" : s instanceof AbstractContainerScreen<?> ? "screen "+s.getClass().getSimpleName() : "other "+s.getClass().getSimpleName();
			results.add(blocks.get(index)+" -> "+r);
			RotaryCraft.LOGGER.info("SCREENCHECK {} -> {}", blocks.get(index), r);
			if (s != null)
				mc.player.closeContainer();
		}
		if (onBlock && timer < SETTLE+WAIT+3)
			return;
		if (index >= blocks.size()) {
			books(mc);
			return;
		}
		index++;
		timer = 0;
		if (index >= blocks.size())
			return;
		ResourceLocation id = blocks.get(index);
		mc.getSingleplayerServer().execute(() -> place(mc, id));
	}

	private static ServerPlayer player(Minecraft mc) {
		return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
	}

	/** Server thread: clears the spot and places the block. */
	private static void place(Minecraft mc, ResourceLocation id) {
		ServerPlayer sp = player(mc);
		ServerLevel world = sp.serverLevel();
		for (BlockPos p : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(1, 3, 1)))
			world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
		world.setBlockAndUpdate(base.below(), Blocks.STONE.defaultBlockState());
		try {
			world.setBlockAndUpdate(base, BuiltInRegistries.BLOCK.get(id).defaultBlockState());
		}
		catch (Throwable t) {
			RotaryCraft.LOGGER.error("SCREENCHECK {} threw on the server when placed", id, t);
			results.add(id+" -> SERVER ERROR "+t);
		}
	}

	/** Server thread: right-clicks the block's top with an empty hand, as a player does. */
	private static void click(Minecraft mc, ResourceLocation id) {
		ServerPlayer sp = player(mc);
		try {
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(base).add(0, 0.5, 0), Direction.UP, base, false);
			sp.gameMode.useItemOn(sp, sp.serverLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
		}
		catch (Throwable t) {
			RotaryCraft.LOGGER.error("SCREENCHECK {} threw on the server when used", id, t);
			results.add(id+" -> SERVER ERROR "+t);
		}
	}

	private static final String[] BOOKS = {"rotarycraft", "reactorcraft", "electricraft"};
	private static int book = -1;

	/** After the blocks: opens each installed mod's handbook (an item screen, not a block's), lets it draw, and closes it. */
	private static void books(Minecraft mc) {
		if (book >= 0 && timer == WAIT) {
			Screen s = mc.screen;
			String r = s instanceof reika.rotarycraft.client.screen.HandbookScreen ? "handbook "+HandbookClient.load(mc, BOOKS[book]).size()+" entries" : "no handbook screen";
			results.add(BOOKS[book]+":handbook -> "+r);
			RotaryCraft.LOGGER.info("SCREENCHECK {}:handbook -> {}", BOOKS[book], r);
			mc.setScreen(null);
		}
		if (book >= 0 && timer < WAIT+3)
			return;
		do
			book++;
		while (book < BOOKS.length && !net.neoforged.fml.ModList.get().isLoaded(BOOKS[book]));
		timer = 0;
		if (book >= BOOKS.length) {
			finish(mc);
			return;
		}
		HandbookClient.open(BOOKS[book]);
	}

	private static void finish(Minecraft mc) {
		long opened = results.stream().filter(r -> r.contains("-> screen")).count();
		RotaryCraft.LOGGER.info("SCREENCHECK done: {} blocks, {} screens opened", results.size(), opened);
		try {
			Files.write(Path.of("screencheck.txt"), results);
		}
		catch (IOException ex) {
			RotaryCraft.LOGGER.error("SCREENCHECK could not write the results", ex);
		}
		mc.stop();
	}
}
