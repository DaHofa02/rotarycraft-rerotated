/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.reactor.LiquidStates;
import reika.reactorcraft.reactor.NeutronTile;
import reika.reactorcraft.reactor.ReactorBlockEntity;
import reika.reactorcraft.reactor.ReactorType;
import reika.reactorcraft.reactor.ReactorTyped;
import reika.reactorcraft.reactor.Temperatured;
import reika.reactorcraft.registry.ReactorBlockEntities;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fusion.TileEntityNeutronAbsorber - catches fusion neutrons, each heating it by 40 degrees.
 * Once a second its temperature evens out with its surroundings and the reactor parts beside it (as a fusion part it gives no heat to
 * parts of other reactor types, as in the original); at 1500 degrees it melts into lava.
 */
public class NeutronAbsorberBlockEntity extends ReactorBlockEntity implements Temperatured, ReactorTyped, NeutronTile {

	private int tempTimer;

	public NeutronAbsorberBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.NEUTRON_ABSORBER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Neutron Absorber";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		tempTimer++;
		if (tempTimer >= 20) {
			tempTimer = 0;
			this.updateTemperature(world, pos);
			if (temperature >= this.getMaxTemperature()) {
				world.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
				world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F+(rand.nextFloat()-rand.nextFloat())*0.8F);
			}
		}
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		if (e.getNeutronType() == NeutronEntity.NeutronType.FUSION) {
			temperature += 40;
			return true;
		}
		return false;
	}

	@Override
	public int getMaxTemperature() {
		return 1500;
	}

	@Override
	public boolean canDumpHeatInto(LiquidStates liq) {
		return liq != LiquidStates.EMPTY;
	}

	@Override
	public ReactorType getReactorType() {
		return ReactorType.FUSION;
	}
}
