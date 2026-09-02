package nyonio;

import net.minecraftforge.common.config.Config;

@Config(modid = BotaniaApplie.MODID, category = "channel_spark")
public final class ChannelSparkConfig {
    @Config.Name("channel_capacity")
    @Config.Comment("The maximum AE2 channel capacity carried by a channel spark link.")
    public static int channelCapacity = 32;

    @Config.Name("transfer_radius")
    @Config.Comment("The maximum distance between channel sparks, in blocks.")
    public static int transferRadius = 36;

    private ChannelSparkConfig() {
    }

    public static int getChannelCapacity() {
        return Math.max(1, Math.min(32, channelCapacity));
    }

    public static int getTransferRadius() {
        return Math.max(1, transferRadius);
    }
}
