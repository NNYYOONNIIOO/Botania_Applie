package nyonio.api;

import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEStack;

public interface IManaNetworkMonitor<T> {
    void initManaMonitor();
    
    void manaPostChange(boolean add, Iterable<T> changes, IActionSource src);
}