/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft) / Mojang (1.7.10 ModelChest geometry). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** The 1.7.10 vanilla ModelChest the original RenderScaleChest used (its chest.png is laid out for it): lid, knob and body. */
public class ModelScaleChest {

	private final ModelPart lid, knob, body;

	public ModelScaleChest(ModelPart root) {
		lid = root.getChild("lid");
		knob = root.getChild("knob");
		body = root.getChild("body");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(0, -5, -14, 14, 5, 14), PartPose.offset(1, 7, 15));
		root.addOrReplaceChild("knob", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -2, -15, 2, 4, 1), PartPose.offset(8, 7, 15));
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 19).addBox(0, 0, 0, 14, 10, 14), PartPose.offset(1, 6, 1));
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** Original ModelChest.renderAll with the lid opened by {@code lidAngle} (0-1, eased as in RenderScaleChest). */
	public void render(PoseStack pose, VertexConsumer vc, int light, int overlay, float lidAngle) {
		float f = 1-lidAngle;
		f = 1-f*f*f;
		lid.xRot = -(f*(float)Math.PI/2F);
		knob.xRot = lid.xRot;
		lid.render(pose, vc, light, overlay);
		knob.render(pose, vc, light, overlay);
		body.render(pose, vc, light, overlay);
	}
}
