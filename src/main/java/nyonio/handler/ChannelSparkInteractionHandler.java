package nyonio.handler;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import nyonio.BotaniaApplie;
import nyonio.entity.EntityChannelSpark;
import vazkii.botania.common.item.ModItems;

/**
 * Makes channel sparks use the forest wand interaction path used by Botania
 * sparks. Some 1.12.2 client/server paths post the entity event without
 * invoking the custom entity callback, so handle the event explicitly.
 */
@Mod.EventBusSubscriber(modid = BotaniaApplie.MODID)
public final class ChannelSparkInteractionHandler {
    private ChannelSparkInteractionHandler() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event == null || event.isCanceled()
                || !(event.getTarget() instanceof EntityChannelSpark)
                || event.getPlayer() == null
                || event.getPlayer().getHeldItem(event.getHand()).isEmpty()
                || event.getPlayer().getHeldItem(event.getHand()).getItem()
                != ModItems.twigWand) {
            return;
        }

        EntityChannelSpark spark = (EntityChannelSpark) event.getTarget();
        if (spark.interactWithTwigWand(event.getPlayer(), event.getHand())) {
            event.setCanceled(true);
        }
    }
}
