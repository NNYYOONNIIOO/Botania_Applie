package nyonio;
import net.minecraftforge.common.ForgeConfigSpec;
public final class ModConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HOUSING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_COMPONENTS;
    public static final ForgeConfigSpec.IntValue SPARK_RADIUS;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("items");
        ENABLE_HOUSING=b.define("enable_mana_cell_housing",false);
        ENABLE_COMPONENTS=b.define("enable_mana_storage_components",true);
        b.pop().push("channel_spark");
        SPARK_RADIUS=b.comment("Maximum distance between adjacent channel sparks; intermediate sparks relay the network.").defineInRange("transfer_radius",36,1,4096);
        b.pop();
        SPEC=b.build();
    }
}
