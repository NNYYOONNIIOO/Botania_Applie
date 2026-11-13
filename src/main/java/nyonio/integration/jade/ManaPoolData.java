package nyonio.integration.jade;

import net.minecraft.nbt.CompoundNBT;

/** Long-valued display data shared by the server provider and Jade's client bar. */
public final class ManaPoolData {
    public static final String TAG = "botania_applie:mana";
    public final long current;
    public final long capacity;

    public ManaPoolData(long current, long capacity) {
        this.capacity = Math.max(0L, capacity);
        this.current = Math.max(0L, Math.min(current, this.capacity));
    }

    public CompoundNBT write() {
        CompoundNBT tag = new CompoundNBT();
        tag.putLong("current", current);
        tag.putLong("capacity", capacity);
        return tag;
    }

    public static ManaPoolData read(CompoundNBT tag) {
        return new ManaPoolData(tag.getLong("current"), tag.getLong("capacity"));
    }

    public int filledWidth(int width) {
        if (width <= 0 || current == 0L || capacity == 0L) {
            return 0;
        }
        // Convert before multiplying: long * pixel width would overflow on large networks.
        return Math.min(width, Math.max(1, (int) (width * ((double) current / capacity))));
    }
}
