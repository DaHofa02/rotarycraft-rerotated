/*******************************************************************************
 * Own model for the port (not converted from the original): magnetostatic engine and dynamo. Private NeoForge 1.21.1 port of RotaryCraft;
 * not for distribution.
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

/** Base plate, a drum housing with three winding bands, a terminal box at the energy side (+x) and the turning output shaft (-x). */
public class ModelConverter extends TechneModel {

	private final ModelPart body;
	private final ModelPart rotor;

	public ModelConverter(ModelPart root) {
		super(root);
		body = root.getChild("body");
		rotor = root.getChild("rotor");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-8, 22, -8, 16, 2, 16)
				.texOffs(0, 18).addBox(-5, 10, -6, 11, 12, 12)
				.texOffs(64, 36).addBox(-4, 9.5F, -6.5F, 2, 13, 13).texOffs(64, 36).addBox(-0.5F, 9.5F, -6.5F, 2, 13, 13)
				.texOffs(64, 36).addBox(3, 9.5F, -6.5F, 2, 13, 13)
				.texOffs(64, 24).addBox(6, 13, -3, 2, 6, 6), PartPose.ZERO);
		root.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(64, 0).addBox(-8, -1, -1, 3, 2, 2)
				.texOffs(64, 8).addBox(-6, -2.5F, -2.5F, 1, 5, 5), PartPose.offset(0, 16, 0));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void renderAll(PoseStack pose, VertexConsumer vc, int light, int overlay, BlockEntity te, List<?> li, float phi, float theta) {
		body.render(pose, vc, light, overlay);
		rotor.xRot = (float)Math.toRadians(phi);
		rotor.render(pose, vc, light, overlay);
	}
}
