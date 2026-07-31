package nyonio;

import appeng.api.implementations.items.IUpgradeModule;

public interface IFluixUpgradeModule extends IUpgradeModule {
    String getUpgradeTypeId();
    int getMaxInstalled();
}
