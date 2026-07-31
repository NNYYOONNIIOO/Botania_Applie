package nyonio;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEInventory;
import appeng.fluids.helper.IFluidInterfaceHost;
import appeng.helpers.IInterfaceHost;
import appeng.me.helpers.MachineSource;
import appeng.tile.networking.TileCableBus;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import nyonio.item.ItemFluixPoolCard;

import java.lang.reflect.Method;

public final class FluixPoolManaHelper {
    private FluixPoolManaHelper() {
    }

    public static boolean hasCard(Object target) {
        return findCard(target) != null;
    }

    public static ItemStack findCard(Object target) {
        if (target instanceof IPart) return findCard((IPart) target);
        String name = target.getClass().getName();
        if (name.contains("TileDualInterface") || name.contains("TileTrioInterface")) {
            ItemStack card = findCardInInventory(getInventoryByName(target, "item_upgrades"));
            if (card != null) return card;
            card = findCardInInventory(getInventoryByName(target, "fluid_upgrades"));
            if (card != null) return card;
            return findCardInInventory(getInventoryByName(target, "gas_upgrades"));
        }
        if (name.contains("TileGasInterface")) return findCardInInventory(getInventoryByName(target, "upgrades"));
        return findCardInInventory(getUpgradeInventory(target));
    }

    private static ItemStack findCard(IPart part) {
        ItemStack card = findCardInInventory(getItemUpgradeInventory(part));
        if (card != null) return card;
        card = findCardInInventory(getFluidUpgradeInventory(part));
        if (card != null) return card;
        return findCardInInventory(getGasUpgradeInventory(part));
    }

    private static ItemStack findCardInInventory(IItemHandler upgrades) {
        if (upgrades == null) return null;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (ItemFluixPoolCard.isCard(stack)) return stack;
        }
        return null;
    }

    private static IItemHandler getInventoryByName(Object target, String name) {
        try {
            Method method = target.getClass().getMethod("getInventoryByName", String.class);
            Object result = method.invoke(target, name);
            return result instanceof IItemHandler ? (IItemHandler) result : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static IItemHandler getUpgradeInventory(Object target) {
        try {
            if (target instanceof TileCableBus) {
                IPart part = findCardPart((TileCableBus) target);
                if (part == null) return null;
                return getUpgradeInventory(part);
            }
            String targetName = target.getClass().getName();
            if (targetName.contains("TileDualInterface") || targetName.contains("TileTrioInterface")) {
                IItemHandler item = getInventoryByName(target, "item_upgrades");
                IItemHandler fluid = getInventoryByName(target, "fluid_upgrades");
                IItemHandler gas = getInventoryByName(target, "gas_upgrades");
                if (item instanceof IItemHandlerModifiable && fluid instanceof IItemHandlerModifiable) {
                    return gas instanceof IItemHandlerModifiable
                            ? new CombinedInvWrapper((IItemHandlerModifiable) item, (IItemHandlerModifiable) fluid, (IItemHandlerModifiable) gas)
                            : new CombinedInvWrapper((IItemHandlerModifiable) item, (IItemHandlerModifiable) fluid);
                }
                if (item != null) return item;
                if (fluid != null) return fluid;
                if (gas != null) return gas;
            }
            if (targetName.contains("TileGasInterface")) return getInventoryByName(target, "upgrades");
            if (target instanceof IInterfaceHost) {
                return ((IInterfaceHost) target).getInterfaceDuality().getInventoryByName("upgrades");
            }
            if (target instanceof IFluidInterfaceHost) {
                IItemHandler item = target instanceof IInterfaceHost ? ((IInterfaceHost) target).getInterfaceDuality().getInventoryByName("upgrades") : null;
                IItemHandler fluid = ((IFluidInterfaceHost) target).getDualityFluidInterface().getInventoryByName("upgrades");
                if (item instanceof IItemHandlerModifiable && fluid instanceof IItemHandlerModifiable) return new CombinedInvWrapper((IItemHandlerModifiable) item, (IItemHandlerModifiable) fluid);
                if (fluid != null) return fluid;
                if (item != null) return item;
            }
            Method method = target.getClass().getMethod("getInventoryByName", String.class);
            Object result = method.invoke(target, "upgrades");
            return result instanceof IItemHandler ? (IItemHandler) result : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static IPart findCardPart(TileCableBus cableBus) {
        for (EnumFacing facing : EnumFacing.values()) {
            IPart part = cableBus.getPart(facing);
            if (part != null && getUpgradeInventory(part) != null && findCard(part) != null) return part;
        }
        return null;
    }

    private static IItemHandler getItemUpgradeInventory(IPart part) {
        String className = part.getClass().getName();
        if (className.contains("PartDualInterface") || className.contains("PartTrioInterface")) {
            return getInventoryByName(part, "item_upgrades");
        }
        try {
            if (part instanceof IInterfaceHost) return ((IInterfaceHost) part).getInterfaceDuality().getInventoryByName("upgrades");
        } catch (Exception ignored) {
        }
        return null;
    }

    private static IItemHandler getFluidUpgradeInventory(IPart part) {
        String className = part.getClass().getName();
        if (className.contains("PartDualInterface") || className.contains("PartTrioInterface")) {
            return getInventoryByName(part, "fluid_upgrades");
        }
        try {
            if (part instanceof IFluidInterfaceHost) return ((IFluidInterfaceHost) part).getDualityFluidInterface().getInventoryByName("upgrades");
        } catch (Exception ignored) {
        }
        return null;
    }

    private static IItemHandler getGasUpgradeInventory(IPart part) {
        String className = part.getClass().getName();
        if (className.contains("PartTrioInterface")) return getInventoryByName(part, "gas_upgrades");
        if (className.contains("PartGasInterface")) return getInventoryByName(part, "upgrades");
        return null;
    }

    public static IItemHandler getUpgradeInventory(IPart part) {
        try {
            IItemHandler item = getItemUpgradeInventory(part);
            IItemHandler fluid = getFluidUpgradeInventory(part);
            IItemHandler gas = getGasUpgradeInventory(part);
            if (item instanceof IItemHandlerModifiable && fluid instanceof IItemHandlerModifiable) {
                return gas instanceof IItemHandlerModifiable
                        ? new CombinedInvWrapper((IItemHandlerModifiable) item, (IItemHandlerModifiable) fluid, (IItemHandlerModifiable) gas)
                        : new CombinedInvWrapper((IItemHandlerModifiable) item, (IItemHandlerModifiable) fluid);
            }
            if (item != null) return item;
            if (fluid != null) return fluid;
            if (gas != null) return gas;
            Method method = part.getClass().getMethod("getInventoryByName", String.class);
            Object result = method.invoke(part, "upgrades");
            return result instanceof IItemHandler ? (IItemHandler) result : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static IActionHost getActionHost(Object target) {
        if (target instanceof TileCableBus) {
            IPart part = findCardPart((TileCableBus) target);
            return part instanceof IActionHost ? (IActionHost) part : null;
        }
        return target instanceof IActionHost ? (IActionHost) target : null;
    }

    private static IStorageGrid getStorage(Object target) {
        IActionHost host = getActionHost(target);
        if (host == null) return null;
        try {
            IGridNode node = host.getActionableNode();
            if (node == null || !node.isActive() || node.getGrid() == null) return null;
            IGrid grid = node.getGrid();
            return grid.getCache(IStorageGrid.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static IMEInventory<nyonio.ae2.ManaStack> getInventory(Object target) {
        IStorageGrid storage = getStorage(target);
        return storage == null ? null : storage.getInventory(nyonio.ae2.ManaStorageChannel.INSTANCE);
    }

    private static MachineSource getSource(Object target) {
        IActionHost host = getActionHost(target);
        return host == null ? null : new MachineSource(host);
    }

    public static long getMana(Object target) {
        try {
            IMEInventory<nyonio.ae2.ManaStack> inventory = getInventory(target);
            MachineSource source = getSource(target);
            if (inventory == null || source == null) return 0;
            nyonio.ae2.ManaStack stack = inventory.extractItems(new nyonio.ae2.ManaStack(Integer.MAX_VALUE), Actionable.SIMULATE, source);
            return stack == null ? 0 : stack.getStackSize();
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static long getSpace(Object target) {
        try {
            IMEInventory<nyonio.ae2.ManaStack> inventory = getInventory(target);
            MachineSource source = getSource(target);
            if (inventory == null || source == null) return 0;
            long request = Integer.MAX_VALUE;
            nyonio.ae2.ManaStack remaining = inventory.injectItems(new nyonio.ae2.ManaStack(request), Actionable.SIMULATE, source);
            return Math.max(0, request - (remaining == null ? 0 : remaining.getStackSize()));
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static int insert(Object target, int amount) {
        if (amount <= 0) return 0;
        try {
            IMEInventory<nyonio.ae2.ManaStack> inventory = getInventory(target);
            MachineSource source = getSource(target);
            if (inventory == null || source == null) return 0;
            nyonio.ae2.ManaStack remaining = inventory.injectItems(new nyonio.ae2.ManaStack(amount), Actionable.MODULATE, source);
            long rejected = remaining == null ? 0 : remaining.getStackSize();
            return (int) Math.max(0, amount - Math.min(amount, rejected));
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static int extract(Object target, int amount) {
        if (amount <= 0) return 0;
        try {
            IMEInventory<nyonio.ae2.ManaStack> inventory = getInventory(target);
            MachineSource source = getSource(target);
            if (inventory == null || source == null) return 0;
            nyonio.ae2.ManaStack extracted = inventory.extractItems(new nyonio.ae2.ManaStack(amount), Actionable.MODULATE, source);
            return extracted == null ? 0 : (int) Math.min(amount, extracted.getStackSize());
        } catch (Exception ignored) {
            return 0;
        }
    }
}
