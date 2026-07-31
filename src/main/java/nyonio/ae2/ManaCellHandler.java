package nyonio.ae2;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import appeng.api.storage.ICellHandler;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;

public class ManaCellHandler implements ICellHandler {

    public static final ManaCellHandler INSTANCE = new ManaCellHandler();

    @Override
    public boolean isCell(ItemStack is) {
        if (is == null || is.isEmpty()) {
            return false;
        }
        return is.getItem() instanceof IManaStorageCell;
    }

    @Override
    @Nullable
    public <T extends IAEStack<T>> ICellInventoryHandler<T> getCellInventory(ItemStack is, ISaveProvider host, IStorageChannel<T> channel) {
        if (!this.isCell(is)) {
            return null;
        }
        
        if (channel != ManaStorageChannel.INSTANCE) {
            return null;
        }

        IManaStorageCell cellItem = (IManaStorageCell) is.getItem();
        ManaCellInventory inventory = new ManaCellInventory(is, host, cellItem.getKilobytes());
        @SuppressWarnings("unchecked")
        ICellInventoryHandler<T> handler = (ICellInventoryHandler<T>) new ManaCellInventoryHandler(inventory);
        return handler;
    }
}
