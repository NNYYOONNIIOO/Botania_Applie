package nyonio.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nyonio.entity.EntityChannelSpark;
import vazkii.botania.client.render.entity.RenderSparkBase;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RenderChannelSpark extends RenderSparkBase<EntityChannelSpark> {
    private static final ResourceLocation SPARK_TEXTURE =
            new ResourceLocation("botania", "textures/items/spark.png");

    public RenderChannelSpark(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntityChannelSpark entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.05F);

        float age = entity.ticksExisted + partialTicks;
        float alpha = 0.7F + 0.3F * (float) (Math.sin(age / 5.0D) + 0.5D) * 2.0F;
        float scale = 0.75F + 0.1F * (float) Math.sin(age / 10.0D);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        GlStateManager.scale(scale, scale, scale);
        bindTexture(SPARK_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.rotate(180.0F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(-0.5D, -0.25D, 0.0D).tex(0.0D, 1.0D).endVertex();
        buffer.pos(0.5D, -0.25D, 0.0D).tex(1.0D, 1.0D).endVertex();
        buffer.pos(0.5D, 0.75D, 0.0D).tex(1.0D, 0.0D).endVertex();
        buffer.pos(-0.5D, 0.75D, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityChannelSpark entity) {
        return SPARK_TEXTURE;
    }
}
