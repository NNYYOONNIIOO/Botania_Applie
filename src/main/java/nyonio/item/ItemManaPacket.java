package nyonio.item;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import nyonio.BotaniaApplie;
import java.util.List;
public class ItemManaPacket extends Item {
    public static final String TAG_MANA="mana";
    public static final String TAG_DISPLAY_ONLY="DisplayOnly";
    public ItemManaPacket() { super(new Item.Properties().maxStackSize(1)); }
    @Override public ITextComponent getDisplayName(ItemStack stack) { return new TranslationTextComponent("botania_applie.mana_packet.display_name").mergeStyle(TextFormatting.GOLD); }
    @Override public void addInformation(ItemStack stack,World world,List<ITextComponent> lines,ITooltipFlag flag) {
        // AE2 displays the network quantity separately; this item only represents its identity.
        if (!isDisplayOnly(stack)) {
            lines.add(new TranslationTextComponent("botania_applie.tooltip.stored_mana",ManaNumberFormat.format(getMana(stack))));
        }
    }
    public static ItemStack create(long amount) { ItemStack stack=new ItemStack(BotaniaApplie.manaPacket);CompoundNBT tag=stack.getOrCreateTag();tag.putLong(TAG_MANA,amount);tag.putBoolean(TAG_DISPLAY_ONLY,true);return stack; }
    public static IAEItemStack createAE(long amount) { IAEItemStack stack=AEItemStack.fromItemStack(create(0));if(stack!=null)stack.setStackSize(amount);return stack; }
    public static boolean isManaPacket(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof ItemManaPacket; }
    public static long getMana(ItemStack stack) { return stack.hasTag()?stack.getTag().getLong(TAG_MANA):0; }
    public static boolean isDisplayOnly(ItemStack stack) { return stack.hasTag()&&stack.getTag().getBoolean(TAG_DISPLAY_ONLY); }
}
