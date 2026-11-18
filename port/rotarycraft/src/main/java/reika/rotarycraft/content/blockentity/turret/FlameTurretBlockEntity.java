/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.turret;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.content.blockentity.FuelEngineBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * The flame turret, written for the port from the original's behaviour: needs 32768 W at 512 rad/s or more from below and a burnable fluid
 * in its 4000 mB tank. Every 4 ticks it uses 5 mB and sets the target on fire, with range and damage by fluid: ethanol 1.2 x 32 blocks,
 * 4 damage; jet fuel 1.8 x 32, 6; oil (c:oil) 0.75 x 32, 6; other engine fuels 1 x 32, 3.
 */
public class FlameTurretBlockEntity extends TurretBlockEntity {

	public static final int CAPACITY = 4000;
	public static final TagKey<Fluid> OIL = FluidTags.create(ResourceLocation.fromNamespaceAndPath("c", "oil"));

	private final FluidTank tank = new FluidTank(CAPACITY, fs -> rangeOf(fs.getFluid()) > 0);

	public FlameTurretBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FLAME_TURRET.get(), pos, state, 1, 512, 32768);
	}

	/** Range factor of a fuel (0 = not a fuel). */
	public static float rangeOf(Fluid f) {
		if (f.isSame(RotaryFluids.ETHANOL.get()))
			return 1.2F;
		if (f.isSame(RotaryFluids.JET_FUEL.get()))
			return 1.8F;
		if (f.is(OIL))
			return 0.75F;
		return FuelEngineBlockEntity.isFuel(f) ? 1F : 0;
	}

	public static float damageOf(Fluid f) {
		if (f.isSame(RotaryFluids.ETHANOL.get()))
			return 4;
		if (f.isSame(RotaryFluids.JET_FUEL.get()) || f.is(OIL))
			return 6;
		return 3;
	}

	@Override
	protected int getAmmoSlots() {
		return 0;
	}

	public FluidTank getTank() {
		return tank;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return side == Direction.DOWN ? null : tank;
	}

	@Override
	public int getRange() {
		return tank.isEmpty() ? 0 : (int)(32*rangeOf(tank.getFluid().getFluid()));
	}

	@Override
	public int getOperationTime() {
		return 4;
	}

	@Override
	public boolean isAmmo(ItemStack is) {
		return false;
	}

	@Override
	protected boolean canFire() {
		return tank.getFluidAmount() >= 5;
	}

	@Override
	protected void fire(Level world, Vec3 muzzle, LivingEntity target) {
		float dmg = damageOf(tank.getFluid().getFluid());
		tank.drain(5, IFluidHandler.FluidAction.EXECUTE);
		target.igniteForSeconds(8);
		target.hurt(world.damageSources().inFire(), dmg);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putString("fluid", BuiltInRegistries.FLUID.getKey(tank.getFluid().getFluid()).toString());
		NBT.putInt("amt", tank.getFluidAmount());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(NBT.getString("fluid")));
		int a = NBT.getInt("amt");
		tank.setFluid(a > 0 && f != Fluids.EMPTY ? new FluidStack(f, a) : FluidStack.EMPTY);
	}
}
