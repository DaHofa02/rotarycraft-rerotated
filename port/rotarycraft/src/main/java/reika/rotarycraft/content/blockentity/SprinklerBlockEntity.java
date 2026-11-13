/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.lib.world.CropHelper;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Farming.TileEntitySprinkler - hangs under a water pipe (180 mB, 3 used a tick) and waters
 * the square of columns within its range (8 with water pressure). Each column is scanned down from its own level for up to 12 blocks
 * (again after 20 ticks or when a block in it changes): fire is put out (chance 1/(20 f)), farmland gets one step wetter (1/(15 f)),
 * crops and growing plants get a random tick (1/(80 f)), saplings a tick right when the column is scanned; farmland, fire, cacti, sugar
 * cane, saplings and opaque blocks (not leaves) end the scan. f = 0.5 + 1.5 * (distance/range)^2, so the middle gets the most. Not
 * ported: wetting machines (no ported machine is Wettable), Growthcraft apples, ReactorCraft radiation clean-up.
 */
public class SprinklerBlockEntity extends SprinklerBaseBlockEntity {

	private enum Effect {
		FIREEXTINGUISH(20), CROPTICK(80), HYDRATEFARMLAND(15);

		private final int randomChance;

		Effect(int c) {
			randomChance = c;
		}

		private boolean doChance(net.minecraft.util.RandomSource rand, float factor) {
			return randomChance <= 0 || rand.nextInt(Math.max(1, (int)(randomChance*factor))) == 0;
		}
	}

	private record Action(BlockPos pos, Block block, Effect effect) {}

	/** Per column (x, z offset), the actions found in its last scan. */
	private final java.util.Map<Long, List<Action>> columns = new java.util.HashMap<>();
	private int age = 20;

	public SprinklerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SPRINKLER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Sprinkler";
	}

	@Override
	public int getCapacity() {
		return 180;
	}

	@Override
	public int getWaterConsumption() {
		return 3;
	}

	@Override
	public Direction getPipeDirection() {
		return Direction.UP;
	}

	@Override
	protected void performEffects(Level world, BlockPos pos) {
		if (world instanceof ServerLevel sl)
			this.hydrate(sl, pos);
		else
			this.spawnParticles(world, pos);
	}

	private void hydrate(ServerLevel world, BlockPos pos) {
		int range = this.getRange();
		boolean rescan = ++age >= 20;
		if (rescan)
			age = 0;
		for (int i = -range; i <= range; i++) {
			for (int k = -range; k <= range; k++) {
				float f = 0.5F+1.5F*(i*i+k*k)/(float)(range*range);
				long key = BlockPos.asLong(i, 0, k);
				List<Action> col = columns.get(key);
				if (col == null || rescan) {
					col = this.scan(world, pos.getX()+i, pos.getZ()+k, pos.getY());
					columns.put(key, col);
				}
				boolean changed = false;
				for (Action a : col) {
					BlockState s = world.getBlockState(a.pos());
					if (!s.is(a.block())) {
						changed = true;
						continue;
					}
					if (a.effect().doChance(rand, f))
						this.doEffect(world, a.pos(), s, a.effect());
				}
				if (changed)
					columns.put(key, this.scan(world, pos.getX()+i, pos.getZ()+k, pos.getY()));
			}
		}
	}

	private void doEffect(ServerLevel world, BlockPos c, BlockState s, Effect e) {
		switch (e) {
			case FIREEXTINGUISH -> {
				world.playSound(null, c, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F+0.4F*rand.nextFloat(), 0.5F+0.5F*rand.nextFloat());
				world.removeBlock(c, false);
			}
			case HYDRATEFARMLAND -> {
				int m = s.getValue(FarmBlock.MOISTURE);
				if (m < FarmBlock.MAX_MOISTURE)
					world.setBlock(c, s.setValue(FarmBlock.MOISTURE, m+1), 3);
			}
			case CROPTICK -> s.randomTick(world, c, world.random);
		}
	}

	/** Original FieldColumn.recalulateLevel. */
	private List<Action> scan(ServerLevel world, int x, int z, int y) {
		List<Action> li = new ArrayList<>();
		for (int dy = y; dy > world.getMinBuildHeight() && y-dy < 12; dy--) {
			BlockPos c = new BlockPos(x, dy, z);
			BlockState s = world.getBlockState(c);
			if (s.isAir())
				continue;
			boolean stop = false;
			if (s.is(Blocks.FIRE)) {
				li.add(new Action(c, s.getBlock(), Effect.FIREEXTINGUISH));
				stop = true;
			}
			else if (s.is(Blocks.FARMLAND)) {
				li.add(new Action(c, s.getBlock(), Effect.HYDRATEFARMLAND));
				stop = true;
			}
			else if (s.getBlock() instanceof SaplingBlock) {
				s.randomTick(world, c, world.random);
				stop = true;
			}
			else if (CropHelper.isCrop(s) || CropHelper.isGrowingPlant(s)) {
				li.add(new Action(c, s.getBlock(), Effect.CROPTICK));
				stop = s.getBlock() instanceof CactusBlock || s.getBlock() instanceof SugarCaneBlock;
			}
			if (stop || (s.isSolidRender(world, c) && !s.is(BlockTags.LEAVES)))
				break;
		}
		return li;
	}

	/** Original spawnParticles: splashes under it, spreading with the range. */
	private void spawnParticles(Level world, BlockPos pos) {
		double py = pos.getY()-0.1875+0.5;
		double r = this.getRange()/10D;
		for (double vel = 0; vel < r; vel += 0.1) {
			int n = rand.nextInt(2)+rand.nextInt(2);
			for (int i = 0; i < n; i++) {
				double px = pos.getX()-1+2*rand.nextFloat(), pz = pos.getZ()-1+2*rand.nextFloat();
				world.addParticle(ParticleTypes.SPLASH, px+0.5, py, pz+0.5, vel*(-1+2*rand.nextFloat())*1.05, 0, vel*(-1+2*rand.nextFloat())*1.05);
			}
		}
	}
}
