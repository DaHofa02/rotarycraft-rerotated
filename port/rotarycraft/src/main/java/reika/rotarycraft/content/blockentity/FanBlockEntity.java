/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.api.interfaces.Screwdriverable;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.lib.world.CropHelper;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Farming.TileEntityFan (TileEntityBeamMachine, MachineRegistry.FAN, BlockDMMachine, 6 sides,
 * z-flipped). Takes at least 1024 W from behind and blows {@code 8 + (P - 1024)/1024} blocks ahead (P capped at 2 MW; with the diffuser
 * upgrade /2048), at most "Max Fan Range" (default 128, at least 32); the first opaque or solid block stops it (crops and sprinklers do
 * not). Every entity in the beam box (three blocks wide with the diffuser) is pushed along the beam with at least 1 block per tick:
 * {@code max(|v| + P*1.25E-4/(mass*|d|), 1)} (an eighth of the push with the diffuser; d = distance to the fan). Along the beam (wide
 * harvest, the default, also one block to each side and two up; the screwdriver toggles it while sneaking) each block gets a 1 in
 * {@code max(50, 600 - 25*log2(omega))} chance per tick (tall grass: three times as likely) to be blown away: snow blocks and fire from
 * 64 rad/s, webs from 256, tall grass from 1024, leaves from 4096 (with their drops), ripe crops from 512 (harvested: drops minus a seed,
 * replanted). Cooling fins in the beam lose min(10, 1 + P/32768) degrees per second while hotter than the air. Standing next to fire or
 * lava the fan blows fire along the beam (1 in 60 per soft block and tick). The diffuser upgrade (right-click) makes it a wide fan.
 * <p>
 * Deviations: the original vertical wide harvest overwrote x with z+j (a typo, it ripped blocks at odd places) - here the 3x3 around
 * the beam; the carried mass of mobs was always 0 in the original (ReikaItemHelper.getItemMass looked up a stack in an item map) and is
 * left out; Galacticraft's "no atmosphere" check and the FanHarvestEvent are not ported.
 */
public class FanBlockEntity extends PowerReceiverBlockEntity implements UpgradeableMachine, Screwdriverable {

	public static final long MAXPOWER = 2097152;
	public static final int FALLOFF = 1024;
	public static final int FALLOFF_WIDE = 2048;
	public static final double BASESPEED = 1.25E-4;
	public static final int WEBSPEED = 256;
	public static final int LEAFSPEED = 4096;
	public static final int GRASSSPEED = 1024;
	public static final int FIRESPEED = 64;
	public static final int HARVESTSPEED = 512;

	private int soundTick;
	private Direction facing = Direction.WEST;
	public boolean wideAreaHarvest = true;
	public boolean wideAreaBlow = false;

	public FanBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FAN.get(), pos, state, 1, 1, 1024);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	/** Original TileEntityBeamMachine.getIOSides: input behind, the beam goes the other way. */
	private void getIOSides(int meta) {
		read = switch (meta) {
			case 0 -> Direction.EAST;
			case 1 -> Direction.WEST;
			case 2 -> Direction.NORTH;
			case 3 -> Direction.SOUTH;
			case 4 -> Direction.DOWN;
			default -> Direction.UP;
		};
		facing = read.getOpposite();
	}

	public Direction getFacing() {
		return facing;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		power = (long)omega*(long)torque;
		this.makeBeam(world, pos, this.getBlockMetadata());
		soundTick++;
		if (soundTick >= 27) { //StepTimer(27)
			soundTick = 0;
			if (omega > 0)
				RotarySounds.play(world, pos, RotarySounds.FAN.get(), MachineSounds.isMuffled(world, pos) ? 0.05F : 0.5F, 1);
		}
	}

	public int getRange() {
		if (power < MINPOWER)
			return 0;
		int power2 = (int)Math.min(power-MINPOWER, MAXPOWER);
		int range = 8+power2/(wideAreaBlow ? FALLOFF_WIDE : FALLOFF);
		return Math.min(range, this.getMaxRange());
	}

	public int getMaxRange() {
		return Math.max(32, RotaryConfig.FANRANGE.get());
	}

	/** Original isStoppedBy: crops never stop the air, other non-cube blocks only if solid (sprinklers never), full cubes always. */
	private boolean isStoppedBy(Level world, BlockPos p) {
		BlockState s = world.getBlockState(p);
		if (s.isAir())
			return false;
		if (CropHelper.isCrop(s))
			return false;
		if (!s.isSolidRender(world, p))
			return s.blocksMotion();
		return true;
	}

	private void makeBeam(Level world, BlockPos pos, int meta) {
		if (power < MINPOWER)
			return;
		long power2 = Math.min(power, MAXPOWER);
		int range = this.getRange();
		for (int i = 1; i <= range; i++) {
			if (this.isStoppedBy(world, pos.relative(facing, i))) {
				range = i;
				break;
			}
		}
		AABB zone = wideAreaBlow ? this.getWideBlowZone(meta, range) : this.getBlowZone(range);
		for (Entity caught : world.getEntitiesOfClass(Entity.class, zone)) {
			if (caught.isSpectator())
				continue;
			double mass = MirrorBlockEntity.getEntityMass(caught);
			Vec3 v = caught.getDeltaMovement();
			double vx = v.x, vy = v.y, vz = v.z;
			if (vx < 1 && facing.getStepX() != 0) {
				double d = caught.getX()-pos.getX();
				vx = facing.getStepX()*this.getSpeedStep(vx, d, power2, mass)+(facing.getStepX() > 0 ? 0.004 : 0);
			}
			if (vy < 1 && facing.getStepY() != 0) {
				double d = caught.getY()-pos.getY();
				vy = facing.getStepY()*this.getSpeedStep(vy, d, power2, mass);
			}
			if (vz < 1 && facing.getStepZ() != 0) {
				double d = caught.getZ()-pos.getZ();
				vz = facing.getStepZ()*this.getSpeedStep(vz, d, power2, mass)+(facing.getStepZ() > 0 ? 0.004 : 0);
			}
			if (vx != v.x || vy != v.y || vz != v.z) {
				caught.setDeltaMovement(vx, vy, vz);
				caught.hurtMarked = true; //players move themselves: tell their client
			}
		}
		this.clearBlocks(world, pos, range);
		this.spreadFire(world, pos, meta, range);
	}

	/** The original per-axis push: max(| |v| + base/(mass*|d|) |, 1), base = multiplier*P*BASESPEED (an eighth with the diffuser). */
	private double getSpeedStep(double v, double d, long power2, double mass) {
		if (d == 0)
			d = 1;
		double multiplier = 1D/(d-this.getMaxRange());
		if (d-this.getMaxRange() > 12)
			multiplier = 0;
		if (multiplier > 1 || multiplier < 0)
			multiplier = 1;
		double base = multiplier*power2*BASESPEED*(wideAreaBlow ? 0.125 : 1);
		return Math.max(Math.abs(Math.abs(v)+base/(mass*Math.abs(d))), 1);
	}

	/** Original ReikaAABBHelper.getBeamBox: the fan's block box stretched range blocks along the beam. */
	public AABB getBlowZone(int range) {
		return new AABB(worldPosition).expandTowards(facing.getStepX()*range, facing.getStepY()*range, facing.getStepZ()*range);
	}

	/** One more block to each side across the beam (meta 0/1: along x, 2/3: along z, 4/5: vertical). */
	public AABB getWideBlowZone(int meta, int range) {
		int ex = meta <= 1 ? 0 : 1;
		int ey = meta >= 4 ? 0 : 1;
		int ez = meta == 2 || meta == 3 ? 0 : 1;
		return this.getBlowZone(range).inflate(ex, ey, ez);
	}

	private void clearBlocks(Level world, BlockPos pos, int range) {
		for (int i = 1; i <= range; i++) {
			BlockPos p = pos.relative(facing, i);
			this.rip2(world, p);
			this.enhanceFinPower(world, p);
			if (wideAreaHarvest) {
				if (facing.getStepY() != 0) {
					for (int k = -1; k <= 1; k++)
						for (int j = -1; j <= 1; j++)
							this.rip2(world, p.offset(k, 0, j)); //original: x = z+j (typo); the 3x3 around the beam here
				}
				else {
					Direction left = facing.getCounterClockWise(); //ReikaDirectionHelper.getLeftBy90
					for (int k = -1; k <= 1; k++)
						for (int j = 0; j <= 2; j++)
							this.rip2(world, p.relative(left, k).above(j));
				}
			}
		}
	}

	private void enhanceFinPower(Level world, BlockPos p) {
		if (world.getBlockEntity(p) instanceof CoolingFinBlockEntity te) {
			if (world instanceof ServerLevel sl)
				sl.sendParticles(ParticleTypes.CLOUD, p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, 1, 0.5, 0.5, 0.5, 0);
			if (world.getGameTime()%20 == 0) {
				int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, p);
				if (te.getTemperature() > Tamb)
					te.addTemperature(-((int)Math.min(10, 1+power/32768)));
			}
		}
	}

	/** Original getHarvestingRand. */
	public int getHarvestingRand() {
		return Math.max(50, 600-25*ReikaMathLibrary.logbase2(omega));
	}

	/** Original rip2: blows away snow, webs, leaves, tall grass, fire and ripe crops, each from its own speed on. */
	public void rip2(Level world, BlockPos p) {
		BlockState s = world.getBlockState(p);
		boolean crop = CropHelper.isCrop(s);
		boolean leaves = s.is(BlockTags.LEAVES);
		boolean grass = s.is(Blocks.SHORT_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.LARGE_FERN); //tallgrass (all metadata)
		boolean fire = s.is(BlockTags.FIRE);
		boolean snow = s.is(Blocks.SNOW_BLOCK);
		boolean web = s.is(Blocks.COBWEB);
		if (!(crop || leaves || grass || fire || snow || web))
			return;
		int c = this.getHarvestingRand();
		if (grass)
			c /= 3;
		c = Math.max(1, c);
		if (rand.nextInt(c) > 0)
			return;
		if (web && omega < WEBSPEED || leaves && omega < LEAFSPEED || grass && omega < GRASSSPEED || (fire || snow) && omega < FIRESPEED
				|| crop && omega < HARVESTSPEED)
			return;
		if (!(world instanceof ServerLevel sl))
			return;
		if (crop) {
			for (ItemStack is : CropHelper.harvest(sl, p, s))
				Block.popResource(sl, p, is);
		}
		else {
			if (!fire)
				Block.dropResources(s, sl, p, world.getBlockEntity(p)); //dropBlocks: the block's normal (tool-less) drops
			world.removeBlock(p, false);
		}
	}

	/** Original spreadFire: next to fire or lava the fan blows fire into soft blocks of the beam (and the columns beside and above it). */
	private void spreadFire(Level world, BlockPos pos, int meta, int range) {
		boolean hot = false;
		for (Direction d : Direction.values()) {
			BlockState s = world.getBlockState(pos.relative(d));
			hot |= s.is(BlockTags.FIRE) || s.getFluidState().is(FluidTags.LAVA);
		}
		if (!hot)
			return;
		int a = meta > 1 ? 1 : 0;
		int b = 1-a;
		int[][] offsets = {{0, 0, 0}, {-a, 0, -b}, {-a, 1, -b}, {-a, 2, -b}, {0, 0, 0}, {0, 1, 0}, {0, 2, 0}, {a, 0, b}, {a, 2, b}}; //sic: two at the beam, none at (a, 1, b)
		for (int i = 1; i <= range; i++) {
			BlockPos p = pos.relative(facing, i);
			for (int[] o : offsets) {
				BlockPos q = p.offset(o[0], o[1], o[2]);
				if (rand.nextInt(60) == 0 && soft(world.getBlockState(q)))
					world.setBlockAndUpdate(q, Blocks.FIRE.defaultBlockState());
			}
		}
	}

	/** Original ReikaWorldHelper.softBlocks. */
	private static boolean soft(BlockState s) {
		return s.isAir() || !s.getFluidState().isEmpty() || s.canBeReplaced() || s.is(Blocks.VINE);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (power >= MINPOWER)
			phi = (float)(phi+3*ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05));
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public void onEMP() {}

	public static boolean isDiffuser(ItemStack is) {
		return is.is(RotaryItems.part(CraftingPart.DIFFUSER));
	}

	@Override
	public void upgrade(ItemStack is) {
		if (!wideAreaBlow && isDiffuser(is))
			wideAreaBlow = true;
	}

	@Override
	public boolean canUpgradeWith(ItemStack item) {
		return !wideAreaBlow && isDiffuser(item);
	}

	/** Original ItemScrewdriver: sneaking toggles the wide harvest (and does not turn the fan). */
	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		wideAreaHarvest = !wideAreaHarvest;
		this.syncAllData();
		return true;
	}

	/** Original ItemScrewdriver generic case: cycles through the 6 directions. */
	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		int m = this.getBlockMetadata();
		this.setBlockMetadata(m < 5 ? m+1 : 0);
		this.syncAllData();
		return true;
	}

	/** Original BreakAction.breakBlock: an installed diffuser drops. */
	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		if (wideAreaBlow)
			Block.popResource(world, pos, new ItemStack(RotaryItems.part(CraftingPart.DIFFUSER)));
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("wideh", wideAreaHarvest);
		NBT.putBoolean("wideb", wideAreaBlow);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		wideAreaBlow = NBT.getBoolean("wideb");
		wideAreaHarvest = NBT.getBoolean("wideh");
	}
}
