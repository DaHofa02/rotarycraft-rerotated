/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.item;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Original: Reika.ReactorCraft.Items.ItemIronFinder - held, the HUD points at magnetic ores within 8 blocks (see IronFinderOverlay).
 * The original list (iron, redstone, nickel, cobalt, certus quartz, nikolite, magnetite, teslatite, silicon, dilithium, mimichite, and
 * their nether variants via the ore dictionary) is matched through the c:ores/* tags, which also cover deepslate and nether variants.
 * The search is cached per player for half a second, as in the original. The ChromatiCraft aura pouch hook is not ported.
 */
public class IronFinderItem extends Item {

	private static final List<TagKey<Block>> ORES = java.util.stream.Stream.of("iron", "redstone", "nickel", "cobalt", "certus_quartz", "nikolite",
			"magnetite", "teslatite", "silicon", "dilithium", "mimichite")
			.map(s -> TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/"+s))).toList();

	private static final Map<UUID, OreCollection> cache = new ConcurrentHashMap<>();

	public IronFinderItem(Properties props) {
		super(props);
	}

	public static boolean isMagneticOre(BlockState s) {
		for (TagKey<Block> t : ORES)
			if (s.is(t))
				return true;
		return false;
	}

	public static Set<BlockPos> getOreNearby(Player ep, int range) {
		OreCollection c = cache.get(ep.getUUID());
		if (c == null || System.currentTimeMillis()-c.time >= 500) {
			c = new OreCollection(findOreNearby(ep, range));
			cache.put(ep.getUUID(), c);
		}
		return Collections.unmodifiableSet(c.locations);
	}

	public static Set<BlockPos> findOreNearby(Player ep, int range) {
		Set<BlockPos> m = new HashSet<>();
		Level world = ep.level();
		int x = Mth.floor(ep.getX());
		int y = Mth.floor(ep.getY()+ep.getEyeHeight());
		int z = Mth.floor(ep.getZ());
		for (int i = -range; i <= range; i++) {
			for (int j = -range; j <= range; j++) {
				for (int k = -range; k <= range; k++) {
					BlockPos p = new BlockPos(x+i, y+j, z+k);
					if (isMagneticOre(world.getBlockState(p)))
						m.add(p);
				}
			}
		}
		return m;
	}

	private record OreCollection(long time, Set<BlockPos> locations) {
		OreCollection(Set<BlockPos> locations) {
			this(System.currentTimeMillis(), locations);
		}
	}
}
