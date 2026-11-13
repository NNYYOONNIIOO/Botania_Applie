package nyonio.integration.jade;

import net.minecraft.nbt.CompoundNBT;
import org.junit.Test;
import static org.junit.Assert.*;

public class ManaPoolDataTest {
    @Test
    public void preservesLongValuesThroughNbt() {
        ManaPoolData data = new ManaPoolData(5000000000L, Long.MAX_VALUE);
        CompoundNBT tag = data.write();
        assertTrue(tag.contains("current", 4)); // NBT long, not int
        assertTrue(tag.contains("capacity", 4));
        ManaPoolData restored = ManaPoolData.read(tag);
        assertEquals(5000000000L, restored.current);
        assertEquals(Long.MAX_VALUE, restored.capacity);
    }

    @Test
    public void scalesFullAndHalfBarsWithoutLongOverflow() {
        assertEquals(148, new ManaPoolData(Long.MAX_VALUE, Long.MAX_VALUE).filledWidth(148));
        assertEquals(74, new ManaPoolData(Long.MAX_VALUE / 2, Long.MAX_VALUE).filledWidth(148));
        assertEquals(74, new ManaPoolData(5000000000L, 10000000000L).filledWidth(148));
    }

    @Test
    public void keepsEmptyAndTinyAmountsDistinct() {
        assertEquals(0, new ManaPoolData(0, Long.MAX_VALUE).filledWidth(148));
        assertEquals(1, new ManaPoolData(1, Long.MAX_VALUE).filledWidth(148));
        assertEquals(0, new ManaPoolData(10, 100).filledWidth(0));
    }

    @Test
    public void clampsMalformedDataAndHandlesMissingCapacity() {
        assertEquals(0, new ManaPoolData(-1, 100).current);
        assertEquals(100, new ManaPoolData(101, 100).current);
        ManaPoolData invalid = new ManaPoolData(100, -1);
        assertEquals(0, invalid.current);
        assertEquals(0, invalid.capacity);
        assertEquals(0, invalid.filledWidth(148));
        assertEquals(0, ManaPoolData.read(new CompoundNBT()).filledWidth(148));
    }
}
