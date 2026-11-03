/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.loot;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: MinecraftForge.addGrassSeed(canolaSeeds, 2). In 1.7.10 a broken tall grass dropped, with chance 1/8, one seed picked by
 * weight from the registered grass seeds (wheat 10, canola 2). Here the vanilla grass/fern loot keeps its 1/8 wheat seed drop and
 * this modifier turns that drop into canola seeds with chance 2/12, which gives the same odds.
 */
public class CanolaGrassSeedModifier extends LootModifier {

	public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RotaryCraft.MOD_ID);

	public static final MapCodec<CanolaGrassSeedModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, CanolaGrassSeedModifier::new));

	public static final Supplier<MapCodec<CanolaGrassSeedModifier>> TYPE = SERIALIZERS.register("canola_grass_seed", () -> CODEC);

	private static final int CANOLA_WEIGHT = 2;
	private static final int WHEAT_WEIGHT = 10;

	public CanolaGrassSeedModifier(LootItemCondition[] conditions) {
		super(conditions);
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
		for (int i = 0; i < loot.size(); i++) {
			ItemStack is = loot.get(i);
			if (is.is(Items.WHEAT_SEEDS) && ctx.getRandom().nextInt(CANOLA_WEIGHT+WHEAT_WEIGHT) < CANOLA_WEIGHT)
				loot.set(i, new ItemStack(RotaryItems.CANOLA_SEEDS.get(), is.getCount()));
		}
		return loot;
	}

	@Override
	public MapCodec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
}
