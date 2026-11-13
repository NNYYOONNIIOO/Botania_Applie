package nyonio.integration.jade;

import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.loading.FMLEnvironment;
import nyonio.BotaniaApplie;
import nyonio.client.render.ManaBarTooltipRenderer;
import nyonio.tile.TileFluixManaPool;

/** Discovered by Jade; nothing in common setup loads the optional Jade API. */
@WailaPlugin(BotaniaApplie.MODID)
public final class JadeIntegration implements IWailaPlugin {
    public static final ResourceLocation MANA = new ResourceLocation(BotaniaApplie.MODID, "mana");

    @Override
    public void register(IRegistrar registrar) {
        FluixManaPoolProvider provider = new FluixManaPoolProvider();
        registrar.addConfig(MANA, true);
        registrar.registerBlockDataProvider(provider, TileFluixManaPool.class);
        registrar.registerComponentProvider(provider, TooltipPosition.BODY, TileFluixManaPool.class);
        if (FMLEnvironment.dist.isClient()) {
            registrar.registerTooltipRenderer(MANA, new ManaBarTooltipRenderer());
        }
    }
}
