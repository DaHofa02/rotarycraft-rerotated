package xyz.dahofa.rotarycraft.datagen;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.ObjModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.util.TransformationHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.block.CanolaSeedBlock;
import xyz.dahofa.rotarycraft.common.registry.RCBlocks;

import java.util.function.Function;

public class RCBlockStateProvider extends BlockStateProvider {

    public RCBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Names.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        registerBlocks();
    }

    private void registerBlocks() {
        ItemModelProvider itemModelProvider = itemModels();
        blockWithItem(RCBlocks.HSLA_STEEL_BLOCK.get());
        blockWithItem(RCBlocks.BEDROCK_ALLOY_BLOCK.get());
        modelDirectionalBlockWithItem(RCBlocks.GRINDER,
                mcLoc("block/stone"),
                "grinder/grinder.obj",
                itemModelProvider);
        makeCrop((CanolaSeedBlock) RCBlocks.CANOLA_SEED.get(), "canola_seeds_stage", "canola_seeds_stage");
    }

    private void modelDirectionalBlockWithItem(DeferredBlock<?> deferredBlock, ResourceLocation particleResourceLocation, String modelFile, ItemModelProvider im, Property<?>... ignored) {
        ModelFile model = models().getBuilder(deferredBlock.getId().getPath())
                .customLoader(ObjModelBuilder::begin)
                .flipV(true)
                .modelLocation(modLoc("models/block/" + modelFile)).end().guiLight(BlockModel.GuiLight.FRONT)
                .texture("particle", particleResourceLocation)
                .rootTransforms()
                .origin(TransformationHelper.TransformOrigin.CORNER)
                .translation(0.5F, 0, 0.5F)
                .end();

        getVariantBuilder((Block) deferredBlock.get())
                .forAllStatesExcept(state -> ConfiguredModel.builder()
                                .modelFile(model)
                                .rotationY(((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + 180) % 360)
                                .build(),
                        ignored);

        im.getBuilder(deferredBlock.getId().toString())
                .customLoader(ObjModelBuilder::begin)
                .flipV(true)
                .modelLocation(modLoc("models/block/" + modelFile))
                .end()

                .rootTransforms()
                .origin(TransformationHelper.TransformOrigin.CORNER)
                .translation(0.5F, 0, 0.5F)
                .end()

                .transforms()

                .transform(ItemDisplayContext.GUI)
                .rotation(30, 135, 0)
                .scale(0.625F)
                .end()

                .transform(ItemDisplayContext.GROUND)
                .translation(0, 3, 0)
                .scale(0.25F)
                .end()

                .transform(ItemDisplayContext.FIXED)
                .rotation(-90, 0, 0)
                .scale(0.5F)
                .end()

                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(75, 45, 0)
                .translation(0, 2.5F, 0)
                .scale(0.375F)
                .end()

                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                .rotation(75, 45, 0)
                .translation(0, 2.5F, 0)
                .scale(0.375F)
                .end()

                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                .rotation(0, 45, 0)
                .scale(0.4F)
                .end()

                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                .rotation(0, 225, 0)
                .scale(0.4F)
                .end()

                .end();
    }

    private void blockWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }

    public void makeCrop(CropBlock block, String modelName, String textureName) {
        Function<BlockState, ConfiguredModel[]> function = state -> states(state, block, modelName, textureName);

        getVariantBuilder(block).forAllStates(function);
    }

    private ConfiguredModel[] states(BlockState state, CropBlock block, String modelName, String textureName) {
        ConfiguredModel[] models = new ConfiguredModel[1];
        models[0] = new ConfiguredModel(models().crop(modelName + state.getValue(((CanolaSeedBlock) block).getAgeProperty()),
                ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "block/" + textureName +
                        state.getValue(((CanolaSeedBlock) block).getAgeProperty()))).renderType("cutout"));

        return models;
    }
}
