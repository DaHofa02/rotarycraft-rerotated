/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.world;

import java.util.Locale;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import reika.reactorcraft.ReactorConfig;
import reika.reactorcraft.registry.FluoriteTypes;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorOres;
import reika.reactorcraft.registry.ReactorWorldGen;

/**
 * Original: ReactorOreGenerator + BasicReactorOreGenerator + ReactorOres.shouldGen/canGenAt. Per chunk, with chance 1/DISCRETE,
 * perChunk*DISCRETE veins (perChunk = count * ore density, at least 1) at a random x/z and a uniform Y in the ore's band.
 * Ores with a "generate even if other mods do" option that is off only generate when no other block is in c:ores/&lt;material&gt;.
 * Ammonium chloride needs lava next to the vein centre. Fluorite: one colour per chunk (original: a new random colour per chunk; here
 * derived from the world seed and chunk so that the eight colour features agree), only that colour's feature places veins.
 */
public final class ReactorOrePlacement extends PlacementModifier {

	public static final MapCodec<ReactorOrePlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("ore").forGetter(p -> p.ore.getName()),
			Codec.STRING.optionalFieldOf("color", "").forGetter(p -> p.color != null ? p.color.getColorName() : "")
			).apply(i, (o, c) -> new ReactorOrePlacement(ReactorOres.valueOf(o.toUpperCase(Locale.ENGLISH)),
					c.isEmpty() ? null : FluoriteTypes.valueOf(c.toUpperCase(Locale.ENGLISH)))));

	private final ReactorOres ore;
	private final FluoriteTypes color;

	public ReactorOrePlacement(ReactorOres ore, FluoriteTypes color) {
		this.ore = ore;
		this.color = color;
	}

	private boolean shouldGen() {
		if (ReactorConfig.alwaysGenerate(ore))
			return true;
		TagKey<Block> tag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/"+ore.dictName));
		Block own = ore == ReactorOres.FLUORITE ? null : ReactorBlocks.ore(ore).get();
		for (var h : BuiltInRegistries.BLOCK.getTagOrEmpty(tag))
			if (h.value() != own && !(h.value() instanceof reika.reactorcraft.content.block.ReactorOreBlock))
				return false;
		return true;
	}

	public static FluoriteTypes chunkColor(long seed, ChunkPos cp) {
		return FluoriteTypes.colorList[RandomSource.create(seed^cp.toLong()*341873128712L).nextInt(FluoriteTypes.colorList.length)];
	}

	@Override
	public Stream<BlockPos> getPositions(PlacementContext ctx, RandomSource random, BlockPos pos) {
		int d = ReactorConfig.DISCRETE.get();
		if (!this.shouldGen() || random.nextInt(d) != 0)
			return Stream.empty();
		if (color != null && chunkColor(ctx.getLevel().getSeed(), new ChunkPos(pos)) != color)
			return Stream.empty();
		int perChunk = Math.max(1, (int)(ore.baseCount*ReactorConfig.getOreMultiplier()));
		int passes = perChunk*d;
		Stream.Builder<BlockPos> s = Stream.builder();
		for (int i = 0; i < passes; i++) {
			BlockPos p = new BlockPos(pos.getX()+random.nextInt(16), ore.minY+random.nextInt(ore.maxY-ore.minY+1), pos.getZ()+random.nextInt(16));
			if (ore == ReactorOres.AMMONIUM && !nextToLava(ctx, p))
				continue;
			s.add(p);
		}
		return s.build();
	}

	private static boolean nextToLava(PlacementContext ctx, BlockPos p) {
		for (Direction dir : Direction.values())
			if (ctx.getBlockState(p.relative(dir)).getFluidState().is(FluidTags.LAVA))
				return true;
		return false;
	}

	@Override
	public PlacementModifierType<?> type() {
		return ReactorWorldGen.ORE_PLACEMENT.get();
	}
}
