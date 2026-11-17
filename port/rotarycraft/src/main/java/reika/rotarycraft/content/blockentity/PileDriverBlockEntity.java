/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.content.block.MiningPipeBlock;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.World.TileEntityPileDriver - with at least 80000 Nm and 16384 W per depth step (from either
 * side of its axis) it drives a pile (mining pipe, "pile" shape) down below itself and, each time the pile reaches the bottom, smashes the
 * 5x5 layer there (no corners): stone becomes cobblestone, stone bricks crack, bedrock, shield blocks and mining pipe stay, anything else
 * breaks and drops; obsidian takes 5 blows; netherrack (2), glowstone (3), glass (4) and wool (1) also break that many blocks deep. A
 * cleared layer (only air or liquid left) lets it go one deeper. Each blow throws entities on the ground within 24 blocks up, crushes
 * living things right at the head (max health * armour, everything if no armour), gives players within 15 blocks nausea, and within 5
 * blocks breaks glass, panes, glowstone and plants, turns ice to water (dropping ice), removes cobwebs (dropping string), drops sand and
 * gravel and breaks hanging pictures. The pile is pulled up again every max(300 / (P / P_needed), 1) ticks per block.
 * Not ported: other mods' rocks (GeoStrata), the player-permission check and the mob-spawner item; the splash and lava particles are
 * reduced to one burst each.
 */
public class PileDriverBlockEntity extends PowerReceiverBlockEntity {

	public static final int BASEPOWER = 16384;
	public static final int BASESPEED = 300;

	private static final Map<Block, Integer> HITS_PER_BLOCK = Map.of(Blocks.OBSIDIAN, 5, Blocks.NETHERRACK, -2, Blocks.GLASS, -4, Blocks.GLOWSTONE, -3);

	public int step;
	private int step2;
	private boolean climbing;
	private boolean smashed;
	private final Map<BlockPos, int[]> numHits = new HashMap<>();

	public PileDriverBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.PILE_DRIVER.get(), pos, state, 80000, 1, 16384);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	private static int getHitCount(BlockState s) {
		if (s.is(BlockTags.WOOL))
			return -1;
		return HITS_PER_BLOCK.getOrDefault(s.getBlock(), 0);
	}

	public int getRequiredPower() {
		return BASEPOWER*(step+1);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (this.getBlockMetadata() == 0) {
			read = Direction.NORTH;
			read2 = Direction.SOUTH;
		}
		else {
			read = Direction.EAST;
			read2 = Direction.WEST;
		}
		if (world.isClientSide)
			return;
		this.readPowerDoubleSided();
		int speed = BASESPEED;
		int minpower = this.getRequiredPower();
		if (power >= minpower && torque >= MINTORQUE) {
			if (power > minpower)
				speed = Math.max(BASESPEED/(int)(power/minpower), 1);
			tickcount++;
			if (this.drawPile3(world, pos, speed) || step == 0) {
				climbing = true;
				tickcount = 0;
				BlockPos head = pos.below(step+1);
				if (this.smash(world, head))
					step++;
				head = pos.below(step+1);
				this.bounce(world, head);
				this.dealDamage(world, head);
				this.addNausea(world, head);
				RotarySounds.play(world, pos, RotarySounds.PILEDRIVER.get(), 1, 1);
				this.syncAllData();
			}
		}
	}

	private boolean isPipe(Level world, BlockPos p) {
		return world.getBlockState(p).is(RotaryBlocks.MINING_PIPE.get());
	}

	private boolean drawPile3(Level world, BlockPos pos, int speed) {
		if (climbing && tickcount > speed) {
			if (this.isPipe(world, pos.below(step2+2)))
				world.removeBlock(pos.below(step2+2), false);
			step2--;
			if (world.getBlockState(pos.below(step2)).is(this.getBlockState().getBlock())) {
				climbing = false;
				smashed = false;
			}
			else
				world.removeBlock(pos.below(step2+1), false);
			tickcount = 0;
		}
		if (climbing && tickcount <= speed) {
			if (step2 >= step)
				step2--;
			if (world.getBlockState(pos.below(step2)).is(this.getBlockState().getBlock())) {
				climbing = false;
				smashed = false;
			}
			else
				world.removeBlock(pos.below(step2+1), false);
		}
		if (!climbing) {
			BlockPos p = pos.below(step2+1);
			FluidState f = world.getFluidState(p);
			if (world instanceof ServerLevel sl && !f.isEmpty()) {
				boolean lava = f.is(net.minecraft.tags.FluidTags.LAVA);
				sl.sendParticles(lava ? ParticleTypes.LAVA : ParticleTypes.SPLASH, p.getX()+0.5, p.getY()+2, p.getZ()+0.5, 18, 0.5, 0.1, 0.5, 0.2);
				world.playSound(null, p, lava ? SoundEvents.LAVA_EXTINGUISH : SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1, 1);
			}
			world.setBlockAndUpdate(p, MiningPipeBlock.state(4));
			step2++;
		}
		if (world.getBlockState(pos.below(step2+1)).isAir()) {
			while (world.getBlockState(pos.below(step2+2)).isAir() && pos.getY()-step2-2 > world.getMinBuildHeight() && step == step2) {
				step++;
				step2 = step;
			}
		}
		return step2 == step;
	}

	private boolean smash(Level world, BlockPos c) {
		smashed = true;
		for (int i = -2; i < 3; i++) {
			for (int j = -2; j < 3; j++) {
				if (i*j == 4 || i*j == -4)
					continue;
				BlockPos p = c.offset(i, 0, j);
				BlockState id = world.getBlockState(p);
				if (id.isAir())
					continue;
				for (int h = 1; h <= 4; h++) {
					BlockState id2 = world.getBlockState(p.below(h));
					int hits = getHitCount(id2);
					if (hits < 0 && Math.abs(hits) >= h)
						this.checkIncrementAndBreak(world, p.below(h), id2);
				}
				this.checkIncrementAndBreak(world, p, id);
			}
		}
		RotarySounds.play(world, c, RotarySounds.PILEDRIVER.get(), 1, 1);
		boolean cleared = true;
		for (int i = -2; i < 3; i++)
			for (int j = -2; j < 3; j++) {
				if (i*j == 4 || i*j == -4)
					continue;
				BlockState s = world.getBlockState(c.offset(i, 0, j));
				if (!s.isAir() && s.getFluidState().isEmpty())
					cleared = false;
			}
		this.breakGlass(world, c);
		return cleared;
	}

	private void checkIncrementAndBreak(Level world, BlockPos p, BlockState id) {
		int[] c = numHits.get(p);
		boolean flag = false;
		if (c != null) {
			c[1]++;
			if (c[1] >= c[0]) {
				numHits.remove(p);
				flag = true;
			}
		}
		else {
			int ct = getHitCount(id);
			if (ct <= 0)
				flag = true;
			else
				numHits.put(p.immutable(), new int[] {ct, 0});
		}
		if (flag) {
			BlockState to = this.getBlockProduct(id);
			List<ItemStack> li = Block.getDrops(id, (ServerLevel)world, p, world.getBlockEntity(p));
			if (to != id)
				world.setBlockAndUpdate(p, to);
			if (to.isAir())
				for (ItemStack is : li)
					Containers.dropItemStack(world, p.getX(), p.getY(), p.getZ(), is);
		}
	}

	/** Original BLOCK_CONVERSION (anything not listed becomes air; liquids stay). */
	private BlockState getBlockProduct(BlockState id) {
		if (!id.getFluidState().isEmpty() && id.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)
			return id;
		if (id.is(Blocks.BEDROCK) || id.is(RotaryBlocks.SHIELD_BLOCK.get()))
			return id;
		if (id.is(Blocks.STONE))
			return Blocks.COBBLESTONE.defaultBlockState();
		if (id.is(Blocks.STONE_BRICKS))
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		if (id.is(RotaryBlocks.MINING_PIPE.get()) && id.getValue(MiningPipeBlock.SHAPE) == MiningPipeBlock.Shape.FULL)
			return id;
		return Blocks.AIR.defaultBlockState();
	}

	private void bounce(Level world, BlockPos c) {
		AABB zone = new AABB(c.getX()-2, c.getY(), c.getZ()-2, c.getX()+3, c.getY()+1, c.getZ()+3).inflate(24);
		for (Entity ent : world.getEntitiesOfClass(Entity.class, zone)) {
			if (ent.onGround()) {
				double d = Math.sqrt(ent.distanceToSqr(c.getX(), c.getY(), c.getZ()));
				ent.setDeltaMovement(ent.getDeltaMovement().add(0, 0.5/ReikaMathLibrary.doubpow(d, 0.5), 0));
			}
			ent.hurtMarked = true;
		}
	}

	private void dealDamage(Level world, BlockPos c) {
		for (LivingEntity el : world.getEntitiesOfClass(LivingEntity.class, new AABB(c).inflate(0.5, 2, 0.5))) {
			float dmg = el.getMaxHealth()*el.getArmorValue();
			if (dmg <= 0)
				dmg = Float.MAX_VALUE;
			el.hurt(world.damageSources().inWall(), dmg);
		}
	}

	private void addNausea(Level world, BlockPos c) {
		for (Player ep : world.getEntitiesOfClass(Player.class, new AABB(c).inflate(15)))
			if (!ep.isCreative())
				ep.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 150, 10));
	}

	private void breakGlass(Level world, BlockPos c) {
		int range = 5;
		for (BlockPos p : BlockPos.betweenClosed(c.offset(-range, -range, -range), c.offset(range, range, range))) {
			BlockState b = world.getBlockState(p);
			if (!b.isAir())
				this.breakGlass_do(world, p.immutable(), b);
		}
		AABB nearby = new AABB(c).inflate(range);
		for (HangingEntity e : world.getEntitiesOfClass(HangingEntity.class, nearby))
			e.hurt(world.damageSources().fellOutOfWorld(), 100);
	}

	private void breakGlass_do(Level world, BlockPos p, BlockState id) {
		ItemStack drop = ItemStack.EMPTY;
		if (id.is(Blocks.GLASS) || id.is(Blocks.GLASS_PANE) || id.is(Blocks.GLOWSTONE)
				|| id.is(Blocks.CACTUS) || id.is(Blocks.SUGAR_CANE) || id.is(Blocks.VINE) || id.is(Blocks.LILY_PAD) || id.is(BlockTags.REPLACEABLE_BY_TREES) && !id.is(BlockTags.LEAVES)
				|| id.is(BlockTags.SAPLINGS) || id.is(Blocks.COCOA) || id.is(Blocks.TRIPWIRE)) {
			world.destroyBlock(p, true);
			return;
		}
		if (id.is(Blocks.ICE)) {
			world.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
			drop = new ItemStack(Blocks.ICE);
		}
		if (id.is(Blocks.COBWEB)) {
			world.removeBlock(p, false);
			drop = new ItemStack(Items.STRING);
		}
		if (id.getBlock() instanceof FallingBlock && FallingBlock.isFree(world.getBlockState(p.below())))
			FallingBlockEntity.fall(world, p, id);
		if (!drop.isEmpty())
			Containers.dropItemStack(world, p.getX(), p.getY(), p.getZ(), drop);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (power >= this.getRequiredPower() && torque >= MINTORQUE)
			phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("step", step);
		NBT.putInt("step2", step2);
		NBT.putBoolean("climbing", climbing);
		NBT.putBoolean("smashed", smashed);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		step = NBT.getInt("step");
		step2 = NBT.getInt("step2");
		climbing = NBT.getBoolean("climbing");
		smashed = NBT.getBoolean("smashed");
	}
}
