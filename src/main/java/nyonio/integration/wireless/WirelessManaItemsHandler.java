package nyonio.integration.wireless;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import vazkii.botania.api.mana.ManaItemsEvent;

public final class WirelessManaItemsHandler {

    @SubscribeEvent
    public void onManaItems(ManaItemsEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }

        if (WirelessManaAccess.hasWirelessTerminal(player)) {
            event.add(WirelessManaContainerItem.create(player));
        }
    }
}
