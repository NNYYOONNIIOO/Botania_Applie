package nyonio.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.List;

public class ItemManaStorageComponent extends Item {
    private final int kilobytes;
    
    public ItemManaStorageComponent(int kilobytes) {
        this.kilobytes = kilobytes;
        this.setMaxStackSize(64);
        this.setHasSubtypes(false);
    }
    
    public int getKilobytes() {
        return this.kilobytes;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public void getSubItems(CreativeTabs tab, net.minecraft.util.NonNullList<ItemStack> subItems) {
        if (this.isInCreativeTab(tab)) {
            subItems.add(new ItemStack(this));
        }
    }
}
