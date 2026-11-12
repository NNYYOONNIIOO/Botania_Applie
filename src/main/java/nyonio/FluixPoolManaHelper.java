package nyonio;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.*;
import appeng.api.storage.IMEInventory;
import appeng.api.util.AEPartLocation;
import appeng.helpers.IInterfaceHost;
import appeng.fluids.helper.IFluidInterfaceHost;
import appeng.me.helpers.MachineSource;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import nyonio.ae2.*;
import nyonio.item.ItemFluixPoolCard;
import appeng.tile.networking.CableBusTileEntity;
/** Public AE2 host APIs; no reflective inventory lookup on hot paths. */
public final class FluixPoolManaHelper {
    private FluixPoolManaHelper() {}
    public static boolean hasCard(Object host) { return findCard(host)!=null; }
    public static ItemStack findCard(Object host) {
        if(host instanceof CableBusTileEntity) { IPart part=findCardPart((CableBusTileEntity)host);return part==null?null:findCard(part); }
        IItemHandler inv=getUpgradeInventory(host);
        if(inv!=null)for(int i=0;i<inv.getSlots();i++)if(ItemFluixPoolCard.isCard(inv.getStackInSlot(i)))return inv.getStackInSlot(i);
        return null;
    }
    public static IItemHandler getUpgradeInventory(Object host) {
        if(host instanceof IInterfaceHost)return ((IInterfaceHost)host).getInterfaceDuality().getInventoryByName("upgrades");
        if(host instanceof IFluidInterfaceHost)return ((IFluidInterfaceHost)host).getDualityFluidInterface().getInventoryByName("upgrades");
        return null;
    }
    public static IItemHandler getUpgradeInventory(IPart host) { return getUpgradeInventory((Object)host); }
    public static IPart findCardPart(CableBusTileEntity host) {
        for(AEPartLocation side:AEPartLocation.values()) {IPart part=host.getPart(side);if(part!=null && hasCard(part))return part;}
        return null;
    }
    public static IActionHost getActionHost(Object host) {
        if(host instanceof CableBusTileEntity) { IPart part=findCardPart((CableBusTileEntity)host);return part instanceof IActionHost?(IActionHost)part:null; }
        return host instanceof IActionHost?(IActionHost)host:null;
    }
    private static IMEInventory<ManaStack> inventory(Object target) {
        IActionHost host=getActionHost(target);if(host==null)return null;
        IGridNode node=host.getActionableNode();
        if(node==null||!node.isActive())return null;
        IStorageGrid grid=node.getGrid().getCache(IStorageGrid.class);
        return grid==null?null:grid.getInventory(ManaStorageChannel.INSTANCE);
    }
    public static long getMana(Object host) {
        IMEInventory<ManaStack> inv=inventory(host);if(inv==null)return 0;
        ManaStack result=inv.extractItems(new ManaStack(Integer.MAX_VALUE),Actionable.SIMULATE,new MachineSource(getActionHost(host)));
        return result==null?0:result.getStackSize();
    }
    public static long getSpace(Object host) {
        IMEInventory<ManaStack> inv=inventory(host);if(inv==null)return 0;
        ManaStack result=inv.injectItems(new ManaStack(Integer.MAX_VALUE),Actionable.SIMULATE,new MachineSource(getActionHost(host)));
        return Integer.MAX_VALUE-(result==null?0:result.getStackSize());
    }
    public static int insert(Object host,int amount) {
        if(amount<=0)return 0;IMEInventory<ManaStack> inv=inventory(host);if(inv==null)return 0;
        ManaStack result=inv.injectItems(new ManaStack(amount),Actionable.MODULATE,new MachineSource(getActionHost(host)));
        return (int)Math.max(0,amount-(result==null?0:result.getStackSize()));
    }
    public static int extract(Object host,int amount) {
        if(amount<=0)return 0;IMEInventory<ManaStack> inv=inventory(host);if(inv==null)return 0;
        ManaStack result=inv.extractItems(new ManaStack(amount),Actionable.MODULATE,new MachineSource(getActionHost(host)));
        return result==null?0:(int)Math.min(amount,result.getStackSize());
    }
}
