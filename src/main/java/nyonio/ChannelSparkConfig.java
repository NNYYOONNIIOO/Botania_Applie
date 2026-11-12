package nyonio;
public final class ChannelSparkConfig {
    private ChannelSparkConfig() {}
    public static int getTransferRadius() { return ModConfig.SPARK_RADIUS.get(); }
}
