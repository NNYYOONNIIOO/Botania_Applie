package nyonio.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import nyonio.BotaniaApplie;
import nyonio.ModConfig;

public final class ItemManaCellHousing extends Item {
    public ItemManaCellHousing() { super(BotaniaApplie.properties()); }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (ModConfig.ENABLE_HOUSING.get()) super.fillItemGroup(group, items);
    }
}
