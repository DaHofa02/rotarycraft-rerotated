/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.client;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.registry.ReactorFluids;

/** Original: ReactorCraft.textureHook - the fluid icons (fluid/*); low pressure water uses the water texture. */
@EventBusSubscriber(modid = ReactorCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ReactorClient {

	private ReactorClient() {}

	public static final net.minecraft.client.model.geom.ModelLayerLocation HEAVY_PUMP = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "fluid_extractor"), "main");

	public static final net.minecraft.client.model.geom.ModelLayerLocation ISO_CENTRIFUGE = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "isocentrifuge"), "main");

	/** Original RenderCentrifuge: the model with the rotor turning by -phi. */
	static final class IsoCentrifuge implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.content.blockentity.IsoCentrifugeBlockEntity> {
		private final reika.reactorcraft.client.model.ModelCentrifuge model;

		IsoCentrifuge(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelCentrifuge(ctx.bakeLayer(ISO_CENTRIFUGE));
		}

		@Override
		public void render(reika.reactorcraft.content.blockentity.IsoCentrifugeBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			model.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("centrifuge.png"))), light, overlay, tile, null, -tile.phi, 0);
			pose.popPose();
		}
	}

	public static final net.minecraft.client.model.geom.ModelLayerLocation CONTROL_ROD = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "control_rod"), "main");

	/** Original RenderControl: the model with the rods moved by the rod position (one step = 1/28 block); the render box reaches 2 blocks up. */
	static final class ControlRod implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.reactor.ControlRodBlockEntity> {
		private final reika.reactorcraft.client.model.ModelControl model;

		ControlRod(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelControl(ctx.bakeLayer(CONTROL_ROD));
		}

		@Override
		public void render(reika.reactorcraft.reactor.ControlRodBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			model.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("control.png"))), light, overlay, tile, null, tile.getRodPosition(), 0);
			pose.popPose();
		}

		@Override
		public net.minecraft.world.phys.AABB getRenderBoundingBox(reika.reactorcraft.reactor.ControlRodBlockEntity tile) {
			return new net.minecraft.world.phys.AABB(tile.getBlockPos()).expandTowards(0, 2, 0);
		}
	}

	public static final net.minecraft.client.model.geom.ModelLayerLocation STEAM_GRATE = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "steam_grate"), "main");
	public static final net.minecraft.client.model.geom.ModelLayerLocation[] TURBINE = new net.minecraft.client.model.geom.ModelLayerLocation[5];

	static {
		for (int i = 0; i < TURBINE.length; i++)
			TURBINE[i] = new net.minecraft.client.model.geom.ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "turbine_core"), "stage"+i);
	}

	public static final net.minecraft.client.model.geom.ModelLayerLocation CONDENSER = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "condenser"), "main");

	public static final net.minecraft.client.model.geom.ModelLayerLocation STEAM_DIFFUSER = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "steam_diffuser"), "main");

	/** Original RenderSteamDiffuser: the model, turned by the metadata (0: 90, 1: 270, 2: 180, 3: 0 degrees). */
	static final class SteamDiffuser implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.powergen.SteamDiffuserBlockEntity> {
		private final reika.reactorcraft.client.model.ModelDiffuser model;

		SteamDiffuser(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelDiffuser(ctx.bakeLayer(STEAM_DIFFUSER));
		}

		@Override
		public void render(reika.reactorcraft.powergen.SteamDiffuserBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			int rot = switch (tile.getBlockMetadata()) {
				case 0 -> 90;
				case 1 -> 270;
				case 2 -> 180;
				default -> 0;
			};
			pose.mulPose(reika.rotarycraft.client.model.TechneModel.rotation(rot, 0, 1, 0));
			model.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("diffuser.png"))), light, overlay, tile, null, -tile.phi, 0);
			pose.popPose();
		}
	}

	public static final net.minecraft.client.model.geom.ModelLayerLocation PRESSURIZER = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "pressurizer"), "main");

	/** Original RenderReactorPump: the model (not turned), its rotor at -phi (the pump turns it 15 degrees a tick while powered). */
	static final class Pressurizer implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.powergen.ReactorPumpBlockEntity> {
		private final reika.reactorcraft.client.model.ModelReactorPump model;

		Pressurizer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelReactorPump(ctx.bakeLayer(PRESSURIZER));
		}

		@Override
		public void render(reika.reactorcraft.powergen.ReactorPumpBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			model.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("pump.png"))), light, overlay, tile, null, -tile.phi, 0);
			pose.popPose();
		}
	}

	public static final net.minecraft.client.model.geom.ModelLayerLocation WASTE_STORAGE = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "waste_storage"), "main");

	/** Original RenderWasteStorage: the model, unrotated. */
	static final class WasteStorage implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.waste.WasteStorageBlockEntity> {
		private final reika.reactorcraft.client.model.ModelWasteStorage model;

		WasteStorage(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelWasteStorage(ctx.bakeLayer(WASTE_STORAGE));
		}

		@Override
		public void render(reika.reactorcraft.waste.WasteStorageBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			model.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("storage.png"))), light, overlay, tile, null, 0, 0);
			pose.popPose();
		}
	}

	public static final net.minecraft.client.model.geom.ModelLayerLocation HEAT_EXCHANGER = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "heat_exchanger"), "main");

	/** Original RenderExchanger: the model and, while it has a coolant, the hot fluid in the inner column and the cold one in the outer ring. */
	static final class HeatExchanger implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.powergen.HeatExchangerBlockEntity> {
		private final reika.reactorcraft.client.model.ModelExchanger model;

		HeatExchanger(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelExchanger(ctx.bakeLayer(HEAT_EXCHANGER));
		}

		@Override
		public void render(reika.reactorcraft.powergen.HeatExchangerBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			model.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("exchanger2.png"))), light, overlay, tile, null, 0, 0);
			pose.popPose();
			var e = tile.getLevel() != null ? tile.getCurrentRecipe() : null;
			if (e != null) {
				fluidBox(pose, buf, e.getHotFluid(), 0.375F, 0.0635F, 0.375F, 0.625F, 1.0135F, 0.625F, light);
				fluidBox(pose, buf, e.getColdFluid(), -0.04F, 0.4025F, -0.04F, 1.04F, 0.5925F, 1.04F, light);
			}
		}
	}

	/** A box of the fluid's still texture and tint (original ReikaLiquidRenderer display list, scaled). */
	static void fluidBox(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buf, net.minecraft.world.level.material.Fluid f,
			float x0, float y0, float z0, float x1, float y1, float z1, int light) {
		var ext = IClientFluidTypeExtensions.of(f);
		var sprite = net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
		int clr = ext.getTintColor();
		var vc = buf.getBuffer(net.minecraft.client.renderer.RenderType.translucent());
		org.joml.Matrix4f m = pose.last().pose();
		float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
		float[][] q = {
				{x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0}, {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1},
				{x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0}, {x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1},
				{x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0}, {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1}};
		float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
		for (float[] f4 : q)
			for (int i = 0; i < 4; i++)
				vc.addVertex(m, f4[i*3], f4[i*3+1], f4[i*3+2]).setColor(clr).setUv(uv[i][0], uv[i][1]).setLight(light).setNormal(0, 1, 0);
	}

	/** Original RenderCondenser: the model, unrotated. */
	static final class Condenser implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.powergen.CondenserBlockEntity> {
		private final reika.reactorcraft.client.model.ModelCondenser model;

		Condenser(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelCondenser(ctx.bakeLayer(CONDENSER));
		}

		@Override
		public void render(reika.reactorcraft.powergen.CondenserBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			model.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("condenser.png"))), light, overlay, tile, null, 0, 0);
			pose.popPose();
		}
	}

	/** Original RenderSteamGrate: the model, unrotated. */
	static final class SteamGrate implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.powergen.SteamGrateBlockEntity> {
		private final reika.reactorcraft.client.model.ModelSteamGrate model;

		SteamGrate(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelSteamGrate(ctx.bakeLayer(STEAM_GRATE));
		}

		@Override
		public void render(reika.reactorcraft.powergen.SteamGrateBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			model.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("steamgrate.png"))), light, overlay, tile, null, 0, 0);
			pose.popPose();
		}
	}

	public static final net.minecraft.client.model.geom.ModelLayerLocation[] BIG_TURBINE = new net.minecraft.client.model.geom.ModelLayerLocation[7];

	static {
		for (int i = 0; i < BIG_TURBINE.length; i++)
			BIG_TURBINE[i] = new net.minecraft.client.model.geom.ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "hip_turbine"), "stage"+i);
	}

	/**
	 * Original RenderTurbine / RenderBigTurbine: the stage's model turned by the metadata (0: 90, 1: 270, 2: 180, 3: 0) and spun by -phi;
	 * damage thins the blades. A high pressure turbine without its multiblock is drawn as a dark block (multiblock icon 3).
	 */
	static final class TurbineCore<T extends reika.reactorcraft.powergen.TurbineCoreBlockEntity> implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<T> {
		private final reika.reactorcraft.client.model.ModelTurbine[] models;
		private final String texture;

		TurbineCore(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx, boolean big) {
			net.minecraft.client.model.geom.ModelLayerLocation[] layers = big ? BIG_TURBINE : TURBINE;
			models = new reika.reactorcraft.client.model.ModelTurbine[layers.length];
			for (int i = 0; i < models.length; i++)
				models[i] = big ? new reika.reactorcraft.client.model.ModelBigTurbine(ctx.bakeLayer(layers[i]), i) : new reika.reactorcraft.client.model.ModelTurbine(ctx.bakeLayer(layers[i]), i);
			texture = big ? "bigturbine.png" : "turbine.png";
		}

		@Override
		public void render(T tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			int rot = switch (tile.getBlockMetadata()) {
				case 0 -> 90;
				case 1 -> 270;
				case 2 -> 180;
				default -> 0;
			};
			pose.mulPose(reika.rotarycraft.client.model.TechneModel.rotation(rot, 0, 1, 0));
			var vc = buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex(texture)));
			if (tile.getLevel() == null) { //item
				boolean big = tile instanceof reika.reactorcraft.powergen.HiPTurbineBlockEntity;
				float sc = big ? 0.4F : 0.6F;
				pose.scale(sc, sc, sc);
				pose.translate(big ? -0.3 : -0.1, big ? 1.7 : 0.8, 0);
				models[0].renderAll(pose, vc, light, overlay, tile, null, -tile.phi, 0);
				pose.popPose();
				return;
			}
			if (!tile.hasMultiBlock()) {
				pose.popPose();
				renderCube(pose, buf, light, "block/multi/turbine_3");
				return;
			}
			int dmg = tile.getDamage();
			int stage = Math.min(tile.getStage(), models.length-1);
			models[stage].renderAll(pose, vc, light, overlay, tile, dmg > 0 ? java.util.List.of(dmg) : null, -tile.phi, 0);
			pose.popPose();
		}

		@Override
		public net.minecraft.world.phys.AABB getRenderBoundingBox(T tile) {
			return new net.minecraft.world.phys.AABB(tile.getBlockPos()).inflate(6);
		}

		@Override
		public int getViewDistance() {
			return 128; //original: 4x the normal render distance
		}
	}

	/** The unformed turbine/generator cores: a plain cube with one block texture on every side. */
	static void renderCube(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buf, int light, String texture) {
			var sprite = net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
					.apply(ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, texture));
			var vc = buf.getBuffer(net.minecraft.client.renderer.RenderType.solid());
			var m = pose.last();
			float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
			float[][] q = {{0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0}, {0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1}, {0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0},
					{1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1}, {0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0}, {1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1}};
			float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
			for (float[] f4 : q)
				for (int i = 0; i < 4; i++)
					vc.addVertex(m, f4[i*3], f4[i*3+1], f4[i*3+2]).setColor(0xFFFFFFFF).setUv(uv[i][0], uv[i][1]).setUv1(0, 10).setUv2(light & 0xFFFF, light >> 16)
							.setNormal(m, 0, 1, 0);
	}

	/**
	 * Original RenderGenerator: formed, the generator model (rotor turning by phi) with the texture of its mode; unformed, a cube with
	 * the mode's block texture (generator_10..12); as an item, the model at 1/8 size.
	 */
	static final class Generator implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.powergen.ReactorGeneratorBlockEntity> {
		private final reika.reactorcraft.client.model.ModelGenerator model;

		Generator(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelGenerator(ctx.bakeLayer(GENERATOR));
		}

		@Override
		public void render(reika.reactorcraft.powergen.ReactorGeneratorBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			int mode = tile.getMode().ordinal();
			if (tile.getLevel() != null && !tile.hasMultiBlock()) {
				renderCube(pose, buf, light, "block/multi/generator_"+(10+mode));
				return;
			}
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			int rot = switch (tile.getBlockMetadata()) {
				case 0 -> 270;
				case 1 -> 90;
				case 3 -> 180;
				default -> 0;
			};
			pose.mulPose(reika.rotarycraft.client.model.TechneModel.rotation(rot, 0, 1, 0));
			var vc = buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("generator"+mode+".png")));
			if (tile.getLevel() == null) { //item
				pose.mulPose(reika.rotarycraft.client.model.TechneModel.rotation(180, 0, 1, 0));
				pose.translate(0, 0.875, 0.5);
				pose.scale(0.125F, 0.125F, 0.125F);
				model.renderAll(pose, vc, light, overlay, tile, null, -tile.phi, 0);
			}
			else
				model.renderAll(pose, vc, light, overlay, tile, null, tile.phi, 0);
			pose.popPose();
		}

		@Override
		public net.minecraft.world.phys.AABB getRenderBoundingBox(reika.reactorcraft.powergen.ReactorGeneratorBlockEntity tile) {
			net.minecraft.core.BlockPos p = tile.getBlockPos();
			net.minecraft.core.Direction f = tile.getFacing();
			int l = reika.reactorcraft.powergen.ReactorGeneratorBlockEntity.GENERATOR_LENGTH;
			int x1 = p.getX()+1+f.getStepX()*l, z1 = p.getZ()+1+f.getStepZ()*l;
			return new net.minecraft.world.phys.AABB(Math.min(x1, p.getX()), p.getY()-2, Math.min(z1, p.getZ()), Math.max(x1, p.getX()), p.getY()+3,
					Math.max(z1, p.getZ())).inflate(6);
		}

		@Override
		public boolean shouldRenderOffScreen(reika.reactorcraft.powergen.ReactorGeneratorBlockEntity tile) {
			return true;
		}
	}

	/**
	 * Original RenderTurbineWheel: formed, the flywheel model turning with phi (6x the turbine's); unformed, a cube with the flywheel
	 * dampener texture; as an item, the model at 1/4 size.
	 */
	static final class Flywheel implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.powergen.ReactorFlywheelBlockEntity> {
		private final reika.reactorcraft.client.model.ModelFlywheel model;

		Flywheel(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelFlywheel(ctx.bakeLayer(FLYWHEEL));
		}

		@Override
		public void render(reika.reactorcraft.powergen.ReactorFlywheelBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			if (tile.getLevel() != null && !tile.hasMultiBlock()) {
				renderCube(pose, buf, light, "block/multi/flywheel_1");
				return;
			}
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			int rot = switch (tile.getBlockMetadata()) {
				case 0 -> 270;
				case 1 -> 90;
				case 3 -> 180;
				default -> 0;
			};
			pose.mulPose(reika.rotarycraft.client.model.TechneModel.rotation(rot, 0, 1, 0));
			var vc = buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("flywheel.png")));
			if (tile.getLevel() == null) { //item
				pose.mulPose(reika.rotarycraft.client.model.TechneModel.rotation(180, 0, 1, 0));
				pose.translate(0, 0.75, 0);
				pose.scale(0.25F, 0.25F, 0.25F);
				model.renderAll(pose, vc, light, overlay, tile, null, -tile.phi, 0);
			}
			else
				model.renderAll(pose, vc, light, overlay, tile, null, tile.phi, 0);
			pose.popPose();
		}

		@Override
		public net.minecraft.world.phys.AABB getRenderBoundingBox(reika.reactorcraft.powergen.ReactorFlywheelBlockEntity tile) {
			return new net.minecraft.world.phys.AABB(tile.getBlockPos()).inflate(3);
		}
	}

	/** Original RenderSolarExchanger: the model, unrotated. */
	static final class SolarExchanger implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.powergen.SolarExchangerBlockEntity> {
		private final reika.reactorcraft.client.model.ModelSolarExchanger model;

		SolarExchanger(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelSolarExchanger(ctx.bakeLayer(SOLAR_EXCHANGER));
		}

		@Override
		public void render(reika.reactorcraft.powergen.SolarExchangerBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			model.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("solar.png"))), light, overlay, tile, null, 0, 0);
			pose.popPose();
		}
	}

	/**
	 * Original RenderSolarTop: the receiver model (the cap upside down and turned 90 degrees on top of the lower part), its glowing parts
	 * in the blackbody colour of 200+2*T at full brightness (black as an item); above 400 C the active part also shows the solar flare,
	 * facing the viewer, (T-400)/1500*6 blocks wide and as bright as the mirrors.
	 */
	static final class SolarTop implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.powergen.SolarTopBlockEntity> {
		private final reika.reactorcraft.client.model.ModelSolarTop model;

		SolarTop(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelSolarTop(ctx.bakeLayer(SOLAR_TOP));
		}

		@Override
		public void render(reika.reactorcraft.powergen.SolarTopBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			boolean inWorld = tile.getLevel() != null;
			boolean flip = inWorld && tile.getLevel().getBlockEntity(tile.getBlockPos().below()) instanceof reika.reactorcraft.powergen.SolarTopBlockEntity;
			pose.pushPose();
			pose.translate(0, flip ? 0 : 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			if (flip) {
				pose.mulPose(reika.rotarycraft.client.model.TechneModel.rotation(180, 1, 0, 0));
				pose.mulPose(reika.rotarycraft.client.model.TechneModel.rotation(90, 0, 1, 0));
			}
			int c = inWorld ? reika.rotarycraft.lib.math.BlackbodyColor.getColorForTemperature(200+tile.getTemperature()*2) : 0x000000;
			model.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("solartop.png"))), light, overlay, tile, java.util.List.of(c, 0, 0), 0, 0);
			pose.popPose();
			if (inWorld && tile.isActive() && tile.getTemperature() > 400)
				this.renderFlare(tile, pose, buf);
		}

		private void renderFlare(reika.reactorcraft.powergen.SolarTopBlockEntity te, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buf) {
			var sprite = net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
					.apply(ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "block/solarflare"));
			double f = (te.getTemperature()-400)/1500D;
			float s = (float)(6*f);
			float b = te.getLevel().getBlockEntity(te.getBlockPos().below()) instanceof reika.rotarycraft.content.blockentity.SolarTowerBlockEntity t ? t.getArrayOverallBrightness() : 0;
			int g = (int)(b*255);
			int color = 0xFF000000 | g << 16 | g << 8 | g;
			pose.pushPose();
			pose.translate(0.5, 1.0, 0.5); //original: 0.5 above the centre in the flipped model space = the top face
			pose.mulPose(net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
			pose.scale(s, s, s);
			var vc = buf.getBuffer(net.minecraft.client.renderer.RenderType.eyes(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)); //original additive blend
			var m = pose.last();
			vc.addVertex(m, -1, -1, 0).setColor(color).setUv(sprite.getU0(), sprite.getV1()).setOverlay(0).setLight(0xF000F0).setNormal(m, 0, 0, 1);
			vc.addVertex(m, 1, -1, 0).setColor(color).setUv(sprite.getU1(), sprite.getV1()).setOverlay(0).setLight(0xF000F0).setNormal(m, 0, 0, 1);
			vc.addVertex(m, 1, 1, 0).setColor(color).setUv(sprite.getU1(), sprite.getV0()).setOverlay(0).setLight(0xF000F0).setNormal(m, 0, 0, 1);
			vc.addVertex(m, -1, 1, 0).setColor(color).setUv(sprite.getU0(), sprite.getV0()).setOverlay(0).setLight(0xF000F0).setNormal(m, 0, 0, 1);
			pose.popPose();
		}

		@Override
		public net.minecraft.world.phys.AABB getRenderBoundingBox(reika.reactorcraft.powergen.SolarTopBlockEntity tile) {
			return new net.minecraft.world.phys.AABB(tile.getBlockPos()).inflate(4);
		}
	}

	/** Original RenderElectrolyzer: the model, unrotated. */
	static final class Electrolyzer implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.content.blockentity.ElectrolyzerBlockEntity> {
		private final reika.reactorcraft.client.model.ModelElectrolyzer model;

		Electrolyzer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelElectrolyzer(ctx.bakeLayer(ELECTROLYZER));
		}

		@Override
		public void render(reika.reactorcraft.content.blockentity.ElectrolyzerBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			model.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("electrolyzer.png"))), light, overlay, tile, null, 0, 0);
			pose.popPose();
		}
	}

	public static final net.minecraft.client.model.geom.ModelLayerLocation ELECTROLYZER = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "electrolyzer"), "main");

	public static final net.minecraft.client.model.geom.ModelLayerLocation SOLAR_EXCHANGER = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "solar_exchanger"), "main");
	public static final net.minecraft.client.model.geom.ModelLayerLocation SOLAR_TOP = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "solar_top"), "main");

	public static final net.minecraft.client.model.geom.ModelLayerLocation FLYWHEEL = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "turbine_flywheel"), "main");

	public static final net.minecraft.client.model.geom.ModelLayerLocation GENERATOR = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "reactor_generator"), "main");

	public static final net.minecraft.client.model.geom.ModelLayerLocation PROCESSOR = new net.minecraft.client.model.geom.ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "processor"), "main");

	/** Original SteamLineRenderer: the steam line's wool is drawn at half brightness. */
	@SubscribeEvent
	public static void blockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block e) {
		e.register((state, world, pos, tint) -> 0xFF7F7F7F, reika.reactorcraft.registry.ReactorBlocks.STEAM_LINE.get());
	}

	@SubscribeEvent
	public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e) {
		e.register(reika.reactorcraft.registry.ReactorMenus.PROCESSOR.get(), reika.reactorcraft.client.screen.UProcessorScreen::new);
		e.register(reika.reactorcraft.registry.ReactorMenus.SYNTHESIZER.get(), reika.reactorcraft.client.screen.SynthesizerScreen::new);
		e.register(reika.reactorcraft.registry.ReactorMenus.ELECTROLYZER.get(), reika.reactorcraft.client.screen.ElectrolyzerScreen::new);
		e.register(reika.reactorcraft.registry.ReactorMenus.ISO_CENTRIFUGE.get(), reika.reactorcraft.client.screen.IsoCentrifugeScreen::new);
		e.register(reika.reactorcraft.registry.ReactorMenus.NUCLEAR_CORE.get(), reika.reactorcraft.client.screen.NuclearCoreScreen::new);
		e.register(reika.reactorcraft.registry.ReactorMenus.CPU.get(), reika.reactorcraft.client.screen.CpuScreen::new);
		e.register(reika.reactorcraft.registry.ReactorMenus.THORIUM_CORE.get(), reika.reactorcraft.client.screen.ThoriumCoreScreen::new);
		e.register(reika.reactorcraft.registry.ReactorMenus.WASTE_CONTAINER.get(),
				(reika.reactorcraft.content.menu.WasteMenus.Container m, net.minecraft.world.entity.player.Inventory i, net.minecraft.network.chat.Component t) -> new reika.reactorcraft.client.screen.WasteScreen<>(m, i, t, "wastecontainer2", 175));
		e.register(reika.reactorcraft.registry.ReactorMenus.WASTE_STORAGE.get(),
				(reika.reactorcraft.content.menu.WasteMenus.Storage m, net.minecraft.world.entity.player.Inventory i, net.minecraft.network.chat.Component t) -> new reika.reactorcraft.client.screen.WasteScreen<>(m, i, t, "wastestorage", 186));
		e.register(reika.reactorcraft.registry.ReactorMenus.PEBBLE_BED.get(),
				(reika.reactorcraft.content.menu.WasteMenus.PebbleBed m, net.minecraft.world.entity.player.Inventory i, net.minecraft.network.chat.Component t) -> new reika.reactorcraft.client.screen.WasteScreen<>(m, i, t, "pebblegui", 240, 237));
		e.register(reika.reactorcraft.registry.ReactorMenus.WASTE_DECAYER.get(),
				(reika.reactorcraft.content.menu.WasteMenus.Decayer m, net.minecraft.world.entity.player.Inventory i, net.minecraft.network.chat.Component t) -> new reika.reactorcraft.client.screen.WasteScreen<>(m, i, t, "wastedecayer", 175));
	}

	/** Original RenderProcessor: lifted by 0.01, turned by the metadata (0: 270, 1: 90, 2: 0, 3: 180). */
	static final class Processor implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.content.blockentity.UProcessorBlockEntity> {
		private final reika.reactorcraft.client.model.ModelProcessor model;

		Processor(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelProcessor(ctx.bakeLayer(PROCESSOR));
		}

		@Override
		public void render(reika.reactorcraft.content.blockentity.UProcessorBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			pose.translate(0.0F, 0.01F, 0.0F);
			float a = 0;
			if (tile.getLevel() != null) {
				a = switch (tile.getBlockMetadata()) {
					case 0 -> 270;
					case 1 -> 90;
					case 3 -> 180;
					default -> 0;
				};
			}
			pose.mulPose(reika.rotarycraft.client.model.TechneModel.rotation(a, 0, 1, 0));
			model.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("processor.png"))), light, overlay, tile, null, -tile.phi, 0);
			pose.popPose();
		}
	}

	static ResourceLocation tex(String name) {
		return ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "textures/machine/"+name);
	}

	@SubscribeEvent
	public static void guiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent e) {
		//original IronFinderOverlay drew in the HELMET (camera overlay) pass
		e.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CAMERA_OVERLAYS, ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "iron_finder"),
				IronFinderOverlay::render);
	}

	@SubscribeEvent
	public static void layers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions e) {
		FusionRenderers.registerLayers(e);
		e.registerLayerDefinition(HEAVY_PUMP, reika.reactorcraft.client.model.ModelHeavyPump::createLayer);
		e.registerLayerDefinition(PROCESSOR, reika.reactorcraft.client.model.ModelProcessor::createLayer);
		e.registerLayerDefinition(ISO_CENTRIFUGE, reika.reactorcraft.client.model.ModelCentrifuge::createLayer);
		e.registerLayerDefinition(CONTROL_ROD, reika.reactorcraft.client.model.ModelControl::createLayer);
		e.registerLayerDefinition(STEAM_GRATE, reika.reactorcraft.client.model.ModelSteamGrate::createLayer);
		e.registerLayerDefinition(CONDENSER, reika.reactorcraft.client.model.ModelCondenser::createLayer);
		e.registerLayerDefinition(STEAM_DIFFUSER, reika.reactorcraft.client.model.ModelDiffuser::createLayer);
		e.registerLayerDefinition(PRESSURIZER, reika.reactorcraft.client.model.ModelReactorPump::createLayer);
		e.registerLayerDefinition(GENERATOR, reika.reactorcraft.client.model.ModelGenerator::createLayer);
		e.registerLayerDefinition(FLYWHEEL, reika.reactorcraft.client.model.ModelFlywheel::createLayer);
		e.registerLayerDefinition(ELECTROLYZER, reika.reactorcraft.client.model.ModelElectrolyzer::createLayer);
		e.registerLayerDefinition(SOLAR_EXCHANGER, reika.reactorcraft.client.model.ModelSolarExchanger::createLayer);
		e.registerLayerDefinition(SOLAR_TOP, reika.reactorcraft.client.model.ModelSolarTop::createLayer);
		e.registerLayerDefinition(HEAT_EXCHANGER, reika.reactorcraft.client.model.ModelExchanger::createLayer);
		e.registerLayerDefinition(WASTE_STORAGE, reika.reactorcraft.client.model.ModelWasteStorage::createLayer);
		for (int i = 0; i < TURBINE.length; i++) {
			int stage = i;
			e.registerLayerDefinition(TURBINE[i], () -> reika.reactorcraft.client.model.ModelTurbine.createLayer(stage));
		}
		for (int i = 0; i < BIG_TURBINE.length; i++) {
			int stage = i;
			e.registerLayerDefinition(BIG_TURBINE[i], () -> reika.reactorcraft.client.model.ModelBigTurbine.createLayer(stage));
		}
	}

	@SubscribeEvent
	public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e) {
		FusionRenderers.registerRenderers(e);
		e.registerEntityRenderer(reika.reactorcraft.registry.ReactorEntities.RADIATION.get(), reika.reactorcraft.client.RadiationRenderer::new);
		e.registerEntityRenderer(reika.reactorcraft.registry.ReactorEntities.NUCLEAR_WASTE.get(), net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
		e.registerEntityRenderer(reika.reactorcraft.registry.ReactorEntities.NEUTRON.get(), reika.reactorcraft.client.NeutronRenderer::new);
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.HEAVY_PUMP.get(), ctx -> new reika.rotarycraft.client.render.IORenderer<>(new HeavyPump(ctx)));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.PROCESSOR.get(), ctx -> new Processor(ctx));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.CONTROL_ROD.get(), ctx -> new ControlRod(ctx));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.STEAM_GRATE.get(), ctx -> new SteamGrate(ctx));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.CONDENSER.get(), ctx -> new Condenser(ctx));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.STEAM_DIFFUSER.get(), ctx -> new reika.rotarycraft.client.render.IORenderer<>(new SteamDiffuser(ctx)));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.PRESSURIZER.get(), ctx -> new reika.rotarycraft.client.render.IORenderer<>(new Pressurizer(ctx)));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.WASTE_PIPE.get(), reika.rotarycraft.client.render.PipeFluidRenderer::new);
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.HEAT_EXCHANGER.get(), ctx -> new reika.rotarycraft.client.render.IORenderer<>(new HeatExchanger(ctx)));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.WASTE_STORAGE.get(), ctx -> new WasteStorage(ctx));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.TURBINE_CORE.get(), ctx -> new reika.rotarycraft.client.render.IORenderer<>(new TurbineCore<>(ctx, false)));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.HIP_TURBINE.get(), ctx -> new reika.rotarycraft.client.render.IORenderer<>(new TurbineCore<>(ctx, true)));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.GENERATOR.get(), ctx -> new Generator(ctx));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.FLYWHEEL.get(), ctx -> new reika.rotarycraft.client.render.IORenderer<>(new Flywheel(ctx)));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.ELECTROLYZER.get(), ctx -> new Electrolyzer(ctx));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.SOLAR_EXCHANGER.get(), ctx -> new reika.rotarycraft.client.render.IORenderer<>(new SolarExchanger(ctx)));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.SOLAR_TOP.get(), ctx -> new SolarTop(ctx));
		e.registerBlockEntityRenderer(reika.reactorcraft.registry.ReactorBlockEntities.ISO_CENTRIFUGE.get(), ctx -> new reika.rotarycraft.client.render.IORenderer<>(new IsoCentrifuge(ctx)));
	}

	/** Original RenderHeavyPump: the model with the impeller turning by -phi. */
	static final class HeavyPump implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<reika.reactorcraft.content.blockentity.HeavyPumpBlockEntity> {
		private final reika.reactorcraft.client.model.ModelHeavyPump model;

		HeavyPump(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
			model = new reika.reactorcraft.client.model.ModelHeavyPump(ctx.bakeLayer(HEAVY_PUMP));
		}

		@Override
		public void render(reika.reactorcraft.content.blockentity.HeavyPumpBlockEntity tile, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
				net.minecraft.client.renderer.MultiBufferSource buf, int light, int overlay) {
			pose.pushPose();
			pose.translate(0, 2.0F, 1.0F);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.translate(0.5F, 0.5F, 0.5F);
			model.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(tex("heavypump.png"))), light, overlay, tile, null, -tile.phi, 0);
			pose.popPose();
		}
	}

	@SubscribeEvent
	public static void extensions(RegisterClientExtensionsEvent e) {
		//original PotionRadiation had no status icon
		e.registerMobEffect(new net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions() {
			@Override
			public boolean renderInventoryIcon(net.minecraft.world.effect.MobEffectInstance instance, net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen<?> screen,
					net.minecraft.client.gui.GuiGraphics g, int x, int y, int blitOffset) {
				return true;
			}

			@Override
			public boolean renderGuiIcon(net.minecraft.world.effect.MobEffectInstance instance, net.minecraft.client.gui.Gui gui, net.minecraft.client.gui.GuiGraphics g,
					int x, int y, float z, float alpha) {
				return true;
			}
		}, reika.reactorcraft.registry.ReactorEffects.RADIATION.value());
		reika.rotarycraft.client.MachineItemRenderer[] r = new reika.rotarycraft.client.MachineItemRenderer[1];
		e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
			@Override
			public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (r[0] == null)
					r[0] = new reika.rotarycraft.client.MachineItemRenderer();
				return r[0];
			}
		}, reika.reactorcraft.registry.ReactorBlocks.FLUID_EXTRACTOR.get().asItem(), reika.reactorcraft.registry.ReactorBlocks.PROCESSOR.get().asItem(),
				reika.reactorcraft.registry.ReactorBlocks.ISO_CENTRIFUGE.get().asItem(), reika.reactorcraft.registry.ReactorBlocks.CONTROL_ROD.get().asItem(),
				reika.reactorcraft.registry.ReactorBlocks.STEAM_GRATE.get().asItem(), reika.reactorcraft.registry.ReactorBlocks.TURBINE_CORE.get().asItem(),
				reika.reactorcraft.registry.ReactorBlocks.CONDENSER.get().asItem(), reika.reactorcraft.registry.ReactorBlocks.WASTE_STORAGE.get().asItem(),
				reika.reactorcraft.registry.ReactorBlocks.STEAM_DIFFUSER.get().asItem(), reika.reactorcraft.registry.ReactorBlocks.PRESSURIZER.get().asItem(),
				reika.reactorcraft.registry.ReactorBlocks.HEAT_EXCHANGER.get().asItem(),
				reika.reactorcraft.registry.ReactorBlocks.HIP_TURBINE.get().asItem(), reika.reactorcraft.registry.ReactorBlocks.GENERATOR.get().asItem(),
				reika.reactorcraft.registry.ReactorBlocks.FLYWHEEL.get().asItem(), reika.reactorcraft.registry.ReactorBlocks.SOLAR_EXCHANGER.get().asItem(),
				reika.reactorcraft.registry.ReactorBlocks.SOLAR_TOP.get().asItem(), reika.reactorcraft.registry.ReactorBlocks.ELECTROLYZER.get().asItem(),
				//the fusion machines' item models are builtin/entity too: without this they were drawn as nothing
				reika.reactorcraft.registry.ReactorBlocks.TOROID_MAGNET.get().asItem(), reika.reactorcraft.registry.ReactorBlocks.SOLENOID.get().asItem(),
				reika.reactorcraft.registry.ReactorBlocks.GAS_COLLECTOR.get().asItem(), reika.reactorcraft.registry.ReactorBlocks.FUSION_MARKER.get().asItem());
		for (ReactorFluids.Def d : ReactorFluids.DEFS.values()) {
			boolean water = d.texture() == null;
			ResourceLocation still = water ? ResourceLocation.withDefaultNamespace("block/water_still") : ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "block/fluid/"+d.texture());
			ResourceLocation flow = water ? ResourceLocation.withDefaultNamespace("block/water_flow") : still;
			e.registerFluidType(new IClientFluidTypeExtensions() {
				@Override
				public ResourceLocation getStillTexture() {
					return still;
				}

				@Override
				public ResourceLocation getFlowingTexture() {
					return flow;
				}

				/** The 1.7.10 water icon was already blue; the 1.21 one is grey and tinted. */
				@Override
				public int getTintColor() {
					return water ? 0xFF3F76E4 : 0xFFFFFFFF;
				}
			}, ReactorFluids.TYPE.get(d.id()).get());
		}
	}
}
