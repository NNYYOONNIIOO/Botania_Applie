package nyonio.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nyonio.entity.EntityChannelSpark;
import vazkii.botania.client.core.handler.MiscellaneousIcons;
import vazkii.botania.client.render.entity.RenderSparkBase;

@SideOnly(Side.CLIENT)
public class RenderChannelSpark extends RenderSparkBase<EntityChannelSpark> {
    private static final ResourceLocation BOTANIA_SPARK_SPRITE =
            new ResourceLocation("botania", "items/spark");

    public RenderChannelSpark(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    protected TextureAtlasSprite getBaseIcon(EntityChannelSpark entity) {
        TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks();
        TextureAtlasSprite icon = MiscellaneousIcons.INSTANCE.sparkWorldIcon;
        if (icon != null && icon != textureMap.getMissingSprite()) {
            return icon;
        }
        return textureMap.getAtlasSprite(BOTANIA_SPARK_SPRITE.toString());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityChannelSpark entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
