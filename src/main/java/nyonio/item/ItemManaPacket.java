package nyonio.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import nyonio.BotaniaApplie;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemManaPacket extends Item {

    public static final String TAG_MANA = "mana";
    public static final String TAG_DISPLAY_ONLY = "DisplayOnly";

    private static ItemStack BASE_PACKET = null; // 统一的基础物品模板

    public ItemManaPacket() {
        this.setMaxStackSize(1);
        this.setRegistryName(BotaniaApplie.MODID, "mana_packet");
        this.setUnlocalizedName("botania_applie.mana_packet");
        this.setCreativeTab(null);
    }

    @Override
    public void getSubItems(@Nonnull CreativeTabs tab, net.minecraft.util.NonNullList<ItemStack> items) {
    }

    @Override
    protected boolean isInCreativeTab(CreativeTabs targetTab) {
        return false;
    }

    @Override
    @Nonnull
    public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        if (isDisplayOnly(stack)) {
            return getManaName();
        }
        long mana = getMana(stack);
        return String.format("%s (%,d)", getManaName(), mana);
    }

    @Override
    public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (!isDisplayOnly(stack)) {
            tooltip.add("\u00a77Hold Shift for details");
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                long mana = getMana(stack);
                tooltip.add(String.format("\u00a7bStored Mana: \u00a7f%,d", mana));
            }
        }
    }

    /**
     * 获取或创建统一的基础物品模板
     * 所有mana packet都应该基于这个模板创建，确保可以正确堆叠
     */
    private static synchronized ItemStack getBasePacket() {
        if (BASE_PACKET == null || BASE_PACKET.isEmpty()) {
            BASE_PACKET = new ItemStack(BotaniaApplie.manaPacket);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong(TAG_MANA, 0);  // 固定值0
            tag.setBoolean(TAG_DISPLAY_ONLY, true);
            BASE_PACKET.setTagCompound(tag);
        }
        return BASE_PACKET.copy(); // 返回副本以避免修改原始对象
    }

    /**
     * 创建用于显示的ItemStack（包含具体魔力量）
     * 仅用于tooltip等显示用途
     */
    public static ItemStack create(long mana) {
        ItemStack packet = getBasePacket();
        // 更新NBT中的魔力量用于显示
        if (packet.hasTagCompound()) {
            packet.getTagCompound().setLong(TAG_MANA, mana);
        }
        return packet;
    }

    /**
     * 创建用于AE2终端显示的IAEItemStack
     * 关键：所有调用都返回相同类型的栈，只是数量不同
     */
    public static appeng.api.storage.data.IAEItemStack createAE(long mana) {
        try {
            // 使用统一的基础物品
            appeng.api.storage.data.IAEItemStack base = appeng.util.item.AEItemStack.fromItemStack(getBasePacket());
            
            if (base != null && mana > 0) {
                // 设置数量为魔力量（这是关键！）
                base.setStackSize(mana);
            }
            
            return base;
        } catch (Exception e) {
            BotaniaApplie.getLogger().error("Failed to create AE item stack for mana packet", e);
            return null;
        }
    }

    public static boolean isManaPacket(ItemStack is) {
        return !is.isEmpty() && is.getItem() instanceof ItemManaPacket;
    }

    public static long getMana(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return 0;
        }
        return stack.getTagCompound().getLong(TAG_MANA);
    }

    public static boolean isDisplayOnly(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return false;
        }
        return stack.getTagCompound().getBoolean(TAG_DISPLAY_ONLY);
    }

    private String getManaName() {
        return "\u00a76" + I18n.format("botania_applie.mana_packet.display_name");
    }
}