package nyonio.client.render;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import nyonio.BotaniaApplie;
import nyonio.entity.EntityChannelSpark;
import vazkii.botania.client.render.entity.RenderSparkBase;
public class RenderChannelSpark extends RenderSparkBase<EntityChannelSpark> {
    public RenderChannelSpark(EntityRendererManager manager) { super(manager); }
    @Override protected TextureAtlasSprite getBaseIcon(EntityChannelSpark entity) { return Minecraft.getInstance().getAtlasSpriteGetter(AtlasTexture.LOCATION_BLOCKS_TEXTURE).apply(BotaniaApplie.id(entity.isMainChannelSpark()?"items/main_channel_spark":"items/channel_spark")); }
}
