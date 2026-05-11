package nyonio.ae2;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.cache.GridStorageCache;
import appeng.me.cache.NetworkMonitor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class ManaFakeMonitor implements IMEMonitor<IAEItemStack> {

    private final NetworkMonitor<ManaStack> monitor;
    private final GridStorageCache storage;

    public ManaFakeMonitor(final GridStorageCache grid, final IStorageChannel<ManaStack> manaChannel) {
        nyonio.BotaniaApplie.getLogger().info("[BotaniaApplie] ManaFakeMonitor constructor called");
        nyonio.BotaniaApplie.getLogger().info("[BotaniaApplie] grid: " + grid);
        nyonio.BotaniaApplie.getLogger().info("[BotaniaApplie] manaChannel: " + manaChannel);
        
        this.monitor = (NetworkMonitor<ManaStack>) grid.getInventory(manaChannel);
        this.storage = grid;
        
        nyonio.BotaniaApplie.getLogger().info("[BotaniaApplie] monitor from grid: " + this.monitor);
    }

    @Override
    public IAEItemStack injectItems(final IAEItemStack stack, final Actionable actionable, final IActionSource source) {
        if (stack == null) return null;

        ItemStack itemRep = stack.asItemStackRepresentation();
        if (itemRep == null || !nyonio.item.ItemManaPacket.isManaPacket(itemRep)) {
            return stack;
        }

        long mana = nyonio.item.ItemManaPacket.getMana(itemRep);
        if (mana <= 0) {
            return stack;
        }

        ManaStack toInject = new ManaStack(mana);
        FakeMonitorSource fakeSource = FakeMonitorSource.release(source);
        ManaStack result = monitor.injectItems(toInject, actionable, fakeSource);
        fakeSource.recycle();

        if (result == null || result.getStackSize() <= 0) {
            return null; // All injected
        } else {
            // Return remaining as IAEItemStack
            IAEItemStack remaining = nyonio.item.ItemManaPacket.createAE(result.getStackSize());
            return remaining;
        }
    }

    @Override
    public IAEItemStack extractItems(final IAEItemStack stack, final Actionable actionable, final IActionSource source) {
        if (stack == null) return null;

        ItemStack itemRep = stack.asItemStackRepresentation();
        if (itemRep == null || !nyonio.item.ItemManaPacket.isManaPacket(itemRep)) {
            return null;
        }

        long requestedMana = nyonio.item.ItemManaPacket.getMana(itemRep);
        if (requestedMana <= 0) {
            return null;
        }

        ManaStack toExtract = new ManaStack(requestedMana);
        FakeMonitorSource fakeSource = FakeMonitorSource.release(source);
        ManaStack result = monitor.extractItems(toExtract, actionable, fakeSource);
        fakeSource.recycle();

        if (result == null || result.getStackSize() <= 0) {
            return null; // Nothing extracted
        } else {
            // Return as IAEItemStack
            IAEItemStack extracted = nyonio.item.ItemManaPacket.createAE(result.getStackSize());
            return extracted;
        }
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> list) {
        if (list == null) return list;
        
        try {
            // 清除旧的mana条目（使用AE2的方式）
            IAEItemStack manaDrop = nyonio.item.ItemManaPacket.createAE(1);
            if (manaDrop != null) {
                list.findFuzzy(manaDrop, appeng.api.config.FuzzyMode.IGNORE_ALL)
                    .forEach(i -> i.setStackSize(0));
            }
            
            // 获取实际存储的魔力并添加
            IItemList<ManaStack> storageList = monitor.getStorageList();
            
            if (storageList != null && !storageList.isEmpty()) {
                for (ManaStack ms : storageList) {
                    long manaAmount = ms.getStackSize();
                    if (manaAmount > 0) {
                        IAEItemStack manaDisplay = nyonio.item.ItemManaPacket.createAE(manaAmount);
                        if (manaDisplay != null) {
                            manaDisplay.setStackSize(manaAmount);
                            list.addStorage(manaDisplay);  // 使用addStorage而不是add！
                        }
                    }
                }
            }
        } catch (Exception e) {
            nyonio.BotaniaApplie.getLogger().error("[BotaniaApplie] Error in getAvailableItems", e);
        }

        return list;
    }

    @Override
    public IStorageChannel<IAEItemStack> getChannel() {
        return null;
    }

    @Override
    public IItemList<IAEItemStack> getStorageList() {
        return null;
    }

    @Override
    public void addListener(IMEMonitorHandlerReceiver<IAEItemStack> receiver, Object verificationToken) {
    }

    @Override
    public void removeListener(IMEMonitorHandlerReceiver<IAEItemStack> receiver) {
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public boolean isPrioritized(IAEItemStack stack) {
        if (stack == null) return false;
        ItemStack itemRep = stack.asItemStackRepresentation();
        return itemRep != null && nyonio.item.ItemManaPacket.isManaPacket(itemRep);
    }

    @Override
    public boolean canAccept(IAEItemStack stack) {
        if (stack == null) return false;
        ItemStack itemRep = stack.asItemStackRepresentation();
        return itemRep != null && nyonio.item.ItemManaPacket.isManaPacket(itemRep);
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getSlot() {
        return monitor.getSlot();
    }

    @Override
    public boolean validForPass(int i) {
        return i == 2;
    }

    public GridStorageCache getStorage() {
        return storage;
    }

    public static class FakeMonitorSource implements IActionSource {

        private static final Deque<FakeMonitorSource> POOL = new ArrayDeque<>(100);
        private IActionSource source;

        public static FakeMonitorSource release(IActionSource source) {
            synchronized (POOL) {
                if (!POOL.isEmpty()) {
                    FakeMonitorSource s = POOL.peek();
                    s.source = source;
                    return s;
                }
            }
            return new FakeMonitorSource(source);
        }

        private FakeMonitorSource(IActionSource source) {
            this.source = source;
        }

        public IActionSource getSource() {
            return source;
        }

        public void recycle() {
            synchronized (POOL) {
                if (POOL.size() < 100) POOL.add(this);
            }
        }

        @Nonnull
        @Override
        public final java.util.Optional<EntityPlayer> player() {
            return source.player();
        }

        @Nonnull
        @Override
        public final java.util.Optional<IActionHost> machine() {
            return source.machine();
        }

        @Nonnull
        @Override
        public final <T> java.util.Optional<T> context(@Nonnull Class<T> aClass) {
            return source.context(aClass);
        }
    }
}