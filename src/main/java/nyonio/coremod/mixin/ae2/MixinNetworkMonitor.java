package nyonio.coremod.mixin.ae2;

import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.cache.GridStorageCache;
import appeng.me.cache.NetworkMonitor;
import nyonio.ae2.ManaFakeMonitor;
import nyonio.ae2.ManaStorageChannel;
import nyonio.api.IManaNetworkMonitor;
import nyonio.BotaniaApplie;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;

@SuppressWarnings("unchecked")
@Mixin(value = NetworkMonitor.class, remap = false)
public abstract class MixinNetworkMonitor<T extends IAEStack<T>> implements IManaNetworkMonitor<T> {

    @Shadow
    @Final
    @Nonnull
    private IStorageChannel<?> myChannel;

    @Shadow
    @Final
    @Nonnull
    private GridStorageCache myGridCache;

    @Shadow
    protected abstract void postChange(boolean add, Iterable<T> changes, IActionSource src);

    @Unique
    private ManaFakeMonitor manaMonitor;

    @Override
    public void initManaMonitor() {
        if (this.myChannel instanceof IItemStorageChannel) {
            manaMonitor = new ManaFakeMonitor(this.myGridCache, ManaStorageChannel.INSTANCE);
            BotaniaApplie.getLogger().info("[BotaniaApplie] ManaFakeMonitor initialized for ME terminal display");
        }
    }

    @Override
    public void manaPostChange(boolean add, Iterable<T> changes, IActionSource src) {
        if (manaMonitor != null && this.myChannel instanceof IItemStorageChannel) {
            this.postChange(add, changes, src);
        }
    }

    @Inject(method = "getAvailableItems", at = @At("TAIL"))
    public void getAvailableItems(final IItemList<T> out, final CallbackInfoReturnable<IItemList<T>> cir) {
        if (manaMonitor != null && this.myChannel instanceof IItemStorageChannel) {
            try {
                IItemList<IAEItemStack> itemOut = (IItemList<IAEItemStack>) out;
                manaMonitor.getAvailableItems(itemOut);
            } catch (Exception e) {
                BotaniaApplie.getLogger().error("Failed to add mana to terminal display", e);
            }
        }
    }
}