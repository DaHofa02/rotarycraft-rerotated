/*******************************************************************************
 * Own model for the port (not converted from the original): worm gear, high-ratio gear, industrial coils and CVT. Private NeoForge 1.21.1 port of RotaryCraft;
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

import reika.rotarycraft.content.blockentity.AdvancedGearBlockEntity;

/** Base plate, a housing (small for the worm gear, banded for the high-ratio gear, a wound drum for the coils) and the turning through-shaft (along x). */
public class ModelAdvancedGear extends TechneModel {

	private final ModelPart base;
	private final ModelPart wormHousing;
	private final ModelPart highHousing;
	private final ModelPart coil;
	private final ModelPart cvt;
	private final ModelPart shaft;

	public ModelAdvancedGear(ModelPart root) {
		super(root);
		base = root.getChild("base");
		wormHousing = root.getChild("worm_housing");
		highHousing = root.getChild("high_housing");
		coil = root.getChild("coil");
		cvt = root.getChild("cvt");
		shaft = root.getChild("shaft");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-8, 22, -8, 16, 2, 16), PartPose.ZERO);
		root.addOrReplaceChild("worm_housing", CubeListBuilder.create().texOffs(0, 42).addBox(-5, 12, -5, 10, 10, 10)
				.texOffs(64, 24).addBox(-3, 9, -3, 6, 3, 6), PartPose.ZERO);
		root.addOrReplaceChild("high_housing", CubeListBuilder.create().texOffs(0, 18).addBox(-6, 10, -6, 12, 12, 12)
				.texOffs(64, 36).addBox(-6.5F, 13, -6.5F, 13, 2, 13).texOffs(64, 36).addBox(-6.5F, 18, -6.5F, 13, 2, 13), PartPose.ZERO);
		root.addOrReplaceChild("coil", CubeListBuilder.create().texOffs(0, 18).addBox(-6, 9, -6, 12, 13, 12)
				.texOffs(64, 36).addBox(-6.5F, 11, -6.5F, 13, 2, 13).texOffs(64, 36).addBox(-6.5F, 15, -6.5F, 13, 2, 13)
				.texOffs(64, 36).addBox(-6.5F, 19, -6.5F, 13, 2, 13), PartPose.ZERO);
		root.addOrReplaceChild("cvt", CubeListBuilder.create().texOffs(0, 18).addBox(-7, 12, -6, 14, 10, 12)
				.texOffs(64, 24).addBox(-5.5F, 9, -2.5F, 5, 3, 5).texOffs(64, 24).addBox(1.5F, 10, -1.5F, 3, 2, 3), PartPose.ZERO);
		root.addOrReplaceChild("shaft", CubeListBuilder.create().texOffs(64, 0).addBox(-8, -1, -1, 16, 2, 2)
				.texOffs(64, 8).addBox(-7.5F, -2.5F, -2.5F, 1, 5, 5).texOffs(64, 8).addBox(6.5F, -2.5F, -2.5F, 1, 5, 5), PartPose.offset(0, 16, 0));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void renderAll(PoseStack pose, VertexConsumer vc, int light, int overlay, BlockEntity te, List<?> li, float phi, float theta) {
		base.render(pose, vc, light, overlay);
		AdvancedGearBlockEntity.Kind k = te instanceof AdvancedGearBlockEntity a ? a.kind : AdvancedGearBlockEntity.Kind.WORM;
		(k == AdvancedGearBlockEntity.Kind.HIGH ? highHousing : k.storesEnergy() ? coil : k == AdvancedGearBlockEntity.Kind.CVT ? cvt : wormHousing).render(pose, vc, light, overlay);
		shaft.xRot = (float)Math.toRadians(phi);
		shaft.render(pose, vc, light, overlay);
	}
}
