/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.neoforged.neoforge.registries.DeferredRegister;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.entity.NuclearWasteEntity;
import reika.reactorcraft.content.entity.RadiationEntity;

/** Original: ReactorEntities (the ones ported so far). */
public final class ReactorEntities {

	public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ReactorCraft.MOD_ID);

	public static final Supplier<EntityType<RadiationEntity>> RADIATION = TYPES.register("radiation", () -> EntityType.Builder.<RadiationEntity>of(RadiationEntity::new, MobCategory.MISC)
			.sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(200).build("radiation"));
	public static final Supplier<EntityType<NuclearWasteEntity>> NUCLEAR_WASTE = TYPES.register("nuclear_waste", () -> EntityType.Builder.<NuclearWasteEntity>of(NuclearWasteEntity::new, MobCategory.MISC)
			.sized(0.25F, 0.25F).clientTrackingRange(6).updateInterval(20).build("nuclear_waste"));

	public static final Supplier<EntityType<reika.reactorcraft.content.entity.NeutronEntity>> NEUTRON = TYPES.register("neutron",
			() -> EntityType.Builder.<reika.reactorcraft.content.entity.NeutronEntity>of(reika.reactorcraft.content.entity.NeutronEntity::new, MobCategory.MISC)
			.sized(0.1F, 0.1F).clientTrackingRange(4).updateInterval(1).build("neutron"));

	/** Original ReactorEntities PLASMA and FUSION. */
	public static final Supplier<EntityType<reika.reactorcraft.fusion.PlasmaEntity>> PLASMA = TYPES.register("plasma",
			() -> EntityType.Builder.<reika.reactorcraft.fusion.PlasmaEntity>of(reika.reactorcraft.fusion.PlasmaEntity::new, MobCategory.MISC)
			.sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1).build("plasma"));
	public static final Supplier<EntityType<reika.reactorcraft.fusion.FusionEntity>> FUSION = TYPES.register("fusion",
			() -> EntityType.Builder.<reika.reactorcraft.fusion.FusionEntity>of(reika.reactorcraft.fusion.FusionEntity::new, MobCategory.MISC)
			.sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1).build("fusion"));

	private ReactorEntities() {}
}
