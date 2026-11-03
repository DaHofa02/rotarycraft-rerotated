/*******************************************************************************
 * Original: Reika.RotaryCraft.Base.RotaryModelBase (Techne model base). @author Reika Kalseki.
 * Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.model;

import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Base for the converted RotaryCraft models: one {@link ModelPart} per original box, rendered by the converted renderAll. */
public abstract class TechneModel {

	protected final ModelPart root;

	protected TechneModel(ModelPart root) {
		this.root = root;
	}

	/** Original renderAll(TileEntity, ArrayList conditions, float phi, float theta). */
	public abstract void renderAll(PoseStack pose, VertexConsumer vc, int light, int overlay, BlockEntity te, List<?> li, float phi, float theta);

	/** Original renderList: every part in declaration order. */
	protected void renderAllParts(PoseStack pose, VertexConsumer vc, int light, int overlay) {
		root.render(pose, vc, light, overlay);
	}

	/** GL11.glRotatef(angle, x, y, z) as a quaternion (degrees, axis need not be normalized). */
	public static Quaternionf rotation(double angle, double x, double y, double z) {
		Vector3f axis = new Vector3f((float)x, (float)y, (float)z);
		if (axis.lengthSquared() == 0)
			return new Quaternionf();
		axis.normalize();
		return new Quaternionf().rotateAxis((float)Math.toRadians(angle), axis);
	}
}
