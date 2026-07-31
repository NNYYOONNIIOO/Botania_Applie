package nyonio.client.handler;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import vazkii.botania.api.recipe.RecipeManaInfusion;
import vazkii.botania.client.core.handler.HUDHandler;
import vazkii.botania.client.core.helper.RenderHelper;
import nyonio.tile.TileFluixManaPool;

@SideOnly(Side.CLIENT)
public class FluixPoolHUDHandler {
    
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if(event.getType() != RenderGameOverlayEvent.ElementType.ALL)
            return;
        
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if(player == null || mc.currentScreen != null)
            return;
        
        RayTraceResult trace = mc.objectMouseOver;
        if(trace == null || trace.typeOfHit != RayTraceResult.Type.BLOCK)
            return;
        
        BlockPos pos = trace.getBlockPos();
        TileEntity tile = mc.world.getTileEntity(pos);
        
        if(tile instanceof TileFluixManaPool && !player.getHeldItemMainhand().isEmpty()) {
            renderPoolRecipeHUD(event.getResolution(), (TileFluixManaPool) tile, player.getHeldItemMainhand());
        }
    }
    
    private void renderPoolRecipeHUD(ScaledResolution res, TileFluixManaPool tile, ItemStack stack) {
        Minecraft mc = Minecraft.getMinecraft();
        
        RecipeManaInfusion recipe = TileFluixManaPool.getMatchingRecipe(stack, tile.getWorld().getBlockState(tile.getPos().down()));
        if(recipe != null) {
            int x = res.getScaledWidth() / 2 - 11;
            int y = res.getScaledHeight() / 2 + 10;
            
            int u = tile.getCurrentMana() >= recipe.getManaToConsume() ? 0 : 22;
            int v = 8;
            
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            
            mc.renderEngine.bindTexture(HUDHandler.manaBar);
            RenderHelper.drawTexturedModalRect(x, y, 0, u, v, 22, 15);
            GlStateManager.color(1F, 1F, 1F, 1F);
            
            net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x - 20, y);
            mc.getRenderItem().renderItemAndEffectIntoGUI(recipe.getOutput(), x + 26, y);
            mc.getRenderItem().renderItemOverlays(mc.fontRenderer, recipe.getOutput(), x + 26, y);
            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            
            GlStateManager.disableLighting();
            GlStateManager.disableBlend();
        }
    }
}
