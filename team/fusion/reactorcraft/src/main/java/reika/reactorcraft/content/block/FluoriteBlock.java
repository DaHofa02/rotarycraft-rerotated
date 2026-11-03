/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.block;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.item.enchantment.Enchantments;

import reika.reactorcraft.registry.FluoriteTypes;
import reika.reactorcraft.registry.ReactorItems;
import reika.reactorcraft.registry.ReactorOres;

/**
 * Original: BlockFluorite (block of fluorite, hardness 1.2, resistance 4) and BlockFluoriteOre. Neutrons and radiation "activate"
 * fluorite (original: metadata + 8): it then glows (block 15, ore 12 instead of 6) and gives off coloured particles until a random tick
 * resets it. The ore drops 1 + fortune + random(0 .. 4 + 2*fortune) crystals of its colour and with 40 % chance 1 XP; the block drops
 * itself. Both let light through.
 */
public class FluoriteBlock extends Block {

	public static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");

	public final FluoriteTypes color;
	public final boolean isOre;

	public FluoriteBlock(Properties props, FluoriteTypes color, boolean ore) {
		super(props.randomTicks().lightLevel(s -> s.getValue(ACTIVATED) ? (ore ? 12 : 15) : (ore ? 6 : 0)));
		this.color = color;
		this.isOre = ore;
		this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVATED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ACTIVATED);
	}

	/** Original updateTick: an activated block goes back to normal. */
	@Override
	protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		if (state.getValue(ACTIVATED))
			world.setBlock(pos, state.setValue(ACTIVATED, false), 3);
	}

	/** Original randomDisplayTick: coloured particles around an activated block (white drawn brighter). */
	@Override
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
		if (!state.getValue(ACTIVATED))
			return;
		float r = color.red/255F, g = color.green/255F, b = color.blue/255F;
		DustParticleOptions dust = new DustParticleOptions(new Vector3f(r, g, b), 1);
		double o = 0.125;
		for (int i = 0; i < 4; i++) {
			double x = pos.getX()-o+rand.nextDouble()*(1+2*o);
			double y = pos.getY()-o+rand.nextDouble()*(1+2*o);
			double z = pos.getZ()-o+rand.nextDouble()*(1+2*o);
			world.addParticle(dust, x, y, z, 0, 0, 0);
		}
	}

	@Override
	protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
		return 0;
	}

	@Override
	protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return isOre ? 0.7F : super.getShadeBrightness(state, level, pos);
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> li = new ArrayList<>();
		if (!isOre || ReactorOreBlock.level(params, Enchantments.SILK_TOUCH) > 0) {
			li.add(new ItemStack(this));
			return li;
		}
		int fortune = ReactorOreBlock.level(params, Enchantments.FORTUNE);
		int count = 1+fortune+params.getLevel().random.nextInt(5+2*fortune);
		for (int i = 0; i < count; i++)
			li.add(new ItemStack(ReactorItems.fluorite(color).get()));
		return li;
	}

	@Override
	protected void spawnAfterBreak(BlockState state, ServerLevel world, BlockPos pos, ItemStack tool, boolean dropExperience) {
		super.spawnAfterBreak(state, world, pos, tool, dropExperience);
		if (isOre && dropExperience && world.random.nextFloat() < ReactorOres.FLUORITE.xpDropped)
			this.tryDropExperience(world, pos, tool, net.minecraft.util.valueproviders.ConstantInt.of(1)); //none with silk touch
	}
}
