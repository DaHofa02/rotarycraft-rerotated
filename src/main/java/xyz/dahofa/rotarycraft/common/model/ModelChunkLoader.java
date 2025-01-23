// Made with Blockbench 4.12.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports
/*

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import xyz.dahofa.rotarycraft.common.entity.EntityChunkLoader;

public class ModelChunkLoader<T extends EntityChunkLoader> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "modelchunkloader"), "main");
	private final ModelPart group2;
	private final ModelPart group3;
	private final ModelPart group4;
	private final ModelPart group5;
	private final ModelPart group;

	public ModelChunkLoader(ModelPart root) {
		this.group2 = root.getChild("group2");
		this.group3 = this.group2.getChild("group3");
		this.group4 = this.group2.getChild("group4");
		this.group5 = this.group2.getChild("group5");
		this.group = this.group2.getChild("group");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition group2 = partdefinition.addOrReplaceChild("group2", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-8.0F, -3.0F, -8.0F, 16.0F, 3.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition group3 = group2.addOrReplaceChild("group3", CubeListBuilder.create().texOffs(0, 40).addBox(-12.0F, -1.0F, 4.0F, 12.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(42, 40).mirror().addBox(-12.0F, -1.0F, 6.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(29, 40).mirror().addBox(-2.0F, -1.0F, 0.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(6.0F, -4.0F, -5.0F));

		PartDefinition group4 = group2.addOrReplaceChild("group4", CubeListBuilder.create().texOffs(0, 34).mirror().addBox(-11.0F, -4.0F, 4.0F, 10.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(36, 34).addBox(-11.0F, -4.0F, 6.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(25, 34).addBox(-3.0F, -4.0F, 1.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, -4.0F, -5.0F));

		PartDefinition group5 = group2.addOrReplaceChild("group5", CubeListBuilder.create().texOffs(0, 30).mirror().addBox(-10.0F, -7.0F, 4.0F, 8.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(30, 30).addBox(-10.0F, -7.0F, 6.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(21, 30).addBox(-4.0F, -7.0F, 2.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, -4.0F, -5.0F));

		PartDefinition group = group2.addOrReplaceChild("group", CubeListBuilder.create().texOffs(0, 26).mirror().addBox(-9.0F, -10.0F, 4.0F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(24, 26).addBox(-9.0F, -10.0F, 6.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(17, 26).addBox(-5.0F, -10.0F, 3.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, -4.0F, -5.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(EntityChunkLoader entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		group2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}*/