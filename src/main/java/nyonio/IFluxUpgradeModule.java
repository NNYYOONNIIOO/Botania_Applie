package nyonio;

import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import net.minecraft.item.ItemStack;

public interface IFluxUpgradeModule extends IUpgradeModule {
    String getUpgradeTypeId();
    int getMaxInstalled();
    @Override
    Upgrades getType(ItemStack stack);
}
