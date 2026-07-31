package nyonio.ae2;

import net.minecraft.item.ItemStack;

public interface IManaStorageCell {
    int getKilobytes();
    
    default int getBytesPerType(ItemStack cellItem) {
        return 8;
    }
    
    default int getTotalTypes(ItemStack cellItem) {
        return 1;
    }
    
    default double getIdleDrain() {
        return 1.0;
    }
}
