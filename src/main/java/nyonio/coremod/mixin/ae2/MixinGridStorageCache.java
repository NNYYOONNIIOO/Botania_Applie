package nyonio.coremod.mixin.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.cache.GridStorageCache;
import appeng.me.cache.NetworkMonitor;
import nyonio.ae2.ManaStorageChannel;
import nyonio.api.IManaNetworkMonitor;
import nyonio.BotaniaApplie;
import nyonio.item.ItemManaPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@SuppressWarnings("unchecked")
@Mixin(value = GridStorageCache.class, remap = false)
public abstract class MixinGridStorageCache {

    @Shadow
    @Final
    private Map<IItemStorageChannel, NetworkMonitor<?>> storageMonitors;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(final IGrid g, final CallbackInfo ci) {
        try {
            NetworkMonitor<?> itemMonitor = this.storageMonitors.get(
                appeng.api.AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class)
            );
            if (itemMonitor instanceof IManaNetworkMonitor) {
                ((IManaNetworkMonitor<?>) itemMonitor).initManaMonitor();
                BotaniaApplie.getLogger().info("[BotaniaApplie] ManaFakeMonitor initialized");
            }
        } catch (Exception e) {
            BotaniaApplie.getLogger().error("[BotaniaApplie] Failed to initialize mana monitor", e);
        }
    }

    @Inject(method = "postAlterationOfStoredItems", at = @At("TAIL"))
    public void postAlterationOfStoredItems(final IStorageChannel<?> chan, final Iterable<? extends IAEStack<?>> input, final IActionSource src, final CallbackInfo ci) {
        BotaniaApplie.getLogger().info("[BotaniaApplie] postAlterationOfStoredItems called - channel: " + chan.getClass().getSimpleName());
        
        // 当ManaStorageChannel中的存储发生变化时
        if (chan == ManaStorageChannel.INSTANCE) {
            try {
                java.util.List<IAEItemStack> changes = new java.util.ArrayList<>();
                
                for (IAEStack<?> stack : input) {
                    long manaAmount = stack.getStackSize();
                    BotaniaApplie.getLogger().info("[BotaniaApplie] Mana change: " + manaAmount);
                    
                    if (manaAmount != 0) {
                        IAEItemStack manaDisplay = ItemManaPacket.createAE(Math.abs(manaAmount));
                        if (manaDisplay != null) {
                            manaDisplay.setStackSize(manaAmount);
                            changes.add(manaDisplay);
                        }
                    }
                }
                
                if (!changes.isEmpty()) {
                    NetworkMonitor<?> itemMonitor = this.storageMonitors.get(
                        appeng.api.AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class)
                    );
                    
                    if (itemMonitor instanceof IManaNetworkMonitor) {
                        BotaniaApplie.getLogger().info("[BotaniaApplie] Notifying terminal of mana changes: " + changes.size() + " entries");
                        ((IManaNetworkMonitor<IAEItemStack>) itemMonitor).manaPostChange(true, changes, src);
                    }
                }
            } catch (Exception e) {
                BotaniaApplie.getLogger().error("[BotaniaApplie] Failed to post mana alteration", e);
            }
        }
    }

    @Inject(method = "postChangesToNetwork", at = @At("TAIL"))
    private <T extends IAEStack<T>, C extends IStorageChannel<T>> void postChangesToNetwork(
            final C chan, 
            final int upOrDown, 
            final IItemList<T> availableItems, 
            final IActionSource src, 
            final CallbackInfo ci
    ) {
        BotaniaApplie.getLogger().info("[BotaniaApplie] postChangesToNetwork called - channel: " + chan.getClass().getSimpleName() + ", direction: " + (upOrDown > 0 ? "ADD" : "REMOVE"));
        
        // 当ManaStorageChannel变化传播到网络时
        if (chan == ManaStorageChannel.INSTANCE) {
            try {
                java.util.List<IAEItemStack> changes = new java.util.ArrayList<>();
                
                for (IAEStack<?> stack : availableItems) {
                    long manaAmount = stack.getStackSize();
                    if (manaAmount != 0) {
                        IAEItemStack manaDisplay = ItemManaPacket.createAE(Math.abs(manaAmount));
                        if (manaDisplay != null) {
                            manaDisplay.setStackSize(manaAmount);
                            changes.add(manaDisplay);
                        }
                    }
                }
                
                if (!changes.isEmpty()) {
                    NetworkMonitor<?> itemMonitor = this.storageMonitors.get(
                        appeng.api.AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class)
                    );
                    
                    if (itemMonitor instanceof IManaNetworkMonitor) {
                        BotaniaApplie.getLogger().info("[BotaniaApplie] Notifying network of mana changes: " + changes.size() + " entries");
                        ((IManaNetworkMonitor<IAEItemStack>) itemMonitor).manaPostChange(upOrDown > 0, changes, src);
                    }
                }
            } catch (Exception e) {
                BotaniaApplie.getLogger().error("[BotaniaApplie] Failed to post mana changes to network", e);
            }
        }
    }
}