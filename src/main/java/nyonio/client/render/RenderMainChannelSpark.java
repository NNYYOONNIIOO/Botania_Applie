package nyonio.client.render;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import nyonio.BotaniaApplie;
import nyonio.entity.EntityChannelSpark;

public class RenderMainChannelSpark extends RenderChannelSpark {
    public RenderMainChannelSpark(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    protected ResourceLocation getSparkSprite(EntityChannelSpark entity) {
        return new ResourceLocation(BotaniaApplie.MODID, "items/main_channel_spark");
    }
}
