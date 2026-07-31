package nyonio;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = BotaniaApplie.MODID)
public class ModConfig {
    
    @Config.Name("Items")
    @Config.Comment("Item settings")
    public static final Items ITEMS = new Items();
    
    public static class Items {
        @Config.Name("Enable Mana Cell Housing")
        @Config.Comment("Set to true to enable the Mana Cell Housing item")
        @Config.RequiresWorldRestart
        public boolean enableManaCellHousing = false;
        
        @Config.Name("Enable Mana Storage Components")
        @Config.Comment("Set to true to enable the ME Mana Storage Component items")
        @Config.RequiresWorldRestart
        public boolean enableManaStorageComponent = true;
    }
    
    @Mod.EventBusSubscriber(modid = BotaniaApplie.MODID)
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(BotaniaApplie.MODID)) {
                ConfigManager.sync(BotaniaApplie.MODID, Config.Type.INSTANCE);
            }
        }
    }
}
