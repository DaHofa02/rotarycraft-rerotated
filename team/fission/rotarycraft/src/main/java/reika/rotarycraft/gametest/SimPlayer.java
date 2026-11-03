/*******************************************************************************
 * Test harness of the private NeoForge 1.21.1 ports (not part of the original mods): a simulated player for "live" GameTest scenarios.
 ******************************************************************************/
package reika.rotarycraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A server player in the GameTest world (survival or creative) that does what a player does, through the same code paths as a real
 * client's packets: placing blocks from items while facing a direction (so the machines take their orientation from the look as in the
 * game), right-clicking blocks with an item or an empty hand (buckets, screwdriver, GUIs), clicking slots in open menus, crafting from
 * its inventory in a crafting grid, and breaking blocks with the held tool (drops picked up into the inventory). In survival everything
 * used comes out of its inventory; in creative placing does not use up items, as in the game.
 */
public final class SimPlayer {

	/** The test it acts in (null outside tests, e.g. in a scene built in a world). */
	public final GameTestHelper helper;
	private final net.minecraft.server.level.ServerLevel level;
	/** World position of relative (0, 0, 0). */
	private final BlockPos origin;
	public final ServerPlayer player;
	public final GameType mode;

	public SimPlayer(GameTestHelper h, GameType mode) {
		this(h, h.getLevel(), h.absolutePos(BlockPos.ZERO), mode, new GameProfile(UUID.randomUUID(), "sim-"+mode.getName()));
	}

	/**
	 * Outside a test (a scene built in a world): positions are relative to {@code origin}; {@code as} is whose hands do it (the real
	 * player's profile, so they own what is placed).
	 */
	public SimPlayer(net.minecraft.server.level.ServerLevel level, BlockPos origin, GameType mode, GameProfile as) {
		this(null, level, origin, mode, as);
	}

	private SimPlayer(GameTestHelper helper, net.minecraft.server.level.ServerLevel level, BlockPos origin, GameType mode, GameProfile as) {
		this.helper = helper;
		this.level = level;
		this.origin = origin;
		this.mode = mode;
		if (level.getServer() instanceof net.minecraft.gametest.framework.GameTestServer)
			offlineProfileCache(level.getServer());
		//NeoForge's fake player: a real ServerPlayer whose packets go nowhere (so GUIs open without a client)
		player = new Fake(level, as);
		player.setGameMode(mode);
		this.moveNear(BlockPos.ZERO);
	}

	/**
	 * The GameTest server runs without account services (no profile cache), which a real server has; mods that look up a placer's
	 * name when a player places their block (Mekanism's security owner) need one. This gives the test server an offline cache that
	 * knows no names (lookups find nothing, as for an unknown player).
	 */
	private static synchronized void offlineProfileCache(net.minecraft.server.MinecraftServer server) {
		if (server.getProfileCache() != null)
			return;
		try {
			com.mojang.authlib.GameProfileRepository none = (names, callback) -> {
				for (String n : names)
					callback.onProfileLookupFailed(n, new IllegalStateException("offline test server"));
			};
			java.io.File f = java.io.File.createTempFile("gametest_usercache", ".json");
			f.deleteOnExit();
			var cache = new net.minecraft.server.players.GameProfileCache(none, f);
			var services = new net.minecraft.server.Services(null, com.mojang.authlib.yggdrasil.ServicesKeySet.EMPTY, none, cache);
			java.lang.reflect.Field fd = net.minecraft.server.MinecraftServer.class.getDeclaredField("services");
			fd.setAccessible(true);
			fd.set(server, services);
		}
		catch (ReflectiveOperationException | java.io.IOException e) {
			throw new IllegalStateException("could not give the test server a profile cache", e);
		}
	}

	public boolean survival() {
		return mode == GameType.SURVIVAL;
	}

	/** Leaves the server (call before the test ends). */
	public void leave() {
		player.closeContainer();
		player.discard();
	}

	private BlockPos abs(BlockPos rel) {
		return rel.offset(origin);
	}

	/** Stands two blocks south-east of the block (within reach and GUI distance), feet one block below it. */
	public void moveNear(BlockPos rel) {
		BlockPos a = abs(rel);
		player.moveTo(a.getX()+2.5, a.getY(), a.getZ()+2.5, player.getYRot(), player.getXRot());
	}

	/** Looks horizontally in that direction (machines placed now take their orientation from it). */
	public void look(Direction d) {
		float yaw = switch (d) {
			case SOUTH -> 0;
			case WEST -> 90;
			case NORTH -> 180;
			case EAST -> -90;
			default -> player.getYRot();
		};
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		player.setXRot(d == Direction.UP ? -90 : d == Direction.DOWN ? 90 : 0);
	}

	public void give(ItemStack is) {
		if (!player.getInventory().add(is.copy()))
			throw new IllegalStateException("inventory full");
	}

	public void give(ItemLike i, int n) {
		this.give(new ItemStack(i, n));
	}

	public int count(Item i) {
		return player.getInventory().countItem(i);
	}

	/** Puts a stack of that item from the inventory into the main hand (creative: gets one if missing). */
	public ItemStack hold(Item i) {
		int slot = -1;
		for (int k = 0; k < player.getInventory().getContainerSize(); k++)
			if (player.getInventory().getItem(k).is(i)) {
				slot = k;
				break;
			}
		if (slot < 0) {
			if (survival())
				throw new IllegalStateException("survival player has no "+i);
			return this.creativePick(new ItemStack(i));
		}
		return this.holdSlot(slot);
	}

	/** Holds exactly this stack (it must be in the inventory; creative adds it). */
	public ItemStack hold(ItemStack is) {
		for (int k = 0; k < player.getInventory().getContainerSize(); k++)
			if (ItemStack.isSameItemSameComponents(player.getInventory().getItem(k), is))
				return this.holdSlot(k);
		if (survival())
			throw new IllegalStateException("survival player has no "+is);
		return this.creativePick(is);
	}

	/** Creative: gets the stack as pick-block does - into a free slot, or in place of what the selected hotbar slot holds. */
	private ItemStack creativePick(ItemStack is) {
		int free = player.getInventory().getFreeSlot();
		int slot = free >= 0 ? free : player.getInventory().selected;
		player.getInventory().setItem(slot, is.copy());
		return this.holdSlot(slot);
	}

	private ItemStack holdSlot(int slot) {
		if (slot < 9) {
			player.getInventory().selected = slot;
		}
		else {
			int sel = player.getInventory().selected;
			ItemStack a = player.getInventory().getItem(sel);
			player.getInventory().setItem(sel, player.getInventory().getItem(slot));
			player.getInventory().setItem(slot, a);
		}
		return player.getMainHandItem();
	}

	public void emptyHand() {
		for (int k = 0; k < 9; k++)
			if (player.getInventory().getItem(k).isEmpty()) {
				player.getInventory().selected = k;
				return;
			}
		int sel = player.getInventory().selected;
		for (int k = 9; k < 36; k++)
			if (player.getInventory().getItem(k).isEmpty()) {
				player.getInventory().setItem(k, player.getInventory().getItem(sel));
				player.getInventory().setItem(sel, ItemStack.EMPTY);
				return;
			}
		if (!this.survival()) { //creative: put away by clearing the slot (it can be picked again at will)
			player.getInventory().setItem(sel, ItemStack.EMPTY);
			return;
		}
		throw new IllegalStateException("inventory full");
	}

	/** Right-clicks the given face of the block with whatever is in the main hand. */
	public InteractionResult useOn(BlockPos rel, Direction face) {
		BlockPos a = abs(rel);
		this.moveNear(rel);
		Vec3 hit = Vec3.atCenterOf(a).add(face.getStepX()*0.5, face.getStepY()*0.5, face.getStepZ()*0.5);
		BlockHitResult r = new BlockHitResult(hit, face, a, false);
		try {
			return player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, r);
		}
		catch (RuntimeException e) {
			StringBuilder where = new StringBuilder();
			for (StackTraceElement f : e.getStackTrace())
				if (where.length() < 900)
					where.append("\n  at ").append(f);
			throw new IllegalStateException("right-clicking "+rel+" ("+face+") with "+player.getMainHandItem()+" threw "+e+where, e);
		}
	}

	/** Right-click on the top of the block with the held item (buckets, tools, GUIs with an empty hand). */
	public InteractionResult use(BlockPos rel) {
		return this.useOn(rel, Direction.UP);
	}

	/** Right-clicks the block with that item held. */
	public InteractionResult use(BlockPos rel, Item i) {
		this.hold(i);
		return this.use(rel);
	}

	/** Right-clicks the air with the held item. */
	public InteractionResult useItem() {
		return player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);
	}

	/**
	 * Places the item as a block at {@code rel} the way a player does: looking {@code look}, clicking the top of the block below it
	 * (which must be solid or another block); fails the test if nothing was placed.
	 */
	public void place(BlockPos rel, ItemStack is, Direction look) {
		this.hold(is);
		this.look(look);
		BlockPos against = rel.below();
		Direction face = Direction.UP;
		if (level.getBlockState(abs(against)).isAir()) { //no floor below: click the face of a neighbouring block
			for (Direction d : Direction.values()) {
				BlockPos n = rel.relative(d);
				if (!level.getBlockState(abs(n)).isAir() && !level.getBlockState(abs(n)).canBeReplaced()) {
					against = n;
					face = d.getOpposite();
					break;
				}
			}
		}
		InteractionResult r = this.useOn(against, face);
		if (Boolean.getBoolean("sim.debug"))
			System.out.println("SIMDEBUG place "+is+" at "+rel+": "+r+" below="+level.getBlockState(abs(rel.below()))+" at="+level.getBlockState(abs(rel))
					+" above="+level.getBlockState(abs(rel.above()))+" hand="+player.getMainHandItem()+" pos="+player.position());
		if (level.getBlockState(abs(rel)).isAir())
			throw new IllegalStateException("could not place "+is+" at "+rel+": "+r);
	}

	/** Places while sneaking, as a player does to set a block onto a machine that would take the click itself. */
	public void placeSneaking(BlockPos rel, ItemStack is, Direction look) {
		player.setShiftKeyDown(true);
		try {
			this.place(rel, is, look);
		}
		finally {
			player.setShiftKeyDown(false);
		}
	}

	public void place(BlockPos rel, ItemLike i, Direction look) {
		this.place(rel, new ItemStack(i), look);
	}

	/**
	 * Builds the blueprint (relative positions) block by block from items, always next to something already there (floor or placed
	 * blocks), the blocks in {@code last} at the very end in that order (for structures whose last part completes them).
	 */
	public void build(java.util.Map<BlockPos, net.minecraft.world.level.block.Block> blueprint, List<BlockPos> last, Direction look) {
		java.util.Set<BlockPos> todo = new java.util.LinkedHashSet<>(blueprint.keySet());
		todo.removeAll(last);
		List<BlockPos> props = new ArrayList<>();
		while (!todo.isEmpty()) {
			BlockPos next = null;
			for (BlockPos q : todo) {
				for (Direction d : Direction.values()) {
					var st = level.getBlockState(abs(q.relative(d)));
					if (!st.isAir() && !st.canBeReplaced()) {
						next = q;
						break;
					}
				}
				if (next != null)
					break;
			}
			if (next == null) { //hanging part: a player props it up with a dirt column from below, removed again afterwards
				next = todo.iterator().next();
				BlockPos b = next.below();
				while (level.getBlockState(abs(b)).isAir() && b.getY() > 0)
					b = b.below();
				for (BlockPos q = b.above(); q.getY() < next.getY(); q = q.above()) {
					if (blueprint.containsKey(q)) {
						this.place(q, new ItemStack(blueprint.get(q)), look);
						todo.remove(q);
					}
					else {
						if (this.survival())
							this.give(Items.DIRT, 1);
						this.place(q, Items.DIRT, look);
						props.add(q);
					}
				}
			}
			this.place(next, new ItemStack(blueprint.get(next)), look);
			todo.remove(next);
		}
		for (int i = props.size()-1; i >= 0; i--)
			this.mine(props.get(i));
		for (BlockPos q : last)
			this.place(q, new ItemStack(blueprint.get(q)), look);
	}

	/** Breaks the block with the held item and picks up what drops; returns the drops. */
	public List<ItemStack> mine(BlockPos rel) {
		BlockPos a = abs(rel);
		this.moveNear(rel);
		player.gameMode.destroyBlock(a);
		List<ItemStack> got = new ArrayList<>();
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(a).inflate(2))) {
			got.add(e.getItem().copy());
			player.getInventory().add(e.getItem().copy());
			e.discard();
		}
		return got;
	}

	public List<ItemStack> mine(BlockPos rel, Item tool) {
		this.hold(tool);
		return this.mine(rel);
	}

	/** Opens the block's GUI with an empty hand; returns the open menu (fails if none opened). */
	public AbstractContainerMenu open(BlockPos rel) {
		this.emptyHand();
		InteractionResult r = this.use(rel);
		if (player.containerMenu == player.inventoryMenu)
			throw new IllegalStateException("no GUI opened at "+rel+" ("+level.getBlockState(abs(rel))+"): "+r+", hand "+player.getMainHandItem()
					+", offhand "+player.getOffhandItem()+", shift "+player.isShiftKeyDown()+", dist "+Math.sqrt(player.distanceToSqr(Vec3.atCenterOf(abs(rel)))));
		return player.containerMenu;
	}

	public AbstractContainerMenu menu() {
		return player.containerMenu;
	}

	/** A menu slot index of the player's inventory slot k (0-35) in the open menu. */
	public int menuSlotOf(int invSlot) {
		AbstractContainerMenu m = player.containerMenu;
		for (int i = 0; i < m.slots.size(); i++) {
			Slot s = m.slots.get(i);
			if (s.container == player.getInventory() && s.getContainerSlot() == invSlot)
				return i;
		}
		throw new IllegalStateException("inventory slot "+invSlot+" not in the menu");
	}

	public void click(int slot, int button, ClickType type) {
		player.containerMenu.clicked(slot, button, type, player);
	}

	/** Takes {@code n} of the item from the inventory with the mouse and drops them into the menu slot (left-click, or right-clicks for less than a stack). */
	public void putInto(int menuSlot, Item i, int n) {
		int inv = -1;
		for (int k = 0; k < 36; k++)
			if (player.getInventory().getItem(k).is(i)) {
				inv = k;
				break;
			}
		if (inv < 0) {
			if (survival())
				throw new IllegalStateException("survival player has no "+i);
			int free = player.getInventory().getFreeSlot(); //creative: as pick-block, in place of the selected slot when full
			player.getInventory().setItem(free >= 0 ? free : player.getInventory().selected, new ItemStack(i, n));
			this.putInto(menuSlot, i, n);
			return;
		}
		int from = this.menuSlotOf(inv);
		this.click(from, 0, ClickType.PICKUP); //whole stack onto the cursor
		ItemStack cursor = player.containerMenu.getCarried();
		if (n >= cursor.getCount())
			this.click(menuSlot, 0, ClickType.PICKUP);
		else
			for (int k = 0; k < n; k++)
				this.click(menuSlot, 1, ClickType.PICKUP); //one at a time
		if (!player.containerMenu.getCarried().isEmpty())
			this.click(from, 0, ClickType.PICKUP); //the rest back
	}

	/** Shift-clicks the menu slot (moves its stack into the inventory or the machine). */
	public void shiftClick(int menuSlot) {
		this.click(menuSlot, 0, ClickType.QUICK_MOVE);
	}

	/** Puts what is on the mouse cursor into a free inventory slot (as a player clicks it there). */
	public void dropCursorIntoInventory() {
		if (player.containerMenu.getCarried().isEmpty())
			return;
		for (int k = 0; k < 36; k++)
			if (player.getInventory().getItem(k).isEmpty()) {
				this.click(this.menuSlotOf(k), 0, ClickType.PICKUP);
				return;
			}
		throw new IllegalStateException("no free inventory slot for "+player.containerMenu.getCarried());
	}

	public void closeMenu() {
		player.closeContainer();
	}

	/**
	 * Crafts the recipe in the open 3x3 crafting menu (vanilla crafting table or RotaryCraft worktable: grid slots 1-9, result slot 0):
	 * lays the ingredients into the grid from the inventory (creative: they are taken from the creative supply) and takes the result.
	 */
	public ItemStack craft(ResourceLocation recipeId, int gridFirstSlot, int resultSlot) {
		RecipeHolder<?> holder = level.getRecipeManager().byKey(recipeId).orElseThrow(() -> new IllegalStateException("no recipe "+recipeId));
		Recipe<?> r = holder.value();
		List<Ingredient> layout = new ArrayList<>();
		CraftingRecipe cr = r instanceof CraftingRecipe c ? c : null;
		if (r instanceof reika.rotarycraft.content.recipe.WorktableRecipe w)
			layout.addAll(w.getGridLayout());
		else if (cr instanceof ShapedRecipe s) {
			for (int y = 0; y < 3; y++)
				for (int x = 0; x < 3; x++)
					layout.add(x < s.getWidth() && y < s.getHeight() ? s.getIngredients().get(x+y*s.getWidth()) : Ingredient.EMPTY);
		}
		else
			layout.addAll(r.getIngredients());
		for (int k = 0; k < layout.size(); k++) {
			Ingredient ing = layout.get(k);
			if (ing.isEmpty())
				continue;
			Item use = null;
			for (ItemStack opt : ing.getItems())
				if (this.count(opt.getItem()) > 0) {
					use = opt.getItem();
					break;
				}
			if (use == null) {
				if (survival())
					throw new IllegalStateException("survival player lacks an ingredient of "+recipeId+" ("+ing.getItems()[0]+")");
				use = ing.getItems()[0].getItem();
			}
			this.putInto(gridFirstSlot+k, use, 1);
		}
		//vanilla shows the result before taking it; the worktable crafts when its (empty) main output is clicked
		ItemStack out = r.getResultItem(level.registryAccess()).copy();
		int before = this.countMatching(out);
		this.shiftClick(resultSlot);
		this.dropCursorIntoInventory();
		if (this.countMatching(out) <= before)
			throw new IllegalStateException("crafting "+recipeId+" gave nothing (grid: "+this.gridContents(gridFirstSlot)+", cursor "+player.containerMenu.getCarried()
					+", main output "+player.containerMenu.getSlot(resultSlot).getItem()+")");
		for (int k = 0; k < player.getInventory().getContainerSize(); k++) //the crafted stack itself (with its components)
			if (player.getInventory().getItem(k).is(out.getItem()))
				return player.getInventory().getItem(k).copyWithCount(1);
		return out;
	}

	private int countMatching(ItemStack is) {
		int n = 0;
		for (int k = 0; k < player.getInventory().getContainerSize(); k++)
			if (player.getInventory().getItem(k).is(is.getItem()))
				n += player.getInventory().getItem(k).getCount();
		return n;
	}

	private String gridContents(int first) {
		StringBuilder sb = new StringBuilder();
		for (int k = 0; k < 9; k++)
			sb.append(player.containerMenu.getSlot(first+k).getItem()).append(k%3 == 2 ? " | " : ", ");
		return sb.toString();
	}

	/** Survival: gives the ingredients of one craft of the recipe (as if gathered); creative needs none. */
	public void gatherFor(ResourceLocation recipeId) {
		if (!survival())
			return;
		Recipe<?> r = level.getRecipeManager().byKey(recipeId).orElseThrow(() -> new IllegalStateException("no recipe "+recipeId)).value();
		for (Ingredient ing : r.getIngredients())
			if (!ing.isEmpty())
				this.give(ing.getItems()[0].copyWithCount(1));
	}

	/** Lays a stone floor (y = 1) under the given area, for building on at y = 2. */
	public static void floor(GameTestHelper h, int x0, int z0, int x1, int z1) {
		for (int x = x0; x <= x1; x++)
			for (int z = z0; z <= z1; z++)
				h.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.STONE);
	}

	/** NeoForge's fake player (packets go nowhere), but one that opens menus on the server as a real player does. */
	private static final class Fake extends net.neoforged.neoforge.common.util.FakePlayer {
		private int counter;

		Fake(ServerLevel level, GameProfile profile) {
			super(level, profile);
		}

		@Override
		public java.util.OptionalInt openMenu(@javax.annotation.Nullable net.minecraft.world.MenuProvider provider,
				@javax.annotation.Nullable java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf> extra) {
			if (provider == null)
				return java.util.OptionalInt.empty();
			if (containerMenu != inventoryMenu)
				this.doCloseContainer();
			counter = counter%100+1;
			AbstractContainerMenu m = provider.createMenu(counter, this.getInventory(), this);
			if (m == null)
				return java.util.OptionalInt.empty();
			containerMenu = m;
			net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open(this, m));
			return java.util.OptionalInt.of(counter);
		}
	}

	public ServerLevel level() {
		return level;
	}
}
