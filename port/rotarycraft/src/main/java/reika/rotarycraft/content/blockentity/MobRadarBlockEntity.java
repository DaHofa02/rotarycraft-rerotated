/*******************************************************************************
 * Port of Reika.RotaryCraft.TileEntities.Surveying.TileEntityMobRadar. Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki;
 * not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.api.interfaces.RadarJammer;
import reika.rotarycraft.registry.RotaryBlockEntities;

public class MobRadarBlockEntity extends PowerReceiverBlockEntity {

	public static final int FALLOFF = 1024;

	private List<LivingEntity> inzone = new ArrayList<>();

	public boolean hostile = true;
	public boolean animal = true;
	public boolean player = true;

	private boolean isJammed;

	public MobRadarBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.MOB_RADAR.get(), pos, state, 1, 1, 8192); //PowerReceivers.MOBRADAR
	}

	@Override
	public String getName() {
		return "Mob Radar";
	}

	public List<LivingEntity> getEntities() {
		return Collections.unmodifiableList(inzone);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (!world.isClientSide) {
			read = Direction.DOWN; //original getPowerBelow
			this.readPower();
		}
		this.getMobs(world, pos);
	}

	public int getRange() {
		int range = (int)(8L+(power-MINPOWER)/FALLOFF);
		return Math.min(range, this.getMaxRange());
	}

	public int getMaxRange() {
		return 256;
	}

	public boolean isJammed() {
		return isJammed;
	}

	private void getMobs(Level world, BlockPos pos) {
		boolean was = isJammed;
		isJammed = false;
		int range = this.getRange();
		AABB zone = new AABB(pos.getX()-range, world.getMinBuildHeight(), pos.getZ()-range, pos.getX()+1+range, world.getMaxBuildHeight(), pos.getZ()+1+range);
		inzone = world.getEntitiesOfClass(LivingEntity.class, zone, this::isSelected);
		for (LivingEntity ent : inzone) {
			if (ent instanceof RadarJammer j && j.jamRadar(world, pos)) {
				isJammed = true;
				break;
			}
		}
		if (was != isJammed && !world.isClientSide)
			this.syncAllData();
	}

	/** Original ConfigurableEntitySelector with the radar's three flags. */
	private boolean isSelected(LivingEntity e) {
		if (e instanceof Player p)
			return player && !p.isSpectator();
		if (e instanceof Animal)
			return animal;
		return hostile && isHostile(e);
	}

	/** Original ReikaEntityHelper.isHostile: monsters, and an iron golem while it hunts a player or a pet. */
	public static boolean isHostile(LivingEntity e) {
		if (e instanceof IronGolem g) {
			LivingEntity tgt = g.getTarget();
			return tgt instanceof Player || tgt instanceof OwnableEntity o && o.getOwnerUUID() != null;
		}
		return e instanceof Enemy;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (power >= MINPOWER)
			phi += 4F;
	}

	@Override
	protected void writeSyncTag(CompoundTag tag, HolderLookup.Provider regs) {
		super.writeSyncTag(tag, regs);
		tag.putBoolean("jam", isJammed);
	}

	@Override
	protected void readSyncTag(CompoundTag tag, HolderLookup.Provider regs) {
		super.readSyncTag(tag, regs);
		isJammed = tag.getBoolean("jam");
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}
}
