package nyonio.integration.jade;

import java.text.NumberFormat;
import java.util.List;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IDataAccessor;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerDataProvider;
import mcp.mobius.waila.api.RenderableTextComponent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import nyonio.tile.TileFluixManaPool;

public final class FluixManaPoolProvider implements IComponentProvider, IServerDataProvider<TileEntity> {
    @Override
    public void appendServerData(CompoundNBT data, ServerPlayerEntity player, World world, TileEntity tile) {
        if (tile instanceof TileFluixManaPool) {
            TileFluixManaPool pool = (TileFluixManaPool) tile;
            data.put(ManaPoolData.TAG, new ManaPoolData(pool.getProbeCurrentMana(), pool.getProbeMaxMana()).write());
        }
    }

    @Override
    public void appendBody(List<ITextComponent> tooltip, IDataAccessor accessor, IPluginConfig config) {
        CompoundNBT serverData = accessor.getServerData();
        if (!config.get(JadeIntegration.MANA) || !serverData.contains(ManaPoolData.TAG, Constants.NBT.TAG_COMPOUND)) {
            return;
        }
        // Never query AE2 from the client: use the server's full long-valued snapshot.
        ManaPoolData mana = ManaPoolData.read(serverData.getCompound(ManaPoolData.TAG));
        NumberFormat numbers = NumberFormat.getIntegerInstance();
        tooltip.add(new TranslationTextComponent("botania_applie.tooltip.stored_mana", numbers.format(mana.current)));
        tooltip.add(new TranslationTextComponent("botania_applie.tooltip.mana_capacity", numbers.format(mana.capacity)));
        tooltip.add(new RenderableTextComponent(JadeIntegration.MANA, mana.write()));
    }
}
