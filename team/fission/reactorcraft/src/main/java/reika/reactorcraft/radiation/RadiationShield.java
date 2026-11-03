/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.radiation;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.registry.ReactorBlocks;

/** Original: Reika.ReactorCraft.Registry.RadiationShield - neutron absorption and radiation deflection chances (%) of shielding blocks. */
public enum RadiationShield {

	STEEL(90, 95, s -> s.is(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/steel")))),
	CONCRETE(60, 70, s -> s.is(ReactorBlocks.CONCRETE.get())),
	WATER(30, 10, s -> s.getFluidState().is(FluidTags.WATER)),
	BEDINGOT(97.5, 100, s -> s.is(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/bedrock")))),
	LEAD(75, 50, s -> s.is(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/lead")))),
	OBSIDIAN(50, 80, s -> s.is(Blocks.OBSIDIAN)),
	BLASTGLASS(80, 20, s -> s.is(reika.rotarycraft.registry.RotaryBlocks.BLAST_GLASS.get()));

	public final double neutronAbsorbChance;
	public final double radiationDeflectChance;
	private final Predicate<BlockState> match;

	RadiationShield(double neutron, double radiation, Predicate<BlockState> match) {
		neutronAbsorbChance = neutron;
		radiationDeflectChance = radiation;
		this.match = match;
	}

	@Nullable
	public static RadiationShield getFrom(BlockState s) {
		for (RadiationShield r : values())
			if (r.match.test(s))
				return r;
		return null;
	}
}
