/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelGrinder;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.GrinderBlockEntity;

/** Original: Reika.RotaryCraft.Renders.DMI.RenderGrinder (the enchantment glint is not ported, nor are machine enchantments). */
public class GrinderRenderer extends MachineRenderer<GrinderBlockEntity> {

	private final ModelGrinder model;

	public GrinderRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelGrinder(ctx.bakeLayer(RotaryModelLayers.GRINDER));
	}

	@Override
	public void render(GrinderBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		this.setupGL(tile, pose);
		if (isInWorld(tile)) {
			int var11 = switch(tile.getBlockMetadata()) {
				case 0 -> 180;
				case 2 -> 270;
				case 3 -> 90;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation((float)var11-90, 0.0F, 1.0F, 0.0F));
		}
		model.renderAll(pose, consumer(buf, MachineRenderer.tex("grindertex.png")), light, overlay, tile, null, -tile.phi, 0);
		this.closeGL(tile, pose);
	}
}
