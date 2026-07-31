package nyonio.integration.wireless;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import vazkii.botania.api.mana.ManaItemsEvent;

public final class WirelessManaItemsHandler {

    @SubscribeEvent
    public void onManaItems(ManaItemsEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }

        if (WirelessManaAccess.hasWirelessTerminal(player)) {
            event.add(WirelessManaContainerItem.create(player));
        }
    }
}
