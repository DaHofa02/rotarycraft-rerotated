/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The air pressure gun, written for the port from the original's behaviour: with 16384 W at 512 Nm or more into its back, every
 * max(4, 16 - log2(speed+1)) ticks it blasts every living thing standing on the ground in the 10 + 2*log2(torque+1) blocks in front of it
 * away from it, at log2(torque+1)/4 blocks a tick (and half a block upwards). Its owner is not hit.
 */
public class AirGunBlockEntity extends PowerReceiverBlockEntity {

	public int blasts;

	public AirGunBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.AIR_GUN.get(), pos, state, 512, 1, 16384);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public Direction getFacing() {
		return read == null ? Direction.WEST : read.getOpposite();
	}

	public int getRange() {
		return 10+2*(int)ReikaMathLibrary.logbase(torque+1, 2);
	}

	public int getOperationTime() {
		return Math.max(16-(int)ReikaMathLibrary.logbase(omega+1, 2), 4);
	}

	public double getFirePower() {
		return ReikaMathLibrary.logbase(torque+1, 2);
	}

	public AABB getArea() {
		Direction d = this.getFacing();
		BlockPos start = worldPosition.relative(d);
		BlockPos end = worldPosition.relative(d, this.getRange());
		return new AABB(start).minmax(new AABB(end));
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		if (power < MINPOWER || torque < MINTORQUE)
			return;
		if (this.getTicksExisted()%this.getOperationTime() != 0)
			return;
		List<LivingEntity> li = world.getEntitiesOfClass(LivingEntity.class, this.getArea(), e -> e.isAlive() && e.onGround()
				&& !(e instanceof Player p && this.isPlacer(p)));
		if (li.isEmpty())
			return;
		double v = this.getFirePower()/4;
		Direction d = this.getFacing();
		for (LivingEntity e : li) {
			e.setDeltaMovement(d.getStepX()*v, 0.5, d.getStepZ()*v);
			e.hurtMarked = true;
		}
		blasts++;
	}

	private boolean isPlacer(Player p) {
		return p.getUUID().equals(this.getPlacerUUID());
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}
}
