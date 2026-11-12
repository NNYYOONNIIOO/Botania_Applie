package nyonio.integration.top;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import nyonio.tile.TileFluixManaPool;

public class FluixManaPoolProbeProvider implements IProbeInfoProvider {
    @Override
    public String getID() {
        return "botania_applie:fluix_mana_pool";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, PlayerEntity player, World world,
                             BlockState blockState, IProbeHitData data) {
        TileEntity tile = world.getTileEntity(data.getPos());
        if (!(tile instanceof TileFluixManaPool)) {
            return;
        }

        TileFluixManaPool pool = (TileFluixManaPool) tile;
        long maxMana = Math.max(1L, pool.getProbeMaxMana());
        long currentMana = Math.max(0L, Math.min(maxMana, pool.getProbeCurrentMana()));
        probeInfo.progress(currentMana, maxMana,
                probeInfo.defaultProgressStyle()
                        .suffix(" Mana")
                        .filledColor(0x00C6FF)
                        .alternateFilledColor(0x0088CC));
    }
}
