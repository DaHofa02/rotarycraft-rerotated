/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Farming.TileEntityMobHarvester (MachineRegistry.MOBHARVESTER, PowerReceivers: 4096 W, no
 * minimum speed or torque). Takes power from every side (getSummativeSidedPower). From 4096 W it hurts every mob (1.7.10 EntityLiving;
 * no villagers, no players) in the column above it - 2 blocks high, one more per 524288 W, at most 6 - as a player attack by whoever
 * placed it, so the kills drop experience and player-only loot; that needs the placer to be online. Damage per hit:
 * {@code (int)log2((2 + 0.5 * P / 4096)^6)}, i.e. 7 at 4096 W and 19 at 65536 W. Mobs in the column do not fall or jump (vertical motion
 * zeroed) even while the placer is away.
 * Not ported: machine enchantments (sharpness +2 damage per level, fire aspect, looting, infinity, silk touch: heads - the original
 * MachineEnchantmentHandler and HarvesterDamage's looting hook), the ChromatiCraft hook (an empty block in the original).
 */
public class MobHarvesterBlockEntity extends PowerReceiverBlockEntity {

	public boolean laser;
	/** The column's height as the server last sent it (the client does not know the power). */
	private int height = 2;

	public MobHarvesterBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.MOB_HARVESTER.get(), pos, state, 0, 0, 4096);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.readPowerSummative();
		boolean oneplus = false;
		if (power >= MINPOWER) {
			Player ep = this.getPlacer();
			for (Mob ent : world.getEntitiesOfClass(Mob.class, this.getBox())) {
				if (ent instanceof Villager)
					continue;
				oneplus = true;
				if (ep != null) {
					int dmg = this.getDamage();
					if (dmg > 0)
						ent.hurt(world.damageSources().playerAttack(ep), dmg);
				}
				Vec3 v = ent.getDeltaMovement();
				ent.setDeltaMovement(v.x, 0, v.z);
			}
		}
		//the original left the laser on after the power was gone (it only wrote the flag while powered)
		int h = this.computeHeight();
		if (oneplus != laser && power >= MINPOWER || h != height) {
			if (power >= MINPOWER)
				laser = oneplus;
			height = h;
			this.syncAllData();
		}
	}

	public int getDamage() {
		double pdiff = 2+0.5*power/MINPOWER;
		double ppdiff = ReikaMathLibrary.intpow(pdiff, 6);
		return (int)ReikaMathLibrary.logbase(ppdiff, 2);
	}

	public AABB getBox() {
		BlockPos p = worldPosition;
		return new AABB(p.getX(), p.getY()+1, p.getZ(), p.getX()+1, p.getY()+this.getHeight()+1, p.getZ()+1);
	}

	private int computeHeight() {
		return Math.min(6, 2+Math.max(0, (int)power/524288));
	}

	public int getHeight() {
		return level != null && level.isClientSide ? height : this.computeHeight();
	}

	public AABB getLaser() {
		BlockPos p = worldPosition;
		return new AABB(p.getX()+0.4, p.getY()+1, p.getZ()+0.4, p.getX()+0.6, p.getY()+this.getHeight(), p.getZ()+0.6);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("laser", laser);
		NBT.putInt("height", height);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		laser = NBT.getBoolean("laser");
		height = NBT.contains("height") ? NBT.getInt("height") : 2;
	}
}
