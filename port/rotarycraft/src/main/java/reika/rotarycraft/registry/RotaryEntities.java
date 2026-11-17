/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.entity.DischargeEntity;

/** Original: Reika.RotaryCraft.Registry.EntityRegistry (the entities ported so far). */
public final class RotaryEntities {

	public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, RotaryCraft.MOD_ID);

	public static final Supplier<EntityType<DischargeEntity>> DISCHARGE = TYPES.register("discharge", () -> EntityType.Builder.<DischargeEntity>of(DischargeEntity::new, MobCategory.MISC)
			.sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).noSave().build("discharge"));

	/** Original: EntityRegistry.SONIC (EntitySonicShot). */
	public static final Supplier<EntityType<reika.rotarycraft.content.entity.SonicShotEntity>> SONIC_SHOT = TYPES.register("sonic_shot", () -> EntityType.Builder.<reika.rotarycraft.content.entity.SonicShotEntity>of(reika.rotarycraft.content.entity.SonicShotEntity::new, MobCategory.MISC)
			.sized(0.2F, 0.2F).clientTrackingRange(16).updateInterval(1).noSave().build("sonic_shot"));

	private RotaryEntities() {}
}
