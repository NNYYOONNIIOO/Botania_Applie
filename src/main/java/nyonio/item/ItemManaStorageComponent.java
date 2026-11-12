package nyonio.item;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import nyonio.BotaniaApplie;
import nyonio.ModConfig;
public class ItemManaStorageComponent extends Item {
    private final int kilobytes;
    public ItemManaStorageComponent(int kilobytes) { super(BotaniaApplie.properties());this.kilobytes=kilobytes; }
    public int getKilobytes() { return kilobytes; }
    @Override public void fillItemGroup(ItemGroup group,NonNullList<ItemStack> items) { if(ModConfig.ENABLE_COMPONENTS.get()) super.fillItemGroup(group,items); }
}
