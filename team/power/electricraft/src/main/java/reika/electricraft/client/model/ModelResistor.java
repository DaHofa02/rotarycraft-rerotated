/*******************************************************************************
 * Converted by hand from the original model classes ModelResistorBase, ModelResistor and ModelPreciseResistor (the converter does not
 * handle the band list). Geometry unchanged.
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.client.model;

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

import reika.electricraft.content.blockentity.ResistorBlockEntity.ColorBand;
import reika.rotarycraft.client.model.TechneModel;

/** Body of the resistor and its colour bands (3 for the resistor, 4 for the precision resistor), each band tinted with its dye colour. */
public class ModelResistor extends TechneModel {

	private static final String[] BODY = {"shape1", "shape2a", "shape2", "shape3a", "shape3", "shape3b", "shape3c"};
	/** z of band 1..4 (ModelResistor: 1-3, ModelPreciseResistor: 1-4) */
	private static final float[] BAND_Z = {-6F, -3F, 0F, 3F};

	private final ModelPart[] body = new ModelPart[BODY.length];
	private final ModelPart[] bandA;
	private final ModelPart[] bandB;

	public ModelResistor(ModelPart root, int bands) {
		super(root);
		for (int i = 0; i < BODY.length; i++)
			body[i] = root.getChild(BODY[i]);
		bandA = new ModelPart[bands];
		bandB = new ModelPart[bands];
		for (int i = 0; i < bands; i++) {
			bandA[i] = root.getChild("band"+(i+1)+"a");
			bandB[i] = root.getChild("band"+(i+1)+"b");
		}
	}

	public static LayerDefinition createLayer(int bands) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("shape1", CubeListBuilder.create().texOffs(63, 0).addBox(0F, 0F, 0F, 12F, 5F, 16F), PartPose.offset(-6F, 19F, -8F));
		root.addOrReplaceChild("shape2a", CubeListBuilder.create().texOffs(63, 23).addBox(0F, 0F, 0F, 6F, 2F, 16F), PartPose.offset(-3F, 12F, -8F));
		root.addOrReplaceChild("shape2", CubeListBuilder.create().texOffs(0, 18).addBox(0F, 0F, 0F, 8F, 5F, 16F), PartPose.offset(-4F, 14F, -8F));
		root.addOrReplaceChild("shape3a", CubeListBuilder.create().texOffs(0, 41).addBox(0F, 0F, 0F, 8F, 1F, 15F), PartPose.offset(-4F, 12.5F, -7.5F));
		root.addOrReplaceChild("shape3", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 10F, 1F, 15F), PartPose.offset(-5F, 16F, -7.5F));
		root.addOrReplaceChild("shape3b", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 10F, 1F, 15F), PartPose.offset(-5F, 14.5F, -7.5F));
		root.addOrReplaceChild("shape3c", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 10F, 1F, 15F), PartPose.offset(-5F, 17.5F, -7.5F));
		for (int i = 0; i < bands; i++) {
			root.addOrReplaceChild("band"+(i+1)+"a", CubeListBuilder.create().texOffs(0, 63).addBox(0F, 0F, 0F, 7F, 6F, 2F), PartPose.offset(-3.5F, 11.5F, BAND_Z[i]));
			root.addOrReplaceChild("band"+(i+1)+"b", CubeListBuilder.create().texOffs(22, 63).addBox(0F, 0F, 0F, 9F, 6F, 2F), PartPose.offset(-4.5F, 13.5F, BAND_Z[i]));
		}
		return LayerDefinition.create(mesh, 128, 128);
	}

	/** li: the ColorBand of each band. */
	@Override
	public void renderAll(PoseStack pose, VertexConsumer vc, int light, int overlay, BlockEntity te, List<?> li, float phi, float theta) {
		for (ModelPart p : body)
			p.render(pose, vc, light, overlay);
		for (int i = 0; i < bandA.length; i++) {
			int c = 0xff000000 | ((ColorBand)li.get(i)).color;
			bandA[i].render(pose, vc, light, overlay, c);
			bandB[i].render(pose, vc, light, overlay, c);
		}
	}
}
