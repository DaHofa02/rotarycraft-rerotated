/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;

/** Kinetic energy in an ME network. There is only one kind, so this key is a singleton; amounts are joules. */
public final class KineticKey extends AEKey {

	public static final KineticKey INSTANCE = new KineticKey();
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RotaryME.MOD_ID, "kinetic");

	private KineticKey() {}

	@Override
	public AEKeyType getType() {
		return KineticKeyType.INSTANCE;
	}

	@Override
	public AEKey dropSecondary() {
		return this;
	}

	@Override
	public CompoundTag toTag(HolderLookup.Provider registries) {
		return new CompoundTag();
	}

	@Override
	public Object getPrimaryKey() {
		return ID;
	}

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public void writeToPacket(RegistryFriendlyByteBuf buf) {}

	@Override
	protected Component computeDisplayName() {
		return Component.translatable("key.rotaryme.kinetic");
	}

	/** Energy cannot be dropped as items. */
	@Override
	public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {}

	@Override
	public boolean hasComponents() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		return o == this;
	}

	@Override
	public int hashCode() {
		return ID.hashCode();
	}
}
