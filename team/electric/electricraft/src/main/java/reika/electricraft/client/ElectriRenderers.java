/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import reika.electricraft.ElectriCraft;
import reika.electricraft.client.model.ModelElecMotor;
import reika.electricraft.client.model.ModelGenerator;
import reika.electricraft.content.blockentity.GeneratorBlockEntity;
import reika.electricraft.content.blockentity.MotorBlockEntity;
import reika.electricraft.registry.ElectriBlockEntities;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.client.render.IORenderer;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: ElectriTERenderer, RenderGenerator and RenderMotor. Both machines turn with their facing (east 270, west 90, north 180,
 * south 0; the generator another 180); flipped ones on the z axis turn round once more. The motor's fins take the power colour.
 */
@EventBusSubscriber(modid = ElectriCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ElectriRenderers {

	public static final ModelLayerLocation GENERATOR = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "generator"), "main");
	public static final ModelLayerLocation MOTOR = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "motor"), "main");
	public static final ModelLayerLocation TRANSFORMER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "transformer"), "main");
	public static final ModelLayerLocation RELAY = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "relay"), "main");
	public static final ModelLayerLocation FUSE = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "fuse"), "main");
	public static final ModelLayerLocation METER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "meter"), "main");
	public static final ModelLayerLocation RESISTOR = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "resistor"), "main");
	public static final ModelLayerLocation PRECISE_RESISTOR = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "precise_resistor"), "main");

	private ElectriRenderers() {}

	/** The machine items are drawn by their block entity renderer (RotaryCraft MachineItemRenderer, original ElectriItemRenderer). */
	@SubscribeEvent
	public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e) {
		reika.rotarycraft.client.MachineItemRenderer[] r = new reika.rotarycraft.client.MachineItemRenderer[1];
		e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
			@Override
			public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (r[0] == null)
					r[0] = new reika.rotarycraft.client.MachineItemRenderer();
				return r[0];
			}
		}, reika.electricraft.registry.ElectriBlocks.GENERATOR.get().asItem(), reika.electricraft.registry.ElectriBlocks.MOTOR.get().asItem(),
				reika.electricraft.registry.ElectriBlocks.RELAY.get().asItem(), reika.electricraft.registry.ElectriBlocks.RESISTOR.get().asItem(),
				reika.electricraft.registry.ElectriBlocks.PRECISE_RESISTOR.get().asItem(), reika.electricraft.registry.ElectriBlocks.FUSE.get().asItem(),
				reika.electricraft.registry.ElectriBlocks.METER.get().asItem(), reika.electricraft.registry.ElectriBlocks.TRANSFORMER.get().asItem());
	}

	@SubscribeEvent
	public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e) {
		e.register(reika.electricraft.registry.ElectriMenus.TRANSFORMER.get(), reika.electricraft.client.screen.TransformerScreen::new);
		e.register(reika.electricraft.registry.ElectriMenus.RF_CABLE.get(), reika.electricraft.client.screen.RFCableScreen::new);
	}

	@SubscribeEvent
	public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
		e.registerLayerDefinition(GENERATOR, ModelGenerator::createLayer);
		e.registerLayerDefinition(MOTOR, ModelElecMotor::createLayer);
		e.registerLayerDefinition(TRANSFORMER, reika.electricraft.client.model.ModelTransformer::createLayer);
		e.registerLayerDefinition(RELAY, reika.electricraft.client.model.ModelRelay::createLayer);
		e.registerLayerDefinition(FUSE, reika.electricraft.client.model.ModelFuse::createLayer);
		e.registerLayerDefinition(METER, reika.electricraft.client.model.ModelMeter::createLayer);
		e.registerLayerDefinition(RESISTOR, () -> reika.electricraft.client.model.ModelResistor.createLayer(3));
		e.registerLayerDefinition(PRECISE_RESISTOR, () -> reika.electricraft.client.model.ModelResistor.createLayer(4));
	}

	@SubscribeEvent
	public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
		e.registerBlockEntityRenderer(ElectriBlockEntities.GENERATOR.get(), ctx -> new IORenderer<>(new Generator(ctx)));
		e.registerBlockEntityRenderer(ElectriBlockEntities.MOTOR.get(), ctx -> new IORenderer<>(new Motor(ctx)));
		e.registerBlockEntityRenderer(ElectriBlockEntities.RELAY.get(), ctx -> new IORenderer<>(new Component<>(new reika.electricraft.client.model.ModelRelay(ctx.bakeLayer(RELAY)), t -> tex("relay.png"), false)));
		e.registerBlockEntityRenderer(ElectriBlockEntities.RESISTOR.get(), ctx -> new IORenderer<>(new Component<>(new reika.electricraft.client.model.ModelResistor(ctx.bakeLayer(RESISTOR), 3), t -> tex("resistor.png"), false)));
		e.registerBlockEntityRenderer(ElectriBlockEntities.PRECISE_RESISTOR.get(), ctx -> new IORenderer<>(new Component<>(new reika.electricraft.client.model.ModelResistor(ctx.bakeLayer(PRECISE_RESISTOR), 4), t -> tex("resistor.png"), false)));
		e.registerBlockEntityRenderer(ElectriBlockEntities.FUSE.get(), ctx -> new IORenderer<>(new Component<>(new reika.electricraft.client.model.ModelFuse(ctx.bakeLayer(FUSE)), ElectriRenderers::fuseTexture, true)));
		e.registerBlockEntityRenderer(ElectriBlockEntities.METER.get(), ctx -> new IORenderer<>(new Meter(ctx)));
		e.registerBlockEntityRenderer(ElectriBlockEntities.RF_BATTERY.get(), ctx -> new RFBattery());
		e.registerBlockEntityRenderer(ElectriBlockEntities.TRANSFORMER.get(), ctx -> new IORenderer<>(new Transformer(ctx)));
	}

	static ResourceLocation tex(String name) {
		return ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "textures/machine/"+name);
	}

	static boolean inWorld(RotaryCraftBlockEntity te) {
		return te.getLevel() != null;
	}

	/** Original ElectriTERenderer.setupGL. */
	static void setup(RotaryCraftBlockEntity tile, PoseStack pose) {
		pose.pushPose();
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		pose.translate(0, -2, -1);
		if (inWorld(tile) && tile.isFlipped) {
			pose.mulPose(TechneModel.rotation(180, 1, 0, 0));
			pose.translate(0, -2, 0);
		}
	}

	static int facingAngle(Direction d) {
		return switch (d) {
			case EAST -> 270;
			case WEST -> 90;
			case NORTH -> 180;
			default -> 0;
		};
	}

	static final class Generator implements BlockEntityRenderer<GeneratorBlockEntity> {
		private final ModelGenerator model;

		Generator(BlockEntityRendererProvider.Context ctx) {
			model = new ModelGenerator(ctx.bakeLayer(GENERATOR));
		}

		@Override
		public void render(GeneratorBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
			setup(tile, pose);
			pose.mulPose(TechneModel.rotation(facingAngle(tile.getFacing())+180, 0.0F, 1.0F, 0.0F));
			if (tile.isFlipped && tile.getFacing().getAxis() == Direction.Axis.Z)
				pose.mulPose(TechneModel.rotation(180, 0, 1, 0));
			model.renderAll(pose, buf.getBuffer(RenderType.entityCutout(tex("generatortex.png"))), light, overlay, tile, null, 0, 0);
			pose.popPose();
		}
	}

	static final class Motor implements BlockEntityRenderer<MotorBlockEntity> {
		private final ModelElecMotor model;

		Motor(BlockEntityRendererProvider.Context ctx) {
			model = new ModelElecMotor(ctx.bakeLayer(MOTOR));
		}

		@Override
		public void render(MotorBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
			setup(tile, pose);
			pose.mulPose(TechneModel.rotation(facingAngle(tile.getFacing()), 0.0F, 1.0F, 0.0F));
			if (tile.isFlipped && tile.getFacing().getAxis() == Direction.Axis.Z)
				pose.mulPose(TechneModel.rotation(180, 0, 1, 0));
			model.renderAll(pose, buf.getBuffer(RenderType.entityCutout(tex("elecmotortex.png"))), light, overlay, tile,
					List.of(5, tile.getFinColor(), tile.getPower() > 0), tile.phi, 0);
			pose.popPose();
		}
	}

	/** Original RenderFuse: burnt, or hotter textures at 25/50/75 % of the limit. */
	static ResourceLocation fuseTexture(reika.electricraft.content.blockentity.WireComponentBlockEntity te) {
		reika.electricraft.content.blockentity.FuseBlockEntity f = (reika.electricraft.content.blockentity.FuseBlockEntity)te;
		if (f.isOverloaded())
			return tex("fusetex-burn.png");
		float frac = f.getMaxCurrent() > 0 ? f.getWireCurrent()/(float)f.getMaxCurrent() : 0;
		if (frac >= 0.75)
			return tex("fusetex-hot3.png");
		if (frac >= 0.5)
			return tex("fusetex-hot2.png");
		if (frac >= 0.25)
			return tex("fusetex-hot.png");
		return tex("fusetex.png");
	}

	/** Original RenderRelay / RenderResistor / RenderPreciseResistor / RenderFuse: the model turned to the facing (the fuse blended). */
	static final class Component<T extends reika.electricraft.content.blockentity.WireComponentBlockEntity> implements BlockEntityRenderer<T> {
		private final TechneModel model;
		private final java.util.function.Function<T, ResourceLocation> texture;
		private final boolean translucent;

		Component(TechneModel model, java.util.function.Function<T, ResourceLocation> texture, boolean translucent) {
			this.model = model;
			this.texture = texture;
			this.translucent = translucent;
		}

		@Override
		public void render(T tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
			setup(tile, pose);
			pose.mulPose(TechneModel.rotation(facingAngle(tile.getFacing()), 0.0F, 1.0F, 0.0F));
			ResourceLocation tex = texture.apply(tile);
			List<?> li = tile instanceof reika.electricraft.content.blockentity.ResistorBlockEntity r ? List.of((Object[])r.getColorBands()) : null;
			model.renderAll(pose, buf.getBuffer(translucent ? RenderType.entityTranslucent(tex) : RenderType.entityCutout(tex)), light, overlay, tile, li, tile.phi, 0);
			pose.popPose();
		}
	}

	/** Original RenderElectricMeter: the model, and on its top the voltage and current, turned in 90 degree steps towards the viewer. */
	static final class Meter implements BlockEntityRenderer<reika.electricraft.content.blockentity.MeterBlockEntity> {
		private final reika.electricraft.client.model.ModelMeter model;
		private final net.minecraft.client.gui.Font font;

		Meter(BlockEntityRendererProvider.Context ctx) {
			model = new reika.electricraft.client.model.ModelMeter(ctx.bakeLayer(METER));
			font = ctx.getFont();
		}

		@Override
		public void render(reika.electricraft.content.blockentity.MeterBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
			setup(tile, pose);
			model.renderAll(pose, buf.getBuffer(RenderType.entityCutout(tex("metertex.png"))), light, overlay, tile, null, tile.phi, 0);
			if (inWorld(tile)) {
				pose.pushPose();
				float ang = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getYRot()%360;
				if (ang < 0)
					ang += 360;
				int ra = 90*(int)((ang+180+45)/90F);
				pose.mulPose(TechneModel.rotation(ra, 0, 1, 0));
				pose.translate(0, 0.515, 0);
				float s = 0.01F;
				pose.scale(s, s, s);
				pose.mulPose(TechneModel.rotation(-90, 1, 0, 0));
				int dx = -30, dy = -30;
				int full = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
				org.joml.Matrix4f m = pose.last().pose();
				net.minecraft.client.gui.Font.DisplayMode mode = net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET;
				font.drawInBatch("Voltage:", dx, dy, 0xffffff, false, m, buf, mode, 0, full);
				font.drawInBatch(tile.getWireVoltage()+"V", dx, dy+10, 0xffffff, false, m, buf, mode, 0, full);
				font.drawInBatch("Current:", dx, dy+30, 0xffffff, false, m, buf, mode, 0, full);
				font.drawInBatch(tile.getWireCurrent()+"A", dx, dy+40, 0xffffff, false, m, buf, mode, 0, full);
				pose.popPose();
			}
			pose.popPose();
		}
	}

	/** Original RenderTransformer: the model turned to the facing with coils by turn count, and for 64 ticks after placing an arrow to the output. */
	static final class Transformer implements BlockEntityRenderer<reika.electricraft.content.blockentity.TransformerBlockEntity> {
		private final reika.electricraft.client.model.ModelTransformer model;

		Transformer(BlockEntityRendererProvider.Context ctx) {
			model = new reika.electricraft.client.model.ModelTransformer(ctx.bakeLayer(TRANSFORMER));
		}

		@Override
		public void render(reika.electricraft.content.blockentity.TransformerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
			setup(tile, pose);
			int ang = switch (tile.getFacing()) {
				case EAST -> 180;
				case NORTH -> 90;
				case SOUTH -> 270;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(ang, 0.0F, 1.0F, 0.0F));
			model.renderAll(pose, buf.getBuffer(RenderType.entityCutout(tex("transformertex.png"))), light, overlay, tile, List.of(tile.getN1(), tile.getN2()), tile.phi, 0);
			pose.popPose();
			if (inWorld(tile))
				this.renderArrow(tile, pose, buf);
		}

		private void renderArrow(reika.electricraft.content.blockentity.TransformerBlockEntity te, PoseStack pose, MultiBufferSource buf) {
			int a = Math.max(0, 512-te.getTicksExisted()*8);
			if (a <= 0)
				return;
			a = Math.min(255, a);
			com.mojang.blaze3d.vertex.VertexConsumer vc = buf.getBuffer(RenderType.lines());
			PoseStack.Pose p = pose.last();
			Direction f = te.getFacing();
			double h = 1.1, dr = 0.125, r = 0.375, w = 0.08;
			float dx = (float)(0.5+f.getStepX()*r);
			float dz = (float)(0.5+f.getStepZ()*r);
			line(vc, p, 0.5F, (float)h, 0.5F, dx, (float)h, dz, a);
			line(vc, p, dx, (float)h, dz, (float)(dx-f.getStepX()*dr+f.getStepZ()*w), (float)h, (float)(dz-f.getStepZ()*dr+f.getStepX()*w), a);
			line(vc, p, dx, (float)h, dz, (float)(dx-f.getStepX()*dr-f.getStepZ()*w), (float)h, (float)(dz-f.getStepZ()*dr-f.getStepX()*w), a);
		}

		private static void line(com.mojang.blaze3d.vertex.VertexConsumer vc, PoseStack.Pose p, float x1, float y1, float z1, float x2, float y2, float z2, int a) {
			float nx = x2-x1, ny = y2-y1, nz = z2-z1;
			float l = (float)Math.sqrt(nx*nx+ny*ny+nz*nz);
			if (l > 0) {
				nx /= l;
				ny /= l;
				nz /= l;
			}
			vc.addVertex(p, x1, y1, z1).setColor(255, 255, 255, a).setNormal(p, nx, ny, nz);
			vc.addVertex(p, x2, y2, z2).setColor(255, 255, 255, a).setNormal(p, nx, ny, nz);
		}
	}

	/**
	 * Original RenderModBattery: on each side a bar of up to 64 stripes, 64*log10(stored)/log10(capacity) of them lit, rising from almost black
	 * at the bottom to red (0xff1111) at the top, untextured and full-bright.
	 */
	static final class RFBattery implements BlockEntityRenderer<reika.electricraft.content.blockentity.RFBatteryBlockEntity> {
		@Override
		public void render(reika.electricraft.content.blockentity.RFBatteryBlockEntity te, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
			long e = te.getStoredEnergy();
			if (e <= 0)
				return;
			int n = 64;
			int c = (int)(64*Math.log10(e)/Math.log10(reika.electricraft.content.blockentity.RFBatteryBlockEntity.CAPACITY));
			double o = 0.0025, w = 0.1875, h = 0.0625;
			double d = (1-h*2)/n;
			int c1 = 0xff1111;
			int r1 = (c1 >> 16) & 255, g1 = (c1 >> 8) & 255, b1 = c1 & 255;
			com.mojang.blaze3d.vertex.VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
			org.joml.Matrix4f m = pose.last().pose();
			for (int i = 0; i < c; i++) {
				float f = (float)i/n; //original mixColors(c1, c1*0.05, i/n)
				int r = (int)(r1*f+r1*0.05F*(1-f)), g = (int)(g1*f+g1*0.05F*(1-f)), b = (int)(b1*f+b1*0.05F*(1-f));
				float y0 = (float)(h+d*i), y1 = (float)(h+d*(i+1));
				float a0 = (float)(0.5-w), a1 = (float)(0.5+w);
				quad(vc, m, a0, y0, (float)-o, a1, y1, (float)-o, r, g, b, true);
				quad(vc, m, a0, y0, (float)(1+o), a1, y1, (float)(1+o), r, g, b, true);
				quad(vc, m, (float)(1+o), y0, a0, (float)(1+o), y1, a1, r, g, b, false);
				quad(vc, m, (float)-o, y0, a0, (float)-o, y1, a1, r, g, b, false);
			}
		}

		private static void quad(com.mojang.blaze3d.vertex.VertexConsumer vc, org.joml.Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, int r, int g, int b, boolean alongX) {
			if (alongX) {
				vc.addVertex(m, x0, y0, z0).setColor(r, g, b, 255);
				vc.addVertex(m, x1, y0, z0).setColor(r, g, b, 255);
				vc.addVertex(m, x1, y1, z0).setColor(r, g, b, 255);
				vc.addVertex(m, x0, y1, z0).setColor(r, g, b, 255);
			}
			else {
				vc.addVertex(m, x0, y0, z0).setColor(r, g, b, 255);
				vc.addVertex(m, x0, y0, z1).setColor(r, g, b, 255);
				vc.addVertex(m, x0, y1, z1).setColor(r, g, b, 255);
				vc.addVertex(m, x0, y1, z0).setColor(r, g, b, 255);
			}
		}
	}
}
