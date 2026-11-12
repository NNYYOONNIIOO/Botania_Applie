package nyonio.ae2;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.items.IItemHandler;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.cells.ICellInventory;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.data.IItemList;
import appeng.items.contents.CellConfig;
import appeng.util.Platform;
import nyonio.item.ItemManaStorageCell;

public class ManaCellInventory implements ICellInventory<ManaStack> {

    private static final String MANA_TAG = "mana";
    
    private static final String ITEM_SLOT_0 = "#0";
    private static final String ITEM_COUNT_0 = "@0";
    private static final String ITEM_TYPE_TAG = "it";
    private static final String ITEM_COUNT_TAG = "ic";

    private final ItemStack itemStack;
    private final ISaveProvider saveProvider;
    private final int totalBytes;
    private long storedMana;
    private boolean isPersisted = true;
    private final CompoundNBT tagCompound;

    public ManaCellInventory(ItemStack itemStack, ISaveProvider saveProvider, int kilobytes) {
        this.itemStack = itemStack;
        this.saveProvider = saveProvider;
        this.totalBytes = kilobytes * 1024;
        
        // 使用Platform.openNbtData获取NBT的直接引用（与AE2一致）
        this.tagCompound = itemStack.getOrCreateTag();
        
        // 从AE2标准格式读取魔力值
        if (this.tagCompound.contains(ITEM_SLOT_0) && this.tagCompound.getCompound(ITEM_SLOT_0).contains(MANA_TAG)) {
            this.storedMana = this.tagCompound.getCompound(ITEM_SLOT_0).getLong(MANA_TAG);
        } else {
            this.storedMana = 0;
        }
    }

    @Override
    public ItemStack getItemStack() {
        return this.itemStack;
    }

    @Override
    public double getIdleDrain() {
        return 1.0;
    }

    @Override
    public FuzzyMode getFuzzyMode() {
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public IItemHandler getConfigInventory() {
        if (this.itemStack.getItem() instanceof ItemManaStorageCell) {
            return ((ItemManaStorageCell) this.itemStack.getItem()).getConfigInventory(this.itemStack);
        }
        return new CellConfig(this.itemStack);
    }

    @Override
    public IItemHandler getUpgradesInventory() {
        if (this.itemStack.getItem() instanceof ItemManaStorageCell) {
            return ((ItemManaStorageCell) this.itemStack.getItem()).getUpgradesInventory(this.itemStack);
        }
        return new ManaCellUpgrades(this.itemStack, 5);
    }

    @Override
    public int getBytesPerType() {
        return 8;
    }

    @Override
    public boolean canHoldNewItem() {
        return getRemainingItemCount() > 0;
    }

    @Override
    public long getTotalBytes() {
        return this.totalBytes;
    }

    @Override
    public long getFreeBytes() {
        return this.totalBytes - this.getUsedBytes();
    }

    @Override
    public long getUsedBytes() {
        long unitsPerByte = ManaStorageChannel.INSTANCE.getUnitsPerByte();
        return (this.storedMana + unitsPerByte - 1) / unitsPerByte;
    }

    @Override
    public long getTotalItemTypes() {
        return 1;
    }

    @Override
    public long getStoredItemCount() {
        return this.storedMana;
    }

    @Override
    public long getStoredItemTypes() {
        return this.storedMana > 0 ? 1 : 0;
    }

    @Override
    public long getRemainingItemTypes() {
        return this.storedMana > 0 ? 0 : 1;
    }

    @Override
    public long getRemainingItemCount() {
        long maxMana = (long) this.totalBytes * ManaStorageChannel.INSTANCE.getUnitsPerByte();
        return maxMana - this.storedMana;
    }

    @Override
    public int getUnusedItemCount() {
        return 0;
    }

    @Override
    public appeng.api.storage.cells.CellState getStatusForCell() {
        if (this.storedMana == 0) {
            return appeng.api.storage.cells.CellState.EMPTY;
        }
        
        if (this.canHoldNewItem()) {
            return appeng.api.storage.cells.CellState.NOT_EMPTY;
        }
        
        if (this.getRemainingItemCount() > 0) {
            return appeng.api.storage.cells.CellState.TYPES_FULL;
        }
        
        return appeng.api.storage.cells.CellState.FULL;
    }

    @Override
    public void persist() {
        if (this.isPersisted) {
            return;
        }
        
        if (this.storedMana > 0) {
            CompoundNBT data = new CompoundNBT();
            data.putLong(MANA_TAG, this.storedMana);
            
            // 写入到#0标签（AE2标准数据槽）
            this.tagCompound.put(ITEM_SLOT_0, data);
            // 写入数量到@0标签
            this.tagCompound.putLong(ITEM_COUNT_0, this.storedMana);
            // 写入类型计数（1种类型：mana）
            this.tagCompound.putShort(ITEM_TYPE_TAG, (short) 1);
            // 更新总物品计数
            this.tagCompound.putLong(ITEM_COUNT_TAG, this.storedMana);
        } else {
            // 清空时移除所有相关标签
            this.tagCompound.remove(ITEM_SLOT_0);
            this.tagCompound.remove(ITEM_COUNT_0);
            this.tagCompound.remove(ITEM_TYPE_TAG);
            this.tagCompound.remove(ITEM_COUNT_TAG);
        }
        
        this.isPersisted = true;
    }

    @Override
    public ManaStack injectItems(ManaStack input, Actionable mode, IActionSource src) {
        if (input == null) {
            return null;
        }

        long maxMana = (long) this.totalBytes * ManaStorageChannel.INSTANCE.getUnitsPerByte();
        long canInsert = Math.min(maxMana - this.storedMana, input.getStackSize());

        if (mode == Actionable.MODULATE) {
            this.storedMana += canInsert;
            this.saveChanges();
        }

        if (canInsert >= input.getStackSize()) {
            return null;
        }

        ManaStack result = input.copy();
        result.setStackSize(input.getStackSize() - canInsert);
        return result;
    }

    @Override
    public ManaStack extractItems(ManaStack request, Actionable mode, IActionSource src) {
        if (request == null) {
            return null;
        }

        long canExtract = Math.min(this.storedMana, request.getStackSize());

        if (mode == Actionable.MODULATE) {
            this.storedMana -= canExtract;
            this.saveChanges();
        }

        if (canExtract <= 0) {
            return null;
        }

        ManaStack result = new ManaStack(canExtract);
        return result;
    }

    @Override
    public IItemList<ManaStack> getAvailableItems(IItemList<ManaStack> out) {
        if (this.storedMana > 0) {
            out.add(new ManaStack(this.storedMana));
        }
        return out;
    }

    @Override
    public ManaStorageChannel getChannel() {
        return ManaStorageChannel.INSTANCE;
    }

    private void saveChanges() {
        this.isPersisted = false;
        if (this.saveProvider != null) {
            this.saveProvider.saveChanges(this);
        } else {
            this.persist();
        }
    }
}
