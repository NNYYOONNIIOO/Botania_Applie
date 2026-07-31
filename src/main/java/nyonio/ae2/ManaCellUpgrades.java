package nyonio.ae2;

import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.inv.filter.IAEItemFilter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;
import nyonio.item.ItemManaCard;

public class ManaCellUpgrades extends AppEngInternalInventory {

    private final ItemStack is;
    private boolean loading = false;

    public ManaCellUpgrades(final ItemStack is, final int upgrades) {
        super(null, upgrades, 1);
        this.is = is;
        this.setFilter(new ManaCardUpgradeFilter());
        this.readFromNBT(Platform.openNbtData(is), "upgrades");
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (!this.loading) {
            this.writeToNBT(Platform.openNbtData(this.is), "upgrades");
        }
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        this.loading = true;
        try {
            NBTTagList tagList = data.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound itemTags = tagList.getCompoundTagAt(i);
                int slot = itemTags.getInteger("Slot");
                if (slot >= 0 && slot < this.getSlots()) {
                    this.setStackInSlot(slot, new ItemStack(itemTags));
                }
            }
        } finally {
            this.loading = false;
        }
    }

    private class ManaCardUpgradeFilter implements IAEItemFilter {

        @Override
        public boolean allowExtract(IItemHandler inv, int slot, int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(IItemHandler inv, int slot, ItemStack itemstack) {
            if (itemstack.isEmpty()) {
                return false;
            }
            return itemstack.getItem() instanceof ItemManaCard;
        }
    }
}
