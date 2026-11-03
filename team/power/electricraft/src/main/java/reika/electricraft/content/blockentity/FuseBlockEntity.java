/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3f;

import reika.electricraft.network.WireFuse;
import reika.electricraft.registry.ElectriBlockEntities;
import reika.electricraft.registry.ElectriComponents;

/**
 * Original: Reika.ElectriCraft.TileEntities.TileEntityFuse - a path carrying more than its limit (32/128/1024/8192 A, set by the
 * crafting recipe and kept on the item) burns it out: it stops conducting for good.
 */
public class FuseBlockEntity extends WireComponentBlockEntity implements WireFuse {

	public static final int[] TIERS = {32, 128, 1024, 8192};

	private boolean overloaded;
	private int currentLimit;

	public FuseBlockEntity(BlockPos pos, BlockState state) {
		super(ElectriBlockEntities.FUSE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Fuse";
	}

	@Override
	public int getResistance() {
		return !this.isOverloaded() ? 0 : Integer.MAX_VALUE;
	}

	@Override
	public int getMaxCurrent() {
		return currentLimit;
	}

	public boolean isOverloaded() {
		return overloaded;
	}

	@Override
	public boolean canConnect() {
		return !this.isOverloaded();
	}

	@Override
	public void overload(int current) {
		overloaded = true;
		if (level != null && !level.isClientSide) {
			level.playSound(null, worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1, 1);
			level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 2, 1);
			if (level instanceof ServerLevel sl) //original: 32 white coloured particles
				sl.sendParticles(new DustParticleOptions(new Vector3f(1, 1, 1), 1), worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, 32, 0.25, 0.25, 0.25, 0);
			this.syncAllData();
		}
	}

	@Override
	public float getHeight() {
		return 0.625F;
	}

	@Override
	public float getWidth() {
		return 0.75F;
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		Integer lim = input.get(ElectriComponents.FUSE_LIMIT.get());
		if (lim != null)
			currentLimit = lim;
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(ElectriComponents.FUSE_LIMIT.get(), currentLimit);
	}

	public void setCurrentLimit(int limit) {
		currentLimit = limit;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("overload", overloaded);
		NBT.putInt("limit", currentLimit);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		overloaded = NBT.getBoolean("overload");
		currentLimit = NBT.getInt("limit");
	}
}
