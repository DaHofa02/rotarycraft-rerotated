/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.Blocks.BlockCanola - a crop with ten growth stages (0-9). It needs moist farmland, survives with
 * light 6 or sky access and grows (1/3 chance per random tick) with light 9 or more when nothing solid but non-opaque sits on top.
 * Fully grown it drops a random handful of seeds (fortune raises it), otherwise one seed. Bonemeal does not work (the original
 * denied the BonemealEvent).
 */
public class CanolaBlock extends Block {

	public static final MapCodec<CanolaBlock> CODEC = simpleCodec(CanolaBlock::new);
	public static final int GROWN = 9;
	public static final IntegerProperty AGE = IntegerProperty.create("age", 0, GROWN);

	public CanolaBlock(Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return Block.box(0, 0, 0, 16, 16*getPlantHeight(state.getValue(AGE)), 16);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return Shapes.empty();
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource r) {
		if (!canSurvive(world, pos)) {
			this.die(world, pos);
		}
		else if (canGrowAt(world, pos)) {
			int age = state.getValue(AGE);
			if (age < GROWN && r.nextInt(3) == 0)
				world.setBlock(pos, state.setValue(AGE, age+1), Block.UPDATE_ALL);
		}
	}

	public void die(Level world, BlockPos pos) {
		world.removeBlock(pos, false);
		net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(RotaryItems.CANOLA_SEEDS.get()));
	}

	@Override
	protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moving) {
		if (!world.isClientSide && !canSurvive(world, pos))
			this.die(world, pos);
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
		if (!world.isClientSide && !canSurvive(world, pos))
			this.die(world, pos);
	}

	public static boolean canSurvive(LevelReader world, BlockPos pos) {
		return (world.getMaxLocalRawBrightness(pos) >= 6 || world.canSeeSky(pos)) && isValidFarmBlock(world, pos.below());
	}

	public static boolean canGrowAt(LevelReader world, BlockPos pos) {
		return world.getMaxLocalRawBrightness(pos) >= 9 && isValidFarmBlock(world, pos.below()) && !isBlockedUpwards(world, pos);
	}

	/** Original: something solid on top that is not a full opaque block (glass, slabs, ...) stops the growth. */
	private static boolean isBlockedUpwards(LevelReader world, BlockPos pos) {
		BlockPos p = pos.above();
		BlockState b = world.getBlockState(p);
		return !(b.isSolidRender(world, p) && b.getLightBlock(world, p) >= 15) && b.blocksMotion();
	}

	/** Original isValidFarmBlock: fertile blocks (moist farmland) that sustain crops. */
	public static boolean isValidFarmBlock(BlockGetter world, BlockPos pos) {
		BlockState b = world.getBlockState(pos);
		return !b.isAir() && b.isFertile(world, pos);
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		int fortune = 0;
		ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
		if (tool != null && !tool.isEmpty())
			fortune = EnchantmentHelper.getItemEnchantmentLevel(params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), tool);
		return List.of(getDrops(state.getValue(AGE), fortune, params.getLevel().random));
	}

	public static ItemStack getDrops(int age, int fortune, RandomSource rand) {
		int ndrops = 1;
		if (age == GROWN)
			ndrops = getDrops(fortune, rand);
		return new ItemStack(RotaryItems.CANOLA_SEEDS.get(), ndrops);
	}

	public static int getDrops(int fortune, RandomSource rand) {
		int ndrops = Math.max(fortune*2, (1+rand.nextInt(2))*(2+rand.nextInt(8)+rand.nextInt(5)));
		if (fortune > 0)
			ndrops = Math.max(ndrops, (int)(ndrops*rand.nextDouble()*(1+fortune)));
		return ndrops;
	}

	public static float getPlantHeight(int meta) {
		return Math.max(0.125F, meta/(float)GROWN);
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return new ItemStack(RotaryItems.CANOLA_SEEDS.get());
	}
}
