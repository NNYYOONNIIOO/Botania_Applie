package nyonio;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nyonio.client.handler.FluixPoolHUDHandler;
import nyonio.client.handler.ManaTerminalHandler;
import nyonio.client.model.ManaPacketModel;
import nyonio.entity.EntityChannelSpark;
import nyonio.client.render.RenderChannelSpark;
import nyonio.client.render.RenderTileFluixManaPool;
import nyonio.tile.TileFluixManaPool;
import vazkii.botania.api.state.enums.PoolVariant;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }
    
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ClientRegistry.bindTileEntitySpecialRenderer(TileFluixManaPool.class, new RenderTileFluixManaPool());
        MinecraftForge.EVENT_BUS.register(new FluixPoolHUDHandler());
        MinecraftForge.EVENT_BUS.register(new ManaTerminalHandler());
        ModelLoaderRegistry.registerLoader(new ManaPacketModel.Loader());
        RenderingRegistry.registerEntityRenderingHandler(EntityChannelSpark.class,
            RenderChannelSpark::new);
    }
    
    @Override
    public void registerModels() {
        for (PoolVariant variant : PoolVariant.values()) {
            ModelLoader.setCustomModelResourceLocation(
                Item.getItemFromBlock(BotaniaApplie.fluixManaPool),
                variant.ordinal(),
                new ModelResourceLocation(BotaniaApplie.MODID + ":fluix_mana_pool", "variant=" + variant.getName())
            );
        }
        
        if (ModConfig.ITEMS.enableManaCellHousing && BotaniaApplie.manaCellHousing != null) {
            registerItemModel(BotaniaApplie.manaCellHousing, 0, "mana_cell_housing");
        }
        registerItemModel(BotaniaApplie.manaStorageCell1k, 0, "mana_storage_cell_1k");
        registerItemModel(BotaniaApplie.manaStorageCell4k, 0, "mana_storage_cell_4k");
        registerItemModel(BotaniaApplie.manaStorageCell16k, 0, "mana_storage_cell_16k");
        registerItemModel(BotaniaApplie.manaStorageCell64k, 0, "mana_storage_cell_64k");
        registerItemModel(BotaniaApplie.manaStorageCell256k, 0, "mana_storage_cell_256k");
        registerItemModel(BotaniaApplie.manaStorageCell1m, 0, "mana_storage_cell_1m");
        registerItemModel(BotaniaApplie.manaStorageCell4m, 0, "mana_storage_cell_4m");
        registerItemModel(BotaniaApplie.manaStorageCell16m, 0, "mana_storage_cell_16m");
        registerItemModel(BotaniaApplie.manaStorageCell64m, 0, "mana_storage_cell_64m");
        registerItemModel(BotaniaApplie.manaStorageCell256m, 0, "mana_storage_cell_256m");
        registerItemModel(BotaniaApplie.manaStorageCell1g, 0, "mana_storage_cell_1g");
        
        if (ModConfig.ITEMS.enableManaStorageComponent && BotaniaApplie.manaStorageComponent1k != null) {
            registerItemModel(BotaniaApplie.manaStorageComponent1k, 0, "mana_storage_component_1k");
            registerItemModel(BotaniaApplie.manaStorageComponent4k, 0, "mana_storage_component_4k");
            registerItemModel(BotaniaApplie.manaStorageComponent16k, 0, "mana_storage_component_16k");
            registerItemModel(BotaniaApplie.manaStorageComponent64k, 0, "mana_storage_component_64k");
            registerItemModel(BotaniaApplie.manaStorageComponent256k, 0, "mana_storage_component_256k");
            registerItemModel(BotaniaApplie.manaStorageComponent1m, 0, "mana_storage_component_1m");
            registerItemModel(BotaniaApplie.manaStorageComponent4m, 0, "mana_storage_component_4m");
            registerItemModel(BotaniaApplie.manaStorageComponent16m, 0, "mana_storage_component_16m");
            registerItemModel(BotaniaApplie.manaStorageComponent64m, 0, "mana_storage_component_64m");
            registerItemModel(BotaniaApplie.manaStorageComponent256m, 0, "mana_storage_component_256m");
            registerItemModel(BotaniaApplie.manaStorageComponent1g, 0, "mana_storage_component_1g");
        }
        registerItemModel(BotaniaApplie.manaPacket, 0, "mana_packet");
        registerItemModel(BotaniaApplie.channelSpark, 0, "channel_spark");
        registerItemModel(BotaniaApplie.manaCardBasic, 0, "mana_card_basic");
        registerItemModel(BotaniaApplie.manaCardAdvanced, 0, "mana_card_advanced");
        registerItemModel(BotaniaApplie.fluixPoolCard, 0, "fluixpool_card");
    }
    
    private void registerItemModel(Item item, int meta, String name) {
        ModelLoader.setCustomModelResourceLocation(item, meta, 
            new ModelResourceLocation(BotaniaApplie.MODID + ":" + name, "inventory"));
    }
}
