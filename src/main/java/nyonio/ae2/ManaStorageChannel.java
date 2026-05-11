package nyonio.ae2;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IItemList;

public class ManaStorageChannel implements IStorageChannel<ManaStack> {

    public static final ManaStorageChannel INSTANCE = new ManaStorageChannel();

    @Override
    public int transferFactor() {
        return 500;
    }

    @Override
    public int getUnitsPerByte() {
        return 500;
    }

    @Nonnull
    @Override
    public IItemList<ManaStack> createList() {
        return new ManaList();
    }

    @Nullable
    @Override
    public ManaStack createStack(@Nonnull Object input) {
        if (input instanceof ManaStack) {
            return ((ManaStack) input).copy();
        }
        return null;
    }

    @Nullable
    @Override
    public ManaStack readFromPacket(@Nonnull ByteBuf input) throws IOException {
        long amount = input.readLong();
        return new ManaStack(amount);
    }

    @Nullable
    @Override
    public ManaStack createFromNBT(@Nonnull NBTTagCompound nbt) {
        long amount = nbt.getLong("mana");
        return new ManaStack(amount);
    }
}
