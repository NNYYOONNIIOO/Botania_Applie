package nyonio.ae2;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import appeng.api.config.AccessRestriction;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.data.IItemList;

public class ManaCellInventoryHandler implements ICellInventoryHandler<ManaStack> {

    private final ManaCellInventory inventory;

    public ManaCellInventoryHandler(ManaCellInventory inventory) {
        this.inventory = inventory;
    }

    @Nullable
    @Override
    public ManaCellInventory getCellInv() {
        return this.inventory;
    }

    @Override
    public boolean isPreformatted() {
        return false;
    }

    @Override
    public boolean isFuzzy() {
        return false;
    }

    @Override
    public IncludeExclude getIncludeExcludeMode() {
        return IncludeExclude.WHITELIST;
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public boolean isPrioritized(ManaStack input) {
        return false;
    }

    @Override
    public boolean canAccept(ManaStack input) {
        return true;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public int getSlot() {
        return 0;
    }

    @Override
    public boolean validForPass(int i) {
        return true;
    }

    @Override
    public ManaStack injectItems(ManaStack input, appeng.api.config.Actionable mode, appeng.api.networking.security.IActionSource src) {
        return this.inventory.injectItems(input, mode, src);
    }

    @Override
    public ManaStack extractItems(ManaStack request, appeng.api.config.Actionable mode, appeng.api.networking.security.IActionSource src) {
        return this.inventory.extractItems(request, mode, src);
    }

    @Override
    public IItemList<ManaStack> getAvailableItems(IItemList<ManaStack> out) {
        return this.inventory.getAvailableItems(out);
    }

    @Override
    public ManaStorageChannel getChannel() {
        return ManaStorageChannel.INSTANCE;
    }

    public void persist() {
        this.inventory.persist();
    }
}
