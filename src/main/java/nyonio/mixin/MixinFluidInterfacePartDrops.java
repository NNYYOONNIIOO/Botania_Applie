package nyonio.mixin;

import appeng.fluids.helper.IFluidInterfaceHost;
import appeng.fluids.parts.FluidInterfacePart;
import appeng.parts.AEBasePart;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = FluidInterfacePart.class, remap = false)
public abstract class MixinFluidInterfacePartDrops extends AEBasePart {
    protected MixinFluidInterfacePartDrops(ItemStack stack) { super(stack); }

    @Override
    public void getDrops(List<ItemStack> drops, boolean wrenched) {
        super.getDrops(drops, wrenched);
        IItemHandler cards = ((IFluidInterfaceHost) this).getInventoryByName("upgrades");
        if (cards != null) {
            for (int slot = 0; slot < cards.getSlots(); slot++) {
                ItemStack card = cards.getStackInSlot(slot);
                if (!card.isEmpty()) drops.add(card.copy());
            }
        }
    }
}
