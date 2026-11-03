/*******************************************************************************
 * Ported by hand from the original model class ModelBigTurbine (extends ModelTurbine; the geometry depends on the stage). Geometry unchanged.
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.client.model;

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

import reika.rotarycraft.client.model.TechneModel;

/**
 * Original ModelBigTurbine(stage): the turbine blades of the high pressure turbine (longer, 7 stages) and a segmented housing ring
 * (two rings of plates around the blades; the first stage also shows the angled inlet vanes).
 */
public class ModelBigTurbine extends ModelTurbine {

	private final ModelPart housing;
	private final ModelPart housing2;
	private final ModelPart shape220b;

	public ModelBigTurbine(ModelPart root, int stage) {
		super(root, stage, true);
		housing = root.getChild("housing");
		housing2 = root.getChild("housing2");
		shape220b = root.getChild("shape220b");
	}

	public static LayerDefinition createLayer(int stage) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		int len = getBladeLength(stage, true);
		addParts(root, len, getBladeWidth(stage));
		int w1 = getHousingLength(stage);
		int w2 = stage == 0 ? 12 : getHousingLength(stage)+1;
		int l1 = len+2;
		int l2 = (int)Math.ceil(len*scale(stage))+2;
		int d = getHousingDepth(stage);
		root.addOrReplaceChild("housing", CubeListBuilder.create().texOffs(58, 11).mirror(false).addBox(-w1, l1, 0, w1*2, 1, d), PartPose.offset(0F, 15F, 0));
		root.addOrReplaceChild("housing2", CubeListBuilder.create().texOffs(58, 11).mirror(false).addBox(-w2, l2, -8, w2*2, 1, d), PartPose.offset(0F, 15F, 0));
		root.addOrReplaceChild("shape220b", CubeListBuilder.create().texOffs(39, 10).mirror(false).addBox(-4F, -16F, 0F, 8, 16, 1),
				PartPose.offsetAndRotation(0F, 34F, 6F, -0.3490659F, 0F, 0F));
		return LayerDefinition.create(mesh, 128, 128);
	}

	private static double scale(int stage) {
		double d = stage == 0 ? 1.3 : 1.1;
		if (stage == 3)
			d = 1.075;
		return stage >= 5 ? d*(1-(stage-2)*0.01) : d;
	}

	@Override
	public void renderAll(PoseStack pose, VertexConsumer vc, int light, int overlay, BlockEntity te, List<?> li, float phi, float theta) {
		super.renderAll(pose, vc, light, overlay, te, li, phi, theta);
		this.renderHousing(pose, vc, light, overlay);
	}

	private void renderHousing(PoseStack pose, VertexConsumer vc, int light, int overlay) {
		int a = 360/getHousingSegments(stage);
		for (int i = 0; i < 360; i += a) {
			pose.pushPose();
			pose.translate(0, 1, 0);
			pose.mulPose(TechneModel.rotation(i, 0, 0, 1));
			pose.translate(0, -1, 0);
			housing.render(pose, vc, light, overlay);
			housing2.render(pose, vc, light, overlay);
			if (stage == 0) {
				double d = 1.25;
				pose.translate(0, -0.3, -0.15);
				pose.scale((float)d, (float)d, (float)d);
				shape220b.render(pose, vc, light, overlay);
			}
			pose.popPose();
		}
	}

	public static int getHousingDepth(int stage) {
		return switch (stage) {
			case 3, 4 -> 7;
			case 5 -> 8;
			case 6 -> 9;
			default -> 6;
		};
	}

	public static int getHousingSegments(int stage) {
		return switch (stage) {
			case 0 -> 8;
			case 1 -> 10;
			case 2, 3 -> 12;
			case 4, 5 -> 18;
			case 6 -> 20;
			default -> 10;
		};
	}

	public static int getHousingLength(int stage) {
		return switch (stage) {
			case 0 -> 9;
			case 1, 2 -> 11;
			case 3 -> 13;
			case 4 -> 10;
			case 5, 6 -> 12;
			default -> 4;
		};
	}
}
