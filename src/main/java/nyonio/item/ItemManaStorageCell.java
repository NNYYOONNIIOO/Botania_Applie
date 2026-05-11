package nyonio.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import nyonio.ae2.IManaStorageCell;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemManaStorageCell extends Item implements IManaStorageCell {

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
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        
        try {
            long storedMana = getStoredManaFromNBT(stack);
            
            int totalBytes = this.kilobytes * 1024;
            int unitsPerByte = nyonio.ae2.ManaStorageChannel.INSTANCE.getUnitsPerByte();
            long usedBytes = (storedMana + unitsPerByte - 1) / unitsPerByte;
            
            tooltip.add(net.minecraft.client.resources.I18n.format("botania_applie.tooltip.bytes_used", usedBytes, totalBytes));
            tooltip.add(net.minecraft.client.resources.I18n.format("botania_applie.tooltip.stored_mana", storedMana));
        } catch (Exception e) {
            tooltip.add("DEBUG: Exception - " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
    
    private long getStoredManaFromNBT(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return 0;
        }
        
        net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
        
        // Botania Applie格式：mana_data子标签中存储实际魔力数据
        if (tag.hasKey("mana_data") && tag.getCompoundTag("mana_data").hasKey("mana")) {
            return tag.getCompoundTag("mana_data").getLong("mana");
        }
        
        // AE2标准格式：#0子标签中存储实际数据
        if (tag.hasKey("#0") && tag.getCompoundTag("#0").hasKey("mana")) {
            return tag.getCompoundTag("#0").getLong("mana");
        }
        
        // 兼容旧格式：根级别
        if (tag.hasKey("mana")) {
            return tag.getLong("mana");
        }
        
        // 搜索所有子标签
        for (String key : tag.getKeySet()) {
            if (tag.getTag(key) instanceof net.minecraft.nbt.NBTTagCompound) {
                net.minecraft.nbt.NBTTagCompound subTag = tag.getCompoundTag(key);
                if (subTag.hasKey("mana")) {
                    return subTag.getLong("mana");
                }
            }
        }
        
        return 0;
    }
}
