/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelAdvancedGear;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.AdvancedGearBlockEntity;

/** Own model and textures (textures/machine/own/): the shaft lies along the machine's input/output axis and turns with phi. */
public class AdvancedGearRenderer extends MachineRenderer<AdvancedGearBlockEntity> {

	private static final ResourceLocation WORM = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/machine/own/worm_gear.png");
	private static final ResourceLocation HIGH = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/machine/own/high_gear.png");

	private static final ResourceLocation COIL = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/machine/own/industrial_coil.png");
	private static final ResourceLocation BEDROCK_COIL = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/machine/own/bedrock_coil.png");

	private static final ResourceLocation CVT = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/machine/own/cvt.png");

	private final ModelAdvancedGear model;

	public AdvancedGearRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelAdvancedGear(ctx.bakeLayer(RotaryModelLayers.ADVANCED_GEAR));
	}

	@Override
	public void render(AdvancedGearBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (tile.hasLevel() && tile.getBlockMetadata()%4 >= 2)
			pose.mulPose(TechneModel.rotation(90, 0, 1, 0));
		model.renderAll(pose, consumer(buf, switch (tile.kind) { case WORM -> WORM; case HIGH -> HIGH; case COIL -> COIL; case BEDROCK_COIL -> BEDROCK_COIL; case CVT -> CVT; }), light, overlay, tile, null, tile.phi, 0);
		pose.popPose();
	}
}
