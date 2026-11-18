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
import reika.rotarycraft.client.model.ModelConverter;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/** Own model and textures (textures/machine/own/): the output shaft points the way the machine writes (meta 0 west, 1 east, 2 north, 3 south). */
public class ConverterRenderer<T extends RotaryCraftBlockEntity> extends MachineRenderer<T> {

	private final ResourceLocation texture;
	private final ModelConverter model;

	public ConverterRenderer(BlockEntityRendererProvider.Context ctx, String name) {
		model = new ModelConverter(ctx.bakeLayer(RotaryModelLayers.CONVERTER));
		texture = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/machine/own/"+name+".png");
	}

	@Override
	public void render(T tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		int meta = tile.hasLevel() ? tile.getBlockMetadata()%4 : 0;
		if (meta != 0)
			pose.mulPose(TechneModel.rotation(meta == 1 ? 180 : meta == 2 ? 90 : 270, 0, 1, 0));
		model.renderAll(pose, consumer(buf, texture), light, overlay, tile, null, tile.phi, 0);
		pose.popPose();
	}
}
