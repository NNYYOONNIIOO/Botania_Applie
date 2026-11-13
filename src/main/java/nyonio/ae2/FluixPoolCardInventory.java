package nyonio.ae2;

import appeng.fluids.helper.IFluidInterfaceHost;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import nyonio.item.ItemFluixPoolCard;

/** Kept outside the mixin so Mixin never generates a class in AE2's signed package. */
public final class FluixPoolCardInventory extends ItemStackHandler {
    private final IFluidInterfaceHost host;

    public FluixPoolCardInventory(IFluidInterfaceHost host) {
        super(1);
        this.host = host;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return ItemFluixPoolCard.isCard(stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (host != null) {
            host.saveChanges();
        }
    }
}
