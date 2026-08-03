package nyonio;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import nyonio.integration.terminal.ManaResourceProvider;
import nyonio.network.CPacketManaContainerAction;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import nyonio.ae2.ManaCellHandler;
import nyonio.ae2.ManaStorageChannel;
import nyonio.block.BlockFluixManaPool;
import nyonio.handler.ManaCardTickHandler;
import nyonio.handler.ObedienceStickHandler;
import nyonio.integration.wireless.WirelessManaItemsHandler;
import nyonio.integration.top.TopIntegration;
import nyonio.item.ItemManaCard;
import nyonio.item.ItemManaPacket;
import nyonio.item.ItemManaStorageCell;
import nyonio.item.ItemManaStorageComponent;
import nyonio.tile.TileFluixManaPool;
import nyonio.terminal_interaction_integration.api.ResourceRegistrationEvent;
import nyonio.terminal_interaction_integration.api.UpgradeModuleRegistration;
import org.apache.logging.log4j.Logger;
import appeng.api.AEApi;
import zone.rong.mixinbooter.ILateMixinLoader;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.Loader;

@Mod(modid = BotaniaApplie.MODID, name = BotaniaApplie.NAME, version = BotaniaApplie.VERSION, dependencies = "required-after:appliedenergistics2;required-after:terminal_interaction_integration")
@Mod.EventBusSubscriber
public class BotaniaApplie implements ILateMixinLoader
{
    public static final String MODID = "botania_applie";
    public static final String NAME = "Botania Applie";
    public static final String VERSION = "1.2.2";

    @SidedProxy(clientSide = "nyonio.ClientProxy", serverSide = "nyonio.CommonProxy")
    public static CommonProxy proxy;

    private static Logger logger;
    
    private static SimpleNetworkWrapper network;
    
    public static SimpleNetworkWrapper getNetwork() {
        return network;
    }
    
    public static Logger getLogger() {
        return logger;
    }

    public static Block fluixManaPool;
    public static Item fluixManaPoolItem;

    public static Item manaCellHousing;
    public static Item manaStorageCell1k;
    public static Item manaStorageCell4k;
    public static Item manaStorageCell16k;
    public static Item manaStorageCell64k;
    public static Item manaStorageCell256k;
    public static Item manaStorageCell1m;
    public static Item manaStorageCell4m;
    public static Item manaStorageCell16m;
    public static Item manaStorageCell64m;
    public static Item manaStorageCell256m;
    public static Item manaStorageCell1g;

    public static Item manaStorageComponent1k;
    public static Item manaStorageComponent4k;
    public static Item manaStorageComponent16k;
    public static Item manaStorageComponent64k;
    public static Item manaStorageComponent256k;
    public static Item manaStorageComponent1m;
    public static Item manaStorageComponent4m;
    public static Item manaStorageComponent16m;
    public static Item manaStorageComponent64m;
    public static Item manaStorageComponent256m;
    public static Item manaStorageComponent1g;

    public static Item manaPacket;

    public static Item manaCardBasic;
    public static Item manaCardAdvanced;
    public static Item fluixPoolCard;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        network.registerMessage(CPacketManaContainerAction.Handler.class, CPacketManaContainerAction.class, 0, Side.SERVER);

        AEApi.instance().storage().registerStorageChannel(ManaStorageChannel.class, ManaStorageChannel.INSTANCE);

        MinecraftForge.EVENT_BUS.register(new TerminalIntegrationHandler());
        MinecraftForge.EVENT_BUS.register(new ManaCardTickHandler());
        MinecraftForge.EVENT_BUS.register(new ObedienceStickHandler());
        MinecraftForge.EVENT_BUS.register(new WirelessManaItemsHandler());
        fluixManaPool = new BlockFluixManaPool().setRegistryName(MODID, "fluix_mana_pool");
        fluixManaPoolItem = new ItemBlock(fluixManaPool).setRegistryName(MODID, "fluix_mana_pool").setUnlocalizedName("botania_applie.fluix_mana_pool").setCreativeTab(BotaniaApplieTab.INSTANCE);
        
        if (ModConfig.ITEMS.enableManaCellHousing) {
            manaCellHousing = new Item().setRegistryName(MODID, "mana_cell_housing").setUnlocalizedName("botania_applie.mana_cell_housing").setCreativeTab(BotaniaApplieTab.INSTANCE);
        }
        manaStorageCell1k = new ItemManaStorageCell(1, 0.5).setRegistryName(MODID, "mana_storage_cell_1k").setUnlocalizedName("botania_applie.mana_storage_cell_1k").setCreativeTab(BotaniaApplieTab.INSTANCE);
        manaStorageCell4k = new ItemManaStorageCell(4, 1.0).setRegistryName(MODID, "mana_storage_cell_4k").setUnlocalizedName("botania_applie.mana_storage_cell_4k").setCreativeTab(BotaniaApplieTab.INSTANCE);
        manaStorageCell16k = new ItemManaStorageCell(16, 1.5).setRegistryName(MODID, "mana_storage_cell_16k").setUnlocalizedName("botania_applie.mana_storage_cell_16k").setCreativeTab(BotaniaApplieTab.INSTANCE);
        manaStorageCell64k = new ItemManaStorageCell(64, 2.0).setRegistryName(MODID, "mana_storage_cell_64k").setUnlocalizedName("botania_applie.mana_storage_cell_64k").setCreativeTab(BotaniaApplieTab.INSTANCE);
        manaStorageCell256k = new ItemManaStorageCell(256, 2.5).setRegistryName(MODID, "mana_storage_cell_256k").setUnlocalizedName("botania_applie.mana_storage_cell_256k").setCreativeTab(BotaniaApplieTab.INSTANCE);
        manaStorageCell1m = new ItemManaStorageCell(1024, 3.0).setRegistryName(MODID, "mana_storage_cell_1m").setUnlocalizedName("botania_applie.mana_storage_cell_1m").setCreativeTab(BotaniaApplieTab.INSTANCE);
        manaStorageCell4m = new ItemManaStorageCell(4096, 3.5).setRegistryName(MODID, "mana_storage_cell_4m").setUnlocalizedName("botania_applie.mana_storage_cell_4m").setCreativeTab(BotaniaApplieTab.INSTANCE);
        manaStorageCell16m = new ItemManaStorageCell(16384, 4.0).setRegistryName(MODID, "mana_storage_cell_16m").setUnlocalizedName("botania_applie.mana_storage_cell_16m").setCreativeTab(BotaniaApplieTab.INSTANCE);
        manaStorageCell64m = new ItemManaStorageCell(65536, 4.5).setRegistryName(MODID, "mana_storage_cell_64m").setUnlocalizedName("botania_applie.mana_storage_cell_64m").setCreativeTab(BotaniaApplieTab.INSTANCE);
        manaStorageCell256m = new ItemManaStorageCell(262144, 5.0).setRegistryName(MODID, "mana_storage_cell_256m").setUnlocalizedName("botania_applie.mana_storage_cell_256m").setCreativeTab(BotaniaApplieTab.INSTANCE);
        manaStorageCell1g = new ItemManaStorageCell(1048576, 6.0).setRegistryName(MODID, "mana_storage_cell_1g").setUnlocalizedName("botania_applie.mana_storage_cell_1g").setCreativeTab(BotaniaApplieTab.INSTANCE);
        
        if (ModConfig.ITEMS.enableManaStorageComponent) {
            manaStorageComponent1k = new ItemManaStorageComponent(1).setRegistryName(MODID, "mana_storage_component_1k").setUnlocalizedName("botania_applie.mana_storage_component_1k").setCreativeTab(BotaniaApplieTab.INSTANCE);
            manaStorageComponent4k = new ItemManaStorageComponent(4).setRegistryName(MODID, "mana_storage_component_4k").setUnlocalizedName("botania_applie.mana_storage_component_4k").setCreativeTab(BotaniaApplieTab.INSTANCE);
            manaStorageComponent16k = new ItemManaStorageComponent(16).setRegistryName(MODID, "mana_storage_component_16k").setUnlocalizedName("botania_applie.mana_storage_component_16k").setCreativeTab(BotaniaApplieTab.INSTANCE);
            manaStorageComponent64k = new ItemManaStorageComponent(64).setRegistryName(MODID, "mana_storage_component_64k").setUnlocalizedName("botania_applie.mana_storage_component_64k").setCreativeTab(BotaniaApplieTab.INSTANCE);
            manaStorageComponent256k = new ItemManaStorageComponent(256).setRegistryName(MODID, "mana_storage_component_256k").setUnlocalizedName("botania_applie.mana_storage_component_256k").setCreativeTab(BotaniaApplieTab.INSTANCE);
            manaStorageComponent1m = new ItemManaStorageComponent(1024).setRegistryName(MODID, "mana_storage_component_1m").setUnlocalizedName("botania_applie.mana_storage_component_1m").setCreativeTab(BotaniaApplieTab.INSTANCE);
            manaStorageComponent4m = new ItemManaStorageComponent(4096).setRegistryName(MODID, "mana_storage_component_4m").setUnlocalizedName("botania_applie.mana_storage_component_4m").setCreativeTab(BotaniaApplieTab.INSTANCE);
            manaStorageComponent16m = new ItemManaStorageComponent(16384).setRegistryName(MODID, "mana_storage_component_16m").setUnlocalizedName("botania_applie.mana_storage_component_16m").setCreativeTab(BotaniaApplieTab.INSTANCE);
            manaStorageComponent64m = new ItemManaStorageComponent(65536).setRegistryName(MODID, "mana_storage_component_64m").setUnlocalizedName("botania_applie.mana_storage_component_64m").setCreativeTab(BotaniaApplieTab.INSTANCE);
            manaStorageComponent256m = new ItemManaStorageComponent(262144).setRegistryName(MODID, "mana_storage_component_256m").setUnlocalizedName("botania_applie.mana_storage_component_256m").setCreativeTab(BotaniaApplieTab.INSTANCE);
            manaStorageComponent1g = new ItemManaStorageComponent(1048576).setRegistryName(MODID, "mana_storage_component_1g").setUnlocalizedName("botania_applie.mana_storage_component_1g").setCreativeTab(BotaniaApplieTab.INSTANCE);
        }
        
        manaPacket = new ItemManaPacket();

        manaCardBasic = new ItemManaCard(40).setRegistryName(MODID, "mana_card_basic").setUnlocalizedName("botania_applie.mana_card_basic").setCreativeTab(BotaniaApplieTab.INSTANCE);
        manaCardAdvanced = new ItemManaCard(200).setRegistryName(MODID, "mana_card_advanced").setUnlocalizedName("botania_applie.mana_card_advanced").setCreativeTab(BotaniaApplieTab.INSTANCE);
        fluixPoolCard = new nyonio.item.ItemFluixPoolCard().setRegistryName(MODID, "fluixpool_card").setUnlocalizedName("botania_applie.fluixpool_card").setCreativeTab(BotaniaApplieTab.INSTANCE);
        UpgradeModuleRegistration.register(fluixPoolCard, 1);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init(event);

        TopIntegration.register();

        AEApi.instance().registries().cell().addCellHandler(ManaCellHandler.INSTANCE);

        GameRegistry.registerTileEntity(TileFluixManaPool.class, "botania_applie:fluix_mana_pool");
    }
    
    public static class TerminalIntegrationHandler {
        @SubscribeEvent
        public void onResourceRegistration(ResourceRegistrationEvent event) {
            event.register(new ManaResourceProvider());
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
        IForgeRegistry<Block> registry = event.getRegistry();
        registry.register(fluixManaPool);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        IForgeRegistry<Item> registry = event.getRegistry();
        registry.register(fluixManaPoolItem);
        if (ModConfig.ITEMS.enableManaCellHousing && manaCellHousing != null) {
            registry.register(manaCellHousing);
        }
        registry.register(manaStorageCell1k);
        registry.register(manaStorageCell4k);
        registry.register(manaStorageCell16k);
        registry.register(manaStorageCell64k);
        registry.register(manaStorageCell256k);
        registry.register(manaStorageCell1m);
        registry.register(manaStorageCell4m);
        registry.register(manaStorageCell16m);
        registry.register(manaStorageCell64m);
        registry.register(manaStorageCell256m);
        registry.register(manaStorageCell1g);
        if (ModConfig.ITEMS.enableManaStorageComponent && manaStorageComponent1k != null) {
            registry.register(manaStorageComponent1k);
            registry.register(manaStorageComponent4k);
            registry.register(manaStorageComponent16k);
            registry.register(manaStorageComponent64k);
            registry.register(manaStorageComponent256k);
            registry.register(manaStorageComponent1m);
            registry.register(manaStorageComponent4m);
            registry.register(manaStorageComponent16m);
            registry.register(manaStorageComponent64m);
            registry.register(manaStorageComponent256m);
            registry.register(manaStorageComponent1g);
        }
        registry.register(manaPacket);
        registry.register(manaCardBasic);
        registry.register(manaCardAdvanced);
        registry.register(fluixPoolCard);
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event)
    {
        proxy.registerModels();
    }

    @Override
    public List<String> getMixinConfigs() {
        List<String> configs = new java.util.ArrayList<>();
        configs.add("botania_applie.mixins.json");
        if (Loader.isModLoaded("ae2fc")) {
            configs.add("botania_applie.ae2fc.mixins.json");
        }
        if (Loader.isModLoaded("ae2fc") && Loader.isModLoaded("mekeng")) {
            configs.add("botania_applie.mekeng.mixins.json");
        }
        return configs;
    }
}
