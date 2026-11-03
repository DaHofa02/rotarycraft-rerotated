/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.client;

import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.client.model.ModelGasCollector;
import reika.reactorcraft.client.model.ModelMagnet;
import reika.reactorcraft.client.model.ModelSolenoid;
import reika.reactorcraft.fusion.FusionEntity;
import reika.reactorcraft.fusion.FusionMarkerBlockEntity;
import reika.reactorcraft.fusion.GasCollectorBlockEntity;
import reika.reactorcraft.fusion.PlasmaEntity;
import reika.reactorcraft.fusion.SolenoidMagnetBlockEntity;
import reika.reactorcraft.fusion.ToroidMagnetBlockEntity;
import reika.reactorcraft.fusion.ToroidMagnetBlockEntity.Aim;
import reika.rotarycraft.client.model.TechneModel;

/** The fusion reactor's renderers (original RenderMagnet, RenderSolenoid, RenderGasCollector, RenderFusionMarker, RenderPlasma, RenderFusion). */
public final class FusionRenderers {

	public static final ModelLayerLocation MAGNET = layer("toroid_magnet");
	public static final ModelLayerLocation SOLENOID = layer("solenoid");
	public static final ModelLayerLocation GAS_COLLECTOR = layer("gas_collector");

	private FusionRenderers() {}

	private static ModelLayerLocation layer(String id) {
		return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, id), "main");
	}

	private static ResourceLocation tex(String name) {
		return ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "textures/machine/"+name);
	}

	private static void origin(PoseStack pose) {
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
	}

	private static void line(VertexConsumer vc, PoseStack.Pose p, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
		float dx = x2-x1, dy = y2-y1, dz = z2-z1;
		float l = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
		if (l == 0)
			l = 1;
		vc.addVertex(p, x1, y1, z1).setColor(r, g, b, a).setNormal(p, dx/l, dy/l, dz/l);
		vc.addVertex(p, x2, y2, z2).setColor(r, g, b, a).setNormal(p, dx/l, dy/l, dz/l);
	}

	/** Original RenderMagnet: the magnet turned to its aim (90 - angle); the aim arrow and three circles (fading after a screwdriver click). */
	public static final class Magnet implements BlockEntityRenderer<ToroidMagnetBlockEntity> {
		private final ModelMagnet model;

		public Magnet(BlockEntityRendererProvider.Context ctx) {
			model = new ModelMagnet(ctx.bakeLayer(MAGNET));
		}

		@Override
		public void render(ToroidMagnetBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
			boolean world = tile.getLevel() != null;
			pose.pushPose();
			origin(pose);
			if (!world) {
				pose.translate(0, 0.6, 0);
				pose.scale(0.4F, 0.4F, 0.4F);
			}
			pose.mulPose(TechneModel.rotation(90-tile.getAngle(), 0, 1, 0));
			model.renderAll(pose, buf.getBuffer(RenderType.entityCutout(tex("magnet.png"))), light, overlay, tile, null, 0, 0);
			pose.popPose();
			if (world && tile.getAlpha() > 0)
				this.renderAngleLine(tile, pose, buf);
		}

		private void renderAngleLine(ToroidMagnetBlockEntity tile, PoseStack pose, MultiBufferSource buf) {
			int a = Math.min(255, tile.getAlpha());
			pose.pushPose();
			pose.translate(0.5, 0.5, 0.5);
			pose.mulPose(TechneModel.rotation(tile.getAngle()+90, 0, 1, 0));
			VertexConsumer vc = buf.getBuffer(RenderType.lines());
			PoseStack.Pose p = pose.last();
			line(vc, p, 0, 0.1F, 0, 4, 0.1F, 0, 100, 192, 255, a);
			line(vc, p, 3.5F, 0.1F, 0.5F, 4, 0.1F, 0, 100, 192, 255, a);
			line(vc, p, 3.5F, 0.1F, -0.5F, 4, 0.1F, 0, 100, 192, 255, a);
			for (int r = 1; r < 4; r++) { //original renderVCircle(r, ..., 90, 10): a vertical circle across the arrow
				for (int d = 0; d < 360; d += 10) {
					double t1 = Math.toRadians(d), t2 = Math.toRadians(d+10);
					line(vc, p, 0, (float)(r*Math.sin(t1)), (float)(r*Math.cos(t1)), 0, (float)(r*Math.sin(t2)), (float)(r*Math.cos(t2)), 255, 255, 255, a);
				}
			}
			pose.popPose();
		}

		@Override
		public AABB getRenderBoundingBox(ToroidMagnetBlockEntity te) {
			return new AABB(te.getBlockPos()).inflate(3);
		}
	}

	/** Original RenderSolenoid: the spinning coil while its multiblock is complete, otherwise a plain block with the solenoid part textures. */
	public static final class Solenoid implements BlockEntityRenderer<SolenoidMagnetBlockEntity> {
		private final ModelSolenoid model;

		public Solenoid(BlockEntityRendererProvider.Context ctx) {
			model = new ModelSolenoid(ctx.bakeLayer(SOLENOID));
		}

		@Override
		public void render(SolenoidMagnetBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
			boolean world = tile.getLevel() != null;
			if (world && !tile.canRenderCoil()) {
				this.renderPlain(pose, buf, light, overlay);
				return;
			}
			pose.pushPose();
			origin(pose);
			if (!world) {
				pose.translate(0, 0.8, 0);
				pose.scale(0.4F, 0.4F, 0.4F);
			}
			else {
				//the solenoid sits inside its structure (opaque parts all round), where the light is 0: the whole ring drawn with it came out
				//black. Light it as the open space above the core (the original took the world brightness at the coil).
				BlockPos p = tile.getBlockPos();
				int above = net.minecraft.client.renderer.LevelRenderer.getLightColor(tile.getLevel(), p.above(2));
				light = net.minecraft.client.renderer.LightTexture.pack(Math.max(net.minecraft.client.renderer.LightTexture.block(light), net.minecraft.client.renderer.LightTexture.block(above)),
						Math.max(net.minecraft.client.renderer.LightTexture.sky(light), net.minecraft.client.renderer.LightTexture.sky(above)));
			}
			model.renderAll(pose, buf.getBuffer(RenderType.entityCutout(tex("solenoid.png"))), light, overlay, tile, List.of(world && tile.canRenderCoil()), -tile.phi, 0);
			pose.popPose();
		}

		private void renderPlain(PoseStack pose, MultiBufferSource buf, int light, int overlay) {
			var atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
			TextureAtlasSprite top = atlas.apply(ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "block/multi/solenoid_3"));
			TextureAtlasSprite side = atlas.apply(ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "block/multi/solenoid_6"));
			VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
			Matrix4f m = pose.last().pose();
			PoseStack.Pose p = pose.last();
			quad(vc, m, p, top, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 0, light, overlay);
			quad(vc, m, p, top, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, -1, 0, light, overlay);
			quad(vc, m, p, side, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0, -1, light, overlay);
			quad(vc, m, p, side, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 0, 0, 1, light, overlay);
			quad(vc, m, p, side, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0, -1, 0, 0, light, overlay);
			quad(vc, m, p, side, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 0, light, overlay);
		}

		private static void quad(VertexConsumer vc, Matrix4f m, PoseStack.Pose p, TextureAtlasSprite s, float x1, float y1, float z1, float x2, float y2, float z2,
				float x3, float y3, float z3, float x4, float y4, float z4, float nx, float ny, float nz, int light, int overlay) {
			vc.addVertex(m, x1, y1, z1).setColor(-1).setUv(s.getU0(), s.getV1()).setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
			vc.addVertex(m, x2, y2, z2).setColor(-1).setUv(s.getU0(), s.getV0()).setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
			vc.addVertex(m, x3, y3, z3).setColor(-1).setUv(s.getU1(), s.getV0()).setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
			vc.addVertex(m, x4, y4, z4).setColor(-1).setUv(s.getU1(), s.getV1()).setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
		}

		@Override
		public AABB getRenderBoundingBox(SolenoidMagnetBlockEntity te) {
			return new AABB(te.getBlockPos()).inflate(9, 2, 9);
		}
	}

	/** Original RenderGasCollector: turned to the face it sits on; the intake shown when a furnace or refrigerator is behind it. */
	public static final class GasCollector implements BlockEntityRenderer<GasCollectorBlockEntity> {
		private final ModelGasCollector model;

		public GasCollector(BlockEntityRendererProvider.Context ctx) {
			model = new ModelGasCollector(ctx.bakeLayer(GAS_COLLECTOR));
		}

		@Override
		public void render(GasCollectorBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
			boolean world = tile.getLevel() != null;
			pose.pushPose();
			origin(pose);
			if (world) {
				int meta = tile.getBlockMetadata();
				int var11 = switch (meta) {
					case 0, 2 -> 180;
					case 5 -> 90;
					case 4 -> 270;
					default -> 0;
				};
				if (meta < 2) {
					pose.mulPose(TechneModel.rotation(var11, 0, 0, 1));
					if (meta == 0)
						pose.translate(0, -2, 0);
				}
				else {
					pose.mulPose(TechneModel.rotation(90, 1, 0, 0));
					pose.mulPose(TechneModel.rotation(var11, 0, 0, 1));
					pose.translate(0, -1, -1);
				}
			}
			boolean flag = world && tile.hasFurnace();
			model.renderAll(pose, buf.getBuffer(RenderType.entityCutout(tex("co2collector.png"))), light, overlay, tile, List.of(flag), 0, 0);
			pose.popPose();
		}
	}

	/**
	 * Original RenderFusionMarker: two crossed blue redstone-torch sprites; with a redstone signal, the outlines of the 36 magnet places
	 * (yellow), the 4 injector frames (blue) and the solenoid ring (red) of the tokamak around it (in the original's flipped frame).
	 */
	public static final class Marker implements BlockEntityRenderer<FusionMarkerBlockEntity> {

		public Marker(BlockEntityRendererProvider.Context ctx) {}

		@Override
		public void render(FusionMarkerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			origin(pose);
			this.renderBody(pose, buf, light, overlay);
			pose.mulPose(TechneModel.rotation(90, 0, 1, 0));
			this.renderBody(pose, buf, light, overlay);
			pose.popPose();
			if (tile.getLevel() != null && tile.renderLines()) {
				pose.pushPose();
				origin(pose);
				this.renderPositions(tile, pose, buf.getBuffer(RenderType.lines()));
				pose.popPose();
			}
		}

		private void renderBody(PoseStack pose, MultiBufferSource buf, int light, int overlay) {
			TextureAtlasSprite s = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.withDefaultNamespace("block/redstone_torch"));
			VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
			pose.translate(0, 0.5, 0);
			Matrix4f m = pose.last().pose();
			PoseStack.Pose p = pose.last();
			int r = 0, g = 51, b = 255;
			float[][] q = {{-0.5F, 0, 0}, {0.5F, 0, 0}, {0.5F, 1, 0}, {-0.5F, 1, 0}};
			float[][] uv = {{s.getU0(), s.getV0()}, {s.getU1(), s.getV0()}, {s.getU1(), s.getV1()}, {s.getU0(), s.getV1()}};
			for (int i = 0; i < 4; i++)
				vc.addVertex(m, q[i][0], q[i][1], q[i][2]).setColor(r, g, b, 255).setUv(uv[i][0], uv[i][1]).setOverlay(overlay).setLight(light).setNormal(p, 0, 0, 1);
			for (int i = 0; i < 4; i++)
				vc.addVertex(m, q[i][2], q[i][1], q[i][0]).setColor(r, g, b, 255).setUv(uv[i][0], uv[i][1]).setOverlay(overlay).setLight(light).setNormal(p, 1, 0, 0);
			pose.translate(0, -0.5, 0);
		}

		private void renderPositions(FusionMarkerBlockEntity tile, PoseStack pose, VertexConsumer vc) {
			int x = 0, z = 14;
			List<Aim> li = FusionMarkerBlockEntity.getAimPoints();
			for (int i = 0; i < li.size(); i++) {
				Aim a = li.get(i);
				pose.pushPose();
				pose.translate(x, 1.499, z);
				pose.mulPose(TechneModel.rotation(a.angle, 0, 1, 0));
				PoseStack.Pose p = pose.last();
				if (i%10 != 0) {
					float[][] loop = {{0, -0.5F}, {0, 0.5F}, {-1, 1.5F}, {-3, 0.5F}, {-3, -0.5F}, {-1, -1.5F}};
					for (float w : new float[]{0.25F, -0.25F})
						for (int k = 0; k < loop.length; k++) {
							float[] c1 = loop[k], c2 = loop[(k+1)%loop.length];
							line(vc, p, w, c1[0], c1[1], w, c2[0], c2[1], 255, 255, 0, 255);
						}
					for (float[] c : loop)
						line(vc, p, 0.25F, c[0], c[1], -0.25F, c[0], c[1], 255, 255, 0, 255);
				}
				else {
					float[][] top = {{-1.5F, 0, -2.5F}, {1.5F, 0, -2.5F}, {1.5F, 0, 6.5F}, {-1.5F, 0, 6.5F}};
					float[][] bot = {{-1.5F, -5, -2.5F}, {1.5F, -5, -2.5F}, {1.5F, -3, 6.5F}, {-1.5F, -3, 6.5F}};
					for (int k = 0; k < 4; k++) {
						line(vc, p, top[k][0], top[k][1], top[k][2], top[(k+1)%4][0], top[(k+1)%4][1], top[(k+1)%4][2], 0, 200, 255, 255);
						line(vc, p, bot[k][0], bot[k][1], bot[k][2], bot[(k+1)%4][0], bot[(k+1)%4][1], bot[(k+1)%4][2], 0, 200, 255, 255);
						line(vc, p, top[k][0], top[k][1], top[k][2], bot[k][0], bot[k][1], bot[k][2], 0, 200, 255, 255);
					}
				}
				pose.popPose();
				x += a.xOffset;
				z += a.zOffset;
			}
			this.renderSolenoid(pose, vc);
		}

		private static final int[][] RING = {{8, 3}, {8, -3}, {7, -5}, {6, -6}, {5, -7}, {3, -8}, {-3, -8}, {-5, -7}, {-6, -6}, {-7, -5}, {-8, -3}, {-8, 3},
				{-7, 5}, {-6, 6}, {-5, 7}, {-3, 8}, {3, 8}, {5, 7}, {6, 6}, {7, 5}};

		private void renderSolenoid(PoseStack pose, VertexConsumer vc) {
			pose.pushPose();
			pose.translate(0, 0.499, 0);
			PoseStack.Pose p = pose.last();
			for (int k = 0; k < RING.length; k++) {
				int[] c1 = RING[k], c2 = RING[(k+1)%RING.length];
				line(vc, p, c1[0], 1, c1[1], c2[0], 1, c2[1], 255, 0, 0, 255);
				line(vc, p, c1[0], -2, c1[1], c2[0], -2, c2[1], 255, 0, 0, 255);
				line(vc, p, c1[0], -2, c1[1], c1[0], 1, c1[1], 255, 0, 0, 255);
			}
			for (int y : new int[]{1, -2}) {
				line(vc, p, 8, y, 0, -8, y, 0, 255, 0, 0, 255);
				line(vc, p, 0, y, 8, 0, y, -8, 255, 0, 0, 255);
				line(vc, p, 6, y, 6, -6, y, -6, 255, 0, 0, 255);
				line(vc, p, -6, y, 6, 6, y, -6, 255, 0, 0, 255);
			}
			pose.popPose();
		}

		@Override
		public boolean shouldRenderOffScreen(FusionMarkerBlockEntity te) {
			return true;
		}

		@Override
		public AABB getRenderBoundingBox(FusionMarkerBlockEntity te) {
			return AABB.INFINITE;
		}
	}

	/** Original RenderPlasma: a camera-facing 1.5 block plasma7.png sprite, additive, full bright. */
	public static final class Plasma extends EntityRenderer<PlasmaEntity> {
		private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "textures/entity/plasma7.png");

		public Plasma(EntityRendererProvider.Context ctx) {
			super(ctx);
		}

		@Override
		public void render(PlasmaEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
			sprite(pose, buf, this.entityRenderDispatcher, TEX, 1.5F, 127);
			super.render(e, yaw, partial, pose, buf, light);
		}

		@Override
		public ResourceLocation getTextureLocation(PlasmaEntity e) {
			return TEX;
		}
	}

	/** Original RenderFusion: a camera-facing fusion2.png flash shrinking from 2 blocks, additive. */
	public static final class Fusion extends EntityRenderer<FusionEntity> {
		private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "textures/entity/fusion2.png");

		public Fusion(EntityRendererProvider.Context ctx) {
			super(ctx);
		}

		@Override
		public void render(FusionEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
			float size = 2-(e.tickCount+partial)/10F;
			if (size > 0)
				sprite(pose, buf, this.entityRenderDispatcher, TEX, size, 127);
			super.render(e, yaw, partial, pose, buf, light);
		}

		@Override
		public ResourceLocation getTextureLocation(FusionEntity e) {
			return TEX;
		}
	}

	private static void sprite(PoseStack pose, MultiBufferSource buf, net.minecraft.client.renderer.entity.EntityRenderDispatcher d, ResourceLocation tex, float size, int c) {
		pose.pushPose();
		pose.mulPose(d.cameraOrientation());
		pose.scale(size, size, 1);
		VertexConsumer vc = buf.getBuffer(RenderType.eyes(tex));
		Matrix4f m = pose.last().pose();
		PoseStack.Pose p = pose.last();
		int full = 0xF000F0;
		vc.addVertex(m, -0.5F, -0.5F, 0).setColor(c, c, c, 255).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full).setNormal(p, 0, 1, 0);
		vc.addVertex(m, 0.5F, -0.5F, 0).setColor(c, c, c, 255).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full).setNormal(p, 0, 1, 0);
		vc.addVertex(m, 0.5F, 0.5F, 0).setColor(c, c, c, 255).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full).setNormal(p, 0, 1, 0);
		vc.addVertex(m, -0.5F, 0.5F, 0).setColor(c, c, c, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full).setNormal(p, 0, 1, 0);
		pose.popPose();
	}

	/** Called from ReactorClient's renderer and layer events. */
	public static void registerLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions e) {
		e.registerLayerDefinition(MAGNET, ModelMagnet::createLayer);
		e.registerLayerDefinition(SOLENOID, ModelSolenoid::createLayer);
		e.registerLayerDefinition(GAS_COLLECTOR, ModelGasCollector::createLayer);
	}

	public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e) {
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.TOROID_MAGNET.get(), Magnet::new);
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.SOLENOID.get(), ctx -> new reika.rotarycraft.client.render.IORenderer<>(new Solenoid(ctx)));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.GAS_COLLECTOR.get(), GasCollector::new);
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.FUSION_MARKER.get(), Marker::new);
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.MAGNETIC_PIPE.get(), reika.rotarycraft.client.render.PipeFluidRenderer::new);
		e.registerEntityRenderer(reika.reactorcraft.registry.ReactorEntities.PLASMA.get(), Plasma::new);
		e.registerEntityRenderer(reika.reactorcraft.registry.ReactorEntities.FUSION.get(), Fusion::new);
	}
}
