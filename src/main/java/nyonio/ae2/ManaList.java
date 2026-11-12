package nyonio.ae2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.math.BigInteger;

import javax.annotation.Nonnull;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IItemList;

public class ManaList implements IItemList<ManaStack> {

    // Keep the exact total so later extraction deltas remain correct even if
    // several cells together exceed the long-valued AE2 display limit.
    private BigInteger manaAmount = BigInteger.ZERO;

    private long visibleAmount() {
        return manaAmount.min(BigInteger.valueOf(Long.MAX_VALUE)).max(BigInteger.ZERO).longValue();
    }

    @Override
    public void add(ManaStack option) {
        if (option != null) {
            this.manaAmount = this.manaAmount.add(BigInteger.valueOf(option.getStackSize()));
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
        if (this.manaAmount.signum() > 0) {
            return new ManaStack(visibleAmount());
        }
        return null;
    }

    @Override
    public Collection<ManaStack> findFuzzy(ManaStack input, FuzzyMode fuzzy) {
        Collection<ManaStack> result = new ArrayList<>();
        if (this.manaAmount.signum() > 0) {
            result.add(new ManaStack(visibleAmount()));
        }
        return result;
    }

    @Override
    public int size() {
        return this.manaAmount.signum() > 0 ? 1 : 0;
    }

    @Override
    public nyonio.ae2.ManaStack getFirstItem() {
        if (this.manaAmount.signum() > 0) {
            return new nyonio.ae2.ManaStack(visibleAmount());
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return this.manaAmount.signum() <= 0;
    }

    @Override
    public void resetStatus() {
        this.manaAmount = BigInteger.ZERO;
    }

    @Nonnull
    @Override
    public Iterator<ManaStack> iterator() {
        return new Iterator<ManaStack>() {
            private boolean hasNext = manaAmount.signum() > 0;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public ManaStack next() {
                if (hasNext) {
                    hasNext = false;
                    return new ManaStack(visibleAmount());
                }
                throw new NoSuchElementException();
            }
        };
    }
}
