/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import net.neoforged.neoforge.registries.DeferredRegister;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.recipe.BlastCraftingRecipe;
import reika.rotarycraft.content.recipe.BlastFurnaceRecipe;
import reika.rotarycraft.content.recipe.FrictionHeaterRecipe;
import reika.rotarycraft.content.recipe.WorktableRecipe;

/** Original: the machine recipe handlers RecipesBlastFurnace and WorktableRecipes; their recipes are datapack recipes here. */
public final class RotaryRecipeTypes {

	public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, RotaryCraft.MOD_ID);
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, RotaryCraft.MOD_ID);

	public static final Supplier<RecipeType<BlastFurnaceRecipe>> BLAST_FURNACE = type("blast_furnace");
	public static final Supplier<RecipeType<BlastCraftingRecipe>> BLAST_CRAFTING = type("blast_crafting");
	public static final Supplier<RecipeType<WorktableRecipe>> WORKTABLE = type("worktable");
	public static final Supplier<RecipeType<FrictionHeaterRecipe>> FRICTION = type("friction_heater");
	public static final Supplier<RecipeType<reika.rotarycraft.content.recipe.GrinderRecipe>> GRINDER = type("grinder");
	public static final Supplier<RecipeType<reika.rotarycraft.content.recipe.CentrifugeRecipe>> CENTRIFUGE = type("centrifuge");
	public static final Supplier<RecipeType<reika.rotarycraft.content.recipe.PulseJetRecipe>> PULSE_JET = type("pulse_jet");

	public static final Supplier<RecipeSerializer<BlastFurnaceRecipe>> BLAST_FURNACE_SERIALIZER = SERIALIZERS.register("blast_furnace", BlastFurnaceRecipe.Serializer::new);
	public static final Supplier<RecipeSerializer<BlastCraftingRecipe>> BLAST_CRAFTING_SERIALIZER = SERIALIZERS.register("blast_crafting", BlastCraftingRecipe.Serializer::new);
	public static final Supplier<RecipeSerializer<BlastCraftingRecipe>> BLAST_CRAFTING_SHAPELESS_SERIALIZER = SERIALIZERS.register("blast_crafting_shapeless", BlastCraftingRecipe.ShapelessSerializer::new);
	public static final Supplier<RecipeSerializer<WorktableRecipe.Shaped>> WORKTABLE_SHAPED = SERIALIZERS.register("worktable_shaped",
			() -> WorktableRecipe.serializer(WorktableRecipe.Shaped.CODEC, WorktableRecipe.Shaped.STREAM_CODEC));
	public static final Supplier<RecipeSerializer<WorktableRecipe.Shapeless>> WORKTABLE_SHAPELESS = SERIALIZERS.register("worktable_shapeless",
			() -> WorktableRecipe.serializer(WorktableRecipe.Shapeless.CODEC, WorktableRecipe.Shapeless.STREAM_CODEC));
	public static final Supplier<RecipeSerializer<WorktableRecipe.Recycling>> RECYCLING = SERIALIZERS.register("recycling",
			() -> WorktableRecipe.serializer(WorktableRecipe.Recycling.CODEC, WorktableRecipe.Recycling.STREAM_CODEC));

	public static final Supplier<RecipeSerializer<FrictionHeaterRecipe>> FRICTION_SERIALIZER = SERIALIZERS.register("friction_heater",
			() -> WorktableRecipe.serializer(FrictionHeaterRecipe.CODEC, FrictionHeaterRecipe.STREAM_CODEC));

	public static final Supplier<RecipeSerializer<reika.rotarycraft.content.recipe.GrinderRecipe>> GRINDER_SERIALIZER = SERIALIZERS.register("grinder",
			() -> WorktableRecipe.serializer(reika.rotarycraft.content.recipe.GrinderRecipe.CODEC, reika.rotarycraft.content.recipe.GrinderRecipe.STREAM_CODEC));

	public static final Supplier<RecipeSerializer<reika.rotarycraft.content.recipe.CentrifugeRecipe>> CENTRIFUGE_SERIALIZER = SERIALIZERS.register("centrifuge",
			() -> WorktableRecipe.serializer(reika.rotarycraft.content.recipe.CentrifugeRecipe.CODEC, reika.rotarycraft.content.recipe.CentrifugeRecipe.STREAM_CODEC));

	public static final Supplier<RecipeSerializer<reika.rotarycraft.content.recipe.PulseJetRecipe>> PULSE_JET_SERIALIZER = SERIALIZERS.register("pulse_jet",
			() -> WorktableRecipe.serializer(reika.rotarycraft.content.recipe.PulseJetRecipe.CODEC, reika.rotarycraft.content.recipe.PulseJetRecipe.STREAM_CODEC));

	private RotaryRecipeTypes() {}

	private static <T extends Recipe<?>> Supplier<RecipeType<T>> type(String name) {
		return TYPES.register(name, () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, name)));
	}
}
