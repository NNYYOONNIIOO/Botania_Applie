package nyonio.coremod.mixin.ae2.gui;

import appeng.client.gui.AEBaseMEGui;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.me.SlotME;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import nyonio.BotaniaApplie;
import nyonio.item.ItemManaPacket;
import nyonio.network.CPacketManaContainerAction;
import nyonio.util.UtilClient;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import vazkii.botania.api.mana.IManaItem;

import java.util.Arrays;

@Mixin(value = GuiMEMonitorable.class, remap = false)
public abstract class MixinGuiMEMonitorable extends AEBaseMEGui {

    public MixinGuiMEMonitorable(Container container) {
        super(container);
    }

    @Intrinsic
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        Slot slot = this.getSlotUnderMouse();
        if (slot instanceof SlotME) {
            SlotME s = (SlotME) slot;
            ItemStack heldItem = UtilClient.getMouseItem();
            
            if (s.getAEStack() != null && s.getAEStack().getItem() instanceof ItemManaPacket) {
                if (!heldItem.isEmpty() && heldItem.getItem() instanceof IManaItem) {
                    IManaItem manaItem = (IManaItem) heldItem.getItem();
                    int currentMana = manaItem.getMana(heldItem);
                    
                    String manaName = I18n.format("botania_applie.mana");
                    String containerName = heldItem.getDisplayName();
                    String depositText = I18n.format("botania_applie.action.deposit");
                    String separator = " : ";
                    
                    String actionText;
                    if (currentMana > 0) {
                        actionText = I18n.format("botania_applie.action.fill");
                    } else {
                        actionText = I18n.format("botania_applie.action.extract");
                    }
                    
                    this.drawHoveringText(
                        Arrays.asList(
                            TextFormatting.DARK_GRAY + GameSettings.getKeyDisplayString(-100) + separator + TextFormatting.RESET + actionText + " " + manaName,
                            TextFormatting.DARK_GRAY + GameSettings.getKeyDisplayString(-99) + separator + TextFormatting.RESET + depositText + " " + containerName
                        ),
                        mouseX,
                        mouseY
                    );
                    return;
                }
            }
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    @Intrinsic
    protected void handleMouseClick(Slot slot, int slotIdx, int mouseButton, ClickType clickType) {
        if (slot instanceof SlotME) {
            SlotME s = (SlotME) slot;
            ItemStack heldItem = UtilClient.getMouseItem();
            
            if (s.getAEStack() != null && s.getAEStack().getItem() instanceof ItemManaPacket) {
                if (!heldItem.isEmpty() && heldItem.getItem() instanceof IManaItem) {
                    if (mouseButton == 0) {
                        IManaItem manaItem = (IManaItem) heldItem.getItem();
                        int currentMana = manaItem.getMana(heldItem);
                        
                        SimpleNetworkWrapper network = BotaniaApplie.getNetwork();
                        
                        if (currentMana > 0) {
                            network.sendToServer(new CPacketManaContainerAction(currentMana, false));
                        } else {
                            long manaAmount = s.getAEStack().getStackSize();
                            network.sendToServer(new CPacketManaContainerAction(manaAmount, true));
                        }
                        return;
                    }
                } else {
                    return;
                }
            }
        }
        super.handleMouseClick(slot, slotIdx, mouseButton, clickType);
    }
}
