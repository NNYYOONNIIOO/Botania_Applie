package nyonio.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderItemInFrameEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nyonio.client.render.ManaRenderUtils;
import nyonio.item.ItemManaPacket;

@SideOnly(Side.CLIENT)
public class ManaTerminalHandler {

    @SubscribeEvent
    public void onRenderItem(RenderItemInFrameEvent event) {
        ItemStack stack = event.getItem();
        if (ItemManaPacket.isManaPacket(stack)) {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            ManaRenderUtils.renderManaPacketIntoGuiSlot(null, stack, font);
        }
    }
}
