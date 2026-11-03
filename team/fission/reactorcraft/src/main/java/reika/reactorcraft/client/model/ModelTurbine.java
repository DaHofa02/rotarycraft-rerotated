/*******************************************************************************
 * Ported by hand from the original model class ModelTurbine (the geometry depends on the stage). Geometry unchanged.
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
 * Original ModelTurbine(stage): two crossed shaft bars and a ring of twisted blades (length, spacing, twist and width by stage), drawn
 * twice (the second ring scaled up and set back). Damage levels thin out the blades. The old-texture villager variant is not ported.
 */
public class ModelTurbine extends TechneModel {

	private final ModelPart shaft1;
	private final ModelPart shaft1a;
	private final ModelPart blade;
	protected final int stage;

	protected final boolean big;

	public ModelTurbine(ModelPart root, int stage) {
		this(root, stage, false);
	}

	protected ModelTurbine(ModelPart root, int stage, boolean big) {
		super(root);
		this.stage = stage;
		this.big = big;
		shaft1 = root.getChild("shaft1");
		shaft1a = root.getChild("shaft1a");
		blade = root.getChild("blade");
	}

	public static LayerDefinition createLayer(int stage) {
		MeshDefinition mesh = new MeshDefinition();
		addParts(mesh.getRoot(), getBladeLength(stage, false), getBladeWidth(stage));
		return LayerDefinition.create(mesh, 128, 128);
	}

	protected static void addParts(PartDefinition root, int len, int w) {
		root.addOrReplaceChild("shaft1", CubeListBuilder.create().texOffs(0, 106).mirror(false).addBox(-2F, -2F, 0F, 4F, 4F, 16F),
				PartPose.offsetAndRotation(0F, 15F, -8F, 0F, 0F, 0.7853982F));
		root.addOrReplaceChild("shaft1a", CubeListBuilder.create().texOffs(0, 106).mirror(false).addBox(-2F, -2F, 0F, 4F, 4F, 16F),
				PartPose.offsetAndRotation(0F, 15F, -8F, 0F, 0F, 0F));
		root.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(-0.5F, -len, -w/2F, 1, len, w),
				PartPose.offsetAndRotation(0F, 15F, 0F, 0F, 0F, 0F));
	}

	@Override
	public void renderAll(PoseStack pose, VertexConsumer vc, int light, int overlay, BlockEntity te, List<?> li, float phi, float theta) {
		double vo = 0.9375;
		int damage = li != null && !li.isEmpty() ? (Integer)li.get(0) : 0;
		pose.pushPose();
		pose.translate(0, vo, 0);
		pose.mulPose(TechneModel.rotation(phi, 0, 0, 1));
		pose.translate(0, -vo, 0);
		this.renderModelDirect(pose, vc, light, overlay, damage, vo);
		pose.popPose();
	}

	private void renderModelDirect(PoseStack pose, VertexConsumer vc, int light, int overlay, int damage, double vo) {
		double dd = 0.25;
		double sc = this.getScaleFactor();
		shaft1.render(pose, vc, light, overlay);
		shaft1a.render(pose, vc, light, overlay);
		if (this.renderTwoStages())
			pose.translate(0, 0, dd);
		this.renderBlades(pose, vc, light, overlay, damage, vo);
		if (this.renderTwoStages()) {
			pose.translate(0, 0, -dd*2);
			pose.translate(0, vo, 0);
			pose.scale((float)sc, (float)sc, 1);
			pose.translate(0, -vo, 0);
			this.renderBlades(pose, vc, light, overlay, damage, vo);
			pose.translate(0, vo, 0);
			pose.scale((float)(1D/sc), (float)(1D/sc), 1);
			pose.translate(0, -vo, 0);
			pose.translate(0, 0, dd);
		}
	}

	protected boolean renderTwoStages() {
		return true;
	}

	private void renderBlades(PoseStack pose, VertexConsumer vc, int light, int overlay, int damage, double vo) {
		double da = getAngularSeparation(stage, big)*(damage+1);
		for (int i = 0; i < 360; i += da) {
			pose.pushPose();
			pose.translate(0, vo, 0);
			pose.mulPose(TechneModel.rotation(i, 0, 0, 1));
			pose.translate(0, -vo, 0);
			pose.mulPose(TechneModel.rotation(-getBladeTwist(stage), 0, 1, 0));
			blade.render(pose, vc, light, overlay);
			pose.popPose();
		}
	}

	/** Original getBladeLength (ModelBigTurbine overrides it with longer blades for 7 stages). */
	public static int getBladeLength(int stage, boolean big) {
		if (big)
			return switch (stage) {
				case 0 -> 20;
				case 1 -> 31;
				case 2 -> 38;
				case 3 -> 46;
				case 4 -> 53;
				case 5 -> 62;
				case 6 -> 72;
				default -> 4;
			};
		return switch (stage) {
			case 0 -> 16;
			case 1 -> 24;
			case 2 -> 28;
			case 3 -> 33;
			case 4 -> 40;
			default -> 4;
		};
	}

	public static int getAngularSeparation(int stage, boolean big) {
		if (big)
			return switch (stage) {
				case 0, 6 -> 4;
				case 1, 2, 3, 4, 5 -> 3;
				default -> 10;
			};
		return switch (stage) {
			case 0, 2, 3, 4 -> 8;
			case 1 -> 5;
			case 5 -> 9;
			default -> 10;
		};
	}

	public static int getBladeTwist(int stage) {
		return switch (stage) {
			case 0 -> 10;
			case 1 -> 15;
			case 2 -> 20;
			case 3 -> 30;
			case 4, 5 -> 45;
			case 6 -> 50;
			default -> 10;
		};
	}

	public static int getBladeWidth(int stage) {
		return switch (stage) {
			case 3 -> 3;
			case 4 -> 4;
			case 5 -> 6;
			case 6 -> 8;
			default -> 2;
		};
	}

	public double getScaleFactor() {
		if (big) {
			double d = stage == 0 ? 1.3 : 1.1;
			if (stage == 3)
				d = 1.075;
			return stage >= 5 ? d*(1-(stage-2)*0.01) : d;
		}
		return stage < 1 ? 1.3 : 1.1;
	}
}
