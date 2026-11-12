package nyonio;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
public final class BotaniaApplieTab extends ItemGroup {
    public static final BotaniaApplieTab INSTANCE = new BotaniaApplieTab();
    private BotaniaApplieTab() { super(BotaniaApplie.MODID); }
    @Override public ItemStack createIcon() { return new ItemStack(BotaniaApplie.fluixManaPoolItem); }
}
