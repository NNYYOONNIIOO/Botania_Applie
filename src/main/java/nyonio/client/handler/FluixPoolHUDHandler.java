package nyonio.client.handler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nyonio.BotaniaApplie;
import nyonio.tile.TileFluixManaPool;
import vazkii.botania.api.recipe.IManaInfusionRecipe;
import vazkii.botania.client.core.handler.HUDHandler;
import vazkii.botania.client.core.helper.RenderHelper;
@Mod.EventBusSubscriber(modid=BotaniaApplie.MODID,value=Dist.CLIENT)
public final class FluixPoolHUDHandler {
    @SubscribeEvent public static void render(RenderGameOverlayEvent.Post e) {
        if(e.getType()!=RenderGameOverlayEvent.ElementType.ALL)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.world==null||mc.currentScreen!=null||!(mc.objectMouseOver instanceof BlockRayTraceResult))return;
        TileEntity tile=mc.world.getTileEntity(((BlockRayTraceResult)mc.objectMouseOver).getPos());
        if(!(tile instanceof TileFluixManaPool))return;
        ItemStack held=mc.player.getHeldItemMainhand();
        TileFluixManaPool pool=(TileFluixManaPool)tile;
        IManaInfusionRecipe recipe=pool.getMatchingRecipe(held,mc.world.getBlockState(pool.getPos().down()));
        if(recipe==null)return;
        int x=mc.getMainWindow().getScaledWidth()/2-11,y=mc.getMainWindow().getScaledHeight()/2+10;
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        mc.getTextureManager().bindTexture(HUDHandler.manaBar);
        RenderHelper.drawTexturedModalRect(e.getMatrixStack(),x,y,pool.getCurrentMana()>=recipe.getManaToConsume()?0:22,8,22,15);
        ItemStack result=recipe.getRecipeOutput(held);
        mc.getItemRenderer().renderItemAndEffectIntoGUI(held,x-20,y);
        mc.getItemRenderer().renderItemAndEffectIntoGUI(result,x+26,y);
        mc.getItemRenderer().renderItemOverlays(mc.fontRenderer,result,x+26,y);
        RenderSystem.disableBlend();
    }
}
