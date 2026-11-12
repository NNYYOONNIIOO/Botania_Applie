package nyonio.integration.top;
import mcjty.theoneprobe.api.ITheOneProbe;
import net.minecraftforge.fml.InterModComms;
import java.util.function.Function;
public final class TopIntegration {
    private TopIntegration() {}
    public static void register() { InterModComms.sendTo("theoneprobe","getTheOneProbe",() -> (Function<ITheOneProbe,Void>) probe -> {probe.registerProvider(new FluixManaPoolProbeProvider());return null;}); }
}
