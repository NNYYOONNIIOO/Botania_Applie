package nyonio.item;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.items.contents.CellConfig;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import nyonio.BotaniaApplie;
import nyonio.ae2.*;
import java.util.List;
public class ItemManaStorageCell extends Item implements IManaStorageCell,ICellWorkbenchItem {
    private final int kilobytes;
    private final double drain;
    public ItemManaStorageCell(int kilobytes,double idleDrain) { super(BotaniaApplie.properties().maxStackSize(1));this.kilobytes=kilobytes;drain=idleDrain; }
    @Override public int getKilobytes() { return kilobytes; }
    @Override public double getIdleDrain() { return drain; }
    @Override public boolean isEditable(ItemStack cell) { return true; }
    @Override public IItemHandler getConfigInventory(ItemStack cell) { return new CellConfig(cell); }
    @Override public IItemHandler getUpgradesInventory(ItemStack cell) { return new ManaCellUpgrades(cell,5); }
    @Override public FuzzyMode getFuzzyMode(ItemStack cell) { return FuzzyMode.IGNORE_ALL; }
    @Override public void setFuzzyMode(ItemStack cell,FuzzyMode mode) {}
    public static int getInstalledCardCount(ItemStack cell) {
        if(cell.isEmpty() || !cell.hasTag()) return 0;
        int count=0;ListNBT list=cell.getTag().getCompound("upgrades").getList("Items",10);
        for(int i=0;i<list.size();i++) { String id=list.getCompound(i).getString("id");if(id.equals("botania_applie:mana_card_basic")||id.equals("botania_applie:mana_card_advanced"))count++; }
        return count+cell.getTag().getList("manaCards",10).size();
    }
    @Override public void addInformation(ItemStack cell,World world,List<ITextComponent> lines,ITooltipFlag flag) {
        long amount=cell.getOrCreateTag().getCompound("#0").getLong("mana");
        lines.add(new TranslationTextComponent("botania_applie.tooltip.bytes_used",(amount+499)/500,kilobytes*1024));
        lines.add(new TranslationTextComponent("botania_applie.tooltip.stored_mana",ManaNumberFormat.format(amount)));
        int count=getInstalledCardCount(cell);
        if(count>0) {
            lines.add(new TranslationTextComponent("botania_applie.tooltip.mana_card_installed",count,5));
            lines.add(new TranslationTextComponent("botania_applie.tooltip.mana_card_gen",ItemManaCard.getTotalManaRate(cell)));
        }
    }
}
