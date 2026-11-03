/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import reika.reactorcraft.registry.ReactorItems;
import reika.reactorcraft.registry.ReactorOres;

/**
 * Original: BlockReactorOre (hardness 2, resistance 5) - most ores drop themselves; calcite, ammonium chloride (with a netherrack) and
 * magnetite (lodestone) drop 1 + random(0..fortune) of their mineral and, with the ore's chance, 1 XP. Silk touch gives the ore.
 */
public class ReactorOreBlock extends Block {

	public final ReactorOres ore;

	public ReactorOreBlock(Properties props, ReactorOres ore) {
		super(props);
		this.ore = ore;
	}

	static int level(LootParams.Builder params, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ench) {
		ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
		if (tool == null || tool.isEmpty())
			return 0;
		return params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ench)
				.map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, tool)).orElse(0);
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> li = new ArrayList<>();
		if (ore.dropsSelf() || level(params, Enchantments.SILK_TOUCH) > 0) {
			li.add(new ItemStack(this));
			return li;
		}
		int fortune = level(params, Enchantments.FORTUNE);
		int n = 1+params.getLevel().random.nextInt(1+fortune);
		for (int i = 0; i < n; i++) {
			switch (ore) {
				case CALCITE -> li.add(new ItemStack(ReactorItems.raw(ReactorItems.Raw.CALCITE).get()));
				case MAGNETITE -> li.add(new ItemStack(ReactorItems.raw(ReactorItems.Raw.MAGNETITE).get()));
				case AMMONIUM -> {
					li.add(new ItemStack(ReactorItems.raw(ReactorItems.Raw.AMMONIUM).get()));
					li.add(new ItemStack(Items.NETHERRACK));
				}
				default -> li.add(new ItemStack(this));
			}
		}
		return li;
	}

	/** Original droppedXP: one XP with the ore's chance, when it does not drop itself. */
	@Override
	protected void spawnAfterBreak(BlockState state, ServerLevel world, BlockPos pos, ItemStack tool, boolean dropExperience) {
		super.spawnAfterBreak(state, world, pos, tool, dropExperience);
		if (dropExperience && !ore.dropsSelf() && world.random.nextFloat() < ore.xpDropped)
			this.tryDropExperience(world, pos, tool, net.minecraft.util.valueproviders.ConstantInt.of(1)); //none with silk touch
	}
}
