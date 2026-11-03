/*******************************************************************************
 * Middle-click (pick block) of the private NeoForge 1.21.1 ports of RotaryCraft, ReactorCraft and ElectriCraft. Not part of the
 * original mods' code; it checks the original getPickBlock behaviour: picking a machine gives the machine as it was crafted - its
 * variant (gearbox material and ratio, shaft material, flywheel type, bedrock versions ...), not its contents (charge, fluid, damage,
 * which vanilla copies on Ctrl + middle-click).
 ******************************************************************************/
package reika.reactorcraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.rotarycraft.gametest.SimPlayer;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class PickBlockTests {

	private static final String BIG = "platform_48x12x48";
	private static final Set<String> MODS = Set.of("rotarycraft", "reactorcraft", "electricraft");
	/** Contents and settings, not the variant: not part of a pick (vanilla copies them with Ctrl + middle-click). */
	private static final Set<String> CONTENTS = Set.of("rotarycraft:gearbox_data", "rotarycraft:reservoir_data", "rotarycraft:jet_damage",
			"electricraft:battery_energy", "electricraft:fuse_limit", "electricraft:motor_amp");

	/** Gases: as air, nothing to pick (original BlockSteam had no item either). */
	private static final Set<String> GAS = Set.of("steam");

	private static List<ItemStack> tabStacks(GameTestHelper h) {
		CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess());
		List<ItemStack> li = new ArrayList<>();
		for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
			ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
			if (id != null && MODS.contains(id.getNamespace()))
				for (ItemStack is : tab.getDisplayItems())
					if (is.getItem() instanceof BlockItem)
						li.add(is.copyWithCount(1));
		}
		return li;
	}

	private static ItemStack pick(GameTestHelper h, SimPlayer p, BlockPos rel) {
		BlockPos a = h.absolutePos(rel);
		BlockState s = h.getLevel().getBlockState(a);
		return s.getCloneItemStack(new BlockHitResult(Vec3.atCenterOf(a), Direction.UP, a, false), h.getLevel(), a, p.player);
	}

	private static String variant(ItemStack is) {
		java.util.TreeMap<String, String> m = new java.util.TreeMap<>();
		for (var e : is.getComponentsPatch().entrySet()) {
			ResourceLocation k = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(e.getKey());
			if (k != null && MODS.contains(k.getNamespace()) && !CONTENTS.contains(k.toString()))
				m.put(k.getPath(), e.getValue().map(Object::toString).orElse("-"));
		}
		StringBuilder sb = new StringBuilder();
		m.forEach((k, v) -> sb.append(k).append('=').append(v).append(' '));
		return sb.toString().trim();
	}

	private static BlockPos slot(int i) {
		return new BlockPos(1+2*(i%23), 2, 1+2*(i/23));
	}

	/** Every block stack of the three mods' creative tabs, set down by a (creative) player and picked again: the same item and variant. */
	@GameTest(template = BIG, timeoutTicks = 100, batch = "pick")
	public static void middleClickGivesThePlacedVariant(GameTestHelper h) {
		SimPlayer p = new SimPlayer(h, GameType.CREATIVE);
		SimPlayer.floor(h, 0, 0, 47, 47);
		List<ItemStack> stacks = tabStacks(h);
		List<String> bad = new ArrayList<>();
		int n = 0, checked = 0;
		for (ItemStack is : stacks) {
			if (n >= 23*23)
				break;
			BlockPos at = slot(n++);
			String id = BuiltInRegistries.ITEM.getKey(is.getItem())+(variant(is).isEmpty() ? "" : " {"+variant(is)+"}");
			try {
				p.place(at, is, Direction.NORTH);
			}
			catch (RuntimeException e) {
				bad.add(id+": could not be placed ("+e.getMessage()+")");
				continue;
			}
			ItemStack got = pick(h, p, at);
			checked++;
			if (got.isEmpty())
				bad.add(id+": middle-click gives nothing");
			else if (got.getItem() != is.getItem())
				bad.add(id+": middle-click gives "+BuiltInRegistries.ITEM.getKey(got.getItem()));
			else if (!variant(got).equals(variant(is)))
				bad.add(id+": middle-click gives {"+variant(got)+"}");
			for (BlockPos q : BlockPos.betweenClosed(at.offset(-1, 0, -1), at.offset(1, 4, 1)))
				h.setBlock(q, Blocks.AIR);
		}
		p.leave();
		bad.forEach(b -> ReactorCraft.LOGGER.error("PICK {}", b));
		ReactorCraft.LOGGER.info("PICK {} tab stacks, {} placed and picked, {} wrong", stacks.size(), checked, bad.size());
		h.assertTrue(bad.isEmpty(), bad.size()+" of "+stacks.size()+" wrong, e.g. "+(bad.isEmpty() ? "" : bad.get(0)));
		h.succeed();
	}

	/** Every block of the three mods (also those without an item of their own - multiblock parts, crops ...): middle-click gives an item. */
	@GameTest(template = BIG, timeoutTicks = 100, batch = "pick")
	public static void middleClickNeverEmpty(GameTestHelper h) {
		SimPlayer p = new SimPlayer(h, GameType.CREATIVE);
		SimPlayer.floor(h, 0, 0, 47, 47);
		List<String> bad = new ArrayList<>();
		int n = 0;
		for (Block b : BuiltInRegistries.BLOCK) {
			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
			if (!MODS.contains(id.getNamespace()) || b instanceof LiquidBlock || GAS.contains(id.getPath()))
				continue;
			BlockPos at = slot(n++ % (23*23));
			h.setBlock(at.below(), Blocks.FARMLAND); //crops (canola) need it
			h.getLevel().setBlock(h.absolutePos(at), b.defaultBlockState(), 2);
			BlockPos a = h.absolutePos(at);
			//asked of the state set down (canola removes itself on the dark test platform at once; in daylight it stays)
			ItemStack got = b.defaultBlockState().getCloneItemStack(new BlockHitResult(Vec3.atCenterOf(a), Direction.UP, a, false), h.getLevel(), a, p.player);
			if (got.isEmpty())
				bad.add(id+": middle-click gives nothing");
			h.getLevel().setBlock(h.absolutePos(at), Blocks.AIR.defaultBlockState(), 2);
			h.setBlock(at.below(), Blocks.STONE);
		}
		p.leave();
		bad.forEach(b -> ReactorCraft.LOGGER.error("PICK {}", b));
		ReactorCraft.LOGGER.info("PICK {} blocks, {} without a pick item", n, bad.size());
		h.assertTrue(bad.isEmpty(), bad.size()+" blocks give nothing, e.g. "+(bad.isEmpty() ? "" : bad.get(0)));
		h.succeed();
	}
}
