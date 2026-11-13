package nyonio.integration.jade;

import java.util.ArrayList;
import java.util.List;
import mcp.mobius.waila.api.IDataAccessor;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.RenderableTextComponent;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import nyonio.tile.TileFluixManaPool;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FluixManaPoolProviderTest {
    @Test
    public void sendsPoolTotalsAsLongWithoutReplacingOtherProvidersData() {
        TileFluixManaPool pool = mock(TileFluixManaPool.class);
        when(pool.getProbeCurrentMana()).thenReturn(5000000000L);
        when(pool.getProbeMaxMana()).thenReturn(Long.MAX_VALUE);
        CompoundNBT tag = new CompoundNBT();
        tag.putString("other_provider", "preserve");
        new FluixManaPoolProvider().appendServerData(tag, null, null, pool);
        ManaPoolData data = ManaPoolData.read(tag.getCompound(ManaPoolData.TAG));
        assertEquals(5000000000L, data.current);
        assertEquals(Long.MAX_VALUE, data.capacity);
        assertEquals("preserve", tag.getString("other_provider"));
    }

    @Test
    public void usesServerSnapshotWithoutReadingClientNetwork() {
        IDataAccessor accessor = mock(IDataAccessor.class);
        IPluginConfig config = mock(IPluginConfig.class);
        when(config.get(JadeIntegration.MANA)).thenReturn(true);
        CompoundNBT tag = new CompoundNBT();
        tag.put(ManaPoolData.TAG, new ManaPoolData(5000000000L, Long.MAX_VALUE).write());
        when(accessor.getServerData()).thenReturn(tag);
        List<ITextComponent> tooltip = new ArrayList<>();
        new FluixManaPoolProvider().appendBody(tooltip, accessor, config);
        assertEquals(3, tooltip.size());
        assertTrue(tooltip.get(2) instanceof RenderableTextComponent);
        verify(accessor, never()).getTileEntity();
        verify(accessor, never()).getWorld();
    }

    @Test
    public void addsNothingWhenDataIsMissingOrDisplayIsDisabled() {
        IDataAccessor accessor = mock(IDataAccessor.class);
        IPluginConfig config = mock(IPluginConfig.class);
        when(accessor.getServerData()).thenReturn(new CompoundNBT());
        when(config.get(JadeIntegration.MANA)).thenReturn(true);
        List<ITextComponent> tooltip = new ArrayList<>();
        FluixManaPoolProvider provider = new FluixManaPoolProvider();
        provider.appendBody(tooltip, accessor, config);
        assertTrue(tooltip.isEmpty());

        CompoundNBT data = new CompoundNBT();
        data.put(ManaPoolData.TAG, new ManaPoolData(1, 100).write());
        when(accessor.getServerData()).thenReturn(data);
        when(config.get(JadeIntegration.MANA)).thenReturn(false);
        provider.appendBody(tooltip, accessor, config);
        assertTrue(tooltip.isEmpty());
    }
}
