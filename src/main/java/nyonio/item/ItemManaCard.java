package nyonio.item;

import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
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
import nyonio.BotaniaApplie;

import javax.annotation.Nullable;
import java.util.List;

public class ItemManaCard extends Item implements IUpgradeModule {

    private final int manaPerSecond;

    public ItemManaCard(int manaPerSecond) {
        this.setMaxStackSize(1);
        this.manaPerSecond = manaPerSecond;
    }

    public int getManaPerSecond() {
        return this.manaPerSecond;
    }

    public int getManaPerTick() {
        return this.manaPerSecond / 20;
    }

    @Override
    public Upgrades getType(ItemStack stack) {
        return Upgrades.CAPACITY;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(I18n.translateToLocalFormatted("botania_applie.tooltip.mana_card_rate", this.manaPerSecond));
    }

    public static int getTotalManaRate(ItemStack cellStack) {
        int totalRate = 0;
        if (cellStack == null || cellStack.isEmpty()) return 0;

        NBTTagCompound tag = Platform.openNbtData(cellStack);
        if (tag == null) return 0;

        if (tag.hasKey("upgrades")) {
            NBTTagCompound upgradesTag = tag.getCompoundTag("upgrades");
            NBTTagList items = upgradesTag.getTagList("Items", 10);
            for (int i = 0; i < items.tagCount(); i++) {
                NBTTagCompound itemNbt = items.getCompoundTagAt(i);
                String id = itemNbt.getString("id");
                if (id.equals(BotaniaApplie.MODID + ":mana_card_basic")) {
                    totalRate += 40;
                } else if (id.equals(BotaniaApplie.MODID + ":mana_card_advanced")) {
                    totalRate += 200;
                }
            }
        }

        if (tag.hasKey("manaCards")) {
            NBTTagList cards = tag.getTagList("manaCards", 10);
            for (int i = 0; i < cards.tagCount(); i++) {
                NBTTagCompound cardNbt = cards.getCompoundTagAt(i);
                String id = cardNbt.getString("id");
                if (id.equals(BotaniaApplie.MODID + ":mana_card_basic")) {
                    totalRate += 40;
                } else if (id.equals(BotaniaApplie.MODID + ":mana_card_advanced")) {
                    totalRate += 200;
                }
            }
        }

        return totalRate;
    }
}
