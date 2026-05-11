package nyonio;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class BotaniaApplieTab extends CreativeTabs {

    public static final BotaniaApplieTab INSTANCE = new BotaniaApplieTab();

    private BotaniaApplieTab() {
        super("botania_applie");
    }

    @Override
    public ItemStack getTabIconItem() {
        return new ItemStack(BotaniaApplie.manaStorageCell16k);
    }
}
