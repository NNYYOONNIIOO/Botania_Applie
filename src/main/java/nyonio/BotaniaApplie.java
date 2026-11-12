package nyonio;

import appeng.core.Api;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import nyonio.ae2.*;
import nyonio.block.BlockFluixManaPool;
import nyonio.channel.ChannelSparkNetwork;
import nyonio.entity.*;
import nyonio.handler.*;
import nyonio.integration.terminal.ManaResourceProvider;
import nyonio.integration.wireless.WirelessManaItemsHandler;
import nyonio.item.*;
import nyonio.tile.TileFluixManaPool;
import nyonio.terminal_interaction_integration.api.ResourceRegistrationEvent;
import nyonio.terminal_interaction_integration.api.UpgradeModuleRegistration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.List;

@Mod(BotaniaApplie.MODID)
public final class BotaniaApplie {
    public static final String MODID = "botania_applie";
    public static final String NAME = "Botania Applie";
    public static final String VERSION = "1.3.0-1.16.5";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<Item> ITEMS = new ArrayList<>();
    private static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(id("main"), () -> "1", "1"::equals, "1"::equals);
    public static final BlockFluixManaPool fluixManaPool = (BlockFluixManaPool) new BlockFluixManaPool().setRegistryName(id("fluix_mana_pool"));
    public static final TileEntityType<TileFluixManaPool> FLUIX_POOL_TILE = (TileEntityType<TileFluixManaPool>) TileEntityType.Builder.create(TileFluixManaPool::new, fluixManaPool).build(null).setRegistryName(id("fluix_mana_pool"));
    public static final Item fluixManaPoolItem = item("fluix_mana_pool", new BlockItem(fluixManaPool, properties()));
    public static final Item manaCellHousing = item("mana_cell_housing", new ItemManaCellHousing());
    public static final Item manaPacket = item("mana_packet", new ItemManaPacket());
    public static final Item channelSpark = item("channel_spark", new ItemChannelSpark());
    public static final Item mainChannelSpark = item("main_channel_spark", new ItemMainChannelSpark());
    public static final Item manaCardBasic = item("mana_card_basic", new ItemManaCard(40));
    public static final Item manaCardAdvanced = item("mana_card_advanced", new ItemManaCard(200));
    public static final Item fluixPoolCard = item("fluixpool_card", new ItemFluixPoolCard());
    public static final EntityType<EntityChannelSpark> CHANNEL_SPARK = (EntityType<EntityChannelSpark>) EntityType.Builder.<EntityChannelSpark>create(EntityChannelSpark::new, EntityClassification.MISC).size(0.1F, 0.5F).trackingRange(64).updateInterval(1).immuneToFire().build(MODID + ":channel_spark").setRegistryName(id("channel_spark"));
    public static final EntityType<EntityMainChannelSpark> MAIN_CHANNEL_SPARK = (EntityType<EntityMainChannelSpark>) EntityType.Builder.<EntityMainChannelSpark>create(EntityMainChannelSpark::new, EntityClassification.MISC).size(0.1F, 0.5F).trackingRange(64).updateInterval(1).immuneToFire().build(MODID + ":main_channel_spark").setRegistryName(id("main_channel_spark"));
    public static final Item[] STORAGE_CELLS = createCells();
    public static final Item[] STORAGE_COMPONENTS = createComponents();
    public static final Item manaStorageCell1k=STORAGE_CELLS[0], manaStorageCell4k=STORAGE_CELLS[1], manaStorageCell16k=STORAGE_CELLS[2], manaStorageCell64k=STORAGE_CELLS[3], manaStorageCell256k=STORAGE_CELLS[4], manaStorageCell1m=STORAGE_CELLS[5], manaStorageCell4m=STORAGE_CELLS[6], manaStorageCell16m=STORAGE_CELLS[7], manaStorageCell64m=STORAGE_CELLS[8], manaStorageCell256m=STORAGE_CELLS[9], manaStorageCell1g=STORAGE_CELLS[10];
    public BotaniaApplie() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);
        bus.addGenericListener(Item.class, this::registerItems);
        bus.addGenericListener(Block.class, this::registerBlocks);
        bus.addGenericListener(TileEntityType.class, this::registerTiles);
        bus.addGenericListener(EntityType.class, this::registerEntities);
        bus.addListener(this::setup);
        bus.addListener(this::registerTerminalResources);
        bus.addListener(this::enqueueIMC);
        MinecraftForge.EVENT_BUS.register(ChannelSparkNetwork.class);
        MinecraftForge.EVENT_BUS.register(new ManaCardTickHandler());
        MinecraftForge.EVENT_BUS.register(new ObedienceStickHandler());
        MinecraftForge.EVENT_BUS.register(new WirelessManaItemsHandler());
    }
    private void registerItems(RegistryEvent.Register<Item> e) { ITEMS.forEach(e.getRegistry()::register); }
    private void registerBlocks(RegistryEvent.Register<Block> e) { e.getRegistry().register(fluixManaPool); }
    private void registerTiles(RegistryEvent.Register<TileEntityType<?>> e) { e.getRegistry().register(FLUIX_POOL_TILE); }
    private void registerEntities(RegistryEvent.Register<EntityType<?>> e) { e.getRegistry().registerAll(CHANNEL_SPARK, MAIN_CHANNEL_SPARK); }
    private void registerTerminalResources(ResourceRegistrationEvent e) { e.register(new ManaResourceProvider()); }
    private void setup(FMLCommonSetupEvent e) { e.enqueueWork(() -> {
        Api.instance().storage().registerStorageChannel(ManaStorageChannel.class, ManaStorageChannel.INSTANCE);
        net.minecraftforge.common.crafting.CraftingHelper.register(new nyonio.recipe.ConfigCondition.Serializer());
        Api.instance().registries().cell().addCellHandler(ManaCellHandler.INSTANCE);
        UpgradeModuleRegistration.register(fluixPoolCard, 1);
    }); }
    private void enqueueIMC(InterModEnqueueEvent e) { if (net.minecraftforge.fml.ModList.get().isLoaded("theoneprobe")) nyonio.integration.top.TopIntegration.register(); }
    public static Logger getLogger() { return LOGGER; }
    public static SimpleChannel getNetwork() { return NETWORK; }
    public static ResourceLocation id(String name) { return new ResourceLocation(MODID,name); }
    public static Item.Properties properties() { return new Item.Properties().group(BotaniaApplieTab.INSTANCE); }
    private static <T extends Item> T item(String name, T item) { item.setRegistryName(id(name)); ITEMS.add(item); return item; }
    private static Item[] createCells() {
        String[] names={"1k","4k","16k","64k","256k","1m","4m","16m","64m","256m","1g"};
        Item[] cells=new Item[names.length];
        for (int i=0,size=1;i<names.length;i++,size*=4) cells[i]=item("mana_storage_cell_"+names[i],new ItemManaStorageCell(size,0.5+i*0.5));
        return cells;
    }
    private static Item[] createComponents() {
        String[] names={"1k","4k","16k","64k","256k","1m","4m","16m","64m","256m","1g"};
        Item[] parts=new Item[names.length];
        for(int i=0,size=1;i<names.length;i++,size*=4) parts[i]=item("mana_storage_component_"+names[i],new ItemManaStorageComponent(size));
        return parts;
    }
}
