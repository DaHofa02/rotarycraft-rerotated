/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.world;

import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import reika.electricraft.ElectriConfig;
import reika.electricraft.registry.ElectriBlocks;
import reika.electricraft.registry.ElectriOres;
import reika.electricraft.registry.ElectriWorldGen;

/**
 * Original: ElectriOreGenerator + BasicElectriOreGenerator + ElectriOres.isOreEnabled. Per chunk, with chance 1/DISCRETE,
 * perChunk*DISCRETE veins at a random x/z in the chunk and a uniform Y in [minY, maxY]; the ore is skipped when its "Ore Control"
 * option is off and another mod already provides that ore (c:ores/&lt;metal&gt; holds another block).
 */
public final class OreControlPlacement extends PlacementModifier {

	public static final MapCodec<OreControlPlacement> CODEC = Codec.STRING.fieldOf("ore")
			.xmap(s -> new OreControlPlacement(ElectriOres.valueOf(s.toUpperCase(java.util.Locale.ENGLISH))), p -> p.ore.getName());

	private final ElectriOres ore;

	public OreControlPlacement(ElectriOres ore) {
		this.ore = ore;
	}

	private boolean isOreEnabled() {
		if (ElectriConfig.isOreGenEnabled(ore))
			return true;
		return !this.hasEquivalents();
	}

	private boolean hasEquivalents() {
		TagKey<Block> tag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/"+ore.getName()));
		Block own = ElectriBlocks.ore(ore).get();
		for (var h : BuiltInRegistries.BLOCK.getTagOrEmpty(tag))
			if (h.value() != own)
				return true;
		return false;
	}

	@Override
	public Stream<BlockPos> getPositions(PlacementContext ctx, RandomSource random, BlockPos pos) {
		int d = ElectriConfig.DISCRETE.get();
		if (!this.isOreEnabled() || random.nextInt(d) != 0)
			return Stream.empty();
		int passes = ore.perChunk*d;
		Stream.Builder<BlockPos> s = Stream.builder();
		for (int i = 0; i < passes; i++) {
			int x = pos.getX()+random.nextInt(16);
			int z = pos.getZ()+random.nextInt(16);
			int y = ore.minY+random.nextInt(ore.maxY-ore.minY+1);
			s.add(new BlockPos(x, y, z));
		}
		return s.build();
	}

	@Override
	public PlacementModifierType<?> type() {
		return ElectriWorldGen.ORE_CONTROL.get();
	}
}
