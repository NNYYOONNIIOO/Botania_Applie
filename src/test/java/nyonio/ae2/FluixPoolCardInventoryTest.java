package nyonio.ae2;

import appeng.fluids.helper.IFluidInterfaceHost;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FluixPoolCardInventoryTest {
    @Test
    public void createsOneCardSlotAndPreservesTheOldNbtFormat() {
        FluixPoolCardInventory inventory = new FluixPoolCardInventory(mock(IFluidInterfaceHost.class));
        assertEquals(1, inventory.getSlots());
        assertEquals(1, inventory.getSlotLimit(0));
        assertTrue(inventory.getStackInSlot(0).isEmpty());
        CompoundNBT data = inventory.serializeNBT();
        assertEquals(1, data.getInt("Size"));
        assertTrue(data.contains("Items", 9));
        FluixPoolCardInventory restored = new FluixPoolCardInventory(null);
        restored.deserializeNBT(data);
        assertEquals(1, restored.getSlots());
        assertTrue(restored.getStackInSlot(0).isEmpty());
    }

    @Test
    public void notifiesTheFluidInterfaceWhenContentsChange() {
        IFluidInterfaceHost host = mock(IFluidInterfaceHost.class);
        FluixPoolCardInventory inventory = new FluixPoolCardInventory(host);
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        verify(host).saveChanges();
        new FluixPoolCardInventory(null).setStackInSlot(0, ItemStack.EMPTY);
    }
}
