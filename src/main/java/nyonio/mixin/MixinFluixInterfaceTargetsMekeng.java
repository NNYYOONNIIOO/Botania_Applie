package nyonio.mixin;

import nyonio.FluixPoolManaHelper;
import nyonio.IFluixManaReceiver;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = {
        "com.glodblock.github.common.part.PartTrioInterface",
        "com.glodblock.github.common.tile.TileTrioInterface",
        "com.mekeng.github.common.part.PartGasInterface",
        "com.mekeng.github.common.tile.TileGasInterface"
}, remap = false)
public abstract class MixinFluixInterfaceTargetsMekeng implements IFluixManaReceiver {
    @Override
    public boolean hasFluixPoolCard() {
        return FluixPoolManaHelper.hasCard(this);
    }

    @Override
    public Object getFluixManaTarget() {
        return this;
    }
}
