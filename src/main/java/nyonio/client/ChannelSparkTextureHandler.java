package nyonio.client;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import nyonio.BotaniaApplie;
import nyonio.client.render.RenderChannelSpark;
import vazkii.botania.client.render.tile.RenderTilePool;
@Mod.EventBusSubscriber(modid=BotaniaApplie.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ChannelSparkTextureHandler {
    private ChannelSparkTextureHandler() {}
    @SubscribeEvent public static void client(FMLClientSetupEvent e) {
        RenderingRegistry.registerEntityRenderingHandler(BotaniaApplie.CHANNEL_SPARK,RenderChannelSpark::new);
        RenderingRegistry.registerEntityRenderingHandler(BotaniaApplie.MAIN_CHANNEL_SPARK,RenderChannelSpark::new);
        ClientRegistry.bindTileEntityRenderer(BotaniaApplie.FLUIX_POOL_TILE,RenderTilePool::new);
    }
    @SubscribeEvent public static void stitch(TextureStitchEvent.Pre e) {
        if(!e.getMap().getTextureLocation().equals(AtlasTexture.LOCATION_BLOCKS_TEXTURE))return;
        e.addSprite(BotaniaApplie.id("items/channel_spark"));
        e.addSprite(BotaniaApplie.id("items/main_channel_spark"));
    }
}
