/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.api.interfaces.ComplexIO;
import reika.rotarycraft.api.power.PowerAcceptor;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntityBevelGear. The 24 read/write pairs keep their original order. */
public class BevelGearBlockEntity extends OneDTransmitterBlockEntity {

	public record IOPair(Direction read, Direction write) {}

	private static final List<IOPair> directions = new ArrayList<>();

	static {
		for (int i = 0; i < 24; i++) { //do it this way to preserve ordering
			registerDirectionValue(i);
		}
	}

	private static void registerDirectionValue(int value) {
		Direction read = null;
		Direction write = null;
		switch(value) {
			case 0 -> { read = Direction.WEST; write = Direction.NORTH; } //-x -> -z
			case 1 -> { read = Direction.NORTH; write = Direction.EAST; }
			case 2 -> { read = Direction.EAST; write = Direction.SOUTH; }
			case 3 -> { read = Direction.SOUTH; write = Direction.WEST; }
			case 4 -> { read = Direction.WEST; write = Direction.SOUTH; }
			case 5 -> { read = Direction.NORTH; write = Direction.WEST; }
			case 6 -> { write = Direction.NORTH; read = Direction.EAST; }
			case 7 -> { read = Direction.SOUTH; write = Direction.EAST; }
			case 8 -> { read = Direction.WEST; write = Direction.UP; } //VERTICAL POSITIONS - going up from flat
			case 9 -> { read = Direction.NORTH; write = Direction.UP; }
			case 10 -> { read = Direction.EAST; write = Direction.UP; }
			case 11 -> { read = Direction.SOUTH; write = Direction.UP; }
			case 12 -> { read = Direction.DOWN; write = Direction.WEST; } //VERTICAL POSITIONS - going flat from up
			case 13 -> { read = Direction.DOWN; write = Direction.NORTH; }
			case 14 -> { read = Direction.DOWN; write = Direction.EAST; }
			case 15 -> { read = Direction.DOWN; write = Direction.SOUTH; }
			case 16 -> { write = Direction.DOWN; read = Direction.WEST; } //VERTICAL POSITIONS - going down from flat
			case 17 -> { write = Direction.DOWN; read = Direction.NORTH; }
			case 18 -> { write = Direction.DOWN; read = Direction.EAST; }
			case 19 -> { write = Direction.DOWN; read = Direction.SOUTH; }
			case 20 -> { read = Direction.UP; write = Direction.WEST; } //VERTICAL POSITIONS - going flat from down
			case 21 -> { read = Direction.UP; write = Direction.NORTH; }
			case 22 -> { read = Direction.UP; write = Direction.EAST; }
			case 23 -> { read = Direction.UP; write = Direction.SOUTH; }
		}
		directions.add(new IOPair(read, write));
	}

	public static List<IOPair> getDirectionMap() {
		return List.copyOf(directions);
	}

	public static boolean isValid(Direction read, Direction write) {
		return directions.contains(new IOPair(read, write));
	}

	public int direction;

	public BevelGearBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.BEVEL_GEARS.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Bevel Gears";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();

		if (this.getTicksExisted() == 0 && !world.isClientSide)
			this.findRoute(world, pos);

		power = (long)omega*(long)torque;
		this.getIOSides();
		this.transferPower(world, pos, this.getBlockMetadata());

		this.basicPowerReceiver();
	}

	private void findRoute(Level world, BlockPos pos) {
		Direction write = null;
		Direction read = null;
		for (Direction dir : Direction.values()) {
			BlockEntity te = this.getAdjacentTileEntity(dir);
			if (te instanceof IOMachineBlockEntity io) {
				if (read == null) {
					if (io.isWritingToCoordinate(pos) || io.isWritingToCoordinate2(pos)) {
						read = dir;
					}
				}
				if (write == null) {
					if (io.isReadingFrom(this) || io.isReadingFrom2(this) || io.isReadingFrom3(this) || io.isReadingFrom4(this)) {
						write = dir;
					}
				}
			}
			else if (te instanceof PowerAcceptor acc) {
				if (write == null) {
					if (acc.canReadFrom(dir.getOpposite())) {
						write = dir;
					}
				}
			}
		}
		if (write != null && read != null && read != write && read != write.getOpposite()) {
			direction = directions.indexOf(new IOPair(read, write));
		}
	}

	/** Used by the bevel gear GUI and the screwdriver to pick one of the 24 directions. */
	public void setDirection(int idx) {
		direction = Math.floorMod(idx, directions.size());
		this.syncAllData();
	}

	public void getIOSides() {
		IOPair dirs = directions.get(direction);
		read = dirs.read();
		write = dirs.write();
	}

	@Override
	protected void transferPower(Level world, BlockPos pos, int meta) {
		if (world.isClientSide)
			return;
		omegain = torquein = 0;
		BlockEntity te = this.getAdjacentTileEntity(read);
		if (this.isProvider(te)) {
			if (te instanceof ShaftBlockEntity devicein) {
				if (devicein.isCross()) {
					this.readFromCross(devicein);
					return;
				}
				else if (devicein.isWritingTo(this)) {
					torquein = devicein.torque;
					omegain = devicein.omega;
				}
			}
			if (te instanceof SimpleProvider) {
				this.copyStandardPower(te);
			}
			if (te instanceof ComplexIO pwr) {
				Direction dir = this.getInputForgeDirection().getOpposite();
				omegain = pwr.getSpeedToSide(dir);
				torquein = pwr.getTorqueToSide(dir);
			}
			if (te instanceof ShaftPowerEmitter sp) {
				if (sp.isEmitting() && sp.canWriteTo(read.getOpposite())) {
					torquein = sp.getTorque();
					omegain = sp.getOmega();
				}
			}
			if (te instanceof SplitterBlockEntity devicein) {
				if (devicein.isSplitting()) {
					this.readFromSplitter(world, pos, devicein);
					torquein = torque;
					omegain = omega;
					return;
				}
				else if (devicein.isWritingTo(this)) {
					torquein = devicein.torque;
					omegain = devicein.omega;
				}
			}
		}
		else {
			omega = 0;
			torque = 0;
			power = 0;
			return;
		}

		omega = omegain;
		torque = torquein;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("posn", direction);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		direction = Math.floorMod(NBT.getInt("posn"), directions.size());
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public void onEMP() {}
}
