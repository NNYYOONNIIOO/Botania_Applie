package nyonio.integration.wireless;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.features.IWirelessTermRegistry;
import appeng.helpers.WirelessTerminalGuiObject;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import vazkii.botania.api.BotaniaAPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nyonio.FluixPoolManaHelper;

/**
 * Reads and extracts mana through every AE2 wireless terminal carried by a player.
 */
public final class WirelessManaAccess {

    private static final double TERMINAL_POWER_PER_REQUEST = 0.5D;

    private WirelessManaAccess() {
    }

    public static boolean hasWirelessTerminal(EntityPlayer player) {
        return !findTerminals(player).isEmpty();
    }

    public static long getAvailableMana(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) {
            return 0;
        }

        long available = 0;
        Set<String> visitedNetworks = new HashSet<>();
        for (TerminalReference terminal : findTerminals(player)) {
            if (visitedNetworks.contains(terminal.encryptionKey)) {
                continue;
            }

            WirelessTerminalGuiObject access = createAccess(terminal, player);
            if (access == null || !hasPower(terminal, player)) {
                continue;
            }

            visitedNetworks.add(terminal.encryptionKey);
            available += FluixPoolManaHelper.getMana(access);
            if (available >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return available;
    }

    public static int extractMana(EntityPlayer player, int amount) {
        if (player == null || player.world == null || player.world.isRemote || amount <= 0) {
            return 0;
        }

        int extracted = 0;
        Set<String> visitedNetworks = new HashSet<>();
        for (TerminalReference terminal : findTerminals(player)) {
            if (visitedNetworks.contains(terminal.encryptionKey)) {
                continue;
            }

            WirelessTerminalGuiObject access = createAccess(terminal, player);
            if (access == null || !hasPower(terminal, player)) {
                continue;
            }

            long available = FluixPoolManaHelper.getMana(access);
            visitedNetworks.add(terminal.encryptionKey);
            if (available <= 0) {
                continue;
            }

            int request = (int) Math.min((long) (amount - extracted), available);
            if (request <= 0 || !usePower(terminal, player)) {
                continue;
            }

            extracted += FluixPoolManaHelper.extract(access, request);
            if (extracted >= amount) {
                return amount;
            }
        }
        return extracted;
    }

    private static List<TerminalReference> findTerminals(EntityPlayer player) {
        List<TerminalReference> terminals = new ArrayList<>();
        if (player == null) {
            return terminals;
        }

        IWirelessTermRegistry registry;
        try {
            registry = AEApi.instance().registries().wireless();
        } catch (Throwable ignored) {
            return terminals;
        }

        for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
            addTerminal(registry, terminals, player.inventory.getStackInSlot(slot), slot, false);
        }

        try {
            IItemHandler baubles = BotaniaAPI.internalHandler.getBaublesInventoryWrapped(player);
            if (baubles != null) {
                for (int slot = 0; slot < baubles.getSlots(); slot++) {
                    addTerminal(registry, terminals, baubles.getStackInSlot(slot), slot, true);
                }
            }
        } catch (Throwable ignored) {
            // Baubles is optional; the main inventory remains usable without it.
        }

        return terminals;
    }

    private static void addTerminal(IWirelessTermRegistry registry, List<TerminalReference> terminals,
                                    ItemStack stack, int slot, boolean bauble) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        try {
            IWirelessTermHandler handler = registry.getWirelessTerminalHandler(stack);
            if (handler == null) {
                return;
            }

            String encryptionKey = handler.getEncryptionKey(stack);
            if (encryptionKey != null && !encryptionKey.isEmpty()) {
                terminals.add(new TerminalReference(stack, slot, bauble, handler, encryptionKey));
            }
        } catch (Throwable ignored) {
            // A foreign wireless handler must not break Botania's mana lookup.
        }
    }

    private static WirelessTerminalGuiObject createAccess(TerminalReference terminal, EntityPlayer player) {
        try {
            return new WirelessTerminalGuiObject(terminal.handler, terminal.stack, player, player.world,
                    terminal.slot, terminal.bauble ? 1 : 0, Integer.MIN_VALUE);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasPower(TerminalReference terminal, EntityPlayer player) {
        try {
            return terminal.handler.hasPower(player, TERMINAL_POWER_PER_REQUEST, terminal.stack);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean usePower(TerminalReference terminal, EntityPlayer player) {
        try {
            return terminal.handler.usePower(player, TERMINAL_POWER_PER_REQUEST, terminal.stack);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class TerminalReference {
        private final ItemStack stack;
        private final int slot;
        private final boolean bauble;
        private final IWirelessTermHandler handler;
        private final String encryptionKey;

        private TerminalReference(ItemStack stack, int slot, boolean bauble,
                                  IWirelessTermHandler handler, String encryptionKey) {
            this.stack = stack;
            this.slot = slot;
            this.bauble = bauble;
            this.handler = handler;
            this.encryptionKey = encryptionKey;
        }
    }
}
