package nyonio.integration.terminal;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.me.helpers.PlayerSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import nyonio.BotaniaApplie;
import nyonio.ae2.ManaStack;
import nyonio.ae2.ManaStorageChannel;
import nyonio.terminal_interaction_integration.api.IContainerHandler;
import vazkii.botania.api.mana.IManaItem;

public class ManaContainerHandler implements IContainerHandler {
    
    @Override
    public boolean canHandle(ItemStack container) {
        return !container.isEmpty() && container.getItem() instanceof IManaItem;
    }
    
    @Override
    public long getStoredAmount(ItemStack container) {
        if (container.isEmpty() || !(container.getItem() instanceof IManaItem)) {
            return 0;
        }
        IManaItem manaItem = (IManaItem) container.getItem();
        return manaItem.getMana(container);
    }
    
    @Override
    public long getMaxCapacity(ItemStack container) {
        if (container.isEmpty() || !(container.getItem() instanceof IManaItem)) {
            return 0;
        }
        IManaItem manaItem = (IManaItem) container.getItem();
        return manaItem.getMaxMana(container);
    }
    
    @Override
    public long extract(ItemStack container, long amount, IActionSource source) {
        if (container.isEmpty() || !(container.getItem() instanceof IManaItem)) {
            return 0;
        }
        
        IManaItem manaItem = (IManaItem) container.getItem();
        int currentMana = manaItem.getMana(container);
        
        if (currentMana <= 0) {
            return 0;
        }
        
        int toExtract = (int) Math.min(amount, currentMana);
        manaItem.addMana(container, -toExtract);
        
        return toExtract;
    }
    
    @Override
    public long inject(ItemStack container, long amount, IActionSource source) {
        if (container.isEmpty() || !(container.getItem() instanceof IManaItem)) {
            return 0;
        }
        
        IManaItem manaItem = (IManaItem) container.getItem();
        int currentMana = manaItem.getMana(container);
        int maxMana = manaItem.getMaxMana(container);
        int space = maxMana - currentMana;
        
        if (space <= 0) {
            return 0;
        }
        
        int toInject = (int) Math.min(amount, space);
        manaItem.addMana(container, toInject);
        
        return toInject;
    }
    
    @Override
    public String getContainerDisplayName(ItemStack container) {
        if (container.isEmpty()) {
            return "";
        }
        return container.getDisplayName();
    }
}
