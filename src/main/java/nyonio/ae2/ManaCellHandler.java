package nyonio.ae2;

import javax.annotation.Nullable;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraft.item.ItemStack;
import appeng.api.storage.ICellHandler;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;

public class ManaCellHandler implements ICellHandler {

    private static final Logger LOGGER = LogManager.getLogger("BotaniaApplie");
    public static final ManaCellHandler INSTANCE = new ManaCellHandler();

    @Override
    public boolean isCell(ItemStack is) {
        if (is == null || is.isEmpty()) {
            return false;
        }
        boolean result = is.getItem() instanceof IManaStorageCell;
        
        LOGGER.info("ManaCellHandler.isCell: item=" + (is.getItem() != null ? is.getItem().getClass().getSimpleName() : "null") + 
                   ", result=" + result);
        
        return result;
    }

    @Override
    @Nullable
    public <T extends IAEStack<T>> ICellInventoryHandler<T> getCellInventory(ItemStack is, ISaveProvider host, IStorageChannel<T> channel) {
        if (!this.isCell(is)) {
            LOGGER.info("getCellInventory: isCell returned false");
            return null;
        }
        
        if (channel != ManaStorageChannel.INSTANCE) {
            LOGGER.info("getCellInventory: channel mismatch - expected=" + ManaStorageChannel.INSTANCE.getClass().getSimpleName() + 
                       ", actual=" + (channel != null ? channel.getClass().getSimpleName() : "null"));
            return null;
        }

        LOGGER.info("getCellInventory: creating inventory for mana storage cell");
        
        IManaStorageCell cellItem = (IManaStorageCell) is.getItem();
        ManaCellInventory inventory = new ManaCellInventory(is, host, cellItem.getKilobytes());
        @SuppressWarnings("unchecked")
        ICellInventoryHandler<T> handler = (ICellInventoryHandler<T>) new ManaCellInventoryHandler(inventory);
        return handler;
    }
}
