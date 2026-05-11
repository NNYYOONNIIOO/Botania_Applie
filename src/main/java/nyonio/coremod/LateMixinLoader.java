package nyonio.coremod;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import zone.rong.mixinbooter.ILateMixinLoader;

import javax.annotation.Nonnull;
import java.util.List;

@SuppressWarnings("unused")
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        List<String> configs = new ObjectArrayList<>();
        configs.add("botania_applie.mixins.json");
        return configs;
    }

    @Override
    public boolean shouldMixinConfigQueue(@Nonnull String mixinConfig) {
        return "botania_applie.mixins.json".equals(mixinConfig);
    }
}