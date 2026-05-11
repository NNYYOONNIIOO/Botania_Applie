package nyonio.ae2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nonnull;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IItemList;

public class ManaList implements IItemList<ManaStack> {

    private long manaAmount = 0;

    @Override
    public void add(ManaStack option) {
        if (option != null) {
            this.manaAmount += option.getStackSize();
        }
    }

    @Override
    public void addStorage(ManaStack option) {
        this.add(option);
    }

    @Override
    public void addCrafting(ManaStack option) {
    }

    @Override
    public void addRequestable(ManaStack option) {
    }

    @Override
    public ManaStack findPrecise(ManaStack i) {
        if (this.manaAmount > 0) {
            return new ManaStack(this.manaAmount);
        }
        return null;
    }

    @Override
    public Collection<ManaStack> findFuzzy(ManaStack input, FuzzyMode fuzzy) {
        Collection<ManaStack> result = new ArrayList<>();
        if (this.manaAmount > 0) {
            result.add(new ManaStack(this.manaAmount));
        }
        return result;
    }

    @Override
    public int size() {
        return this.manaAmount > 0 ? 1 : 0;
    }

    @Override
    public nyonio.ae2.ManaStack getFirstItem() {
        if (this.manaAmount > 0) {
            return new nyonio.ae2.ManaStack(this.manaAmount);
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return this.manaAmount <= 0;
    }

    @Override
    public void resetStatus() {
        this.manaAmount = 0;
    }

    @Nonnull
    @Override
    public Iterator<ManaStack> iterator() {
        return new Iterator<ManaStack>() {
            private boolean hasNext = manaAmount > 0;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public ManaStack next() {
                if (hasNext) {
                    hasNext = false;
                    return new ManaStack(manaAmount);
                }
                return null;
            }
        };
    }
}
