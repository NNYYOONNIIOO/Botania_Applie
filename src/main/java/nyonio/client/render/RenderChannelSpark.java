package nyonio.client.render;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nyonio.entity.EntityChannelSpark;
import vazkii.botania.client.render.entity.RenderSparkBase;

@SideOnly(Side.CLIENT)
public class RenderChannelSpark extends RenderSparkBase<EntityChannelSpark> {
    public RenderChannelSpark(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityChannelSpark entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
