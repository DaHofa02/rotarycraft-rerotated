/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelGearbox;
import reika.rotarycraft.client.model.ModelGearbox16;
import reika.rotarycraft.client.model.ModelGearbox4;
import reika.rotarycraft.client.model.ModelGearbox8;
import reika.rotarycraft.client.model.ModelGearboxBase;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;

/** Original: Reika.RotaryCraft.Renders.RenderGearbox. The lubricant level overlay and I/O indicator are not ported yet. */
public class GearboxRenderer extends MachineRenderer<GearboxBlockEntity> {

	private final ModelGearbox model2;
	private final ModelGearbox4 model4;
	private final ModelGearbox8 model8;
	private final ModelGearbox16 model16;

	public GearboxRenderer(BlockEntityRendererProvider.Context ctx) {
		model2 = new ModelGearbox(ctx.bakeLayer(RotaryModelLayers.GEARBOX));
		model4 = new ModelGearbox4(ctx.bakeLayer(RotaryModelLayers.GEARBOX4));
		model8 = new ModelGearbox8(ctx.bakeLayer(RotaryModelLayers.GEARBOX8));
		model16 = new ModelGearbox16(ctx.bakeLayer(RotaryModelLayers.GEARBOX16));
	}

	@Override
	public void render(GearboxBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		this.setupGL(tile, pose);
		var vc = consumer(buf, MachineRenderer.tex("transmission/gear/"+tile.getGearboxType().getBaseGearboxTexture()+".png"));
		ModelGearboxBase m = switch(tile.getRatioFromMetadata()) {
			case 4 -> model4;
			case 8 -> model8;
			case 16 -> model16;
			default -> model2;
		};
		if (isInWorld(tile)) {
			int var11 = switch(tile.getBlockMetadata()&3) {
				case 1 -> 180;
				case 2 -> 90;
				case 3 -> 270;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(var11, 0.0F, 1.0F, 0.0F));
			m.supportConsumer = consumer(buf, MachineRenderer.tex("transmission/gear/"+tile.getBearingTier().getBaseGearboxTexture()+".png"));
			m.renderAll(pose, vc, light, overlay, tile, List.of(tile.getBearingTier()), -tile.phi, 0);
		}
		else {
			pose.mulPose(TechneModel.rotation(-90, 0.0F, 1.0F, 0.0F));
			m.supportConsumer = null;
			m.renderAll(pose, vc, light, overlay, tile, null, 0, 0);
		}
		m.supportConsumer = null;
		this.closeGL(tile, pose);
	}
}
