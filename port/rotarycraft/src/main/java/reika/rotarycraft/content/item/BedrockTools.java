/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.item;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.registry.RotaryItems;

/**
 * The bedrock tools and armour, written for the port from the original's behaviour. All are unbreakable and made from bedrock alloy.
 * Pickaxe, axe and shovel dig everything of their kind at speed 20 (the original's 20-36); the axe fells the whole tree (connected logs, up
 * to 128) unless sneaking; the sword does 15 damage and wears the target's armour down by 100 points a hit; the hoe tills 3x3; the scythe
 * cuts the same kind of plant around it (mature crops within 6 blocks, leaves within 8, other plants within 7); the shears are unbreakable
 * shears. The armour protects like netherite and more (4/9/7/4, toughness 4, knockback resistance 0.2); the helmet gives night vision.
 * The paxel (only offered when a paxel mod is installed) digs whatever a pickaxe, axe or shovel digs.
 * Built-in enchantments like the original's: pickaxe (and paxel) silk touch and fortune V, sword sharpness V and looting V, scythe fortune
 * V (also on the plants it cuts around), helmet projectile protection IV and respiration III, chestplate blast protection IV, leggings fire
 * protection IV, boots feather falling IV; the axe never keeps silk touch. They are put on when crafted, shown in the creative tab, and put
 * back (raised to these levels) every tick the item is in an inventory; other enchantments can be added on top with books.
 * Changes: the original destroyed a tool that had lost them and hurt its holder, here they simply come back; the shovel's extra drops are
 * left out.
 */
public final class BedrockTools {

	private BedrockTools() {}

	public static final TagKey<Block> PAXEL_MINEABLE = BlockTags.create(ResourceLocation.fromNamespaceAndPath("rotarycraft", "mineable/paxel"));

	public static final Tier TIER = new Tier() {
		@Override
		public int getUses() {
			return 4096;
		}

		@Override
		public float getSpeed() {
			return 20F;
		}

		@Override
		public float getAttackDamageBonus() {
			return 5F;
		}

		@Override
		public TagKey<Block> getIncorrectBlocksForDrops() {
			return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
		}

		@Override
		public int getEnchantmentValue() {
			return 10;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of(RotaryItems.part(CraftingPart.BEDINGOT));
		}
	};

	/** Item properties with the unbreakable component. */
	public static Item.Properties props() {
		return new Item.Properties().component(DataComponents.UNBREAKABLE, new Unbreakable(true)).fireResistant();
	}

	public static class Pickaxe extends PickaxeItem {
		public Pickaxe() {
			super(TIER, props().attributes(PickaxeItem.createAttributes(TIER, 1, -2.8F)));
		}

		@Override
		public void inventoryTick(ItemStack is, Level world, Entity e, int slot, boolean selected) {
			keep(is, world);
		}

		@Override
		public void onCraftedPostProcess(ItemStack is, Level world) {
			keep(is, world);
		}
	}

	public static class Shovel extends ShovelItem {
		public Shovel() {
			super(TIER, props().attributes(ShovelItem.createAttributes(TIER, 1.5F, -3F)));
		}
	}

	public static class Axe extends AxeItem {
		public static final int MAX_LOGS = 128;

		public Axe() {
			super(TIER, props().attributes(AxeItem.createAttributes(TIER, 5, -3F)));
		}

		@Override
		public void inventoryTick(ItemStack is, Level world, Entity e, int slot, boolean selected) {
			keep(is, world);
		}

		@Override
		public void onCraftedPostProcess(ItemStack is, Level world) {
			keep(is, world);
		}

		@Override
		public boolean mineBlock(ItemStack is, Level world, BlockState state, BlockPos pos, LivingEntity user) {
			if (!world.isClientSide && state.is(BlockTags.LOGS) && !user.isShiftKeyDown())
				fell(world, pos, state.getBlock(), user);
			return super.mineBlock(is, world, state, pos, user);
		}

		/** Breaks the logs of that kind connected to pos (also diagonally), up to MAX_LOGS. */
		public static int fell(Level world, BlockPos start, Block log, LivingEntity user) {
			Set<BlockPos> seen = new HashSet<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(start);
			seen.add(start);
			int n = 0;
			while (!queue.isEmpty() && n < MAX_LOGS) {
				BlockPos p = queue.poll();
				for (BlockPos q : BlockPos.betweenClosed(p.offset(-1, 0, -1), p.offset(1, 1, 1))) {
					BlockPos qi = q.immutable();
					if (seen.add(qi) && world.getBlockState(qi).is(log)) {
						world.destroyBlock(qi, true, user);
						queue.add(qi);
						n++;
					}
				}
			}
			return n;
		}
	}

	public static class Sword extends SwordItem {
		public Sword() {
			super(TIER, props().attributes(SwordItem.createAttributes(TIER, 9, -2.4F)));
		}

		@Override
		public void inventoryTick(ItemStack is, Level world, Entity e, int slot, boolean selected) {
			keep(is, world);
		}

		@Override
		public void onCraftedPostProcess(ItemStack is, Level world) {
			keep(is, world);
		}

		@Override
		public boolean hurtEnemy(ItemStack is, LivingEntity target, LivingEntity attacker) {
			for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				ItemStack armour = target.getItemBySlot(s);
				if (!armour.isEmpty() && armour.isDamageableItem())
					armour.hurtAndBreak(100, target, s);
			}
			return super.hurtEnemy(is, target, attacker);
		}
	}

	public static class Hoe extends HoeItem {
		public Hoe() {
			super(TIER, props().attributes(HoeItem.createAttributes(TIER, 0, 0F)));
		}

		/** Tills the 3x3 around the clicked block (each spot like a hoe would, if it can be tilled and has air above). */
		@Override
		public InteractionResult useOn(UseOnContext ctx) {
			Level world = ctx.getLevel();
			BlockPos c = ctx.getClickedPos();
			if (ctx.getClickedFace() == Direction.DOWN)
				return InteractionResult.PASS;
			int n = 0;
			for (BlockPos p : BlockPos.betweenClosed(c.offset(-1, 0, -1), c.offset(1, 0, 1))) {
				BlockState s = world.getBlockState(p);
				if ((s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT_PATH) || s.is(Blocks.COARSE_DIRT)) && world.getBlockState(p.above()).isAir()) {
					if (!world.isClientSide)
						world.setBlock(p, s.is(Blocks.COARSE_DIRT) ? Blocks.DIRT.defaultBlockState() : Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
					n++;
				}
			}
			return n > 0 ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
		}
	}

	public static class Scythe extends Item {
		public Scythe() {
			super(props().stacksTo(1));
		}

		@Override
		public void inventoryTick(ItemStack is, Level world, Entity e, int slot, boolean selected) {
			keep(is, world);
		}

		@Override
		public void onCraftedPostProcess(ItemStack is, Level world) {
			keep(is, world);
		}

		public static int rangeFor(BlockState s) {
			if (s.getBlock() instanceof CropBlock)
				return 6;
			if (s.is(BlockTags.LEAVES))
				return 8;
			if (s.is(BlockTags.FLOWERS) || s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(BlockTags.SAPLINGS))
				return 7;
			return 0;
		}

		private static boolean sameKind(BlockState a, BlockState b) {
			if (a.getBlock() instanceof CropBlock)
				return b.getBlock() instanceof CropBlock cb && cb.isMaxAge(b);
			if (a.is(BlockTags.LEAVES))
				return b.is(BlockTags.LEAVES);
			return rangeFor(b) == 7;
		}

		@Override
		public boolean mineBlock(ItemStack is, Level world, BlockState state, BlockPos pos, LivingEntity user) {
			int r = rangeFor(state);
			if (!world.isClientSide && r > 0)
				for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r)))
					if (!p.equals(pos) && sameKind(state, world.getBlockState(p))) {
						Block.dropResources(world.getBlockState(p), world, p, world.getBlockEntity(p), user, is);
						world.destroyBlock(p, false, user);
					}
			return true;
		}

		@Override
		public float getDestroySpeed(ItemStack is, BlockState state) {
			return rangeFor(state) > 0 ? 20F : 1F;
		}
	}

	public static class Shears extends ShearsItem {
		public Shears() {
			super(props().component(DataComponents.TOOL, ShearsItem.createToolProperties()));
		}
	}

	public static class Paxel extends DiggerItem {
		public Paxel() {
			super(TIER, PAXEL_MINEABLE, props().attributes(DiggerItem.createAttributes(TIER, 6, -3F)));
		}

		@Override
		public void inventoryTick(ItemStack is, Level world, Entity e, int slot, boolean selected) {
			keep(is, world);
		}

		@Override
		public void onCraftedPostProcess(ItemStack is, Level world) {
			keep(is, world);
		}
	}

	public static class Armour extends ArmorItem {
		public Armour(net.minecraft.core.Holder<ArmorMaterial> material, Type type) {
			super(material, type, props().durability(type.getDurability(64)));
		}

		@Override
		public void inventoryTick(ItemStack is, Level world, Entity e, int slot, boolean selected) {
			keep(is, world);
			if (!world.isClientSide && this.getType() == Type.HELMET && e instanceof Player p && p.getItemBySlot(EquipmentSlot.HEAD) == is)
				p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, false));
		}

		@Override
		public void onCraftedPostProcess(ItemStack is, Level world) {
			keep(is, world);
		}
	}

	/** The built-in enchantments of a bedrock item (empty for those without). */
	public static Map<ResourceKey<Enchantment>, Integer> forced(Item item) {
		if (item instanceof Pickaxe || item instanceof Paxel)
			return Map.of(Enchantments.SILK_TOUCH, 1, Enchantments.FORTUNE, 5);
		if (item instanceof Sword)
			return Map.of(Enchantments.SHARPNESS, 5, Enchantments.LOOTING, 5);
		if (item instanceof Scythe)
			return Map.of(Enchantments.FORTUNE, 5);
		if (item instanceof Armour a)
			return switch (a.getType()) {
				case HELMET -> Map.of(Enchantments.PROJECTILE_PROTECTION, 4, Enchantments.RESPIRATION, 3);
				case CHESTPLATE -> Map.of(Enchantments.BLAST_PROTECTION, 4);
				case LEGGINGS -> Map.of(Enchantments.FIRE_PROTECTION, 4);
				default -> Map.of(Enchantments.FEATHER_FALLING, 4);
			};
		return Map.of();
	}

	/** Raises the stack's built-in enchantments to their levels (and takes silk touch off the axe); returns whether it changed anything. */
	public static boolean force(ItemStack is, HolderLookup.Provider reg) {
		HolderLookup.RegistryLookup<Enchantment> lookup = reg.lookupOrThrow(Registries.ENCHANTMENT);
		ItemEnchantments now = is.getTagEnchantments();
		Map<ResourceKey<Enchantment>, Integer> want = forced(is.getItem());
		Holder<Enchantment> silk = lookup.getOrThrow(Enchantments.SILK_TOUCH);
		boolean dropSilk = is.getItem() instanceof Axe && now.getLevel(silk) > 0;
		boolean raise = want.entrySet().stream().anyMatch(e -> now.getLevel(lookup.getOrThrow(e.getKey())) < e.getValue());
		if (!dropSilk && !raise)
			return false;
		EnchantmentHelper.updateEnchantments(is, m -> {
			want.forEach((k, lvl) -> {
				Holder<Enchantment> h = lookup.getOrThrow(k);
				if (m.getLevel(h) < lvl)
					m.set(h, lvl);
			});
			if (dropSilk)
				m.removeIf(h -> h.is(Enchantments.SILK_TOUCH));
		});
		return true;
	}

	/** A new stack of the item with its built-in enchantments (for the creative tab). */
	public static ItemStack withForced(Item item, HolderLookup.Provider reg) {
		ItemStack is = new ItemStack(item);
		force(is, reg);
		return is;
	}

	private static void keep(ItemStack is, Level world) {
		if (!world.isClientSide)
			force(is, world.registryAccess());
	}

	/** Whether a paxel mod is installed (Mekanism Tools or another known one): only then is the bedrock paxel offered. */
	public static boolean paxelModLoaded() {
		var ml = net.neoforged.fml.ModList.get();
		return ml != null && (ml.isLoaded("mekanismtools") || ml.isLoaded("paxels") || ml.isLoaded("paxi"));
	}

	public static boolean isServer(Level world) {
		return world instanceof ServerLevel;
	}
}
