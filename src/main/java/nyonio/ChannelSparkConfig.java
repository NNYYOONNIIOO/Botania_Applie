package nyonio;

import net.minecraftforge.common.config.Config;

@Config(modid = BotaniaApplie.MODID, category = "channel_spark")
public final class ChannelSparkConfig {
    @Config.Name("transfer_radius")
    @Config.Comment("The maximum distance between channel sparks, in blocks.")
    public static int transferRadius = 36;

    private ChannelSparkConfig() {
    }

    public static int getTransferRadius() {
        return Math.max(1, transferRadius);
    }
}
