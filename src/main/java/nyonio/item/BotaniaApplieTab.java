package nyonio.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import nyonio.BotaniaApplie;

public class BotaniaApplieTab extends CreativeTabs {
    public static final BotaniaApplieTab INSTANCE = new BotaniaApplieTab();
    private BotaniaApplieTab() { super(BotaniaApplie.MODID); }
    @Override public ItemStack getTabIconItem() { return new ItemStack(BotaniaApplie.fluixManaPool); }
}
