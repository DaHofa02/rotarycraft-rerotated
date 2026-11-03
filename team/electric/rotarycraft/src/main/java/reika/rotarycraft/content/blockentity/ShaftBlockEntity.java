/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.api.interfaces.ComplexIO;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.lib.math.ReikaEngLibrary;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntityShaft.
 * Not yet ported: the conversion into a Portal Shaft next to a portal, the ShaftFailureEvent API event and the
 * MRADS32/GIGAWATT achievements (these arrive with the Portal Shaft, the API events and the advancements).
 */
public class ShaftBlockEntity extends OneDTransmitterBlockEntity {

	public int[] readtorque = new int[2];
	public int[] readomega = new int[2];
	public boolean reading2Dir = false; //Is reading a 2-direction block (splitter, cross)

	public float crossphi1 = 0;
	public float crossphi2 = 0;

	private MaterialRegistry type = MaterialRegistry.WOOD;
	private boolean failed = false;

	public ShaftBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SHAFT.get(), pos, state);
	}

	public MaterialRegistry getShaftType() {
		return type;
	}

	public void setShaftType(MaterialRegistry mat) {
		type = mat;
		this.syncAllData();
	}

	@Override
	public String getName() {
		return "Shaft";
	}

	public void fail(Level world, BlockPos pos, boolean speed) {
		failed = true;
		double x = pos.getX()+0.5, y = pos.getY(), z = pos.getZ()+0.5;
		ItemStack item;
		if (speed) {
			world.explode(null, x, y+0.5, z, 1F, Level.ExplosionInteraction.BLOCK);
			item = switch(type) {
				case WOOD -> new ItemStack(RotaryItems.part(CraftingPart.SAWDUST));
				case STONE -> new ItemStack(Items.GRAVEL);
				case STEEL, TUNGSTEN -> new ItemStack(RotaryItems.part(CraftingPart.SCRAP));
				case DIAMOND -> new ItemStack(Items.DIAMOND);
				case BEDROCK -> new ItemStack(RotaryItems.part(CraftingPart.BEDROCKDUST));
			};
			ItemEntity ei = new ItemEntity(world, x, y+1.25, z, item);
			ei.setDeltaMovement(rand.nextFloat()/5, 0.4F+0.6F*rand.nextFloat(), rand.nextFloat()/5);
			if (world.isClientSide)
				return;
			ei.hurtMarked = true;
			if (rand.nextInt(24) == 0)
				world.addFreshEntity(ei);
		}
		else {
			world.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1F, 1F);
			item = switch(type) {
				case WOOD -> new ItemStack(Items.STICK);
				case STONE -> new ItemStack(Items.COBBLESTONE);
				case STEEL, TUNGSTEN -> new ItemStack(RotaryItems.shaftUnitItem(MaterialRegistry.STEEL));
				case DIAMOND -> new ItemStack(Items.DIAMOND);
				case BEDROCK -> new ItemStack(RotaryItems.part(CraftingPart.BEDROCKDUST));
			};
			ItemEntity ei = new ItemEntity(world, x, y+1, z, item);
			ei.setDeltaMovement(rand.nextFloat()/5, rand.nextFloat()/5, rand.nextFloat()/5);
			if (world.isClientSide)
				return;
			ei.hurtMarked = true;
			if (rand.nextInt(24) == 0)
				world.addFreshEntity(ei);
		}
	}

	public void repair() {
		failed = false;
	}

	public boolean failed() {
		return failed;
	}

	public void testFailure() {
		if (ReikaEngLibrary.mat_rotfailure(type.rho, 0.0625, ReikaMathLibrary.doubpow(omega, type.getSpeedForceExponent()), type.tensile)) {
			this.fail(level, worldPosition, true);
		}
		else if (ReikaEngLibrary.mat_twistfailure(torque, 0.0625, type.shear/16D)) {
			this.fail(level, worldPosition, false);
		}
	}

	private void crossReadFromCross(ShaftBlockEntity cross, int dir) {
		reading2Dir = true;
		if (cross.isWritingTo(this)) {
			readomega[dir] = cross.readomega[0];
			readtorque[dir] = cross.readtorque[0];
		}
		else if (cross.isWritingTo2(this)) {
			readomega[dir] = cross.readomega[1];
			readtorque[dir] = cross.readtorque[1];
		}
	}

	@Override
	protected void readFromCross(ShaftBlockEntity cross) {
		reading2Dir = true;
		super.readFromCross(cross);
	}

	private void crossReadFromSplitter(SplitterBlockEntity spl, int dir) {
		reading2Dir = true;
		int sratio = spl.getRatioFromMode();
		if (sratio == 0)
			return;
		boolean favorbent = false;
		if (sratio < 0) {
			favorbent = true;
			sratio = -sratio;
		}
		if (spl.isInlineTarget(worldPosition)) { //We are the inline
			readomega[dir] = spl.omega; //omega always constant
			if (sratio == 1) { //Even split, favorbent irrelevant
				readtorque[dir] = spl.torque/2;
				return;
			}
			if (favorbent) {
				readtorque[dir] = spl.torque/sratio;
			}
			else {
				readtorque[dir] = (int)(spl.torque*((sratio-1D)/(sratio)));
			}
		}
		else if (spl.isBendTarget(worldPosition)) { //We are the bend
			readomega[dir] = spl.omega; //omega always constant
			if (sratio == 1) { //Even split, favorbent irrelevant
				readtorque[dir] = spl.torque/2;
				return;
			}
			if (favorbent) {
				readtorque[dir] = (int)(spl.torque*((sratio-1D)/(sratio)));
			}
			else {
				readtorque[dir] = spl.torque/sratio;
			}
		}
		else { //We are not one of its write-to blocks
			readtorque[dir] = 0;
			readomega[dir] = 0;
		}
	}

	@Override
	protected void readFromSplitter(Level world, BlockPos pos, SplitterBlockEntity spl) {
		reading2Dir = true;
		super.readFromSplitter(world, pos, spl);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		ratio = 1;
		if (failed) {
			torque = 0;
			omega = 0;
			power = 0;

			this.basicPowerReceiver();
			return;
		}
		this.getIOSides(this.getBlockMetadata());
		this.transferPower(world, pos, this.getBlockMetadata());
	}

	public boolean isVertical() {
		return this.getBlockMetadata() >= 4 && this.getBlockMetadata() < 6;
	}

	public boolean isCross() {
		return this.getBlockMetadata() >= 6;
	}

	public void getIOSides(int meta) {
		switch(meta) {
			case 0:
				read = Direction.EAST;
				write = Direction.WEST;
				break;
			case 1:
				read = Direction.WEST;
				write = Direction.EAST;
				break;
			case 2:
				read = Direction.SOUTH;
				write = Direction.NORTH;
				break;
			case 3:
				read = Direction.NORTH;
				write = Direction.SOUTH;
				break;
			case 4:	//moving up
				read = Direction.DOWN;
				write = Direction.UP;
				break;
			case 5:	//moving down
				read = Direction.UP;
				write = Direction.DOWN;
				break;
			case 6:	//cross - has 4 states
				read = Direction.EAST;
				read2 = Direction.SOUTH;
				write = Direction.WEST;
				write2 = Direction.NORTH;
				break;
			case 7:
				read = Direction.EAST;
				read2 = Direction.NORTH;
				write = Direction.WEST;
				write2 = Direction.SOUTH;
				break;
			case 8:
				read = Direction.WEST;
				read2 = Direction.NORTH;
				write = Direction.EAST;
				write2 = Direction.SOUTH;
				break;
			case 9:
				read = Direction.WEST;
				read2 = Direction.SOUTH;
				write = Direction.EAST;
				write2 = Direction.NORTH;
				break;
		}
	}

	private void crossTransfer(Level world, boolean check1, boolean check2) {
		if (check1 && check2) {
			readomega[0] = 0;
			readomega[1] = 0;
			readtorque[0] = 0;
			readtorque[1] = 0;
		}
		BlockEntity te1 = this.getAdjacentTileEntity(read);
		BlockEntity te2 = this.getAdjacentTileEntity(read2);

		if (check1) {
			if (this.isProvider(te1)) {
				if (te1 instanceof ShaftBlockEntity devicein) {
					if (devicein.isCross()) {
						this.crossReadFromCross(devicein, 0);
						return;
					}
					else if (devicein.isWritingTo(this)) {
						readomega[0] = devicein.omega;
						readtorque[0] = devicein.torque;
					}
				}
				if (te1 instanceof SimpleProvider) {
					if (((IOMachineBlockEntity)te1).isWritingTo(this)) {
						readtorque[0] = ((IOMachineBlockEntity)te1).torque;
						readomega[0] = ((IOMachineBlockEntity)te1).omega;
					}
				}
				if (te1 instanceof ShaftPowerEmitter sp) {
					if (sp.isEmitting() && sp.canWriteTo(read.getOpposite())) {
						readtorque[0] = sp.getTorque();
						readomega[0] = sp.getOmega();
					}
				}
				if (te1 instanceof SplitterBlockEntity devicein) {
					if (devicein.isSplitting()) {
						this.crossReadFromSplitter(devicein, 0);
						return;
					}
					else if (devicein.isWritingTo(this)) {
						readtorque[0] = devicein.torque;
						readomega[0] = devicein.omega;
					}
				}
			}
			else {
				readtorque[0] = 0;
				readomega[0] = 0;
			}
		}

		if (check2) {
			if (this.isProvider(te2)) {
				if (te2 instanceof ShaftBlockEntity devicein2) {
					if (devicein2.isCross()) {
						this.crossReadFromCross(devicein2, 1);
						return;
					}
					else if (devicein2.isWritingTo(this)) {
						readomega[1] = devicein2.omega;
						readtorque[1] = devicein2.torque;
					}
				}
				if (te2 instanceof SimpleProvider) {
					readtorque[1] = ((IOMachineBlockEntity)te2).torque;
					readomega[1] = ((IOMachineBlockEntity)te2).omega;
				}
				if (te2 instanceof ShaftPowerEmitter sp) {
					if (sp.isEmitting() && sp.canWriteTo(read2.getOpposite())) {
						readtorque[1] = sp.getTorque();
						readomega[1] = sp.getOmega();
					}
				}
				if (te2 instanceof SplitterBlockEntity devicein2) {
					if (devicein2.isSplitting()) {
						this.crossReadFromSplitter(devicein2, 1);
						return;
					}
					else if (devicein2.isWritingTo(this)) {
						readtorque[1] = devicein2.torque;
						readomega[1] = devicein2.omega;
					}
				}
			}
			else {
				readtorque[1] = 0;
				readomega[1] = 0;
			}
		}

		if (!check1 || !check2)
			return;

		this.writeToPowerReceiver(write, readomega[0], readtorque[0]);
		this.writeToPowerReceiver(write2, readomega[1], readtorque[1]);
	}

	@Override
	protected void transferPower(Level world, BlockPos pos, int meta) {
		if (world.isClientSide)
			return;
		reading2Dir = false;
		if (this.isCross()) {
			this.crossTransfer(world, true, true);
			return;
		}
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
					omegain = omega;
					torquein = torque;
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

		omega = omegain / ratio;
		torque = torquein * ratio;
		power = (long)omega*(long)torque;

		this.basicPowerReceiver();

		if (!type.isInfiniteStrength())
			this.testFailure();
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("failed", failed);
		NBT.putString("shafttype", type.name());

		NBT.putIntArray("readtorque", readtorque);
		NBT.putIntArray("readomega", readomega);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		failed = NBT.getBoolean("failed");
		if (NBT.contains("shafttype"))
			type = MaterialRegistry.valueOf(NBT.getString("shafttype"));

		readtorque = NBT.getIntArray("readtorque");
		readomega = NBT.getIntArray("readomega");
		if (readtorque.length != 2)
			readtorque = new int[2];
		if (readomega.length != 2)
			readomega = new int[2];
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		NBT.putFloat("cphi1", crossphi1);
		NBT.putFloat("cphi2", crossphi2);
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		crossphi1 = NBT.getFloat("cphi1");
		crossphi2 = NBT.getFloat("cphi2");
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		type = input.getOrDefault(RotaryComponents.SHAFT_MATERIAL.get(), MaterialRegistry.WOOD);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(RotaryComponents.SHAFT_MATERIAL.get(), type);
		if (this.isCross())
			components.set(RotaryComponents.SHAFT_CROSS.get(), true);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.25);

		crossphi1 += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(readomega[0]+1, 2), 1.25);
		crossphi2 += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(readomega[1]+1, 2), 1.25);
	}

	@Override
	public void onEMP() {}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		if (this.isCross()) {
			if (this.isWritingTo(io)) {
				return PowerSourceList.getAllFrom(level, read, worldPosition.relative(read), this, caller);
			}
			else {
				return PowerSourceList.getAllFrom(level, read2, worldPosition.relative(read2), this, caller);
			}
		}
		else
			return super.getPowerSources(io, caller);
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		if (this.isCross()) {
			if (dir == read) {
				c.add(this.getAdjacentTileEntity(write));
			}
			else if (dir == read2) {
				c.add(this.getAdjacentTileEntity(write2));
			}
		}
		else {
			super.getAllOutputs(c, dir);
		}
	}
}
