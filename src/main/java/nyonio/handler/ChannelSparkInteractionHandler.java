package nyonio.handler;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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
                || event.getEntityPlayer() == null
                || event.getEntityPlayer().getHeldItem(event.getHand()).isEmpty()
                || event.getEntityPlayer().getHeldItem(event.getHand()).getItem()
                != ModItems.twigWand) {
            return;
        }

        EntityChannelSpark spark = (EntityChannelSpark) event.getTarget();
        if (spark.interactWithTwigWand(event.getEntityPlayer(), event.getHand())) {
            event.setCanceled(true);
        }
    }
}
