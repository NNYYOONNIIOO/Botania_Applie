package nyonio.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ItemManaPacketTooltipTest {
    @Test
    public void virtualEntriesDoNotAddStoredManaRegardlessOfTheirNbtAmount() {
        ItemManaPacket item = new ItemManaPacket();
        for (long amount : new long[] {0L, 6634000L, Long.MAX_VALUE}) {
            List<ITextComponent> tooltip = new ArrayList<>();
            ITextComponent existingLine = new StringTextComponent("AE2 storage amount");
            tooltip.add(existingLine);
            item.addInformation(stack(amount, true), null, tooltip, ITooltipFlag.TooltipFlags.NORMAL);
            assertEquals(1, tooltip.size());
            assertSame(existingLine, tooltip.get(0));
        }
    }

    @Test
    public void nonDisplayPacketsKeepTheirStoredManaInformation() {
        List<ITextComponent> tooltip = new ArrayList<>();
        new ItemManaPacket().addInformation(stack(5000000000L, false), null, tooltip,
                ITooltipFlag.TooltipFlags.NORMAL);
        assertEquals(1, tooltip.size());
        assertTrue(tooltip.get(0) instanceof TranslationTextComponent);
        assertTrue(ITextComponent.Serializer.toJson(tooltip.get(0))
                .contains(ManaNumberFormat.format(5000000000L)));
    }

    private ItemStack stack(long amount, boolean displayOnly) {
        CompoundNBT tag = new CompoundNBT();
        tag.putLong(ItemManaPacket.TAG_MANA, amount);
        tag.putBoolean(ItemManaPacket.TAG_DISPLAY_ONLY, displayOnly);
        ItemStack stack = mock(ItemStack.class);
        when(stack.hasTag()).thenReturn(true);
        when(stack.getTag()).thenReturn(tag);
        return stack;
    }
}
