package nyonio.item;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.ICellWorkbenchItem;
import appeng.items.contents.CellConfig;
import appeng.util.Platform;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.items.IItemHandler;
import nyonio.BotaniaApplie;
import nyonio.ae2.IManaStorageCell;
import nyonio.ae2.ManaCellUpgrades;

import javax.annotation.Nullable;
import java.util.List;

public class ItemManaStorageCell extends Item implements IManaStorageCell, ICellWorkbenchItem {

    private static final int MAX_CARDS = 5;

    private final int kilobytes;
    private final double idleDrain;

    public ItemManaStorageCell(int kilobytes, double idleDrain) {
        this.setMaxStackSize(1);
        this.kilobytes = kilobytes;
        this.idleDrain = idleDrain;
    }

    @Override
    public int getKilobytes() {
        return this.kilobytes;
    }

    @Override
    public double getIdleDrain() {
        return this.idleDrain;
    }

    @Override
    public boolean isEditable(ItemStack is) {
        return true;
    }

    @Override
    public IItemHandler getConfigInventory(ItemStack is) {
        return new CellConfig(is);
    }

    @Override
    public IItemHandler getUpgradesInventory(ItemStack is) {
        return new ManaCellUpgrades(is, MAX_CARDS);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack is) {
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {
    }

    public static int getInstalledCardCount(ItemStack cellStack) {
        if (cellStack == null || cellStack.isEmpty()) return 0;
        NBTTagCompound tag = Platform.openNbtData(cellStack);
        int count = 0;

        if (tag.hasKey("upgrades")) {
            NBTTagCompound upgradesTag = tag.getCompoundTag("upgrades");
            NBTTagList items = upgradesTag.getTagList("Items", 10);
            for (int i = 0; i < items.tagCount(); i++) {
                NBTTagCompound itemNbt = items.getCompoundTagAt(i);
                String id = itemNbt.getString("id");
                if (id.equals(BotaniaApplie.MODID + ":mana_card_basic") || id.equals(BotaniaApplie.MODID + ":mana_card_advanced")) {
                    count++;
                }
            }
        }

        if (tag.hasKey("manaCards")) {
            count += tag.getTagList("manaCards", 10).tagCount();
        }

        return count;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);

        try {
            long storedMana = getStoredManaFromNBT(stack);

            int totalBytes = this.kilobytes * 1024;
            int unitsPerByte = nyonio.ae2.ManaStorageChannel.INSTANCE.getUnitsPerByte();
            long usedBytes = (storedMana + unitsPerByte - 1) / unitsPerByte;

            tooltip.add(I18n.translateToLocalFormatted("botania_applie.tooltip.bytes_used", usedBytes, totalBytes));
            tooltip.add(I18n.translateToLocalFormatted("botania_applie.tooltip.stored_mana", storedMana));

            int manaRate = ItemManaCard.getTotalManaRate(stack);
            int cardCount = getInstalledCardCount(stack);
            if (cardCount > 0) {
                tooltip.add(I18n.translateToLocalFormatted("botania_applie.tooltip.mana_card_installed", cardCount, MAX_CARDS));
                tooltip.add(I18n.translateToLocalFormatted("botania_applie.tooltip.mana_card_gen", manaRate));
            }
        } catch (Exception e) {
        }
    }

    private long getStoredManaFromNBT(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return 0;
        }

        NBTTagCompound tag = stack.getTagCompound();

        if (tag.hasKey("#0") && tag.getCompoundTag("#0").hasKey("mana")) {
            return tag.getCompoundTag("#0").getLong("mana");
        }

        if (tag.hasKey("mana")) {
            return tag.getLong("mana");
        }

        for (String key : tag.getKeySet()) {
            if (tag.getTag(key) instanceof NBTTagCompound) {
                NBTTagCompound subTag = tag.getCompoundTag(key);
                if (subTag.hasKey("mana")) {
                    return subTag.getLong("mana");
                }
            }
        }

        return 0;
    }
}
