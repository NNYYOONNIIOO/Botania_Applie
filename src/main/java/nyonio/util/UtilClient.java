package nyonio.util;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public class UtilClient {
    
    public static ItemStack getMouseItem() {
        if (Minecraft.getMinecraft().player == null) {
            return ItemStack.EMPTY;
        }
        return Minecraft.getMinecraft().player.inventory.getItemStack();
    }
}
