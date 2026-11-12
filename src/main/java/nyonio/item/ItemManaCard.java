package nyonio.item;
import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import nyonio.BotaniaApplie;
import java.util.List;
public class ItemManaCard extends Item implements IUpgradeModule {
    private final int manaPerSecond;
    public ItemManaCard(int rate) { super(BotaniaApplie.properties().maxStackSize(1));manaPerSecond=rate; }
    public int getManaPerSecond() { return manaPerSecond; }
    public int getManaPerTick() { return manaPerSecond/20; }
    @Override public Upgrades getType(ItemStack stack) { return Upgrades.CAPACITY; }
    @Override public void addInformation(ItemStack stack,World world,List<ITextComponent> lines,ITooltipFlag flag) { lines.add(new TranslationTextComponent("botania_applie.tooltip.mana_card_rate",manaPerSecond)); }
    public static int getTotalManaRate(ItemStack cell) {
        if(cell.isEmpty() || !cell.hasTag()) return 0;
        CompoundNBT tag=cell.getTag();
        int total=0;
        ListNBT cards=tag.getCompound("upgrades").getList("Items",10);
        for(int i=0;i<cards.size();i++) total+=rate(cards.getCompound(i).getString("id"));
        cards=tag.getList("manaCards",10);
        for(int i=0;i<cards.size();i++) total+=rate(cards.getCompound(i).getString("id"));
        return total;
    }
    private static int rate(String id) { return id.equals("botania_applie:mana_card_basic")?40:id.equals("botania_applie:mana_card_advanced")?200:0; }
}
