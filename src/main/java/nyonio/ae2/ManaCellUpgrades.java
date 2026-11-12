package nyonio.ae2;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import nyonio.item.ItemManaCard;
public class ManaCellUpgrades extends AppEngInternalInventory {
    private final ItemStack cell;
    private boolean loading=true;
    public ManaCellUpgrades(ItemStack cell,int slots) {
        super(null,slots,1);
        this.cell=cell;
        setFilter(new IAEItemFilter() {
            @Override public boolean allowExtract(IItemHandler inv,int slot,int amount) { return true; }
            @Override public boolean allowInsert(IItemHandler inv,int slot,ItemStack stack) { return stack.getItem() instanceof ItemManaCard; }
        });
        readFromNBT(cell.getOrCreateTag(),"upgrades");
        loading=false;
    }
    @Override protected void onContentsChanged(int slot) { if(!loading && cell!=null)writeToNBT(cell.getOrCreateTag(),"upgrades"); }
}
