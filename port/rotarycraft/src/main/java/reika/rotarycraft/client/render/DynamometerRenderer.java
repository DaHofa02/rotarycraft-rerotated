/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelMonitor;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.content.item.PowerFormat;

/** Original: Reika.RotaryCraft.Renders.RenderMonitor - the model plus power, torque and speed written on both faces. */
public class DynamometerRenderer extends MachineRenderer<DynamometerBlockEntity> {

	private final ModelMonitor model;

	public DynamometerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelMonitor(ctx.bakeLayer(RotaryModelLayers.MONITOR));
	}

	@Override
	public void render(DynamometerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int var11 = switch(tile.getBlockMetadata()) {
				case 1 -> 180;
				case 2 -> 90;
				case 3 -> 270;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(var11, 0.0F, 1.0F, 0.0F));
		}
		model.renderAll(pose, consumer(buf, MachineRenderer.tex("monitortex.png")), light, overlay, tile, null, -tile.phi, 0);
		if (isInWorld(tile)) {
			Font var17 = Minecraft.getInstance().font;
			float var10 = 0.6666667F*1.2F;
			pose.scale(var10, -var10, -var10);
			float var112 = 0.016666668F * var10;
			pose.translate(0.0F, 0.5F * var10, 0.07F * var10);
			pose.scale(var112, -var112, var112);
			pose.translate(5, -48, 37);
			for (int i = 0; i < 2; i++) {
				pose.translate(-10*i, 0, -37*2*i-9*i);
				if (i == 1)
					pose.scale(-1, 1, 1);
				draw(var17, pose, buf, "Power:", -37, 140);
				draw(var17, pose, buf, PowerFormat.formatPower(tile.power), -28, 148);
				draw(var17, pose, buf, "Torque:", -37, 164);
				draw(var17, pose, buf, PowerFormat.formatTorque(tile.torque), -28, 172);
				draw(var17, pose, buf, "Speed:", -37, 188);
				draw(var17, pose, buf, PowerFormat.formatSpeed(tile.omega), -28, 196);
			}
		}
		pose.popPose();
	}

	/** Unlit, like the original (lighting disabled for the text). */
	private static void draw(Font font, PoseStack pose, MultiBufferSource buf, String s, int x, int y) {
		font.drawInBatch(s, x, y, 0xffffff, false, pose.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
	}
}
