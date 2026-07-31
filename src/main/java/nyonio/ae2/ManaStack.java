package nyonio.ae2;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import nyonio.item.ItemManaPacket;

public class ManaStack implements IAEStack<ManaStack> {

    public static final ManaStack STATIC_STACK = new ManaStack(0);

    private long stackSize;
    private long countRequestable;
    private boolean isCraftable;

    public ManaStack(long amount) {
        this.stackSize = amount;
        this.countRequestable = 0;
        this.isCraftable = false;
    }

    @Override
    public void add(ManaStack is) {
        if (is != null) {
            this.stackSize += is.stackSize;
        }
    }

    @Override
    public long getStackSize() {
        return this.stackSize;
    }

    @Override
    public ManaStack setStackSize(long stackSize) {
        this.stackSize = stackSize;
        return this;
    }

    @Override
    public long getCountRequestable() {
        return this.countRequestable;
    }

    @Override
    public ManaStack setCountRequestable(long countRequestable) {
        this.countRequestable = countRequestable;
        return this;
    }

    @Override
    public boolean isCraftable() {
        return this.isCraftable;
    }

    @Override
    public ManaStack setCraftable(boolean isCraftable) {
        this.isCraftable = isCraftable;
        return this;
    }

    @Override
    public ManaStack reset() {
        this.stackSize = 0;
        this.countRequestable = 0;
        this.isCraftable = false;
        return this;
    }

    @Override
    public boolean isMeaningful() {
        return this.stackSize != 0 || this.countRequestable != 0 || this.isCraftable;
    }

    @Override
    public void incStackSize(long i) {
        this.stackSize += i;
    }

    @Override
    public void decStackSize(long i) {
        this.stackSize -= i;
    }

    @Override
    public void incCountRequestable(long i) {
        this.countRequestable += i;
    }

    @Override
    public void decCountRequestable(long i) {
        this.countRequestable -= i;
    }

    @Override
    public void writeToNBT(NBTTagCompound i) {
        i.setLong("mana", this.stackSize);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ManaStack;
    }

    @Override
    public int hashCode() {
        return ManaStack.class.hashCode();
    }

    @Override
    public boolean fuzzyComparison(ManaStack other, FuzzyMode mode) {
        return true;
    }

    @Override
    public void writeToPacket(ByteBuf data) throws IOException {
        data.writeLong(this.stackSize);
    }

    @Override
    public ManaStack copy() {
        ManaStack stack = new ManaStack(this.stackSize);
        stack.setCountRequestable(this.countRequestable);
        stack.setCraftable(this.isCraftable);
        return stack;
    }

    @Override
    public ManaStack empty() {
        return new ManaStack(0);
    }

    @Override
    public boolean isItem() {
        return false;
    }

    @Override
    public boolean isFluid() {
        return false;
    }

    @Override
    public IStorageChannel<ManaStack> getChannel() {
        return ManaStorageChannel.INSTANCE;
    }

    @Override
    public ItemStack asItemStackRepresentation() {
        return ItemManaPacket.create(this.stackSize);
    }
}
