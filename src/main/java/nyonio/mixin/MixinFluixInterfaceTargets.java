package nyonio.mixin;

import nyonio.FluixPoolManaHelper;
import nyonio.IFluixManaReceiver;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {
        appeng.tile.networking.CableBusTileEntity.class,
        appeng.tile.misc.InterfaceTileEntity.class
}, remap = false)
public abstract class MixinFluixInterfaceTargets implements IFluixManaReceiver {
    @Override
    public boolean hasFluixPoolCard() {
        return FluixPoolManaHelper.hasCard(this);
    }

    @Override
    public Object getFluixManaTarget() {
        if ((Object) this instanceof appeng.tile.networking.CableBusTileEntity) {
            appeng.api.parts.IPart part = nyonio.FluixPoolManaHelper.findCardPart((appeng.tile.networking.CableBusTileEntity) (Object) this);
            return part == null ? this : part;
        }
        return this;
    }
}
