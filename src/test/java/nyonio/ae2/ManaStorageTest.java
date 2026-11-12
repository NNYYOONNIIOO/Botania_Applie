package nyonio.ae2;

import appeng.api.config.Actionable;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ManaStorageTest {
    private ManaCellInventory cell(CompoundNBT tag,int kilobytes) {
        ItemStack stack=mock(ItemStack.class);
        when(stack.getOrCreateTag()).thenReturn(tag);
        return new ManaCellInventory(stack,null,kilobytes);
    }
    @Test public void simulationDoesNotCreateOrDestroyMana() {
        ManaCellInventory inv=cell(new CompoundNBT(),1);
        assertNull(inv.injectItems(new ManaStack(1234),Actionable.SIMULATE,null));
        assertEquals(0,inv.getStoredItemCount());
        inv.injectItems(new ManaStack(1234),Actionable.MODULATE,null);
        assertEquals(100,inv.extractItems(new ManaStack(100),Actionable.SIMULATE,null).getStackSize());
        assertEquals(1234,inv.getStoredItemCount());
    }
    @Test public void roundTripPersistsOnlyTheRemainingMana() {
        CompoundNBT tag=new CompoundNBT();
        ManaCellInventory inv=cell(tag,1);
        inv.injectItems(new ManaStack(12345),Actionable.MODULATE,null);
        assertEquals(2345,inv.extractItems(new ManaStack(2345),Actionable.MODULATE,null).getStackSize());
        assertEquals(10000,cell(tag,1).getStoredItemCount());
        inv.extractItems(new ManaStack(Long.MAX_VALUE),Actionable.MODULATE,null);
        assertEquals(0,cell(tag,1).getStoredItemCount());
        assertFalse(tag.contains("#0"));
    }
    @Test public void capacityRejectsExcessAndExtractionCannotDuplicateMana() {
        ManaCellInventory inv=cell(new CompoundNBT(),1);
        long capacity=1024L*500;
        ManaStack remainder=inv.injectItems(new ManaStack(capacity+10),Actionable.MODULATE,null);
        assertEquals(10,remainder.getStackSize());
        assertEquals(capacity,inv.extractItems(new ManaStack(Long.MAX_VALUE),Actionable.MODULATE,null).getStackSize());
        assertNull(inv.extractItems(new ManaStack(1),Actionable.MODULATE,null));
        assertEquals(0,inv.getStoredItemCount());
    }
    @Test public void oneGigabyteCellExceedsIntWithoutTruncation() {
        CompoundNBT tag=new CompoundNBT();
        ManaCellInventory inv=cell(tag,1048576);
        long capacity=1048576L*1024*500;
        inv.injectItems(new ManaStack(capacity),Actionable.MODULATE,null);
        assertEquals(capacity,cell(tag,1048576).getStoredItemCount());
        assertEquals(capacity,inv.extractItems(new ManaStack(Long.MAX_VALUE),Actionable.SIMULATE,null).getStackSize());
    }
    @Test public void quantityUsesLongInNbtAndNetworkPackets() {
        long amount=5000000000L;
        ManaStack stack=new ManaStack(amount);
        CompoundNBT tag=new CompoundNBT();stack.writeToNBT(tag);
        assertEquals(amount,ManaStorageChannel.INSTANCE.createFromNBT(tag).getStackSize());
        PacketBuffer buffer=new PacketBuffer(Unpooled.buffer());
        try {stack.writeToPacket(buffer);assertEquals(amount,ManaStorageChannel.INSTANCE.readFromPacket(buffer).getStackSize());}finally{buffer.release();}
    }
    @Test public void copyKeepsQuantityAndCraftingStateIndependent() {
        ManaStack original=new ManaStack(5000000000L).setCraftable(true).setCountRequestable(7);
        ManaStack copy=original.copy();copy.setStackSize(1).setCraftable(false);
        assertEquals(5000000000L,original.getStackSize());assertTrue(original.isCraftable());assertEquals(7,copy.getCountRequestable());
    }
    @Test public void storageListAppliesNegativeExtractionDeltas() {
        ManaList list=new ManaList();
        list.addStorage(new ManaStack(5000000000L));
        list.addStorage(new ManaStack(-1234));
        assertEquals(4999998766L,list.getFirstItem().getStackSize());
        list.addStorage(new ManaStack(-4999998766L));assertTrue(list.isEmpty());
    }
    @Test public void saturatedListStillAccountsForLaterExtractionDeltas() {
        ManaList list=new ManaList();
        list.addStorage(new ManaStack(Long.MAX_VALUE));
        list.addStorage(new ManaStack(200));
        assertEquals(Long.MAX_VALUE,list.getFirstItem().getStackSize());
        list.addStorage(new ManaStack(-201));
        assertEquals(Long.MAX_VALUE-1,list.getFirstItem().getStackSize());
        list.addStorage(new ManaStack(-(Long.MAX_VALUE-1)));
        assertTrue(list.isEmpty());
    }
}
