/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.MobRadarBlockEntity;
import reika.rotarycraft.content.menu.MobRadarMenu;

/** Original GuiMobRadar (a GuiPowerOnlyMachine): a 200 x 200 map of the living entities around the radar. */
public class MobRadarScreen extends AbstractContainerScreen<MobRadarMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/mobradargui.png");
	private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/mobicons.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	/** The icon sheet is indexed by the global entity ids of 1.7.10 (EntityList); players are -1. */
	private static final Map<EntityType<?>, Integer> ICON_IDS = new HashMap<>();

	static {
		ICON_IDS.put(EntityType.CREEPER, 50);
		ICON_IDS.put(EntityType.SKELETON, 51);
		ICON_IDS.put(EntityType.SPIDER, 52);
		ICON_IDS.put(EntityType.GIANT, 53);
		ICON_IDS.put(EntityType.ZOMBIE, 54);
		ICON_IDS.put(EntityType.SLIME, 55);
		ICON_IDS.put(EntityType.GHAST, 56);
		ICON_IDS.put(EntityType.ZOMBIFIED_PIGLIN, 57);
		ICON_IDS.put(EntityType.ENDERMAN, 58);
		ICON_IDS.put(EntityType.CAVE_SPIDER, 59);
		ICON_IDS.put(EntityType.SILVERFISH, 60);
		ICON_IDS.put(EntityType.BLAZE, 61);
		ICON_IDS.put(EntityType.MAGMA_CUBE, 62);
		ICON_IDS.put(EntityType.ENDER_DRAGON, 63);
		ICON_IDS.put(EntityType.WITHER, 64);
		ICON_IDS.put(EntityType.BAT, 65);
		ICON_IDS.put(EntityType.WITCH, 66);
		ICON_IDS.put(EntityType.PIG, 90);
		ICON_IDS.put(EntityType.SHEEP, 91);
		ICON_IDS.put(EntityType.COW, 92);
		ICON_IDS.put(EntityType.CHICKEN, 93);
		ICON_IDS.put(EntityType.SQUID, 94);
		ICON_IDS.put(EntityType.WOLF, 95);
		ICON_IDS.put(EntityType.MOOSHROOM, 96);
		ICON_IDS.put(EntityType.SNOW_GOLEM, 97);
		ICON_IDS.put(EntityType.OCELOT, 98);
		ICON_IDS.put(EntityType.IRON_GOLEM, 99);
		ICON_IDS.put(EntityType.HORSE, 100);
		ICON_IDS.put(EntityType.VILLAGER, 120);
	}

	private final Random rand = new Random();

	public MobRadarScreen(MobRadarMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 214;
		imageHeight = 223;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int j = leftPos, k = topPos+1;
		g.blit(TEX, j, k-1, 0, 0, imageWidth, imageHeight);
		MobRadarBlockEntity radar = menu.tile;
		if (radar == null)
			return;
		this.drawPowerTab(g, j, k-1, radar);
		int range = radar.getRange();
		if (radar.isJammed())
			this.drawStatic(g, j+7, k+15, j+206, k+214);
		else if (range > 0)
			for (LivingEntity e : radar.getEntities()) {
				int dx = 100+Mth.floor(100D*(e.getX()-radar.getBlockPos().getX()-0.5)/range);
				int dz = 100+Mth.floor(100D*(e.getZ()-radar.getBlockPos().getZ()-0.5)/range);
				this.drawMobIcon(g, j+7, k+16, dx, dz, e);
			}
		int a = j+7, b = k+16;
		g.drawString(font, range+"m", a+102, b, 11206570, false);
		g.drawString(font, (int)(0.63*range)+"m", a+102, b+37, 11206570, false);
		g.drawString(font, Mth.ceil(0.31*range)+"m", a+102, b+69, 11206570, false);
	}

	/** Original drawMobIcon; mobs newer than the icon sheet get a dot: blue players, green animals, red for the rest. */
	private void drawMobIcon(GuiGraphics g, int a, int b, int px, int py, LivingEntity e) {
		Integer id = e instanceof Player ? Integer.valueOf(-1) : ICON_IDS.get(e.getType());
		if (id == null) {
			int color = e instanceof Animal ? 0xff00c000 : 0xffe00000;
			g.fill(a+px, b+py, a+px+4, b+py+4, color);
			return;
		}
		int u = 8*(id%16), v = 8*(id/16);
		if (id == -1) {
			u = 8;
			v = 0;
		}
		g.blit(ICONS, a+px-2, b+py-2, u, v, 8, 8);
	}

	/** Original ReikaGuiAPI.renderStatic: grey noise over the screen while a RadarJammer is in range. */
	private void drawStatic(GuiGraphics g, int minx, int miny, int maxx, int maxy) {
		for (int x = minx; x <= maxx; x += 2) {
			for (int y = miny; y <= maxy; y += 2) {
				int br = rand.nextInt(256);
				g.fill(x, y, Math.min(x+2, maxx+1), Math.min(y+2, maxy+1), 0xff000000 | br << 16 | br << 8 | br);
			}
		}
	}

	/** Original GuiPowerOnlyMachine.drawPowerTab. */
	private void drawPowerTab(GuiGraphics g, int j, int k, MobRadarBlockEntity te) {
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, 24);
		g.blit(POWERTAB, j+imageWidth, k+4+23, 0, 157, 42, 6);
		long frac = Math.min(29, te.power*29L/te.MINPOWER);
		g.blit(POWERTAB, j+imageWidth+5, k+22, 0, 0, (int)frac, 4);
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
