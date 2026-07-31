package nyonio.mixin;

import nyonio.FluixPoolManaHelper;
import nyonio.IFluixManaReceiver;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = {
        "appeng.tile.networking.TileCableBus",
        "appeng.tile.misc.TileInterface",
        "appeng.fluids.tile.TileFluidInterface"
}, remap = false)
public abstract class MixinFluixInterfaceTargets implements IFluixManaReceiver {
    @Override
    public boolean hasFluixPoolCard() {
        return FluixPoolManaHelper.hasCard(this);
    }

    @Override
    public Object getFluixManaTarget() {
        if ((Object) this instanceof appeng.tile.networking.TileCableBus) {
            appeng.api.parts.IPart part = nyonio.FluixPoolManaHelper.findCardPart((appeng.tile.networking.TileCableBus) (Object) this);
            return part == null ? this : part;
        }
        return this;
    }
}
