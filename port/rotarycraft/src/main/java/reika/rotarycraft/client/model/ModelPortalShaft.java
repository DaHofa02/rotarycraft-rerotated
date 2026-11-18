/*******************************************************************************
 * Own model for the port (not converted from the original): the portal shaft. Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
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

/** A bearing block with a glowing collar; the axle (along z) turns with phi. */
public class ModelPortalShaft extends TechneModel {

	private final ModelPart frame;
	private final ModelPart axle;

	public ModelPortalShaft(ModelPart root) {
		super(root);
		frame = root.getChild("frame");
		axle = root.getChild("axle");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 0).addBox(-5, 21, -5, 10, 3, 10).texOffs(0, 14).addBox(-4, 12, -4, 8, 9, 8), PartPose.ZERO);
		root.addOrReplaceChild("axle", CubeListBuilder.create().texOffs(0, 32).addBox(-1, -1, -8, 2, 2, 16).texOffs(40, 0).addBox(-2.5F, -2.5F, -1, 5, 5, 2),
				PartPose.offset(0, 16, 0));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void renderAll(PoseStack pose, VertexConsumer vc, int light, int overlay, BlockEntity te, List<?> li, float phi, float theta) {
		frame.render(pose, vc, light, overlay);
		axle.zRot = (float)Math.toRadians(phi);
		axle.render(pose, vc, light, overlay);
	}
}
