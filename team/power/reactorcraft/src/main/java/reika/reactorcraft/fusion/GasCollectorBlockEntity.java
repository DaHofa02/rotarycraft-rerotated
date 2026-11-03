/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.api.interfaces.RefrigeratorAttachment;
import reika.rotarycraft.content.blockentity.RefrigeratorBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;

/**
 * Original: Reika.ReactorCraft.TileEntities.TileEntityGasCollector - fixed to the face it was placed on, it reads the block behind it:
 * a lit furnace with coal or wood as its fuel gives 10 mB of CO2 per tick; a RotaryCraft refrigerator takes it as an attachment and it
 * collects 2/7 of each cycle's liquid nitrogen as liquid oxygen. Its 1000 mB tank is emptied only from the front.
 */
public class GasCollectorBlockEntity extends RotaryCraftBlockEntity implements PipeConnector, RefrigeratorAttachment {

	private final FluidTank tank = new FluidTank(1000);

	private Direction readDir = Direction.DOWN;

	public int ticks = 512;

	public GasCollectorBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.GAS_COLLECTOR.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Gas Collector";
	}

	/** Original ItemMaterialController COAL or WOOD. */
	private static boolean isCoalOrWood(ItemStack is) {
		return is.is(Items.COAL) || is.is(Items.CHARCOAL) || is.is(Items.COAL_BLOCK) || is.is(ItemTags.LOGS) || is.is(ItemTags.PLANKS)
				|| is.is(ItemTags.WOODEN_SLABS) || is.is(Items.STICK);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (ticks > 0)
			ticks -= 8;
		readDir = Direction.from3DDataValue(this.getBlockMetadata()).getOpposite();
		if (world.isClientSide)
			return;
		BlockPos p = pos.relative(readDir);
		BlockState s = world.getBlockState(p);
		if (s.is(Blocks.FURNACE) && s.getValue(BlockStateProperties.LIT) && world.getBlockEntity(p) instanceof AbstractFurnaceBlockEntity te) {
			ItemStack fuel = te.getItem(1);
			if (!fuel.isEmpty() && isCoalOrWood(fuel)) {
				tank.fill(new FluidStack(ReactorFluids.get("co2"), 10), IFluidHandler.FluidAction.EXECUTE);
				this.setChanged();
			}
		}
		else if (world.getBlockEntity(p) instanceof RefrigeratorBlockEntity te) {
			te.addAttachment(this, readDir.getOpposite());
		}
	}

	public Direction getReadDirection() {
		return readDir;
	}

	public boolean hasFurnace() {
		BlockState s = level.getBlockState(worldPosition.relative(readDir));
		return s.is(Blocks.FURNACE) || level.getBlockEntity(worldPosition.relative(readDir)) instanceof RefrigeratorBlockEntity;
	}

	@Override
	public void onCompleteCycle(int ln2) {
		tank.fill(new FluidStack(ReactorFluids.get("liquid_oxygen"), ln2*2/7), IFluidHandler.FluidAction.EXECUTE);
		this.setChanged();
	}

	public FluidTank getTank() {
		return tank;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p) && this.getFlowForSide(side) != Flow.NONE;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == readDir.getOpposite() ? Flow.OUTPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from != readDir.getOpposite())
			return FluidStack.EMPTY;
		FluidStack fs = tank.drain(maxDrain, action);
		if (!fs.isEmpty() && action.execute())
			this.setChanged();
		return fs;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 1;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return tank.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return 1000;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return false;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return 0;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return tank.getFluid().getFluid().isSame(resource.getFluid()) ? GasCollectorBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return GasCollectorBlockEntity.this.drain(side, max, action);
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
	}
}
