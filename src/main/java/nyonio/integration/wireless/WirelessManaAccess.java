package nyonio.integration.wireless;

import appeng.core.Api;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.features.IWirelessTermRegistry;
import appeng.helpers.WirelessTerminalGuiObject;
import net.minecraft.entity.player.PlayerEntity;
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

    public static boolean hasWirelessTerminal(PlayerEntity player) {
        return !findTerminals(player).isEmpty();
    }

    public static long getAvailableMana(PlayerEntity player) {
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

    public static int extractMana(PlayerEntity player, int amount) {
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

    private static List<TerminalReference> findTerminals(PlayerEntity player) {
        List<TerminalReference> terminals = new ArrayList<>();
        if (player == null) {
            return terminals;
        }

        IWirelessTermRegistry registry;
        try {
            registry = Api.instance().registries().wireless();
        } catch (Throwable ignored) {
            return terminals;
        }

        for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
            addTerminal(registry, terminals, player.inventory.getStackInSlot(slot), slot);
        }

        try {
            IItemHandler curios = top.theillusivec4.curios.api.CuriosApi.getCuriosHelper().getEquippedCurios(player).orElse(null);
            if (curios != null) {
                for (int slot = 0; slot < curios.getSlots(); slot++) {
                    addTerminal(registry, terminals, curios.getStackInSlot(slot), slot);
                }
            }
        } catch (Throwable ignored) {
            // Curios is optional; the main inventory remains usable without it.
        }

        return terminals;
    }

    private static void addTerminal(IWirelessTermRegistry registry, List<TerminalReference> terminals,
                                    ItemStack stack, int slot) {
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
                terminals.add(new TerminalReference(stack, slot, handler, encryptionKey));
            }
        } catch (Throwable ignored) {
            // A foreign wireless handler must not break Botania's mana lookup.
        }
    }

    private static WirelessTerminalGuiObject createAccess(TerminalReference terminal, PlayerEntity player) {
        try {
            return new WirelessTerminalGuiObject(terminal.handler, terminal.stack, player, terminal.slot);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasPower(TerminalReference terminal, PlayerEntity player) {
        try {
            return terminal.handler.hasPower(player, TERMINAL_POWER_PER_REQUEST, terminal.stack);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean usePower(TerminalReference terminal, PlayerEntity player) {
        try {
            return terminal.handler.usePower(player, TERMINAL_POWER_PER_REQUEST, terminal.stack);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class TerminalReference {
        private final ItemStack stack;
        private final int slot;
        private final IWirelessTermHandler handler;
        private final String encryptionKey;

        private TerminalReference(ItemStack stack, int slot,
                                  IWirelessTermHandler handler, String encryptionKey) {
            this.stack = stack;
            this.slot = slot;
            this.handler = handler;
            this.encryptionKey = encryptionKey;
        }
    }
}
