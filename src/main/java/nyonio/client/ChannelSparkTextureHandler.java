package nyonio.client;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nyonio.BotaniaApplie;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = BotaniaApplie.MODID, value = Side.CLIENT)
public final class ChannelSparkTextureHandler {
    private static final ResourceLocation CHANNEL_SPARK_SPRITE =
            new ResourceLocation(BotaniaApplie.MODID, "items/channel_spark");
    private static final ResourceLocation MAIN_CHANNEL_SPARK_SPRITE =
            new ResourceLocation(BotaniaApplie.MODID, "items/main_channel_spark");
    private static final ResourceLocation BOTANIA_SPARK_SPRITE =
            new ResourceLocation("botania", "items/spark");

    private ChannelSparkTextureHandler() {
    }

    @SubscribeEvent
    public static void registerBotaniaSparkSprite(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(CHANNEL_SPARK_SPRITE);
        event.getMap().registerSprite(MAIN_CHANNEL_SPARK_SPRITE);
        event.getMap().registerSprite(BOTANIA_SPARK_SPRITE);
    }
}
