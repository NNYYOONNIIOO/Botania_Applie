package nyonio.coremod.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.cache.SecurityCache;
import appeng.me.storage.NetworkInventoryHandler;
import nyonio.ae2.ManaStack;
import nyonio.ae2.ManaStorageChannel;
import nyonio.item.ItemManaPacket;
import nyonio.BotaniaApplie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings({"unchecked", "DataFlowIssue"})
@Mixin(value = NetworkInventoryHandler.class, remap = false, priority = 1001)
public abstract class MixinNetworkInventoryHandler<T extends IAEStack<T>> {

    @Unique
    private IMEMonitor<ManaStack> manaMonitor;

    @Unique
    private IMEMonitor<IAEItemStack> itemMonitor;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void onInit(final IStorageChannel<?> chan, final SecurityCache security, final CallbackInfo ci) {
        try {
            itemMonitor = security.getGrid().<appeng.api.networking.storage.IStorageGrid>getCache(appeng.api.networking.storage.IStorageGrid.class).getInventory(
                appeng.api.AEApi.instance().storage().getStorageChannel(appeng.api.storage.channels.IItemStorageChannel.class)
            );
            manaMonitor = security.getGrid().<appeng.api.networking.storage.IStorageGrid>getCache(appeng.api.networking.storage.IStorageGrid.class).getInventory(ManaStorageChannel.INSTANCE);
        } catch (Exception e) {
            BotaniaApplie.getLogger().error("[BotaniaApplie] Failed to initialize MixinNetworkInventoryHandler", e);
        }
    }

    @Inject(method = "injectItems", at = @At("HEAD"), cancellable = true)
    public void injectManaItems(final T input, final Actionable mode, final IActionSource src, final CallbackInfoReturnable<T> cir) {
        if (input == null || !(input instanceof IAEItemStack)) return;
        
        IAEItemStack itemInput = (IAEItemStack) input;
        if (!ItemManaPacket.isManaPacket(itemInput.createItemStack())) return;
        
        cir.setReturnValue(input);
    }

    @Inject(method = "extractItems", at = @At("HEAD"), cancellable = true)
    public void extractManaItems(final T request, final Actionable mode, final IActionSource src, final CallbackInfoReturnable<T> cir) {
        if (request == null || !(request instanceof IAEItemStack)) return;
        
        IAEItemStack itemRequest = (IAEItemStack) request;
        if (!ItemManaPacket.isManaPacket(itemRequest.createItemStack())) return;
        
        cir.setReturnValue(null);
    }
}
