package nyonio.mixin;

import nyonio.FluixPoolManaHelper;
import nyonio.IFluixManaReceiver;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = {
        "com.glodblock.github.common.part.PartDualInterface",
        "com.glodblock.github.common.tile.TileDualInterface"
}, remap = false)
public abstract class MixinFluixInterfaceTargetsAE2FC implements IFluixManaReceiver {
    @Override
    public boolean hasFluixPoolCard() {
        return FluixPoolManaHelper.hasCard(this);
    }

    @Override
    public Object getFluixManaTarget() {
        return this;
    }
}
