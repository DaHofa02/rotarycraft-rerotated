/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import com.mojang.authlib.GameProfile;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.content.block.BorerBlock;
import reika.rotarycraft.content.block.MiningPipeBlock;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Production.TileEntityBorer (MachineRegistry.BORER, a TileEntityBeamMachine): the boring
 * machine. It reads shaft power from its back (meta 0 east, 1 west, 2 north, 3 south) and digs a tunnel the other way, one slice per
 * operation: a 7 wide, 5 high pattern from its own height upwards, centred on its column ({@link #cutShape}, chosen in the GUI - nothing
 * selected: it does not dig; the first slice is always cut whole). Before each slice it adds up what it needs: per block
 * {@code 64*10*hardness} W ({@code 64} times the config factor) and {@code ceilPseudo2Exp(10*hardness)} Nm (air and liquids count
 * nothing); unbreakable blocks (hardness < 0) and the Borer Shield Block jam it, as does too little power or torque (comparator 15 while
 * jammed). The cut blocks' drops (and the contents of containers it digs out) go whole into an adjacent inventory, else onto the ground
 * above it ("Drops Off" in the GUI: destroyed); each cut cell is filled with a Mining Pipe (the first slice with full blocks), sand and
 * gravel resting on the top of the tunnel become sandstone and stone. RotaryCraft machines, bedrock, end portal frames and blocks a
 * protection mod refuses (a block-break event as the placer) are not cut; the latter jams it and tells the placer. A slice takes
 * {@code max(1, 720 - 40*log2(omega+1))} ticks (DurationRegistry.BORER), through air every 5 ticks; existing mining pipes along the way
 * are skipped. Without power the head goes back to the machine. With "Borer Requires Maintenance" the drill wears out after 256 slices and
 * is replaced by right-clicking with a drill.
 * Not ported: machine enchantments (fortune, efficiency, silk touch, sharpness - the original MachineEnchantmentHandler), spawners as
 * RotaryCraft spawner items (they are cut like other blocks, vanilla gives no drop), forced world generation ahead (1.21 generates the
 * chunks it reads), the BorerDigEvent / HarvestDropsEvent, Twilight Forest blocks, achievements, the WAILA power line.
 */
public class BorerBlockEntity extends PowerReceiverBlockEntity {

	/** Original DIGPOWER base (times ConfigRegistry.BORERPOW). */
	public static final int DIGPOWER_BASE = 64;

	public static final int WIDTH = 7, HEIGHT = 5;

	public boolean drops = true;
	public final boolean[][] cutShape = new boolean[WIDTH][HEIGHT];

	private Direction facing = Direction.WEST;
	private int pipemeta;
	private int pipemeta2;
	private int reqpow;
	private int mintorque;
	private int step = 1;
	private boolean jammed;
	private boolean nodig;
	private boolean isMiningAir;
	private boolean hitProtection;
	private int notifiedPlayer;
	private int durability = maxDurability();
	private int soundtick;

	public BorerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.BORER.get(), pos, state, 1, 1, 1);
	}

	private static int maxDurability() {
		return RotaryConfig.BORERMAINTAIN.get() ? 256 : Integer.MAX_VALUE;
	}

	public static int getDigPower() {
		return (int)(DIGPOWER_BASE*RotaryConfig.BORERPOW.get());
	}

	@Override
	public String getName() {
		return "Boring Machine";
	}

	/** Original TileEntityBeamMachine.getIOSides. */
	private void getIOSides(int metadata) {
		switch (metadata) {
			case 0 -> {
				read = Direction.EAST;
				pipemeta = 0;
			}
			case 1 -> {
				read = Direction.WEST;
				pipemeta = 0;
			}
			case 2 -> {
				read = Direction.NORTH;
				pipemeta = 2;
			}
			case 3 -> {
				read = Direction.SOUTH;
				pipemeta = 2;
			}
			default -> {}
		}
		if (read != null)
			facing = read.getOpposite();
	}

	public Direction getFacing() {
		return facing;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		tickcount++;
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		this.updateActive(world, pos, state);
		if (power <= 0) {
			this.setJammed(false);
			if (step != 1)
				this.reset();
			return;
		}
		if (hitProtection && notifiedPlayer < 10 && world.getGameTime()%100 == 0) {
			Player ep = this.getPlacer();
			if (ep != null) {
				notifiedPlayer++;
				ep.sendSystemMessage(Component.literal("Your "+this.getName()+" has hit a protected area at "+this.getHeadX()+", "+this.getHeadZ()+" and has jammed."));
			}
		}
		if (durability <= 0) {
			if (tickcount%5 == 0) {
				world.playSound(null, pos, SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 0.75F, 0.05F);
				this.smoke((ServerLevel)world, pos);
			}
			return;
		}
		nodig = true;
		for (int i = 0; i < WIDTH && nodig; i++)
			for (int j = 0; j < HEIGHT && nodig; j++)
				if (cutShape[i][j])
					nodig = false;
		if (jammed && tickcount%5 == 0) {
			world.playSound(null, pos, SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 0.75F, 1F);
			this.smoke((ServerLevel)world, pos);
		}
		if (nodig || omega <= 0)
			return;
		if (tickcount == 1 || step == 1)
			isMiningAir = this.checkMiningAir(world, pos);
		if (soundtick > 0)
			soundtick--;
		if (tickcount >= this.getOperationTime() || (isMiningAir && tickcount%5 == 0)) {
			this.skipMiningPipes(world, pos, 0, 128);
			this.calcReqPowerSafe(world, pos);
			if (power >= reqpow && reqpow != -1) {
				this.setJammed(false);
				this.safeDig(world, pos);
				if (!isMiningAir) {
					if (soundtick == 0) {
						world.playSound(null, pos, RotarySounds.RUMBLE.get(), SoundSource.BLOCKS, 1, 1);
						soundtick = 5;
					}
					durability--;
				}
			}
			else {
				this.setJammed(true);
			}
			tickcount = 0;
			isMiningAir = false;
		}
	}

	private void smoke(ServerLevel world, BlockPos pos) {
		for (int i = 0; i < 6; i++) {
			world.sendParticles(ParticleTypes.SMOKE, pos.getX()+rand.nextDouble(), pos.getY()+1+rand.nextDouble()*0.2, pos.getZ()+rand.nextDouble(), 1, 0, 0, 0, 0);
			world.sendParticles(ParticleTypes.CRIT, pos.getX()+rand.nextDouble(), pos.getY()+1+rand.nextDouble()*0.2, pos.getZ()+rand.nextDouble(), 1, 0, 0, 0, 0);
		}
	}

	/** Original getActiveTexture: the front lights up while the power covers the current slice. */
	public boolean isActive() {
		return power > 0 && power >= reqpow && torque >= mintorque;
	}

	private void updateActive(Level world, BlockPos pos, BlockState state) {
		boolean active = this.isActive();
		if (state.hasProperty(BorerBlock.ACTIVE) && state.getValue(BorerBlock.ACTIVE) != active)
			world.setBlock(pos, state.setValue(BorerBlock.ACTIVE, active), 3);
	}

	/** The front faces the dig direction (original BlockDMachine icons), kept in the blockstate for the model. */
	@Override
	public void setBlockMetadata(int meta) {
		super.setBlockMetadata(meta);
		this.getIOSides(meta);
		if (level != null && !level.isClientSide) {
			BlockState bs = this.getBlockState();
			if (bs.hasProperty(BorerBlock.FACING))
				level.setBlock(worldPosition, bs.setValue(BorerBlock.FACING, facing), 3);
		}
	}

	public int getHeadX() {
		return worldPosition.getX()+facing.getStepX()*step;
	}

	public int getHeadZ() {
		return worldPosition.getZ()+facing.getStepZ()*step;
	}

	public boolean isJammed() {
		return jammed;
	}

	private void setJammed(boolean jam) {
		boolean old = jammed;
		jammed = jam;
		if (old != jammed) {
			level.updateNeighborsAt(worldPosition, this.getBlockState().getBlock());
			level.updateNeighbourForOutputSignal(worldPosition, this.getBlockState().getBlock());
			this.syncAllData();
		}
	}

	/** Original reset (GUI "Reset Pos'n", and every tick without power): the head goes back to the machine. */
	public void reset() {
		step = 1;
		this.syncAllData();
	}

	/** Original repair: a new drill once the old one is worn out (only with "Borer Requires Maintenance"). */
	public boolean repair() {
		if (durability > 0)
			return false;
		durability = maxDurability();
		return true;
	}

	public int getDurability() {
		return durability;
	}

	public void toggleDrops() {
		drops = !drops;
		this.setChanged();
		this.syncAllData();
	}

	/** Original PacketHandlerCore BORER: sets one cell of the pattern (i = column 0-6, j = row 0-4 from the top). */
	public void setCut(int i, int j, boolean cut) {
		cutShape[i][j] = cut;
		this.setChanged();
		this.syncAllData();
	}

	/** Original PacketHandlerCore BORERTOGGLEALL: inverts every cell. */
	public void toggleAll() {
		for (int i = 0; i < WIDTH; i++)
			for (int j = 0; j < HEIGHT; j++)
				cutShape[i][j] = !cutShape[i][j];
		this.setChanged();
		this.syncAllData();
	}

	/** World position of pattern cell (i, j) in the current slice. */
	private BlockPos cell(BlockPos pos, int i, int j) {
		int a = this.getBlockMetadata() > 1 ? 1 : 0;
		int b = 1-a;
		return new BlockPos(pos.getX()+step*facing.getStepX()+a*(i-3), pos.getY()+step*facing.getStepY()+(4-j), pos.getZ()+step*facing.getStepZ()+b*(i-3));
	}

	private boolean isCut(int i, int j) {
		return cutShape[i][j] || step == 1;
	}

	private boolean checkMiningAir(Level world, BlockPos pos) {
		for (int i = 0; i < WIDTH; i++)
			for (int j = 0; j < HEIGHT; j++)
				if (this.isCut(i, j) && !world.getBlockState(this.cell(pos, i, j)).is(Blocks.AIR))
					return false;
		return true;
	}

	/** Original skipMiningPipes: a slice that holds only mining pipes running along the tunnel (or full ones) is stepped over. */
	private void skipMiningPipes(Level world, BlockPos pos, int stepped, int max) {
		if (stepped >= max)
			return;
		boolean allpipe = true;
		boolean haspipe = false;
		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				if (this.isCut(i, j)) {
					BlockState bs = world.getBlockState(this.cell(pos, i, j));
					if (bs.is(RotaryBlocks.MINING_PIPE.get())) {
						haspipe = true;
						MiningPipeBlock.Shape s = bs.getValue(MiningPipeBlock.SHAPE);
						Direction dir = s == MiningPipeBlock.Shape.X ? Direction.EAST : s == MiningPipeBlock.Shape.Z ? Direction.SOUTH : null;
						int dx = dir != null ? dir.getStepX() : 0, dz = dir != null ? dir.getStepZ() : 0;
						if (s != MiningPipeBlock.Shape.FULL && (Math.abs(dx) != Math.abs(facing.getStepX()) || Math.abs(dz) != Math.abs(facing.getStepZ())))
							allpipe = false;
					}
				}
			}
		}
		if (haspipe && allpipe) {
			step++;
			this.skipMiningPipes(world, pos, stepped+1, max);
		}
	}

	/** Original ignoreBlockExistence: air and liquids cost nothing. */
	private static boolean ignoreBlockExistence(BlockState b) {
		return b.isAir() || b.getBlock() instanceof LiquidBlock;
	}

	private void calcReqPowerSafe(Level world, BlockPos pos) {
		try {
			this.calcReqPower(world, pos);
		}
		catch (RuntimeException e) {
			reika.rotarycraft.RotaryCraft.LOGGER.error("{} triggered an exception mining a chunk, probably during worldgen!", this.getName(), e);
			reqpow = -1;
		}
	}

	private void calcReqPower(Level world, BlockPos pos) {
		reqpow = 0;
		mintorque = 0;
		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				if (this.isCut(i, j)) {
					this.reqPowAdd(world, this.cell(pos, i, j));
					if (reqpow == -1)
						return;
				}
			}
		}
		if (torque < mintorque)
			reqpow = -1;
	}

	private void reqPowAdd(Level world, BlockPos p) {
		if (step > 30000000) {
			reqpow = -1;
			return;
		}
		BlockState b = world.getBlockState(p);
		if (ignoreBlockExistence(b))
			return;
		float hard = b.getDestroySpeed(world, p);
		if (hard < 0)
			reqpow = -1;
		else if (b.is(RotaryBlocks.SHIELD_BLOCK.get()))
			reqpow = -1;
		else {
			reqpow += (int)(getDigPower()*10*hard);
			mintorque += calculateTorqueForHardness(hard, 0);
		}
	}

	/** Original calculateTorqueForHardness (sharpness enchantment levels lower it; not ported, always 0 here). */
	public static int calculateTorqueForHardness(float hard, int sharp) {
		float c = 10-0.5F*sharp;
		int add = ceilPseudo2Exp((int)(c*hard));
		if (sharp > 0)
			add = Math.min(add, ReikaMathLibrary.intpow2(2, 10-sharp/3));
		return add;
	}

	/** DragonAPI ReikaMathLibrary.ceil2exp. */
	public static int ceil2exp(int val) {
		if (val <= 0)
			return 0;
		val = val >> 1 | --val;
		val = val >> 2 | val;
		val = val >> 4 | val;
		val = val >> 8 | val;
		val = val >> 16 | val;
		return val+1;
	}

	/** DragonAPI ReikaMathLibrary.ceilPseudo2Exp: the next value of 1, 2, 3, 4, 6, 8, 12, 16, 24 ... */
	public static int ceilPseudo2Exp(int val) {
		int pow = ceil2exp(val);
		int prev = ReikaMathLibrary.intpow2(2, ReikaMathLibrary.logbase2(pow)-1);
		int mid = prev*3/2;
		if (prev >= val)
			return prev;
		return mid >= val ? mid : pow;
	}

	public int getRequiredTorque() {
		return mintorque;
	}

	public long getRequiredPower() {
		return reqpow;
	}

	public int getStep() {
		return step;
	}

	/** Original getCurrentRequiredPower (shown by WAILA in the original). */
	public String getCurrentRequiredPower() {
		if (reqpow < 0)
			return "Infinity - Blocked";
		return String.format("Required Power: %.3f%sW; Required Torque: %.3f%sNm", ReikaMathLibrary.getThousandBase(reqpow),
				reika.rotarycraft.lib.math.ReikaEngLibrary.getSIPrefix(reqpow), ReikaMathLibrary.getThousandBase(mintorque),
				reika.rotarycraft.lib.math.ReikaEngLibrary.getSIPrefix(mintorque));
	}

	/** Original support: sand and gravel resting on the top of the cut become sandstone and stone. */
	private void support(Level world, BlockPos pos) {
		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				if (this.isCut(i, j)) {
					BlockPos up = this.cell(pos, i, j).above();
					BlockState id = world.getBlockState(up);
					boolean sand = id.is(Blocks.SAND) || id.is(Blocks.RED_SAND); //original: Blocks.sand, both metadata
					if ((sand || id.is(Blocks.GRAVEL)) && this.checkTop(i, j))
						world.setBlock(up, (sand ? Blocks.SANDSTONE : Blocks.STONE).defaultBlockState(), 3);
				}
			}
		}
	}

	private boolean checkTop(int i, int j) {
		while (j > 0) {
			if (cutShape[i][--j])
				return false;
		}
		return true;
	}

	/** Original playerCanBreakAt: a block-break event as the placer (protection mods can refuse it). */
	private boolean canBreakAt(ServerLevel world, BlockPos p, BlockState b) {
		FakePlayer fp = this.getPlacerUUID() != null ? FakePlayerFactory.get(world, new GameProfile(this.getPlacerUUID(), this.getPlacerName() != null && !this.getPlacerName().isEmpty() ? this.getPlacerName() : "[RotaryCraft]"))
				: FakePlayerFactory.getMinecraft(world);
		return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(world, p, b, fp)).isCanceled();
	}

	/** Original dropBlocks: whether the cell may be cut; hands out its drops. */
	private boolean dropBlocks(ServerLevel world, BlockPos pos, BlockPos p, BlockState b) {
		if (b.is(Blocks.BEDROCK) || b.is(Blocks.END_PORTAL_FRAME))
			return false;
		if (!this.canBreakAt(world, p, b)) {
			hitProtection = true;
			return false;
		}
		BlockEntity tile = world.getBlockEntity(p);
		if (tile instanceof RotaryCraftBlockEntity)
			return false;
		if (drops && !b.isAir()) {
			if (tile instanceof Container ii) {
				List<ItemStack> contents = new ArrayList<>();
				for (int i = 0; i < ii.getContainerSize(); i++) {
					if (!ii.getItem(i).isEmpty())
						contents.add(ii.getItem(i).copy());
				}
				ii.clearContent();
				for (ItemStack is : contents)
					this.output(world, pos, is);
			}
			for (ItemStack is : Block.getDrops(b, world, p, tile))
				this.output(world, pos, is);
		}
		return true;
	}

	/** Into an adjacent inventory if it fits whole, else dropped above the machine (ReikaItemHelper.dropItem with speed factor 3). */
	private void output(ServerLevel world, BlockPos pos, ItemStack is) {
		if (is.isEmpty() || this.chestCheck(world, pos, is))
			return;
		ItemEntity ei = new ItemEntity(world, pos.getX()+0.5, pos.getY()+1.125, pos.getZ()+0.5, is.copy());
		ei.setPickUpDelay(10);
		ei.setDeltaMovement((-0.1+0.2*rand.nextDouble())*3, 0.2*rand.nextDouble()*3, (-0.1+0.2*rand.nextDouble())*3);
		world.addFreshEntity(ei);
	}

	/** Original chestCheck: the first adjacent inventory (down, up, north, south, west, east) that takes the whole stack. */
	private boolean chestCheck(Level world, BlockPos pos, ItemStack is) {
		for (Direction dir : Direction.values()) {
			IItemHandler h = world.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(dir), dir.getOpposite());
			if (h != null && ItemHandlerHelper.insertItemStacked(h, is.copy(), true).isEmpty()) {
				ItemHandlerHelper.insertItemStacked(h, is.copy(), false);
				return true;
			}
		}
		return false;
	}

	private void safeDig(Level world, BlockPos pos) {
		try {
			this.dig((ServerLevel)world, pos);
		}
		catch (RuntimeException e) {
			reika.rotarycraft.RotaryCraft.LOGGER.error("{} triggered an exception mining a chunk, probably during worldgen!", this.getName(), e);
		}
	}

	private void dig(ServerLevel world, BlockPos pos) {
		this.support(world, pos);
		if (step == 1) {
			pipemeta2 = pipemeta;
			pipemeta = 3;
		}
		else if (pipemeta > 2 && pipemeta2 != 3) {
			pipemeta = pipemeta2;
		}
		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				if (this.isCut(i, j)) {
					BlockPos p = this.cell(pos, i, j);
					BlockState bk = world.getBlockState(p);
					if (this.dropBlocks(world, pos, p, bk)) {
						if (!bk.isAir())
							world.levelEvent(2001, p, Block.getId(bk)); //ReikaSoundHelper.playBreakSound
						world.setBlock(p, MiningPipeBlock.state(pipemeta), 3);
					}
					else {
						step--;
					}
				}
			}
		}
		step++;
		this.syncAllData();
	}

	/** Original DurationRegistry.BORER: DurationFormula(720, 40) = max(1, 720 - 40*log2(omega+1)) ticks (efficiency not ported). */
	public int getOperationTime() {
		return (int)Math.max(1, 720-40*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	@Override
	public int getRedstoneOverride() {
		return jammed ? 15 : 0;
	}

	/** Original hasWork (BuildCraft IHasWork). */
	public boolean hasWork() {
		return !nodig && !isMiningAir;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("step", step);
		NBT.putBoolean("jam", jammed);
		NBT.putInt("dura", durability);
		NBT.putInt("reqpow", reqpow);
		NBT.putInt("reqtrq", mintorque);
		//The original sent these with the whole tile NBT; the GUI shows them
		NBT.putBoolean("drops", drops);
		for (int i = 0; i < WIDTH; i++)
			for (int j = 0; j < HEIGHT; j++)
				NBT.putBoolean("cut"+(i*7+j), cutShape[i][j]);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		step = NBT.contains("step") ? NBT.getInt("step") : 1;
		jammed = NBT.getBoolean("jam");
		durability = NBT.contains("dura") ? NBT.getInt("dura") : maxDurability();
		reqpow = NBT.getInt("reqpow");
		mintorque = NBT.getInt("reqtrq");
		drops = !NBT.contains("drops") || NBT.getBoolean("drops");
		for (int i = 0; i < WIDTH; i++)
			for (int j = 0; j < HEIGHT; j++)
				cutShape[i][j] = NBT.getBoolean("cut"+(i*7+j));
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}
}
