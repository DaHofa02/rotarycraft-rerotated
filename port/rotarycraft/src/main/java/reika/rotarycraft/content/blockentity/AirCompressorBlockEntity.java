/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.compat.PneumaticCompat;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The air compressor, written for the port from the original's behaviour: shaft power into its back is pumped as compressed air into the
 * PneumaticCraft machine in front of it, power/520 mL a tick (1 mL of air per FE). Needs PneumaticCraft; without it, it does nothing.
 */
public class AirCompressorBlockEntity extends PowerReceiverBlockEntity {

	public long pumped;
	public boolean hasTarget;

	public AirCompressorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.AIR_COMPRESSOR.get(), pos, state, 1, 1, 1);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public Direction getOutputSide() {
		return read == null ? Direction.WEST : read.getOpposite();
	}

	public int getAirPerTick() {
		return (int)(power/MagneticEngineBlockEntity.WATTS_PER_FE);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide || !reika.rotarycraft.compat.OptionalMods.pneumaticCraft())
			return;
		this.readPower();
		Direction out = this.getOutputSide();
		int air = this.getAirPerTick();
		hasTarget = PneumaticCompat.pushAir(world, pos.relative(out), out.getOpposite(), air);
		if (hasTarget)
			pumped += air;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}
}
