package nyonio.integration.terminal;

import appeng.api.storage.data.IAEItemStack;
import net.minecraft.item.ItemStack;
import nyonio.ae2.ManaStorageChannel;
import nyonio.item.ItemManaPacket;
import nyonio.terminal_interaction_integration.api.IPacketType;

public class ManaPacketType implements IPacketType {
    
    @Override
    public String getName() {
        return "mana";
    }
    
    @Override
    public String getDisplayName() {
        return "\u00a76Mana";
    }
    
    @Override
    public boolean isPacket(ItemStack stack) {
        return ItemManaPacket.isManaPacket(stack);
    }
    
    @Override
    public long getAmount(ItemStack stack) {
        return ItemManaPacket.getMana(stack);
    }
    
    @Override
    public IAEItemStack createAEStack(long amount) {
        return ItemManaPacket.createAE(amount);
    }
    
    @Override
    public ItemStack createItemStack(long amount) {
        return ItemManaPacket.create(amount);
    }
}
