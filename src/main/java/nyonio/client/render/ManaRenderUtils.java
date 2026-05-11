package nyonio.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import nyonio.item.ItemManaPacket;

public class ManaRenderUtils {

    private static final ResourceLocation MANA_TEXTURE = new ResourceLocation("botania_applie:blocks/mana_packet");

    public static void renderManaIntoGui(int x, int y, int width, int height, long mana, long capacity) {
        if (mana <= 0) return;

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(MANA_TEXTURE.toString());

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        double fraction = Math.min(1.0, Math.max(0.0, (double) mana / (double) capacity));
        int manaHeight = (int) Math.round(height * fraction);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        int x2 = x + width;

        while (manaHeight > 0) {
            buf.begin(7, DefaultVertexFormats.POSITION_TEX);
            double y1 = y + height - manaHeight;
            double y2 = y1 + Math.min(manaHeight, width);
            double u1 = sprite.getMinU();
            double v1 = sprite.getMinV();
            double u2 = sprite.getMaxU();
            double v2 = sprite.getMaxV();

            if (manaHeight < width) {
                v2 = v1 + (v2 - v1) * ((double) manaHeight / (double) width);
                manaHeight = 0;
            } else {
                manaHeight -= width;
            }

            buf.pos(x, y1, 0).tex(u1, v1).endVertex();
            buf.pos(x, y2, 0).tex(u1, v2).endVertex();
            buf.pos(x2, y2, 0).tex(u2, v2).endVertex();
            buf.pos(x2, y1, 0).tex(u2, v1).endVertex();
            tess.draw();
        }

        GlStateManager.disableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    public static boolean renderManaPacketIntoGuiSlot(Slot slot, ItemStack stack,
                                                       FontRenderer fontRenderer) {
        if (!ItemManaPacket.isManaPacket(stack)) {
            return false;
        }

        long mana = ItemManaPacket.getMana(stack);
        if (mana <= 0) {
            return false;
        }

        renderManaIntoGui(slot.xPos, slot.yPos, 16, 16, mana, mana);

        String displayText;
        if (mana >= 1000000) {
            displayText = String.format("%.1fM", mana / 1000000.0);
        } else if (mana >= 1000) {
            displayText = String.format("%.1fk", mana / 1000.0);
        } else {
            displayText = String.valueOf(mana);
        }

        fontRenderer.drawStringWithShadow(displayText,
                slot.xPos + 17 - fontRenderer.getStringWidth(displayText),
                slot.yPos + 8 - fontRenderer.FONT_HEIGHT / 2,
                0xFFFFFF);

        return true;
    }
}
