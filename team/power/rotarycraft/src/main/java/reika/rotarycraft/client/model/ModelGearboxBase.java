/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.model;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Original: Reika.RotaryCraft.Base.ModelGearboxBase (hand-ported: it switches texture for the support columns, which in 1.21
 * means drawing them with a second vertex consumer, see {@link #supportConsumer}).
 */
public abstract class ModelGearboxBase extends TechneModel {

	protected final ModelPart Shape1, Shape2, Shape3, Shape4, Shape5, Shape6, Shape7, Shape8, Shape9, Shape10, Shape11;

	private final List<ModelPart> supportColumns = new ArrayList<>();

	/** Set by the renderer before renderAll: the consumer bound to the bearing-tier texture, or null to use the main one. */
	public VertexConsumer supportConsumer;

	protected ModelGearboxBase(ModelPart root) {
		super(root);
		Shape1 = root.getChild("shape1");
		Shape2 = root.getChild("shape2");
		Shape3 = root.getChild("shape3");
		Shape4 = root.getChild("shape4");
		Shape5 = root.getChild("shape5");
		Shape6 = root.getChild("shape6");
		Shape7 = root.getChild("shape7");
		Shape8 = root.getChild("shape8");
		Shape9 = root.getChild("shape9");
		Shape10 = root.getChild("shape10");
		Shape11 = root.getChild("shape11");
	}

	protected static void addParts(PartDefinition root) {
		root.addOrReplaceChild("shape1", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 16F, 1F, 16F), PartPose.offset(-8F, 23F, -8F));
		root.addOrReplaceChild("shape2", CubeListBuilder.create().texOffs(64, 0).addBox(0F, 0F, 0F, 1F, 12F, 16F), PartPose.offset(7F, 11F, -8F));
		root.addOrReplaceChild("shape3", CubeListBuilder.create().texOffs(64, 0).addBox(0F, 0F, 0F, 1F, 12F, 16F), PartPose.offset(-8F, 11F, -8F));
		root.addOrReplaceChild("shape4", CubeListBuilder.create().texOffs(0, 17).addBox(0F, 0F, 0F, 14F, 4F, 1F), PartPose.offset(-7F, 19F, 7F));
		root.addOrReplaceChild("shape5", CubeListBuilder.create().texOffs(0, 17).addBox(0F, 0F, 0F, 14F, 4F, 1F), PartPose.offset(-7F, 19F, -8F));
		root.addOrReplaceChild("shape6", CubeListBuilder.create().texOffs(30, 17).addBox(0F, 0F, 0F, 1F, 3F, 1F), PartPose.offset(6F, 16F, 7F));
		root.addOrReplaceChild("shape7", CubeListBuilder.create().texOffs(30, 17).addBox(0F, 0F, 0F, 1F, 3F, 1F), PartPose.offset(6F, 16F, -8F));
		root.addOrReplaceChild("shape8", CubeListBuilder.create().texOffs(30, 17).addBox(0F, 0F, 0F, 1F, 3F, 1F), PartPose.offset(-7F, 16F, 7F));
		root.addOrReplaceChild("shape9", CubeListBuilder.create().texOffs(30, 17).addBox(0F, 0F, 0F, 1F, 3F, 1F), PartPose.offset(-7F, 16F, -8F));
		root.addOrReplaceChild("shape10", CubeListBuilder.create().texOffs(42, 17).addBox(0F, 0F, 0F, 1F, 3F, 10F), PartPose.offset(7F, 8F, -5F));
		root.addOrReplaceChild("shape11", CubeListBuilder.create().texOffs(42, 17).addBox(0F, 0F, 0F, 1F, 3F, 10F), PartPose.offset(-8F, 8F, -5F));
	}

	protected final void addSupport(ModelPart p) {
		supportColumns.add(p);
	}

	@Override
	public void renderAll(PoseStack pose, VertexConsumer vc, int light, int overlay, BlockEntity te, List<?> li, float phi, float theta) {
		Shape1.render(pose, vc, light, overlay);
		Shape2.render(pose, vc, light, overlay);
		Shape3.render(pose, vc, light, overlay);
		Shape4.render(pose, vc, light, overlay);
		Shape5.render(pose, vc, light, overlay);
		Shape6.render(pose, vc, light, overlay);
		Shape7.render(pose, vc, light, overlay);
		Shape8.render(pose, vc, light, overlay);
		Shape9.render(pose, vc, light, overlay);
		Shape10.render(pose, vc, light, overlay);
		Shape11.render(pose, vc, light, overlay);
	}

	/** The support columns use the bearing tier's gearbox texture (original renderSupports). */
	protected final void renderSupports(PoseStack pose, VertexConsumer vc, int light, int overlay, BlockEntity te, List<?> li) {
		VertexConsumer c = li != null && !li.isEmpty() && supportConsumer != null ? supportConsumer : vc;
		for (ModelPart p : supportColumns)
			p.render(pose, c, light, overlay);
	}
}
