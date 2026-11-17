/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.Tags;

import reika.rotarycraft.content.blockentity.GprBlockEntity;
import reika.rotarycraft.content.menu.GprMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.Blocks.BlockGPR - a full block, steel with a top showing the biome it was set down in (getBiomeDesign:
 * grass, mushroom, jungle, hills, ocean, desert, snow, nether, end, swamp, ice; the property "design"). Set down facing north or south,
 * its slice runs east-west. A right click opens the radar GUI.
 * Changes: the biome design is chosen by 1.21 biome tags for the original's biome lists and BiomeDictionary types.
 */
public class GprBlock extends MachineBlock {

	public static final IntegerProperty DESIGN = IntegerProperty.create("design", 0, 10);

	public GprBlock(Properties props) {
		super(props, RotaryBlockEntities.GPR, Placement.NONE, false);
		this.registerDefaultState(this.stateDefinition.any().setValue(DESIGN, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		super.createBlockStateDefinition(b);
		b.add(DESIGN);
	}

	public static int getBiomeDesign(Holder<Biome> b) {
		if (b.is(Biomes.MUSHROOM_FIELDS))
			return 1;
		if (b.is(BiomeTags.IS_JUNGLE))
			return 2;
		if (b.is(Biomes.FROZEN_OCEAN) || b.is(Biomes.DEEP_FROZEN_OCEAN) || b.is(Biomes.FROZEN_RIVER))
			return 10;
		if (b.is(BiomeTags.IS_NETHER))
			return 7;
		if (b.is(BiomeTags.IS_END))
			return 8;
		if (b.is(Tags.Biomes.IS_SWAMP))
			return 9;
		if (b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_RIVER) || b.is(BiomeTags.IS_BEACH) && !b.is(Tags.Biomes.IS_SANDY))
			return 4;
		if (b.is(Tags.Biomes.IS_DESERT) || b.is(BiomeTags.IS_BADLANDS) || b.is(BiomeTags.IS_BEACH))
			return 5;
		if (b.is(Tags.Biomes.IS_SNOWY) || b.is(BiomeTags.IS_TAIGA))
			return 6;
		if (b.is(BiomeTags.IS_MOUNTAIN) || b.is(BiomeTags.IS_HILL))
			return 3;
		return 0;
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		world.setBlock(pos, world.getBlockState(pos).setValue(DESIGN, getBiomeDesign(world.getBiome(pos))), 3);
		if (placer != null && world.getBlockEntity(pos) instanceof GprBlockEntity te) {
			int i = Mth.floor(placer.getYRot()*4F/360F+0.5D) & 3;
			te.setDirection(i == 0 || i == 2);
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof GprBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new GprMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
