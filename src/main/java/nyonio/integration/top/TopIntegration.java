package nyonio.integration.top;

import java.util.function.Function;

import mcjty.theoneprobe.api.ITheOneProbe;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInterModComms;

public final class TopIntegration {
    private TopIntegration() {
    }

    public static void register() {
        if (Loader.isModLoaded("theoneprobe")) {
            FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", Register.class.getName());
        }
    }

    public static class Register implements Function<ITheOneProbe, Void> {
        @Override
        public Void apply(ITheOneProbe probe) {
            probe.registerProvider(new FluixManaPoolProbeProvider());
            return null;
        }
    }
}
