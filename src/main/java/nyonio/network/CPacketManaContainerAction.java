package nyonio.network;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;
import appeng.me.helpers.PlayerSource;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import nyonio.ae2.ManaStack;
import nyonio.ae2.ManaStorageChannel;
import vazkii.botania.api.mana.IManaItem;

import java.io.IOException;

public class CPacketManaContainerAction implements IMessage {

    private long manaAmount;
    private boolean extractMode;

    public CPacketManaContainerAction() {
    }

    public CPacketManaContainerAction(long manaAmount, boolean extractMode) {
        this.manaAmount = manaAmount;
        this.extractMode = extractMode;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.manaAmount = buf.readLong();
        this.extractMode = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.manaAmount);
        buf.writeBoolean(this.extractMode);
    }

    public long getManaAmount() {
        return this.manaAmount;
    }

    public boolean isExtractMode() {
        return this.extractMode;
    }

    public static class Handler implements IMessageHandler<CPacketManaContainerAction, IMessage> {

        @Override
        public IMessage onMessage(CPacketManaContainerAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack heldItem = player.inventory.getItemStack();
                if (!heldItem.isEmpty() && heldItem.getItem() instanceof IManaItem) {
                    IManaItem manaItem = (IManaItem) heldItem.getItem();
                    
                    if (player.openContainer instanceof ContainerMEMonitorable) {
                        ContainerMEMonitorable container = (ContainerMEMonitorable) player.openContainer;
                        IStorageGrid grid = container.getNetworkNode().getGrid().getCache(IStorageGrid.class);
                        IActionSource source = new PlayerSource(player, (IActionHost) container.getTarget());
                        
                        if (grid != null) {
                            if (message.isExtractMode()) {
                                handleExtractMana(manaItem, heldItem, grid, source, message.getManaAmount(), player);
                            } else {
                                handleInjectMana(manaItem, heldItem, grid, source, player);
                            }
                        }
                    }
                }
            });
            return null;
        }
        
        private static void handleExtractMana(IManaItem manaItem, ItemStack heldItem, IStorageGrid grid, IActionSource source, long manaAmount, EntityPlayerMP player) {
            int currentMana = manaItem.getMana(heldItem);
            int maxMana = manaItem.getMaxMana(heldItem);
            int space = maxMana - currentMana;
            
            if (space > 0) {
                int manaToExtract = (int) Math.min(manaAmount, space);
                ManaStack request = new ManaStack(manaToExtract);
                ManaStack extracted = grid.getInventory(ManaStorageChannel.INSTANCE).extractItems(request, Actionable.MODULATE, source);
                
                if (extracted != null && extracted.getStackSize() > 0) {
                    manaItem.addMana(heldItem, (int) extracted.getStackSize());
                    updateHeld(player);
                }
            }
        }
        
        private static void handleInjectMana(IManaItem manaItem, ItemStack heldItem, IStorageGrid grid, IActionSource source, EntityPlayerMP player) {
            int currentMana = manaItem.getMana(heldItem);
            
            if (currentMana > 0) {
                ManaStack toInject = new ManaStack(currentMana);
                ManaStack leftover = grid.getInventory(ManaStorageChannel.INSTANCE).injectItems(toInject, Actionable.MODULATE, source);
                
                long injected = currentMana - (leftover != null ? leftover.getStackSize() : 0);
                
                if (injected > 0) {
                    manaItem.addMana(heldItem, (int) -injected);
                    updateHeld(player);
                }
            }
        }
        
        private static void updateHeld(EntityPlayerMP player) {
            if (Platform.isServer()) {
                try {
                    NetworkHandler.instance().sendTo(
                        new PacketInventoryAction(
                            InventoryAction.UPDATE_HAND, 
                            0, 
                            AEItemStack.fromItemStack(player.inventory.getItemStack())
                        ), 
                        player
                    );
                } catch (IOException e) {
                    AELog.debug(e);
                }
            }
        }
    }
}
