/*******************************************************************************
 * Own model for the port (not converted from the original): the CCTV camera. Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.model;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A small base plate and post; the camera body with its lens hood turns (phi = yaw) and tilts (theta = pitch) on top. */
public class ModelCctv extends TechneModel {

	private final ModelPart base;
	private final ModelPart head;

	public ModelCctv(ModelPart root) {
		super(root);
		base = root.getChild("base");
		head = root.getChild("head");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-3, 23, -3, 6, 1, 6).texOffs(0, 8).addBox(-1, 18, -1, 2, 5, 2), PartPose.ZERO);
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0).addBox(-2, -2, -4, 4, 4, 8).texOffs(24, 16).addBox(-1.5F, -1.5F, -6, 3, 3, 2),
				PartPose.offset(0, 16, 0));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void renderAll(PoseStack pose, VertexConsumer vc, int light, int overlay, BlockEntity te, List<?> li, float phi, float theta) {
		base.render(pose, vc, light, overlay);
		head.yRot = (float)Math.toRadians(phi);
		head.xRot = (float)Math.toRadians(theta);
		head.render(pose, vc, light, overlay);
	}
}
