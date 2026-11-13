package nyonio.smoke;

import appeng.core.Api;
import appeng.tile.misc.InterfaceTileEntity;
import appeng.tile.networking.CableBusTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import nyonio.BotaniaApplie;
import nyonio.IFluixManaReceiver;
import nyonio.ae2.ManaStorageChannel;
import nyonio.mixin.TilePoolManaAccessor;
import nyonio.tile.TileFluixManaPool;
import nyonio.terminal_interaction_integration.api.TerminalInteractionRegistry;

/** Only loaded with -PsmokeTest, and never included in the release jar. */
@Mod("botania_applie_smoke")
public final class PortSmokeTest {
    private boolean verified;
    private boolean finished;
    private Throwable setupFailure;

    public PortSmokeTest() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BotaniaApplie.getLogger().info("PORT_SMOKE_SETUP_START");
            require(Api.instance().storage().getStorageChannel(ManaStorageChannel.class)
                    == ManaStorageChannel.INSTANCE, "mana channel registration");
            require(TerminalInteractionRegistry.getProvider("mana") != null,
                    "terminal integration registration");
            require(IFluixManaReceiver.class.isAssignableFrom(CableBusTileEntity.class),
                    "cable bus mana interface");
            require(IFluixManaReceiver.class.isAssignableFrom(InterfaceTileEntity.class),
                    "item interface mana interface");
            TileFluixManaPool pool = new TileFluixManaPool();
            require(pool instanceof TilePoolManaAccessor, "Botania pool mana accessor");
            require(pool.getType() == BotaniaApplie.FLUIX_POOL_TILE, "pool tile type");

            verified = true;
            BotaniaApplie.getLogger().info("PORT_SMOKE_SETUP_PASSED");
        }).whenComplete((ignored, failure) -> {
            if (failure != null) {
                setupFailure = failure;
                verified = true;
                BotaniaApplie.getLogger().error("PORT_SMOKE_SETUP_FAILED", failure);
            }
        });
    }

    private void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || !verified || finished
                || !(mc.currentScreen instanceof MainMenuScreen)) return;
        finished = true;
        try {
            require(setupFailure == null, "startup checks failed");
            verifyRecipes();
            // Models and textures have finished loading by the main menu.
            for (String texture : new String[] {"channel_spark", "main_channel_spark"}) {
                ResourceLocation sprite = BotaniaApplie.id("items/" + texture);
                require(mc.getAtlasSpriteGetter(net.minecraft.client.renderer.texture.AtlasTexture.LOCATION_BLOCKS_TEXTURE)
                        .apply(sprite).getName().equals(sprite), "spark texture " + texture);
                require(mc.getRenderManager().getRenderer((texture.equals("channel_spark")
                        ? BotaniaApplie.CHANNEL_SPARK : BotaniaApplie.MAIN_CHANNEL_SPARK)
                        .create(null)) != null, "spark entity renderer " + texture);
            }
            BotaniaApplie.getLogger().info("PORT_SMOKE_CLIENT_PASSED");
        } catch (Throwable failure) {
            BotaniaApplie.getLogger().error("PORT_SMOKE_CLIENT_FAILED", failure);
        } finally {
            mc.shutdown();
        }
    }

    private static void require(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }

    private static void verifyRecipes() throws java.io.IOException {
        java.nio.file.Path folder = java.nio.file.Paths.get("../src/main/resources/data/botania_applie/recipes");
        int count = 0;
        try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.list(folder)) {
            for (java.nio.file.Path path : (Iterable<java.nio.file.Path>) paths.filter(p -> p.toString().endsWith(".json"))::iterator) {
                com.google.gson.JsonObject json;
                try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(path)) {
                    json = new com.google.gson.JsonParser().parse(reader).getAsJsonObject();
                }
                String name = path.getFileName().toString().replace(".json", "");
                net.minecraft.item.crafting.IRecipeSerializer<?> serializer = net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS
                        .getValue(new ResourceLocation(json.get("type").getAsString()));
                require(serializer != null && serializer.read(BotaniaApplie.id(name), json) != null,
                        "recipe " + name);
                count++;
            }
        }
        BotaniaApplie.getLogger().info("PORT_SMOKE_RECIPES_PASSED: {}", count);
    }
}
